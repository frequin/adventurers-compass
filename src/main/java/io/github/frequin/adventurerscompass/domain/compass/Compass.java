package io.github.frequin.adventurerscompass.domain.compass;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import io.github.frequin.adventurerscompass.domain.InstanceKey;
import io.github.frequin.adventurerscompass.domain.search.SearchRequest;
import io.github.frequin.adventurerscompass.domain.search.SearchResult;

/** The compass state machine: each command takes a state and returns an {@link Outcome}. */
public final class Compass {

	/** The rules every compass applies before a search starts. */
	public static final List<LaunchRule> DEFAULT_RULES = List.of(LaunchRule.SOMETHING_SEARCHABLE, LaunchRule.NOT_BROKEN);

	private final List<LaunchRule> rules;

	public Compass() {
		this(DEFAULT_RULES);
	}

	/** A compass with these launch rules, checked in order before "Search" and "Pass". */
	public Compass(List<LaunchRule> rules) {
		this.rules = List.copyOf(rules);
	}

	/** "Search": clears Skipped and searches the nearest Instance of the Selection here. */
	public Outcome search(CompassState state, SearchContext context, UUID id) {
		return launch(state, Set.of(), context, id);
	}

	/** "Pass": adds the Target to Skipped, then searches again. */
	public Outcome pass(CompassState state, SearchContext context, UUID id) {
		if (state.target() == null) {
			return new Outcome.Refused(Refusal.NO_TARGET);
		}
		Set<InstanceKey> skipped = new HashSet<>(state.skipped());
		skipped.add(state.target().instance());
		return launch(state, skipped, context, id);
	}

	private Outcome launch(CompassState state, Set<InstanceKey> skipped, SearchContext context, UUID id) {
		for (LaunchRule rule : rules) {
			Optional<Refusal> refusal = rule.check(state, context);
			if (refusal.isPresent()) {
				return new Outcome.Refused(refusal.get());
			}
		}
		CompassState next = state.withSkipped(skipped).withSearch(id).withNothingFound(null);
		List<Effect> effects = new ArrayList<>();
		if (state.isSearching()) {
			effects.add(new Effect.CancelSearch(state.search()));
		}
		effects.add(new Effect.StartSearch(id, new SearchRequest(context.dimension(), context.origin(), context.searchable(), context.maxRadius(), context.visited(), next.skipped())));
		return new Outcome.Accepted(next, effects);
	}

	/** "Cancel": stops the running search for free; the Target stays. */
	public Outcome cancel(CompassState state) {
		if (!state.isSearching()) {
			return new Outcome.Accepted(state, List.of());
		}
		return new Outcome.Accepted(state.withSearch(null), cancelling(state));
	}

	/** The Selection changed: cancels the search, clears the Target and Skipped, and does not search again. */
	public Outcome selectionChanged(CompassState state) {
		CompassState next = state.withSearch(null).withTarget(null).withSkipped(Set.of()).withNothingFound(null);
		return new Outcome.Accepted(next, cancelling(state));
	}

	/**
	 * The player changed dimension: cancels the search and keeps the Target and
	 * Skipped. Whether the Target is in the Current dimension is read by
	 * {@link CompassState#status}, so it becomes active again on return.
	 */
	public Outcome dimensionChanged(CompassState state) {
		return new Outcome.Accepted(state.withSearch(null), cancelling(state));
	}

	private static List<Effect> cancelling(CompassState state) {
		return state.isSearching() ? List.of(new Effect.CancelSearch(state.search())) : List.of();
	}

	/**
	 * "Mark Visited": marks the Target Visited, wherever its dimension. The
	 * compass itself is unchanged (no wear, no new search).
	 */
	public Outcome markVisited(CompassState state) {
		if (state.target() == null) {
			return new Outcome.Refused(Refusal.NO_TARGET);
		}
		return new Outcome.Accepted(state, List.of(new Effect.MarkVisited(state.target().instance())));
	}

	/**
	 * An Instance just became Visited for the player carrying this compass. When
	 * it is the Target, the Target is kept (the compass keeps pointing at it,
	 * with no new search) and the arrival is signalled.
	 */
	public Outcome instanceVisited(CompassState state, InstanceKey instance) {
		boolean isTarget = state.target() != null && state.target().instance().equals(instance);
		return new Outcome.Accepted(state, isTarget ? List.of(new Effect.TargetVisited(instance)) : List.of());
	}

	/** The search {@code id} ended with {@code result}, without wearing the compass. */
	public CompassState searchEnded(CompassState state, UUID id, SearchResult result) {
		return searchEnded(state, id, result, WearContext.NONE);
	}

	/** The search {@code id} ended with {@code result}; a found Target wears the compass by one, per {@code wear}. */
	public CompassState searchEnded(CompassState state, UUID id, SearchResult result, WearContext wear) {
		if (!id.equals(state.search())) {
			// A search this compass no longer waits for
			return state;
		}
		return switch (result) {
			case SearchResult.Found found -> state.withSearch(null).withTarget(new Target(found.instance(), found.pos())).withNothingFound(null).withWear(worn(state.wear(), wear));
			case SearchResult.NothingFound nothing -> state.withSearch(null).withTarget(null).withNothingFound(nothing);
			case SearchResult.Cancelled cancelled -> state.withSearch(null);
		};
	}

	/** Repair (crafting with a compass): resets wear and keeps the Target, Skipped and any running search. */
	public CompassState repair(CompassState state) {
		return state.withWear(0);
	}

	private static int worn(int current, WearContext wear) {
		return wear.wears() ? Math.min(current + 1, wear.durability()) : current;
	}

	/** "Teleport" to the Target: neither wears the compass nor marks the Target Visited. */
	public Outcome teleport(CompassState state, TeleportContext context) {
		if (!context.allowTeleport() || !(context.operator() || context.creative())) {
			return new Outcome.Refused(Refusal.TELEPORT_NOT_ALLOWED);
		}
		if (state.target() == null) {
			return new Outcome.Refused(Refusal.NO_TARGET);
		}
		if (!state.target().dimension().equals(context.dimension())) {
			return new Outcome.Refused(Refusal.TARGET_IN_OTHER_DIMENSION);
		}
		return new Outcome.Accepted(state, List.of(new Effect.TeleportTo(state.target().pos())));
	}

}
