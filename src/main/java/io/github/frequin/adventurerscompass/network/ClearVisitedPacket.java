package io.github.frequin.adventurerscompass.network;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.visited.PlayerVisited;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server: unmark all of the player's Visited Instances. */
public record ClearVisitedPacket() implements CustomPacketPayload {

	public static final Type<ClearVisitedPacket> TYPE = new Type<ClearVisitedPacket>(Identifier.fromNamespaceAndPath(AdventurersCompass.MODID, "clear_visited"));

	public static final StreamCodec<FriendlyByteBuf, ClearVisitedPacket> CODEC = StreamCodec.unit(new ClearVisitedPacket());

	public static void handle(ClearVisitedPacket packet, ServerPlayNetworking.Context context) {
		context.server().execute(() -> PlayerVisited.clear(context.player()));
	}

	@Override
	public Type<ClearVisitedPacket> type() {
		return TYPE;
	}
}
