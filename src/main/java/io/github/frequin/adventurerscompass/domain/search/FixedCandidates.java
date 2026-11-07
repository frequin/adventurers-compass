package io.github.frequin.adventurerscompass.domain.search;

import java.util.List;
import java.util.function.Function;

import io.github.frequin.adventurerscompass.domain.BlockXZ;
import io.github.frequin.adventurerscompass.domain.ChunkCoord;

/**
 * Candidates known in advance, such as the precomputed positions of a
 * concentric rings placement (strongholds). Produced in a single batch.
 */
public final class FixedCandidates implements CandidateSource {

	private final List<Candidate> candidates;
	private boolean produced;

	public FixedCandidates(BlockXZ origin, List<ChunkCoord> chunks, Function<ChunkCoord, BlockXZ> locatePos) {
		this.candidates = chunks.stream().map(chunk -> new Candidate(chunk, locatePos.apply(chunk))).toList();
	}

	@Override
	public double nextLowerBound() {
		return produced || candidates.isEmpty() ? Double.POSITIVE_INFINITY : 0;
	}

	@Override
	public List<Candidate> nextBatch() {
		if (produced) {
			return List.of();
		}
		produced = true;
		return candidates;
	}

}
