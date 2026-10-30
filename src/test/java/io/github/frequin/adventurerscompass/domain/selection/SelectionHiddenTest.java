package io.github.frequin.adventurerscompass.domain.selection;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class SelectionHiddenTest {

	private static final String OVERWORLD = "minecraft:overworld";
	private static final String NETHER = "minecraft:the_nether";

	private static final String VILLAGES = "minecraft:villages";
	private static final String PLAINS = "minecraft:village_plains";
	private static final String DESERT = "minecraft:village_desert";
	private static final String SNOWY = "minecraft:village_snowy";

	private static final String STRONGHOLDS = "minecraft:strongholds";
	private static final String STRONGHOLD = "minecraft:stronghold";

	private static final String OUTPOSTS = "minecraft:pillager_outposts";
	private static final String OUTPOST = "minecraft:pillager_outpost";

	/** Desert villages and the stronghold are Hidden. */
	private static final Catalogue CATALOGUE = new Catalogue(List.of(
			new CatalogueEntry(PLAINS, VILLAGES, Set.of(OVERWORLD), "Minecraft"),
			new CatalogueEntry(DESERT, VILLAGES, Set.of(OVERWORLD), "Minecraft", true),
			new CatalogueEntry(SNOWY, VILLAGES, Set.of(OVERWORLD), "Minecraft"),
			new CatalogueEntry(STRONGHOLD, STRONGHOLDS, Set.of(OVERWORLD), "Minecraft", true),
			new CatalogueEntry(OUTPOST, OUTPOSTS, Set.of(OVERWORLD), "Minecraft")));

	@Test
	void aHiddenStructureIsNotListedInTheTree() {
		assertEquals(List.of(
				new TreeEntry.GroupEntry(VILLAGES, List.of(PLAINS, SNOWY)),
				new TreeEntry.StructureEntry(OUTPOST)), CATALOGUE.tree(OVERWORLD));
	}

	@Test
	void aHiddenStructureIsNotSearchedEvenThroughItsGroup() {
		Selection selection = Selection.EMPTY.tickGroup(VILLAGES, CATALOGUE).tickStructure(STRONGHOLD).tickStructure(OUTPOST);

		assertEquals(Set.of(PLAINS, SNOWY, OUTPOST), selection.resolve(CATALOGUE, OVERWORLD));
	}

	@Test
	void explodingAGroupDoesNotTickItsHiddenMembers() {
		Selection selection = Selection.EMPTY.tickGroup(VILLAGES, CATALOGUE).untickStructure(PLAINS, CATALOGUE);

		assertEquals(new Selection(Set.of(SNOWY), Set.of()), selection);
	}

	@Test
	void aHiddenMemberTickedAloneDoesNotMakeItsGroupPartial() {
		Selection selection = Selection.EMPTY.tickStructure(DESERT);

		assertEquals(GroupState.EMPTY, selection.groupState(VILLAGES, CATALOGUE, OVERWORLD));
	}

	@Test
	void hiddenStructuresAreNotCountedAsGeneratedInOtherDimensions() {
		Catalogue catalogue = new Catalogue(List.of(
				new CatalogueEntry(PLAINS, VILLAGES, Set.of(OVERWORLD), "Minecraft"),
				new CatalogueEntry(STRONGHOLD, STRONGHOLDS, Set.of(OVERWORLD), "Minecraft", true)));

		assertEquals(1, catalogue.countOutside(NETHER));
	}

	@Test
	void aStructureHiddenAfterBeingSelectedStaysAsAGreyedHiddenChip() {
		Selection selection = Selection.EMPTY.tickStructure(STRONGHOLD).tickStructure(OUTPOST);

		assertEquals(List.of(
				new Chip(Chip.Kind.STRUCTURE, OUTPOST, ChipState.ACTIVE),
				new Chip(Chip.Kind.STRUCTURE, STRONGHOLD, ChipState.HIDDEN)), selection.chips(CATALOGUE, OVERWORLD));
	}

	@Test
	void aHiddenChipIsGreyedAsHiddenEvenOutsideItsDimension() {
		Selection selection = Selection.EMPTY.tickStructure(STRONGHOLD);

		assertEquals(List.of(new Chip(Chip.Kind.STRUCTURE, STRONGHOLD, ChipState.HIDDEN)), selection.chips(CATALOGUE, NETHER));
	}

	@Test
	void aGroupStaysActiveWhileOneOfItsMembersIsNotHidden() {
		Selection selection = Selection.EMPTY.tickGroup(VILLAGES, CATALOGUE);

		assertEquals(List.of(new Chip(Chip.Kind.GROUP, VILLAGES, ChipState.ACTIVE)), selection.chips(CATALOGUE, OVERWORLD));
	}

	@Test
	void aGroupWhoseMembersAreAllHiddenIsAGreyedHiddenChip() {
		Selection selection = Selection.EMPTY.tickGroup(STRONGHOLDS, CATALOGUE);

		assertEquals(List.of(new Chip(Chip.Kind.GROUP, STRONGHOLDS, ChipState.HIDDEN)), selection.chips(CATALOGUE, OVERWORLD));
		assertEquals(false, selection.canSearchIn(CATALOGUE, OVERWORLD));
	}

}
