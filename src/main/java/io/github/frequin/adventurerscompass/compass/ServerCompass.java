package io.github.frequin.adventurerscompass.compass;

import java.util.Set;
import java.util.UUID;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.config.AdventurersCompassConfig;
import io.github.frequin.adventurerscompass.domain.BlockXZ;
import io.github.frequin.adventurerscompass.domain.compass.Compass;
import io.github.frequin.adventurerscompass.domain.compass.CompassState;
import io.github.frequin.adventurerscompass.domain.compass.Effect;
import io.github.frequin.adventurerscompass.domain.compass.Outcome;
import io.github.frequin.adventurerscompass.domain.compass.SearchContext;
import io.github.frequin.adventurerscompass.domain.compass.WearContext;
import io.github.frequin.adventurerscompass.domain.search.SearchResult;
import io.github.frequin.adventurerscompass.domain.selection.Selection;
import io.github.frequin.adventurerscompass.search.CompassSearches;
import io.github.frequin.adventurerscompass.util.ItemUtils;
import io.github.frequin.adventurerscompass.util.StructureUtils;
import io.github.frequin.adventurerscompass.visited.PlayerVisited;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
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

	private static final Compass COMPASS = new Compass();

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
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (ItemUtils.isCompass(stack)) {
				apply(player, stack, COMPASS.dimensionChanged(CompassStacks.read(stack)));
			}
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
		return new SearchContext(dimension, origin, searchable, AdventurersCompassConfig.maxRadius, PlayerVisited.get(player).asSet(), AdventurersCompassConfig.compassDurability);
	}

	private static void apply(ServerPlayer player, ItemStack stack, Outcome outcome) {
		if (!(outcome instanceof Outcome.Accepted accepted)) {
			return;
		}
		CompassStacks.write(stack, accepted.state());
		for (Effect effect : accepted.effects()) {
			switch (effect) {
				case Effect.StartSearch start -> CompassSearches.start(player, start.id(), start.request(), result -> searchEnded(player, start.id(), result));
				case Effect.CancelSearch cancel -> CompassSearches.cancel(player, cancel.id());
			}
		}
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
			// Interim until the XP rule (#24) moves into the domain
			stack.set(AdventurersCompass.DISPLAY_COORDS, AdventurersCompassConfig.displayCoordinates);
			chargeXp(player, Identifier.parse(found.instance().structureId()));
		}
	}

	private static ItemStack findBySearch(ServerPlayer player, UUID searchId) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (ItemUtils.isCompass(stack) && searchId.equals(stack.get(AdventurersCompass.SEARCH))) {
				return stack;
			}
		}
		ItemStack carried = player.containerMenu.getCarried();
		if (ItemUtils.isCompass(carried) && searchId.equals(carried.get(AdventurersCompass.SEARCH))) {
			return carried;
		}
		return ItemStack.EMPTY;
	}

	private static void chargeXp(ServerPlayer player, Identifier structureId) {
		int xpLevels = Math.min(StructureUtils.getXpLevelsForStructure(player.level(), structureId), player.experienceLevel);
		if (!player.hasInfiniteMaterials() && xpLevels > 0) {
			player.giveExperienceLevels(-xpLevels);
		}
	}

	/** Repair: resets wear and keeps the rest of the state. */
	public static void repair(ItemStack stack) {
		CompassStacks.write(stack, COMPASS.repair(CompassStacks.read(stack)));
	}

}
