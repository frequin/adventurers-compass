package io.github.frequin.adventurerscompass.domain.compass;

import java.util.UUID;

import io.github.frequin.adventurerscompass.domain.search.SearchRequest;

/** Something the adapter must do outside the compass after a command. */
public sealed interface Effect {

	/** Start this search; its result comes back through {@link Compass#searchEnded}. */
	record StartSearch(UUID id, SearchRequest request) implements Effect {
	}

	/** Stop this search if it still runs. */
	record CancelSearch(UUID id) implements Effect {
	}

}
