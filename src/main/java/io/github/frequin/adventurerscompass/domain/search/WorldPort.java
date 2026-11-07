package io.github.frequin.adventurerscompass.domain.search;

import java.util.List;

import io.github.frequin.adventurerscompass.domain.BlockXZ;
import io.github.frequin.adventurerscompass.domain.ChunkCoord;

/**
 * What the search needs to know about a world. Implemented by the Minecraft
 * adapter from the chunk generator, and by fakes in tests.
 *
 * @param <P> the adapter's placement type. Placements are compared with
 *            {@code equals}: Structures sharing a placement are searched together.
 */
public interface WorldPort<P> {

	/** Placements that can generate a Structure in a dimension; empty if none. */
	List<P> placements(String dimension, String structureId);

	/**
	 * Candidate positions of a placement, produced lazily by increasing distance
	 * from {@code origin} (see {@link CandidateSource}).
	 */
	CandidateSource candidates(String dimension, P placement, BlockXZ origin);

	/**
	 * Whether an Instance of the Structure starts at the candidate chunk. May be
	 * expensive (it can generate the chunk up to structure starts).
	 */
	boolean isPresent(String dimension, String structureId, P placement, ChunkCoord chunk);

}
