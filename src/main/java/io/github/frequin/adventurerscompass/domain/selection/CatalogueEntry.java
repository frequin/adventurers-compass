package io.github.frequin.adventurerscompass.domain.selection;

import java.util.Set;

/**
 * One Structure, as the server describes it to the client.
 *
 * @param structureId Structure registry id, e.g. {@code minecraft:village_plains}
 * @param groupId     id of its Group, e.g. {@code minecraft:villages}
 * @param dimensions  ids of the dimensions that can generate it
 * @param source      display name of the mod or datapack that adds it
 * @param hidden      whether it is Hidden (see {@link HiddenRule}): kept in the
 *                    catalogue so a Selection that holds it can grey its chip,
 *                    but neither listed in the tree nor searched
 */
public record CatalogueEntry(String structureId, String groupId, Set<String> dimensions, String source, boolean hidden) {

	public CatalogueEntry {
		dimensions = Set.copyOf(dimensions);
	}

	/** A listable (not Hidden) Structure. */
	public CatalogueEntry(String structureId, String groupId, Set<String> dimensions, String source) {
		this(structureId, groupId, dimensions, source, false);
	}

	public boolean generatesIn(String dimension) {
		return dimensions.contains(dimension);
	}

	/** Listed in the tree of {@code dimension} and searched there. */
	public boolean listedIn(String dimension) {
		return !hidden && generatesIn(dimension);
	}

}
