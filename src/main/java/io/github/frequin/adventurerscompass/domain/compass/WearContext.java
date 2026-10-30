package io.github.frequin.adventurerscompass.domain.compass;

/**
 * What decides whether a found Target wears the compass.
 *
 * @param durability        the configured {@code compassDurability}; 0 or less means the compass never wears
 * @param infiniteMaterials the player is in creative: no wear
 */
public record WearContext(int durability, boolean infiniteMaterials) {

	/** Nothing wears: used where a result can only be a cancellation. */
	public static final WearContext NONE = new WearContext(0, true);

	boolean wears() {
		return durability > 0 && !infiniteMaterials;
	}

}
