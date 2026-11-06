package io.github.frequin.adventurerscompass.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.ToIntFunction;

import org.junit.jupiter.api.Test;

class EllipsisTest {

	/** Every code point is 2 wide, so widths in these tests are easy to count. */
	private static final ToIntFunction<String> WIDTH = text -> 2 * text.codePointCount(0, text.length());

	@Test
	void keepsATextThatFits() {
		assertEquals("Search", Ellipsis.fit("Search", 12, WIDTH));
	}

	@Test
	void cutsATextTooLongAndEndsItWithAnEllipsis() {
		assertEquals("Sea…", Ellipsis.fit("Search", 8, WIDTH));
	}

	@Test
	void theCutTextNeverExceedsTheWidth() {
		assertEquals("Sea…", Ellipsis.fit("Search", 9, WIDTH));
	}

	@Test
	void dropsTheSpaceBeforeTheEllipsis() {
		assertEquals("Clear…", Ellipsis.fit("Clear Selection", 14, WIDTH));
	}

	@Test
	void showsOnlyTheEllipsisWhenNoCharacterFitsBesideIt() {
		assertEquals("…", Ellipsis.fit("Search", 3, WIDTH));
	}

	@Test
	void showsNothingWhenEvenTheEllipsisDoesNotFit() {
		assertEquals("", Ellipsis.fit("Search", 1, WIDTH));
	}

	@Test
	void neverSplitsACharacterOutsideTheBasicPlane() {
		assertEquals("a🧭…", Ellipsis.fit("a🧭bcd", 6, WIDTH));
	}

}
