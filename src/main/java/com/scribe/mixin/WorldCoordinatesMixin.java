package com.scribe.mixin;

import com.mojang.brigadier.StringReader;
import com.scribe.data.CoordinateEntry;
import com.scribe.data.CoordinateStorage;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(WorldCoordinates.class)
public class WorldCoordinatesMixin {

	@Inject(method = "parseInt", at = @At("HEAD"), cancellable = true)
	private static void scribe$parseInt(StringReader reader, CallbackInfoReturnable<WorldCoordinates> cir) {
		WorldCoordinates coords = scribe$tryParseWaypoint(reader, false);
		if (coords != null) {
			cir.setReturnValue(coords);
		}
	}

	@Inject(method = "parseDouble", at = @At("HEAD"), cancellable = true)
	private static void scribe$parseDouble(StringReader reader, boolean center, CallbackInfoReturnable<WorldCoordinates> cir) {
		WorldCoordinates coords = scribe$tryParseWaypoint(reader, center);
		if (coords != null) {
			cir.setReturnValue(coords);
		}
	}

	private static WorldCoordinates scribe$tryParseWaypoint(StringReader reader, boolean center) {
		if (!reader.canRead()) return null;
		int startCursor = reader.getCursor();

		char firstChar = reader.peek();
		if (Character.isDigit(firstChar) || firstChar == '~' || firstChar == '^' || firstChar == '-' || firstChar == '+' || firstChar == '.' || Character.isWhitespace(firstChar)) {
			return null;
		}

		String word = reader.readUnquotedString();
		Optional<CoordinateEntry> entry = CoordinateStorage.get(word);
		if (entry.isPresent()) {
			BlockPos pos = entry.get().pos();
			double x = center ? pos.getX() + 0.5 : pos.getX();
			double y = pos.getY();
			double z = center ? pos.getZ() + 0.5 : pos.getZ();
			return WorldCoordinates.absolute(x, y, z);
		} else {
			reader.setCursor(startCursor);
			return null;
		}
	}
}
