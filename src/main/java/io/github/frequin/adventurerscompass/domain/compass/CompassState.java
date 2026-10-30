package io.github.frequin.adventurerscompass.domain.compass;

import java.util.Set;
import java.util.UUID;

import io.github.frequin.adventurerscompass.domain.InstanceKey;
import io.github.frequin.adventurerscompass.domain.search.SearchResult;

/**
 * What one compass knows, carried by the item. Immutable; the commands of
 * {@link Compass} return the next state.
 *
 * @param search       id of the running search, or {@code null} when none runs
 * @param target       the Target, or {@code null}
 * @param skipped      Instances passed over with "Pass"
 * @param nothingFound the last search's "nothing found", or {@code null}; cleared by the next search
 */
public record CompassState(UUID search, Target target, Set<InstanceKey> skipped, SearchResult.NothingFound nothingFound) {

	public static final CompassState EMPTY = new CompassState(null, null, Set.of(), null);

	public CompassState {
		skipped = Set.copyOf(skipped);
	}

	public boolean isSearching() {
		return search != null;
	}

	/** What the compass shows to a player standing in {@code currentDimension}. */
	public Status status(String currentDimension) {
		if (isSearching()) {
			return Status.SEARCHING;
		}
		if (target != null) {
			return target.dimension().equals(currentDimension) ? Status.TARGET : Status.OTHER_DIMENSION;
		}
		return nothingFound != null ? Status.NOTHING_FOUND : Status.IDLE;
	}

	public CompassState withSearch(UUID search) {
		return new CompassState(search, target, skipped, nothingFound);
	}

	public CompassState withTarget(Target target) {
		return new CompassState(search, target, skipped, nothingFound);
	}

	public CompassState withSkipped(Set<InstanceKey> skipped) {
		return new CompassState(search, target, skipped, nothingFound);
	}

	public CompassState withNothingFound(SearchResult.NothingFound nothingFound) {
		return new CompassState(search, target, skipped, nothingFound);
	}

}
