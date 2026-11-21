package com.scribe.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.scribe.data.CoordinateEntry;
import com.scribe.data.CoordinateStorage;
import com.scribe.network.ScribeNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * All of Scribe's chat-command logic.
 */
public final class ScribeCommand {

	private static final Logger LOGGER = LoggerFactory.getLogger("scribe");
	private static final int ITEMS_PER_PAGE = 8;

	private static final SuggestionProvider<CommandSourceStack> SUGGEST_NAMES =
			(ctx, builder) -> SharedSuggestionProvider.suggest(CoordinateStorage.names(), builder);

	private ScribeCommand() {
	}

	public static void register(
			CommandDispatcher<CommandSourceStack> dispatcher,
			CommandBuildContext registryAccess
	) {
		LiteralArgumentBuilder<CommandSourceStack> main = buildTree("coords");
		var mainNode = dispatcher.register(main);

		// "coordinates" and "scribe" are aliases redirecting to "coords".
		// .executes() is required on the alias literal so executing just "/coordinates"
		// or "/scribe" triggers the help handler rather than failing syntax parsing.
		dispatcher.register(Commands.literal("coordinates")
				.executes(ScribeCommand::executeHelp)
				.redirect(mainNode));
		dispatcher.register(Commands.literal("scribe")
				.executes(ScribeCommand::executeHelp)
				.redirect(mainNode));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildTree(String name) {
		return Commands.literal(name)
				// /coords            -> same as /coords help
				.executes(ScribeCommand::executeHelp)

				// /coords help
				.then(Commands.literal("help")
						.executes(ScribeCommand::executeHelp))

				// /coords list
				// /coords list <page>
				.then(Commands.literal("list")
						.executes(ctx -> executeList(ctx, 1))
						.then(Commands.argument("page", IntegerArgumentType.integer(1))
								.executes(ctx -> executeList(ctx, IntegerArgumentType.getInteger(ctx, "page")))))

				// /coords search <query>
				.then(Commands.literal("search")
						.then(Commands.argument("query", StringArgumentType.word())
								.suggests(SUGGEST_NAMES)
								.executes(ScribeCommand::executeSearch)))

				// /coords near
				.then(Commands.literal("near")
						.executes(ScribeCommand::executeNear))

				// /coords add <name>
				// /coords add <name> <coordinates>
				// /coords add <name> <coordinates> <dimension>
				.then(Commands.literal("add")
						.then(Commands.argument("name", StringArgumentType.word())
								.executes(ctx -> executeSave(ctx, false, false, false))
								.then(Commands.argument("coordinates", BlockPosArgument.blockPos())
										.executes(ctx -> executeSave(ctx, false, true, false))
										.then(Commands.argument("dimension", DimensionArgument.dimension())
												.executes(ctx -> executeSave(ctx, false, true, true))))))

				// /coords replace <name>
				// /coords replace <name> <coordinates>
				// /coords replace <name> <coordinates> <dimension>
				.then(Commands.literal("replace")
						.then(Commands.argument("name", StringArgumentType.word())
								.suggests(SUGGEST_NAMES)
								.executes(ctx -> executeSave(ctx, true, false, false))
								.then(Commands.argument("coordinates", BlockPosArgument.blockPos())
										.executes(ctx -> executeSave(ctx, true, true, false))
										.then(Commands.argument("dimension", DimensionArgument.dimension())
												.executes(ctx -> executeSave(ctx, true, true, true))))))

				// /coords rename <old_name> <new_name>
				.then(Commands.literal("rename")
						.then(Commands.argument("old_name", StringArgumentType.word())
								.suggests(SUGGEST_NAMES)
								.then(Commands.argument("new_name", StringArgumentType.word())
										.executes(ScribeCommand::executeRename))))

				// /coords recall <name>
				.then(Commands.literal("recall")
						.then(Commands.argument("name", StringArgumentType.word())
								.suggests(SUGGEST_NAMES)
								.executes(ScribeCommand::executeRecall)))

				// /coords remove <name>
				.then(Commands.literal("remove")
						.then(Commands.argument("name", StringArgumentType.word())
								.suggests(SUGGEST_NAMES)
								.executes(ScribeCommand::executeRemove)));
	}

	// ------------------------------------------------------------------
	// add & replace
	// ------------------------------------------------------------------

	public static boolean isValidWaypointName(String name) {
		if (name == null || name.isEmpty()) {
			return false;
		}
		char first = name.charAt(0);
		return !Character.isDigit(first) && first != '~' && first != '^' && first != '-' && first != '+' && first != '.';
	}

	private static int executeSave(
			CommandContext<CommandSourceStack> context,
			boolean allowOverwrite,
			boolean explicitCoordinates,
			boolean explicitDimension
	) throws CommandSyntaxException {
		BlockPos pos;
		String dimension;

		if (explicitCoordinates) {
			// getBlockPos does not force chunk loading, allowing waypoints
			// to be saved for unloaded regions and from console/datapacks.
			pos = BlockPosArgument.getBlockPos(context, "coordinates");
			if (explicitDimension) {
				dimension = DimensionArgument.getDimension(context, "dimension").dimension().identifier().toString();
			} else {
				dimension = context.getSource().getLevel().dimension().identifier().toString();
			}
		} else {
			ServerPlayer player = context.getSource().getPlayerOrException();
			pos = player.blockPosition();
			dimension = player.level().dimension().identifier().toString();
		}

		return saveEntry(context, pos, dimension, allowOverwrite);
	}

	private static int saveEntry(
			CommandContext<CommandSourceStack> context,
			BlockPos pos,
			String dimension,
			boolean allowOverwrite
	) {
		String name = StringArgumentType.getString(context, "name");

		if (!isValidWaypointName(name)) {
			context.getSource().sendFailure(
					Component.literal("Waypoint name cannot start with a number or special symbol (~, ^, -, +, .).")
			);
			return 0;
		}

		if (!allowOverwrite && CoordinateStorage.exists(name)) {
			String replaceCmd = "/coords replace " + name + " " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " " + dimension;
			Style suggestStyle = Style.EMPTY
					.withColor(ChatFormatting.YELLOW)
					.withClickEvent(new ClickEvent.SuggestCommand(replaceCmd))
					.withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to insert command in chat")))
					.withUnderlined(true);

			Component errorMessage = Component.literal("Waypoint '")
					.append(Component.literal(name).withStyle(ChatFormatting.AQUA))
					.append(Component.literal("' already exists! Click "))
					.append(Component.literal(replaceCmd).withStyle(suggestStyle))
					.append(Component.literal(" to overwrite it."));

			context.getSource().sendFailure(errorMessage);
			return 0;
		}

		CoordinateStorage.put(new CoordinateEntry(name, pos, dimension));
		LOGGER.info("[Scribe] Source '{}' {} waypoint '{}' at [{}, {}, {}] ({})",
				context.getSource().getTextName(),
				allowOverwrite ? "replaced" : "added",
				name, pos.getX(), pos.getY(), pos.getZ(), dimension);
		ScribeNetworking.broadcastSync(context.getSource().getServer());

		Component message = Component.literal("Saved waypoint '")
				.append(Component.literal(name).withStyle(ChatFormatting.AQUA))
				.append(Component.literal("' at "))
				.append(formatClickableCoords(pos))
				.append(formatDimension(dimension));

		context.getSource().sendSuccess(() -> message, false);
		return 1;
	}

	// ------------------------------------------------------------------
	// rename
	// ------------------------------------------------------------------

	private static int executeRename(CommandContext<CommandSourceStack> context) {
		String oldName = StringArgumentType.getString(context, "old_name");
		String newName = StringArgumentType.getString(context, "new_name");

		if (!isValidWaypointName(newName)) {
			context.getSource().sendFailure(
					Component.literal("New waypoint name cannot start with a number or special symbol (~, ^, -, +, .).")
			);
			return 0;
		}

		Optional<CoordinateEntry> existingOpt = CoordinateStorage.get(oldName);
		if (existingOpt.isEmpty()) {
			context.getSource().sendFailure(
					Component.literal("No waypoint named '" + oldName + "' was found.")
			);
			return 0;
		}

		if (!oldName.equalsIgnoreCase(newName) && CoordinateStorage.exists(newName)) {
			context.getSource().sendFailure(
					Component.literal("A waypoint named '" + newName + "' already exists.")
			);
			return 0;
		}

		CoordinateEntry oldEntry = existingOpt.get();
		CoordinateStorage.rename(oldName, newName);

		LOGGER.info("[Scribe] Source '{}' renamed waypoint '{}' to '{}'",
				context.getSource().getTextName(), oldEntry.name(), newName);
		ScribeNetworking.broadcastSync(context.getSource().getServer());

		Component message = Component.literal("Renamed waypoint '")
				.append(Component.literal(oldEntry.name()).withStyle(ChatFormatting.AQUA))
				.append(Component.literal("' to '"))
				.append(Component.literal(newName).withStyle(ChatFormatting.AQUA))
				.append(Component.literal("'."));

		context.getSource().sendSuccess(() -> message, false);
		return 1;
	}

	// ------------------------------------------------------------------
	// search & near
	// ------------------------------------------------------------------

	private static int executeSearch(CommandContext<CommandSourceStack> context) {
		String query = StringArgumentType.getString(context, "query");
		String lowerQuery = query.toLowerCase(Locale.ROOT);

		List<CoordinateEntry> matches = CoordinateStorage.all().stream()
				.filter(e -> e.name().toLowerCase(Locale.ROOT).contains(lowerQuery)
						|| e.dimension().toLowerCase(Locale.ROOT).contains(lowerQuery))
				.toList();

		if (matches.isEmpty()) {
			context.getSource().sendFailure(
					Component.literal("No waypoints matching '" + query + "' were found.")
			);
			return 0;
		}

		var message = Component.literal("--- Scribe Search: '" + query + "' (" + matches.size() + " found) ---\n")
				.withStyle(ChatFormatting.GOLD);

		for (int i = 0; i < matches.size(); i++) {
			message.append(Component.literal("• ").withStyle(ChatFormatting.WHITE))
					.append(formatEntry(context.getSource(), matches.get(i)));
			if (i < matches.size() - 1) {
				message.append(Component.literal("\n"));
			}
		}

		context.getSource().sendSuccess(() -> message, false);
		return 1;
	}

	private static int executeNear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		String currentDim = player.level().dimension().identifier().toString();
		BlockPos playerPos = player.blockPosition();

		CoordinateEntry nearest = null;
		double bestDistSqr = Double.MAX_VALUE;

		for (CoordinateEntry entry : CoordinateStorage.all()) {
			if (currentDim.equals(entry.dimension())) {
				double distSqr = playerPos.distSqr(entry.pos());
				if (distSqr < bestDistSqr) {
					bestDistSqr = distSqr;
					nearest = entry;
				}
			}
		}

		if (nearest == null) {
			context.getSource().sendFailure(
					Component.literal("No waypoints saved in your current dimension (" + currentDim + ").")
			);
			return 0;
		}

		CoordinateEntry finalNearest = nearest;
		Component message = Component.literal("Nearest waypoint: ").withStyle(ChatFormatting.GOLD)
				.append(formatEntry(context.getSource(), finalNearest));
		context.getSource().sendSuccess(() -> message, false);
		return 1;
	}

	// ------------------------------------------------------------------
	// formatting helpers
	// ------------------------------------------------------------------

	private static Component formatClickableCoords(BlockPos pos) {
		String coordsText = pos.getX() + " " + pos.getY() + " " + pos.getZ();
		Style clickableStyle = Style.EMPTY
				.withColor(ChatFormatting.GREEN)
				.withClickEvent(new ClickEvent.CopyToClipboard(coordsText))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy coordinates")))
				.withUnderlined(true);
		return Component.literal(coordsText).withStyle(clickableStyle);
	}

	private static Component formatDimension(String dimension) {
		ChatFormatting color = switch (dimension) {
			case "minecraft:overworld" -> ChatFormatting.DARK_GREEN;
			case "minecraft:the_nether" -> ChatFormatting.RED;
			case "minecraft:the_end" -> ChatFormatting.DARK_PURPLE;
			default -> ChatFormatting.GRAY;
		};
		return Component.literal(" (" + dimension + ")").withStyle(color);
	}

	private static Component formatDistance(CommandSourceStack source, CoordinateEntry entry) {
		if (!(source.getEntity() instanceof ServerPlayer player)) {
			// Executed from console, a command block, etc. — no reference position,
			// so we do not show distance.
			return Component.empty();
		}

		String currentDimension = player.level().dimension().identifier().toString();
		if (!currentDimension.equals(entry.dimension())) {
			return Component.empty();
		}

		double distance = Math.sqrt(player.blockPosition().distSqr(entry.pos()));
		return Component.literal(" (" + Math.round(distance) + "m)").withStyle(ChatFormatting.GRAY);
	}

	private static Component formatEntry(CommandSourceStack source, CoordinateEntry entry) {
		String tpCmd = "/execute in " + entry.dimension() + " run tp @s "
				+ entry.pos().getX() + " " + entry.pos().getY() + " " + entry.pos().getZ();
		Style nameStyle = Style.EMPTY
				.withColor(ChatFormatting.AQUA)
				.withClickEvent(new ClickEvent.SuggestCommand(tpCmd))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to prepare teleport command")));

		return Component.literal(entry.name()).withStyle(nameStyle)
				.append(Component.literal(": ").withStyle(ChatFormatting.AQUA))
				.append(formatClickableCoords(entry.pos()))
				.append(formatDimension(entry.dimension()))
				.append(formatDistance(source, entry));
	}

	// ------------------------------------------------------------------
	// recall
	// ------------------------------------------------------------------

	private static int executeRecall(CommandContext<CommandSourceStack> context) {
		String name = StringArgumentType.getString(context, "name");
		Optional<CoordinateEntry> found = CoordinateStorage.get(name);

		if (found.isEmpty()) {
			context.getSource().sendFailure(
					Component.literal("No waypoint named '" + name + "' was found. Use Tab to see saved names.")
			);
			return 0;
		}

		Component message = formatEntry(context.getSource(), found.get());
		context.getSource().sendSuccess(() -> message, false);
		return 1;
	}

	// ------------------------------------------------------------------
	// remove
	// ------------------------------------------------------------------

	private static int executeRemove(CommandContext<CommandSourceStack> context) {
		String name = StringArgumentType.getString(context, "name");
		boolean removed = CoordinateStorage.remove(name);

		if (removed) {
			LOGGER.info("[Scribe] Source '{}' removed waypoint '{}'", context.getSource().getTextName(), name);
			ScribeNetworking.broadcastSync(context.getSource().getServer());
			context.getSource().sendSuccess(
					() -> Component.literal("Removed waypoint '")
							.append(Component.literal(name).withStyle(ChatFormatting.AQUA))
							.append(Component.literal("'.")), false);
			return 1;
		}

		context.getSource().sendFailure(
				Component.literal("No waypoint named '" + name + "' was found."));
		return 0;
	}

	// ------------------------------------------------------------------
	// list
	// ------------------------------------------------------------------

	private static int executeList(CommandContext<CommandSourceStack> context, int page) {
		if (CoordinateStorage.isEmpty()) {
			context.getSource().sendSuccess(
					() -> Component.literal("No waypoints saved yet. Try /coords add <name>."), false);
			return 1;
		}

		List<CoordinateEntry> entries = CoordinateStorage.all();
		int totalItems = entries.size();
		int totalPages = Math.max(1, Math.ceilDiv(totalItems, ITEMS_PER_PAGE));

		if (page > totalPages || page < 1) {
			context.getSource().sendFailure(
					Component.literal("Invalid page number. Page must be between 1 and " + totalPages + ".")
			);
			return 0;
		}

		int startIndex = (page - 1) * ITEMS_PER_PAGE;
		int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, totalItems);

		var message = Component.literal("--- Scribe Waypoints (Page " + page + "/" + totalPages + ") ---\n")
				.withStyle(ChatFormatting.GOLD);

		for (int i = startIndex; i < endIndex; i++) {
			message.append(Component.literal("• ").withStyle(ChatFormatting.WHITE))
					.append(formatEntry(context.getSource(), entries.get(i)));
			if (i < endIndex - 1 || totalPages > 1) {
				message.append(Component.literal("\n"));
			}
		}

		if (totalPages > 1) {
			var navBar = Component.literal("--- ").withStyle(ChatFormatting.GRAY);
			if (page > 1) {
				navBar.append(Component.literal("[< Prev] ")
						.withStyle(Style.EMPTY
								.withColor(ChatFormatting.YELLOW)
								.withClickEvent(new ClickEvent.RunCommand("/coords list " + (page - 1)))
								.withHoverEvent(new HoverEvent.ShowText(Component.literal("Go to page " + (page - 1))))
								.withUnderlined(true)));
			}
			navBar.append(Component.literal("Page " + page + " of " + totalPages).withStyle(ChatFormatting.WHITE));
			if (page < totalPages) {
				navBar.append(Component.literal(" [Next >]")
						.withStyle(Style.EMPTY
								.withColor(ChatFormatting.YELLOW)
								.withClickEvent(new ClickEvent.RunCommand("/coords list " + (page + 1)))
								.withHoverEvent(new HoverEvent.ShowText(Component.literal("Go to page " + (page + 1))))
								.withUnderlined(true)));
			}
			navBar.append(Component.literal(" ---").withStyle(ChatFormatting.GRAY));
			message.append(navBar);
		}

		context.getSource().sendSuccess(() -> message, false);
		return 1;
	}

	// ------------------------------------------------------------------
	// help
	// ------------------------------------------------------------------

	private static int executeHelp(CommandContext<CommandSourceStack> context) {
		context.getSource().sendSuccess(() -> Component.literal(
				"""
				Scribe — save and recall coordinates (aliases: /coords, /coordinates, /scribe)
				  add <name> [<x> <y> <z> [<dim>]]     - save a waypoint (uses your position if omitted)
				  replace <name> [<x> <y> <z> [<dim>]] - overwrite an existing waypoint (or create if new)
				  rename <old_name> <new_name>         - rename an existing waypoint
				  recall <name>                        - show a saved waypoint (click coords to copy)
				  remove <name>                        - delete a saved waypoint
				  near                                 - find the closest waypoint in your dimension
				  search <query>                       - search waypoints by name or dimension
				  list [<page>]                        - show saved waypoints with pagination"""
		), false);
		return 1;
	}
}
