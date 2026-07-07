package com.scribe.data;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
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
 */
public final class CoordinateStorage {

	private static final Logger LOGGER = LoggerFactory.getLogger("scribe");
	private static final String DATA_DIR = "scribe";
	private static final String FILE_NAME = "saved_coords.txt";

	// LinkedHashMap keeps insertion order, which makes /coords list and
	// tab-completion feel stable/predictable to the player.
	private static final Map<String, CoordinateEntry> ENTRIES = new LinkedHashMap<>();

	private static Path filePath;

	private CoordinateStorage() {
	}

	/**
	 * Resolves the save file path for the given server and loads any
	 * existing entries into memory. Call this on server start (see
	 * {@link com.scribe.Scribe}).
	 */
	public static void load(MinecraftServer server) {
		Path worldRoot = server.getWorldPath(LevelResource.ROOT);
		Path dataDir = worldRoot.resolve("data").resolve(DATA_DIR);
		filePath = dataDir.resolve(FILE_NAME);

		ENTRIES.clear();

		if (!Files.exists(filePath)) {
			LOGGER.info("[Scribe] No existing save file found, starting fresh at {}", filePath);
			return;
		}

		try {
			List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
			for (String line : lines) {
				if (line.isBlank()) {
					continue;
				}
				CoordinateEntry entry = CoordinateEntry.fromFileLine(line);
				if (entry == null) {
					LOGGER.warn("[Scribe] Skipping malformed line in {}: '{}'", filePath, line);
					continue;
				}
				ENTRIES.put(key(entry.name()), entry);
			}
			LOGGER.info("[Scribe] Loaded {} saved coordinate(s) from {}", ENTRIES.size(), filePath);
		} catch (IOException e) {
			LOGGER.error("[Scribe] Failed to read {}", filePath, e);
		}
	}

	/**
	 * Adds (or overwrites) an entry and immediately rewrites the save file.
	 */
	public static void put(CoordinateEntry entry) {
		ENTRIES.put(key(entry.name()), entry);
		save();
	}

	/**
	 * Removes an entry by name (case-insensitive).
	 *
	 * @return true if an entry was removed.
	 */
	public static boolean remove(String name) {
		boolean removed = ENTRIES.remove(key(name)) != null;
		if (removed) {
			save();
		}
		return removed;
	}

	public static Optional<CoordinateEntry> get(String name) {
		return Optional.ofNullable(ENTRIES.get(key(name)));
	}

	public static boolean exists(String name) {
		return ENTRIES.containsKey(key(name));
	}

	/**
	 * @return the display names of every saved entry, in insertion order.
	 * Used both for /coords list and for tab-completion suggestions.
	 */
	public static Collection<String> names() {
		return ENTRIES.values().stream().map(CoordinateEntry::name).toList();
	}

	public static Collection<CoordinateEntry> all() {
		return ENTRIES.values();
	}

	public static boolean isEmpty() {
		return ENTRIES.isEmpty();
	}

	private static String key(String name) {
		return name.toUpperCase(java.util.Locale.ROOT);
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

		try {
			Files.createDirectories(filePath.getParent());

			StringBuilder content = new StringBuilder();
			for (CoordinateEntry entry : ENTRIES.values()) {
				content.append(entry.toFileLine()).append(System.lineSeparator());
			}

			// Se escribe primero a un fichero temporal y se renombra de forma
			// atómica, para no dejar saved_coords.txt corrupto si el servidor
			// se cae justo durante la escritura.
			Path tmp = filePath.resolveSibling(FILE_NAME + ".tmp");
			Files.writeString(tmp, content.toString(), StandardCharsets.UTF_8);
			try {
				Files.move(tmp, filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (java.nio.file.AtomicMoveNotSupportedException e) {
				Files.move(tmp, filePath, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			LOGGER.error("[Scribe] Failed to write {}", filePath, e);
		}
	}
}
