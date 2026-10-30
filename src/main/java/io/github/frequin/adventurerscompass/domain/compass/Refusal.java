package io.github.frequin.adventurerscompass.domain.compass;

/** Why "Search" or "Pass" is refused. */
public enum Refusal {

	/** Nothing in the Selection generates in the Current dimension. */
	NOTHING_SEARCHABLE,

	/** "Pass" without a Target. */
	NO_TARGET,

	/** The compass is worn out: it must be repaired before it searches again. */
	BROKEN,

	/** The Current dimension is listed in {@code disabledDimensions}. */
	DISABLED_DIMENSION,

	/** "Teleport": {@code allowTeleport} is off, or the player is neither op nor creative. */
	TELEPORT_NOT_ALLOWED,

	/** "Teleport" to a Target outside the Current dimension. */
	TARGET_IN_OTHER_DIMENSION

}
