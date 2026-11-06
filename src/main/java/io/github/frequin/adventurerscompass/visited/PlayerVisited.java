package io.github.frequin.adventurerscompass.visited;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.codec.CompassCodecs;
import io.github.frequin.adventurerscompass.domain.InstanceKey;
import io.github.frequin.adventurerscompass.domain.visited.VisitedSet;
import io.github.frequin.adventurerscompass.util.LevelUtils;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Stores each player's {@link VisitedSet} as a Fabric data attachment on the
 * player: saved with the player data (so per player and per world), and copied
 * to the respawned player on death.
 */
public final class PlayerVisited {

	/** Synced to the player it belongs to only, for the Visited tab of the screen and the HUD. */
	public static final AttachmentType<VisitedSet> VISITED = AttachmentRegistry.create(
			Identifier.fromNamespaceAndPath(AdventurersCompass.MODID, "visited"),
			builder -> builder.persistent(CompassCodecs.VISITED).initializer(VisitedSet::empty).copyOnDeath().syncWith(CompassCodecs.VISITED_STREAM, AttachmentSyncPredicate.targetOnly()));

	private PlayerVisited() {
	}

	/** Loads the class, which registers the attachment. Call from the common initializer. */
	public static void register() {
	}

	public static VisitedSet get(ServerPlayer player) {
		return player.getAttachedOrElse(VISITED, VisitedSet.empty());
	}

	/** Marks an Instance Visited for this player, on today's world day; true if it was not Visited yet. */
	public static boolean mark(ServerPlayer player, InstanceKey key) {
		VisitedSet.Marked marked = get(player).mark(key, LevelUtils.worldDay(player.level()));
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

	public static void clear(ServerPlayer player) {
		player.setAttached(VISITED, get(player).clear());
	}

}
