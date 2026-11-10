# Changelog

All notable changes to this project will be documented in this file.

## [0.9.3-beta] - 2026-11-10

### Added
- Updated to Minecraft 26.3 and Fabric API 0.166.0+26.3 (ModMenu 21.0.0).
- Waypoint names in `/coords list` and `/coords recall` are now clickable to quickly suggest `/coords recall <name>` in chat.
- Support for comment lines starting with `#` in `saved_coords.txt`.

### Changed
- Updated minimum dependencies in `fabric.mod.json` for Minecraft 26.3+.

## [0.9.2-beta] - 2026-08-14

## [0.9.1.1-beta] - 2026-08-04

### Changed
- Removed redundant `[other dimension]` text from `/coords list` and `/coords recall` commands.

## [0.9.1-beta] - 2026-07-08

### Added
- Display waypoint distance (in meters) and dimension warning when viewing coordinates in `/coords list` and `/coords recall`.

### Changed
- Atomic file saving for `saved_coords.txt` with temporary file fallback to prevent data loss on server crash.
- Optimized Brigadier command tree registration and redirection for command aliases (`/coordinates`, `/scribe`).
- Updated ModMenu quick guide descriptions and standardized internal documentation in English.
