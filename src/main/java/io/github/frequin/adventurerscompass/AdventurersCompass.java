package io.github.frequin.adventurerscompass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.frequin.adventurerscompass.config.AdventurersCompassConfig;
import io.github.frequin.adventurerscompass.item.AdventurersCompassItem;
import io.github.frequin.adventurerscompass.network.SearchForNextPacket;
import io.github.frequin.adventurerscompass.network.SearchPacket;
import io.github.frequin.adventurerscompass.network.SyncPacket;
import io.github.frequin.adventurerscompass.network.TeleportPacket;
import io.github.frequin.adventurerscompass.search.CompassSearches;
import io.github.frequin.adventurerscompass.visited.PlayerVisited;
import io.github.frequin.adventurerscompass.visited.VisitedDetection;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.mojang.serialization.Codec;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTabs;

public class AdventurersCompass implements ModInitializer {

	public static final String MODID = "adventurerscompass";

	public static final Logger LOGGER = LogManager.getLogger(MODID);
	
	public static final AdventurersCompassItem ADVENTURERS_COMPASS_ITEM = new AdventurersCompassItem();
	
	public static final DataComponentType<String> STRUCTURE_ID = DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build();
	public static final DataComponentType<Integer> COMPASS_STATE = DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build();
	/** Dimension of the Target, e.g. {@code minecraft:overworld}. */
	public static final DataComponentType<String> TARGET_DIMENSION = DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build();
	public static final DataComponentType<Integer> FOUND_X = DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build();
	public static final DataComponentType<Integer> FOUND_Z = DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build();
	public static final DataComponentType<Integer> SEARCH_RADIUS = DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build();
	public static final DataComponentType<Integer> SAMPLES = DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build();
	public static final DataComponentType<Boolean> DISPLAY_COORDS = DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build();
	public static final DataComponentType<Boolean> IS_GROUP = DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build();
	public static final DataComponentType<List<BlockPos>> PREV_POS = DataComponentType.<List<BlockPos>>builder().persistent(BlockPos.CODEC.listOf().xmap(ArrayList::new, list -> list)).networkSynchronized(ByteBufCodecs.collection(ArrayList::new, BlockPos.STREAM_CODEC)).build();
	public static final DataComponentType<Integer> DAMAGE = DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build();
	
	public static boolean synced;
	public static boolean canTeleport;
	public static int maxNextSearches;
	public static boolean infiniteXp;
	public static List<Identifier> allowedStructures;
	public static Map<Identifier, Integer> xpLevelsForAllowedStructures;
	public static ListMultimap<Identifier, Identifier> dimensionsForAllowedStructures;
	public static Map<Identifier, Identifier> structureIdsToGroupIds;

	@Override
	public void onInitialize() {
		AdventurersCompassConfig.load();
		
		Registry.register(BuiltInRegistries.ITEM, AdventurersCompassItem.KEY, ADVENTURERS_COMPASS_ITEM);
		
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath(MODID, "structure_id"), STRUCTURE_ID);
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath(MODID, "compass_state"), COMPASS_STATE);
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath(MODID, "target_dimension"), TARGET_DIMENSION);
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath(MODID, "found_x"), FOUND_X);
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath(MODID, "found_z"), FOUND_Z);
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath(MODID, "search_radius"), SEARCH_RADIUS);
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath(MODID, "samples"), SAMPLES);
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath(MODID, "display_coords"), DISPLAY_COORDS);
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath(MODID, "is_group"), IS_GROUP);
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath(MODID, "prev_pos"), PREV_POS);
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath(MODID, "damage"), DAMAGE);

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> entries.accept(ADVENTURERS_COMPASS_ITEM));

		PayloadTypeRegistry.serverboundPlay().register(SearchPacket.TYPE, SearchPacket.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SearchForNextPacket.TYPE, SearchForNextPacket.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(TeleportPacket.TYPE, TeleportPacket.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SyncPacket.TYPE, SyncPacket.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(SearchPacket.TYPE, SearchPacket::handle);
		ServerPlayNetworking.registerGlobalReceiver(SearchForNextPacket.TYPE, SearchForNextPacket::handle);
		ServerPlayNetworking.registerGlobalReceiver(TeleportPacket.TYPE, TeleportPacket::handle);

		CompassSearches.register();
		PlayerVisited.register();
		VisitedDetection.register();
		
		allowedStructures = new ArrayList<Identifier>();
		xpLevelsForAllowedStructures = new HashMap<Identifier, Integer>();
		dimensionsForAllowedStructures = ArrayListMultimap.create();
		structureIdsToGroupIds = new HashMap<Identifier, Identifier>();
	}

}