package io.github.frequin.adventurerscompass.network;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.compass.ServerCompass;
import io.github.frequin.adventurerscompass.util.ItemUtils;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Client to server: "Pass" with the held compass: the Target becomes Skipped and the compass searches again. */
public record PassPacket() implements CustomPacketPayload {

	public static final Type<PassPacket> TYPE = new Type<PassPacket>(Identifier.fromNamespaceAndPath(AdventurersCompass.MODID, "pass"));

	public static final StreamCodec<FriendlyByteBuf, PassPacket> CODEC = StreamCodec.unit(new PassPacket());

	public static void handle(PassPacket packet, ServerPlayNetworking.Context context) {
		context.server().execute(() -> {
			final ItemStack stack = ItemUtils.getHeldItem(context.player(), AdventurersCompass.ADVENTURERS_COMPASS_ITEM);
			if (!stack.isEmpty()) {
				ServerCompass.pass(context.player(), stack);
			}
		});
	}

	@Override
	public Type<PassPacket> type() {
		return TYPE;
	}
}
