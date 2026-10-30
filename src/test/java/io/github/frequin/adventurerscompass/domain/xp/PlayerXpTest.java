package io.github.frequin.adventurerscompass.domain.xp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PlayerXpTest {

	@Test
	void aFoundTargetTakesItsCost() {
		assertEquals(5, new PlayerXp(12, false).payment(5));
	}

	@Test
	void aPlayerWhoSpentXpDuringTheSearchPaysOnlyTheLevelsLeft() {
		assertEquals(3, new PlayerXp(3, false).payment(10));
		assertEquals(0, new PlayerXp(0, false).payment(10));
	}

	@Test
	void creativeNeverPays() {
		assertEquals(0, new PlayerXp(30, true).payment(10));
	}

	@Test
	void aPlayerCanLaunchWithAtLeastTheThreshold() {
		assertEquals(true, new PlayerXp(10, false).canLaunch(new XpThreshold(10, true)));
		assertEquals(false, new PlayerXp(9, false).canLaunch(new XpThreshold(10, true)));
	}

	@Test
	void creativeIgnoresTheThreshold() {
		assertEquals(true, new PlayerXp(0, true).canLaunch(new XpThreshold(10, false)));
	}

}
