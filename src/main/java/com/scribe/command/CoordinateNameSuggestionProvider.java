package com.scribe.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.scribe.data.CoordinateStorage;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;

import java.util.concurrent.CompletableFuture;

/**
 * Offers every currently-saved waypoint name as a tab-completion candidate.
 * Used on the {@code <name>} argument of {@code /coords recall} and
 * {@code /coords remove}, exactly like vanilla suggests known values (e.g.
 * item ids on {@code /give}).
 */
public final class CoordinateNameSuggestionProvider implements SuggestionProvider<CommandSourceStack> {

	public static final CoordinateNameSuggestionProvider INSTANCE = new CoordinateNameSuggestionProvider();

	private CoordinateNameSuggestionProvider() {
	}

	@Override
	public CompletableFuture<Suggestions> getSuggestions(
			CommandContext<CommandSourceStack> context,
			SuggestionsBuilder builder
	) {
		return SharedSuggestionProvider.suggest(CoordinateStorage.names(), builder);
	}
}
