package io.github.frequin.adventurerscompass.domain.search;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import io.github.frequin.adventurerscompass.domain.Clock;

/**
 * Runs the players' searches on the server tick.
 *
 * <p>At most one search per player: starting one cancels the player's
 * previous search. Each {@link #tick()} spends a total time budget shared in
 * turn between the searching players; the player served last waits for the
 * others before being served again.
 *
 * <p>Not thread-safe: call it from the server thread only.
 *
 * @param <K> player identity (a UUID in game)
 */
public final class SearchScheduler<K> {

	/** About 5 ms per server tick, as the spec asks. */
	public static final long DEFAULT_TICK_BUDGET_NANOS = 5_000_000;

	private record Running(NearestFirstSearch<?> search, Consumer<SearchResult> onEnd) {
	}

	private final Clock clock;
	private final long tickBudgetNanos;
	private final Map<K, Running> running = new HashMap<>();
	private final Deque<K> turns = new ArrayDeque<>();

	public SearchScheduler(Clock clock, long tickBudgetNanos) {
		this.clock = clock;
		this.tickBudgetNanos = tickBudgetNanos;
	}

	/**
	 * Starts a player's search, cancelling theirs if one runs. {@code onEnd}
	 * gets the result once, from {@link #tick()}, {@link #cancel} or a later
	 * {@code start} for the same player.
	 */
	public void start(K player, NearestFirstSearch<?> search, Consumer<SearchResult> onEnd) {
		cancel(player);
		running.put(player, new Running(search, onEnd));
		turns.addLast(player);
	}

	/** Cancels a player's search, if any; its {@code onEnd} gets {@link SearchResult.Cancelled}. */
	public void cancel(K player) {
		Running previous = running.remove(player);
		if (previous != null) {
			turns.remove(player);
			previous.search().cancel();
			previous.onEnd().accept(previous.search().step(0).orElseThrow());
		}
	}

	/** Cancels every search, e.g. when the server stops. */
	public void cancelAll() {
		for (K player : List.copyOf(turns)) {
			cancel(player);
		}
	}

	public boolean isSearching(K player) {
		return running.containsKey(player);
	}

	public boolean hasSearches() {
		return !running.isEmpty();
	}

	/**
	 * Advances the searches in turn until the tick budget is spent or none is
	 * left. Each turn gets an equal share of the time left. A tick can overrun by
	 * at most one unit of search work (one presence test).
	 */
	public void tick() {
		long deadline = clock.nanoTime() + tickBudgetNanos;
		while (!turns.isEmpty()) {
			long left = deadline - clock.nanoTime();
			if (left <= 0) {
				return;
			}
			K player = turns.pollFirst();
			Running current = running.get(player);
			var result = current.search().step(left / (turns.size() + 1));
			if (result.isPresent()) {
				running.remove(player);
				current.onEnd().accept(result.get());
			} else {
				turns.addLast(player);
			}
		}
	}

}
