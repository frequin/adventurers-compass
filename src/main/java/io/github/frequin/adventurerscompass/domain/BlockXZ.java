package io.github.frequin.adventurerscompass.domain;

/** A block column: horizontal block coordinates, height ignored. */
public record BlockXZ(int x, int z) {

	/** Squared horizontal distance, exact. */
	public long distanceSq(BlockXZ other) {
		long dx = (long) x - other.x;
		long dz = (long) z - other.z;
		return dx * dx + dz * dz;
	}

}
