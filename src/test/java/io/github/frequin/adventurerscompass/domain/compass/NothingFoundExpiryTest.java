package io.github.frequin.adventurerscompass.domain.compass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.frequin.adventurerscompass.domain.BlockXZ;
import io.github.frequin.adventurerscompass.domain.ChunkCoord;
import io.github.frequin.adventurerscompass.domain.InstanceKey;
import io.github.frequin.adventurerscompass.domain.search.FakeClock;
import io.github.frequin.adventurerscompass.domain.search.SearchResult;

class NothingFoundExpiryTest {

	private static final String OVERWORLD = "minecraft:overworld";
	private static final String VILLAGE = "minecraft:village_plains";
	private static final BlockXZ HERE = new BlockXZ(10, 20);
	private static final SearchResult.NothingFound NOTHING = new SearchResult.NothingFound(OVERWORLD, 1000, 3, 2);
	private static final long ONE_SECOND = 1_000_000_000L;
	private static final boolean HELD = true;

	private final FakeClock clock = new FakeClock();
	private final Compass compass = new Compass(Compass.DEFAULT_RULES, clock);

	private static SearchContext overworld() {
		return new SearchContext(OVERWORLD, HERE, Set.of(VILLAGE), 1000, Set.of());
	}

	private static CompassState withTarget() {
		var instance = new InstanceKey(OVERWORLD, VILLAGE, new ChunkCoord(3, 4));
		return CompassState.EMPTY.withTarget(new Target(instance, new BlockXZ(48, 64)));
	}

	/** The compass after a search that found nothing, at the clock's current time. */
	private CompassState nothingFound(CompassState from) {
		var id = UUID.randomUUID();
		var searching = ((Outcome.Accepted) compass.search(from, overworld(), id)).state();
		return compass.searchEnded(searching, id, NOTHING);
	}

	@Test
	void afterFiveSecondsThePreviousTargetIsBack() {
		var nothing = nothingFound(withTarget());

		clock.advance(5 * ONE_SECOND);
		var after = compass.tick(nothing, HELD);

		assertEquals(Status.TARGET, after.status(OVERWORLD));
		assertEquals(withTarget().target(), after.target());
	}

	@Test
	void theMessageStaysUntilTheFiveSecondsAreUp() {
		var nothing = nothingFound(withTarget());

		clock.advance(5 * ONE_SECOND - 1);

		assertEquals(Status.NOTHING_FOUND, compass.tick(nothing, HELD).status(OVERWORLD));
	}

	@Test
	void afterFiveSecondsWithoutATargetNothingShows() {
		var nothing = nothingFound(CompassState.EMPTY);

		clock.advance(5 * ONE_SECOND);

		assertEquals(Status.IDLE, compass.tick(nothing, HELD).status(OVERWORLD));
	}

	@Test
	void puttingTheCompassAwayEndsTheMessageForGood() {
		var nothing = nothingFound(withTarget());

		clock.advance(ONE_SECOND);
		var away = compass.tick(nothing, !HELD);
		clock.advance(ONE_SECOND);
		var backInHand = compass.tick(away, HELD);

		assertEquals(Status.TARGET, backInHand.status(OVERWORLD));
	}

	@Test
	void aRecentMessageOutOfTheHandIsForgottenAtOnceAsWhenThePlayerReconnects() {
		var nothing = nothingFound(withTarget());

		assertEquals(Status.TARGET, compass.tick(nothing, !HELD).status(OVERWORLD));
	}

	@Test
	void aMessageSavedByAnEarlierRunWhoseClockWasAheadIsForgotten() {
		var saved = withTarget().withNothingFound(new NothingFoundMessage(NOTHING, 100 * ONE_SECOND));

		assertEquals(Status.TARGET, compass.tick(saved, HELD).status(OVERWORLD));
	}

	@Test
	void aMessageWithoutATimeIsForgotten() {
		var saved = withTarget().withNothingFound(new NothingFoundMessage(NOTHING, null));

		assertEquals(Status.TARGET, compass.tick(saved, HELD).status(OVERWORLD));
	}

	@Test
	void aNewNothingFoundGetsItsOwnFiveSeconds() {
		var first = nothingFound(withTarget());

		clock.advance(4 * ONE_SECOND);
		var second = nothingFound(first);
		clock.advance(4 * ONE_SECOND);

		assertEquals(Status.NOTHING_FOUND, compass.tick(second, HELD).status(OVERWORLD));
	}

	@Test
	void aSelectionChangeEndsTheMessageAtOnce() {
		var nothing = nothingFound(CompassState.EMPTY);

		var changed = ((Outcome.Accepted) compass.selectionChanged(nothing)).state();

		assertEquals(Status.IDLE, changed.status(OVERWORLD));
	}

	@Test
	void aTickWithNothingToForgetKeepsTheState() {
		var target = withTarget();

		assertSame(target, compass.tick(target, !HELD));
	}

}
