package io.github.frequin.adventurerscompass.item;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.compass.CompassStacks;
import io.github.frequin.adventurerscompass.compass.ServerCompass;
import io.github.frequin.adventurerscompass.config.AdventurersCompassConfig;
import io.github.frequin.adventurerscompass.gui.GuiWrapper;
import io.github.frequin.adventurerscompass.network.SyncPacket;
import io.github.frequin.adventurerscompass.util.ItemUtils;
import io.github.frequin.adventurerscompass.util.PlayerUtils;
import io.github.frequin.adventurerscompass.util.StructureUtils;
import io.github.frequin.adventurerscompass.domain.selection.Catalogue;
import io.github.frequin.adventurerscompass.domain.selection.CatalogueEntry;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class AdventurersCompassItem extends Item {

	public static final String NAME = "adventurerscompass";

	public static final ResourceKey<Item> KEY = ResourceKey.create(BuiltInRegistries.ITEM.key(), Identifier.fromNamespaceAndPath(AdventurersCompass.MODID, NAME));

	public AdventurersCompassItem() {
		super(new Properties().setId(KEY).stacksTo(1));
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!player.isCrouching()) {
			// A broken compass still opens: it shows its Target and refuses "Search" and "Pass"
			if (level.isClientSide()) {
				final ItemStack stack = ItemUtils.getHeldItem(player, AdventurersCompass.ADVENTURERS_COMPASS_ITEM);
				GuiWrapper.openGUI(level, player, stack);
			} else {
				final ServerLevel serverLevel = (ServerLevel) level;
				final ServerPlayer serverPlayer = (ServerPlayer) player;
				final boolean canTeleport = PlayerUtils.canTeleport(serverLevel.getServer(), player);
				final boolean hasInfiniteXp = player.hasInfiniteMaterials();
				final String dimension = serverLevel.dimension().identifier().toString();
				final boolean dimensionDisabled = AdventurersCompassConfig.isDisabledDimension(dimension);
				// The compass lists nothing in a disabled dimension
				final Catalogue catalogue = dimensionDisabled ? Catalogue.EMPTY : StructureUtils.buildCatalogue(serverLevel);
				final Map<String, Integer> xpLevels = new HashMap<String, Integer>();
				for (CatalogueEntry entry : catalogue.entries()) {
					xpLevels.put(entry.structureId(), StructureUtils.getXpLevelsForStructure(serverLevel, Identifier.parse(entry.structureId())));
				}
				ServerPlayNetworking.send(serverPlayer, new SyncPacket(canTeleport, hasInfiniteXp, dimension, catalogue, xpLevels, dimensionDisabled));
			}
		} else if (player instanceof ServerPlayer serverPlayer) {
			// Crouch-use: "Cancel"
			ServerCompass.cancel(serverPlayer, player.getItemInHand(hand));
		}

		return InteractionResult.CONSUME;
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return durability(stack) > 0 && CompassStacks.read(stack).wear() > 0;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		int durability = durability(stack);
		if (durability <= 0) {
			return MAX_BAR_WIDTH;
		}
		return Math.clamp(Math.round(MAX_BAR_WIDTH * (float) CompassStacks.read(stack).searchesLeft(durability) / durability), 0, MAX_BAR_WIDTH);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		int durability = durability(stack);
		float left = durability > 0 ? (float) CompassStacks.read(stack).searchesLeft(durability) / durability : 1.0f;
		return Mth.hsvToRgb(Math.max(0.0F, left / 3.0F), 1.0F, 1.0F);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		int durability = durability(stack);
		if (durability > 0) {
			tooltip.accept(Component.translatable("string.adventurerscompass.searchesLeft", CompassStacks.read(stack).searchesLeft(durability), durability).withStyle(ChatFormatting.GRAY));
		}
	}

	@Override
	public boolean allowComponentsUpdateAnimation(Player player, InteractionHand hand, ItemStack oldStack, ItemStack newStack) {
		// The compass state changes often (search, Target, Skipped): never replay the equip animation for it
		return false;
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot) {
		ServerCompass.interruptIfOrphaned(stack);
		// The client reads the server's durability from the compass, for the wear bar, the tooltip and the screen
		if (durability(stack) != AdventurersCompassConfig.compassDurability) {
			stack.set(AdventurersCompass.DURABILITY, AdventurersCompassConfig.compassDurability);
		}
	}

	/** The server's {@code compassDurability} as last copied on this compass; 0 means it never wears. */
	public static int durability(ItemStack stack) {
		return stack.getOrDefault(AdventurersCompass.DURABILITY, 0);
	}

}
