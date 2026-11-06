package io.github.frequin.adventurerscompass.domain.visited;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.frequin.adventurerscompass.domain.ChunkCoord;
import io.github.frequin.adventurerscompass.domain.InstanceKey;

class VisitedSetTest {

	private static final InstanceKey PLAINS_VILLAGE = new InstanceKey("minecraft:overworld", "minecraft:village_plains", new ChunkCoord(10, -5));
	private static final InstanceKey DESERT_PYRAMID = new InstanceKey("minecraft:overworld", "minecraft:desert_pyramid", new ChunkCoord(-30, 4));

	@Test
	void markingAnInstanceIsNewOnlyTheFirstTime() {
		var first = VisitedSet.empty().mark(PLAINS_VILLAGE, 12);
		var second = first.visited().mark(PLAINS_VILLAGE, 12);

		assertTrue(first.isNew());
		assertFalse(second.isNew());
		assertTrue(second.visited().contains(PLAINS_VILLAGE));
	}

	@Test
	void anUnmarkedInstanceIsNoLongerVisitedAndCanBeMarkedAgain() {
		var visited = VisitedSet.empty().mark(PLAINS_VILLAGE, 12).visited().mark(DESERT_PYRAMID, 40).visited();

		var unmarked = visited.unmark(PLAINS_VILLAGE);

		assertFalse(unmarked.contains(PLAINS_VILLAGE));
		assertTrue(unmarked.contains(DESERT_PYRAMID));
		assertTrue(unmarked.mark(PLAINS_VILLAGE, 12).isNew());
	}

	@Test
	void listsVisitedInstancesInTheOrderTheyWereMarked() {
		var visited = VisitedSet.empty().mark(DESERT_PYRAMID, 40).visited().mark(PLAINS_VILLAGE, 12).visited().mark(DESERT_PYRAMID, 40).visited();

		assertEquals(List.of(DESERT_PYRAMID, PLAINS_VILLAGE), visited.list());
		assertEquals(List.of(), VisitedSet.empty().list());
	}

	@Test
	void anInstanceRemembersTheWorldDayItWasMarked() {
		var visited = VisitedSet.empty().mark(DESERT_PYRAMID, 40).visited().mark(PLAINS_VILLAGE, 431).visited();

		assertEquals(List.of(new VisitedInstance(DESERT_PYRAMID, 40), new VisitedInstance(PLAINS_VILLAGE, 431)), visited.instances());
	}

	@Test
	void markingAVisitedInstanceAgainKeepsTheDayOfTheFirstMark() {
		var visited = VisitedSet.empty().mark(PLAINS_VILLAGE, 12).visited().mark(PLAINS_VILLAGE, 99).visited();

		assertEquals(List.of(new VisitedInstance(PLAINS_VILLAGE, 12)), visited.instances());
	}

	@Test
	void anInstanceUnmarkedThenMarkedAgainTakesTheNewDay() {
		var visited = VisitedSet.empty().mark(PLAINS_VILLAGE, 12).visited().unmark(PLAINS_VILLAGE).mark(PLAINS_VILLAGE, 99).visited();

		assertEquals(List.of(new VisitedInstance(PLAINS_VILLAGE, 99)), visited.instances());
	}

	@Test
	void clearingForgetsEveryInstanceAndTheyCanBeMarkedAgain() {
		var visited = VisitedSet.empty().mark(PLAINS_VILLAGE, 12).visited().mark(DESERT_PYRAMID, 40).visited();

		var cleared = visited.clear();

		assertEquals(List.of(), cleared.list());
		assertFalse(cleared.contains(PLAINS_VILLAGE));
		assertTrue(cleared.mark(PLAINS_VILLAGE, 50).isNew());
	}

}
