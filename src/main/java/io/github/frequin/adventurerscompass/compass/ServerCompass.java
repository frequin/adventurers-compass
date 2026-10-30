package io.github.frequin.adventurerscompass.compass;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.config.AdventurersCompassConfig;
import io.github.frequin.adventurerscompass.domain.BlockXZ;
import io.github.frequin.adventurerscompass.domain.InstanceKey;
import io.github.frequin.adventurerscompass.domain.compass.Compass;
import io.github.frequin.adventurerscompass.domain.compass.CompassState;
import io.github.frequin.adventurerscompass.domain.compass.Effect;
import io.github.frequin.adventurerscompass.domain.compass.LaunchRule;
import io.github.frequin.adventurerscompass.domain.compass.Outcome;
import io.github.frequin.adventurerscompass.domain.compass.Refusal;
import io.github.frequin.adventurerscompass.domain.compass.SearchContext;
import io.github.frequin.adventurerscompass.domain.compass.TeleportContext;
import io.github.frequin.adventurerscompass.domain.compass.WearContext;
import io.github.frequin.adventurerscompass.domain.search.SearchResult;
import io.github.frequin.adventurerscompass.domain.selection.Selection;
import io.github.frequin.adventurerscompass.network.TeleportPacket;
import io.github.frequin.adventurerscompass.search.CompassSearches;
import io.github.frequin.adventurerscompass.util.ItemUtils;
import io.github.frequin.adventurerscompass.util.PlayerUtils;
import io.github.frequin.adventurerscompass.util.StructureUtils;
import io.github.frequin.adventurerscompass.visited.PlayerVisited;
import io.github.frequin.adventurerscompass.visited.VisitedDetection;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * The server adapter of the compass state machine: turns packets and game
 * events into {@link Compass} commands, stores the resulting state on the
 * compass item and runs the effects. Search results come back here and find
 * their compass again by the search id stored on it.
 */
public final class ServerCompass {

	private static final Compass COMPASS = new Compass(List.of(LaunchRule.notIn(AdventurersCompassConfig::isDisabledDimension), LaunchRule.SOMETHING_SEARCHABLE, LaunchRule.NOT_BROKEN, LaunchRule.ENOUGH_XP));

	private ServerCompass() {
	}

	public static void register() {
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> dimensionChanged(player));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> CompassSearches.cancel(handler.player));
	}

	/** "Search" with the compass the player holds. */
	public static void search(ServerPlayer player, ItemStack stack) {
		apply(player, stack, COMPASS.search(CompassStacks.read(stack), context(player, stack), UUID.randomUUID()));
	}

	/** "Pass" with the compass the player holds. */
	public static void pass(ServerPlayer player, ItemStack stack) {
		apply(player, stack, COMPASS.pass(CompassStacks.read(stack), context(player, stack), UUID.randomUUID()));
	}

	/** "Cancel" with the compass the player holds. */
	public static void cancel(ServerPlayer player, ItemStack stack) {
		apply(player, stack, COMPASS.cancel(CompassStacks.read(stack)));
	}

	/** "Mark Visited" with the compass the player holds: marks its Target, wherever it is. */
	public static void markVisited(ServerPlayer player, ItemStack stack) {
		apply(player, stack, COMPASS.markVisited(CompassStacks.read(stack)));
	}

	/** "Teleport" to the Target of the compass the player holds. */
	public static void teleport(ServerPlayer player, ItemStack stack) {
		Outcome outcome = COMPASS.teleport(CompassStacks.read(stack), teleportContext(player));
		if (outcome instanceof Outcome.Refused refused) {
			AdventurersCompass.LOGGER.warn("Player " + player.getDisplayName().getString() + " could not teleport: " + refused.reason());
		}
		apply(player, stack, outcome);
	}

	/** Who asks for "Teleport", and where: the one source of the teleport rule's inputs, for the command and the screen's button. */
	public static TeleportContext teleportContext(ServerPlayer player) {
		return new TeleportContext(player.level().dimension().identifier().toString(), AdventurersCompassConfig.allowTeleport, PlayerUtils.isOperator(player.level().getServer(), player), player.isCreative());
	}

	/** Stores a new Selection on the compass; a real change cancels the search and clears the Target and Skipped. */
	public static void selectionChanged(ServerPlayer player, ItemStack stack, Selection selection) {
		if (selection.equals(stack.getOrDefault(AdventurersCompass.SELECTION, Selection.EMPTY))) {
			return;
		}
		if (selection.equals(Selection.EMPTY)) {
			stack.remove(AdventurersCompass.SELECTION);
		} else {
			stack.set(AdventurersCompass.SELECTION, selection);
		}
		apply(player, stack, COMPASS.selectionChanged(CompassStacks.read(stack)));
	}

	/** Every compass the player carries cancels its search and keeps its Target and Skipped. */
	private static void dimensionChanged(ServerPlayer player) {
		for (ItemStack stack : ItemUtils.allCompasses(player)) {
			apply(player, stack, COMPASS.dimensionChanged(CompassStacks.read(stack)));
		}
	}

	/**
	 * An Instance just became Visited for the player: every compass they carry
	 * hears it. A compass whose Target it is keeps it; the arrival sound plays
	 * once, however many compasses point at it.
	 */
	static void instanceVisited(ServerPlayer player, InstanceKey instance) {
		boolean arrived = false;
		for (ItemStack stack : ItemUtils.allCompasses(player)) {
			Outcome outcome = COMPASS.instanceVisited(CompassStacks.read(stack), instance);
			boolean arrival = outcome instanceof Outcome.Accepted accepted && accepted.effects().stream().anyMatch(Effect.TargetVisited.class::isInstance);
			if (arrival && arrived) {
				continue;
			}
			arrived |= arrival;
			apply(player, stack, outcome);
		}
	}

	/**
	 * Called while the compass ticks in an inventory: a compass still marked as
	 * searching whose search no longer runs (logout, server restart, death, a
	 * stay in a chest) goes back to idle.
	 */
	public static void interruptIfOrphaned(ItemStack stack) {
		CompassState state = CompassStacks.read(stack);
		if (state.isSearching() && !CompassSearches.isRunning(state.search())) {
			CompassStacks.write(stack, COMPASS.searchEnded(state, state.search(), new SearchResult.Cancelled()));
		}
	}

	private static SearchContext context(ServerPlayer player, ItemStack stack) {
		ServerLevel level = player.level();
		String dimension = level.dimension().identifier().toString();
		Selection selection = stack.getOrDefault(AdventurersCompass.SELECTION, Selection.EMPTY);
		Set<String> searchable = selection.resolve(StructureUtils.buildCatalogue(level), dimension);
		BlockXZ origin = new BlockXZ(player.blockPosition().getX(), player.blockPosition().getZ());
		return new SearchContext(dimension, origin, searchable, AdventurersCompassConfig.maxRadius, PlayerVisited.get(player).asSet(), AdventurersCompassConfig.compassDurability, ServerXp.costs(), ServerXp.of(player));
	}

	private static void apply(ServerPlayer player, ItemStack stack, Outcome outcome) {
		if (outcome instanceof Outcome.Refused(Refusal reason)) {
			Component message = refusalMessage(player, stack, reason);
			if (message != null) {
				player.sendOverlayMessage(message);
			}
		}
		if (!(outcome instanceof Outcome.Accepted accepted)) {
			return;
		}
		CompassStacks.write(stack, accepted.state());
		for (Effect effect : accepted.effects()) {
			switch (effect) {
				case Effect.StartSearch start -> CompassSearches.start(player, start.id(), start.request(), result -> searchEnded(player, start.id(), result));
				case Effect.CancelSearch cancel -> CompassSearches.cancel(player, cancel.id());
				case Effect.MarkVisited mark -> CompassVisited.mark(player, mark.instance());
				case Effect.TargetVisited visited -> CompassVisited.playArrivalSound(player);
				case Effect.TeleportTo teleport -> TeleportPacket.teleport(player, teleport.pos().x(), teleport.pos().z());
			}
		}
	}

	/**
	 * What the action bar says when "Search" or "Pass" is refused; the screen
	 * greys the buttons for the same reasons, but its state can be stale.
	 * {@code null} for refusals the screen never lets through (no Target, teleport).
	 */
	private static Component refusalMessage(ServerPlayer player, ItemStack stack, Refusal reason) {
		return switch (reason) {
			case DISABLED_DIMENSION -> Component.translatable("string.adventurerscompass.disabledDimension");
			case NOTHING_SEARCHABLE -> Component.translatable("string.adventurerscompass.nothingSearchableHere");
			case BROKEN -> Component.translatable("string.adventurerscompass.broken");
			case NOT_ENOUGH_XP -> Component.translatable("string.adventurerscompass.searchNeedsXp", context(player, stack).xpThreshold().levels());
			case NO_TARGET, TELEPORT_NOT_ALLOWED, TARGET_IN_OTHER_DIMENSION -> null;
		};
	}

	private static void searchEnded(ServerPlayer launcher, UUID searchId, SearchResult result) {
		// The player may have respawned or reconnected since: use the current entity
		ServerPlayer player = launcher.level().getServer().getPlayerList().getPlayer(launcher.getUUID());
		if (player == null) {
			return;
		}
		ItemStack stack = findBySearch(player, searchId);
		if (stack.isEmpty()) {
			return;
		}
		WearContext wear = new WearContext(AdventurersCompassConfig.compassDurability, player.hasInfiniteMaterials());
		CompassStacks.write(stack, COMPASS.searchEnded(CompassStacks.read(stack), searchId, result, wear));
		if (result instanceof SearchResult.Found found) {
			pay(player, found);
		}
	}

	/** A found Target costs its Structure's real cost, capped at the levels left; the player is told the price. */
	private static void pay(ServerPlayer player, SearchResult.Found found) {
		String structureId = found.instance().structureId();
		int paid = ServerXp.of(player).payment(ServerXp.costs().cost(structureId, found.instance().dimension()));
		if (paid > 0) {
			player.giveExperienceLevels(-paid);
		}
		Component name = VisitedDetection.structureName(Identifier.parse(structureId));
		player.sendOverlayMessage(paid > 0 ? Component.translatable("string.adventurerscompass.targetFoundPaid", name, paid) : Component.translatable("string.adventurerscompass.targetFound", name));
	}

	private static ItemStack findBySearch(ServerPlayer player, UUID searchId) {
		for (ItemStack stack : ItemUtils.allCompasses(player)) {
			if (searchId.equals(stack.get(AdventurersCompass.SEARCH))) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	/** Repair: resets wear and keeps the rest of the state. */
	public static void repair(ItemStack stack) {
		CompassStacks.write(stack, COMPASS.repair(CompassStacks.read(stack)));
	}

}
