package io.github.frequin.adventurerscompass.domain.visited;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import io.github.frequin.adventurerscompass.domain.InstanceKey;

/**
 * The Instances one player has Visited in one world, each with the world day it
 * was marked on. Immutable: every change returns a new set, which suits a value
 * stored in a player data attachment. Instances keep the order they were marked in.
 */
public final class VisitedSet {

	private static final VisitedSet EMPTY = new VisitedSet(new LinkedHashMap<>());

	private final LinkedHashMap<InstanceKey, Long> days;

	private VisitedSet(LinkedHashMap<InstanceKey, Long> days) {
		this.days = days;
	}

	public static VisitedSet empty() {
		return EMPTY;
	}

	/** The set holding these Instances, in this order (for a repeated key, the first day is kept). */
	public static VisitedSet of(Collection<VisitedInstance> instances) {
		var days = new LinkedHashMap<InstanceKey, Long>();
		instances.forEach(visited -> days.putIfAbsent(visited.key(), visited.day()));
		return days.isEmpty() ? EMPTY : new VisitedSet(days);
	}

	/** Marks an Instance Visited on this world day. {@link Marked#isNew()} is false if it already was, and it keeps its day. */
	public Marked mark(InstanceKey key, long day) {
		if (days.containsKey(key)) {
			return new Marked(this, false);
		}
		var marked = new LinkedHashMap<>(days);
		marked.put(key, day);
		return new Marked(new VisitedSet(marked), true);
	}

	/** The result of {@link #mark(InstanceKey, long)}: the new set and whether the Instance was newly Visited. */
	public record Marked(VisitedSet visited, boolean isNew) {
	}

	/** Unmarks an Instance, so searches can find it again. */
	public VisitedSet unmark(InstanceKey key) {
		if (!days.containsKey(key)) {
			return this;
		}
		var unmarked = new LinkedHashMap<>(days);
		unmarked.remove(key);
		return unmarked.isEmpty() ? EMPTY : new VisitedSet(unmarked);
	}

	/** Unmarks every Instance. */
	public VisitedSet clear() {
		return EMPTY;
	}

	public boolean contains(InstanceKey key) {
		return days.containsKey(key);
	}

	/** The Visited Instances with their days, in the order they were marked. */
	public List<VisitedInstance> instances() {
		return days.entrySet().stream().map(entry -> new VisitedInstance(entry.getKey(), entry.getValue())).toList();
	}

	/** The Visited Instances as a set, for a {@code SearchRequest}. */
	public Set<InstanceKey> asSet() {
		return Collections.unmodifiableSet(days.keySet());
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof VisitedSet that && instances().equals(that.instances());
	}

	@Override
	public int hashCode() {
		return days.hashCode();
	}

	@Override
	public String toString() {
		return "VisitedSet" + days;
	}

}
