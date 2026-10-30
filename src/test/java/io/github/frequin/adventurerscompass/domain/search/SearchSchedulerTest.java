package io.github.frequin.adventurerscompass.domain.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.github.frequin.adventurerscompass.domain.BlockXZ;
import io.github.frequin.adventurerscompass.domain.ChunkCoord;
import io.github.frequin.adventurerscompass.domain.InstanceKey;

class SearchSchedulerTest {

	private static final String OVERWORLD = "minecraft:overworld";
	private static final long MS = 1_000_000;

	private final FakeClock clock = new FakeClock();
	private final SearchScheduler<String> scheduler = new SearchScheduler<>(clock, 5 * MS);
	private final Map<String, List<SearchResult>> results = new HashMap<>();

	/** A search whose only Instance is at the given chunk; every presence test costs 1 ms. */
	private NearestFirstSearch<FakeWorld.Placement> searchFor(int chunkX, int chunkZ) {
		var huts = new FakeWorld.Placement("huts", 2);
		var world = new FakeWorld(clock, MS).instance("mod:hut", huts, chunkX, chunkZ);
		return new NearestFirstSearch<>(world, new SearchRequest(OVERWORLD, new BlockXZ(0, 0), Set.of("mod:hut"), 1000, Set.of(), Set.of()), clock);
	}

	private void start(String player, NearestFirstSearch<?> search) {
		scheduler.start(player, search, result -> results.computeIfAbsent(player, p -> new ArrayList<>()).add(result));
	}

	private void tickUntilIdle() {
		for (int i = 0; i < 100_000 && scheduler.hasSearches(); i++) {
			scheduler.tick();
		}
	}

	@Test
	void runsASearchOverTicksAndReportsItsResult() {
		start("alex", searchFor(20, 0));

		scheduler.tick();
		assertTrue(scheduler.isSearching("alex"));
		tickUntilIdle();

		assertEquals(List.of(new SearchResult.Found(new InstanceKey(OVERWORLD, "mod:hut", new ChunkCoord(20, 0)), new BlockXZ(320, 0))), results.get("alex"));
		assertFalse(scheduler.isSearching("alex"));
	}

	@Test
	void aNewSearchCancelsThePlayersPreviousOne() {
		start("alex", searchFor(20, 0));
		scheduler.tick();

		start("alex", searchFor(0, 4));
		tickUntilIdle();

		assertEquals(List.of(new SearchResult.Cancelled(), new SearchResult.Found(new InstanceKey(OVERWORLD, "mod:hut", new ChunkCoord(0, 4)), new BlockXZ(0, 64))), results.get("alex"));
	}

	@Test
	void playersSearchIndependently() {
		start("alex", searchFor(20, 0));
		start("steve", searchFor(0, 4));
		tickUntilIdle();

		assertEquals(List.of(new SearchResult.Found(new InstanceKey(OVERWORLD, "mod:hut", new ChunkCoord(20, 0)), new BlockXZ(320, 0))), results.get("alex"));
		assertEquals(List.of(new SearchResult.Found(new InstanceKey(OVERWORLD, "mod:hut", new ChunkCoord(0, 4)), new BlockXZ(0, 64))), results.get("steve"));
	}

	@Test
	void cancellingReportsCancelledAndStopsTheSearch() {
		start("alex", searchFor(20, 0));
		scheduler.tick();

		scheduler.cancel("alex");
		tickUntilIdle();

		assertEquals(List.of(new SearchResult.Cancelled()), results.get("alex"));
		assertFalse(scheduler.isSearching("alex"));
	}

	@Test
	void cancellingWithoutASearchDoesNothing() {
		scheduler.cancel("alex");

		assertEquals(null, results.get("alex"));
	}

	@Test
	void aTickSpendsAtMostItsBudgetAcrossAllPlayers() {
		for (String player : List.of("alex", "steve", "kai")) {
			start(player, searchFor(20, 0));
		}

		while (scheduler.hasSearches()) {
			long before = clock.nanoTime();
			scheduler.tick();
			assertTrue(clock.nanoTime() - before <= 5 * MS, "a tick overran its budget");
		}
	}

	@Test
	void playersTakeTurnsSoNoneIsStarved() {
		List<String> players = List.of("p0", "p1", "p2", "p3", "p4", "p5", "p6", "p7", "p8", "p9");
		for (String player : players) {
			start(player, searchFor(20, 0));
		}

		Map<String, Integer> endTick = new HashMap<>();
		for (int tick = 0; scheduler.hasSearches(); tick++) {
			scheduler.tick();
			for (String player : players) {
				if (results.containsKey(player)) {
					endTick.putIfAbsent(player, tick);
				}
			}
		}

		int first = endTick.values().stream().min(Integer::compare).orElseThrow();
		int last = endTick.values().stream().max(Integer::compare).orElseThrow();
		assertTrue(first > 100, "searches should take many ticks, first ended at " + first);
		assertTrue(last - first <= 2, "identical searches should end together, but ended between ticks " + first + " and " + last);
	}

	@Test
	void aTickWithoutSearchesSpendsNoTime() {
		scheduler.tick();

		assertEquals(0, clock.nanoTime());
	}

}
