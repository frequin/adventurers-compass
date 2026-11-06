package io.github.frequin.adventurerscompass.compass;

import io.github.frequin.adventurerscompass.config.AdventurersCompassConfig;
import io.github.frequin.adventurerscompass.domain.InstanceKey;
import io.github.frequin.adventurerscompass.visited.PlayerVisited;
import io.github.frequin.adventurerscompass.visited.VisitedDetection;

import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * The server side of the Visited rules of the compass: "Mark Visited",
 * "Unmark", "Unmark all", and what happens when an Instance becomes Visited (the action-bar
 * message, and the sound when it is the Target of a compass the player carries).
 */
public final class CompassVisited {

	private CompassVisited() {
	}

	/** Marks the Instance Visited for the player; a new mark is announced. */
	static void mark(ServerPlayer player, InstanceKey instance) {
		if (PlayerVisited.mark(player, instance)) {
			becameVisited(player, instance);
		}
	}

	/** Makes the Instance searchable again for the player. */
	public static void unmark(ServerPlayer player, InstanceKey instance) {
		PlayerVisited.unmark(player, instance);
	}

	/** Makes every Visited Instance searchable again for the player. */
	public static void clear(ServerPlayer player) {
		PlayerVisited.clear(player);
	}

	/**
	 * The Instance just became Visited for the player (automatically or by
	 * hand): the action-bar message, then the sound once if it is the Target of
	 * any compass they carry. No compass searches again.
	 */
	public static void becameVisited(ServerPlayer player, InstanceKey instance) {
		VisitedDetection.announce(player, instance);
		ServerCompass.instanceVisited(player, instance);
	}

	/**
	 * The soft sound of a Target that became Visited, heard by this player only.
	 * Not the amethyst chime: vanilla defines it at volume 0.2 on a very quiet
	 * recording, and the client caps the gain at 1, so no packet volume makes it heard.
	 */
	static void playArrivalSound(ServerPlayer player) {
		if (AdventurersCompassConfig.targetVisitedSound) {
			player.connection.send(new ClientboundSoundPacket(SoundEvents.NOTE_BLOCK_CHIME, SoundSource.PLAYERS, player.getX(), player.getY(), player.getZ(), 1.0F, 1.0F, player.getRandom().nextLong()));
		}
	}

}
