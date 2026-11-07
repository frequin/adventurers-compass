package io.github.frequin.adventurerscompass.domain.search;

import io.github.frequin.adventurerscompass.domain.BlockXZ;
import io.github.frequin.adventurerscompass.domain.InstanceKey;

/** How a search ended. */
public sealed interface SearchResult {

	/** The nearest Instance that is neither Visited nor Skipped. */
	record Found(InstanceKey instance, BlockXZ pos) implements SearchResult {
	}

	/** No eligible Instance within the radius; counts the Instances set aside on the way. */
	record NothingFound(String dimension, int radius, int visitedIgnored, int skippedIgnored) implements SearchResult {
	}

	/** Cancelled before it ended. */
	record Cancelled() implements SearchResult {
	}

}
