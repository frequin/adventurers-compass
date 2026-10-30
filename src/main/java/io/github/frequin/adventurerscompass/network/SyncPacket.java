package io.github.frequin.adventurerscompass.network;

import java.util.HashMap;
import java.util.Map;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.codec.SelectionCodecs;
import io.github.frequin.adventurerscompass.domain.selection.Catalogue;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server to client, sent when the compass screen opens: the Current dimension,
 * the catalogue of listable Structures, and the config the screen needs.
 *
 * @param xpLevels XP levels charged per Structure id of the catalogue
 * @param dimensionDisabled the Current dimension is in {@code disabledDimensions} (the catalogue is then empty)
 */
public record SyncPacket(boolean canTeleport, boolean infiniteXp, String dimension, Catalogue catalogue, Map<String, Integer> xpLevels, boolean dimensionDisabled) implements CustomPacketPayload {

	public static final Type<SyncPacket> TYPE = new Type<SyncPacket>(Identifier.fromNamespaceAndPath(AdventurersCompass.MODID, "sync"));

	public static final StreamCodec<FriendlyByteBuf, SyncPacket> CODEC = StreamCodec.ofMember(SyncPacket::write, SyncPacket::read);

	private static final StreamCodec<ByteBuf, Map<String, Integer>> XP_LEVELS = ByteBufCodecs.<ByteBuf, String, Integer, Map<String, Integer>>map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT);

	public static SyncPacket read(FriendlyByteBuf buf) {
		final boolean canTeleport = buf.readBoolean();
		final boolean infiniteXp = buf.readBoolean();
		final String dimension = buf.readUtf();
		final Catalogue catalogue = SelectionCodecs.CATALOGUE_STREAM.decode(buf);
		final Map<String, Integer> xpLevels = XP_LEVELS.decode(buf);
		final boolean dimensionDisabled = buf.readBoolean();
		return new SyncPacket(canTeleport, infiniteXp, dimension, catalogue, xpLevels, dimensionDisabled);
	}

	public void write(FriendlyByteBuf buf) {
		buf.writeBoolean(canTeleport);
		buf.writeBoolean(infiniteXp);
		buf.writeUtf(dimension);
		SelectionCodecs.CATALOGUE_STREAM.encode(buf, catalogue);
		XP_LEVELS.encode(buf, xpLevels);
		buf.writeBoolean(dimensionDisabled);
	}

	public static void handle(SyncPacket packet, ClientPlayNetworking.Context context) {
		context.client().execute(() -> {
			AdventurersCompass.synced = true;
			AdventurersCompass.canTeleport = packet.canTeleport;
			AdventurersCompass.infiniteXp = packet.infiniteXp;
			AdventurersCompass.currentDimension = packet.dimension;
			AdventurersCompass.catalogue = packet.catalogue;
			AdventurersCompass.xpLevels = packet.xpLevels;
			AdventurersCompass.dimensionDisabled = packet.dimensionDisabled;
		});
	}

	@Override
	public Type<SyncPacket> type() {
		return TYPE;
	}

}
