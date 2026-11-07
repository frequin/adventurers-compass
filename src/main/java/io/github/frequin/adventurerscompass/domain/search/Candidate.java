package io.github.frequin.adventurerscompass.domain.search;

import io.github.frequin.adventurerscompass.domain.BlockXZ;
import io.github.frequin.adventurerscompass.domain.ChunkCoord;

/**
 * A chunk where a placement may start an Instance.
 *
 * @param chunk     the candidate start chunk
 * @param locatePos the block the compass points at if an Instance starts there
 */
public record Candidate(ChunkCoord chunk, BlockXZ locatePos) {
}
