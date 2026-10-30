package io.github.frequin.adventurerscompass.domain.search;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import io.github.frequin.adventurerscompass.domain.BlockXZ;
import io.github.frequin.adventurerscompass.domain.ChunkCoord;

/**
 * Candidates of a random spread placement: the world is cut into square cells
 * of {@code spacing} chunks, each holding at most one potential start chunk.
 * Produces one square ring of cells around the origin's cell per batch.
 */
public final class RandomSpreadCandidates implements CandidateSource {

	/** The potential start chunk of a cell, given in cell coordinates. */
	@FunctionalInterface
	public interface PotentialChunk {
		ChunkCoord in(int cellX, int cellZ);
	}

	private final int spacing;
	private final int originCellX;
	private final int originCellZ;
	private final PotentialChunk potentialChunk;
	private final Function<ChunkCoord, BlockXZ> locatePos;
	private int ring;

	public RandomSpreadCandidates(BlockXZ origin, int spacing, PotentialChunk potentialChunk, Function<ChunkCoord, BlockXZ> locatePos) {
		if (spacing <= 0) {
			throw new IllegalArgumentException("spacing must be positive: " + spacing);
		}
		ChunkCoord originChunk = ChunkCoord.containing(origin);
		this.spacing = spacing;
		this.originCellX = Math.floorDiv(originChunk.x(), spacing);
		this.originCellZ = Math.floorDiv(originChunk.z(), spacing);
		this.potentialChunk = potentialChunk;
		this.locatePos = locatePos;
	}

	@Override
	public double nextLowerBound() {
		// A cell of ring r is at least (r - 1) * spacing chunks away from the origin's chunk.
		// One chunk of margin covers a locate position offset inside its chunk.
		return Math.max(0, ((double) (ring - 1) * spacing - 1) * 16);
	}

	@Override
	public List<Candidate> nextBatch() {
		List<Candidate> batch = new ArrayList<>(Math.max(1, 8 * ring));
		for (int dx = -ring; dx <= ring; dx++) {
			for (int dz = -ring; dz <= ring; dz++) {
				if (Math.abs(dx) == ring || Math.abs(dz) == ring) {
					ChunkCoord chunk = potentialChunk.in(originCellX + dx, originCellZ + dz);
					batch.add(new Candidate(chunk, locatePos.apply(chunk)));
				}
			}
		}
		ring++;
		return batch;
	}

}
