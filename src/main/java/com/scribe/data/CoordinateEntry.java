package com.scribe.data;

import net.minecraft.core.BlockPos;

import java.util.Objects;

/**
 * A single saved coordinate entry.
 * <p>
 * Coordinates are always stored as whole block coordinates (integers) —
 * relative arguments like {@code ~ ~ ~} are resolved to absolute
 * coordinates before a CoordinateEntry is ever created.
 *
 * @param name      the display name of this entry, e.g. "CASA"
 * @param pos       the absolute block position
 * @param dimension the dimension id as a string, e.g. "minecraft:overworld"
 */
public record CoordinateEntry(String name, BlockPos pos, String dimension) {

	public CoordinateEntry {
		Objects.requireNonNull(name, "name cannot be null");
		Objects.requireNonNull(pos, "pos cannot be null");
		Objects.requireNonNull(dimension, "dimension cannot be null");
	}

	/**
	 * Formats this entry for the plain-text save file: "NAME X Y Z DIMENSION".
	 */
	public String toFileLine() {
		return name + " " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " " + dimension;
	}

	/**
	 * Parses one line of the save file back into a CoordinateEntry.
	 * Expected format: "NAME X Y Z DIMENSION" (exactly 5 whitespace-separated tokens).
	 * Uses a fast zero-regex scanner to avoid pattern allocation during file loading.
	 *
	 * @return the parsed entry, or null if the line is malformed.
	 */
	public static CoordinateEntry fromFileLine(String line) {
		if (line == null) {
			return null;
		}
		int len = line.length();
		int idx = 0;

		String[] tokens = new String[5];
		for (int t = 0; t < 5; t++) {
			while (idx < len && Character.isWhitespace(line.charAt(idx))) {
				idx++;
			}
			if (idx >= len) {
				return null;
			}
			int start = idx;
			while (idx < len && !Character.isWhitespace(line.charAt(idx))) {
				idx++;
			}
			tokens[t] = line.substring(start, idx);
		}

		// Ensure no extra trailing tokens exist
		while (idx < len && Character.isWhitespace(line.charAt(idx))) {
			idx++;
		}
		if (idx < len) {
			return null;
		}

		try {
			int x = Integer.parseInt(tokens[1]);
			int y = Integer.parseInt(tokens[2]);
			int z = Integer.parseInt(tokens[3]);
			return new CoordinateEntry(tokens[0], new BlockPos(x, y, z), tokens[4]);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}

