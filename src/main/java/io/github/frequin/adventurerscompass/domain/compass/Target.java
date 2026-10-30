package io.github.frequin.adventurerscompass.domain.compass;

import io.github.frequin.adventurerscompass.domain.BlockXZ;
import io.github.frequin.adventurerscompass.domain.InstanceKey;

/**
 * The Instance the compass points at.
 *
 * @param instance its key, which holds its dimension and Structure
 * @param pos      the block column the needle points at
 */
public record Target(InstanceKey instance, BlockXZ pos) {

	public String dimension() {
		return instance.dimension();
	}

	public String structureId() {
		return instance.structureId();
	}

}
