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
						"• /coords add <name> - Save current coordinates\n" +
						"• /coords list - View waypoints (click to share in chat)\n" +
						"• /coords remove <name> - Delete a waypoint\n" +
						"• /coords replace <name> [x y z] - Update a waypoint\n\n" +
						"Data is stored in your world's data/scribe/saved_coords.txt file."),
				URI.create("https://github.com/MiguelPR55/Scribe"),
				() -> Minecraft.getInstance().setScreenAndShow(parent)
		);
	}
}
