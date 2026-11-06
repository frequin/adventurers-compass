package io.github.frequin.adventurerscompass.domain;

import java.util.function.ToIntFunction;

/** Cuts a text too wide for its place, so that it ends in "…" rather than running over. */
public final class Ellipsis {

	private static final String ELLIPSIS = "…";

	private Ellipsis() {
	}

	/**
	 * {@code text} when it fits in {@code maxWidth}, else its longest start that
	 * fits with "…" after it, or nothing when not even "…" fits. {@code width}
	 * measures a text.
	 */
	public static String fit(String text, int maxWidth, ToIntFunction<String> width) {
		if (width.applyAsInt(text) <= maxWidth) {
			return text;
		}
		if (width.applyAsInt(ELLIPSIS) > maxWidth) {
			return "";
		}
		int end = text.length();
		while (end > 0) {
			end = text.offsetByCodePoints(end, -1);
			String cut = text.substring(0, end).stripTrailing() + ELLIPSIS;
			if (width.applyAsInt(cut) <= maxWidth) {
				return cut;
			}
		}
		return ELLIPSIS;
	}

}
