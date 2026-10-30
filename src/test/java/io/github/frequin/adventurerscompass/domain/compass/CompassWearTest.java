package io.github.frequin.adventurerscompass.domain.compass;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.frequin.adventurerscompass.domain.BlockXZ;
import io.github.frequin.adventurerscompass.domain.ChunkCoord;
import io.github.frequin.adventurerscompass.domain.InstanceKey;
import io.github.frequin.adventurerscompass.domain.search.SearchResult;

/** Wear (one per found Target, Pass included), the broken compass, and repair. */
class CompassWearTest {

	private static final String OVERWORLD = "minecraft:overworld";
	private static final String VILLAGE = "minecraft:village_plains";
	private static final BlockXZ HERE = new BlockXZ(10, 20);
	private static final int DURABILITY = 3;
	private static final WearContext SURVIVAL = new WearContext(DURABILITY, false);

	private final Compass compass = new Compass();

	private static SearchContext overworld() {
		return new SearchContext(OVERWORLD, HERE, Set.of(VILLAGE), 1000, Set.of(), DURABILITY);
	}

	private static InstanceKey village(int chunkX, int chunkZ) {
		return new InstanceKey(OVERWORLD, VILLAGE, new ChunkCoord(chunkX, chunkZ));
	}

	private static Outcome.Accepted accepted(Outcome outcome) {
		return (Outcome.Accepted) outcome;
	}

	/** "Search" then a Found result for the village at chunk ({@code n}, {@code n}). */
	private CompassState searchAndFind(CompassState state, int n, WearContext wear) {
		var id = UUID.randomUUID();
		var searching = accepted(compass.search(state, overworld(), id)).state();
		return compass.searchEnded(searching, id, new SearchResult.Found(village(n, n), new BlockXZ(16 * n, 16 * n)), wear);
	}

	@Test
	void eachFoundTargetWearsTheCompassByOne() {
		var once = searchAndFind(CompassState.EMPTY, 1, SURVIVAL);
		var twice = searchAndFind(once, 2, SURVIVAL);

		assertEquals(2, once.searchesLeft(DURABILITY));
		assertEquals(1, twice.searchesLeft(DURABILITY));
	}

	@Test
	void aTargetFoundByPassWearsTheCompassToo() {
		var target = searchAndFind(CompassState.EMPTY, 1, SURVIVAL);
		var id = UUID.randomUUID();
		var passing = accepted(compass.pass(target, overworld(), id)).state();

		var after = compass.searchEnded(passing, id, new SearchResult.Found(village(2, 2), new BlockXZ(32, 32)), SURVIVAL);

		assertEquals(1, after.searchesLeft(DURABILITY));
	}

	@Test
	void aTargetFoundInCreativeDoesNotWearTheCompass() {
		var after = searchAndFind(CompassState.EMPTY, 1, new WearContext(DURABILITY, true));

		assertEquals(DURABILITY, after.searchesLeft(DURABILITY));
	}

	@Test
	void searchesThatFindNothingOrAreCancelledDoNotWearTheCompass() {
		var id = UUID.randomUUID();
		var searching = accepted(compass.search(CompassState.EMPTY, overworld(), id)).state();

		var nothing = compass.searchEnded(searching, id, new SearchResult.NothingFound(OVERWORLD, 1000, 0, 0), SURVIVAL);
		var cancelled = compass.searchEnded(searching, id, new SearchResult.Cancelled(), SURVIVAL);
		var cancelledByHand = accepted(compass.cancel(searching)).state();

		assertEquals(DURABILITY, nothing.searchesLeft(DURABILITY));
		assertEquals(DURABILITY, cancelled.searchesLeft(DURABILITY));
		assertEquals(DURABILITY, cancelledByHand.searchesLeft(DURABILITY));
	}

	@Test
	void wearSurvivesSelectionAndDimensionChanges() {
		var worn = searchAndFind(CompassState.EMPTY, 1, SURVIVAL);

		assertEquals(2, accepted(compass.selectionChanged(worn)).state().searchesLeft(DURABILITY));
		assertEquals(2, accepted(compass.dimensionChanged(worn)).state().searchesLeft(DURABILITY));
	}

	@Test
	void teleportingDoesNotWearTheCompass() {
		var worn = searchAndFind(CompassState.EMPTY, 1, SURVIVAL);

		var after = accepted(compass.teleport(worn, new TeleportContext(OVERWORLD, true, true, false))).state();

		assertEquals(2, after.searchesLeft(DURABILITY));
	}

	@Test
	void theCompassBreaksAfterDurabilityFoundTargets() {
		var state = CompassState.EMPTY;
		for (int n = 1; n <= DURABILITY; n++) {
			assertEquals(false, state.isBroken(DURABILITY));
			state = searchAndFind(state, n, SURVIVAL);
		}

		assertEquals(true, state.isBroken(DURABILITY));
		assertEquals(0, state.searchesLeft(DURABILITY));
	}

	@Test
	void aBrokenCompassKeepsItsTargetButRefusesSearchAndPass() {
		var broken = brokenWithTarget();

		assertEquals(new Outcome.Refused(Refusal.BROKEN), compass.search(broken, overworld(), UUID.randomUUID()));
		assertEquals(new Outcome.Refused(Refusal.BROKEN), compass.pass(broken, overworld(), UUID.randomUUID()));
		assertEquals(new Target(village(3, 3), new BlockXZ(48, 48)), broken.target());
		assertEquals(Status.TARGET, broken.status(OVERWORLD));
	}

	@Test
	void withoutDurabilityTheCompassNeverWearsNorBreaks() {
		var unbreakable = new WearContext(0, false);
		var state = CompassState.EMPTY;
		for (int n = 1; n <= 5; n++) {
			state = searchAndFind(state, n, unbreakable);
		}
		var noDurability = new SearchContext(OVERWORLD, HERE, Set.of(VILLAGE), 1000, Set.of(), 0);

		assertEquals(false, state.isBroken(0));
		assertEquals(Outcome.Accepted.class, compass.search(state, noDurability, UUID.randomUUID()).getClass());
	}

	@Test
	void repairResetsWearAndKeepsTheTargetSkippedAndSearch() {
		var broken = brokenWithTarget().withSkipped(Set.of(village(1, 1))).withSearch(UUID.randomUUID());

		var repaired = compass.repair(broken);

		assertEquals(DURABILITY, repaired.searchesLeft(DURABILITY));
		assertEquals(broken.withWear(0), repaired);
	}

	@Test
	void aRepairedCompassSearchesAgain() {
		var repaired = compass.repair(brokenWithTarget());

		assertEquals(Outcome.Accepted.class, compass.search(repaired, overworld(), UUID.randomUUID()).getClass());
	}

	private CompassState brokenWithTarget() {
		var state = CompassState.EMPTY;
		for (int n = 1; n <= DURABILITY; n++) {
			state = searchAndFind(state, n, SURVIVAL);
		}
		return state;
	}


	@Test
	void markVisitedDoesNotWearTheCompass() {
		var state = searchAndFind(CompassState.EMPTY, 1, SURVIVAL);

		var outcome = accepted(compass.markVisited(state));

		assertEquals(state.wear(), outcome.state().wear());
	}

}
