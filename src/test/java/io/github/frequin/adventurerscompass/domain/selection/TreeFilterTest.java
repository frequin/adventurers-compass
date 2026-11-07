package io.github.frequin.adventurerscompass.domain.selection;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

class TreeFilterTest {

	private static final String OVERWORLD = "minecraft:overworld";
	private static final String NETHER = "minecraft:the_nether";
	private static final String END = "minecraft:the_end";

	private static final String VILLAGES = "minecraft:villages";
	private static final String PLAINS = "minecraft:village_plains";
	private static final String DESERT = "minecraft:village_desert";
	private static final String OUTPOST = "minecraft:pillager_outpost";
	private static final String FORTRESS = "minecraft:fortress";
	private static final String END_CITY = "minecraft:end_city";

	private static final String TOWERS = "towers:towers";
	private static final String ICE_TOWER = "towers:ice_tower";
	private static final String DESERT_TOWER = "towers:desert_tower";

	private static final Catalogue CATALOGUE = new Catalogue(List.of(
			new CatalogueEntry(PLAINS, VILLAGES, Set.of(OVERWORLD), "Minecraft"),
			new CatalogueEntry(DESERT, VILLAGES, Set.of(OVERWORLD), "Minecraft"),
			new CatalogueEntry(OUTPOST, "minecraft:pillager_outposts", Set.of(OVERWORLD), "Minecraft"),
			new CatalogueEntry(FORTRESS, "minecraft:nether_complexes", Set.of(NETHER), "Minecraft"),
			new CatalogueEntry(END_CITY, "minecraft:end_cities", Set.of(END), "Minecraft"),
			new CatalogueEntry(ICE_TOWER, TOWERS, Set.of(OVERWORLD), "Towers Galore"),
			new CatalogueEntry(DESERT_TOWER, TOWERS, Set.of(OVERWORLD), "Towers Galore")));

	private static final Map<String, String> NAMES = Map.of(
			VILLAGES, "Villages", PLAINS, "Plains Village", DESERT, "Desert Village", OUTPOST, "Pillager Outpost",
			TOWERS, "Towers", ICE_TOWER, "Ice Tower", DESERT_TOWER, "Sandy Spire");
	private static final TreeNames TREE_NAMES = names(id -> NAMES.getOrDefault(id, id), id -> NAMES.getOrDefault(id, id));

	private static TreeNames names(Function<String, String> structures, Function<String, String> groups) {
		return new TreeNames() {

			@Override
			public String structure(String structureId) {
				return structures.apply(structureId);
			}

			@Override
			public String group(String groupId) {
				return groups.apply(groupId);
			}

		};
	}

	private static List<TreeEntry> filter(String query) {
		return TreeFilter.of(query).apply(CATALOGUE.tree(OVERWORLD), CATALOGUE, TREE_NAMES);
	}

	@Test
	void aGroupMatchesByItsGroupNameNotAStructureNameOfTheSameId() {
		TreeNames names = names(id -> "Unrelated", id -> id.equals(VILLAGES) ? "Hameaux" : "Autre");

		List<TreeEntry> kept = TreeFilter.of("hameaux").apply(CATALOGUE.tree(OVERWORLD), CATALOGUE, names);

		assertEquals(List.of(new TreeEntry.GroupEntry(VILLAGES, List.of(PLAINS, DESERT))), kept);
	}

	@Test
	void structuresOfOtherDimensionsAreCounted() {
		assertEquals(2, CATALOGUE.countOutside(OVERWORLD));
		assertEquals(6, CATALOGUE.countOutside(NETHER));
	}

	@Test
	void aBlankQueryKeepsTheWholeTree() {
		assertEquals(CATALOGUE.tree(OVERWORLD), filter("  "));
	}

	@Test
	void textKeepsOnlyTheStructuresWhoseNameMatchesUnderTheirGroup() {
		assertEquals(List.of(
				new TreeEntry.GroupEntry(VILLAGES, List.of(DESERT))), filter("desert v"));
	}

	@Test
	void textMatchesStructureIdsToo() {
		assertEquals(List.of(new TreeEntry.GroupEntry(TOWERS, List.of(DESERT_TOWER))), filter("desert_tower"));
	}

	@Test
	void textIgnoresCase() {
		assertEquals(List.of(new TreeEntry.StructureEntry(OUTPOST)), filter("PILLAGER"));
	}

	@Test
	void aGroupWhoseNameMatchesKeepsAllItsStructures() {
		assertEquals(List.of(new TreeEntry.GroupEntry(VILLAGES, List.of(PLAINS, DESERT))), filter("villages"));
	}

	@Test
	void atModKeepsOnlyThatSourcesStructures() {
		assertEquals(List.of(new TreeEntry.GroupEntry(TOWERS, List.of(ICE_TOWER, DESERT_TOWER))), filter("@galore"));
	}

	@Test
	void atModMatchesTheNamespaceToo() {
		assertEquals(List.of(new TreeEntry.GroupEntry(TOWERS, List.of(ICE_TOWER, DESERT_TOWER))), filter("@towers"));
	}

	@Test
	void atModDoesNotMatchNames() {
		assertEquals(List.of(), filter("@desert"));
	}

	@Test
	void nothingMatchingGivesAnEmptyTree() {
		assertEquals(List.of(), filter("bastion"));
	}

}
