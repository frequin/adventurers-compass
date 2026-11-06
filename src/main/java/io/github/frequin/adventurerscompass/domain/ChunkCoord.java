package io.github.frequin.adventurerscompass.domain;

/** A chunk column, in chunk coordinates (16 blocks per chunk). */
public record ChunkCoord(int x, int z) {

	/** The chunk holding a block. */
	public static ChunkCoord containing(BlockXZ block) {
		return new ChunkCoord(block.x() >> 4, block.z() >> 4);
	}

	/** The north-west block of this chunk. */
	public BlockXZ minBlock() {
		return new BlockXZ(x << 4, z << 4);
	}

	/** The middle block of this chunk. */
	public BlockXZ center() {
		return new BlockXZ((x << 4) + 8, (z << 4) + 8);
	}

}
