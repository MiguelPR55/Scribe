package com.scribe.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.NoticeWithLinkScreen;
import net.minecraft.network.chat.Component;
import java.net.URI;

public class ScribeModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> new NoticeWithLinkScreen(
				Component.literal("Scribe - Quick Guide"),
				Component.literal("Scribe is a lightweight command-based waypoint utility.\n\n" +
						"Available Commands:\n" +
						"• /coords add <name> [x y z] - Save a waypoint\n" +
						"• /coords replace <name> [x y z] - Overwrite a waypoint\n" +
						"• /coords recall <name> - Show a saved waypoint (click to copy coords)\n" +
						"• /coords remove <name> - Delete a waypoint\n" +
						"• /coords list - View all saved waypoints\n\n" +
						"Data is stored in your world's data/scribe/saved_coords.txt file."),
				URI.create("https://github.com/MiguelPR55/Scribe"),
				() -> Minecraft.getInstance().setScreenAndShow(parent)
		);
	}
}
