package io.github.frequin.adventurerscompass.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.mojang.serialization.JsonOps;

import io.github.frequin.adventurerscompass.domain.BlockXZ;
import io.github.frequin.adventurerscompass.domain.ChunkCoord;
import io.github.frequin.adventurerscompass.domain.InstanceKey;
import io.github.frequin.adventurerscompass.domain.compass.Target;
import io.github.frequin.adventurerscompass.domain.search.SearchResult;
import io.github.frequin.adventurerscompass.domain.visited.VisitedInstance;
import io.github.frequin.adventurerscompass.domain.visited.VisitedSet;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

class CompassCodecsTest {

	private static final InstanceKey VILLAGE = new InstanceKey("minecraft:overworld", "minecraft:village_plains", new ChunkCoord(3, -4));
	private static final InstanceKey FORTRESS = new InstanceKey("minecraft:the_nether", "minecraft:fortress", new ChunkCoord(-1, 2));
	private static final Target TARGET = new Target(VILLAGE, new BlockXZ(48, -64));
	private static final SearchResult.NothingFound NOTHING = new SearchResult.NothingFound("minecraft:the_nether", 1000, 3, 2);

	@Test
	void aTargetSurvivesBeingSavedAndCrossingTheNetwork() {
		var saved = CompassCodecs.TARGET.encodeStart(JsonOps.INSTANCE, TARGET).getOrThrow();
		assertEquals(TARGET, CompassCodecs.TARGET.parse(JsonOps.INSTANCE, saved).getOrThrow());

		ByteBuf buf = Unpooled.buffer();
		CompassCodecs.TARGET_STREAM.encode(buf, TARGET);
		assertEquals(TARGET, CompassCodecs.TARGET_STREAM.decode(buf));
	}

	@Test
	void skippedSurviveBeingSavedAndCrossingTheNetwork() {
		var skipped = Set.of(VILLAGE, FORTRESS);
		var saved = CompassCodecs.SKIPPED.encodeStart(JsonOps.INSTANCE, skipped).getOrThrow();
		assertEquals(skipped, CompassCodecs.SKIPPED.parse(JsonOps.INSTANCE, saved).getOrThrow());

		ByteBuf buf = Unpooled.buffer();
		CompassCodecs.SKIPPED_STREAM.encode(buf, skipped);
		assertEquals(skipped, CompassCodecs.SKIPPED_STREAM.decode(buf));
	}

	@Test
	void visitedKeepTheirDaysAndOrderBeingSavedAndCrossingTheNetwork() {
		var visited = VisitedSet.of(List.of(new VisitedInstance(VILLAGE, 431), new VisitedInstance(FORTRESS, 12)));
		var saved = CompassCodecs.VISITED.encodeStart(JsonOps.INSTANCE, visited).getOrThrow();
		assertEquals(visited, CompassCodecs.VISITED.parse(JsonOps.INSTANCE, saved).getOrThrow());

		ByteBuf buf = Unpooled.buffer();
		CompassCodecs.VISITED_STREAM.encode(buf, visited);
		assertEquals(visited, CompassCodecs.VISITED_STREAM.decode(buf));
	}

	@Test
	void nothingFoundSurvivesBeingSavedAndCrossingTheNetwork() {
		var saved = CompassCodecs.NOTHING_FOUND.encodeStart(JsonOps.INSTANCE, NOTHING).getOrThrow();
		assertEquals(NOTHING, CompassCodecs.NOTHING_FOUND.parse(JsonOps.INSTANCE, saved).getOrThrow());

		ByteBuf buf = Unpooled.buffer();
		CompassCodecs.NOTHING_FOUND_STREAM.encode(buf, NOTHING);
		assertEquals(NOTHING, CompassCodecs.NOTHING_FOUND_STREAM.decode(buf));
	}

}
