package io.github.frequin.adventurerscompass.domain.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.github.frequin.adventurerscompass.domain.BlockXZ;
import io.github.frequin.adventurerscompass.domain.ChunkCoord;
import io.github.frequin.adventurerscompass.domain.InstanceKey;

class NearestFirstSearchTest {

	private static final String OVERWORLD = "minecraft:overworld";
	private static final long UNLIMITED = Long.MAX_VALUE;

	private static SearchResult runToEnd(NearestFirstSearch<?> search) {
		while (true) {
			var result = search.step(UNLIMITED);
			if (result.isPresent()) {
				return result.get();
			}
		}
	}

	private static NearestFirstSearch<FakeWorld.Placement> search(FakeWorld world, BlockXZ origin, int maxRadius, Set<String> structures) {
		return new NearestFirstSearch<>(world, new SearchRequest(OVERWORLD, origin, structures, maxRadius, Set.of(), Set.of()), new FakeClock());
	}

	@Test
	void findsTheOnlyInstanceOfAStructure() {
		var villages = new FakeWorld.Placement("villages", 34);
		var world = new FakeWorld().instance("minecraft:village_plains", villages, 10, -5);

		var result = runToEnd(search(world, new BlockXZ(0, 0), 1000, Set.of("minecraft:village_plains")));

		assertEquals(new SearchResult.Found(new InstanceKey(OVERWORLD, "minecraft:village_plains", new ChunkCoord(10, -5)), new BlockXZ(160, -80)), result);
	}

	@Test
	void findsTheNearestInstanceAcrossPlacementsWithDifferentSpacings() {
		var small = new FakeWorld.Placement("small", 10);
		var large = new FakeWorld.Placement("large", 40);
		var world = new FakeWorld()
				// 400 blocks away, in the third ring of cells of the small placement
				.instance("mod:hut", small, 25, 0)
				// 800 blocks away, in the second ring of cells of the large placement
				.instance("mod:tower", large, 40, 30);

		var result = runToEnd(search(world, new BlockXZ(0, 0), 1000, Set.of("mod:hut", "mod:tower")));

		assertEquals(new SearchResult.Found(new InstanceKey(OVERWORLD, "mod:hut", new ChunkCoord(25, 0)), new BlockXZ(400, 0)), result);
	}

	@Test
	void prefersTheNearestInstanceOverAnEarlierRingCorner() {
		var huts = new FakeWorld.Placement("huts", 10);
		var world = new FakeWorld()
				// ring 1, corner cell: about 430 blocks away
				.instance("mod:hut", huts, 19, 19)
				// ring 3, edge cell: 400 blocks away
				.instance("mod:hut", huts, -25, 0);

		var result = runToEnd(search(world, new BlockXZ(0, 0), 1000, Set.of("mod:hut")));

		assertEquals(new SearchResult.Found(new InstanceKey(OVERWORLD, "mod:hut", new ChunkCoord(-25, 0)), new BlockXZ(-400, 0)), result);
	}

	@Test
	void searchesEveryStructureThatSharesAPlacement() {
		var villages = new FakeWorld.Placement("villages", 34);
		var world = new FakeWorld()
				.instance("minecraft:village_plains", villages, 40, 40)
				.instance("minecraft:village_desert", villages, 5, 3);

		var result = runToEnd(search(world, new BlockXZ(0, 0), 1000, Set.of("minecraft:village_plains", "minecraft:village_desert")));

		assertEquals(new SearchResult.Found(new InstanceKey(OVERWORLD, "minecraft:village_desert", new ChunkCoord(5, 3)), new BlockXZ(80, 48)), result);
	}

	@Test
	void measuresDistanceFromTheOrigin() {
		var huts = new FakeWorld.Placement("huts", 10);
		var world = new FakeWorld()
				.instance("mod:hut", huts, 2, 0)
				.instance("mod:hut", huts, 95, 0);

		var result = runToEnd(search(world, new BlockXZ(1500, 0), 1000, Set.of("mod:hut")));

		assertEquals(new SearchResult.Found(new InstanceKey(OVERWORLD, "mod:hut", new ChunkCoord(95, 0)), new BlockXZ(1520, 0)), result);
	}


	@Test
	void ignoresInstancesBeyondTheMaxRadius() {
		var huts = new FakeWorld.Placement("huts", 10);
		var world = new FakeWorld().instance("mod:hut", huts, 63, 0);

		var result = runToEnd(search(world, new BlockXZ(0, 0), 1000, Set.of("mod:hut")));

		assertEquals(new SearchResult.NothingFound(OVERWORLD, 1000, 0, 0), result);
	}

	@Test
	void findsAnInstanceExactlyAtTheMaxRadius() {
		var huts = new FakeWorld.Placement("huts", 10);
		var world = new FakeWorld().instance("mod:hut", huts, 0, 62);

		var result = runToEnd(search(world, new BlockXZ(0, 8), 984, Set.of("mod:hut")));

		assertEquals(new SearchResult.Found(new InstanceKey(OVERWORLD, "mod:hut", new ChunkCoord(0, 62)), new BlockXZ(0, 992)), result);
	}

	@Test
	void findsNothingWhenNoSearchedStructureGeneratesInTheDimension() {
		var result = runToEnd(search(new FakeWorld(), new BlockXZ(0, 0), 1000, Set.of("minecraft:bastion_remnant")));

		assertEquals(new SearchResult.NothingFound(OVERWORLD, 1000, 0, 0), result);
	}

	@Test
	void setsVisitedAndSkippedInstancesAsideAndCountsThem() {
		var huts = new FakeWorld.Placement("huts", 10);
		var world = new FakeWorld()
				.instance("mod:hut", huts, 5, 0)
				.instance("mod:hut", huts, 15, 0)
				.instance("mod:hut", huts, 0, 25)
				.instance("mod:hut", huts, 45, 0);
		var visited = Set.of(key("mod:hut", 5, 0), key("mod:hut", 0, 25));
		var skipped = Set.of(key("mod:hut", 15, 0));

		var result = runToEnd(new NearestFirstSearch<>(world, new SearchRequest(OVERWORLD, new BlockXZ(0, 0), Set.of("mod:hut"), 1000, visited, skipped), new FakeClock()));

		assertEquals(new SearchResult.Found(key("mod:hut", 45, 0), new BlockXZ(720, 0)), result);
		assertEquals(Set.of(new ChunkCoord(45, 0)), world.testedInstanceChunks());
	}

	@Test
	void reportsTheSetAsideCountsWhenNothingIsLeft() {
		var huts = new FakeWorld.Placement("huts", 10);
		var world = new FakeWorld()
				.instance("mod:hut", huts, 5, 0)
				.instance("mod:hut", huts, 15, 0)
				.instance("mod:hut", huts, 0, 25)
				// Visited but out of range: not counted
				.instance("mod:hut", huts, 95, 0);
		var visited = Set.of(key("mod:hut", 5, 0), key("mod:hut", 0, 25), key("mod:hut", 95, 0));
		var skipped = Set.of(key("mod:hut", 15, 0));

		var result = runToEnd(new NearestFirstSearch<>(world, new SearchRequest(OVERWORLD, new BlockXZ(0, 0), Set.of("mod:hut"), 1000, visited, skipped), new FakeClock()));

		assertEquals(new SearchResult.NothingFound(OVERWORLD, 1000, 2, 1), result);
	}

	@Test
	void setsAsideOnlyTheVisitedStructureWhenTwoShareAStartChunk() {
		var shared = new FakeWorld.Placement("shared", 10);
		var world = new FakeWorld()
				.instance("mod:hut", shared, 5, 0)
				.instance("mod:hut", shared, 25, 0)
				.instance("mod:tower", shared, 25, 0);

		var result = runToEnd(new NearestFirstSearch<>(world, new SearchRequest(OVERWORLD, new BlockXZ(0, 0), Set.of("mod:tower"), 1000, Set.of(key("mod:hut", 25, 0)), Set.of()), new FakeClock()));

		assertEquals(new SearchResult.Found(key("mod:tower", 25, 0), new BlockXZ(400, 0)), result);
	}

	@Test
	void eachStepStopsOnceItsBudgetIsSpent() {
		var clock = new FakeClock();
		var huts = new FakeWorld.Placement("huts", 2);
		// Every presence test costs 1 ms; the only Instance is far away.
		var world = new FakeWorld(clock, 1_000_000).instance("mod:hut", huts, 50, 0);
		var search = new NearestFirstSearch<>(world, new SearchRequest(OVERWORLD, new BlockXZ(0, 0), Set.of("mod:hut"), 1000, Set.of(), Set.of()), clock);

		int steps = 0;
		SearchResult result = null;
		while (result == null) {
			long before = clock.nanoTime();
			result = search.step(3_000_000).orElse(null);
			assertTrue(clock.nanoTime() - before <= 3_000_000, "a step overran its budget");
			steps++;
		}

		assertEquals(new SearchResult.Found(key("mod:hut", 50, 0), new BlockXZ(800, 0)), result);
		assertTrue(steps > 1, "the search should need several steps");
	}

	@Test
	void aCancelledSearchEndsAsCancelled() {
		var clock = new FakeClock();
		var huts = new FakeWorld.Placement("huts", 2);
		var world = new FakeWorld(clock, 1_000_000).instance("mod:hut", huts, 50, 0);
		var search = new NearestFirstSearch<>(world, new SearchRequest(OVERWORLD, new BlockXZ(0, 0), Set.of("mod:hut"), 1000, Set.of(), Set.of()), clock);

		assertEquals(Optional.empty(), search.step(3_000_000));
		search.cancel();

		assertEquals(Optional.of(new SearchResult.Cancelled()), search.step(3_000_000));
	}

	@Test
	void cancellingAnEndedSearchKeepsItsResult() {
		var huts = new FakeWorld.Placement("huts", 10);
		var world = new FakeWorld().instance("mod:hut", huts, 5, 0);
		var search = search(world, new BlockXZ(0, 0), 1000, Set.of("mod:hut"));
		var found = runToEnd(search);

		search.cancel();

		assertEquals(Optional.of(found), search.step(UNLIMITED));
	}

	private static InstanceKey key(String structureId, int chunkX, int chunkZ) {
		return new InstanceKey(OVERWORLD, structureId, new ChunkCoord(chunkX, chunkZ));
	}

}
