package io.github.frequin.adventurerscompass.util;

import io.github.frequin.adventurerscompass.config.AdventurersCompassConfig;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

@Environment(EnvType.CLIENT)
public class RenderUtils {
	
	private static final Minecraft mc = Minecraft.getInstance();
	private static final Font font = mc.font;

	public static void drawStringLeft(GuiGraphicsExtractor guiGraphics, String string, Font font, int x, int y, int color) {
		guiGraphics.text(font, string, x, y, color, true);
	}

	public static void drawStringRight(GuiGraphicsExtractor guiGraphics, String string, Font font, int x, int y, int color) {
		guiGraphics.text(font, string, x - font.width(string), y, color, true);
	}

	public static void drawConfiguredStringOnHUD(GuiGraphicsExtractor guiGraphics, String string, int xOffset, int yOffset, int color, int relLineOffset) {
		yOffset += (relLineOffset + AdventurersCompassConfig.overlayLineOffset) * 9;
		if (AdventurersCompassConfig.overlaySide == OverlaySide.LEFT) {
			drawStringLeft(guiGraphics, string, font, xOffset + 2, yOffset + 2, color);
		} else {
			drawStringRight(guiGraphics, string, font, mc.getWindow().getGuiScaledWidth() - xOffset - 2, yOffset + 2, color);
		}
	}
	
	/**
	 * Frames an element that has the focus while the player moves with the keyboard.
	 * A click leaves the focus on what was clicked, so the frame goes with the mouse.
	 */
	public static void drawKeyboardFocus(GuiGraphicsExtractor guiGraphics, boolean focused, int x, int y, int width, int height) {
		if (hasKeyboardFocus(focused)) {
			guiGraphics.outline(x, y, width, height, 0xffffffff);
		}
	}

	/** Whether an element that has the focus got it from the keyboard, so that it shows its frame and tooltip. */
	public static boolean hasKeyboardFocus(boolean focused) {
		return focused && mc.getLastInputType().isKeyboard();
	}

	public static int getBackgroundColor(boolean active, boolean hovered) {
		float state = 2;
		if (!active) {
			state = 5;
		} else if (hovered) {
			state = 3;
		}
		final float f = state / 2 * 0.9F + 0.1F;
		final int color = (int) (255.0F * f);

		return color / 2 << 24;
	}

}