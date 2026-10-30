package io.github.frequin.adventurerscompass.domain.compass;

import java.util.UUID;

import io.github.frequin.adventurerscompass.domain.BlockXZ;
import io.github.frequin.adventurerscompass.domain.InstanceKey;
import io.github.frequin.adventurerscompass.domain.search.SearchRequest;

/** Something the adapter must do outside the compass after a command. */
public sealed interface Effect {

	/** Start this search; its result comes back through {@link Compass#searchEnded}. */
	record StartSearch(UUID id, SearchRequest request) implements Effect {
	}

	/** Stop this search if it still runs. */
	record CancelSearch(UUID id) implements Effect {
	}

	/** Mark this Instance Visited for the player. */
	record MarkVisited(InstanceKey instance) implements Effect {
	}

	/** The Target became Visited: play the arrival sound (when enabled). */
	record TargetVisited(InstanceKey instance) implements Effect {
	}

	/** Move the player to this position of the Current dimension (the height is the adapter's). */
	record TeleportTo(BlockXZ pos) implements Effect {
	}

}
