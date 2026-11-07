package io.github.frequin.adventurerscompass.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The name to show for a Group, Structure or dimension no language file names:
 * the path of its id in title case, so "towns:small/village_plains" gives
 * "Small Village Plains".
 */
public final class ReadableName {

	private ReadableName() {
	}

	/** The words of the path, or the id itself when the path holds none. */
	public static String of(String id) {
		String path = id.substring(id.indexOf(':') + 1);
		List<String> words = new ArrayList<>();
		for (String word : path.split("[_/.\\-]")) {
			if (!word.isEmpty()) {
				words.add(word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1));
			}
		}
		return words.isEmpty() ? id : String.join(" ", words);
	}

}
