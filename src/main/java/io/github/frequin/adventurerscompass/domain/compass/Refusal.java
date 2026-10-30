package io.github.frequin.adventurerscompass.domain.compass;

/** Why "Search" or "Pass" is refused. */
public enum Refusal {

	/** Nothing in the Selection generates in the Current dimension. */
	NOTHING_SEARCHABLE,

	/** "Pass" without a Target. */
	NO_TARGET,

	/** The compass is worn out: it must be repaired before it searches again. */
	BROKEN

}
