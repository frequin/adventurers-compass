package io.github.frequin.adventurerscompass.domain.compass;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.frequin.adventurerscompass.domain.BlockXZ;
import io.github.frequin.adventurerscompass.domain.ChunkCoord;
import io.github.frequin.adventurerscompass.domain.InstanceKey;

/** {@code disabledDimensions}: the compass refuses to search there and loses nothing. */
class DisabledDimensionTest {

	private static final String OVERWORLD = "minecraft:overworld";
	private static final String END = "minecraft:the_end";
	private static final String VILLAGE = "minecraft:village_plains";
	private static final String END_CITY = "minecraft:end_city";

	private final Compass compass = new Compass(List.of(LaunchRule.notIn(Set.of(END)::contains), LaunchRule.SOMETHING_SEARCHABLE));

	private static SearchContext inTheEnd() {
		return new SearchContext(END, new BlockXZ(0, 0), Set.of(END_CITY), 1000, Set.of());
	}

	private static Target overworldVillage() {
		return new Target(new InstanceKey(OVERWORLD, VILLAGE, new ChunkCoord(3, 4)), new BlockXZ(48, 64));
	}

	@Test
	void searchIsRefusedInADisabledDimension() {
		assertEquals(new Outcome.Refused(Refusal.DISABLED_DIMENSION), compass.search(CompassState.EMPTY, inTheEnd(), UUID.randomUUID()));
	}

	@Test
	void passIsRefusedInADisabledDimension() {
		var state = CompassState.EMPTY.withTarget(overworldVillage());

		assertEquals(new Outcome.Refused(Refusal.DISABLED_DIMENSION), compass.pass(state, inTheEnd(), UUID.randomUUID()));
	}

	@Test
	void theDisabledDimensionIsTheReasonGivenEvenWhenNothingIsSearchable() {
		var nothingListed = new SearchContext(END, new BlockXZ(0, 0), Set.of(), 1000, Set.of());

		assertEquals(new Outcome.Refused(Refusal.DISABLED_DIMENSION), compass.search(CompassState.EMPTY, nothingListed, UUID.randomUUID()));
	}

	@Test
	void aTargetFromAnotherDimensionIsKeptAndShownAsOtherDimension() {
		var state = CompassState.EMPTY.withTarget(overworldVillage());

		var arrived = accepted(compass.dimensionChanged(state)).state();

		assertEquals(overworldVillage(), arrived.target());
		assertEquals(Status.OTHER_DIMENSION, arrived.status(END));
	}

	@Test
	void searchStillWorksInADimensionThatIsNotDisabled() {
		var overworld = new SearchContext(OVERWORLD, new BlockXZ(0, 0), Set.of(VILLAGE), 1000, Set.of());

		assertEquals(Outcome.Accepted.class, compass.search(CompassState.EMPTY, overworld, UUID.randomUUID()).getClass());
	}

	private static Outcome.Accepted accepted(Outcome outcome) {
		return (Outcome.Accepted) outcome;
	}

}
