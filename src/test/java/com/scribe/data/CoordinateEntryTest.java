package com.scribe.data;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CoordinateEntryTest {

	@Test
	@DisplayName("Should correctly serialize entry to file line format")
	void testToFileLine() {
		CoordinateEntry entry = new CoordinateEntry("Base", new BlockPos(100, 64, -200), "minecraft:overworld");
		assertEquals("Base 100 64 -200 minecraft:overworld", entry.toFileLine());
	}

	@Test
	@DisplayName("Should correctly parse valid line into CoordinateEntry")
	void testFromFileLineValid() {
		CoordinateEntry entry = CoordinateEntry.fromFileLine("Home 1500 70 -3200 minecraft:the_nether");
		assertNotNull(entry);
		assertEquals("Home", entry.name());
		assertEquals(new BlockPos(1500, 70, -3200), entry.pos());
		assertEquals("minecraft:the_nether", entry.dimension());
	}

	@Test
	@DisplayName("Should handle multiple whitespace separators when parsing")
	void testFromFileLineWithExtraWhitespace() {
		CoordinateEntry entry = CoordinateEntry.fromFileLine("   Portal   0   120   0   minecraft:the_end   ");
		assertNotNull(entry);
		assertEquals("Portal", entry.name());
		assertEquals(new BlockPos(0, 120, 0), entry.pos());
		assertEquals("minecraft:the_end", entry.dimension());
	}

	@Test
	@DisplayName("Should return null for malformed or incomplete lines")
	void testFromFileLineMalformed() {
		assertNull(CoordinateEntry.fromFileLine(null));
		assertNull(CoordinateEntry.fromFileLine(""));
		assertNull(CoordinateEntry.fromFileLine("   "));
		assertNull(CoordinateEntry.fromFileLine("Base 100 64"));
		assertNull(CoordinateEntry.fromFileLine("Base 100 64 -200"));
		assertNull(CoordinateEntry.fromFileLine("Base not_a_number 64 -200 minecraft:overworld"));
		assertNull(CoordinateEntry.fromFileLine("Base 100 64 -200 minecraft:overworld extra_token"));
	}

	@Test
	@DisplayName("Should reject null fields in constructor")
	void testNullValidation() {
		assertThrows(NullPointerException.class, () -> new CoordinateEntry(null, new BlockPos(0, 0, 0), "minecraft:overworld"));
		assertThrows(NullPointerException.class, () -> new CoordinateEntry("Base", null, "minecraft:overworld"));
		assertThrows(NullPointerException.class, () -> new CoordinateEntry("Base", new BlockPos(0, 0, 0), null));
	}
}
