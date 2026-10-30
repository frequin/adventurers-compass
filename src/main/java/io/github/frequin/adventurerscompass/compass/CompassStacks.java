package io.github.frequin.adventurerscompass.compass;

import java.util.Set;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.domain.compass.CompassState;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

/**
 * Reads and writes the domain {@link CompassState} on a compass item, one
 * component per field: {@code search}, {@code target}, {@code skipped},
 * {@code nothing_found}, {@code damage} (wear). An absent component is an empty field. Used on both
 * sides (the HUD and the needle read it too).
 */
public final class CompassStacks {

	private CompassStacks() {
	}

	public static CompassState read(ItemStack stack) {
		return new CompassState(
				stack.get(AdventurersCompass.SEARCH),
				stack.get(AdventurersCompass.TARGET),
				stack.getOrDefault(AdventurersCompass.SKIPPED, Set.of()),
				stack.get(AdventurersCompass.NOTHING_FOUND),
				stack.getOrDefault(AdventurersCompass.DAMAGE, 0));
	}

	public static void write(ItemStack stack, CompassState state) {
		setOrRemove(stack, AdventurersCompass.SEARCH, state.search());
		setOrRemove(stack, AdventurersCompass.TARGET, state.target());
		setOrRemove(stack, AdventurersCompass.SKIPPED, state.skipped().isEmpty() ? null : state.skipped());
		setOrRemove(stack, AdventurersCompass.NOTHING_FOUND, state.nothingFound());
		setOrRemove(stack, AdventurersCompass.DAMAGE, state.wear() == 0 ? null : state.wear());
	}

	private static <T> void setOrRemove(ItemStack stack, DataComponentType<T> type, T value) {
		if (value == null) {
			stack.remove(type);
		} else {
			stack.set(type, value);
		}
	}

}
