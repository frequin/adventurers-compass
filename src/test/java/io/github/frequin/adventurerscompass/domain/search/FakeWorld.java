package io.github.frequin.adventurerscompass.domain.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.frequin.adventurerscompass.domain.BlockXZ;
import io.github.frequin.adventurerscompass.domain.ChunkCoord;

/**
 * A world with Instances put at known chunks. Each placement spreads its
 * Instances on a grid of cells, at most one per cell, like a vanilla random
 * spread placement. Presence tests advance a fake clock by a fixed cost.
 */
public final class FakeWorld implements WorldPort<FakeWorld.Placement> {

	/** How a placement spreads its candidates. */
	public enum Kind {
		/** One candidate per square cell of {@code spacing} chunks. */
		RANDOM_SPREAD,
		/** A fixed list of candidates: the chunks of its Instances plus decoys. */
		CONCENTRIC_RINGS,
		/** Every chunk is a candidate. */
		EVERY_CHUNK
	}

	public record Placement(String name, Kind kind, int spacing) {

		public Placement(String name, int spacing) {
			this(name, Kind.RANDOM_SPREAD, spacing);
		}

		static Placement rings(String name) {
			return new Placement(name, Kind.CONCENTRIC_RINGS, 1);
		}

		static Placement everyChunk(String name) {
			return new Placement(name, Kind.EVERY_CHUNK, 1);
		}

	}

	private record Located(String structureId, ChunkCoord chunk) {
	}

	private final Map<String, List<Placement>> placementsByStructure = new HashMap<>();
	private final Map<Placement, Set<Located>> instances = new HashMap<>();
	private final Map<Placement, List<ChunkCoord>> decoys = new HashMap<>();
	private final Set<ChunkCoord> testedInstanceChunks = new HashSet<>();
	private final FakeClock clock;
	private final long presenceTestCostNanos;

	FakeWorld(FakeClock clock, long presenceTestCostNanos) {
		this.clock = clock;
		this.presenceTestCostNanos = presenceTestCostNanos;
	}

	public FakeWorld() {
		this(new FakeClock(), 0);
	}

	FakeWorld placement(String structureId, Placement placement) {
		placementsByStructure.computeIfAbsent(structureId, id -> new ArrayList<>()).add(placement);
		instances.computeIfAbsent(placement, p -> new HashSet<>());
		return this;
	}

	/** Puts an Instance of a Structure at a chunk; the chunk is its start chunk. */
	public FakeWorld instance(String structureId, Placement placement, int chunkX, int chunkZ) {
		placement(structureId, placement);
		ChunkCoord chunk = new ChunkCoord(chunkX, chunkZ);
		for (Located other : placement.kind() == Kind.RANDOM_SPREAD ? instances.get(placement) : Set.<Located>of()) {
			if (cellOf(placement, other.chunk()).equals(cellOf(placement, chunk)) && !other.chunk().equals(chunk)) {
				throw new IllegalArgumentException("one Instance per cell: " + chunk + " and " + other.chunk());
			}
		}
		instances.get(placement).add(new Located(structureId, chunk));
		return this;
	}

	/** Adds a candidate chunk without an Instance to a concentric rings placement. */
	FakeWorld decoy(Placement placement, int chunkX, int chunkZ) {
		decoys.computeIfAbsent(placement, p -> new ArrayList<>()).add(new ChunkCoord(chunkX, chunkZ));
		return this;
	}

	@Override
	public List<Placement> placements(String dimension, String structureId) {
		return placementsByStructure.getOrDefault(structureId, List.of());
	}

	@Override
	public CandidateSource candidates(String dimension, Placement placement, BlockXZ origin) {
		if (placement.kind() == Kind.CONCENTRIC_RINGS) {
			List<ChunkCoord> chunks = new ArrayList<>(instances.get(placement).stream().map(Located::chunk).toList());
			chunks.addAll(decoys.getOrDefault(placement, List.of()));
			return new FixedCandidates(origin, chunks, ChunkCoord::minBlock);
		}
		if (placement.kind() == Kind.EVERY_CHUNK) {
			return new ChunkSpiralCandidates(origin, ChunkCoord::minBlock);
		}
		return new RandomSpreadCandidates(origin, placement.spacing(), (cellX, cellZ) -> potentialChunk(placement, cellX, cellZ), ChunkCoord::minBlock);
	}

	@Override
	public boolean isPresent(String dimension, String structureId, Placement placement, ChunkCoord chunk) {
		clock.advance(presenceTestCostNanos);
		if (instances.get(placement).stream().anyMatch(located -> located.chunk().equals(chunk))) {
			testedInstanceChunks.add(chunk);
		}
		return instances.get(placement).contains(new Located(structureId, chunk));
	}

	/** Chunks holding an Instance whose presence was tested: in a real world, they would have been generated. */
	Set<ChunkCoord> testedInstanceChunks() {
		return Set.copyOf(testedInstanceChunks);
	}

	private ChunkCoord potentialChunk(Placement placement, int cellX, int cellZ) {
		ChunkCoord cell = new ChunkCoord(cellX, cellZ);
		for (Located located : instances.get(placement)) {
			if (cellOf(placement, located.chunk()).equals(cell)) {
				return located.chunk();
			}
		}
		return new ChunkCoord(cellX * placement.spacing(), cellZ * placement.spacing());
	}

	private static ChunkCoord cellOf(Placement placement, ChunkCoord chunk) {
		return new ChunkCoord(Math.floorDiv(chunk.x(), placement.spacing()), Math.floorDiv(chunk.z(), placement.spacing()));
	}

}
