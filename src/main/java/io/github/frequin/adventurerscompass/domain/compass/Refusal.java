package io.github.frequin.adventurerscompass.domain.compass;

/** Why "Search" or "Pass" is refused. */
public enum Refusal {

	/** Nothing in the Selection generates in the Current dimension. */
	NOTHING_SEARCHABLE,

	/** "Pass" without a Target. */
	NO_TARGET,

	/** The player has fewer levels than the highest cost of the searchable Structures (never in creative). */
	NOT_ENOUGH_XP

}
