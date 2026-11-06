package io.github.frequin.adventurerscompass.domain.selection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.github.frequin.adventurerscompass.domain.selection.IgnoredElement.Reason;

class SelectionIgnoredTest {

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
	void anEmptySelectionIgnoresNothing() {
		assertEquals(List.of(), Selection.EMPTY.ignored(CATALOGUE, OVERWORLD));
	}

	@Test
	void aSelectionThatGeneratesHereIgnoresNothing() {
		Selection selection = Selection.EMPTY.tickStructure(PORTAL).tickGroup(VILLAGES, CATALOGUE, OVERWORLD);

		assertEquals(List.of(), selection.ignored(CATALOGUE, OVERWORLD));
	}

	@Test
	void afterATripToTheNetherOverworldElementsAreIgnoredGroupsFirstAndTheRestIsSearched() {
		Selection selection = Selection.EMPTY.tickGroup(VILLAGES, CATALOGUE, OVERWORLD).tickStructure(PORTAL).tickStructure(FORTRESS);

		assertEquals(List.of(
				new IgnoredElement(VILLAGES, Reason.ABSENT_FROM_DIMENSION),
				new IgnoredElement(PORTAL, Reason.ABSENT_FROM_DIMENSION)), selection.ignored(CATALOGUE, NETHER));
		assertEquals(Set.of(FORTRESS), selection.resolve(CATALOGUE, NETHER));
	}

	@Test
	void aRoundTripBetweenDimensionsLeavesTheSelectionWhole() {
		Selection selection = Selection.EMPTY.tickGroup(VILLAGES, CATALOGUE, OVERWORLD).tickStructure(FORTRESS);

		assertEquals(1, selection.ignored(CATALOGUE, NETHER).size());
		assertEquals(1, selection.ignored(CATALOGUE, OVERWORLD).size());
		assertEquals(Set.of(PLAINS, DESERT), selection.resolve(CATALOGUE, OVERWORLD));
		assertEquals(Set.of(FORTRESS), selection.resolve(CATALOGUE, NETHER));
		assertEquals(new Selection(Set.of(FORTRESS), Set.of(VILLAGES)), selection);
	}

	@Test
	void aGroupSpanningDimensionsIsNotIgnoredWhereOneOfItsStructuresGenerates() {
		Selection selection = Selection.EMPTY.tickGroup(RUINED_PORTALS, CATALOGUE, OVERWORLD);

		assertEquals(List.of(), selection.ignored(CATALOGUE, NETHER));
		assertEquals(List.of(), selection.ignored(CATALOGUE, OVERWORLD));
	}

	@Test
	void clearingEmptiesTheSelection() {
		Selection selection = Selection.EMPTY.tickGroup(VILLAGES, CATALOGUE, OVERWORLD).tickStructure(FORTRESS);

		assertEquals(Selection.EMPTY, selection.clear());
	}

	@Test
	void aSelectionWithNothingGeneratedInTheDimensionCannotBeSearchedThere() {
		Selection selection = Selection.EMPTY.tickGroup(VILLAGES, CATALOGUE, OVERWORLD).tickStructure(PORTAL);

		assertFalse(selection.canSearchIn(CATALOGUE, NETHER));
		assertTrue(selection.canSearchIn(CATALOGUE, OVERWORLD));
		assertFalse(Selection.EMPTY.canSearchIn(CATALOGUE, OVERWORLD));
	}

	@Test
	void oneElementNotIgnoredIsEnoughToSearch() {
		Selection selection = Selection.EMPTY.tickGroup(VILLAGES, CATALOGUE, OVERWORLD).tickStructure(BASTION);

		assertTrue(selection.canSearchIn(CATALOGUE, NETHER));
	}

	@Test
	void anElementMissingFromTheCatalogueIsIgnored() {
		Selection selection = new Selection(Set.of("othermod:gone"), Set.of("othermod:gone_group"));

		assertEquals(List.of(
				new IgnoredElement("othermod:gone_group", Reason.ABSENT_FROM_DIMENSION),
				new IgnoredElement("othermod:gone", Reason.ABSENT_FROM_DIMENSION)), selection.ignored(CATALOGUE, OVERWORLD));
	}

}
