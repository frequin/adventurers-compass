package io.github.frequin.adventurerscompass.recipe;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.compass.CompassStacks;
import io.github.frequin.adventurerscompass.compass.ServerCompass;
import io.github.frequin.adventurerscompass.util.ItemUtils;

import com.mojang.serialization.MapCodec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Repair: a worn Adventurer's Compass plus a vanilla compass gives the same
 * compass with its wear reset. Selection, Target, Skipped and the rest of
 * its components are kept, unlike a shapeless recipe that returns a new item.
 */
public class RepairCompassRecipe extends CustomRecipe {

	public static final RepairCompassRecipe INSTANCE = new RepairCompassRecipe();
	public static final MapCodec<RepairCompassRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
	public static final StreamCodec<RegistryFriendlyByteBuf, RepairCompassRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
	public static final RecipeSerializer<RepairCompassRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

	@Override
	public boolean matches(CraftingInput input, Level level) {
		return !wornCompass(input).isEmpty();
	}

	@Override
	public ItemStack assemble(CraftingInput input) {
		ItemStack compass = wornCompass(input);
		if (compass.isEmpty()) {
			return ItemStack.EMPTY;
		}
		ItemStack repaired = compass.copy();
		ServerCompass.repair(repaired);
		return repaired;
	}

	/** The worn Adventurer's Compass when the grid holds exactly it and one vanilla compass, else empty. */
	private static ItemStack wornCompass(CraftingInput input) {
		ItemStack compass = ItemStack.EMPTY;
		int vanilla = 0;
		for (ItemStack stack : input.items()) {
			if (stack.isEmpty()) {
				continue;
			}
			if (ItemUtils.isCompass(stack) && compass.isEmpty()) {
				compass = stack;
			} else if (stack.is(Items.COMPASS)) {
				vanilla++;
			} else {
				return ItemStack.EMPTY;
			}
		}
		if (vanilla != 1 || compass.isEmpty() || CompassStacks.read(compass).wear() == 0) {
			return ItemStack.EMPTY;
		}
		return compass;
	}

	@Override
	public RecipeSerializer<RepairCompassRecipe> getSerializer() {
		return SERIALIZER;
	}

}
