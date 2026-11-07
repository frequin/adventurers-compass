package io.github.frequin.adventurerscompass.domain.compass;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.frequin.adventurerscompass.domain.BlockXZ;
import io.github.frequin.adventurerscompass.domain.ChunkCoord;
import io.github.frequin.adventurerscompass.domain.InstanceKey;
import io.github.frequin.adventurerscompass.domain.search.SearchResult;

/** The Visited rules of the compass: a Target that becomes Visited, "Mark Visited". */
class CompassVisitedTest {

	private static final String OVERWORLD = "minecraft:overworld";
	private static final String NETHER = "minecraft:the_nether";
	private static final String VILLAGE = "minecraft:village_plains";
	private static final BlockXZ HERE = new BlockXZ(10, 20);

	private final Compass compass = new Compass();

	private static InstanceKey village(int chunkX, int chunkZ) {
		return new InstanceKey(OVERWORLD, VILLAGE, new ChunkCoord(chunkX, chunkZ));
	}

	private static Outcome.Accepted accepted(Outcome outcome) {
		return (Outcome.Accepted) outcome;
	}

	private CompassState withTarget() {
		var id = UUID.randomUUID();
		var context = new SearchContext(OVERWORLD, HERE, Set.of(VILLAGE), 1000, Set.of());
		var searching = accepted(compass.search(CompassState.EMPTY, context, id)).state();
		return compass.searchEnded(searching, id, new SearchResult.Found(village(3, 4), new BlockXZ(48, 64)));
	}

	@Test
	void aTargetThatBecomesVisitedIsKeptWithTheVisitedStatusAndNoNewSearch() {
		var target = withTarget();

		var outcome = accepted(compass.instanceVisited(target, village(3, 4)));

		assertEquals(target, outcome.state());
		assertEquals(List.of(new Effect.TargetVisited(village(3, 4))), outcome.effects());
		assertEquals(Status.TARGET_VISITED, outcome.state().status(OVERWORLD, Set.of(village(3, 4))));
	}

	@Test
	void anotherInstanceBecomingVisitedLeavesTheTargetAsItIs() {
		var target = withTarget();

		var outcome = accepted(compass.instanceVisited(target, village(9, 9)));

		assertEquals(target, outcome.state());
		assertEquals(List.of(), outcome.effects());
		assertEquals(Status.TARGET, outcome.state().status(OVERWORLD, Set.of(village(9, 9))));
	}

	@Test
	void unmarkingTheTargetMakesItAPlainTargetAgain() {
		assertEquals(Status.TARGET, withTarget().status(OVERWORLD, Set.of()));
	}

	@Test
	void aVisitedTargetInAnotherDimensionShowsAsInAnotherDimension() {
		assertEquals(Status.OTHER_DIMENSION, withTarget().status(NETHER, Set.of(village(3, 4))));
	}

	@Test
	void markVisitedMarksTheTargetWithoutTouchingTheCompass() {
		var target = withTarget();

		var outcome = accepted(compass.markVisited(target));

		assertEquals(target, outcome.state());
		assertEquals(List.of(new Effect.MarkVisited(village(3, 4))), outcome.effects());
	}

	@Test
	void markVisitedWorksForATargetInAnotherDimension() {
		var target = withTarget();

		var outcome = accepted(compass.markVisited(accepted(compass.dimensionChanged(target)).state()));

		assertEquals(List.of(new Effect.MarkVisited(village(3, 4))), outcome.effects());
	}

	@Test
	void markVisitedIsRefusedWithoutATarget() {
		assertEquals(new Outcome.Refused(Refusal.NO_TARGET), compass.markVisited(CompassState.EMPTY));
	}

}
