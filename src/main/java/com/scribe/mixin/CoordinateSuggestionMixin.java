package com.scribe.mixin;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.scribe.data.CoordinateStorage;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

@Mixin({ BlockPosArgument.class, Vec3Argument.class })
public class CoordinateSuggestionMixin {

	@Inject(method = "listSuggestions", at = @At("HEAD"))
	private <S> void scribe$listSuggestions(CommandContext<S> context, SuggestionsBuilder builder, CallbackInfoReturnable<CompletableFuture<Suggestions>> cir) {
		String remaining = builder.getRemaining();
		if (!remaining.contains(" ") && !remaining.isEmpty()) {
			char first = remaining.charAt(0);
			if (!Character.isDigit(first) && first != '~' && first != '^' && first != '-' && first != '+' && first != '.') {
				String lowerRemaining = remaining.toLowerCase(Locale.ROOT);
				for (String name : CoordinateStorage.names()) {
					if (name.toLowerCase(Locale.ROOT).startsWith(lowerRemaining)) {
						builder.suggest(name);
					}
				}
			}
		} else if (remaining.isEmpty()) {
			for (String name : CoordinateStorage.names()) {
				builder.suggest(name);
			}
		}
	}
}
