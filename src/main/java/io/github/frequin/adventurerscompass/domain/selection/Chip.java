package io.github.frequin.adventurerscompass.domain.selection;

/**
 * One element of the Selection as the screen shows it: a removable chip.
 *
 * @param kind  whether the chip stands for a Group or a Structure
 * @param id    the Group or Structure id
 * @param state whether the search in the Current dimension uses it
 */
public record Chip(Kind kind, String id, ChipState state) {

	public enum Kind {
		GROUP, STRUCTURE
	}

	public boolean isActive() {
		return state == ChipState.ACTIVE;
	}

}
