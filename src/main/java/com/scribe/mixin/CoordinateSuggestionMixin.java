package com.scribe.mixin;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.scribe.data.CoordinateStorage;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.commands.arguments.coordinates.Vec2Argument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

@Mixin({ BlockPosArgument.class, Vec3Argument.class, ColumnPosArgument.class, Vec2Argument.class })
public class CoordinateSuggestionMixin {

	@Inject(method = "listSuggestions", at = @At("HEAD"), cancellable = true)
	private <S> void scribe$listSuggestions(CommandContext<S> context, SuggestionsBuilder builder, CallbackInfoReturnable<CompletableFuture<Suggestions>> cir) {
		String remaining = builder.getRemaining();
		if (remaining.isEmpty()) {
			for (String name : CoordinateStorage.names()) {
				builder.suggest(name);
			}
		} else if (remaining.indexOf(' ') == -1) {
			char first = remaining.charAt(0);
			if (!Character.isDigit(first) && first != '~' && first != '^' && first != '-' && first != '+' && first != '.') {
				String lowerRemaining = remaining.toLowerCase(Locale.ROOT);
				for (String name : CoordinateStorage.names()) {
					if (SharedSuggestionProvider.matchesSubStr(lowerRemaining, name.toLowerCase(Locale.ROOT))) {
						builder.suggest(name);
					}
				}
				// Short-circuit vanilla coordinate suggestion validation when typing a waypoint name,
				// avoiding futile StringReader exception throwing on non-numeric prefixes.
				cir.setReturnValue(builder.buildFuture());
			}
		}
	}
}
