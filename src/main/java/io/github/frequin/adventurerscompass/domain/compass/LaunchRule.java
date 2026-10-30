package io.github.frequin.adventurerscompass.domain.compass;

import java.util.Optional;

/**
 * A rule checked before "Search" or "Pass" starts a search. The first rule
 * that refuses wins and the compass is left unchanged. Later tickets add
 * their own (broken compass, disabled dimension, XP threshold) to
 * {@link Compass#Compass(java.util.List)}.
 */
@FunctionalInterface
public interface LaunchRule {

	Optional<Refusal> check(CompassState state, SearchContext context);

	/** Refuses when the Selection has nothing searchable in the Current dimension. */
	LaunchRule SOMETHING_SEARCHABLE = (state, context) -> context.searchable().isEmpty() ? Optional.of(Refusal.NOTHING_SEARCHABLE) : Optional.empty();

	/** Refuses when the player has fewer levels than the highest real cost of the searchable Structures; creative passes. */
	LaunchRule ENOUGH_XP = (state, context) -> context.playerXp().canLaunch(context.xpThreshold()) ? Optional.empty() : Optional.of(Refusal.NOT_ENOUGH_XP);

}
