package io.github.frequin.adventurerscompass.util;

import io.github.frequin.adventurerscompass.AdventurersCompass;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemUtils {

	public static boolean isCompass(ItemStack stack) {
		return !stack.isEmpty() && stack.getItem() == AdventurersCompass.ADVENTURERS_COMPASS_ITEM;
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