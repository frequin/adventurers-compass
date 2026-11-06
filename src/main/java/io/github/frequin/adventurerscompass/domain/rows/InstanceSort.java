package io.github.frequin.adventurerscompass.domain.rows;

import java.util.List;

/** The orders of the Visited and Skipped tabs. */
public enum InstanceSort {

	/** Last marked first. */
	RECENT,
	/** The Current dimension first, by distance. */
	NEAREST,
	/** By name, then by {@link #NEAREST}. */
	NAME;

	/** The orders of the Visited tab; the first is its default. */
	public static final List<InstanceSort> VISITED_ORDERS = List.of(RECENT, NEAREST, NAME);
	/** The orders of the Skipped tab, which keeps no date; the first is its default. */
	public static final List<InstanceSort> SKIPPED_ORDERS = List.of(NEAREST, NAME);

	/** The order after this one in {@code orders}, back to the first after the last. */
	public InstanceSort next(List<InstanceSort> orders) {
		return orders.get((orders.indexOf(this) + 1) % orders.size());
	}

}
