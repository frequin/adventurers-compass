package io.github.frequin.adventurerscompass.search;

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
 * advanced at the end of every server tick.
 */
public final class CompassSearches {

	private static final SearchScheduler<UUID> SCHEDULER = new SearchScheduler<>(Clock.SYSTEM, SearchScheduler.DEFAULT_TICK_BUDGET_NANOS);

	private CompassSearches() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> SCHEDULER.tick());
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> SCHEDULER.cancelAll());
	}

	/** Starts the player's search, cancelling their previous one. {@code onEnd} runs on the server thread. */
	public static void start(ServerPlayer player, SearchRequest request, Consumer<SearchResult> onEnd) {
		var search = new NearestFirstSearch<>(new MinecraftWorldPort(player.level().getServer()), request, Clock.SYSTEM);
		SCHEDULER.start(player.getUUID(), search, onEnd);
	}

	public static void cancel(ServerPlayer player) {
		SCHEDULER.cancel(player.getUUID());
	}

}
