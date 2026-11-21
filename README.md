# Scribe 📜

A lightweight Fabric mod for Minecraft **26.3+** that allows players to save and recall waypoints via simple chat commands.

## ✨ Features

- **Exact Aliases**: Use `/coords`, `/coordinates`, or `/scribe` interchangeably.
- **Vanilla Command Integration**: Use waypoint names directly in vanilla commands expecting coordinates (e.g., `/tp @s Base` or `/setblock Home stone`).
- **Smart Autocompletion**: Automatically suggests the coordinates of the block you are looking at as well as your saved waypoint names.
- **Client-Server Synchronization**: Automatically syncs waypoints to connected clients via Fabric Networking (`CustomPayload`), ensuring client-side autocompletion and valid syntax highlighting on multiplayer servers.
- **Safe Saving & Backup Protection**: Prevents accidental overwrites when adding waypoints. Creates automatic `.bak` backups if file recovery is needed.
- **Interactive Recall & List**: Click on recalled waypoints to copy `X Y Z` to clipboard. View paginated lists with interactive `[< Prev]` and `[Next >]` buttons.
- **Per-World Storage**: Waypoints are saved server-side in plain text at `[world]/data/scribe/saved_coords.txt`.
- **No Permissions Required**: Available to all players on the server.

> [!NOTE]
> When using waypoint names in vanilla commands such as `/tp @s <name>`, coordinates are resolved in your current dimension. To teleport across dimensions, use vanilla dimension syntax: `/execute in <dimension> run tp @s <name>`.

## 🚀 Commands

| Command | Description |
| :--- | :--- |
| `/coords add <name> [x y z [dimension]]` | Save a new waypoint (uses current position/dimension if omitted). |
| `/coords replace <name> [x y z [dimension]]` | Overwrite an existing waypoint (or create if new). |
| `/coords rename <old_name> <new_name>` | Rename an existing waypoint while preserving its position and list order. |
| `/coords recall <name>` | Display a saved waypoint with clickable teleport & copy-to-clipboard coordinates. |
| `/coords remove <name>` | Delete a saved waypoint. |
| `/coords near` | Find the closest saved waypoint in your current dimension. |
| `/coords search <query>` | Search saved waypoints by name or dimension. |
| `/coords list [page]` | View saved waypoints with clean pagination (8 per page). |
| `/coords help` | Display command help in chat. |

## 🛠️ Building & Installation

Requirements: **Java 25+**, **Minecraft 26.3+**, **Fabric Loader 0.16.0+**, and **Fabric API 0.166.0+**.

To build the mod from source:
```bash
./gradlew build
```
The compiled jar will be located in `build/libs/`. Drop it into your Minecraft `mods` folder along with Fabric API.

## 📄 License

Licensed under the MIT License.
