package io.github.frequin.adventurerscompass.domain.selection;

/** Whether a Selection element counts in the Current dimension. */
public enum ChipState {
	/** Generated in the Current dimension: the search uses it. */
	ACTIVE,
	/** Not generated in the Current dimension (or no longer listed): greyed, ignored by the search, kept on the compass. */
	ABSENT_FROM_DIMENSION,
	/** Hidden by a datapack tag or the server config: greyed, ignored by the search, kept on the compass. */
	HIDDEN
}
