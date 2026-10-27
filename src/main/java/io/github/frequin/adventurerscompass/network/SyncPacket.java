package io.github.frequin.adventurerscompass.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SyncPacket(boolean canTeleport, int maxNextSearches, boolean infiniteXp, List<Identifier> allowedStructureIds, Map<Identifier, Integer> xpLevelsForAllowedStructures, ListMultimap<Identifier, Identifier> dimensionsForAllowedStructures, Map<Identifier, Identifier> structureIdsToGroupIds) implements CustomPacketPayload {

	public static final Type<SyncPacket> TYPE = new Type<SyncPacket>(Identifier.fromNamespaceAndPath(AdventurersCompass.MODID, "sync"));

	public static final StreamCodec<FriendlyByteBuf, SyncPacket> CODEC = StreamCodec.ofMember(SyncPacket::write, SyncPacket::read);

	public static SyncPacket read(FriendlyByteBuf buf) {
		final boolean canTeleport = buf.readBoolean();
		final int maxNextSearches = buf.readInt();
		final boolean infiniteXp = buf.readBoolean();
		final List<Identifier> allowedStructures = new ArrayList<Identifier>();
		final Map<Identifier, Integer> xpLevelsForAllowedStructures = new HashMap<Identifier, Integer>();
		final ListMultimap<Identifier, Identifier> dimensionsForAllowedStructures = ArrayListMultimap.create();
		final Map<Identifier, Identifier> structureIdsToGroupIds = new HashMap<Identifier, Identifier>();

		final int numStructures = buf.readInt();
		for (int i = 0; i < numStructures; i++) {
			final Identifier structureId = buf.readIdentifier();
			final int numDimensions = buf.readInt();
			final List<Identifier> dimensions = new ArrayList<Identifier>();
			for (int j = 0; j < numDimensions; j++) {
				dimensions.add(buf.readIdentifier());
			}
			final Identifier groupId = buf.readIdentifier();
			final int xpLevels = buf.readInt();
			if (structureId != null) {
				allowedStructures.add(structureId);
				dimensionsForAllowedStructures.putAll(structureId, dimensions);
				structureIdsToGroupIds.put(structureId, groupId);
				xpLevelsForAllowedStructures.put(structureId, xpLevels);
			}
		}

		return new SyncPacket(canTeleport, maxNextSearches, infiniteXp, allowedStructures, xpLevelsForAllowedStructures, dimensionsForAllowedStructures, structureIdsToGroupIds);
	}

	public void write(FriendlyByteBuf buf) {
		buf.writeBoolean(canTeleport);
		buf.writeInt(maxNextSearches);
		buf.writeBoolean(infiniteXp);
		buf.writeInt(allowedStructureIds.size());
		for (Identifier structureId : allowedStructureIds) {
			buf.writeIdentifier(structureId);
			List<Identifier> dimensions = dimensionsForAllowedStructures.get(structureId);
			buf.writeInt(dimensions.size());
			for (Identifier dimensionKey : dimensions) {
				buf.writeIdentifier(dimensionKey);
			}
			Identifier typeKey = structureIdsToGroupIds.get(structureId);
			buf.writeIdentifier(typeKey);
			int xpLevels = xpLevelsForAllowedStructures.get(structureId);
			buf.writeInt(xpLevels);
		}
	}

	public static void handle(SyncPacket packet, ClientPlayNetworking.Context context) {
		context.client().execute(() -> {
			AdventurersCompass.synced = true;
			AdventurersCompass.canTeleport = packet.canTeleport;
			AdventurersCompass.maxNextSearches = packet.maxNextSearches;
			AdventurersCompass.infiniteXp = packet.infiniteXp;
			AdventurersCompass.allowedStructures = packet.allowedStructureIds;
			AdventurersCompass.xpLevelsForAllowedStructures = packet.xpLevelsForAllowedStructures;
			AdventurersCompass.dimensionsForAllowedStructures = packet.dimensionsForAllowedStructures;
			AdventurersCompass.structureIdsToGroupIds = packet.structureIdsToGroupIds;
		});
	}

	@Override
	public Type<SyncPacket> type() {
		return TYPE;
	}

}
