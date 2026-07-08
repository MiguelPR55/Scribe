package com.scribe.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.scribe.data.CoordinateEntry;
import com.scribe.data.CoordinateStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * All of Scribe's chat-command logic.
 */
public final class ScribeCommand {

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

		// "coordinates" and "scribe" are simple aliases that redirect to the tree
		// already registered under "coords", instead of rebuilding the entire tree
		// 3 times separately.
		dispatcher.register(Commands.literal("coordinates").redirect(mainNode));
		dispatcher.register(Commands.literal("scribe").redirect(mainNode));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildTree(String name) {
		return Commands.literal(name)
				// /coords            -> same as /coords help
				.executes(ScribeCommand::executeHelp)

				// /coords help
				.then(Commands.literal("help")
						.executes(ScribeCommand::executeHelp))

				// /coords list
				.then(Commands.literal("list")
						.executes(ScribeCommand::executeList))

				// /coords add <name>
				// /coords add <name> <coordinates>
				.then(Commands.literal("add")
						.then(Commands.argument("name", StringArgumentType.word())
								.executes(ctx -> executeSave(ctx, false, false))
								.then(Commands.argument("coordinates", BlockPosArgument.blockPos())
										.executes(ctx -> executeSave(ctx, false, true)))))

				// /coords replace <name>
				// /coords replace <name> <coordinates>
				.then(Commands.literal("replace")
						.then(Commands.argument("name", StringArgumentType.word())
								.suggests(SUGGEST_NAMES)
								.executes(ctx -> executeSave(ctx, true, false))
								.then(Commands.argument("coordinates", BlockPosArgument.blockPos())
										.executes(ctx -> executeSave(ctx, true, true)))))

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

	private static int executeSave(
			com.mojang.brigadier.context.CommandContext<CommandSourceStack> context,
			boolean allowOverwrite,
			boolean explicitCoordinates
	) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		BlockPos pos = explicitCoordinates
				? BlockPosArgument.getLoadedBlockPos(context, "coordinates")
				: player.blockPosition();
		return saveEntry(context, player, pos, allowOverwrite);
	}

	private static int saveEntry(
			com.mojang.brigadier.context.CommandContext<CommandSourceStack> context,
			ServerPlayer player,
			BlockPos pos,
			boolean allowOverwrite
	) {
		String name = StringArgumentType.getString(context, "name");

		if (!allowOverwrite && CoordinateStorage.exists(name)) {
			String replaceCmd = "/coords replace " + name;
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

		String dimension = player.level().dimension().identifier().toString();

		CoordinateStorage.put(new CoordinateEntry(name, pos, dimension));

		Component message = Component.literal("Saved waypoint '")
				.append(Component.literal(name).withStyle(ChatFormatting.AQUA))
				.append(Component.literal("' at "))
				.append(formatClickableCoords(pos))
				.append(formatDimension(dimension));

		context.getSource().sendSuccess(() -> message, false);
		return 1;
	}

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
		ChatFormatting color;
		switch (dimension) {
			case "minecraft:overworld":
				color = ChatFormatting.DARK_GREEN;
				break;
			case "minecraft:the_nether":
				color = ChatFormatting.RED;
				break;
			case "minecraft:the_end":
				color = ChatFormatting.DARK_PURPLE;
				break;
			default:
				color = ChatFormatting.GRAY;
				break;
		}
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
			return Component.literal(" [other dimension]").withStyle(ChatFormatting.DARK_GRAY);
		}

		double distance = Math.sqrt(player.blockPosition().distSqr(entry.pos()));
		return Component.literal(" (" + Math.round(distance) + "m)").withStyle(ChatFormatting.GRAY);
	}

	// ------------------------------------------------------------------
	// recall
	// ------------------------------------------------------------------

	private static int executeRecall(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
		String name = StringArgumentType.getString(context, "name");
		Optional<CoordinateEntry> found = CoordinateStorage.get(name);

		if (found.isEmpty()) {
			context.getSource().sendFailure(
					Component.literal("No waypoint named '" + name + "' was found. Use Tab to see saved names.")
			);
			return 0;
		}

		CoordinateEntry entry = found.get();
		Component message = Component.literal(entry.name() + ": ").withStyle(ChatFormatting.AQUA)
				.append(formatClickableCoords(entry.pos()))
				.append(formatDimension(entry.dimension()))
				.append(formatDistance(context.getSource(), entry));

		context.getSource().sendSuccess(() -> message, false);
		return 1;
	}

	// ------------------------------------------------------------------
	// remove
	// ------------------------------------------------------------------

	private static int executeRemove(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
		String name = StringArgumentType.getString(context, "name");
		boolean removed = CoordinateStorage.remove(name);

		if (removed) {
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

	private static int executeList(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
		if (CoordinateStorage.isEmpty()) {
			context.getSource().sendSuccess(
					() -> Component.literal("No waypoints saved yet. Try /coords add <name>."), false);
			return 1;
		}

		context.getSource().sendSuccess(() -> Component.literal("Saved waypoints:"), false);
		for (CoordinateEntry entry : CoordinateStorage.all()) {
			Component line = Component.literal("• ")
					.append(Component.literal(entry.name() + ": ").withStyle(ChatFormatting.AQUA))
					.append(formatClickableCoords(entry.pos()))
					.append(formatDimension(entry.dimension()))
					.append(formatDistance(context.getSource(), entry));
			context.getSource().sendSuccess(() -> line, false);
		}
		return 1;
	}

	// ------------------------------------------------------------------
	// help
	// ------------------------------------------------------------------

	private static int executeHelp(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
		context.getSource().sendSuccess(() -> Component.literal(
				"Scribe — save and recall coordinates (aliases: /coords, /coordinates, /scribe)\n"
						+ "  add <name> [<x> <y> <z>]      - save a waypoint (uses your position if coordinates are omitted)\n"
						+ "  replace <name> [<x> <y> <z>]  - overwrite an existing waypoint (or create if new)\n"
						+ "  recall <name>                 - show a saved waypoint (click its coordinates to copy them)\n"
						+ "  remove <name>                 - delete a saved waypoint\n"
						+ "  list                          - show every saved waypoint name"
		), false);
		return 1;
	}
}
