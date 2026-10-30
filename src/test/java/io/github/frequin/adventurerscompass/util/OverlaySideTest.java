package io.github.frequin.adventurerscompass.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class OverlaySideTest {

	@Test
	void readsRightFromConfig() {
		assertEquals(OverlaySide.RIGHT, OverlaySide.fromString("RIGHT"));
	}

	@Test
	void fallsBackToLeftForAnyOtherValue() {
		assertEquals(OverlaySide.LEFT, OverlaySide.fromString("somewhere"));
	}

}
