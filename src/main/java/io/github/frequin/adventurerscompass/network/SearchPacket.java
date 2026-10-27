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

public record SearchPacket(Identifier structureOrGroupId, boolean isGroup) implements CustomPacketPayload {
	
	public static final Type<SearchPacket> TYPE = new Type<SearchPacket>(Identifier.fromNamespaceAndPath(AdventurersCompass.MODID, "search"));
	
	public static final StreamCodec<FriendlyByteBuf, SearchPacket> CODEC = StreamCodec.ofMember(SearchPacket::write, SearchPacket::read);

	public static SearchPacket read(FriendlyByteBuf buf) {
		final Identifier structureOrGroupId = buf.readIdentifier();
		final boolean isGroup = buf.readBoolean();
		return new SearchPacket(structureOrGroupId, isGroup);
	}

	public void write(FriendlyByteBuf buf) {
		buf.writeIdentifier(structureOrGroupId);
		buf.writeBoolean(isGroup);
	}

	public static void handle(SearchPacket packet, ServerPlayNetworking.Context context) {
		context.server().execute(() -> {
			final ItemStack stack = ItemUtils.getHeldItem(context.player(), AdventurersCompass.ADVENTURERS_COMPASS_ITEM);
			if (!stack.isEmpty()) {
				final AdventurersCompassItem adventurersCompass = (AdventurersCompassItem) stack.getItem();
				adventurersCompass.searchForStructure((ServerLevel) context.player().level(), context.player(), context.player().blockPosition(), packet.structureOrGroupId, packet.isGroup, stack);
			}
		});
	}
	
	@Override
	public Type<SearchPacket> type() {
		return TYPE;
	}
}