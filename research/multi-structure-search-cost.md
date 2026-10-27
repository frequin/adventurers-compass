# Cost of a multi-Structure search over a Selection

Research for issue #3. Question: for a **Selection** of N Structures/Groups, how do we find the **nearest Instance** across every placement involved, without multiplying the cost by N and without stalling the server tick? Also: what does filtering out **Visited** Instances cost?

Terms follow `GLOSSARY.md` (Structure, Group, Instance, Selection, Visited).

## Sources

All vanilla references are Minecraft 26.3, decompiled with `./gradlew genSources` (Loom 1.17, mojmap names). The sources jar is at
`.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-86785c7ddb/26.3/minecraft-merged-86785c7ddb-26.3-sources.jar`. Line numbers below refer to that decompilation. Structure-set data comes from `data/minecraft/worldgen/structure_set/*.json` in the 26.3 merged jar.

- [V1] `net/minecraft/world/level/chunk/ChunkGenerator.java`: `findNearestMapStructure` L160-235, rings helper L237-268, random-spread ring helper L270-301, `getStructureGeneratingAt` L303-327.
- [V2] `net/minecraft/world/level/levelgen/structure/StructureCheck.java`: `checkStart` L88-116, `canCreateStructure` L118-134, `tryLoadFromStorage` L136+.
- [V3] `net/minecraft/world/level/levelgen/structure/Structure.java`: `findValidGenerationPoint` L197-199 (`findGenerationPoint(...).filter(isValidBiome)`).
- [V4] `net/minecraft/world/level/levelgen/structure/pools/JigsawPlacement.java`: `addPieces` L51+. It checks the biome column (L113), samples one heightmap column (L117), and returns a lazy `GenerationStub` (L130). The full piece assembly runs later, not during the presence check.
- [V5] `net/minecraft/world/level/levelgen/structure/placement/`: `StructurePlacement` is now an **interface** (`isStructureChunk`, `applyAdditionalChunkRestrictions`, `getLocatePos`). Implementations: `AbstractSpreadingStructurePlacement` (frequency, salt, exclusion zone), `RandomSpreadStructurePlacement` (`getPotentialStructureChunk` is one seeded RNG draw per cell), `ConcentricRingsStructurePlacement`, and the new `DimensionOriginStructurePlacement` (exactly one chunk, the dimension origin; vanilla data does not use it).
- [V6] `net/minecraft/server/commands/LocateCommand.java` L50, L99: `/locate` calls `findNearestMapStructure(..., 100, false)` synchronously and times it with a stopwatch.
- [V7] `ChunkGeneratorStructureState.getRingPositionsFor` L187-191: the ring positions are a precomputed `CompletableFuture<List<ChunkPos>>` (`join()`).
- [M1] Mod: `src/main/java/io/github/frequin/adventurerscompass/worker/` (`SearchWorkerManager`, `StructureSearchWorker`, `RandomSpreadSearchWorker`, `ConcentricRingsSearchWorker`, `GenericSearchWorker`, `WorldWorkerManager`).
- [M2] Mod: `item/AdventurersCompassItem.java` L114-206 (`searchForStructure`, `succeed`, `fail`), `util/StructureUtils.java` L50-79 (a Group is a vanilla `StructureSet`), `mixin/MinecraftServerMixin.java`.

## 1. What the mod does today

- **Group = StructureSet = (almost always) one placement** [M2 `getStructuresForGroup`]. `SearchWorkerManager.createWorkers` groups the requested Structures by `StructurePlacement` and creates **one worker per placement** [M1].
- Workers run **one after another**. `start()` starts only `workers.get(0)`. `fail()` pops it and starts the next one. `succeed()` calls `workerManager.clear()` [M2 L180-206].
  - So the result is **the first Instance found by the first placement that finds one**, not the nearest across placements. Applied to a Selection of N placements as-is, this would be wrong, not just slow.
- Inside a worker, `getStructureGeneratingAt` loops over every Structure that shares the placement and calls `checkStructurePresence` for each one [M1 `StructureSearchWorker`]. This copies vanilla [V1 L303-327] with `createReference=false`.
- `RandomSpreadSearchWorker` walks square rings of **cells** (cell size = `spacing` chunks) and stops at the **first** hit in ring order. A ring corner sits about √2 farther than its edge midpoints, so the hit is "near-nearest", not exact. Vanilla has the same approximation.
- `GenericSearchWorker` is the fallback for any other placement type (in 26.3 that means `DimensionOriginStructurePlacement` or modded types). It samples **every chunk** in a spiral, which costs about 12,000 samples at 1,000 blocks.
- **Time slicing** [M1 `WorldWorkerManager`, `MinecraftServerMixin`]: `tick(true)` at the head of `tickServer`, then at the tail `tick(false)` runs `doWork()` until 50 ms after the tick started (minimum 10 ms). The search therefore only uses spare tick time. One `doWork()` handles one candidate and cannot be interrupted, so a tick can overshoot by the cost of one candidate (see the expensive class D below).

## 2. Cost model of one candidate

`checkStructurePresence` → `StructureCheck.checkStart` [V2 L88-116]. For a candidate chunk and one Structure, the cost falls into one of four classes:

| Class | Path | Order of magnitude (estimate) |
|---|---|---|
| A | Hit in `loadedChunks` or `featureChecks` caches | ~1 µs |
| B | `tryLoadFromStorage`: partial NBT scan of the region file with a **blocking `.join()`**; skipped once the chunk is in `chunksWithoutStartsInStorage` (capped at 131,072 entries) | µs if there is no region file, ~0.1-1 ms if the chunk exists on disk |
| C | `applyAdditionalChunkRestrictions` (frequency), then `canCreateStructure` → `findValidGenerationPoint` = biome check + one heightmap column sample [V3, V4] | ~0.1-0.5 ms (noise sampling for one column) |
| D | C passed (`CHUNK_LOAD_NEEDED`) → `level.getChunk(x, z, STRUCTURE_STARTS)`: synchronous proto-chunk generation up to STRUCTURE_STARTS, which runs `createStructures` for **every** structure set on that chunk, including full jigsaw assembly; the proto-chunk then gets saved | ~5-30 ms in fresh terrain; ~A/B if already generated |

Key observations:

- **Class D is what costs, and it happens almost only on candidates that turn out to be real Instances.** A candidate that fails the biome/height check stops at C, and one that passes C almost always produces a valid start. So total cost ≈ (number of C checks × ~0.2 ms) + (**number of Instances whose start we force-generate** × ~10 ms).
- Order in `checkStart`: the storage scan (B) runs **before** the cheap frequency filter. For spacing-1 placements (mineshafts `frequency 0.004`, buried treasure `0.01`), every chunk in the radius gets a storage scan, then mostly a frequency reject. Calling the public `placement.applyAdditionalChunkRestrictions(x, z, seed)` [V5] **before** `checkStructurePresence` drops about 12,000 candidates to about 50 at 1,000 blocks.
- Structures that share a placement multiply the C cost by k: villages k=5, ruined portals k=7, `abandoned_camp` k=18 in 26.3. This is mostly cheap biome rejections, because each Structure has its own biome predicate.

### Candidate counts at 1,000 blocks (62.5 chunks)

From the 26.3 structure sets, most vanilla random-spread placements have `spacing` 20-40 (villages 34, desert pyramids 32, ancient cities 24, ocean ruins 20, mansions 80; strongholds are concentric rings, count 128).

| spacing (chunks) | candidates in disc (≈ π·62.5²/s²) | in the mod's square rings ((2⌈62.5/s⌉+1)²) |
|---|---|---|
| 20 | ~31 | 81 |
| 32-34 | ~11-12 | 25 |
| 80 | ~2 | 9 |
| 1 (+ frequency) | ~12,000 chunks, ~50 after the frequency filter | ~16,000 |
| concentric rings | ≤128 fixed positions (precomputed [V7]) | n/a |

## 3. Strategies compared

Assumptions for the numbers below (orders of magnitude, **not measured**): every Structure in the Selection has **its own placement** (worst case), spacing ≈ 32 (≈ 12 candidates in the disc, 25 in the square), and the fraction of candidates that pass check C is p ≈ 0.25. Costs: c_C ≈ 0.2 ms, c_D ≈ 10 ms. Spare tick budget ≈ 35 ms/tick (server tick of 15 ms). For fixed (p, s), the expected nearest-Instance distance shrinks like 1/√N as N grows.

### (a) Sequential workers until the first hit (current behaviour)

- Cost ≈ 1/p C-checks + 1 D for the first placement only: ~10-15 ms whatever N is.
- **Wrong answer**: it returns the nearest Instance of placement #1 in map insertion order, not the nearest overall.
- Made correct the naive way (run each worker until its own hit, then keep the minimum): N × (1/p · c_C + c_D) ≈ **11·N ms**. It also **force-generates N proto-chunks**, one per placement. This is the "×N" the issue worries about.

### (a') Sequential workers with a shrinking bound (branch-and-bound)

- Run the workers in sequence and pass `bestDistSq` along. Each worker only evaluates candidates closer than the current best, and stops once its next ring's minimum distance exceeds the bound.
- Expected cost: worker i only finds something if it beats the best of the i-1 before it, so the expected number of D ≈ H_N ≈ 1 + ln N, and the expected C-checks ≈ (1 + ln N)/p.
- Correct (up to the within-ring approximation if rings are kept). Small change to the existing code: one `double bound` shared by the manager, and a distance test before `getStructureGeneratingAt`.

### (b) Interleaved scan, global best-first (recommended)

- One worker per Selection. It lazily enumerates **candidate positions** for all placements and pushes them into a single min-heap keyed by **exact** distance. Producing a candidate is cheap: one RNG draw per random-spread cell (`getPotentialStructureChunk`), one list read for rings, one chunk for dimension-origin. Each `doWork()` pops the nearest candidate and runs the expensive check. **The first valid, non-Visited candidate popped is exactly the nearest Instance.** Every closer candidate has already been rejected, so the within-ring approximation disappears as well.
- To keep the heap small, refill it ring by ring: for each placement, push ring r+1 when the heap top is farther than that ring's minimum possible distance ((r·s - s)·16 blocks for random spread, because the jitter stays inside the cell).
- Expected expensive cost: about π/(4p) + 1 ≈ 4 C-checks and **about 1 D, independent of N**. Within the nearest distance d, the number of candidates is the area divided by the cell area, and d² ∝ 1/(N·p) cancels the N.
- Enumeration overhead: N × (cells per ring) RNG draws, which is well under 1 ms even for N = 100.
- Interleaving by **ring index** (what vanilla does) is **not** distance-ordered when spacings differ: ring 2 of a spacing-10 placement covers about 320 blocks, ring 2 of a spacing-40 placement about 1,280. The interleaving must be by distance.

### (c) Delegating to `ChunkGenerator.findNearestMapStructure(level, HolderSet, pos, radius, createReference)` [V1]

- Pros: already groups by placement, one call for the whole Selection.
- Cons, all visible in the source:
  1. **Synchronous**: one call on the server thread, so the whole cost lands in one tick (`/locate` measures it with a stopwatch [V6]).
  2. **Interleaves by ring index, not distance**, and returns as soon as any placement hits in ring r [V1 L205-231]. With mixed spacings it can return a far Instance of a large-spacing placement before a closer Instance of a small-spacing one. Within a ring it also takes the first hit, not the nearest.
  3. In the terminating ring, **every** placement that has a hit force-generates its proto-chunk (class D): up to N × c_D in one tick, ≈ 1 s for N = 100.
  4. **Ignores** any placement that is neither rings nor random spread [V1 L188-201], so `DimensionOriginStructurePlacement` and modded placement types are missed.
  5. **No skip list**. Its only exclusion mechanism is `createReference=true`, which increments the start's reference count in world data (`tryAddReference` L329). That is global, persisted, not per player and not reversible, so it is incompatible with Visited. It would also make vanilla explorer maps skip those Instances.
  6. `radius` is counted in **cells of each placement's spacing**, not blocks, so "1,000 blocks" has no single value.
- Running it off-thread is not an option: `StructureCheck` uses plain, non-synchronized maps [V2] and `getChunk` must run on the server thread.

### Orders of magnitude, 1,000-block radius (estimates)

| Strategy | N = 5 | N = 20 | N = 100 |
|---|---|---|---|
| (a) first placement wins (current) | ~15 ms, wrong result | ~15 ms, wrong result | ~15 ms, wrong result |
| (a) naive, all placements then min | ~55 ms, 5 proto-chunks | ~220 ms, 20 proto-chunks | ~1.1 s, ~100 proto-chunks |
| (a') sequential + shrinking bound | ~30 ms, ~2-3 D | ~45 ms, ~4 D | ~60 ms, ~5-6 D |
| (b) global best-first | ~12 ms, ~1 D | ~12 ms, ~1 D | ~13 ms, ~1 D |
| (c) vanilla delegation | ~50 ms **in one tick** | ~200 ms in one tick | ~1 s in one tick, inexact, no Visited |
| Floor: no Instance in radius (all must scan every candidate) | 5×12×c_C ≈ 12 ms | ≈ 50 ms | ≈ 250 ms (+ spacing-1 placements without the frequency prefilter: ~12,000 storage scans each) |

For the time-sliced strategies (a/a'/b), wall-clock time ≈ cost / 35 ms ticks: everything above finishes within a few ticks to about 30 ticks (≈1.5 s), with at most one uninterruptible D (~10-30 ms) per tick. Strategy (c) cannot be sliced. Modded packs with many small-spacing placements raise the "no Instance" floor first. The answer-finding cost under (b) stays roughly flat.

## 4. Cost of filtering out Visited Instances

- Today: `shouldIgnore(pos)` = `prevPos.contains(pos)` on a `List<BlockPos>` [M1]. It runs only **after** a candidate is confirmed, so it is O(V) per hit and negligible for V ≤ `maxNextSearches` (100). The real waste: each Visited Instance in front of the target still pays its full presence check before being discarded. That check is usually cheap (A/B), because a Visited Instance's chunks are already generated and on disk.
- Better: run the check **before** `checkStructurePresence`. The locate position is known from the candidate chunk alone (`placement.getLocatePos(candidateChunk)` [V5]; for random spread the start chunk is the candidate chunk). With a `LongOpenHashSet` of Visited locate positions (per player, per world, per GLOSSARY), the cost is **O(1) per candidate, and a Visited Instance costs nothing**. This holds up as Visited grows into the thousands, which a `List` would not.
- Effect on search cost: with V Visited Instances closer than the nearest non-Visited one, the search area grows ∝ V + 1. Under (b), C-checks ≈ (V+1)·(1/p - 1) + 1 and D stays ≈ 1, because Visited ones are skipped before any check. The number of Visited Instances does not multiply generation cost.
- Caveat: a Visited set keyed by locate position relies on `locateOffset` staying stable for a given placement. That holds unless a datapack changes it between sessions.

## 5. Recommendation

1. Replace "one worker per placement, first success wins" with **one best-first worker per Selection** (b). Keep `WorldWorkerManager` time slicing at one heap pop per `doWork()`.
2. Before `checkStructurePresence`, in this order: Visited hash lookup → `applyAdditionalChunkRestrictions` (frequency) → presence check.
3. Do **not** delegate to `findNearestMapStructure`: it runs in one tick, is inexact across spacings, has no per-player skip, and ignores some placement types.
4. If a smaller first step is wanted, (a') adds correctness to the existing workers with about one shared variable, at a ~ln N extra cost.
5. Calibrate c_C / c_D with a profiler (e.g. spark) on a real modpack before relying on the absolute numbers. The **ratios** between strategies are the robust part of this analysis.
