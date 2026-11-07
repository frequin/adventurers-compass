package io.github.frequin.adventurerscompass.domain.compass;

/**
 * Who asks for "Teleport", and where, as the adapter sees it.
 *
 * @param dimension     the Current dimension
 * @param allowTeleport the server's {@code allowTeleport}
 * @param operator      the player is an op (or has cheats on in single player)
 * @param creative      the player is in creative mode
 */
public record TeleportContext(String dimension, boolean allowTeleport, boolean operator, boolean creative) {

	/** Whether "Teleport" is open to this player: {@code allowTeleport}, then op or creative. */
	public boolean allowed() {
		return allowTeleport && (operator || creative);
	}

}
