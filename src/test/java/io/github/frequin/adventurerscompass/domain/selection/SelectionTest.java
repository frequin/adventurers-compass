package io.github.frequin.adventurerscompass.domain.selection;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class SelectionTest {

	private static final String OVERWORLD = "minecraft:overworld";
	private static final String NETHER = "minecraft:the_nether";

	private static final String VILLAGES = "minecraft:villages";
	private static final String PLAINS = "minecraft:village_plains";
	private static final String DESERT = "minecraft:village_desert";
	private static final String SNOWY = "minecraft:village_snowy";

	private static final String OUTPOSTS = "minecraft:pillager_outposts";
	private static final String OUTPOST = "minecraft:pillager_outpost";

	private static final String RUINED_PORTALS = "minecraft:ruined_portals";
	private static final String PORTAL = "minecraft:ruined_portal";
	private static final String PORTAL_NETHER = "minecraft:ruined_portal_nether";

	private static final Catalogue CATALOGUE = new Catalogue(List.of(
			new CatalogueEntry(PLAINS, VILLAGES, Set.of(OVERWORLD), "Minecraft"),
			new CatalogueEntry(DESERT, VILLAGES, Set.of(OVERWORLD), "Minecraft"),
			new CatalogueEntry(SNOWY, VILLAGES, Set.of(OVERWORLD), "Minecraft"),
			new CatalogueEntry(OUTPOST, OUTPOSTS, Set.of(OVERWORLD), "Minecraft"),
			new CatalogueEntry(PORTAL, RUINED_PORTALS, Set.of(OVERWORLD), "Minecraft"),
			new CatalogueEntry(PORTAL_NETHER, RUINED_PORTALS, Set.of(NETHER), "Minecraft")));

	@Test
	void anEmptySelectionHasNothingToSearch() {
		assertEquals(Set.of(), Selection.EMPTY.resolve(CATALOGUE, OVERWORLD));
	}

	@Test
	void aTickedStructureIsSearchable() {
		Selection selection = Selection.EMPTY.tickStructure(DESERT);

		assertEquals(Set.of(DESERT), selection.resolve(CATALOGUE, OVERWORLD));
	}

	@Test
	void aTickedGroupMakesAllItsStructuresSearchable() {
		Selection selection = Selection.EMPTY.tickGroup(VILLAGES, CATALOGUE, OVERWORLD).tickGroup(OUTPOSTS, CATALOGUE, OVERWORLD);

		assertEquals(Set.of(PLAINS, DESERT, SNOWY, OUTPOST), selection.resolve(CATALOGUE, OVERWORLD));
	}

	@Test
	void onlyStructuresGeneratedInTheDimensionAreSearchable() {
		Selection selection = Selection.EMPTY.tickGroup(RUINED_PORTALS, CATALOGUE, OVERWORLD).tickStructure(DESERT);

		assertEquals(Set.of(PORTAL_NETHER), selection.resolve(CATALOGUE, NETHER));
		assertEquals(Set.of(PORTAL, DESERT), selection.resolve(CATALOGUE, OVERWORLD));
	}

	@Test
	void aStructureMissingFromTheCatalogueIsNotSearchable() {
		Selection selection = Selection.EMPTY.tickStructure("othermod:gone").tickStructure(SNOWY);

		assertEquals(Set.of(SNOWY), selection.resolve(CATALOGUE, OVERWORLD));
	}

	@Test
	void untickingAStructureInATickedGroupExplodesTheGroupIntoItsOtherStructures() {
		Selection selection = Selection.EMPTY.tickGroup(VILLAGES, CATALOGUE, OVERWORLD).untickStructure(SNOWY, CATALOGUE);

		assertEquals(new Selection(Set.of(PLAINS, DESERT), Set.of()), selection);
	}

	@Test
	void severalStructuresCanBeTakenOutOfTheSameGroup() {
		Selection selection = Selection.EMPTY.tickGroup(VILLAGES, CATALOGUE, OVERWORLD).untickStructure(SNOWY, CATALOGUE).untickStructure(PLAINS, CATALOGUE);

		assertEquals(Set.of(DESERT), selection.resolve(CATALOGUE, OVERWORLD));
	}

	@Test
	void explodingAGroupKeepsItsStructuresOfOtherDimensions() {
		Selection selection = Selection.EMPTY.tickGroup(RUINED_PORTALS, CATALOGUE, OVERWORLD).untickStructure(PORTAL, CATALOGUE);

		assertEquals(Set.of(), selection.resolve(CATALOGUE, OVERWORLD));
		assertEquals(Set.of(PORTAL_NETHER), selection.resolve(CATALOGUE, NETHER));
	}

	@Test
	void aStructureOfAnotherDimensionTickedOneByOneOutlivesItsGroupBeingTickedThenUnticked() {
		Selection before = Selection.EMPTY.tickStructure(PORTAL_NETHER);

		Selection ticked = before.toggleGroup(RUINED_PORTALS, CATALOGUE, OVERWORLD);
		Selection unticked = ticked.toggleGroup(RUINED_PORTALS, CATALOGUE, OVERWORLD);

		assertEquals(new Selection(Set.of(PORTAL_NETHER), Set.of(RUINED_PORTALS)), ticked);
		assertEquals(before, unticked);
	}

	@Test
	void aTickedGroupCoversItsStructuresOfOtherDimensionsThatWereNeverTicked() {
		Selection selection = Selection.EMPTY.tickGroup(RUINED_PORTALS, CATALOGUE, OVERWORLD);

		assertEquals(new Selection(Set.of(), Set.of(RUINED_PORTALS)), selection);
		assertEquals(StructureState.TICKED_BY_GROUP, selection.structureState(PORTAL_NETHER, CATALOGUE));
		assertEquals(Set.of(PORTAL_NETHER), selection.resolve(CATALOGUE, NETHER));
	}

	@Test
	void tickingAGroupAbsorbsItsStructuresTickedOneByOne() {
		Selection selection = Selection.EMPTY.tickStructure(DESERT).tickStructure(OUTPOST).tickGroup(VILLAGES, CATALOGUE, OVERWORLD);

		assertEquals(new Selection(Set.of(OUTPOST), Set.of(VILLAGES)), selection);
	}

	@Test
	void aGroupReadsTickedPartialOrEmpty() {
		Selection selection = Selection.EMPTY.tickGroup(VILLAGES, CATALOGUE, OVERWORLD).tickStructure(PORTAL);

		assertEquals(GroupState.TICKED, selection.groupState(VILLAGES, CATALOGUE, OVERWORLD));
		assertEquals(GroupState.PARTIAL, selection.groupState(RUINED_PORTALS, CATALOGUE, OVERWORLD));
		assertEquals(GroupState.EMPTY, selection.groupState(OUTPOSTS, CATALOGUE, OVERWORLD));
	}

	@Test
	void aGroupIsPartialOnlyThroughStructuresOfTheDimension() {
		Selection selection = Selection.EMPTY.tickStructure(PORTAL);

		assertEquals(GroupState.EMPTY, selection.groupState(RUINED_PORTALS, CATALOGUE, NETHER));
	}

	@Test
	void aStructureReadsTickedByItselfThroughItsGroupOrNot() {
		Selection selection = Selection.EMPTY.tickGroup(VILLAGES, CATALOGUE, OVERWORLD).tickStructure(OUTPOST);

		assertEquals(StructureState.TICKED, selection.structureState(OUTPOST, CATALOGUE));
		assertEquals(StructureState.TICKED_BY_GROUP, selection.structureState(DESERT, CATALOGUE));
		assertEquals(StructureState.EMPTY, selection.structureState(PORTAL, CATALOGUE));
	}

	@Test
	void theTreeListsGroupsWithTheirStructuresOfTheDimension() {
		assertEquals(List.of(
				new TreeEntry.GroupEntry(VILLAGES, List.of(PLAINS, DESERT, SNOWY)),
				new TreeEntry.StructureEntry(OUTPOST),
				new TreeEntry.StructureEntry(PORTAL)), CATALOGUE.tree(OVERWORLD));
	}

	@Test
	void aGroupWithASingleStructureInTheDimensionAppearsAsThatStructure() {
		assertEquals(List.of(new TreeEntry.StructureEntry(PORTAL_NETHER)), CATALOGUE.tree(NETHER));
	}

	@Test
	void nextVillageOrOutpostTakesTwoTicks() {
		Selection selection = Selection.EMPTY.toggleGroup(VILLAGES, CATALOGUE, OVERWORLD).toggleStructure(OUTPOST, CATALOGUE);

		assertEquals(Set.of(PLAINS, DESERT, SNOWY, OUTPOST), selection.resolve(CATALOGUE, OVERWORLD));
	}

	@Test
	void togglingTwiceGivesTheSelectionBack() {
		Selection selection = Selection.EMPTY.tickStructure(DESERT);

		assertEquals(selection, selection.toggleGroup(OUTPOSTS, CATALOGUE, OVERWORLD).toggleGroup(OUTPOSTS, CATALOGUE, OVERWORLD));
		assertEquals(Selection.EMPTY, selection.toggleStructure(DESERT, CATALOGUE));
	}

	@Test
	void togglingAPartialGroupTicksIt() {
		Selection selection = Selection.EMPTY.tickStructure(DESERT).toggleGroup(VILLAGES, CATALOGUE, OVERWORLD);

		assertEquals(GroupState.TICKED, selection.groupState(VILLAGES, CATALOGUE, OVERWORLD));
	}

	@Test
	void togglingAStructureTickedByItsGroupExplodesTheGroup() {
		Selection selection = Selection.EMPTY.tickGroup(VILLAGES, CATALOGUE, OVERWORLD).toggleStructure(PLAINS, CATALOGUE);

		assertEquals(Set.of(DESERT, SNOWY), selection.resolve(CATALOGUE, OVERWORLD));
	}

	@Test
	void untickingAStructureOrAGroupRemovesIt() {
		Selection selection = Selection.EMPTY.tickStructure(DESERT).tickStructure(OUTPOST).tickGroup(RUINED_PORTALS, CATALOGUE, OVERWORLD)
				.untickStructure(OUTPOST, CATALOGUE).untickGroup(RUINED_PORTALS);

		assertEquals(Set.of(DESERT), selection.resolve(CATALOGUE, OVERWORLD));
	}

}
