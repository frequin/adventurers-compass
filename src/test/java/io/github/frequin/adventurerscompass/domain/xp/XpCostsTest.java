package io.github.frequin.adventurerscompass.domain.xp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class XpCostsTest {

	private static final String OVERWORLD = "minecraft:overworld";
	private static final String NETHER = "minecraft:the_nether";
	private static final String END = "minecraft:the_end";
	private static final String VILLAGE = "minecraft:village_plains";

	/** The spec's defaults: 5 levels, x1 in the Overworld, x2 elsewhere. */
	private static final XpCosts DEFAULTS = new XpCosts(5, Map.of(), 2, Map.of(OVERWORLD, 1.0));

	@Test
	void aStructureCostsTheDefaultLevelTimesTheCurrentDimensionMultiplier() {
		assertEquals(5, DEFAULTS.cost(VILLAGE, OVERWORLD));
		assertEquals(10, DEFAULTS.cost(VILLAGE, NETHER));
	}

	@Test
	void theRealCostIsRoundedUp() {
		var costs = new XpCosts(5, Map.of(), 1, Map.of(NETHER, 1.5, END, 0.7));

		assertEquals(8, costs.cost(VILLAGE, NETHER)); // 7.5
		assertEquals(4, costs.cost(VILLAGE, END)); // 3.5
	}

	@Test
	void aMultiplierThatIsExactInDecimalIsNotRoundedUpByFloatingPointNoise() {
		var costs = new XpCosts(10, Map.of(), 1, Map.of(END, 0.7));

		assertEquals(7, costs.cost(VILLAGE, END)); // 10 x 0.7 = 7.000000000000001 in doubles
	}

	@Test
	void aGlobOverrideReplacesTheDefaultLevelWithoutUpperBound() {
		var costs = new XpCosts(5, Map.of("minecraft:*village*", 1, "minecraft:stronghold", 30, "minecraft:ocean_ruin_????", 2), 2, Map.of(OVERWORLD, 1.0));

		assertEquals(1, costs.cost(VILLAGE, OVERWORLD));
		assertEquals(30, costs.cost("minecraft:stronghold", OVERWORLD));
		assertEquals(60, costs.cost("minecraft:stronghold", NETHER));
		assertEquals(2, costs.cost("minecraft:ocean_ruin_warm", OVERWORLD));
		assertEquals(5, costs.cost("minecraft:ocean_ruin", OVERWORLD));
		assertEquals(5, costs.cost("minecraft:fortress", OVERWORLD));
	}

	@Test
	void theFirstMatchingOverrideWins() {
		var overrides = new LinkedHashMap<String, Integer>();
		overrides.put("minecraft:village_plains", 3);
		overrides.put("minecraft:village_*", 7);

		var costs = new XpCosts(5, overrides, 1, Map.of());

		assertEquals(3, costs.cost(VILLAGE, OVERWORLD));
		assertEquals(7, costs.cost("minecraft:village_desert", OVERWORLD));
	}

	@Test
	void aStructurePricedZeroIsFreeInEveryDimension() {
		var costs = new XpCosts(5, Map.of("minecraft:village_*", 0), 2, Map.of());

		assertEquals(0, costs.cost(VILLAGE, NETHER));
	}

	@Test
	void aDotInAnOverrideMatchesOnlyADot() {
		var costs = new XpCosts(5, Map.of("mod:a.b", 1), 1, Map.of());

		assertEquals(5, costs.cost("mod:axb", OVERWORLD));
		assertEquals(1, costs.cost("mod:a.b", OVERWORLD));
	}

	@Test
	void theThresholdIsTheHighestRealCostOfTheSearchableStructuresAndSaysWhenCostsVary() {
		var costs = new XpCosts(5, Map.of("minecraft:stronghold", 10), 2, Map.of(OVERWORLD, 1.0));

		assertEquals(new XpThreshold(10, true), costs.threshold(Set.of(VILLAGE, "minecraft:stronghold"), OVERWORLD));
		assertEquals(new XpThreshold(10, false), costs.threshold(Set.of(VILLAGE, "minecraft:fortress"), NETHER));
		assertEquals(new XpThreshold(5, false), costs.threshold(Set.of(VILLAGE), OVERWORLD));
	}

	@Test
	void nothingSearchableHasAFreeThreshold() {
		assertEquals(new XpThreshold(0, false), DEFAULTS.threshold(Set.of(), OVERWORLD));
	}

	@Test
	void theClientBuildsTheSameThresholdFromTheRealCostsItReceived() {
		assertEquals(new XpThreshold(10, true), XpThreshold.of(List.of(5, 10, 0)));
		assertEquals(new XpThreshold(3, false), XpThreshold.of(List.of(3, 3)));
	}

	@Test
	void aNegativeLevelOrMultiplierCostsNothingRatherThanGivingXpBack() {
		assertEquals(0, new XpCosts(-3, Map.of(), 1, Map.of()).cost(VILLAGE, OVERWORLD));
		assertEquals(0, new XpCosts(5, Map.of(), -1, Map.of()).cost(VILLAGE, NETHER));
	}

}
