package io.github.frequin.adventurerscompass.gui;

import io.github.frequin.adventurerscompass.util.RenderUtils;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** A tab of the compass screen. The open tab is lighter, underlined and white. */
public class TabButton extends TransparentButton {

	private final boolean open;

	public TabButton(int x, int y, int width, int height, Component label, boolean open, OnPress onPress) {
		super(x, y, width, height, label, onPress);
		this.open = open;
	}

	@Override
	public void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
		guiGraphics.fill(getX(), getY(), getRight(), getBottom(), RenderUtils.getBackgroundColor(true, open || isHovered));
		if (open) {
			guiGraphics.fill(getX(), getBottom() - 1, getRight(), getBottom(), 0xffffffff);
		}
		extractLabel(guiGraphics, mouseX, mouseY, open ? 0xffffffff : 0xffaaaaaa);
	}

}
