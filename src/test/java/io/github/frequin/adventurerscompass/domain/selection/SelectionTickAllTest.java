package io.github.frequin.adventurerscompass.domain.selection;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class SelectionTickAllTest {

	private static final String OVERWORLD = "minecraft:overworld";
	private static final String NETHER = "minecraft:the_nether";

	private static final String VILLAGES = "minecraft:villages";
	private static final String PLAINS = "minecraft:village_plains";
	private static final String DESERT = "minecraft:village_desert";

	private static final String OUTPOSTS = "minecraft:pillager_outposts";
	private static final String OUTPOST = "minecraft:pillager_outpost";

	private static final String FORTRESSES = "minecraft:nether_complexes";
	private static final String FORTRESS = "minecraft:fortress";
	private static final String BASTION = "minecraft:bastion_remnant";

	private static final String PORTALS = "minecraft:ruined_portals";
	private static final String PORTAL = "minecraft:ruined_portal";
	private static final String PORTAL_DESERT = "minecraft:ruined_portal_desert";
	private static final String PORTAL_NETHER = "minecraft:ruined_portal_nether";

	private static final Catalogue CATALOGUE = new Catalogue(List.of(
			new CatalogueEntry(PLAINS, VILLAGES, Set.of(OVERWORLD), "Minecraft"),
			new CatalogueEntry(DESERT, VILLAGES, Set.of(OVERWORLD), "Minecraft"),
			new CatalogueEntry(OUTPOST, OUTPOSTS, Set.of(OVERWORLD), "Minecraft"),
			new CatalogueEntry(FORTRESS, FORTRESSES, Set.of(NETHER), "Minecraft"),
			new CatalogueEntry(BASTION, FORTRESSES, Set.of(NETHER), "Minecraft")));

	private static final TreeNames IDS = new TreeNames() {

		@Override
		public String structure(String structureId) {
			return structureId;
		}

		@Override
		public String group(String groupId) {
			return groupId;
		}

	};

	@Test
	void withoutAFilterEveryRowOfTheTreeIsTicked() {
		Selection selection = Selection.EMPTY.tickAll(CATALOGUE.tree(OVERWORLD), CATALOGUE, OVERWORLD);

		assertEquals(new Selection(Set.of(OUTPOST), Set.of(VILLAGES)), selection);
	}

	@Test
	void aFilterThatKeepsPartOfAGroupTicksOnlyTheStructuresItLists() {
		List<TreeEntry> listed = TreeFilter.of("desert").apply(CATALOGUE.tree(OVERWORLD), CATALOGUE, IDS);

		Selection selection = Selection.EMPTY.tickAll(listed, CATALOGUE, OVERWORLD);

		assertEquals(new Selection(Set.of(DESERT), Set.of()), selection);
	}

	@Test
	void aFilterThatKeepsAWholeGroupTicksTheGroup() {
		List<TreeEntry> listed = TreeFilter.of("villages").apply(CATALOGUE.tree(OVERWORLD), CATALOGUE, IDS);

		Selection selection = Selection.EMPTY.tickAll(listed, CATALOGUE, OVERWORLD);

		assertEquals(new Selection(Set.of(), Set.of(VILLAGES)), selection);
	}

	@Test
	void whatTheFilterLeavesOutStaysAsItWas() {
		List<TreeEntry> listed = TreeFilter.of("desert").apply(CATALOGUE.tree(OVERWORLD), CATALOGUE, IDS);

		Selection selection = Selection.EMPTY.tickStructure(OUTPOST).tickAll(listed, CATALOGUE, OVERWORLD);

		assertEquals(new Selection(Set.of(DESERT, OUTPOST), Set.of()), selection);
	}

	@Test
	void aStructureAlreadyCoveredByItsTickedGroupIsNotTickedAgain() {
		List<TreeEntry> listed = TreeFilter.of("desert").apply(CATALOGUE.tree(OVERWORLD), CATALOGUE, IDS);
		Selection ticked = Selection.EMPTY.tickGroup(VILLAGES, CATALOGUE, OVERWORLD);

		assertEquals(ticked, ticked.tickAll(listed, CATALOGUE, OVERWORLD));
	}

	@Test
	void elementsOutsideTheCurrentDimensionStayInTheSelection() {
		Selection selection = Selection.EMPTY.tickStructure(FORTRESS).tickAll(CATALOGUE.tree(OVERWORLD), CATALOGUE, OVERWORLD);

		assertEquals(new Selection(Set.of(FORTRESS, OUTPOST), Set.of(VILLAGES)), selection);
	}

	@Test
	void aStructureOfAnotherDimensionOfAGroupTickedWholeStaysTickedOneByOne() {
		Catalogue catalogue = new Catalogue(List.of(
				new CatalogueEntry(PORTAL, PORTALS, Set.of(OVERWORLD), "Minecraft"),
				new CatalogueEntry(PORTAL_DESERT, PORTALS, Set.of(OVERWORLD), "Minecraft"),
				new CatalogueEntry(PORTAL_NETHER, PORTALS, Set.of(NETHER), "Minecraft")));

		Selection selection = Selection.EMPTY.tickStructure(PORTAL_NETHER).tickAll(catalogue.tree(OVERWORLD), catalogue, OVERWORLD);

		assertEquals(new Selection(Set.of(PORTAL_NETHER), Set.of(PORTALS)), selection);
		assertEquals(Set.of(PORTAL_NETHER), selection.resolve(catalogue, NETHER));
		assertEquals(new Selection(Set.of(PORTAL_NETHER), Set.of()), selection.untickGroup(PORTALS));
	}

	@Test
	void anEmptyTreeTicksNothing() {
		Selection selection = Selection.EMPTY.tickStructure(OUTPOST);

		assertEquals(selection, selection.tickAll(List.of(), CATALOGUE, OVERWORLD));
	}

}
