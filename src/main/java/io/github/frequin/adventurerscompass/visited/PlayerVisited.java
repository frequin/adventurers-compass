package io.github.frequin.adventurerscompass.visited;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.codec.CompassCodecs;
import io.github.frequin.adventurerscompass.domain.InstanceKey;
import io.github.frequin.adventurerscompass.domain.visited.VisitedSet;
import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Stores each player's {@link VisitedSet} as a Fabric data attachment on the
 * player: saved with the player data (so per player and per world), and copied
 * to the respawned player on death.
 */
public final class PlayerVisited {

	static final Codec<VisitedSet> CODEC = CompassCodecs.INSTANCE_KEY.listOf().xmap(VisitedSet::of, VisitedSet::list);

	static final StreamCodec<ByteBuf, VisitedSet> STREAM_CODEC = CompassCodecs.INSTANCE_KEY_STREAM.apply(ByteBufCodecs.list()).map(VisitedSet::of, VisitedSet::list);

	/** Synced to the player it belongs to only, for the Visited list of the screen and the HUD. */
	public static final AttachmentType<VisitedSet> VISITED = AttachmentRegistry.create(
			Identifier.fromNamespaceAndPath(AdventurersCompass.MODID, "visited"),
			builder -> builder.persistent(CODEC).initializer(VisitedSet::empty).copyOnDeath().syncWith(STREAM_CODEC, AttachmentSyncPredicate.targetOnly()));

	private PlayerVisited() {
	}

	/** Loads the class, which registers the attachment. Call from the common initializer. */
	public static void register() {
	}

	public static VisitedSet get(ServerPlayer player) {
		return player.getAttachedOrElse(VISITED, VisitedSet.empty());
	}

	/** Marks an Instance Visited for this player; true if it was not Visited yet. */
	public static boolean mark(ServerPlayer player, InstanceKey key) {
		VisitedSet.Marked marked = get(player).mark(key);
		if (marked.isNew()) {
			player.setAttached(VISITED, marked.visited());
		}
		return marked.isNew();
	}

	public static void unmark(ServerPlayer player, InstanceKey key) {
		VisitedSet visited = get(player);
		if (visited.contains(key)) {
			player.setAttached(VISITED, visited.unmark(key));
		}
	}

}
