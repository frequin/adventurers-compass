package io.github.frequin.adventurerscompass.domain.compass;

/** What the compass shows (HUD line 1, needle), read from its state and the Current dimension. */
public enum Status {

	/** No Target, no search, nothing to report. */
	IDLE,

	/** A search runs: the needle spins and the HUD reads "Searching…". */
	SEARCHING,

	/** The Target is in the Current dimension: the needle points at it. */
	TARGET,

	/**
	 * The Target in the Current dimension is Visited: the needle still points
	 * at it and the HUD reads "Visited — Search for the next one".
	 */
	TARGET_VISITED,

	/** The Target is in another dimension: the needle spins until the player returns. */
	OTHER_DIMENSION,

	/** The last search found nothing: the HUD explains why; a kept Target is still pointed at. */
	NOTHING_FOUND

}
