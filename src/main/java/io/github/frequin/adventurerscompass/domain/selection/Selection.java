package io.github.frequin.adventurerscompass.domain.selection;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import io.github.frequin.adventurerscompass.domain.selection.IgnoredElement.Reason;

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
	 * exploded: it is replaced by its other non-Hidden Structures, ticked one by one.
	 */
	public Selection untickStructure(String structureId, Catalogue catalogue) {
		Set<String> nextStructures = new TreeSet<>(structures);
		Set<String> nextGroups = new TreeSet<>(groups);
		catalogue.groupOf(structureId).filter(groups::contains).ifPresent(groupId -> {
			nextGroups.remove(groupId);
			catalogue.members(groupId).stream().filter(id -> !catalogue.entry(id).get().hidden()).forEach(nextStructures::add);
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

	/**
	 * "Select all": ticks what the tree lists, {@code listed} being the tree of
	 * {@code dimension} once filtered. A Group listed whole is ticked as its
	 * checkbox would; of a Group the filter narrowed, only the listed Structures
	 * are. Nothing already in the Selection goes out.
	 */
	public Selection tickAll(List<TreeEntry> listed, Catalogue catalogue, String dimension) {
		List<TreeEntry> wholeTree = catalogue.tree(dimension);
		Selection next = this;
		for (TreeEntry entry : listed) {
			next = switch (entry) {
				case TreeEntry.GroupEntry group when wholeTree.contains(group) -> next.tickGroup(group.groupId(), catalogue);
				case TreeEntry.GroupEntry group -> next.tickUncovered(group.structureIds(), catalogue);
				case TreeEntry.StructureEntry structure -> next.tickUncovered(List.of(structure.structureId()), catalogue);
			};
		}
		return next;
	}

	/** Ticks those Structures, leaving alone the ones a ticked Group already covers. */
	private Selection tickUncovered(List<String> structureIds, Catalogue catalogue) {
		Selection next = this;
		for (String structureId : structureIds) {
			if (structureState(structureId, catalogue) == StructureState.EMPTY) {
				next = next.tickStructure(structureId);
			}
		}
		return next;
	}

	public Selection clear() {
		return EMPTY;
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
			if (structures.contains(structureId) && catalogue.entry(structureId).get().listedIn(dimension)) {
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
			if (entry.listedIn(dimension) && (structures.contains(entry.structureId()) || groups.contains(entry.groupId()))) {
				searchable.add(entry.structureId());
			}
		}
		return searchable;
	}

	/**
	 * Whether "Search" is allowed in {@code dimension}: at least one element of
	 * the Selection generates there. Otherwise the search is refused and nothing is charged.
	 */
	public boolean canSearchIn(Catalogue catalogue, String dimension) {
		return !resolve(catalogue, dimension).isEmpty();
	}

	/**
	 * The elements of the Selection a search in {@code dimension} leaves out:
	 * Groups first, then Structures, each sorted by id. An element is ignored when
	 * nothing it stands for generates in the dimension, or when what it stands for
	 * is Hidden; it stays on the compass. Hidden wins over absent.
	 */
	public List<IgnoredElement> ignored(Catalogue catalogue, String dimension) {
		List<IgnoredElement> ignored = new ArrayList<>();
		for (String groupId : new TreeSet<>(groups)) {
			ignoredGroupReason(groupId, catalogue, dimension).ifPresent(reason -> ignored.add(new IgnoredElement(groupId, reason)));
		}
		for (String structureId : new TreeSet<>(structures)) {
			ignoredStructureReason(structureId, catalogue, dimension).ifPresent(reason -> ignored.add(new IgnoredElement(structureId, reason)));
		}
		return ignored;
	}

	private static Optional<Reason> ignoredStructureReason(String structureId, Catalogue catalogue, String dimension) {
		Optional<CatalogueEntry> entry = catalogue.entry(structureId);
		if (entry.isEmpty()) {
			return Optional.of(Reason.ABSENT_FROM_DIMENSION);
		}
		if (entry.get().hidden()) {
			return Optional.of(Reason.HIDDEN);
		}
		return entry.get().generatesIn(dimension) ? Optional.empty() : Optional.of(Reason.ABSENT_FROM_DIMENSION);
	}

	/**
	 * Not ignored when a non-Hidden member generates in the dimension. Otherwise
	 * HIDDEN when the members that generate there (or, if none does, all its
	 * members) are Hidden, else ABSENT_FROM_DIMENSION.
	 */
	private static Optional<Reason> ignoredGroupReason(String groupId, Catalogue catalogue, String dimension) {
		List<CatalogueEntry> members = catalogue.members(groupId).stream().map(id -> catalogue.entry(id).get()).toList();
		if (members.stream().anyMatch(entry -> entry.listedIn(dimension))) {
			return Optional.empty();
		}
		List<CatalogueEntry> here = members.stream().filter(entry -> entry.generatesIn(dimension)).toList();
		List<CatalogueEntry> considered = here.isEmpty() ? members : here;
		return Optional.of(!considered.isEmpty() && considered.stream().allMatch(CatalogueEntry::hidden) ? Reason.HIDDEN : Reason.ABSENT_FROM_DIMENSION);
	}

}
