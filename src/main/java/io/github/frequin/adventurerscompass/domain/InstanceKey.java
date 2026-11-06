package io.github.frequin.adventurerscompass.domain;

import java.util.Comparator;

/**
 * Identifies one Instance: the dimension it is in, its Structure and the chunk
 * its structure start sits in. Shared by Visited, Skipped and the Target.
 *
 * @param dimension   dimension id, e.g. {@code minecraft:overworld}
 * @param structureId Structure registry id, e.g. {@code minecraft:village_plains}
 * @param startChunk  chunk of the structure start
 */
public record InstanceKey(String dimension, String structureId, ChunkCoord startChunk) {

	/** A total order, for lists that must not change from one read to the next. */
	public static final Comparator<InstanceKey> ORDER = Comparator.comparing(InstanceKey::dimension)
			.thenComparing(InstanceKey::structureId)
			.thenComparingInt(key -> key.startChunk().x())
			.thenComparingInt(key -> key.startChunk().z());

}
