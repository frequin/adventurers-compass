package io.github.frequin.adventurerscompass.domain.selection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class HiddenRuleTest {

	@Test
	void theCommonTagHidesAStructureEvenWithAnEmptyConfig() {
		HiddenRule rule = HiddenRule.of(List.of());

		assertTrue(rule.isHidden("somemod:secret_base", Set.of("c:hidden_from_locator_selection")));
		assertFalse(rule.isHidden("minecraft:village_plains", Set.of("minecraft:village")));
	}

	@Test
	void aGlobEntryHidesTheStructuresWhoseIdMatches() {
		HiddenRule rule = HiddenRule.of(List.of("minecraft:*village*", "minecraft:stronghol?"));

		assertTrue(rule.isHidden("minecraft:village_plains", Set.of()));
		assertTrue(rule.isHidden("minecraft:stronghold", Set.of()));
		assertFalse(rule.isHidden("minecraft:pillager_outpost", Set.of()));
		assertFalse(rule.isHidden("othermod:village_plains", Set.of()));
	}

	@Test
	void aTagEntryHidesTheStructuresCarryingThatTag() {
		HiddenRule rule = HiddenRule.of(List.of("#minecraft:village"));

		assertTrue(rule.isHidden("minecraft:village_plains", Set.of("minecraft:village")));
		assertFalse(rule.isHidden("minecraft:pillager_outpost", Set.of("minecraft:on_trail_ruins_maps")));
		assertFalse(rule.isHidden("minecraft:village", Set.of()));
	}

	@Test
	void aDotInAGlobIsLiteral() {
		HiddenRule rule = HiddenRule.of(List.of("mod.a:tower"));

		assertTrue(rule.isHidden("mod.a:tower", Set.of()));
		assertFalse(rule.isHidden("modxa:tower", Set.of()));
	}

}
