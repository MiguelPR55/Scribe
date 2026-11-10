package com.scribe;

import com.scribe.command.ScribeCommand;
import com.scribe.data.CoordinateStorage;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public final class Scribe implements ModInitializer {

	public static final String MOD_ID = "scribe";

	@Override
	public void onInitialize() {
		// Register /coords, /coordinates and /scribe as equivalent aliases.
		CommandRegistrationCallback.EVENT.register(
				(dispatcher, registryAccess, environment) -> ScribeCommand.register(dispatcher, registryAccess)
		);

		// Load this world's saved_coords.txt as soon as the server (integrated
		// or dedicated) starts initializing, so the in-memory map is ready
		// before datapacks, functions, or commands can run.
		ServerLifecycleEvents.SERVER_STARTING.register(CoordinateStorage::load);

		// Unload and clear data when the server stops,
		// preventing state from leaking across different worlds in singleplayer.
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> CoordinateStorage.unload());
	}
}
