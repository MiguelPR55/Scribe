package com.scribe.mixin;

import com.mojang.brigadier.StringReader;
import com.scribe.data.CoordinateEntry;
import com.scribe.data.CoordinateStorage;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ColumnPosArgument.class)
public class ColumnPosArgumentMixin {

	@Inject(method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/commands/arguments/coordinates/Coordinates;", at = @At("HEAD"), cancellable = true)
	private void scribe$parseColumnPos(StringReader reader, CallbackInfoReturnable<Coordinates> cir) {
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
			cir.setReturnValue(WorldCoordinates.absolute(pos.getX(), pos.getY(), pos.getZ()));
		} else {
			reader.setCursor(startCursor);
		}
	}
}
