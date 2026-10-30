package io.github.frequin.adventurerscompass.domain.selection;

import java.util.Set;

/**
 * One listable Structure, as the server describes it to the client.
 *
 * @param structureId Structure registry id, e.g. {@code minecraft:village_plains}
 * @param groupId     id of its Group, e.g. {@code minecraft:villages}
 * @param dimensions  ids of the dimensions that can generate it
 * @param source      display name of the mod or datapack that adds it
 */
public record CatalogueEntry(String structureId, String groupId, Set<String> dimensions, String source) {

	public CatalogueEntry {
		dimensions = Set.copyOf(dimensions);
	}

	public boolean generatesIn(String dimension) {
		return dimensions.contains(dimension);
	}

}
