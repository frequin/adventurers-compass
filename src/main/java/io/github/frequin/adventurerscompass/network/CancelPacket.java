package io.github.frequin.adventurerscompass.network;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.compass.ServerCompass;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server: "Cancel" the held compass's search. */
public record CancelPacket() implements CustomPacketPayload {

	public static final Type<CancelPacket> TYPE = new Type<CancelPacket>(Identifier.fromNamespaceAndPath(AdventurersCompass.MODID, "cancel"));

	public static final StreamCodec<FriendlyByteBuf, CancelPacket> CODEC = StreamCodec.unit(new CancelPacket());

	public static void handle(CancelPacket packet, ServerPlayNetworking.Context context) {
		HeldCompass.run(context, ServerCompass::cancel);
	}

	@Override
	public Type<CancelPacket> type() {
		return TYPE;
	}
}
