package io.github.frequin.adventurerscompass.gui;

import io.github.frequin.adventurerscompass.util.RenderUtils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * A line of text that does nothing but can take the keyboard focus, so that its
 * tooltip shows on focus as well as on hover. (x, y) is where the text starts.
 */
@Environment(EnvType.CLIENT)
final class FocusableText extends AbstractWidget {

	/** Room between the text and the focus frame. */
	private static final int PADDING = 2;

	private final Font font;
	private final int color;

	FocusableText(Font font, int x, int y, int color) {
		super(x - PADDING, y - PADDING, 0, font.lineHeight + 2 * PADDING, Component.empty());
		this.font = font;
		this.color = color;
		visible = false;
	}

	void setText(String text) {
		setMessage(Component.literal(text));
		setWidth(font.width(text) + 2 * PADDING);
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
		guiGraphics.text(font, getMessage(), getX() + PADDING, getY() + PADDING, color);
		RenderUtils.drawKeyboardFocus(guiGraphics, isFocused(), this);
	}

	/** A click neither acts nor takes the focus. */
	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		return false;
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		output.add(NarratedElementType.TITLE, getMessage());
	}

}
