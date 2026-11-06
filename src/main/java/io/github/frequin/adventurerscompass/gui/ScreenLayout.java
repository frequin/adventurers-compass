package io.github.frequin.adventurerscompass.gui;

/** How the compass screen shares its width. Plain numbers, so that the labels can be measured against them outside the game. */
final class ScreenLayout {

	/** The narrowest screen the automatic GUI scale gives, in GUI pixels. Every label should fit at this width. */
	static final int MIN_SCREEN_WIDTH = 320;
	static final int MARGIN = 10;
	static final int GAP = 5;
	/** Kept free on each side of a button's label. */
	static final int LABEL_PADDING = 2;
	/** Added to a label to size the small buttons of the list bar. */
	static final int SMALL_BUTTON_PADDING = 12;

	private ScreenLayout() {
	}

	/** Width of each of the three tabs. */
	static int tabWidth(int screenWidth) {
		return (screenWidth - 2 * MARGIN - 2 * GAP) / 3;
	}

	/** Where the left column of the Search tab ends. */
	static int leftColumnRight(int screenWidth) {
		return screenWidth * 3 / 5;
	}

	/** Width of the right column of the Search tab, and of its full-width buttons. */
	static int rightColumnWidth(int screenWidth) {
		return screenWidth - leftColumnRight(screenWidth) - 2 * MARGIN;
	}

	/** The room a button of this width leaves for its label. */
	static int labelWidth(int buttonWidth) {
		return buttonWidth - 2 * LABEL_PADDING;
	}

	/** The widest "Clear Target" can be: it shares the "Target" caption line of the right column. */
	static int clearTargetRoom(int columnWidth, int captionWidth) {
		return columnWidth - captionWidth - GAP;
	}

	/** The width of a button sized to its label, within {@code room}. */
	static int buttonWidth(int labelWidth, int room) {
		return Math.min(labelWidth + 2 * LABEL_PADDING, room);
	}

}
