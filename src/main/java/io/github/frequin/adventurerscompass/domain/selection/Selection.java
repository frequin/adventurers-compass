package io.github.frequin.adventurerscompass.domain.selection;

import java.util.Set;
import java.util.TreeSet;

/**
 * The Structures and Groups ticked on a compass. Immutable: every operation
 * returns a new Selection.
 *
 * @param structures ids of the Structures ticked one by one
 * @param groups     ids of the Groups ticked as a whole
 */
public record Selection(Set<String> structures, Set<String> groups) {

	public static final Selection EMPTY = new Selection(Set.of(), Set.of());

	public Selection {
		structures = Set.copyOf(structures);
		groups = Set.copyOf(groups);
	}

	public Selection tickStructure(String structureId) {
		Set<String> next = new TreeSet<>(structures);
		next.add(structureId);
		return new Selection(next, groups);
	}

	/**
	 * Unticks a Structure. If it was ticked through its Group, the Group is
	 * exploded: it is replaced by its other Structures, ticked one by one.
	 */
	public Selection untickStructure(String structureId, Catalogue catalogue) {
		Set<String> nextStructures = new TreeSet<>(structures);
		Set<String> nextGroups = new TreeSet<>(groups);
		catalogue.groupOf(structureId).filter(groups::contains).ifPresent(groupId -> {
			nextGroups.remove(groupId);
			nextStructures.addAll(catalogue.members(groupId));
		});
		nextStructures.remove(structureId);
		return new Selection(nextStructures, nextGroups);
	}

	/** Ticks a whole Group; its Structures ticked one by one are absorbed into it. */
	public Selection tickGroup(String groupId, Catalogue catalogue) {
		Set<String> nextStructures = new TreeSet<>(structures);
		catalogue.members(groupId).forEach(nextStructures::remove);
		Set<String> nextGroups = new TreeSet<>(groups);
		nextGroups.add(groupId);
		return new Selection(nextStructures, nextGroups);
	}

	public Selection untickGroup(String groupId) {
		Set<String> next = new TreeSet<>(groups);
		next.remove(groupId);
		return new Selection(structures, next);
	}

	/** A click on a Structure's checkbox: ticks it, or unticks it (exploding its Group if needed). */
	public Selection toggleStructure(String structureId, Catalogue catalogue) {
		return structureState(structureId, catalogue) == StructureState.EMPTY ? tickStructure(structureId) : untickStructure(structureId, catalogue);
	}

	/** A click on a Group's checkbox: an empty or partial Group becomes ticked, a ticked Group empty. */
	public Selection toggleGroup(String groupId, Catalogue catalogue, String dimension) {
		return groupState(groupId, catalogue, dimension) == GroupState.TICKED ? untickGroup(groupId) : tickGroup(groupId, catalogue);
	}

	public GroupState groupState(String groupId, Catalogue catalogue, String dimension) {
		if (groups.contains(groupId)) {
			return GroupState.TICKED;
		}
		for (String structureId : catalogue.members(groupId)) {
			if (structures.contains(structureId) && catalogue.entry(structureId).get().generatesIn(dimension)) {
				return GroupState.PARTIAL;
			}
		}
		return GroupState.EMPTY;
	}

	public StructureState structureState(String structureId, Catalogue catalogue) {
		if (structures.contains(structureId)) {
			return StructureState.TICKED;
		}
		if (catalogue.groupOf(structureId).filter(groups::contains).isPresent()) {
			return StructureState.TICKED_BY_GROUP;
		}
		return StructureState.EMPTY;
	}

	/** The Structures a search in {@code dimension} looks for. */
	public Set<String> resolve(Catalogue catalogue, String dimension) {
		Set<String> searchable = new TreeSet<>();
		for (CatalogueEntry entry : catalogue.entries()) {
			if (entry.generatesIn(dimension) && (structures.contains(entry.structureId()) || groups.contains(entry.groupId()))) {
				searchable.add(entry.structureId());
			}
		}
		return searchable;
	}

}
