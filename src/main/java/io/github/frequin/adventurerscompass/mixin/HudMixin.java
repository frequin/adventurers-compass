package io.github.frequin.adventurerscompass.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.config.AdventurersCompassConfig;
import io.github.frequin.adventurerscompass.item.AdventurersCompassItem;
import io.github.frequin.adventurerscompass.util.CompassState;
import io.github.frequin.adventurerscompass.util.ItemUtils;
import io.github.frequin.adventurerscompass.util.RenderUtils;
import io.github.frequin.adventurerscompass.util.StructureUtils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;
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
		if (minecraft.player != null && !minecraft.gui.hud.isHidden() && !minecraft.debugEntries.isOverlayVisible() && (minecraft.gui.screen() == null || (AdventurersCompassConfig.displayWithChatOpen && minecraft.gui.screen() instanceof ChatScreen))) {
			final Player player = minecraft.player;
			final ItemStack stack = ItemUtils.getHeldItem(player, AdventurersCompass.ADVENTURERS_COMPASS_ITEM);
			if (stack != null && stack.getItem() instanceof AdventurersCompassItem) {
				final AdventurersCompassItem compass = (AdventurersCompassItem) stack.getItem();
				if (compass.getCompassState(stack) == CompassState.SEARCHING) {
					RenderUtils.drawConfiguredStringOnHUD(guiGraphics, I18n.get("string.adventurerscompass.status"), 5, 5, 0xffffffff, 0);
					RenderUtils.drawConfiguredStringOnHUD(guiGraphics, I18n.get("string.adventurerscompass.searching"), 5, 5, 0xffaaaaaa, 1);

					RenderUtils.drawConfiguredStringOnHUD(guiGraphics, I18n.get("string.adventurerscompass.structure"), 5, 5, 0xffffffff, 3);
					RenderUtils.drawConfiguredStringOnHUD(guiGraphics, StructureUtils.getStructureName(Identifier.parse(stack.getOrDefault(AdventurersCompass.STRUCTURE_ID, ""))), 5, 5, 0xffaaaaaa, 4);

					RenderUtils.drawConfiguredStringOnHUD(guiGraphics, I18n.get("string.adventurerscompass.radius"), 5, 5, 0xffffffff, 6);
 					RenderUtils.drawConfiguredStringOnHUD(guiGraphics, String.valueOf(stack.getOrDefault(AdventurersCompass.SEARCH_RADIUS, 0)), 5, 5, 0xffaaaaaa, 7);
				} else if (compass.getCompassState(stack) == CompassState.FOUND) {
					RenderUtils.drawConfiguredStringOnHUD(guiGraphics, I18n.get("string.adventurerscompass.status"), 5, 5, 0xffffffff, 0);
					RenderUtils.drawConfiguredStringOnHUD(guiGraphics, I18n.get("string.adventurerscompass.found"), 5, 5, 0xffaaaaaa, 1);

					RenderUtils.drawConfiguredStringOnHUD(guiGraphics, I18n.get("string.adventurerscompass.structure"), 5, 5, 0xffffffff, 3);
					RenderUtils.drawConfiguredStringOnHUD(guiGraphics, StructureUtils.getStructureName(Identifier.parse(stack.getOrDefault(AdventurersCompass.STRUCTURE_ID, ""))), 5, 5, 0xffaaaaaa, 4);

					if (stack.getOrDefault(AdventurersCompass.DISPLAY_COORDS, false)) {
						RenderUtils.drawConfiguredStringOnHUD(guiGraphics, I18n.get("string.adventurerscompass.coordinates"), 5, 5, 0xffffffff, 6);
						RenderUtils.drawConfiguredStringOnHUD(guiGraphics, stack.getOrDefault(AdventurersCompass.FOUND_X, 0) + ", " + stack.getOrDefault(AdventurersCompass.FOUND_Z, 0), 5, 5, 0xffaaaaaa, 7);

						RenderUtils.drawConfiguredStringOnHUD(guiGraphics, I18n.get("string.adventurerscompass.distance"), 5, 5, 0xffffffff, 9);
						RenderUtils.drawConfiguredStringOnHUD(guiGraphics, String.valueOf(StructureUtils.getHorizontalDistanceToLocation(player, stack.getOrDefault(AdventurersCompass.FOUND_X, 0), stack.getOrDefault(AdventurersCompass.FOUND_Z, 0))), 5, 5, 0xffaaaaaa, 10);
					}
				} else if (compass.getCompassState(stack) == CompassState.NOT_FOUND) {
					RenderUtils.drawConfiguredStringOnHUD(guiGraphics, I18n.get("string.adventurerscompass.status"), 5, 5, 0xffffffff, 0);
					RenderUtils.drawConfiguredStringOnHUD(guiGraphics, I18n.get("string.adventurerscompass.notFound"), 5, 5, 0xffaaaaaa, 1);

					RenderUtils.drawConfiguredStringOnHUD(guiGraphics, I18n.get("string.adventurerscompass.structure"), 5, 5, 0xffffffff, 3);
					RenderUtils.drawConfiguredStringOnHUD(guiGraphics, StructureUtils.getStructureName(Identifier.parse(stack.getOrDefault(AdventurersCompass.STRUCTURE_ID, ""))), 5, 5, 0xffaaaaaa, 4);

					RenderUtils.drawConfiguredStringOnHUD(guiGraphics, I18n.get("string.adventurerscompass.radius"), 5, 5, 0xffffffff, 6);
					RenderUtils.drawConfiguredStringOnHUD(guiGraphics, String.valueOf(stack.getOrDefault(AdventurersCompass.SEARCH_RADIUS, 0)), 5, 5, 0xffaaaaaa, 7);
				}
			}
		}
	}

}