package io.github.frequin.adventurerscompass.gui;

import io.github.frequin.adventurerscompass.domain.Ellipsis;
import io.github.frequin.adventurerscompass.util.RenderUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** A flat button. A label too wide for it is cut with "…" and shown in full in a tooltip, on hover or keyboard focus. */
public class TransparentButton extends Button {

	private boolean hasOwnTooltip;

	public TransparentButton(int x, int y, int width, int height, Component label, OnPress onPress) {
		super(x, y, width, height, label, onPress, DEFAULT_NARRATION);
	}

	/** A tooltip set here, such as why the button is greyed, wins over the full label. */
	@Override
	public void setTooltip(Tooltip tooltip) {
		super.setTooltip(tooltip);
		hasOwnTooltip = tooltip != null;
	}

	@Override
	public void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
		if (visible) {
			int color = RenderUtils.getBackgroundColor(active, isHovered);

			guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), color);
			RenderUtils.drawKeyboardFocus(guiGraphics, isFocused(), getX(), getY(), getWidth(), getHeight());
			extractLabel(guiGraphics, mouseX, mouseY, 0xffffffff);
		}
	}

	/** Draws the label centred, cut to the button's width. */
	protected void extractLabel(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, int color) {
		Font font = Minecraft.getInstance().font;
		String label = getMessage().getString();
		String shown = Ellipsis.fit(label, ScreenLayout.labelWidth(getWidth()), font::width);
		guiGraphics.centeredText(font, Component.literal(shown).withStyle(getMessage().getStyle()), getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, color);
		if (hasOwnTooltip || shown.equals(label)) {
			return;
		}
		if (isHovered) {
			guiGraphics.setTooltipForNextFrame(font, getMessage(), mouseX, mouseY);
		} else if (RenderUtils.hasKeyboardFocus(isFocused())) {
			guiGraphics.setTooltipForNextFrame(font, getMessage(), getX(), getBottom());
		}
	}

}
