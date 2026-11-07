package io.github.frequin.adventurerscompass.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.compass.CompassStacks;
import io.github.frequin.adventurerscompass.gui.CompassTexts;
import io.github.frequin.adventurerscompass.config.AdventurersCompassConfig;
import io.github.frequin.adventurerscompass.util.ItemUtils;
import io.github.frequin.adventurerscompass.util.RenderUtils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
@Mixin(Hud.class)
public class HudMixin {
	
	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V", at = @At(value = "TAIL"))
	private void renderCompassInfo(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, CallbackInfo info) {
		if (!AdventurersCompassConfig.hudEnabled || minecraft.player == null || minecraft.gui.hud.isHidden() || minecraft.debugEntries.isOverlayVisible()) {
			return;
		}
		if (minecraft.gui.screen() != null && !(AdventurersCompassConfig.displayWithChatOpen && minecraft.gui.screen() instanceof ChatScreen)) {
			return;
		}
		final Player player = minecraft.player;
		final ItemStack stack = ItemUtils.getHeldItem(player, AdventurersCompass.ADVENTURERS_COMPASS_ITEM);
		if (!ItemUtils.isCompass(stack)) {
			return;
		}
		List<Component> lines = CompassTexts.hudLines(CompassStacks.read(stack), player, stack.getOrDefault(AdventurersCompass.DISPLAY_COORDS, false));
		for (int line = 0; line < lines.size(); line++) {
			RenderUtils.drawConfiguredStringOnHUD(guiGraphics, lines.get(line).getString(), 5, 5, line == 0 ? 0xffffffff : 0xffaaaaaa, line);
		}
	}

}
