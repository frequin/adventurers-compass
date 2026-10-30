package io.github.frequin.adventurerscompass.domain.search;

import java.util.Set;

import io.github.frequin.adventurerscompass.domain.BlockXZ;
import io.github.frequin.adventurerscompass.domain.InstanceKey;

/**
 * What to search for.
 *
 * @param dimension    the Current dimension, searched alone
 * @param origin       where the player stood when the search started
 * @param structureIds the searchable Structures
 * @param maxRadius    horizontal radius in blocks; farther Instances are ignored
 * @param visited      Instances set aside because they are Visited
 * @param skipped      Instances set aside because they are Skipped
 */
public record SearchRequest(String dimension, BlockXZ origin, Set<String> structureIds, int maxRadius, Set<InstanceKey> visited, Set<InstanceKey> skipped) {

	public SearchRequest {
		structureIds = Set.copyOf(structureIds);
		visited = Set.copyOf(visited);
		skipped = Set.copyOf(skipped);
	}

}
