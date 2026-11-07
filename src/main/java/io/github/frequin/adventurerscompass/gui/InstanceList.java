package io.github.frequin.adventurerscompass.gui;

import java.util.List;
import java.util.function.Consumer;

import io.github.frequin.adventurerscompass.domain.Ellipsis;
import io.github.frequin.adventurerscompass.domain.InstanceKey;
import io.github.frequin.adventurerscompass.domain.rows.InstanceRow;
import io.github.frequin.adventurerscompass.domain.rows.RowLabels;
import io.github.frequin.adventurerscompass.util.RenderUtils;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * The list of the Visited and Skipped tabs: one row per Instance, its name on
 * the left, its details in grey on the right, then a red ✕, the only part of
 * the row that acts with the mouse. With the keyboard, Space, Enter or Delete
 * on the focused row act as its ✕.
 */
@Environment(EnvType.CLIENT)
public class InstanceList extends TransparentList<InstanceList.Row> {

	private static final int ROW_HEIGHT = 11;
	private static final String CROSS = "✕";
	private static final int CROSS_MARGIN = 3;
	private static final int TEXT_GAP = 6;
	private static final int HOVER_COLOR = 0x40ffffff;

	private static final RowLabels LABELS = new RowLabels() {

		@Override
		public String day(long day) {
			return Component.translatable("string.adventurerscompass.visitedDay", day).getString();
		}

		@Override
		public String distance(int blocks) {
			return Component.translatable("string.adventurerscompass.distance", blocks).getString();
		}

		@Override
		public String dimension(String dimension) {
			return CompassTexts.dimensionName(dimension).getString();
		}

	};

	private final Component crossTooltip;
	private final Consumer<InstanceKey> onCross;
	private List<InstanceRow> rows = List.of();
	private Component emptyMessage = Component.empty();

	/** {@code onCross} receives the Instance whose ✕ was clicked. */
	public InstanceList(Minecraft mc, int x, int y, int width, int height, Component crossTooltip, Consumer<InstanceKey> onCross) {
		super(mc, x, y, width, height, ROW_HEIGHT);
		this.crossTooltip = crossTooltip;
		this.onCross = onCross;
	}

	/**
	 * Shows these rows, or {@code emptyMessage} when there are none, keeping the
	 * scroll position. The focus stays on its Instance; once that one is gone, it
	 * goes to the row that took its place, or to the last row.
	 */
	public void setRows(List<InstanceRow> next, Component emptyMessage) {
		this.emptyMessage = emptyMessage;
		if (next.equals(rows)) {
			return;
		}
		InstanceRow focused = isFocused() && getFocused() != null ? getFocused().row : null;
		int focusedIndex = rows.indexOf(focused);
		rows = next;
		double scroll = scrollAmount();
		clearEntries();
		next.forEach(row -> addEntry(new Row(row)));
		setScrollAmount(scroll);
		if (focused != null && !next.isEmpty()) {
			int index = next.stream().map(InstanceRow::instance).toList().indexOf(focused.instance());
			setFocused(children().get(index >= 0 ? index : Math.min(focusedIndex, next.size() - 1)));
		}
	}

	@Override
	public void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.extractWidgetRenderState(guiGraphics, mouseX, mouseY, partialTicks);
		if (rows.isEmpty()) {
			guiGraphics.textWithWordWrap(Minecraft.getInstance().font, emptyMessage, getX() + 4, getY() + 4, getWidth() - 8, 0xff808080);
		}
	}

	public class Row extends ObjectSelectionList.Entry<Row> {

		private final InstanceRow row;
		private final String details;

		Row(InstanceRow row) {
			this.row = row;
			this.details = row.details(LABELS);
		}

		@Override
		public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean isHovering, float partialTick) {
			Font font = Minecraft.getInstance().font;
			int textY = getY() + (getHeight() - font.lineHeight) / 2 + 1;
			if (isHovering) {
				guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), HOVER_COLOR);
			}
			int crossX = crossX(font);
			int detailsX = crossX - TEXT_GAP - font.width(details);
			guiGraphics.text(font, Ellipsis.fit(row.name(), detailsX - TEXT_GAP - getX() - 1, font::width), getX() + 1, textY, 0xffffffff);
			guiGraphics.text(font, details, detailsX, textY, 0xff909090);
			guiGraphics.text(font, CROSS, crossX, textY, 0xffff5555);
			if (isHovering && isOverCross(font, mouseX)) {
				guiGraphics.setTooltipForNextFrame(font, crossTooltip, mouseX, mouseY);
			} else if (RenderUtils.hasKeyboardFocus(InstanceList.this.isFocused() && getFocused() == this)) {
				guiGraphics.setTooltipForNextFrame(font, crossTooltip, crossX, getBottom());
			}
		}

		@Override
		public boolean keyPressed(KeyEvent event) {
			if (!event.isSelection() && event.input() != InputConstants.KEY_DELETE) {
				return false;
			}
			onCross.accept(row.instance());
			return true;
		}

		@Override
		public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
			if (!isOverCross(Minecraft.getInstance().font, event.x())) {
				return false;
			}
			onCross.accept(row.instance());
			return true;
		}

		@Override
		public Component getNarration() {
			return Component.literal(row.name() + ", " + details);
		}

		private int crossX(Font font) {
			return getX() + getWidth() - font.width(CROSS) - 1;
		}

		private boolean isOverCross(Font font, double mouseX) {
			return mouseX >= crossX(font) - CROSS_MARGIN;
		}

	}

}
