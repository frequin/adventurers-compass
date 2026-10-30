package io.github.frequin.adventurerscompass.domain.selection;

/** How a Structure's checkbox reads in the tree. */
public enum StructureState {
	/** Ticked one by one. */
	TICKED,
	/** Not ticked itself, but its Group is. */
	TICKED_BY_GROUP,
	EMPTY
}
