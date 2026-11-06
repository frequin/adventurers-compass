package io.github.frequin.adventurerscompass.domain.selection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * The search box above the Selection tree. Plain text keeps the Structures
 * whose name or id contains it (or every Structure of a Group whose name or id
 * contains it); {@code @text} keeps the Structures whose source (mod name or
 * namespace) contains it. Case is ignored; a blank query keeps everything.
 */
public final class TreeFilter {

	public static final TreeFilter NONE = new TreeFilter("", false);

	private final String needle;
	private final boolean bySource;

	private TreeFilter(String needle, boolean bySource) {
		this.needle = needle;
		this.bySource = bySource;
	}

	public static TreeFilter of(String query) {
		String trimmed = query.strip().toLowerCase(Locale.ROOT);
		if (trimmed.startsWith("@")) {
			return new TreeFilter(trimmed.substring(1).strip(), true);
		}
		return new TreeFilter(trimmed, false);
	}

	/** True when the filter keeps the whole tree. */
	public boolean isNone() {
		return needle.isEmpty();
	}

	/**
	 * Narrows a tree. {@code displayName} gives the name shown for a Structure
	 * or Group id. Groups keep only their matching Structures and disappear when
	 * none match.
	 */
	public List<TreeEntry> apply(List<TreeEntry> tree, Catalogue catalogue, Function<String, String> displayName) {
		if (isNone()) {
			return tree;
		}
		List<TreeEntry> kept = new ArrayList<>();
		for (TreeEntry entry : tree) {
			switch (entry) {
				case TreeEntry.GroupEntry group -> {
					if (!bySource && matchesName(group.groupId(), displayName)) {
						kept.add(group);
					} else {
						List<String> members = group.structureIds().stream().filter(id -> matches(id, catalogue, displayName)).toList();
						if (!members.isEmpty()) {
							kept.add(new TreeEntry.GroupEntry(group.groupId(), members));
						}
					}
				}
				case TreeEntry.StructureEntry structure -> {
					if (matches(structure.structureId(), catalogue, displayName)) {
						kept.add(structure);
					}
				}
			}
		}
		return kept;
	}

	/** Whether the filter keeps this Structure, its Group aside. Also filters the rows of the Visited and Skipped tabs. */
	public boolean matches(String structureId, Catalogue catalogue, Function<String, String> displayName) {
		if (bySource) {
			String source = catalogue.entry(structureId).map(CatalogueEntry::source).orElse("");
			return contains(source) || contains(namespace(structureId));
		}
		return matchesName(structureId, displayName);
	}

	private boolean matchesName(String id, Function<String, String> displayName) {
		return contains(displayName.apply(id)) || contains(id);
	}

	private boolean contains(String text) {
		return text.toLowerCase(Locale.ROOT).contains(needle);
	}

	private static String namespace(String id) {
		int colon = id.indexOf(':');
		return colon < 0 ? "" : id.substring(0, colon);
	}

}
