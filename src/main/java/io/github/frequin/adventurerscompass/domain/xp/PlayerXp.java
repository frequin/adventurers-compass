package io.github.frequin.adventurerscompass.domain.xp;

/**
 * The XP side of a player, read when they click and again when a Target is found.
 *
 * @param levels   their current experience level
 * @param infinite creative ({@code hasInfiniteMaterials}): no cost, threshold ignored
 */
public record PlayerXp(int levels, boolean infinite) {

	/** No cost and no threshold, as in creative. */
	public static final PlayerXp UNLIMITED = new PlayerXp(0, true);

	/** Whether the player may launch "Search" or "Pass" under {@code threshold}. */
	public boolean canLaunch(XpThreshold threshold) {
		return infinite || levels >= threshold.levels();
	}

	/** Levels to take for a found Target of real cost {@code cost}: capped at the levels left, nothing in creative. */
	public int payment(int cost) {
		return infinite ? 0 : Math.max(0, Math.min(cost, levels));
	}

}
