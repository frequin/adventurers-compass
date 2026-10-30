package io.github.frequin.adventurerscompass.network;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.codec.SelectionCodecs;
import io.github.frequin.adventurerscompass.compass.ServerCompass;
import io.github.frequin.adventurerscompass.domain.selection.Selection;
import io.github.frequin.adventurerscompass.util.ItemUtils;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Client to server: the player edited the Selection of the compass they hold, which cancels its search and clears its Target and Skipped. */
public record UpdateSelectionPacket(Selection selection) implements CustomPacketPayload {

	public static final Type<UpdateSelectionPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AdventurersCompass.MODID, "update_selection"));

	public static final StreamCodec<FriendlyByteBuf, UpdateSelectionPacket> CODEC = SelectionCodecs.SELECTION_STREAM.map(UpdateSelectionPacket::new, UpdateSelectionPacket::selection).cast();

	public static void handle(UpdateSelectionPacket packet, ServerPlayNetworking.Context context) {
		context.server().execute(() -> {
			final ItemStack stack = ItemUtils.getHeldItem(context.player(), AdventurersCompass.ADVENTURERS_COMPASS_ITEM);
			if (!stack.isEmpty()) {
				ServerCompass.selectionChanged(context.player(), stack, packet.selection);
			}
		});
	}

	@Override
	public Type<UpdateSelectionPacket> type() {
		return TYPE;
	}

}
