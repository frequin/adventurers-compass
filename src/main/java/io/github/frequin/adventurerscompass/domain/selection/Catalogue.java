package io.github.frequin.adventurerscompass.domain.selection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Snapshot of the Structures, built by the server and sent to the client. Each
 * entry carries its Group, the dimensions that generate it and whether it is
 * Hidden. Hidden entries are kept (a Selection may still hold them) but the
 * tree and the search skip them.
 */
public final class Catalogue {

	public static final Catalogue EMPTY = new Catalogue(List.of());

	private final List<CatalogueEntry> entries;
	private final Map<String, CatalogueEntry> byStructure = new LinkedHashMap<>();

	public Catalogue(List<CatalogueEntry> entries) {
		this.entries = List.copyOf(entries);
		for (CatalogueEntry entry : this.entries) {
			byStructure.put(entry.structureId(), entry);
		}
	}

	public List<CatalogueEntry> entries() {
		return entries;
	}

	public Optional<CatalogueEntry> entry(String structureId) {
		return Optional.ofNullable(byStructure.get(structureId));
	}

	/** The Group of a Structure, empty if the Structure is not listed. */
	public Optional<String> groupOf(String structureId) {
		return entry(structureId).map(CatalogueEntry::groupId);
	}

	/** The Selection tree of a dimension: its Groups, in catalogue order, with only their Structures generated there. */
	public List<TreeEntry> tree(String dimension) {
		Map<String, List<String>> groups = new LinkedHashMap<>();
		for (CatalogueEntry entry : entries) {
			if (entry.listedIn(dimension)) {
				groups.computeIfAbsent(entry.groupId(), id -> new ArrayList<>()).add(entry.structureId());
			}
		}
		List<TreeEntry> tree = new ArrayList<>();
		groups.forEach((groupId, structureIds) -> tree.add(structureIds.size() == 1 ? new TreeEntry.StructureEntry(structureIds.get(0)) : new TreeEntry.GroupEntry(groupId, structureIds)));
		return tree;
	}

	/** How many non-Hidden Structures the tree of {@code dimension} leaves out because they generate only elsewhere. */
	public int countOutside(String dimension) {
		return (int) entries.stream().filter(entry -> !entry.hidden() && !entry.generatesIn(dimension)).count();
	}

	/** Every listed Structure of a Group, whatever dimension generates it. */
	public List<String> members(String groupId) {
		List<String> members = new ArrayList<>();
		for (CatalogueEntry entry : entries) {
			if (entry.groupId().equals(groupId)) {
				members.add(entry.structureId());
			}
		}
		return members;
	}

}
