package io.github.frequin.adventurerscompass.domain.compass;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.frequin.adventurerscompass.domain.BlockXZ;
import io.github.frequin.adventurerscompass.domain.ChunkCoord;
import io.github.frequin.adventurerscompass.domain.InstanceKey;
import io.github.frequin.adventurerscompass.domain.search.FakeClock;
import io.github.frequin.adventurerscompass.domain.search.FakeWorld;
import io.github.frequin.adventurerscompass.domain.search.NearestFirstSearch;
import io.github.frequin.adventurerscompass.domain.search.SearchRequest;
import io.github.frequin.adventurerscompass.domain.search.SearchResult;

class CompassTest {

	private static final String OVERWORLD = "minecraft:overworld";
	private static final String NETHER = "minecraft:the_nether";
	private static final String VILLAGE = "minecraft:village_plains";
	private static final BlockXZ HERE = new BlockXZ(10, 20);

	private final Compass compass = new Compass();

	private static SearchContext overworld() {
		return new SearchContext(OVERWORLD, HERE, Set.of(VILLAGE), 1000, Set.of());
	}

	private static InstanceKey village(int chunkX, int chunkZ) {
		return new InstanceKey(OVERWORLD, VILLAGE, new ChunkCoord(chunkX, chunkZ));
	}

	private static Outcome.Accepted accepted(Outcome outcome) {
		return (Outcome.Accepted) outcome;
	}

	@Test
	void searchClearsSkippedAndStartsASearchOfTheSearchableStructures() {
		var state = CompassState.EMPTY.withSkipped(Set.of(village(1, 1)));
		var id = UUID.randomUUID();

		var outcome = accepted(compass.search(state, overworld(), id));

		assertEquals(Set.of(), outcome.state().skipped());
		assertEquals(id, outcome.state().search());
		assertEquals(List.of(new Effect.StartSearch(id, new SearchRequest(OVERWORLD, HERE, Set.of(VILLAGE), 1000, Set.of(), Set.of()))), outcome.effects());
	}

	@Test
	void theInstanceFoundBecomesTheTargetAndTheSearchEnds() {
		var id = UUID.randomUUID();
		var searching = accepted(compass.search(CompassState.EMPTY, overworld(), id)).state();

		var found = compass.searchEnded(searching, id, new SearchResult.Found(village(3, 4), new BlockXZ(48, 64)));

		assertEquals(new Target(village(3, 4), new BlockXZ(48, 64)), found.target());
		assertEquals(false, found.isSearching());
		assertEquals(Status.TARGET, found.status(OVERWORLD));
	}

	@Test
	void aNewSearchCancelsThePreviousOne() {
		var first = UUID.randomUUID();
		var second = UUID.randomUUID();
		var searching = accepted(compass.search(CompassState.EMPTY, overworld(), first)).state();

		var outcome = accepted(compass.search(searching, overworld(), second));

		assertEquals(new Effect.CancelSearch(first), outcome.effects().get(0));
		assertEquals(second, ((Effect.StartSearch) outcome.effects().get(1)).id());
		assertEquals(second, outcome.state().search());
	}

	@Test
	void aLateResultOfAReplacedSearchIsIgnored() {
		var first = UUID.randomUUID();
		var second = UUID.randomUUID();
		var searching = accepted(compass.search(accepted(compass.search(CompassState.EMPTY, overworld(), first)).state(), overworld(), second)).state();

		var after = compass.searchEnded(searching, first, new SearchResult.Found(village(3, 4), new BlockXZ(48, 64)));

		assertEquals(searching, after);
	}

	private CompassState withTarget() {
		var id = UUID.randomUUID();
		var searching = accepted(compass.search(CompassState.EMPTY, overworld(), id)).state();
		return compass.searchEnded(searching, id, new SearchResult.Found(village(3, 4), new BlockXZ(48, 64)));
	}

	@Test
	void cancelStopsTheSearchAndKeepsTheTarget() {
		var id = UUID.randomUUID();
		var target = withTarget();
		var searching = accepted(compass.search(target, overworld(), id)).state();

		var outcome = accepted(compass.cancel(searching));

		assertEquals(List.of(new Effect.CancelSearch(id)), outcome.effects());
		assertEquals(false, outcome.state().isSearching());
		assertEquals(target.target(), outcome.state().target());
	}

	@Test
	void cancelWithoutASearchChangesNothing() {
		var target = withTarget();

		assertEquals(new Outcome.Accepted(target, List.of()), compass.cancel(target));
	}

	@Test
	void aSearchCancelledFromOutsideLeavesTheCompassIdle() {
		var id = UUID.randomUUID();
		var searching = accepted(compass.search(CompassState.EMPTY, overworld(), id)).state();

		var after = compass.searchEnded(searching, id, new SearchResult.Cancelled());

		assertEquals(Status.IDLE, after.status(OVERWORLD));
	}

	@Test
	void passAddsTheTargetToSkippedAndSearchesAgain() {
		var state = withTarget().withSkipped(Set.of(village(1, 1)));
		var id = UUID.randomUUID();

		var outcome = accepted(compass.pass(state, overworld(), id));

		assertEquals(Set.of(village(1, 1), village(3, 4)), outcome.state().skipped());
		assertEquals(List.of(new Effect.StartSearch(id, new SearchRequest(OVERWORLD, HERE, Set.of(VILLAGE), 1000, Set.of(), Set.of(village(1, 1), village(3, 4))))), outcome.effects());
	}

	@Test
	void passWithoutATargetIsRefused() {
		assertEquals(new Outcome.Refused(Refusal.NO_TARGET), compass.pass(CompassState.EMPTY, overworld(), UUID.randomUUID()));
	}

	@Test
	void changingTheSelectionDuringASearchCancelsItClearsTheTargetAndSkippedWithoutRestarting() {
		var id = UUID.randomUUID();
		var searching = accepted(compass.search(withTarget(), overworld(), id)).state().withSkipped(Set.of(village(1, 1)));

		var outcome = accepted(compass.selectionChanged(searching));

		assertEquals(List.of(new Effect.CancelSearch(id)), outcome.effects());
		assertEquals(CompassState.EMPTY, outcome.state());
	}

	@Test
	void changingTheSelectionWithoutASearchClearsTheTargetAndSkipped() {
		var outcome = accepted(compass.selectionChanged(withTarget().withSkipped(Set.of(village(1, 1)))));

		assertEquals(List.of(), outcome.effects());
		assertEquals(CompassState.EMPTY, outcome.state());
	}

	@Test
	void changingDimensionCancelsTheSearchAndKeepsTheTargetAndSkipped() {
		var id = UUID.randomUUID();
		var target = withTarget();
		var searching = new CompassState(id, target.target(), Set.of(village(1, 1)), null);

		var outcome = accepted(compass.dimensionChanged(searching));

		assertEquals(List.of(new Effect.CancelSearch(id)), outcome.effects());
		assertEquals(new CompassState(null, target.target(), Set.of(village(1, 1)), null), outcome.state());
	}

	@Test
	void aTargetInAnotherDimensionIsShownAsSuchAndBecomesActiveAgainOnReturn() {
		var inNether = accepted(compass.dimensionChanged(withTarget())).state();

		assertEquals(Status.OTHER_DIMENSION, inNether.status(NETHER));
		var back = accepted(compass.dimensionChanged(inNether));
		assertEquals(List.of(), back.effects());
		assertEquals(Status.TARGET, back.state().status(OVERWORLD));
	}

	private static SearchContext nether() {
		return new SearchContext(NETHER, HERE, Set.of("minecraft:fortress"), 1000, Set.of());
	}

	@Test
	void searchFromAnotherDimensionSearchesTheCurrentDimensionAndReplacesTheTarget() {
		var id = UUID.randomUUID();
		var outcome = accepted(compass.search(withTarget(), nether(), id));
		assertEquals(NETHER, ((Effect.StartSearch) outcome.effects().get(0)).request().dimension());

		var fortress = new InstanceKey(NETHER, "minecraft:fortress", new ChunkCoord(2, 2));
		var found = compass.searchEnded(outcome.state(), id, new SearchResult.Found(fortress, new BlockXZ(32, 32)));

		assertEquals(new Target(fortress, new BlockXZ(32, 32)), found.target());
		assertEquals(Status.TARGET, found.status(NETHER));
	}

	@Test
	void passFromAnotherDimensionSearchesTheCurrentDimensionAndReplacesTheTarget() {
		var id = UUID.randomUUID();
		var outcome = accepted(compass.pass(withTarget(), nether(), id));
		var request = ((Effect.StartSearch) outcome.effects().get(0)).request();
		assertEquals(NETHER, request.dimension());
		assertEquals(Set.of(village(3, 4)), request.skipped());

		var fortress = new InstanceKey(NETHER, "minecraft:fortress", new ChunkCoord(2, 2));
		var found = compass.searchEnded(outcome.state(), id, new SearchResult.Found(fortress, new BlockXZ(32, 32)));

		assertEquals(new Target(fortress, new BlockXZ(32, 32)), found.target());
	}

	@Test
	void nothingFoundIsKeptForTheHudInPlaceOfTheTarget() {
		var id = UUID.randomUUID();
		var searching = accepted(compass.pass(withTarget(), overworld(), id)).state();
		var nothing = new SearchResult.NothingFound(OVERWORLD, 1000, 0, 1);

		var after = compass.searchEnded(searching, id, nothing);

		assertEquals(nothing, after.nothingFound());
		assertEquals(null, after.target());
		assertEquals(Status.NOTHING_FOUND, after.status(OVERWORLD));
	}

	@Test
	void theNextSearchForgetsNothingFound() {
		var id = UUID.randomUUID();
		var nothing = CompassState.EMPTY.withNothingFound(new SearchResult.NothingFound(OVERWORLD, 1000, 0, 0));

		var searching = accepted(compass.search(nothing, overworld(), id)).state();
		var cancelled = accepted(compass.cancel(searching)).state();

		assertEquals(Status.IDLE, cancelled.status(OVERWORLD));
	}

	@Test
	void searchAndPassAreRefusedWhenNothingIsSearchableHere() {
		var empty = new SearchContext(NETHER, HERE, Set.of(), 1000, Set.of());
		var target = withTarget();

		assertEquals(new Outcome.Refused(Refusal.NOTHING_SEARCHABLE), compass.search(target, empty, UUID.randomUUID()));
		assertEquals(new Outcome.Refused(Refusal.NOTHING_SEARCHABLE), compass.pass(target, empty, UUID.randomUUID()));
	}

	@Test
	void theSkippedOfAPassAreSetAsideByTheSearchAndCountedInNothingFound() {
		var villages = new FakeWorld.Placement("villages", 34);
		var world = new FakeWorld().instance(VILLAGE, villages, 3, 4);
		var id = UUID.randomUUID();
		var passing = accepted(compass.pass(withTarget(), overworld(), id));
		var request = ((Effect.StartSearch) passing.effects().get(0)).request();

		var result = runToEnd(new NearestFirstSearch<>(world, request, new FakeClock()));
		var after = compass.searchEnded(passing.state(), id, result);

		assertEquals(new SearchResult.NothingFound(OVERWORLD, 1000, 0, 1), after.nothingFound());
	}

	private static SearchResult runToEnd(NearestFirstSearch<?> search) {
		while (true) {
			var result = search.step(Long.MAX_VALUE);
			if (result.isPresent()) {
				return result.get();
			}
		}
	}

}
