package io.github.frequin.adventurerscompass.util;

import net.minecraft.world.level.Level;

public final class LevelUtils {

	private static final long TICKS_PER_DAY = 24000;

	private LevelUtils() {
	}

	/** The dimension id the domain uses for this level, e.g. {@code "minecraft:overworld"}. */
	public static String dimensionId(Level level) {
		return level.dimension().identifier().toString();
	}

	/** The world day: days counted by the Overworld clock from 0, the same in every dimension. */
	public static long worldDay(Level level) {
		return level.getOverworldClockTime() / TICKS_PER_DAY;
	}

}
