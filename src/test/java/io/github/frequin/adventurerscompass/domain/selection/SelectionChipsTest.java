package io.github.frequin.adventurerscompass.domain.selection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.github.frequin.adventurerscompass.domain.selection.Chip.Kind;

class SelectionChipsTest {

	private static final String OVERWORLD = "minecraft:overworld";
	private static final String NETHER = "minecraft:the_nether";

	private static final String VILLAGES = "minecraft:villages";
	private static final String PLAINS = "minecraft:village_plains";
	private static final String DESERT = "minecraft:village_desert";

	private static final String FORTRESSES = "minecraft:nether_complexes";
	private static final String FORTRESS = "minecraft:fortress";
	private static final String BASTION = "minecraft:bastion_remnant";

	private static final String RUINED_PORTALS = "minecraft:ruined_portals";
	private static final String PORTAL = "minecraft:ruined_portal";
	private static final String PORTAL_NETHER = "minecraft:ruined_portal_nether";

	private static final Catalogue CATALOGUE = new Catalogue(List.of(
			new CatalogueEntry(PLAINS, VILLAGES, Set.of(OVERWORLD), "Minecraft"),
			new CatalogueEntry(DESERT, VILLAGES, Set.of(OVERWORLD), "Minecraft"),
			new CatalogueEntry(FORTRESS, FORTRESSES, Set.of(NETHER), "Minecraft"),
			new CatalogueEntry(BASTION, FORTRESSES, Set.of(NETHER), "Minecraft"),
			new CatalogueEntry(PORTAL, RUINED_PORTALS, Set.of(OVERWORLD), "Minecraft"),
			new CatalogueEntry(PORTAL_NETHER, RUINED_PORTALS, Set.of(NETHER), "Minecraft")));

	@Test
	void anEmptySelectionHasNoChips() {
		assertEquals(List.of(), Selection.EMPTY.chips(CATALOGUE, OVERWORLD));
	}

	@Test
	void eachGroupAndStructureOfTheSelectionIsAChipGroupsFirst() {
		Selection selection = Selection.EMPTY.tickStructure(PORTAL).tickGroup(VILLAGES, CATALOGUE);

		assertEquals(List.of(
				new Chip(Kind.GROUP, VILLAGES, ChipState.ACTIVE),
				new Chip(Kind.STRUCTURE, PORTAL, ChipState.ACTIVE)), selection.chips(CATALOGUE, OVERWORLD));
	}

	@Test
	void afterATripToTheNetherOverworldElementsAreGreyedAndTheRestStaysActive() {
		Selection selection = Selection.EMPTY.tickGroup(VILLAGES, CATALOGUE).tickStructure(PORTAL).tickStructure(FORTRESS);

		assertEquals(List.of(
				new Chip(Kind.GROUP, VILLAGES, ChipState.ABSENT_FROM_DIMENSION),
				new Chip(Kind.STRUCTURE, FORTRESS, ChipState.ACTIVE),
				new Chip(Kind.STRUCTURE, PORTAL, ChipState.ABSENT_FROM_DIMENSION)), selection.chips(CATALOGUE, NETHER));
		assertEquals(Set.of(FORTRESS), selection.resolve(CATALOGUE, NETHER));
	}

	@Test
	void aGroupSpanningDimensionsIsActiveWhereverOneOfItsStructuresGenerates() {
		Selection selection = Selection.EMPTY.tickGroup(RUINED_PORTALS, CATALOGUE);

		assertEquals(List.of(new Chip(Kind.GROUP, RUINED_PORTALS, ChipState.ACTIVE)), selection.chips(CATALOGUE, NETHER));
		assertEquals(List.of(new Chip(Kind.GROUP, RUINED_PORTALS, ChipState.ACTIVE)), selection.chips(CATALOGUE, OVERWORLD));
	}

	@Test
	void removingAChipTakesOnlyThatElementOutOfTheSelection() {
		Selection selection = Selection.EMPTY.tickGroup(VILLAGES, CATALOGUE).tickStructure(PORTAL).tickStructure(FORTRESS);

		Selection withoutVillages = selection.remove(new Chip(Kind.GROUP, VILLAGES, ChipState.ACTIVE));
		Selection withoutPortal = selection.remove(new Chip(Kind.STRUCTURE, PORTAL, ChipState.ABSENT_FROM_DIMENSION));

		assertEquals(new Selection(Set.of(PORTAL, FORTRESS), Set.of()), withoutVillages);
		assertEquals(new Selection(Set.of(FORTRESS), Set.of(VILLAGES)), withoutPortal);
	}

	@Test
	void clearingEmptiesTheSelection() {
		Selection selection = Selection.EMPTY.tickGroup(VILLAGES, CATALOGUE).tickStructure(FORTRESS);

		assertEquals(Selection.EMPTY, selection.clear());
	}

	@Test
	void aSelectionWithNothingGeneratedInTheDimensionCannotBeSearchedThere() {
		Selection selection = Selection.EMPTY.tickGroup(VILLAGES, CATALOGUE).tickStructure(PORTAL);

		assertFalse(selection.canSearchIn(CATALOGUE, NETHER));
		assertTrue(selection.canSearchIn(CATALOGUE, OVERWORLD));
		assertFalse(Selection.EMPTY.canSearchIn(CATALOGUE, OVERWORLD));
	}

	@Test
	void oneActiveChipIsEnoughToSearch() {
		Selection selection = Selection.EMPTY.tickGroup(VILLAGES, CATALOGUE).tickStructure(BASTION);

		assertTrue(selection.canSearchIn(CATALOGUE, NETHER));
	}

	@Test
	void anElementMissingFromTheCatalogueIsGreyed() {
		Selection selection = new Selection(Set.of("othermod:gone"), Set.of("othermod:gone_group"));

		assertEquals(List.of(
				new Chip(Kind.GROUP, "othermod:gone_group", ChipState.ABSENT_FROM_DIMENSION),
				new Chip(Kind.STRUCTURE, "othermod:gone", ChipState.ABSENT_FROM_DIMENSION)), selection.chips(CATALOGUE, OVERWORLD));
	}

}
