package io.github.frequin.adventurerscompass.util;

import net.minecraft.world.level.Level;

public final class LevelUtils {

	private LevelUtils() {
	}

	/** The dimension id the domain uses for this level, e.g. {@code "minecraft:overworld"}. */
	public static String dimensionId(Level level) {
		return level.dimension().identifier().toString();
	}

}
