package io.github.frequin.adventurerscompass.domain.visited;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import io.github.frequin.adventurerscompass.domain.InstanceKey;

/**
 * The Instances one player has Visited in one world. Immutable: every change
 * returns a new set, which suits a value stored in a player data attachment.
 * Keys keep the order they were marked in.
 */
public final class VisitedSet {

	private static final VisitedSet EMPTY = new VisitedSet(new LinkedHashSet<>());

	private final Set<InstanceKey> keys;

	private VisitedSet(LinkedHashSet<InstanceKey> keys) {
		this.keys = Collections.unmodifiableSet(keys);
	}

	public static VisitedSet empty() {
		return EMPTY;
	}

	/** The set holding these keys, in this order (duplicates are dropped). */
	public static VisitedSet of(Collection<InstanceKey> keys) {
		return keys.isEmpty() ? EMPTY : new VisitedSet(new LinkedHashSet<>(keys));
	}

	/** Marks an Instance Visited. {@link Marked#isNew()} is false if it already was. */
	public Marked mark(InstanceKey key) {
		if (keys.contains(key)) {
			return new Marked(this, false);
		}
		var marked = new LinkedHashSet<>(keys);
		marked.add(key);
		return new Marked(new VisitedSet(marked), true);
	}

	/** The result of {@link #mark(InstanceKey)}: the new set and whether the Instance was newly Visited. */
	public record Marked(VisitedSet visited, boolean isNew) {
	}

	/** Unmarks an Instance, so searches can find it again. */
	public VisitedSet unmark(InstanceKey key) {
		if (!keys.contains(key)) {
			return this;
		}
		var unmarked = new LinkedHashSet<>(keys);
		unmarked.remove(key);
		return unmarked.isEmpty() ? EMPTY : new VisitedSet(unmarked);
	}

	public boolean contains(InstanceKey key) {
		return keys.contains(key);
	}

	/** The Visited Instances, in the order they were marked. */
	public List<InstanceKey> list() {
		return List.copyOf(keys);
	}

	/** The Visited Instances as a set, for a {@code SearchRequest}. */
	public Set<InstanceKey> asSet() {
		return keys;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof VisitedSet that && list().equals(that.list());
	}

	@Override
	public int hashCode() {
		return keys.hashCode();
	}

	@Override
	public String toString() {
		return "VisitedSet" + keys;
	}

}
