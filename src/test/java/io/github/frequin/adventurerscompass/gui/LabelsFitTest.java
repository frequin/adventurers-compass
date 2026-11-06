package io.github.frequin.adventurerscompass.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

/**
 * Every label of the compass screen fits its button, in every language, on the
 * narrowest screen the automatic GUI scale gives. Counts and costs are the
 * widest a default game reaches: 999 Instances in a list, a 10-level search
 * (5 levels, times 2 outside the Overworld).
 */
class LabelsFitTest {

	private static final String PREFIX = "string.adventurerscompass.";
	private static final String COUNT = "999";
	private static final String LEVELS = "10";
	/** What the filter box of the list tabs must keep beside the sort button and "clear all". */
	private static final int MIN_FILTER_WIDTH = 60;

	private static final GlyphWidths WIDTH = new GlyphWidths();
	private static final JsonObject ENGLISH = LanguageFiles.english();

	@Test
	void widthsAreTheGameFontWidths() {
		assertEquals(6, WIDTH.of("a"));
		assertEquals(2, WIDTH.of("i"));
		assertEquals(4, WIDTH.of(" "));
		assertEquals(6, WIDTH.of("é"));
		assertEquals(24, WIDTH.of("Hello"));
	}

	@Test
	void everyLabelFitsItsButtonInEveryLanguage() {
		List<String> tooWide = new ArrayList<>();
		for (Path file : LanguageFiles.all()) {
			tooWide.addAll(new Language(file).labelsTooWide());
		}
		assertEquals(List.of(), tooWide);
	}

	private static final class Language {

		private final String name;
		private final JsonObject texts;
		private final List<String> tooWide = new ArrayList<>();

		Language(Path file) {
			this.name = file.getFileName().toString();
			this.texts = LanguageFiles.read(file);
		}

		List<String> labelsTooWide() {
			int tab = ScreenLayout.labelWidth(ScreenLayout.tabWidth(ScreenLayout.MIN_SCREEN_WIDTH));
			check(text("tabSearch"), tab);
			check(text("visitedList", COUNT), tab);
			check(text("skippedList", COUNT), tab);

			int column = ScreenLayout.rightColumnWidth(ScreenLayout.MIN_SCREEN_WIDTH);
			int full = ScreenLayout.labelWidth(column);
			String search = text("searchSelection");
			check(search, full);
			check(text("searchCost", search, LEVELS), full);
			check(text("searchCostUpTo", search, LEVELS), full);
			check(text("cancelSearch"), full);
			check(text("pass"), full);
			check(text("markVisited"), full);
			check(text("teleport"), full);
			check(text("clearSelection"), full);

			// "Clear Target" shares the "Target" caption line
			check(text("clearTarget"), ScreenLayout.labelWidth(ScreenLayout.clearTargetRoom(column, WIDTH.of(text("target")))));

			// The list bar sizes its buttons to their labels: the filter box takes what they leave
			int bar = ScreenLayout.MIN_SCREEN_WIDTH - 2 * ScreenLayout.MARGIN - MIN_FILTER_WIDTH - 2 * ScreenLayout.GAP - 2 * ScreenLayout.SMALL_BUTTON_PADDING;
			int widestSort = List.of("sortRecent", "sortNearest", "sortName").stream().mapToInt(key -> WIDTH.of(text(key))).max().orElseThrow();
			check(text("unmarkAll", COUNT), bar - widestSort);
			check(text("removeAll", COUNT), bar - widestSort);
			return tooWide;
		}

		private void check(String label, int room) {
			if (WIDTH.of(label) > room) {
				tooWide.add(name + ": \"" + label + "\" is " + WIDTH.of(label) + " wide, room for " + room);
			}
		}

		private String text(String key, String... arguments) {
			String full = PREFIX + key;
			String text = (texts.has(full) ? texts : ENGLISH).get(full).getAsString();
			for (String argument : arguments) {
				text = text.replaceFirst("%s", argument);
			}
			return text;
		}

	}

}
