package io.github.frequin.adventurerscompass.domain.search;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import io.github.frequin.adventurerscompass.domain.BlockXZ;
import io.github.frequin.adventurerscompass.domain.ChunkCoord;

/**
 * Fallback for placements without a known layout: every chunk is a candidate.
 * Produces one square ring of chunks around the origin's chunk per batch.
 */
public final class ChunkSpiralCandidates implements CandidateSource {

	private final ChunkCoord originChunk;
	private final Function<ChunkCoord, BlockXZ> locatePos;
	private int ring;

	public ChunkSpiralCandidates(BlockXZ origin, Function<ChunkCoord, BlockXZ> locatePos) {
		this.originChunk = ChunkCoord.containing(origin);
		this.locatePos = locatePos;
	}

	@Override
	public double nextLowerBound() {
		// Ring r is at least (r - 1) chunks away from the origin's chunk; one more chunk of margin for the locate offset.
		return Math.max(0, (ring - 2) * 16.0);
	}

	@Override
	public List<Candidate> nextBatch() {
		List<Candidate> batch = new ArrayList<>(Math.max(1, 8 * ring));
		for (int dx = -ring; dx <= ring; dx++) {
			for (int dz = -ring; dz <= ring; dz++) {
				if (Math.abs(dx) == ring || Math.abs(dz) == ring) {
					ChunkCoord chunk = new ChunkCoord(originChunk.x() + dx, originChunk.z() + dz);
					batch.add(new Candidate(chunk, locatePos.apply(chunk)));
				}
			}
		}
		ring++;
		return batch;
	}

}
