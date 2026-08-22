package com.scribe.data;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CoordinateStorageTest {

	@BeforeEach
	@AfterEach
	void cleanup() {
		CoordinateStorage.unload();
	}

	@Test
	@DisplayName("Should store and retrieve entries with case-insensitivity")
	void testPutAndGetCaseInsensitive() {
		CoordinateEntry entry = new CoordinateEntry("BasePrincipal", new BlockPos(100, 64, -200), "minecraft:overworld");
		CoordinateStorage.put(entry);

		assertTrue(CoordinateStorage.exists("BasePrincipal"));
		assertTrue(CoordinateStorage.exists("baseprincipal"));
		assertTrue(CoordinateStorage.exists("BASEPRINCIPAL"));

		Optional<CoordinateEntry> found = CoordinateStorage.get("baseprincipal");
		assertTrue(found.isPresent());
		assertEquals("BasePrincipal", found.get().name(), "Original casing should be preserved");
		assertEquals(new BlockPos(100, 64, -200), found.get().pos());
	}

	@Test
	@DisplayName("Should remove entries correctly")
	void testRemove() {
		CoordinateEntry entry = new CoordinateEntry("Fortress", new BlockPos(300, 70, -800), "minecraft:the_nether");
		CoordinateStorage.put(entry);

		assertTrue(CoordinateStorage.exists("fortress"));
		assertTrue(CoordinateStorage.remove("FORTRESS"));
		assertFalse(CoordinateStorage.exists("fortress"));
		assertFalse(CoordinateStorage.remove("fortress"));
	}

	@Test
	@DisplayName("Should preserve insertion order for names and list")
	void testInsertionOrder() {
		CoordinateStorage.put(new CoordinateEntry("Alpha", new BlockPos(1, 1, 1), "minecraft:overworld"));
		CoordinateStorage.put(new CoordinateEntry("Beta", new BlockPos(2, 2, 2), "minecraft:overworld"));
		CoordinateStorage.put(new CoordinateEntry("Gamma", new BlockPos(3, 3, 3), "minecraft:overworld"));

		Collection<String> names = CoordinateStorage.names();
		assertArrayEquals(new String[]{"Alpha", "Beta", "Gamma"}, names.toArray(new String[0]));
	}

	@Test
	@DisplayName("Should overwrite entry with same case-insensitive key while preserving new casing")
	void testOverwrite() {
		CoordinateStorage.put(new CoordinateEntry("Casa", new BlockPos(10, 64, 10), "minecraft:overworld"));
		assertEquals(1, CoordinateStorage.all().size());

		CoordinateStorage.put(new CoordinateEntry("CASA", new BlockPos(20, 65, 20), "minecraft:overworld"));
		assertEquals(1, CoordinateStorage.all().size());

		Optional<CoordinateEntry> found = CoordinateStorage.get("casa");
		assertTrue(found.isPresent());
		assertEquals("CASA", found.get().name());
		assertEquals(new BlockPos(20, 65, 20), found.get().pos());
	}

	@Test
	@DisplayName("Should validate waypoint names appropriately to avoid coordinate syntax conflicts")
	void testWaypointNameValidation() {
		// Valid names
		assertTrue(com.scribe.command.ScribeCommand.isValidWaypointName("Base"));
		assertTrue(com.scribe.command.ScribeCommand.isValidWaypointName("mi_casa"));
		assertTrue(com.scribe.command.ScribeCommand.isValidWaypointName("_underground"));
		assertTrue(com.scribe.command.ScribeCommand.isValidWaypointName("Base2"));

		// Invalid names (cannot start with digits or coordinate special chars)
		assertFalse(com.scribe.command.ScribeCommand.isValidWaypointName(null));
		assertFalse(com.scribe.command.ScribeCommand.isValidWaypointName(""));
		assertFalse(com.scribe.command.ScribeCommand.isValidWaypointName("1base"));
		assertFalse(com.scribe.command.ScribeCommand.isValidWaypointName("100"));
		assertFalse(com.scribe.command.ScribeCommand.isValidWaypointName("~home"));
		assertFalse(com.scribe.command.ScribeCommand.isValidWaypointName("^look"));
		assertFalse(com.scribe.command.ScribeCommand.isValidWaypointName("-minus"));
		assertFalse(com.scribe.command.ScribeCommand.isValidWaypointName("+plus"));
		assertFalse(com.scribe.command.ScribeCommand.isValidWaypointName(".dot"));
	}
}
