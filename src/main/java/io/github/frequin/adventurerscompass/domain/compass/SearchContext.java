package io.github.frequin.adventurerscompass.domain.compass;

import java.util.Set;

import io.github.frequin.adventurerscompass.domain.BlockXZ;
import io.github.frequin.adventurerscompass.domain.InstanceKey;
import io.github.frequin.adventurerscompass.domain.xp.PlayerXp;
import io.github.frequin.adventurerscompass.domain.xp.XpCosts;
import io.github.frequin.adventurerscompass.domain.xp.XpThreshold;

/**
 * The world around a "Search" or "Pass", as the adapter sees it when the
 * player clicks. Later rules (XP, wear, disabled dimensions) add fields here.
 *
 * @param dimension  the Current dimension
 * @param origin     where the player stands
 * @param searchable the Selection resolved for the Current dimension
 * @param maxRadius  the configured search radius
 * @param visited    the player's Visited Instances
 * @param xpCosts    the XP scale
 * @param playerXp   the player's levels, or creative
 */
public record SearchContext(String dimension, BlockXZ origin, Set<String> searchable, int maxRadius, Set<InstanceKey> visited, XpCosts xpCosts, PlayerXp playerXp) {

	public SearchContext {
		searchable = Set.copyOf(searchable);
		visited = Set.copyOf(visited);
	}

	/** A context where searching costs no XP. */
	public SearchContext(String dimension, BlockXZ origin, Set<String> searchable, int maxRadius, Set<InstanceKey> visited) {
		this(dimension, origin, searchable, maxRadius, visited, XpCosts.FREE, PlayerXp.UNLIMITED);
	}

	/** The levels needed to launch here. */
	public XpThreshold xpThreshold() {
		return xpCosts.threshold(searchable, dimension);
	}

}
