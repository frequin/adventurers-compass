# Detecting that a player is inside an Instance (Fabric 26.3)

Research for issue #4. Vocabulary follows `GLOSSARY.md` (Structure, Instance, Visited).

## Sources

- **[MC]** Vanilla 26.3 classes from `~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/26.3/minecraft-merged-deobf-26.3.jar`. 26.x ships unobfuscated, so the names below are the real Mojang names. No genSources jar was in the cache, so every claim about behaviour comes from `javap -c -p` bytecode of these classes.
- **[FAPI-jar]** `fabric-data-attachment-api-v1-2.2.30+3434d6d95d.jar`, the version pinned by `fabric-api-0.160.6+26.3.pom` (the version in `gradle.properties`), inspected with `javap`.
- **[FAPI-src]** Fabric API sources on the `26.3` branch on GitHub: `fabric-data-attachment-api-v1/.../AttachmentRegistry.java`, `AttachmentTarget.java`, `fabric-entity-events-v1/.../ServerPlayerMixin.java`.
- **[Mod]** This repo: `src/main/java/io/github/frequin/adventurerscompass/worker/StructureSearchWorker.java`.

## 1. Server APIs that answer "is position P inside an Instance?"

All of them are on `net.minecraft.world.level.StructureManager`, which you get from `ServerLevel#structureManager()` [MC].

| Method | What it tests | Precision |
|---|---|---|
| `getAllStructuresAt(BlockPos)` → `Map<Structure, LongSet>` | Returns the structure *references* stored in the chunk containing P. Each `long` is the packed `ChunkPos` of a `StructureStart`. | Chunk level, XZ only. The cheapest pre-filter. |
| `getStructureAt(BlockPos, Structure \| HolderSet)` → `StructureStart` | Takes the starts referenced by P's chunk and returns the first whose `getBoundingBox().isInside(P)`. | Overall bounding box (3D AABB), see the caveat below. |
| `getStructureWithPieceAt(BlockPos, TagKey \| HolderSet \| Predicate<Holder<Structure>>)` and `(x,y,z, Structure)` → `StructureStart` | Same lookup, then `structureHasPieceAt`: does any `StructurePiece#getBoundingBox()` contain P? | Per-piece AABBs. Tightest available. |
| `structureHasPieceAt(BlockPos, StructureStart)` | The per-piece test alone, for a start you already hold. | Per-piece AABBs. |

All of these return `StructureStart.INVALID_START` when nothing matches. Check the result with `isValid()`.

How they work, from the bytecode [MC]:

- `startsForStructure(chunkX, chunkZ, …)` calls `level.getChunk(cx, cz, ChunkStatus.STRUCTURE_REFERENCES)`, reads `ChunkAccess#getAllReferences()` (or `getReferencesForStructure`), then calls `fillStartsForStructure`.
- `fillStartsForStructure` unpacks each reference `long` into a `ChunkPos` and calls `level.getChunk(x, z, ChunkStatus.STRUCTURE_STARTS)`. It reads the start with `getStartForStructure` and keeps it only if `isValid()`.
- References are written at worldgen by `ChunkGenerator#createReferences`. For every start within **8 chunks**, it adds a reference to a chunk if `start.getBoundingBox().intersects(chunkMinX, chunkMinZ, chunkMinX+15, chunkMinZ+15)`. So "the chunk has a reference" means "the start's box overlaps this chunk in XZ".
- `StructureStart#getBoundingBox()` is computed once, cached in a `volatile` field, and equals `structure.adjustBoundingBox(pieceContainer.calculateBoundingBox())`. `Structure#adjustBoundingBox` **inflates the box by 12 blocks** whenever `terrainAdaptation() != TerrainAdjustment.NONE`. That covers villages, ancient cities, trail ruins, trial chambers and similar. As a result, `getStructureAt` fires up to 12 blocks before the player reaches any built block. It also fires anywhere inside the large envelope of a sprawling village. The per-piece test does not use this inflated box.

**Precision verdict.** The piece test (`getStructureWithPieceAt` / `structureHasPieceAt`) is what vanilla itself uses for "is the player in structure X". Pieces are still axis-aligned boxes, so a village counts houses and road boxes, not the air between them. Some pieces overhang slightly. A Visited mark wants "the player actually entered the place", so the piece test fits better than the overall box, which over-reports on terrain-adapted structures.

### Modded structures

The methods key on the `Structure` registry object and on generic `StructurePiece` boxes. Nothing in them is vanilla-specific, and `getStructureWithPieceAt(pos, Predicate<Holder<Structure>>)` accepts any predicate, including "every structure". A modded structure that registers as a `Structure` and generates a `StructureStart` with pieces is detected the same way. Two kinds are not detected:

- "Structures" placed as plain features (no `StructureStart`). These are not Instances anyway.
- Starts generated before the mod was installed. This is not a problem, since starts and references are saved with chunks no matter which mod reads them.

## 2. The vanilla `location` advancement trigger

- `ServerPlayer#doTick()` runs `if (tickCount % 20 == 0) CriteriaTriggers.LOCATION.trigger(this)` [MC]. So vanilla polls each player's location **once per second (every 20 ticks)**.
- `LocationPredicate` has an `Optional<HolderSet<Structure>> structures` field. Its `matches(ServerLevel, x, y, z)` first checks `ServerLevel#isLoaded(pos)`, then calls `level.structureManager().getStructureWithPieceAt(pos, structures).isValid()` [MC]. In other words, the advancement route is exactly the piece test from section 1.
- `PlayerTrigger` is a `SimpleCriterionTrigger`. It only matches criteria from advancements loaded through datapacks, and it reports only a boolean "matched". It does not return **which** `StructureStart` matched, and a criterion completes once and stays done. So it cannot tell Instance A from Instance B, cannot re-fire for each new Instance, and cannot be reversed per Instance.

**Verdict:** don't build Visited detection on the advancement system. Copy its approach instead: poll every 20 ticks with `getStructureWithPieceAt`, but in our own server tick handler, so we get the `StructureStart` back.

## 3. Cost per tick per player

With a poll every N ticks (vanilla uses N = 20) at the player's block position:

1. `getAllStructuresAt(pos)` costs one `getChunk(..., STRUCTURE_REFERENCES)` on the player's own chunk, which is always loaded at FULL. That is a cache hit in `ServerChunkCache` on the main thread, followed by a map read. In most chunks the map is empty and the check ends there, so this is the cheap pre-filter.
2. For each referenced start: one `getChunk(..., STRUCTURE_STARTS)` on the start chunk. The start chunk is at most 8 chunks away, so it is normally loaded around the player. Then one map lookup, and the bounding box (cached after the first call).
3. The piece test is a linear scan over `getPieces()` with an AABB contains-check per piece. That is a few hundred box checks at most for the largest jigsaw structures, and only while the player is inside a reference chunk.

There is no worldgen, no allocation beyond small immutable lists (the `ImmutableList.builder` in `startsForStructure`), and no disk I/O while chunks are loaded. Vanilla already pays the same cost for every player with an open structure-location criterion. So polling every 20 ticks per online player costs about as much as vanilla's own location check. Possible optimisations:

- Skip players whose `chunkPosition()` has not changed and who are not in a reference chunk.
- Use the `Predicate<Holder<Structure>>` overload so that only the needed Structures are scanned.

One caveat: `getChunk(..., status)` uses `require = true`. In the rare case where a start chunk is not loaded (very low view distance, or the player at the edge of the loaded area), the call can load that chunk from disk synchronously. Using a ticking player position keeps this rare. A guard with `ServerLevel#isLoaded`, as `LocationPredicate` does, is cheap.

Multiplayer: everything runs on the server thread with `ServerLevel` and `ServerPlayer`. Every connected player is handled the same way, for example by iterating `server.getPlayerList().getPlayers()` in a Fabric `ServerTickEvents.END_SERVER_TICK` (lifecycle-events-v1 4.1.9 is in the 0.160.6 bundle). No client participation is needed.

## 4. Identifying an Instance stably

- A `StructureStart` holds a `final ChunkPos chunkPos` (`getChunkPos()`) and a `Structure` (`getStructure()`) [MC]. It is saved in chunk NBT as `id` (structure registry id), `ChunkX`, `ChunkZ`, `references` and `Children` (`createTag`). Neither the start chunk nor the id ever changes after generation.
- Starts are stored per chunk as `Map<Structure, StructureStart>` (`ChunkAccess#getAllStarts`). So a given chunk holds **at most one start per Structure**, and `(Structure id, start ChunkPos)` is unique within a dimension.
- The reference `long` returned by `getAllStructuresAt` is that same packed start `ChunkPos` (`ChunkPos.unpack(long)` in `fillStartsForStructure`). You can therefore build the key **without** loading the start, and only load the start for the piece test.
- The mod's search already derives what it shows from the start chunk: `placement.getLocatePos(structureStart.getChunkPos())` in `StructureSearchWorker` [Mod]. Search and detection can therefore share one key.

**Recommended Instance key:** `(ResourceKey<Level> dimension, Identifier structureId, ChunkPos startChunk)`. Store it as `(dim, id, long packedChunkPos)`. Do not key on the overall bounding-box origin: it is inflated by 12 blocks for terrain-adapted structures. Do not key on a `BlockPos` from `getLocatePos`: that is derived from the placement, not a field of the Instance.

## 5. Persisting Visited per player and per world (Fabric 26.3)

### Option A (recommended): Fabric Data Attachment on the `ServerPlayer`

API from [FAPI-jar] and [FAPI-src]:

- `AttachmentRegistry.<A>builder()` takes `.persistent(Codec<A>)`, `.copyOnDeath()`, `.initializer(Supplier<A>)` and `.syncWith(StreamCodec, AttachmentSyncPredicate)`, then `.buildAndRegister(Identifier)`. `AttachmentSyncPredicate.targetOnly()` syncs to the owning player only.
- `AttachmentTarget` is implemented on `Entity`, `BlockEntity`, `ServerLevel`, `ChunkAccess` and `GlobalAttachments` (server-wide, through `GlobalAttachmentsProvider#globalAttachments()` on `MinecraftServer`). Its methods include `getAttached`, `getAttachedOrCreate`, `setAttached`, `modifyAttached` and `removeAttached`.
- Persistence: `EntityMixin` reads and writes attachments together with the entity data. A player's entity data lives in the world save's `playerdata/<uuid>.dat`. Data on the player is therefore **already per world (save) and per player**, in single player and in multiplayer alike.
- Respawn: `AttachmentEntrypoint` registers `ServerPlayerEvents.AFTER_RESPAWN` and calls `AttachmentTargetImpl.transfer(old, new, !alive)`. When the player has died, only types marked `copyOnDeath()` are copied. The Javadoc says: when a player dies and respawns, the attachments of a `copyOnDeath` type "should remain". **Visited data must be `copyOnDeath()`**, otherwise it is lost on death.
- Dimension change: the same entrypoint registers `ServerEntityLevelChangeEvents.AFTER_ENTITY_CHANGE_LEVEL` and transfers everything. In current versions a `ServerPlayer` is kept across dimension changes anyway, so the data stays put.
- Value: an immutable record holding a `Set<InstanceKey>`, with a `Codec` for persistence. Optionally add a `StreamCodec` with `targetOnly()` if the client screen needs to show Visited state. Use `modifyAttached` / `setAttached` with new immutable values: `setAttached` is what marks the change dirty and triggers sync.

### Option B: vanilla `SavedData` keyed by UUID

`SavedDataType<T>(Identifier id, Supplier<T>, Codec<T>, DataFixTypes)` with `SavedDataStorage#computeIfAbsent`. `getDataStorage()` exists on both `MinecraftServer` and `ServerLevel` [MC]. You would hold a `Map<UUID, Set<InstanceKey>>` and call `setDirty()` on every change. This works offline (you can edit a player's Visited set while they are logged out), but you have to handle syncing and per-player cleanup yourself. The Fabric `GlobalAttachments` target offers the same server-wide storage with the attachment API.

**Verdict:** use Option A, a persistent, `copyOnDeath` attachment on the player. It is per player and per world by construction, survives death and dimension change, and can sync to the owner for the UI. Include the dimension in the key, because player data is per save, not per dimension.

## 6. Sketch of the recommended design

```
END_SERVER_TICK, every 20 ticks, for each ServerPlayer p:
  level = p.level(); pos = p.blockPosition()
  refs = level.structureManager().getAllStructuresAt(pos)   // cheap; usually empty
  for (structure, startLongs) in refs:
     for packed in startLongs:
        key = (level.dimension(), id(structure), packed)
        if key in p.visited: continue
        start = getStartForStructure(structure, level.getChunk(unpack(packed), STRUCTURE_STARTS))
        if start.isValid() && structureManager.structureHasPieceAt(pos, start):
           p.modifyAttached(VISITED, s -> s.with(key))
```

## Open points (not settled by the sources)

- Whether "inside a piece" is too strict for some modded structures with very sparse pieces. A tolerance option could fall back to `getBoundingBox()` (overall box, with the 12-block inflation for terrain-adapted structures).
- How long the player must stay inside before the mark is set. One sample at the 20-tick cadence is enough to fire. Debouncing is a product decision.
