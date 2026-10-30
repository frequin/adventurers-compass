package io.github.frequin.adventurerscompass.domain.search;

import java.util.List;

/**
 * Lazily produces the candidates of one placement, batch by batch (typically
 * one ring around the origin per batch).
 *
 * <p>Contract: every candidate a later {@link #nextBatch()} returns has a
 * horizontal distance to the origin of at least {@link #nextLowerBound()}.
 * The bound never decreases. Candidates inside a batch need not be sorted.
 */
public interface CandidateSource {

	/** Lower bound, in blocks, on the distance of any candidate not yet produced; +∞ once exhausted. */
	double nextLowerBound();

	/** The next batch; empty once exhausted. */
	List<Candidate> nextBatch();

}
