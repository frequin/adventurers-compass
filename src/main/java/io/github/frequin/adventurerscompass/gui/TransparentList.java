package io.github.frequin.adventurerscompass.gui;

import io.github.frequin.adventurerscompass.util.RenderUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.util.Mth;

/** A scrolling list of the compass screen: a dark background, a flat scrollbar, and rows that only show the keyboard focus. */
abstract class TransparentList<E extends ObjectSelectionList.Entry<E>> extends ObjectSelectionList<E> {

	TransparentList(Minecraft mc, int x, int y, int width, int height, int rowHeight) {
		super(mc, width, height, y, rowHeight);
		setX(x);
	}

	@Override
	protected int scrollBarX() {
		return getRowLeft() + getRowWidth();
	}

	@Override
	public int getRowWidth() {
		return getWidth() - 6;
	}

	@Override
	public void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
		enableScissor(guiGraphics);
		guiGraphics.fill(getX(), getY(), getX() + getWidth(), getBottom(), RenderUtils.getBackgroundColor(true, false));
		extractListItems(guiGraphics, mouseX, mouseY, partialTicks);
		guiGraphics.disableScissor();
		extractScrollbar(guiGraphics, mouseX, mouseY);
	}

	@Override
	protected void extractSelection(GuiGraphicsExtractor guiGraphics, E entry, int backgroundColor) {
		RenderUtils.drawKeyboardFocus(guiGraphics, hasFocus(entry), entry);
	}

	/** The row that has the focus, or null when the focus is outside the list. */
	E focusedEntry() {
		return isFocused() ? getFocused() : null;
	}

	/** Whether this row has the focus, the list having it too. */
	boolean hasFocus(E entry) {
		return entry != null && focusedEntry() == entry;
	}

	@Override
	protected void extractScrollbar(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
		if (scrollable()) {
			int left = scrollBarX();
			int right = left + 6;
			int height = (int) ((float) ((getBottom() - getY()) * (getBottom() - getY())) / (float) contentHeight());
			height = Mth.clamp(height, 32, getBottom() - getY() - 8);
			int top = (int) scrollAmount() * (getBottom() - getY() - height) / maxScrollAmount() + getY();
			if (top < getY()) {
				top = getY();
			}
			guiGraphics.fill(left, getY(), right, getBottom(), RenderUtils.getBackgroundColor(false, false));
			guiGraphics.fill(left, top, right, top + height, RenderUtils.getBackgroundColor(true, true));
		}
	}

}
