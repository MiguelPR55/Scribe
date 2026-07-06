# Scribe 📜

A lightweight Fabric mod for Minecraft **26.2** that allows players to save and recall waypoints via simple chat commands.

## ✨ Features

- **Exact Aliases**: Use `/coords`, `/coordinates`, or `/scribe` interchangeably.
- **Smart Autocompletion**: Automatically suggests the coordinates of the block you are looking at (just like `/setblock`).
- **Safe Saving**: Prevents accidental overwrites when adding waypoints. Use `/coords replace` to update existing waypoints.
- **Interactive Recall**: Click on any recalled waypoint in chat to copy its `X Y Z` coordinates directly to your clipboard.
- **Per-World Storage**: Waypoints are saved server-side in plain text at `[world]/data/scribe/saved_coords.txt`.
- **No Permissions Required**: Available to all players on the server.

## 🚀 Commands

| Command | Description |
| :--- | :--- |
| `/coords add <name> [x y z]` | Save a new waypoint (uses current position if omitted). |
| `/coords replace <name> [x y z]` | Overwrite an existing waypoint (or create if new). |
| `/coords recall <name>` | Display a saved waypoint with clickable copy-to-clipboard coordinates. |
| `/coords remove <name>` | Delete a saved waypoint. |
| `/coords list` | Show a list of all saved waypoints. |
| `/coords help` | Display command help in chat. |

## 🛠️ Building & Installation

Requirements: **Java 25** and **Minecraft 26.2 (Fabric Loader & Fabric API)**.

To build the mod from source:
```bash
./gradlew build
```
The compiled jar will be located in `build/libs/`. Drop it into your Minecraft `mods` folder along with Fabric API.

## 📄 License

Licensed under the MIT License.
