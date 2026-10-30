package io.github.frequin.adventurerscompass.visited;

import java.util.Map;

import io.github.frequin.adventurerscompass.config.AdventurersCompassConfig;
import io.github.frequin.adventurerscompass.domain.ChunkCoord;
import io.github.frequin.adventurerscompass.domain.InstanceKey;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * Marks an Instance Visited when a player stands in one of its pieces, for any
 * Structure, with or without a compass. Each player is polled every
 * {@link #POLL_INTERVAL_TICKS} ticks, like the vanilla location advancement
 * check, and with the same piece test (no fallback on the start's overall box).
 */
public final class VisitedDetection {

	public static final int POLL_INTERVAL_TICKS = 20;

	private VisitedDetection() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			int tick = server.getTickCount();
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				// Spread players over the interval instead of polling them all on the same tick
				if (Math.floorMod(tick + player.getId(), POLL_INTERVAL_TICKS) == 0) {
					poll(player);
				}
			}
		});
	}

	/** Marks Visited every Instance with a piece at the player's position. Spectators mark nothing. */
	public static void poll(ServerPlayer player) {
		if (player.isSpectator()) {
			return;
		}
		ServerLevel level = player.level();
		BlockPos pos = player.blockPosition();
		if (!level.isLoaded(pos)) {
			return;
		}
		StructureManager structures = level.structureManager();
		// References of the player's chunk: usually empty, and cheap
		Map<Structure, LongSet> references = structures.getAllStructuresAt(pos);
		if (references.isEmpty()) {
			return;
		}
		String dimension = level.dimension().identifier().toString();
		Registry<Structure> registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
		for (Map.Entry<Structure, LongSet> entry : references.entrySet()) {
			Structure structure = entry.getKey();
			Identifier structureId = registry.getKey(structure);
			if (structureId == null) {
				continue;
			}
			for (LongIterator it = entry.getValue().iterator(); it.hasNext();) {
				ChunkPos startChunk = ChunkPos.unpack(it.nextLong());
				InstanceKey key = new InstanceKey(dimension, structureId.toString(), new ChunkCoord(startChunk.x(), startChunk.z()));
				if (PlayerVisited.get(player).contains(key)) {
					continue;
				}
				StructureStart start = structures.getStartForStructure(structure, level.getChunk(startChunk.x(), startChunk.z(), ChunkStatus.STRUCTURE_STARTS));
				if (start != null && start.isValid() && structures.structureHasPieceAt(pos, start) && PlayerVisited.mark(player, key)) {
					announce(player, structureId);
				}
			}
		}
	}

	private static void announce(ServerPlayer player, Identifier structureId) {
		if (AdventurersCompassConfig.visitedMessage) {
			player.sendOverlayMessage(Component.translatable("string.adventurerscompass.visited", structureName(structureId)));
		}
	}

	/** The Structure's translated name when the client has one, else a readable form of its id. */
	public static Component structureName(Identifier structureId) {
		String[] words = structureId.getPath().split("_");
		StringBuilder fallback = new StringBuilder();
		for (String word : words) {
			if (word.isEmpty()) {
				continue;
			}
			if (!fallback.isEmpty()) {
				fallback.append(' ');
			}
			fallback.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}
		return Component.translatableWithFallback(Util.makeDescriptionId("structure", structureId), fallback.toString());
	}

}
