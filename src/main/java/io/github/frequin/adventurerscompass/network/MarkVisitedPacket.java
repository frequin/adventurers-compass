package io.github.frequin.adventurerscompass.network;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.compass.ServerCompass;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server: "Mark Visited" on the held compass's Target, in any dimension. */
public record MarkVisitedPacket() implements CustomPacketPayload {

	public static final Type<MarkVisitedPacket> TYPE = new Type<MarkVisitedPacket>(Identifier.fromNamespaceAndPath(AdventurersCompass.MODID, "mark_visited"));

	public static final StreamCodec<FriendlyByteBuf, MarkVisitedPacket> CODEC = StreamCodec.unit(new MarkVisitedPacket());

	public static void handle(MarkVisitedPacket packet, ServerPlayNetworking.Context context) {
		HeldCompass.run(context, ServerCompass::markVisited);
	}

	@Override
	public Type<MarkVisitedPacket> type() {
		return TYPE;
	}
}
