package io.github.frequin.adventurerscompass.domain.selection;

import java.util.List;

/** One top-level row of the Selection tree for a dimension. */
public sealed interface TreeEntry {

	/** A Group with two or more Structures in the dimension, listed in catalogue order. */
	record GroupEntry(String groupId, List<String> structureIds) implements TreeEntry {

		public GroupEntry {
			structureIds = List.copyOf(structureIds);
		}

	}

	/** A Structure shown on its own, because it is the only one of its Group in the dimension. */
	record StructureEntry(String structureId) implements TreeEntry {
	}

}
