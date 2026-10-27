package io.github.frequin.adventurerscompass.gui;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.item.AdventurersCompassItem;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class GuiWrapper {
	
	public static void openGUI(Level level, Player player, ItemStack stack) {
		Minecraft.getInstance().gui.setScreen(new AdventurersCompassScreen(level, player, stack, (AdventurersCompassItem) stack.getItem(), AdventurersCompass.allowedStructures));
	}

}
