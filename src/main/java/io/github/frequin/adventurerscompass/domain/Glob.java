package io.github.frequin.adventurerscompass.domain;

import java.util.regex.Pattern;

/**
 * An id pattern from the config ({@code hiddenStructures},
 * {@code perStructureXpLevels}): {@code *} matches any run of characters,
 * {@code ?} exactly one, everything else is literal. The whole id must match.
 */
public final class Glob {

	private final Pattern pattern;

	private Glob(Pattern pattern) {
		this.pattern = pattern;
	}

	public static Glob of(String glob) {
		StringBuilder regex = new StringBuilder();
		StringBuilder literal = new StringBuilder();
		for (char c : glob.toCharArray()) {
			if (c == '*' || c == '?') {
				if (!literal.isEmpty()) {
					regex.append(Pattern.quote(literal.toString()));
					literal.setLength(0);
				}
				regex.append(c == '*' ? ".*" : ".");
			} else {
				literal.append(c);
			}
		}
		if (!literal.isEmpty()) {
			regex.append(Pattern.quote(literal.toString()));
		}
		return new Glob(Pattern.compile(regex.toString(), Pattern.DOTALL));
	}

	public boolean matches(String id) {
		return pattern.matcher(id).matches();
	}

}
