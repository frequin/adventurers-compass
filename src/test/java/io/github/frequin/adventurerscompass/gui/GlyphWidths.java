package io.github.frequin.adventurerscompass.gui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Text widths as the game's default font gives them, read from the glyph
 * bitmaps in the Minecraft jar: a glyph is as wide as its rightmost inked
 * column, scaled to the font's height, plus 1. A character the bitmaps lack
 * comes from Unifont, which the jar does not hold: it counts as Unifont's
 * widest glyph, so a measure is never too small.
 */
final class GlyphWidths {

	private static final int SPACE = 4;
	private static final int UNIFONT_WIDEST = 9;

	private final Map<Integer, Integer> advances = new HashMap<>();

	GlyphWidths() {
		advances.put((int) ' ', SPACE);
		JsonObject font = JsonParser.parseReader(new InputStreamReader(open("assets/minecraft/font/include/default.json"), StandardCharsets.UTF_8)).getAsJsonObject();
		for (JsonElement provider : font.getAsJsonArray("providers")) {
			read(provider.getAsJsonObject());
		}
	}

	int of(String text) {
		return text.codePoints().map(codePoint -> advances.getOrDefault(codePoint, UNIFONT_WIDEST)).sum();
	}

	private void read(JsonObject provider) {
		BufferedImage image = image("assets/minecraft/textures/" + provider.get("file").getAsString().replace("minecraft:", ""));
		var rows = provider.getAsJsonArray("chars");
		int cellHeight = image.getHeight() / rows.size();
		int cellWidth = image.getWidth() / rows.get(0).getAsString().codePointCount(0, rows.get(0).getAsString().length());
		float scale = (provider.has("height") ? provider.get("height").getAsInt() : 8) / (float) cellHeight;
		for (int row = 0; row < rows.size(); row++) {
			int[] codePoints = rows.get(row).getAsString().codePoints().toArray();
			for (int column = 0; column < codePoints.length; column++) {
				if (codePoints[column] != 0) {
					int inked = inkedWidth(image, column * cellWidth, row * cellHeight, cellWidth, cellHeight);
					advances.putIfAbsent(codePoints[column], (int) (0.5 + inked * scale) + 1);
				}
			}
		}
	}

	private static int inkedWidth(BufferedImage image, int left, int top, int cellWidth, int cellHeight) {
		for (int x = cellWidth - 1; x >= 0; x--) {
			for (int y = 0; y < cellHeight; y++) {
				if (image.getRGB(left + x, top + y) >>> 24 != 0) {
					return x + 1;
				}
			}
		}
		return 0;
	}

	private static BufferedImage image(String path) {
		try (InputStream in = open(path)) {
			return ImageIO.read(in);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static InputStream open(String path) {
		InputStream in = GlyphWidths.class.getClassLoader().getResourceAsStream(path);
		if (in == null) {
			throw new IllegalStateException("not on the test classpath: " + path);
		}
		return in;
	}

}
