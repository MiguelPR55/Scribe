package com.scribe.data;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Holds all Scribe coordinate entries for the currently loaded world, and
 * keeps {@code [world]/data/scribe/saved_coords.txt} in sync with them.
 * <p>
 * This is intentionally per-world: the file lives inside the save folder
 * returned by {@link MinecraftServer#getWorldPath(LevelResource)}, so a
 * fresh world (or a different save) starts with an empty list.
 * <p>
 * Lookups are case-insensitive (keys are stored upper-cased), but the
 * original casing typed by the player is preserved for display.
 * <p>
 * Performance optimization: immutable snapshots of {@code names()} and
 * {@code all()} are cached and only rebuilt when entries are modified,
 * eliminating allocations on every chat keystroke / Brigadier tab-completion.
 */
public final class CoordinateStorage {

	private static final Logger LOGGER = LoggerFactory.getLogger("scribe");
	private static final String DATA_DIR = "scribe";
	private static final String FILE_NAME = "saved_coords.txt";

	// LinkedHashMap keeps insertion order, which makes /coords list and
	// tab-completion feel stable/predictable to the player.
	private static final Map<String, CoordinateEntry> ENTRIES = new LinkedHashMap<>();

	// Cached immutable snapshots to avoid allocation on every keystroke/tab-completion
	private static volatile List<String> cachedNames = List.of();
	private static volatile List<CoordinateEntry> cachedEntries = List.of();

	private static Path filePath;
	private static boolean loadedSuccessfully = false;
	private static boolean activeServer = false;

	private CoordinateStorage() {
	}

	/**
	 * Resolves the save file path for the given server and loads any
	 * existing entries into memory. Call this on server start (see
	 * {@link com.scribe.Scribe}).
	 */
	public static synchronized void load(MinecraftServer server) {
		activeServer = true;
		Path worldRoot = server.getWorldPath(LevelResource.ROOT);
		Path dataDir = worldRoot.resolve("data").resolve(DATA_DIR);
		load(dataDir.resolve(FILE_NAME));
		activeServer = true;
	}

	/**
	 * Loads entries directly from the specified file path.
	 * Decoupled from MinecraftServer for direct file loading and testability.
	 */
	public static synchronized void load(Path path) {
		filePath = path;
		ENTRIES.clear();
		loadedSuccessfully = false;

		if (!Files.exists(filePath)) {
			loadedSuccessfully = true;
			rebuildCache();
			LOGGER.info("[Scribe] No existing save file found, starting fresh at {}", filePath);
			return;
		}

		try {
			List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
			for (String line : lines) {
				if (line.isBlank() || line.trim().startsWith("#")) {
					continue;
				}
				CoordinateEntry entry = CoordinateEntry.fromFileLine(line);
				if (entry == null) {
					LOGGER.warn("[Scribe] Skipping malformed line in {}: '{}'", filePath, line);
					continue;
				}
				ENTRIES.put(key(entry.name()), entry);
			}
			loadedSuccessfully = true;
			rebuildCache();
			LOGGER.info("[Scribe] Loaded {} saved coordinate(s) from {}", ENTRIES.size(), filePath);
		} catch (IOException e) {
			rebuildCache();
			LOGGER.error("[Scribe] Failed to read {}", filePath, e);
		}
	}

	/**
	 * Clears in-memory entries and resets file path on server shutdown.
	 */
	public static synchronized void unload() {
		ENTRIES.clear();
		filePath = null;
		loadedSuccessfully = false;
		activeServer = false;
		rebuildCache();
	}

	/**
	 * Clears in-memory entries when the client disconnects from a remote server.
	 * If an integrated server is running in this JVM, {@link #unload()} on
	 * {@code SERVER_STOPPED} will handle the cleanup after the server finishes saving.
	 */
	public static synchronized void unloadClient() {
		if (!activeServer) {
			unload();
		}
	}

	/**
	 * Synchronizes entries in-memory directly from a network payload.
	 * Does not write to disk, ensuring client-side multiplayer safety.
	 */
	public static synchronized void syncFromServer(Collection<CoordinateEntry> entries) {
		ENTRIES.clear();
		for (CoordinateEntry entry : entries) {
			ENTRIES.put(key(entry.name()), entry);
		}
		rebuildCache();
	}

	public static synchronized boolean isLoadedSuccessfully() {
		return loadedSuccessfully;
	}

	/**
	 * Adds (or overwrites) an entry and immediately rewrites the save file.
	 */
	public static synchronized void put(CoordinateEntry entry) {
		ENTRIES.put(key(entry.name()), entry);
		rebuildCache();
		save();
	}

	/**
	 * Removes an entry by name (case-insensitive).
	 *
	 * @return true if an entry was removed.
	 */
	public static synchronized boolean remove(String name) {
		boolean removed = ENTRIES.remove(key(name)) != null;
		if (removed) {
			rebuildCache();
			save();
		}
		return removed;
	}

	/**
	 * Renames an existing entry while preserving its position in the insertion order
	 * and performing a single atomic disk write.
	 *
	 * @return the updated CoordinateEntry, or empty if oldName did not exist.
	 */
	public static synchronized Optional<CoordinateEntry> rename(String oldName, String newName) {
		if (oldName == null || newName == null) {
			return Optional.empty();
		}
		String oldKey = key(oldName);
		CoordinateEntry existing = ENTRIES.get(oldKey);
		if (existing == null) {
			return Optional.empty();
		}

		String newKey = key(newName);
		CoordinateEntry updated = new CoordinateEntry(newName, existing.pos(), existing.dimension());

		if (oldKey.equals(newKey)) {
			ENTRIES.put(oldKey, updated);
		} else {
			// Rebuild map entries to preserve insertion order at the original position
			Map<String, CoordinateEntry> reordered = new LinkedHashMap<>(ENTRIES.size());
			for (Map.Entry<String, CoordinateEntry> e : ENTRIES.entrySet()) {
				if (e.getKey().equals(oldKey)) {
					reordered.put(newKey, updated);
				} else {
					reordered.put(e.getKey(), e.getValue());
				}
			}
			ENTRIES.clear();
			ENTRIES.putAll(reordered);
		}

		rebuildCache();
		save();
		return Optional.of(updated);
	}

	public static synchronized Optional<CoordinateEntry> get(String name) {
		if (name == null || name.isEmpty()) {
			return Optional.empty();
		}
		return Optional.ofNullable(ENTRIES.get(key(name)));
	}

	public static synchronized boolean exists(String name) {
		if (name == null || name.isEmpty()) {
			return false;
		}
		return ENTRIES.containsKey(key(name));
	}

	/**
	 * @return an immutable snapshot of display names of every saved entry, in insertion order.
	 * Lock-free fast read via volatile cache for zero-allocation tab-completion.
	 */
	public static List<String> names() {
		return cachedNames;
	}

	/**
	 * @return an immutable snapshot of all saved entries, in insertion order.
	 * Lock-free fast read via volatile cache.
	 */
	public static List<CoordinateEntry> all() {
		return cachedEntries;
	}

	public static boolean isEmpty() {
		return cachedEntries.isEmpty();
	}

	private static void rebuildCache() {
		if (ENTRIES.isEmpty()) {
			cachedEntries = List.of();
			cachedNames = List.of();
			return;
		}
		List<CoordinateEntry> entriesSnapshot = List.copyOf(ENTRIES.values());
		String[] namesArr = new String[entriesSnapshot.size()];
		for (int i = 0; i < entriesSnapshot.size(); i++) {
			namesArr[i] = entriesSnapshot.get(i).name();
		}
		cachedEntries = entriesSnapshot;
		cachedNames = List.of(namesArr);
	}

	private static String key(String name) {
		return name.toUpperCase(Locale.ROOT);
	}

	/**
	 * Rewrites the whole save file from the current in-memory state.
	 * Simple and safe: the file is tiny (a handful of short lines per
	 * waypoint), so a full rewrite on every change is cheap and avoids
	 * any risk of the in-memory map and the file drifting apart.
	 */
	private static void save() {
		if (filePath == null) {
			LOGGER.warn("[Scribe] save() called before load() — no world loaded yet, skipping write.");
			return;
		}

		String baseName = filePath.getFileName().toString();
		Path tmp = filePath.resolveSibling(baseName + ".tmp");
		try {
			// If initial load failed, protect existing file from being overwritten by creating a backup
			if (!loadedSuccessfully && Files.exists(filePath)) {
				Path backup = filePath.resolveSibling(baseName + ".bak");
				Files.copy(filePath, backup, StandardCopyOption.REPLACE_EXISTING);
				LOGGER.warn("[Scribe] Existing coordinate file backed up to {} before write due to previous load failure", backup);
				loadedSuccessfully = true;
			}

			Path parentDir = filePath.getParent();
			if (parentDir != null) {
				Files.createDirectories(parentDir);
			}

			String lineSep = System.lineSeparator();
			StringBuilder content = new StringBuilder(128 + ENTRIES.size() * 48);
			content.append("# Scribe Waypoints - Format: NAME X Y Z DIMENSION").append(lineSep);
			content.append("# Saved automatically. Manual edits must preserve whitespace separation.").append(lineSep);
			for (CoordinateEntry entry : cachedEntries) {
				content.append(entry.toFileLine()).append(lineSep);
			}

			// Write first to a temporary file and rename atomically
			// so saved_coords.txt is not left corrupted if the server
			// crashes during the write.
			Files.writeString(tmp, content.toString(), StandardCharsets.UTF_8);
			try {
				Files.move(tmp, filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(tmp, filePath, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			LOGGER.error("[Scribe] Failed to write {}", filePath, e);
			try {
				Files.deleteIfExists(tmp);
			} catch (IOException ignored) {
			}
		}
	}
}
