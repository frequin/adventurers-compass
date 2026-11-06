package io.github.frequin.adventurerscompass;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.frequin.adventurerscompass.codec.CompassCodecs;
import io.github.frequin.adventurerscompass.codec.SelectionCodecs;
import io.github.frequin.adventurerscompass.compass.ServerCompass;
import io.github.frequin.adventurerscompass.domain.InstanceKey;
import io.github.frequin.adventurerscompass.domain.compass.Target;
import io.github.frequin.adventurerscompass.domain.search.SearchResult;
import io.github.frequin.adventurerscompass.config.AdventurersCompassConfig;
import io.github.frequin.adventurerscompass.domain.selection.Catalogue;
import io.github.frequin.adventurerscompass.domain.selection.Selection;
import io.github.frequin.adventurerscompass.item.AdventurersCompassItem;
import io.github.frequin.adventurerscompass.network.CancelPacket;
import io.github.frequin.adventurerscompass.network.ClearTargetPacket;
import io.github.frequin.adventurerscompass.network.MarkVisitedPacket;
import io.github.frequin.adventurerscompass.network.PassPacket;
import io.github.frequin.adventurerscompass.network.SearchPacket;
import io.github.frequin.adventurerscompass.network.SyncPacket;
import io.github.frequin.adventurerscompass.network.TeleportPacket;
import io.github.frequin.adventurerscompass.network.UnmarkVisitedPacket;
import io.github.frequin.adventurerscompass.network.UpdateSelectionPacket;
import io.github.frequin.adventurerscompass.recipe.RepairCompassRecipe;
import io.github.frequin.adventurerscompass.search.CompassSearches;
import io.github.frequin.adventurerscompass.visited.PlayerVisited;
import io.github.frequin.adventurerscompass.visited.VisitedDetection;
import com.mojang.serialization.Codec;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTabs;

public class AdventurersCompass implements ModInitializer {

	public static final String MODID = "adventurerscompass";

	public static final Logger LOGGER = LogManager.getLogger(MODID);
	
	public static final AdventurersCompassItem ADVENTURERS_COMPASS_ITEM = new AdventurersCompassItem();
	
	/** The Selection of this compass; absent means empty. */
	public static final DataComponentType<Selection> SELECTION = DataComponentType.<Selection>builder().persistent(SelectionCodecs.SELECTION).networkSynchronized(SelectionCodecs.SELECTION_STREAM).build();
	/** The compass state ({@link io.github.frequin.adventurerscompass.compass.CompassStacks}): the running search, absent when none runs. */
	public static final DataComponentType<UUID> SEARCH = DataComponentType.<UUID>builder().persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC).build();
	/** The Target: dimension, Structure, start chunk and the X/Z the needle points at. */
	public static final DataComponentType<Target> TARGET = DataComponentType.<Target>builder().persistent(CompassCodecs.TARGET).networkSynchronized(CompassCodecs.TARGET_STREAM).build();
	/** Instances passed over with "Pass"; absent means none. */
	public static final DataComponentType<Set<InstanceKey>> SKIPPED = DataComponentType.<Set<InstanceKey>>builder().persistent(CompassCodecs.SKIPPED).networkSynchronized(CompassCodecs.SKIPPED_STREAM).build();
	/** The last search's "nothing found", shown by the HUD. */
	public static final DataComponentType<SearchResult.NothingFound> NOTHING_FOUND = DataComponentType.<SearchResult.NothingFound>builder().persistent(CompassCodecs.NOTHING_FOUND).networkSynchronized(CompassCodecs.NOTHING_FOUND_STREAM).build();
	/** The server's {@code displayCoordinates}, copied on the compass when a Target is found, for the HUD. */
	public static final DataComponentType<Boolean> DISPLAY_COORDS = DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build();
	/** Wear: Targets found since the last repair. */
	public static final DataComponentType<Integer> DAMAGE = DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build();
	/** The server's {@code compassDurability}, copied on the compass while it ticks in an inventory, for the wear bar and tooltip. */
	public static final DataComponentType<Integer> DURABILITY = DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build();
	
	public static boolean synced;
	public static boolean canTeleport;
	public static boolean infiniteXp;
	/** Client side: the Current dimension is in the server's {@code disabledDimensions}. */
	public static boolean dimensionDisabled;
	/** Client side: the Current dimension and the catalogue last sent by the server. */
	public static String currentDimension = "";
	public static Catalogue catalogue = Catalogue.EMPTY;
	public static Map<String, Integer> xpLevels = Map.of();

	@Override
	public void onInitialize() {
		AdventurersCompassConfig.load();
		
		Registry.register(BuiltInRegistries.ITEM, AdventurersCompassItem.KEY, ADVENTURERS_COMPASS_ITEM);
		
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath(MODID, "selection"), SELECTION);
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath(MODID, "search"), SEARCH);
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath(MODID, "target"), TARGET);
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath(MODID, "skipped"), SKIPPED);
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath(MODID, "nothing_found"), NOTHING_FOUND);
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath(MODID, "display_coords"), DISPLAY_COORDS);
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath(MODID, "damage"), DAMAGE);
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath(MODID, "durability"), DURABILITY);
		Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Identifier.fromNamespaceAndPath(MODID, "repair_compass"), RepairCompassRecipe.SERIALIZER);

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> entries.accept(ADVENTURERS_COMPASS_ITEM));

		PayloadTypeRegistry.serverboundPlay().register(SearchPacket.TYPE, SearchPacket.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(PassPacket.TYPE, PassPacket.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(CancelPacket.TYPE, CancelPacket.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(TeleportPacket.TYPE, TeleportPacket.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(UpdateSelectionPacket.TYPE, UpdateSelectionPacket.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(MarkVisitedPacket.TYPE, MarkVisitedPacket.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(UnmarkVisitedPacket.TYPE, UnmarkVisitedPacket.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ClearTargetPacket.TYPE, ClearTargetPacket.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SyncPacket.TYPE, SyncPacket.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(SearchPacket.TYPE, SearchPacket::handle);
		ServerPlayNetworking.registerGlobalReceiver(PassPacket.TYPE, PassPacket::handle);
		ServerPlayNetworking.registerGlobalReceiver(CancelPacket.TYPE, CancelPacket::handle);
		ServerPlayNetworking.registerGlobalReceiver(TeleportPacket.TYPE, TeleportPacket::handle);
		ServerPlayNetworking.registerGlobalReceiver(UpdateSelectionPacket.TYPE, UpdateSelectionPacket::handle);
		ServerPlayNetworking.registerGlobalReceiver(MarkVisitedPacket.TYPE, MarkVisitedPacket::handle);
		ServerPlayNetworking.registerGlobalReceiver(UnmarkVisitedPacket.TYPE, UnmarkVisitedPacket::handle);
		ServerPlayNetworking.registerGlobalReceiver(ClearTargetPacket.TYPE, ClearTargetPacket::handle);

		CompassSearches.register();
		ServerCompass.register();
		PlayerVisited.register();
		VisitedDetection.register();
	}

}