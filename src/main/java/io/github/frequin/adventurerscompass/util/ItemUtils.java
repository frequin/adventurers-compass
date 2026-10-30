package io.github.frequin.adventurerscompass.util;

import java.util.ArrayList;
import java.util.List;

import io.github.frequin.adventurerscompass.AdventurersCompass;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemUtils {

	public static boolean isCompass(ItemStack stack) {
		return !stack.isEmpty() && stack.getItem() == AdventurersCompass.ADVENTURERS_COMPASS_ITEM;
	}

	/**
	 * Every compass the player has on them: each inventory slot (hands and
	 * armour included) and the stack held on the cursor in an open container.
	 */
	public static List<ItemStack> allCompasses(Player player) {
		List<ItemStack> compasses = new ArrayList<>();
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (isCompass(stack)) {
				compasses.add(stack);
			}
		}
		ItemStack carried = player.containerMenu.getCarried();
		if (isCompass(carried)) {
			compasses.add(carried);
		}
		return compasses;
	}

	public static ItemStack getHeldItem(Player player, Item item) {
		if (!player.getMainHandItem().isEmpty() && player.getMainHandItem().getItem() == item) {
			return player.getMainHandItem();
		} else if (!player.getOffhandItem().isEmpty() && player.getOffhandItem().getItem() == item) {
			return player.getOffhandItem();
		}

		return ItemStack.EMPTY;
	}

}