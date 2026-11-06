package io.github.frequin.adventurerscompass.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ReadableNameTest {

	@Test
	void capitalizesEachWordOfThePath() {
		assertEquals("Ice Towers", ReadableName.of("towers:ice_towers"));
	}

	@Test
	void leavesTheNamespaceOut() {
		assertEquals("Villages", ReadableName.of("minecraft:villages"));
	}

	@Test
	void readsSubfoldersAsWords() {
		assertEquals("Small Village Plains", ReadableName.of("towns:small/village_plains"));
	}

	@Test
	void readsDashesAndDotsAsWords() {
		assertEquals("Big Stone Tower", ReadableName.of("towers:big-stone.tower"));
	}

	@Test
	void collapsesRepeatedSeparators() {
		assertEquals("Ice Tower", ReadableName.of("towers:_ice__tower/"));
	}

	@Test
	void takesAnIdWithoutNamespace() {
		assertEquals("The Nether", ReadableName.of("the_nether"));
	}

	@Test
	void keepsTheIdWhenItHoldsNoWord() {
		assertEquals("towers:_", ReadableName.of("towers:_"));
	}

}
