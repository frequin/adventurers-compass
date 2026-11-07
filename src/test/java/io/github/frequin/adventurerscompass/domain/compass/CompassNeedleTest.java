package io.github.frequin.adventurerscompass.domain.compass;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.frequin.adventurerscompass.domain.BlockXZ;
import io.github.frequin.adventurerscompass.domain.ChunkCoord;
import io.github.frequin.adventurerscompass.domain.InstanceKey;
import io.github.frequin.adventurerscompass.domain.search.SearchResult;

class CompassNeedleTest {

	private static final String OVERWORLD = "minecraft:overworld";
	private static final String NETHER = "minecraft:the_nether";
	private static final SearchResult.NothingFound NOTHING = new SearchResult.NothingFound(OVERWORLD, 1000, 0, 0);

	private static Target targetIn(String dimension) {
		return new Target(new InstanceKey(dimension, "minecraft:village_plains", new ChunkCoord(1, 2)), new BlockXZ(16, 32));
	}

	@Test
	void pointsAtATargetInTheCurrentDimension() {
		assertEquals(Needle.TARGET, CompassState.EMPTY.withTarget(targetIn(OVERWORLD)).needle(OVERWORLD));
	}

	@Test
	void spinsWhenTheTargetIsInAnotherDimension() {
		assertEquals(Needle.SPIN, CompassState.EMPTY.withTarget(targetIn(NETHER)).needle(OVERWORLD));
	}

	@Test
	void eachCompassFollowsItsOwnTargetInTheSameDimension() {
		var compassA = CompassState.EMPTY.withTarget(targetIn(OVERWORLD));
		var compassB = CompassState.EMPTY.withTarget(targetIn(NETHER));

		assertEquals(Needle.TARGET, compassA.needle(OVERWORLD));
		assertEquals(Needle.SPIN, compassB.needle(OVERWORLD));
		assertEquals(Needle.SPIN, compassA.needle(NETHER));
		assertEquals(Needle.TARGET, compassB.needle(NETHER));
	}

	@Test
	void spinsWhileSearchingEvenWithATargetInTheCurrentDimension() {
		var state = CompassState.EMPTY.withTarget(targetIn(OVERWORLD)).withSearch(UUID.randomUUID());

		assertEquals(Needle.SPIN, state.needle(OVERWORLD));
	}

	@Test
	void pointsAtAKeptTargetEvenWhenTheLastSearchFoundNothing() {
		var state = CompassState.EMPTY.withTarget(targetIn(OVERWORLD)).withNothingFound(new NothingFoundMessage(NOTHING, null));

		assertEquals(Needle.TARGET, state.needle(OVERWORLD));
	}

	@Test
	void pointsAtTheWorldSpawnWithoutATarget() {
		assertEquals(Needle.WORLD_SPAWN, CompassState.EMPTY.needle(OVERWORLD));
	}

}
