package io.github.frequin.adventurerscompass.search;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import io.github.frequin.adventurerscompass.domain.Clock;
import io.github.frequin.adventurerscompass.domain.search.NearestFirstSearch;
import io.github.frequin.adventurerscompass.domain.search.SearchRequest;
import io.github.frequin.adventurerscompass.domain.search.SearchResult;
import io.github.frequin.adventurerscompass.domain.search.SearchScheduler;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server side entry point of compass searches: one scheduler for the server,
 * advanced at the end of every server tick. Each search has the id the
 * compass state machine gave it, so a compass can tell whether its search
 * still runs.
 */
public final class CompassSearches {

	private static final SearchScheduler<UUID> SCHEDULER = new SearchScheduler<>(Clock.SYSTEM, SearchScheduler.DEFAULT_TICK_BUDGET_NANOS);

	/** Player UUID to the id of their running search. Server thread only: not synchronized. */
	private static final Map<UUID, UUID> RUNNING = new HashMap<>();

	private CompassSearches() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> SCHEDULER.tick());
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			SCHEDULER.cancelAll();
			RUNNING.clear();
		});
	}

	/**
	 * Starts the player's search {@code searchId}, cancelling their previous one
	 * (whose callback receives {@code Cancelled} right away). {@code onEnd} runs
	 * on the server thread, exactly once.
	 */
	public static void start(ServerPlayer player, UUID searchId, SearchRequest request, Consumer<SearchResult> onEnd) {
		UUID playerId = player.getUUID();
		var search = new NearestFirstSearch<>(new MinecraftWorldPort(player.level().getServer()), request, Clock.SYSTEM);
		SCHEDULER.start(playerId, search, result -> {
			RUNNING.remove(playerId, searchId);
			onEnd.accept(result);
		});
		RUNNING.put(playerId, searchId);
	}

	/** Cancels the search {@code searchId} if it is the player's running one. */
	public static void cancel(ServerPlayer player, UUID searchId) {
		if (searchId.equals(RUNNING.get(player.getUUID()))) {
			SCHEDULER.cancel(player.getUUID());
		}
	}

	/** Cancels the player's running search, whatever it is. */
	public static void cancel(ServerPlayer player) {
		SCHEDULER.cancel(player.getUUID());
	}

	public static boolean isRunning(UUID searchId) {
		return RUNNING.containsValue(searchId);
	}

}
