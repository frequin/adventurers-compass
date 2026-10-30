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

	/** What the compass shows to a player standing in {@code currentDimension}, ignoring Visited. */
	public Status status(String currentDimension) {
		return status(currentDimension, Set.of());
	}

	/**
	 * What the compass shows to a player standing in {@code currentDimension}
	 * whose Visited Instances are {@code visited}. Visited is read, not stored,
	 * so unmarking the Target makes it a plain Target again.
	 */
	public Status status(String currentDimension, Set<InstanceKey> visited) {
		if (isSearching()) {
			return Status.SEARCHING;
		}
		if (target != null) {
			if (!target.dimension().equals(currentDimension)) {
				return Status.OTHER_DIMENSION;
			}
			return visited.contains(target.instance()) ? Status.TARGET_VISITED : Status.TARGET;
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
