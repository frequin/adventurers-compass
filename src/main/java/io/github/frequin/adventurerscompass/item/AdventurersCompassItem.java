package io.github.frequin.adventurerscompass.item;

import java.util.Map;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.compass.ServerCompass;
import io.github.frequin.adventurerscompass.compass.ServerXp;
import io.github.frequin.adventurerscompass.config.AdventurersCompassConfig;
import io.github.frequin.adventurerscompass.gui.GuiWrapper;
import io.github.frequin.adventurerscompass.network.SyncPacket;
import io.github.frequin.adventurerscompass.util.ItemUtils;
import io.github.frequin.adventurerscompass.util.PlayerUtils;
import io.github.frequin.adventurerscompass.util.StructureUtils;
import io.github.frequin.adventurerscompass.domain.selection.Catalogue;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
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
            if (isBroken(player.getItemInHand(hand))) {
                return InteractionResult.PASS;
            }
            
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
				final Map<String, Integer> xpLevels = ServerXp.realCosts(catalogue, dimension);
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
        int max = AdventurersCompassConfig.compassDurability;
        if (max > 0) {
            int damage = stack.getOrDefault(AdventurersCompass.DAMAGE, 0);
            return damage > 0;
        }
        return false;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int max = AdventurersCompassConfig.compassDurability;
        if (max > 0) {
            int damage = stack.getOrDefault(AdventurersCompass.DAMAGE, 0);
            return Math.clamp(Math.round(13.0f * (1.0f - (float) damage / max)), 0, 13);
        }
        return 13;
    }

    @Override
    public int getBarColor(ItemStack stack) {
        int max = AdventurersCompassConfig.compassDurability;
        int damage = stack.getOrDefault(AdventurersCompass.DAMAGE, 0);
        float f = max > 0 ? (float) damage / max : 0.0f;
        return Mth.hsvToRgb(Math.max(0.0F, (1.0F - f) / 3.0F), 1.0F, 1.0F);
    }

	@Override
	public boolean allowComponentsUpdateAnimation(Player player, InteractionHand hand, ItemStack oldStack, ItemStack newStack) {
		// The compass state changes often (search, Target, Skipped): never replay the equip animation for it
		return false;
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot) {
		ServerCompass.interruptIfOrphaned(stack);
	}

	public int getDamage(ItemStack stack) {
		return stack.getOrDefault(AdventurersCompass.DAMAGE, 0);
	}

    /** One more found Target (Pass included): wears the compass, except in creative. */
    public void wear(Player player, ItemStack stack) {
        int max = AdventurersCompassConfig.compassDurability;
        if (!player.hasInfiniteMaterials() && max > 0) {
            int damage = getDamage(stack) + 1;
            stack.set(AdventurersCompass.DAMAGE, damage);
        }
    }

	public boolean isBroken(ItemStack stack) {
		int max = AdventurersCompassConfig.compassDurability;
		if (max > 0) {
			return getDamage(stack) >= max;
		}
		return false;
	}
}
