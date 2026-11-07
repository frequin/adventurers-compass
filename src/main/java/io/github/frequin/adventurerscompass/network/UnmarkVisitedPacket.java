package io.github.frequin.adventurerscompass.network;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.codec.CompassCodecs;
import io.github.frequin.adventurerscompass.domain.InstanceKey;
import io.github.frequin.adventurerscompass.visited.PlayerVisited;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server: unmark one of the player's Visited Instances, which becomes searchable again. */
public record UnmarkVisitedPacket(InstanceKey instance) implements CustomPacketPayload {

	public static final Type<UnmarkVisitedPacket> TYPE = new Type<UnmarkVisitedPacket>(Identifier.fromNamespaceAndPath(AdventurersCompass.MODID, "unmark_visited"));

	public static final StreamCodec<FriendlyByteBuf, UnmarkVisitedPacket> CODEC = CompassCodecs.INSTANCE_KEY_STREAM.map(UnmarkVisitedPacket::new, UnmarkVisitedPacket::instance).cast();

	public static void handle(UnmarkVisitedPacket packet, ServerPlayNetworking.Context context) {
		context.server().execute(() -> PlayerVisited.unmark(context.player(), packet.instance()));
	}

	@Override
	public Type<UnmarkVisitedPacket> type() {
		return TYPE;
	}
}
