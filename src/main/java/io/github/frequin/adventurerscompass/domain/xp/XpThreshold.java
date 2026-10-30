package io.github.frequin.adventurerscompass.domain.xp;

import java.util.Collection;

/**
 * The levels a player needs to launch "Search" or "Pass": the highest real
 * cost among the searchable Structures. When {@code varies}, the costs differ
 * and the button reads "≤ N levels".
 */
public record XpThreshold(int levels, boolean varies) {

	public static final XpThreshold FREE = new XpThreshold(0, false);

	/** The threshold of these real costs. */
	public static XpThreshold of(Collection<Integer> costs) {
		int max = costs.stream().mapToInt(Integer::intValue).max().orElse(0);
		int min = costs.stream().mapToInt(Integer::intValue).min().orElse(0);
		return new XpThreshold(max, min != max);
	}

}
