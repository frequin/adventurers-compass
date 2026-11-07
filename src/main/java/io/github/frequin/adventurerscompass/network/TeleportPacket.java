package io.github.frequin.adventurerscompass.network;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.compass.ServerCompass;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;

public record TeleportPacket() implements CustomPacketPayload {
	
	public static final Type<TeleportPacket> TYPE = new Type<TeleportPacket>(Identifier.fromNamespaceAndPath(AdventurersCompass.MODID, "teleport"));
	
	public static final StreamCodec<FriendlyByteBuf, TeleportPacket> CODEC = StreamCodec.ofMember(TeleportPacket::write, TeleportPacket::read);

	public static TeleportPacket read(FriendlyByteBuf buf) {
		return new TeleportPacket();
	}

	public void write(FriendlyByteBuf buf) {
	}

	public static void handle(TeleportPacket packet, ServerPlayNetworking.Context context) {
		HeldCompass.run(context, ServerCompass::teleport);
	}

	/** Moves the player to the first free height at X/Z of the Current dimension. */
	public static void teleport(ServerPlayer player, int x, int z) {
		final int y = findValidTeleportHeight(player.level(), x, z);

		player.stopRiding();
		player.connection.teleport(x, y, z, player.getYRot(), player.getXRot());

		if (!player.isFallFlying()) {
			player.setDeltaMovement(player.getDeltaMovement().x(), 0, player.getDeltaMovement().z());
			player.setOnGround(true);
		}
	}

	@Override
	public Type<TeleportPacket> type() {
		return TYPE;
	}
	
	private static int findValidTeleportHeight(Level level, int x, int z) {
		int upY = level.getSeaLevel();
		int downY = level.getSeaLevel();
		while ((!level.isOutsideBuildHeight(upY) || !level.isOutsideBuildHeight(downY)) && !(isValidTeleportPosition(level, new BlockPos(x, upY, z)) || isValidTeleportPosition(level, new BlockPos(x, downY, z)))) {
			upY++;
			downY--;
		}
		BlockPos upPos = new BlockPos(x, upY, z);
		BlockPos downPos = new BlockPos(x, downY, z);
		if (isValidTeleportPosition(level, upPos)) {
			return upY;
		}
		if (isValidTeleportPosition(level, downPos)) {
			return downY;
		}
		return 256;
	}
	
	private static boolean isValidTeleportPosition(Level level, BlockPos pos) {
		return isFree(level, pos) && isFree(level, pos.above()) && !isFree(level, pos.below());
	}
	
	private static boolean isFree(Level level, BlockPos pos) {
		return level.getBlockState(pos).isAir() || level.getBlockState(pos).is(BlockTags.FIRE) || level.getBlockState(pos).liquid() || level.getBlockState(pos).canBeReplaced();
	}

}