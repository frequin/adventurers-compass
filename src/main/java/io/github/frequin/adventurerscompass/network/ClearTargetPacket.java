package io.github.frequin.adventurerscompass.network;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.compass.ServerCompass;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server: "Clear Target" on the held compass. */
public record ClearTargetPacket() implements CustomPacketPayload {

	public static final Type<ClearTargetPacket> TYPE = new Type<ClearTargetPacket>(Identifier.fromNamespaceAndPath(AdventurersCompass.MODID, "clear_target"));

	public static final StreamCodec<FriendlyByteBuf, ClearTargetPacket> CODEC = StreamCodec.unit(new ClearTargetPacket());

	public static void handle(ClearTargetPacket packet, ServerPlayNetworking.Context context) {
		HeldCompass.run(context, ServerCompass::clearTarget);
	}

	@Override
	public Type<ClearTargetPacket> type() {
		return TYPE;
	}
}
