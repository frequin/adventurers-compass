package io.github.frequin.adventurerscompass.network;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.config.AdventurersCompassConfig;
import io.github.frequin.adventurerscompass.domain.compass.Target;
import io.github.frequin.adventurerscompass.util.ItemUtils;
import io.github.frequin.adventurerscompass.util.PlayerUtils;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
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
		context.server().execute(() -> {
			final ItemStack stack = ItemUtils.getHeldItem(context.player(), AdventurersCompass.ADVENTURERS_COMPASS_ITEM);
			if (!stack.isEmpty()) {
				final ServerPlayer player = (ServerPlayer) context.player();
				if (AdventurersCompassConfig.allowTeleport && PlayerUtils.canTeleport(player.level().getServer(), player)) {
					final Target target = stack.get(AdventurersCompass.TARGET);
					// Never across dimensions
					if (target != null && target.dimension().equals(player.level().dimension().identifier().toString())) {
						final int x = target.pos().x();
						final int z = target.pos().z();
						final int y = packet.findValidTeleportHeight(player.level(), x, z);

						player.stopRiding();
						player.connection.teleport(x, y, z, player.getYRot(), player.getXRot());

						if (!player.isFallFlying()) {
							player.setDeltaMovement(player.getDeltaMovement().x(), 0, player.getDeltaMovement().z());
							player.setOnGround(true);
						}
					}
				} else {
					AdventurersCompass.LOGGER.warn("Player " + player.getDisplayName().getString() + " tried to teleport but does not have permission.");
				}
			}
		});
	}
	
	@Override
	public Type<TeleportPacket> type() {
		return TYPE;
	}
	
	private int findValidTeleportHeight(Level level, int x, int z) {
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
	
	private boolean isValidTeleportPosition(Level level, BlockPos pos) {
		return isFree(level, pos) && isFree(level, pos.above()) && !isFree(level, pos.below());
	}
	
	private boolean isFree(Level level, BlockPos pos) {
		return level.getBlockState(pos).isAir() || level.getBlockState(pos).is(BlockTags.FIRE) || level.getBlockState(pos).liquid() || level.getBlockState(pos).canBeReplaced();
	}

}