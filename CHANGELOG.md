# Changelog

All notable changes to this project will be documented in this file.

## [0.9.2-beta] - 2026-08-14

### Fixed
- Fixed command alias execution failing when running `/coordinates` or `/scribe` without subcommands.
- Fixed waypoint saving in unloaded chunks by avoiding forced chunk-loading checks.
- Fixed command execution from server console and command blocks when saving waypoints with explicit coordinates.
- Fixed race conditions during concurrent tab-completion and waypoint modifications.

### Changed
- Automatically clear in-memory coordinate data on server shutdown.
- Optimized tab-completion suggestions and cleaned up redundant command formatting.
- Added automated unit test suite.

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
