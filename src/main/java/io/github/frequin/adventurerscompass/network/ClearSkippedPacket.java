package io.github.frequin.adventurerscompass.network;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.compass.ServerCompass;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server: clear the Skipped of the held compass. */
public record ClearSkippedPacket() implements CustomPacketPayload {

	public static final Type<ClearSkippedPacket> TYPE = new Type<ClearSkippedPacket>(Identifier.fromNamespaceAndPath(AdventurersCompass.MODID, "clear_skipped"));

	public static final StreamCodec<FriendlyByteBuf, ClearSkippedPacket> CODEC = StreamCodec.unit(new ClearSkippedPacket());

	public static void handle(ClearSkippedPacket packet, ServerPlayNetworking.Context context) {
		HeldCompass.run(context, ServerCompass::clearSkipped);
	}

	@Override
	public Type<ClearSkippedPacket> type() {
		return TYPE;
	}
}
