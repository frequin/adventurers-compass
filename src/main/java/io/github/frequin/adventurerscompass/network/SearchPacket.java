package io.github.frequin.adventurerscompass.network;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.item.AdventurersCompassItem;
import io.github.frequin.adventurerscompass.util.ItemUtils;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** Client to server: search the nearest Instance of the held compass's Selection. */
public record SearchPacket() implements CustomPacketPayload {

	public static final Type<SearchPacket> TYPE = new Type<SearchPacket>(Identifier.fromNamespaceAndPath(AdventurersCompass.MODID, "search"));

	public static final StreamCodec<FriendlyByteBuf, SearchPacket> CODEC = StreamCodec.unit(new SearchPacket());

	public static void handle(SearchPacket packet, ServerPlayNetworking.Context context) {
		context.server().execute(() -> {
			final ItemStack stack = ItemUtils.getHeldItem(context.player(), AdventurersCompass.ADVENTURERS_COMPASS_ITEM);
			if (!stack.isEmpty()) {
				final AdventurersCompassItem adventurersCompass = (AdventurersCompassItem) stack.getItem();
				adventurersCompass.searchSelection((ServerLevel) context.player().level(), context.player(), context.player().blockPosition(), stack);
			}
		});
	}

	@Override
	public Type<SearchPacket> type() {
		return TYPE;
	}
}
