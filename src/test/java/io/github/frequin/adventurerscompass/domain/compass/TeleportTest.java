package io.github.frequin.adventurerscompass.domain.compass;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.frequin.adventurerscompass.domain.BlockXZ;
import io.github.frequin.adventurerscompass.domain.ChunkCoord;
import io.github.frequin.adventurerscompass.domain.InstanceKey;

/** "Teleport": op or creative only, when {@code allowTeleport} is on, never across dimensions. */
class TeleportTest {

	private static final String OVERWORLD = "minecraft:overworld";
	private static final String NETHER = "minecraft:the_nether";
	private static final BlockXZ VILLAGE_POS = new BlockXZ(48, 64);

	private final Compass compass = new Compass();

	private static CompassState targeting() {
		return CompassState.EMPTY.withTarget(new Target(new InstanceKey(OVERWORLD, "minecraft:village_plains", new ChunkCoord(3, 4)), VILLAGE_POS));
	}

	private static TeleportContext op(String dimension) {
		return new TeleportContext(dimension, true, true, false);
	}

	@Test
	void anOpTeleportsToTheTargetAndTheCompassIsUnchanged() {
		var state = targeting();

		assertEquals(new Outcome.Accepted(state, List.of(new Effect.TeleportTo(VILLAGE_POS))), compass.teleport(state, op(OVERWORLD)));
	}

	@Test
	void teleportIsRefusedToEveryoneWhenTheServerDoesNotAllowIt() {
		var opInCreative = new TeleportContext(OVERWORLD, false, true, true);

		assertEquals(new Outcome.Refused(Refusal.TELEPORT_NOT_ALLOWED), compass.teleport(targeting(), opInCreative));
	}

	@Test
	void aSurvivalPlayerWhoIsNotOpCannotTeleport() {
		var survival = new TeleportContext(OVERWORLD, true, false, false);

		assertEquals(new Outcome.Refused(Refusal.TELEPORT_NOT_ALLOWED), compass.teleport(targeting(), survival));
	}

	@Test
	void aCreativePlayerWhoIsNotOpCanTeleport() {
		var creative = new TeleportContext(OVERWORLD, true, false, true);

		assertEquals(List.of(new Effect.TeleportTo(VILLAGE_POS)), ((Outcome.Accepted) compass.teleport(targeting(), creative)).effects());
	}

	@Test
	void theTeleportButtonIsOfferedToExactlyThePlayersTeleportAccepts() {
		assertEquals(true, new TeleportContext(OVERWORLD, true, true, false).allowed());
		assertEquals(true, new TeleportContext(OVERWORLD, true, false, true).allowed());
		assertEquals(false, new TeleportContext(OVERWORLD, true, false, false).allowed());
		assertEquals(false, new TeleportContext(OVERWORLD, false, true, true).allowed());
	}

	@Test
	void teleportNeverCrossesDimensions() {
		assertEquals(new Outcome.Refused(Refusal.TARGET_IN_OTHER_DIMENSION), compass.teleport(targeting(), op(NETHER)));
	}

	@Test
	void teleportWithoutATargetIsRefused() {
		assertEquals(new Outcome.Refused(Refusal.NO_TARGET), compass.teleport(CompassState.EMPTY, op(OVERWORLD)));
	}

}
