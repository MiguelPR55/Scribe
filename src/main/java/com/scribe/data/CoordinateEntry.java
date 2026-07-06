package com.scribe.data;

import net.minecraft.core.BlockPos;

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

	/**
	 * Formats this entry for the plain-text save file: "NAME X Y Z DIMENSION".
	 */
	public String toFileLine() {
		return name + " " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " " + dimension;
	}

	/**
	 * Parses one line of the save file back into a CoordinateEntry.
	 * Expected format: "NAME X Y Z DIMENSION" (exactly 5 space-separated tokens).
	 *
	 * @return the parsed entry, or null if the line is malformed.
	 */
	public static CoordinateEntry fromFileLine(String line) {
		String[] parts = line.trim().split("\\s+");
		if (parts.length != 5) {
			return null;
		}

		try {
			String name = parts[0];
			int x = Integer.parseInt(parts[1]);
			int y = Integer.parseInt(parts[2]);
			int z = Integer.parseInt(parts[3]);
			String dimension = parts[4];
			return new CoordinateEntry(name, new BlockPos(x, y, z), dimension);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
