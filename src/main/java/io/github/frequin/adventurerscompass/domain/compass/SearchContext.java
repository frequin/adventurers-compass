package io.github.frequin.adventurerscompass.domain.compass;

import java.util.Set;

import io.github.frequin.adventurerscompass.domain.BlockXZ;
import io.github.frequin.adventurerscompass.domain.InstanceKey;

/**
 * The world around a "Search" or "Pass", as the adapter sees it when the
 * player clicks. Later rules (XP, wear, disabled dimensions) add fields here.
 *
 * @param dimension  the Current dimension
 * @param origin     where the player stands
 * @param searchable the Selection resolved for the Current dimension
 * @param maxRadius  the configured search radius
 * @param visited    the player's Visited Instances
 * @param durability the configured {@code compassDurability}; 0 or less means the compass never breaks
 */
public record SearchContext(String dimension, BlockXZ origin, Set<String> searchable, int maxRadius, Set<InstanceKey> visited, int durability) {

	public SearchContext {
		searchable = Set.copyOf(searchable);
		visited = Set.copyOf(visited);
	}

	/** A context where the compass never breaks. */
	public SearchContext(String dimension, BlockXZ origin, Set<String> searchable, int maxRadius, Set<InstanceKey> visited) {
		this(dimension, origin, searchable, maxRadius, visited, 0);
	}

}
