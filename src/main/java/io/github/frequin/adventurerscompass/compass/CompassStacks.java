package io.github.frequin.adventurerscompass.compass;

import java.util.Set;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.domain.compass.CompassState;
import io.github.frequin.adventurerscompass.domain.compass.NothingFoundMessage;
import io.github.frequin.adventurerscompass.domain.search.SearchResult;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

/**
 * Reads and writes the domain {@link CompassState} on a compass item, one
 * component per field: {@code search}, {@code target}, {@code skipped},
 * {@code nothing_found}, {@code nothing_found_at}, {@code damage} (wear). An absent component is an empty field. Used on both
 * sides (the HUD and the needle read it too).
 */
public final class CompassStacks {

	private CompassStacks() {
	}

	public static CompassState read(ItemStack stack) {
		SearchResult.NothingFound nothingFound = stack.get(AdventurersCompass.NOTHING_FOUND);
		return new CompassState(
				stack.get(AdventurersCompass.SEARCH),
				stack.get(AdventurersCompass.TARGET),
				stack.getOrDefault(AdventurersCompass.SKIPPED, Set.of()),
				nothingFound == null ? null : new NothingFoundMessage(nothingFound, stack.get(AdventurersCompass.NOTHING_FOUND_AT)),
				stack.getOrDefault(AdventurersCompass.DAMAGE, 0));
	}

	public static void write(ItemStack stack, CompassState state) {
		setOrRemove(stack, AdventurersCompass.SEARCH, state.search());
		setOrRemove(stack, AdventurersCompass.TARGET, state.target());
		setOrRemove(stack, AdventurersCompass.SKIPPED, state.skipped().isEmpty() ? null : state.skipped());
		NothingFoundMessage nothingFound = state.nothingFound();
		setOrRemove(stack, AdventurersCompass.NOTHING_FOUND, nothingFound == null ? null : nothingFound.result());
		setOrRemove(stack, AdventurersCompass.NOTHING_FOUND_AT, nothingFound == null ? null : nothingFound.shownAt());
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
