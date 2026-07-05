# Scribe

A Fabric mod for Minecraft **26.2** that lets any player save and recall
named waypoints with quick chat commands: `/coords`, `/coordinates` and
`/scribe` (three exact aliases of the same command tree).

## Commands

```
/coords add <name>                      Save your current position as <name>
/coords add <name> <x> <y> <z>          Save an explicit position (supports ~ ~ ~ like /setblock)
/coords recall <name>                   Show a saved waypoint (Tab-completes <name>)
/coords remove <name>                   Delete a saved waypoint (Tab-completes <name>)
/coords list                            List every saved waypoint name
/coords help                            Show this summary in chat
```

Anyone can use every subcommand — there's no operator/permission check.

While typing the coordinates argument of `add`, Scribe suggests the block
you're currently looking at (computed server-side via a raycast from your
eyes along your view direction — see `LookedAtBlockSuggestionProvider`).

Clicking the coordinates shown by `recall` copies `"X Y Z"` to your
clipboard.

## Data storage

Waypoints are **per-world**, not per-client. They live in plain text at:

```
[world save folder]/data/scribe/saved_coords.txt
```

One line per waypoint: `NAME X Y Z DIMENSION`, e.g.:

```
CASA 100 50 -100 minecraft:overworld
```

The file is read once when the server (integrated singleplayer or
dedicated) starts, and rewritten immediately every time you add or remove
a waypoint.

## A note on this codebase: Minecraft 26.2 uses Mojang's official mappings

Minecraft became unobfuscated starting with 26.1, and Fabric stopped
maintaining Yarn from that version onward. That means the class/method
names in this project are Mojang's official ones, **not** the old Yarn
names you'll see in most existing tutorials:

| Yarn (pre-26.1)         | Mojang mappings (26.1+, used here) |
|--------------------------|-------------------------------------|
| `ServerCommandSource`   | `CommandSourceStack`                |
| `Text.literal(...)`     | `Component.literal(...)`            |
| `CommandManager.literal`| `Commands.literal`                  |
| `WorldSavePath.ROOT`    | `LevelResource.ROOT`                |
| `new ClickEvent(Action.SUGGEST_COMMAND, s)` | `new ClickEvent.SuggestCommand(s)` |

The build script also has no `mappings` line and uses plain
`implementation`/`api` instead of `modImplementation`/`modApi`, since
there's no obfuscation left to remap against.

## Building

Requires JDK 25.

```
./gradlew build
```

> This scaffold ships `gradle/wrapper/gradle-wrapper.properties` (pinned to
> Gradle 9.5.1, as recommended for Loom 1.17) but not the wrapper jar/script
> themselves. Run `gradle wrapper` once with a local Gradle install to
> generate `gradlew`, `gradlew.bat` and `gradle/wrapper/gradle-wrapper.jar`,
> or open the project in an IDE with Gradle support and let it regenerate
> them for you.

The built mod jar will be at `build/libs/scribe-1.0.0.jar`. Drop it, plus
Fabric API 0.154.0+26.2, into your `mods` folder together with Fabric
Loader 0.19.3+.

## Project layout

```
src/main/java/com/scribe/
├── Scribe.java                              mod entrypoint: registers commands + load-on-start
├── command/
│   ├── ScribeCommand.java                   command tree: add/recall/remove/list/help
│   ├── LookedAtBlockSuggestionProvider.java  server-side "looking at" raycast suggestion
│   └── CoordinateNameSuggestionProvider.java Tab-completion of saved names
└── data/
    ├── CoordinateEntry.java                 one waypoint (name, BlockPos, dimension)
    └── CoordinateStorage.java               in-memory map + saved_coords.txt read/write
```
