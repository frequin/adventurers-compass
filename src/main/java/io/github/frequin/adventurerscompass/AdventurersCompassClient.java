package io.github.frequin.adventurerscompass;

import io.github.frequin.adventurerscompass.network.SyncPacket;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class AdventurersCompassClient implements ClientModInitializer {
	
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(SyncPacket.TYPE, SyncPacket::handle);
	}

}
