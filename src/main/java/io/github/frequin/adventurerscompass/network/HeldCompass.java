package io.github.frequin.adventurerscompass.network;

import java.util.function.BiConsumer;

import io.github.frequin.adventurerscompass.AdventurersCompass;
import io.github.frequin.adventurerscompass.util.ItemUtils;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** The shared body of the client-to-server compass commands. */
final class HeldCompass {

	private HeldCompass() {
	}

	/** Runs {@code command} on the server thread with the compass the player holds; nothing when they hold none. */
	static void run(ServerPlayNetworking.Context context, BiConsumer<ServerPlayer, ItemStack> command) {
		context.server().execute(() -> {
			final ItemStack stack = ItemUtils.getHeldItem(context.player(), AdventurersCompass.ADVENTURERS_COMPASS_ITEM);
			if (!stack.isEmpty()) {
				command.accept(context.player(), stack);
			}
		});
	}

}
