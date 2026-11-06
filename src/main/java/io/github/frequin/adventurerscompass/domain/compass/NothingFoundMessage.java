package io.github.frequin.adventurerscompass.domain.compass;

import io.github.frequin.adventurerscompass.domain.search.SearchResult;

/**
 * The last search's "nothing found", shown over the Target for five seconds.
 *
 * @param result  what the search reports
 * @param shownAt the {@link io.github.frequin.adventurerscompass.domain.Clock} time it appeared, or {@code null}
 */
public record NothingFoundMessage(SearchResult.NothingFound result, Long shownAt) {

	/** How long it shows before the HUD goes back to the Target. */
	private static final long SHOWN_NANOS = 5_000_000_000L;

	/** Whether it still shows at the time {@code now}. A time from a previous run of the game means nothing to this clock: it reads as over, whichever side it falls. */
	boolean isShowing(long now) {
		if (shownAt == null) {
			return false;
		}
		long elapsed = now - shownAt;
		return elapsed >= 0 && elapsed < SHOWN_NANOS;
	}

}
