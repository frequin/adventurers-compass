package io.github.frequin.adventurerscompass.domain;

/**
 * Identifies one Instance: the dimension it is in, its Structure and the chunk
 * its structure start sits in. Shared by Visited, Skipped and the Target.
 *
 * @param dimension   dimension id, e.g. {@code minecraft:overworld}
 * @param structureId Structure registry id, e.g. {@code minecraft:village_plains}
 * @param startChunk  chunk of the structure start
 */
public record InstanceKey(String dimension, String structureId, ChunkCoord startChunk) {
}
