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
		String[] p = line.trim().split("\\s+");
		if (p.length != 5) return null;
		try {
			return new CoordinateEntry(p[0], new BlockPos(Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3])), p[4]);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
