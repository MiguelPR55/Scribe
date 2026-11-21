package com.scribe.mixin;

import com.mojang.brigadier.StringReader;
import com.scribe.data.CoordinateEntry;
import com.scribe.data.CoordinateStorage;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec2Argument;
import net.minecraft.commands.arguments.coordinates.WorldCoordinate;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Vec2Argument.class)
public class Vec2ArgumentMixin {

	@Shadow
	@Final
	private boolean centerCorrect;

	@Inject(method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/commands/arguments/coordinates/Coordinates;", at = @At("HEAD"), cancellable = true)
	private void scribe$parseVec2(StringReader reader, CallbackInfoReturnable<Coordinates> cir) {
		if (!reader.canRead()) {
			return;
		}
		int startCursor = reader.getCursor();
		char firstChar = reader.peek();
		if (Character.isDigit(firstChar) || firstChar == '~' || firstChar == '^' || firstChar == '-' || firstChar == '+' || firstChar == '.' || Character.isWhitespace(firstChar)) {
			return;
		}

		String word = reader.readUnquotedString();
		CoordinateEntry entry = CoordinateStorage.get(word).orElse(null);
		if (entry != null) {
			BlockPos pos = entry.pos();
			double x = this.centerCorrect ? pos.getX() + 0.5 : pos.getX();
			double z = this.centerCorrect ? pos.getZ() + 0.5 : pos.getZ();
			cir.setReturnValue(new WorldCoordinates(
					new WorldCoordinate(false, x),
					new WorldCoordinate(true, 0.0),
					new WorldCoordinate(false, z)
			));
		} else {
			reader.setCursor(startCursor);
		}
	}
}
