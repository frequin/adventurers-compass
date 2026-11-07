package io.github.frequin.adventurerscompass.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GlobTest {

	@Test
	void aStarMatchesAnyRunOfCharactersIncludingNone() {
		Glob glob = Glob.of("minecraft:*village*");

		assertTrue(glob.matches("minecraft:village_plains"));
		assertTrue(glob.matches("minecraft:village"));
		assertFalse(glob.matches("othermod:village_plains"));
	}

	@Test
	void aQuestionMarkMatchesExactlyOneCharacter() {
		Glob glob = Glob.of("minecraft:stronghol?");

		assertTrue(glob.matches("minecraft:stronghold"));
		assertFalse(glob.matches("minecraft:strongho"));
		assertFalse(glob.matches("minecraft:strongholds"));
	}

	@Test
	void everythingElseIsLiteral() {
		assertTrue(Glob.of("mod.a:tower").matches("mod.a:tower"));
		assertFalse(Glob.of("mod.a:tower").matches("modxa:tower"));
		assertTrue(Glob.of("mod+[x]:a(b)$").matches("mod+[x]:a(b)$"));
		assertFalse(Glob.of("mod\\Etower").matches("modtower"));
	}

	@Test
	void theWholeIdMustMatch() {
		assertFalse(Glob.of("minecraft:village").matches("minecraft:village_plains"));
		assertFalse(Glob.of("village").matches("minecraft:village"));
	}

}
