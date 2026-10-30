package io.github.frequin.adventurerscompass.domain.compass;

/** Why "Search" or "Pass" is refused. */
public enum Refusal {

	/** Nothing in the Selection generates in the Current dimension. */
	NOTHING_SEARCHABLE,

	/** "Pass" without a Target. */
	NO_TARGET,

	/** The player has fewer levels than the highest cost of the searchable Structures (never in creative). */
	NOT_ENOUGH_XP,

	/** The Current dimension is listed in {@code disabledDimensions}. */
	DISABLED_DIMENSION,

	/** "Teleport": {@code allowTeleport} is off, or the player is neither op nor creative. */
	TELEPORT_NOT_ALLOWED,

	/** "Teleport" to a Target outside the Current dimension. */
	TARGET_IN_OTHER_DIMENSION

}
