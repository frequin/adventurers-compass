package io.github.frequin.adventurerscompass.domain.selection;

/** How a Group's checkbox reads in the tree. */
public enum GroupState {
	/** The Group is ticked as a whole. */
	TICKED,
	/** Some of its Structures of the Current dimension are ticked one by one. */
	PARTIAL,
	EMPTY
}
