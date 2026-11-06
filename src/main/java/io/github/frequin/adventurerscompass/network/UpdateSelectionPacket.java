package io.github.frequin.adventurerscompass.network;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.codec.SelectionCodecs;
import io.github.frequin.adventurerscompass.compass.ServerCompass;
import io.github.frequin.adventurerscompass.domain.selection.Selection;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server: the player edited the Selection of the compass they hold, which cancels its search, keeps its Target and clears its Skipped. */
public record UpdateSelectionPacket(Selection selection) implements CustomPacketPayload {

	public static final Type<UpdateSelectionPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AdventurersCompass.MODID, "update_selection"));

	public static final StreamCodec<FriendlyByteBuf, UpdateSelectionPacket> CODEC = SelectionCodecs.SELECTION_STREAM.map(UpdateSelectionPacket::new, UpdateSelectionPacket::selection).cast();

	public static void handle(UpdateSelectionPacket packet, ServerPlayNetworking.Context context) {
		HeldCompass.run(context, (player, stack) -> ServerCompass.selectionChanged(player, stack, packet.selection));
	}

	@Override
	public Type<UpdateSelectionPacket> type() {
		return TYPE;
	}

}
