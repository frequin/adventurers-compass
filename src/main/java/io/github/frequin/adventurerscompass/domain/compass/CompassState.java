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
 * @param nothingFound the last search's "nothing found", or {@code null}; shown over the Target for {@link Compass#NOTHING_FOUND_NANOS}, cleared sooner by the next search, a Selection change, a dimension change or the compass leaving the hand
 * @param wear         Targets found since the last repair (Pass included)
 * @param nothingFoundAt the {@link io.github.frequin.adventurerscompass.domain.Clock} time {@code nothingFound} appeared, or {@code null}
 */
public record CompassState(UUID search, Target target, Set<InstanceKey> skipped, SearchResult.NothingFound nothingFound, int wear, Long nothingFoundAt) {

	public static final CompassState EMPTY = new CompassState(null, null, Set.of(), null, 0, null);

	public CompassState {
		skipped = Set.copyOf(skipped);
	}

	/** A compass without wear. */
	public CompassState(UUID search, Target target, Set<InstanceKey> skipped, SearchResult.NothingFound nothingFound) {
		this(search, target, skipped, nothingFound, 0, null);
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
		if (nothingFound != null) {
			// The last "nothing found" shows over a kept Target until the next change
			return Status.NOTHING_FOUND;
		}
		if (target != null) {
			if (!target.dimension().equals(currentDimension)) {
				return Status.OTHER_DIMENSION;
			}
			return visited.contains(target.instance()) ? Status.TARGET_VISITED : Status.TARGET;
		}
		return Status.IDLE;
	}

	/** What the needle does for a player standing in {@code currentDimension}. A kept Target is pointed at even while the HUD shows "nothing found". */
	public Needle needle(String currentDimension) {
		if (isSearching()) {
			return Needle.SPIN;
		}
		if (target == null) {
			return Needle.WORLD_SPAWN;
		}
		return target.dimension().equals(currentDimension) ? Needle.TARGET : Needle.SPIN;
	}

	/** Searches left before the compass breaks; {@code durability} 0 or less means it never wears. */
	public int searchesLeft(int durability) {
		return Math.max(0, durability - wear);
	}

	/** A broken compass keeps its Target but refuses "Search" and "Pass". */
	public boolean isBroken(int durability) {
		return durability > 0 && wear >= durability;
	}

	public CompassState withSearch(UUID search) {
		return new CompassState(search, target, skipped, nothingFound, wear, nothingFoundAt);
	}

	public CompassState withTarget(Target target) {
		return new CompassState(search, target, skipped, nothingFound, wear, nothingFoundAt);
	}

	public CompassState withSkipped(Set<InstanceKey> skipped) {
		return new CompassState(search, target, skipped, nothingFound, wear, nothingFoundAt);
	}

	public CompassState withNothingFound(SearchResult.NothingFound nothingFound) {
		return withNothingFound(nothingFound, null);
	}

	public CompassState withNothingFound(SearchResult.NothingFound nothingFound, Long nothingFoundAt) {
		return new CompassState(search, target, skipped, nothingFound, wear, nothingFoundAt);
	}

	public CompassState withWear(int wear) {
		return new CompassState(search, target, skipped, nothingFound, wear, nothingFoundAt);
	}

}
