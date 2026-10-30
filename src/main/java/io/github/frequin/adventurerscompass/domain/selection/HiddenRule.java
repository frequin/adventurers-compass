package io.github.frequin.adventurerscompass.domain.selection;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Decides whether a Structure is Hidden: it carries the common tag
 * {@code #c:hidden_from_locator_selection}, or it matches an entry of the
 * server config {@code hiddenStructures}.
 */
public final class HiddenRule {

	/** Common tag a datapack puts on a Structure to Hide it. */
	public static final String COMMON_TAG = "c:hidden_from_locator_selection";

	private final List<String> entries;

	private HiddenRule(List<String> entries) {
		this.entries = List.copyOf(entries);
	}

	/** @param entries {@code hiddenStructures}: glob ids ({@code *}, {@code ?}) and {@code #namespace:tag} entries */
	public static HiddenRule of(List<String> entries) {
		return new HiddenRule(entries);
	}

	/**
	 * @param structureId Structure registry id
	 * @param tags        ids of the tags the Structure carries, without {@code #}
	 */
	public boolean isHidden(String structureId, Set<String> tags) {
		if (tags.contains(COMMON_TAG)) {
			return true;
		}
		for (String entry : entries) {
			if (entry.startsWith("#")) {
				if (tags.contains(entry.substring(1))) {
					return true;
				}
			} else if (globToRegex(entry).matcher(structureId).matches()) {
				return true;
			}
		}
		return false;
	}

	private static Pattern globToRegex(String glob) {
		StringBuilder regex = new StringBuilder();
		for (char c : glob.toCharArray()) {
			switch (c) {
				case '*' -> regex.append(".*");
				case '?' -> regex.append('.');
				default -> regex.append(Pattern.quote(String.valueOf(c)));
			}
		}
		return Pattern.compile(regex.toString());
	}

}
