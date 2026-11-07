package io.github.frequin.adventurerscompass.domain.compass;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.frequin.adventurerscompass.domain.BlockXZ;
import io.github.frequin.adventurerscompass.domain.ChunkCoord;
import io.github.frequin.adventurerscompass.domain.InstanceKey;
import io.github.frequin.adventurerscompass.domain.search.SearchRequest;
import io.github.frequin.adventurerscompass.domain.search.SearchResult;

/** Removing one Skipped Instance and clearing them all, from the Skipped tab of the screen. */
class CompassSkippedTest {

	private static final String OVERWORLD = "minecraft:overworld";
	private static final String VILLAGE = "minecraft:village_plains";
	private static final BlockXZ HERE = new BlockXZ(10, 20);

	private static final InstanceKey FIRST = village(1, 1);
	private static final InstanceKey SECOND = village(2, 2);
	private static final Target TARGET = new Target(village(3, 3), new BlockXZ(56, 56));
	private static final NothingFoundMessage SHOWN = new NothingFoundMessage(new SearchResult.NothingFound(OVERWORLD, 1000, 0, 2), 7L);

	private final Compass compass = new Compass();

	private static InstanceKey village(int chunkX, int chunkZ) {
		return new InstanceKey(OVERWORLD, VILLAGE, new ChunkCoord(chunkX, chunkZ));
	}

	private static Outcome.Accepted accepted(Outcome outcome) {
		return (Outcome.Accepted) outcome;
	}

	@Test
	void removingASkippedInstanceLeavesTheOthers() {
		var state = new CompassState(null, null, Set.of(FIRST, SECOND), null);

		var outcome = accepted(compass.removeSkipped(state, FIRST));

		assertEquals(Set.of(SECOND), outcome.state().skipped());
		assertEquals(List.of(), outcome.effects());
	}

	@Test
	void aRemovedInstanceIsNoLongerSetAsideByPass() {
		var id = UUID.randomUUID();
		var state = new CompassState(null, TARGET, Set.of(FIRST, SECOND), null);

		var removed = accepted(compass.removeSkipped(state, FIRST)).state();
		var pass = accepted(compass.pass(removed, new SearchContext(OVERWORLD, HERE, Set.of(VILLAGE), 1000, Set.of()), id));

		SearchRequest request = ((Effect.StartSearch) pass.effects().getLast()).request();
		assertEquals(Set.of(SECOND, TARGET.instance()), request.skipped());
	}

	@Test
	void removingAnInstanceThatIsNotSkippedChangesNothing() {
		var state = new CompassState(null, null, Set.of(SECOND), null);

		assertEquals(state, accepted(compass.removeSkipped(state, FIRST)).state());
	}

	@Test
	void clearingEmptiesSkipped() {
		var state = new CompassState(null, null, Set.of(FIRST, SECOND), null);

		var outcome = accepted(compass.clearSkipped(state));

		assertEquals(Set.of(), outcome.state().skipped());
		assertEquals(List.of(), outcome.effects());
	}

	@Test
	void neitherCommandTouchesTheRunningSearchTheTargetTheWearOrTheLastNothingFound() {
		var search = UUID.randomUUID();
		var state = new CompassState(search, TARGET, Set.of(FIRST, SECOND), SHOWN, 2);

		var removed = accepted(compass.removeSkipped(state, FIRST));
		var cleared = accepted(compass.clearSkipped(state));

		assertEquals(new CompassState(search, TARGET, Set.of(SECOND), SHOWN, 2), removed.state());
		assertEquals(new CompassState(search, TARGET, Set.of(), SHOWN, 2), cleared.state());
		assertEquals(List.of(), removed.effects());
		assertEquals(List.of(), cleared.effects());
	}

}
