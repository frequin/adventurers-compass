package io.github.frequin.adventurerscompass.domain.compass;

import java.util.List;

/** What a command of {@link Compass} did. */
public sealed interface Outcome {

	/** The command applies: store {@code state} on the compass, then run {@code effects} in order. */
	record Accepted(CompassState state, List<Effect> effects) implements Outcome {

		public Accepted {
			effects = List.copyOf(effects);
		}

	}

	/** The command is refused and the compass is unchanged. */
	record Refused(Refusal reason) implements Outcome {
	}

}
