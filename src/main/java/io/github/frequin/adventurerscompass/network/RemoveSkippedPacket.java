package io.github.frequin.adventurerscompass.network;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.codec.CompassCodecs;
import io.github.frequin.adventurerscompass.compass.ServerCompass;
import io.github.frequin.adventurerscompass.domain.InstanceKey;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server: remove one Instance from the Skipped of the held compass, which can be found again. */
public record RemoveSkippedPacket(InstanceKey instance) implements CustomPacketPayload {

	public static final Type<RemoveSkippedPacket> TYPE = new Type<RemoveSkippedPacket>(Identifier.fromNamespaceAndPath(AdventurersCompass.MODID, "remove_skipped"));

	public static final StreamCodec<FriendlyByteBuf, RemoveSkippedPacket> CODEC = CompassCodecs.INSTANCE_KEY_STREAM.map(RemoveSkippedPacket::new, RemoveSkippedPacket::instance).cast();

	public static void handle(RemoveSkippedPacket packet, ServerPlayNetworking.Context context) {
		HeldCompass.run(context, (player, stack) -> ServerCompass.removeSkipped(player, stack, packet.instance()));
	}

	@Override
	public Type<RemoveSkippedPacket> type() {
		return TYPE;
	}
}
