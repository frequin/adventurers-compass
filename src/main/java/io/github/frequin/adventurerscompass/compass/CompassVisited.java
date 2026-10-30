package io.github.frequin.adventurerscompass.compass;

import io.github.frequin.adventurerscompass.config.AdventurersCompassConfig;
import io.github.frequin.adventurerscompass.domain.InstanceKey;
import io.github.frequin.adventurerscompass.visited.PlayerVisited;
import io.github.frequin.adventurerscompass.visited.VisitedDetection;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * The server side of the Visited rules of the compass: "Mark Visited",
 * "Unmark", and what happens when an Instance becomes Visited (the action-bar
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

	/**
	 * The Instance just became Visited for the player (automatically or by
	 * hand): the action-bar message, then the sound once if it is the Target of
	 * any compass they carry. No compass searches again.
	 */
	public static void becameVisited(ServerPlayer player, InstanceKey instance) {
		VisitedDetection.announce(player, instance);
		ServerCompass.instanceVisited(player, instance);
	}

	/** The soft sound of a Target that became Visited, heard by this player only. */
	static void playArrivalSound(ServerPlayer player) {
		if (AdventurersCompassConfig.targetVisitedSound) {
			player.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.AMETHYST_BLOCK_CHIME), SoundSource.PLAYERS, player.getX(), player.getY(), player.getZ(), 1.0F, 1.0F, player.getRandom().nextLong()));
		}
	}

}
