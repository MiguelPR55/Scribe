package com.scribe.client;

import com.scribe.data.CoordinateStorage;
import com.scribe.network.ScribeNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

@Environment(EnvType.CLIENT)
public class ScribeClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(
				ScribeNetworking.WaypointsSyncPayload.TYPE,
				(payload, context) -> context.client().execute(() -> CoordinateStorage.syncFromServer(payload.entries()))
		);

		ClientPlayConnectionEvents.DISCONNECT.register(
				(handler, client) -> CoordinateStorage.unloadClient()
		);
	}
}
