package io.github.frequin.adventurerscompass.network;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.compass.ServerCompass;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server: "Search" with the held compass (the nearest Instance of its Selection). */
public record SearchPacket() implements CustomPacketPayload {

	public static final Type<SearchPacket> TYPE = new Type<SearchPacket>(Identifier.fromNamespaceAndPath(AdventurersCompass.MODID, "search"));

	public static final StreamCodec<FriendlyByteBuf, SearchPacket> CODEC = StreamCodec.unit(new SearchPacket());

	public static void handle(SearchPacket packet, ServerPlayNetworking.Context context) {
		HeldCompass.run(context, ServerCompass::search);
	}

	@Override
	public Type<SearchPacket> type() {
		return TYPE;
	}
}
