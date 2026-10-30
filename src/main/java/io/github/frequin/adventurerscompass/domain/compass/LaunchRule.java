package io.github.frequin.adventurerscompass.domain.compass;

import java.util.Optional;
import java.util.function.Predicate;

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

	/** Refuses when the compass is broken; it keeps its Target. */
	LaunchRule NOT_BROKEN = (state, context) -> state.isBroken(context.durability()) ? Optional.of(Refusal.BROKEN) : Optional.empty();

	/** Refuses in a Current dimension where the compass does not work ({@code disabledDimensions}). */
	static LaunchRule notIn(Predicate<String> disabledDimension) {
		return (state, context) -> disabledDimension.test(context.dimension()) ? Optional.of(Refusal.DISABLED_DIMENSION) : Optional.empty();
	}

}
