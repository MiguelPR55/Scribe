# Changelog

All notable changes to this project will be documented in this file.

## [0.9.1-beta] - 2026-07-08

### Added
- Display waypoint distance (in meters) and dimension warning when viewing coordinates in `/coords list` and `/coords recall`.

### Changed
- Atomic file saving for `saved_coords.txt` with temporary file fallback to prevent data loss on server crash.
- Optimized Brigadier command tree registration and redirection for command aliases (`/coordinates`, `/scribe`).
- Updated ModMenu quick guide descriptions and standardized internal documentation in English.
