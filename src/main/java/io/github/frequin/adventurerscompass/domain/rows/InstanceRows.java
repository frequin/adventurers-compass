package io.github.frequin.adventurerscompass.domain.rows;

import java.text.Collator;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Function;

import io.github.frequin.adventurerscompass.domain.BlockXZ;
import io.github.frequin.adventurerscompass.domain.InstanceKey;
import io.github.frequin.adventurerscompass.domain.selection.Catalogue;
import io.github.frequin.adventurerscompass.domain.selection.TreeFilter;
import io.github.frequin.adventurerscompass.domain.visited.VisitedSet;

/**
 * Builds the rows of the Visited and Skipped tabs for a player standing at
 * {@code here} in {@code dimension}: filtered as the Selection tree is, then sorted.
 */
public final class InstanceRows {

	private final String dimension;
	private final BlockXZ here;
	private final Catalogue catalogue;
	private final Function<String, String> displayName;

	/** {@code displayName} gives the name shown for a Structure id. */
	public InstanceRows(String dimension, BlockXZ here, Catalogue catalogue, Function<String, String> displayName) {
		this.dimension = dimension;
		this.here = here;
		this.catalogue = catalogue;
		this.displayName = displayName;
	}

	public List<InstanceRow> visited(VisitedSet visited, TreeFilter filter, InstanceSort sort) {
		return sorted(visited.instances().stream().map(instance -> row(instance.key(), OptionalLong.of(instance.day()))).toList(), filter, sort);
	}

	public List<InstanceRow> skipped(Set<InstanceKey> skipped, TreeFilter filter, InstanceSort sort) {
		return sorted(skipped.stream().map(instance -> row(instance, OptionalLong.empty())).toList(), filter, sort);
	}

	private InstanceRow row(InstanceKey instance, OptionalLong day) {
		BlockXZ center = instance.startChunk().center();
		OptionalInt distance = instance.dimension().equals(dimension) ? OptionalInt.of(here.distance(center)) : OptionalInt.empty();
		return new InstanceRow(instance, displayName.apply(instance.structureId()), day, center, distance);
	}

	/** {@code rows} are in the order they were marked, when they have one. */
	private List<InstanceRow> sorted(Collection<InstanceRow> rows, TreeFilter filter, InstanceSort sort) {
		List<InstanceRow> kept = rows.stream().filter(row -> filter.matches(row.instance().structureId(), catalogue, displayName)).toList();
		return switch (sort) {
			case RECENT -> kept.reversed();
			case NEAREST -> kept.stream().sorted(nearest()).toList();
			case NAME -> kept.stream().sorted(Comparator.comparing(InstanceRow::name, Collator.getInstance(Locale.ROOT)).thenComparing(nearest())).toList();
		};
	}

	/** A total order, so that rows do not swap places from one refresh to the next. */
	private Comparator<InstanceRow> nearest() {
		return Comparator.<InstanceRow, Boolean>comparing(row -> row.distance().isEmpty())
				.thenComparingLong(row -> here.distanceSq(row.center()))
				.thenComparing(InstanceRow::instance, InstanceKey.ORDER);
	}

}
