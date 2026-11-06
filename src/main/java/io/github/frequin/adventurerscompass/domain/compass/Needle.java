package io.github.frequin.adventurerscompass.domain.compass;

/** What the needle of one compass does, read from its state and the Current dimension. */
public enum Needle {

	/** Searching, or the Target is in another dimension. */
	SPIN,

	/** Points at the Target in the Current dimension. */
	TARGET,

	/** No Target: points at the world spawn, like a vanilla compass. */
	WORLD_SPAWN

}
