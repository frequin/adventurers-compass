package io.github.frequin.adventurerscompass.item;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.config.AdventurersCompassConfig;
import io.github.frequin.adventurerscompass.gui.GuiWrapper;
import io.github.frequin.adventurerscompass.network.SyncPacket;
import io.github.frequin.adventurerscompass.util.CompassState;
import io.github.frequin.adventurerscompass.util.ItemUtils;
import io.github.frequin.adventurerscompass.util.PlayerUtils;
import io.github.frequin.adventurerscompass.util.StructureUtils;
import io.github.frequin.adventurerscompass.domain.BlockXZ;
import io.github.frequin.adventurerscompass.domain.ChunkCoord;
import io.github.frequin.adventurerscompass.domain.InstanceKey;
import io.github.frequin.adventurerscompass.domain.search.SearchRequest;
import io.github.frequin.adventurerscompass.domain.search.SearchResult;
import io.github.frequin.adventurerscompass.search.CompassSearches;
import com.google.common.collect.ListMultimap;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
				final boolean canTeleport = AdventurersCompassConfig.allowTeleport && PlayerUtils.canTeleport(serverLevel.getServer(), player);
				final int maxNextSearches = AdventurersCompassConfig.maxNextSearches;
				final boolean hasInfiniteXp = player.hasInfiniteMaterials();
				final List<Identifier> allowedStructureIds = StructureUtils.getAllowedStructureIds(serverLevel);
				final Map<Identifier, Integer> xpLevels = StructureUtils.getXpLevelsForAllowedStructures(serverLevel, allowedStructureIds);
				final ListMultimap<Identifier, Identifier> generatingDimensions = StructureUtils.getGeneratingDimensionIdsForAllowedStructures(serverLevel, allowedStructureIds);
				ServerPlayNetworking.send(serverPlayer, new SyncPacket(canTeleport, maxNextSearches, hasInfiniteXp, allowedStructureIds, xpLevels, generatingDimensions, StructureUtils.structureIdsToGroupIds(serverLevel)));
			}
		} else {
			if (player instanceof ServerPlayer serverPlayer) {
				CompassSearches.cancel(serverPlayer);
			}
			ItemStack stack = player.getItemInHand(hand);
			clearSearchData(stack);
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
		if (getCompassState(oldStack) == getCompassState(newStack)) {
			return false;
		}
		return super.allowComponentsUpdateAnimation(player, hand, oldStack, newStack);
	}

	public void searchForStructure(ServerLevel level, Player player, BlockPos pos, Identifier structureOrGroupId, boolean isGroup, ItemStack stack) {
		if (!isBroken(stack)) {
			List<Identifier> structureIds = isGroup ? StructureUtils.getStructuresForGroup(level, structureOrGroupId) : List.of(structureOrGroupId);
			startSearch(level, player, pos, stack, structureOrGroupId, isGroup, structureIds, new ArrayList<BlockPos>());

			int xpLevels = StructureUtils.getXpLevelsForStructure(level, structureOrGroupId);
			if (!player.hasInfiniteMaterials() && xpLevels > 0) {
				player.giveExperienceLevels(-xpLevels);
			}
		}
	}

	public void searchForNextStructure(ServerLevel level, Player player, BlockPos pos, ItemStack stack) {
		if (!isBroken(stack)) {
			String structureIdStr = stack.getOrDefault(AdventurersCompass.STRUCTURE_ID, null);
			List<BlockPos> prevPos = stack.getOrDefault(AdventurersCompass.PREV_POS, null);
			boolean isGroup = stack.getOrDefault(AdventurersCompass.IS_GROUP, false);
			if (structureIdStr != null && prevPos != null) {
				Identifier structureId = Identifier.parse(structureIdStr);
				// The compass stores the id of the Structure found, even for a Group search, so find the Group again
				Identifier searchedId = isGroup ? StructureUtils.structureIdsToGroupIds(level).get(structureId) : structureId;
				List<Identifier> structureIds = isGroup ? StructureUtils.getStructuresForGroup(level, searchedId) : List.of(structureId);
				startSearch(level, player, pos, stack, searchedId, isGroup, structureIds, new ArrayList<BlockPos>(prevPos));

				int xpLevels = StructureUtils.getXpLevelsForStructure(level, structureId);
				if (!player.hasInfiniteMaterials() && xpLevels > 0) {
					player.giveExperienceLevels(-xpLevels);
				}
			}
		}
	}

	/**
	 * Starts a nearest-first search on the server's scheduler. Instances found by
	 * earlier "search for next" calls ({@code prevPos}, their locate positions)
	 * are set aside as Skipped.
	 */
	private void startSearch(ServerLevel level, Player player, BlockPos pos, ItemStack stack, Identifier searchedId, boolean isGroup, List<Identifier> structureIds, List<BlockPos> prevPos) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}
		search(stack, searchedId, isGroup);
		String dimension = level.dimension().identifier().toString();
		Set<String> structures = new HashSet<String>();
		for (Identifier id : structureIds) {
			structures.add(id.toString());
		}
		Set<InstanceKey> skipped = new HashSet<InstanceKey>();
		for (BlockPos previous : prevPos) {
			ChunkCoord chunk = ChunkCoord.containing(new BlockXZ(previous.getX(), previous.getZ()));
			for (String structure : structures) {
				skipped.add(new InstanceKey(dimension, structure, chunk));
			}
		}
		int maxRadius = AdventurersCompassConfig.maxRadius;
		stack.set(AdventurersCompass.SEARCH_RADIUS, maxRadius);
		SearchRequest request = new SearchRequest(dimension, new BlockXZ(pos.getX(), pos.getZ()), structures, maxRadius, Set.of(), skipped);
		CompassSearches.start(serverPlayer, request, result -> {
			switch (result) {
				case SearchResult.Found found -> {
					prevPos.add(new BlockPos(found.pos().x(), 0, found.pos().z()));
					succeed(player, stack, Identifier.parse(found.instance().structureId()), isGroup, found.instance().dimension(), found.pos().x(), found.pos().z(), prevPos, AdventurersCompassConfig.displayCoordinates);
				}
				case SearchResult.NothingFound nothing -> fail(stack, searchedId, nothing.radius());
				case SearchResult.Cancelled cancelled -> {
					// Cancelled by a new search (which already reset the compass) or by crouch-use (which clears it).
				}
			}
		});
	}

	public void succeed(Player player, ItemStack stack, Identifier structureID, boolean isGroup, String dimension, int x, int z, List<BlockPos> prevPos, boolean displayCoordinates) {
		clearSearchData(stack);
		setCompassState(stack, CompassState.FOUND);
		stack.set(AdventurersCompass.STRUCTURE_ID, structureID.toString());
		stack.set(AdventurersCompass.IS_GROUP, isGroup);
		stack.set(AdventurersCompass.TARGET_DIMENSION, dimension);
		stack.set(AdventurersCompass.FOUND_X, x);
		stack.set(AdventurersCompass.FOUND_Z, z);
		stack.set(AdventurersCompass.PREV_POS, prevPos);
		stack.set(AdventurersCompass.DISPLAY_COORDS, displayCoordinates);
		damageCompass(player, stack);
	}

	public void fail(ItemStack stack, Identifier structureId, int radius) {
		clearSearchData(stack);
		setCompassState(stack, CompassState.NOT_FOUND);
		stack.set(AdventurersCompass.STRUCTURE_ID, structureId.toString());
		stack.set(AdventurersCompass.SEARCH_RADIUS, radius);
	}

	public void search(ItemStack stack, Identifier structureId, boolean isGroup) {
		clearSearchData(stack);
		stack.set(AdventurersCompass.COMPASS_STATE, CompassState.SEARCHING.getID());
		stack.set(AdventurersCompass.STRUCTURE_ID, structureId.toString());
		stack.set(AdventurersCompass.IS_GROUP, isGroup);
		stack.set(AdventurersCompass.SEARCH_RADIUS, 0);
		stack.set(AdventurersCompass.SAMPLES, 0);
	}

	public void setCompassState(ItemStack stack, CompassState state) {
		stack.set(AdventurersCompass.COMPASS_STATE, state.getID());
	}

	public CompassState getCompassState(ItemStack stack) {
        if (stack.has(AdventurersCompass.COMPASS_STATE)) {
            return CompassState.fromID(stack.get(AdventurersCompass.COMPASS_STATE));
        }
        return null;
	}

	public int getDamage(ItemStack stack) {
		return stack.getOrDefault(AdventurersCompass.DAMAGE, 0);
	}

    private void damageCompass(Player player, ItemStack stack) {
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

	private void clearSearchData(ItemStack stack) {
		stack.remove(AdventurersCompass.COMPASS_STATE);
		stack.remove(AdventurersCompass.STRUCTURE_ID);
		stack.remove(AdventurersCompass.TARGET_DIMENSION);
		stack.remove(AdventurersCompass.FOUND_X);
		stack.remove(AdventurersCompass.FOUND_Z);
		stack.remove(AdventurersCompass.PREV_POS);
		stack.remove(AdventurersCompass.IS_GROUP);
		stack.remove(AdventurersCompass.DISPLAY_COORDS);
		stack.remove(AdventurersCompass.SEARCH_RADIUS);
		stack.remove(AdventurersCompass.SAMPLES);
	}
}
