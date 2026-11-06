package io.github.frequin.adventurerscompass.domain.selection;

/**
 * An element of the Selection that the search leaves out in the Current
 * dimension. It stays on the compass.
 *
 * @param id     the Group or Structure id
 * @param reason why the search leaves it out
 */
public record UnsearchableElement(String id, Reason reason) {

	public enum Reason {
		/** Not generated in the Current dimension, or no longer in the catalogue. */
		ABSENT_FROM_DIMENSION,
		/** Hidden by a datapack tag or the server config. */
		HIDDEN
	}

}
