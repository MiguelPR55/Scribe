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

	@Test
	@DisplayName("Should handle loading from non-existent file cleanly")
	void testLoadNonExistentFile(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tempDir) {
		java.nio.file.Path file = tempDir.resolve("saved_coords.txt");
		CoordinateStorage.load(file);

		assertTrue(CoordinateStorage.isLoadedSuccessfully());
		assertTrue(CoordinateStorage.isEmpty());
	}

	@Test
	@DisplayName("Should save entries with header and reload correctly from disk")
	void testSaveAndReload(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tempDir) throws java.io.IOException {
		java.nio.file.Path file = tempDir.resolve("saved_coords.txt");
		CoordinateStorage.load(file);

		CoordinateEntry entry = new CoordinateEntry("Spawn", new BlockPos(0, 64, 0), "minecraft:overworld");
		CoordinateStorage.put(entry);

		assertTrue(java.nio.file.Files.exists(file));
		String content = java.nio.file.Files.readString(file);
		assertTrue(content.contains("# Scribe Waypoints"));
		assertTrue(content.contains("Spawn 0 64 0 minecraft:overworld"));

		// Unload from memory
		CoordinateStorage.unload();
		assertFalse(CoordinateStorage.exists("Spawn"));

		// Reload from disk
		CoordinateStorage.load(file);
		assertTrue(CoordinateStorage.exists("Spawn"));
		assertEquals(new BlockPos(0, 64, 0), CoordinateStorage.get("Spawn").orElseThrow().pos());
	}

	@Test
	@DisplayName("Should ignore comments and skip malformed lines during load")
	void testLoadWithCommentsAndMalformedLines(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tempDir) throws java.io.IOException {
		java.nio.file.Path file = tempDir.resolve("saved_coords.txt");
		String fileContent = """
				# Custom user comment
				# Another comment
				Base 100 64 200 minecraft:overworld
				broken_line_without_enough_tokens
				Portal 300 70 800 minecraft:the_nether
				""";
		java.nio.file.Files.writeString(file, fileContent);

		CoordinateStorage.load(file);
		assertTrue(CoordinateStorage.isLoadedSuccessfully());
		assertEquals(2, CoordinateStorage.all().size());
		assertTrue(CoordinateStorage.exists("Base"));
		assertTrue(CoordinateStorage.exists("Portal"));
	}

	@Test
	@DisplayName("Should create .bak backup before saving if previous load failed")
	void testBackupCreationOnPreviousLoadFailure(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tempDir) throws java.io.IOException {
		java.nio.file.Path file = tempDir.resolve("saved_coords.txt");
		java.nio.file.Path backupFile = tempDir.resolve("saved_coords.txt.bak");
		java.nio.file.Files.writeString(file, "PreExisting 99 99 99 minecraft:overworld\n");

		// Simulate load failure
		CoordinateStorage.load(file);
		// Force loadedSuccessfully to false by unsetting through reflection or simulating unhandled load
		java.lang.reflect.Field field;
		try {
			field = CoordinateStorage.class.getDeclaredField("loadedSuccessfully");
			field.setAccessible(true);
			field.set(null, false);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}

		assertFalse(CoordinateStorage.isLoadedSuccessfully());

		// Saving now should create a .bak file before overwriting
		CoordinateStorage.put(new CoordinateEntry("NewEntry", new BlockPos(1, 2, 3), "minecraft:overworld"));

		assertTrue(java.nio.file.Files.exists(backupFile), "Backup file should have been created");
		String backupContent = java.nio.file.Files.readString(backupFile);
		assertTrue(backupContent.contains("PreExisting 99 99 99 minecraft:overworld"));

		String newContent = java.nio.file.Files.readString(file);
		assertTrue(newContent.contains("NewEntry 1 2 3 minecraft:overworld"));
	}

	@Test
	@DisplayName("Should update in-memory storage from syncFromServer without writing to disk")
	void testSyncFromServer(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tempDir) {
		java.nio.file.Path file = tempDir.resolve("saved_coords.txt");
		CoordinateStorage.load(file);

		CoordinateEntry serverEntry = new CoordinateEntry("RemoteBase", new BlockPos(500, 72, -300), "minecraft:overworld");
		CoordinateStorage.syncFromServer(java.util.List.of(serverEntry));

		assertTrue(CoordinateStorage.exists("RemoteBase"));
		assertEquals(1, CoordinateStorage.all().size());
		// Verify no disk write occurred for remote base
		assertFalse(java.nio.file.Files.exists(file));
	}

	@Test
	@DisplayName("Should rename entry while preserving insertion order and new casing")
	void testRenamePreservesOrder(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tempDir) {
		java.nio.file.Path file = tempDir.resolve("saved_coords.txt");
		CoordinateStorage.load(file);

		CoordinateStorage.put(new CoordinateEntry("First", new BlockPos(1, 1, 1), "minecraft:overworld"));
		CoordinateStorage.put(new CoordinateEntry("Second", new BlockPos(2, 2, 2), "minecraft:overworld"));
		CoordinateStorage.put(new CoordinateEntry("Third", new BlockPos(3, 3, 3), "minecraft:overworld"));

		Optional<CoordinateEntry> renamed = CoordinateStorage.rename("second", "Middle");
		assertTrue(renamed.isPresent());
		assertEquals("Middle", renamed.get().name());
		assertFalse(CoordinateStorage.exists("Second"));
		assertTrue(CoordinateStorage.exists("Middle"));

		assertArrayEquals(new String[]{"First", "Middle", "Third"}, CoordinateStorage.names().toArray(new String[0]));
	}

	@Test
	@DisplayName("Should protect active server storage when unloadClient is called")
	void testUnloadClientOnActiveServer() throws ReflectiveOperationException {
		CoordinateStorage.put(new CoordinateEntry("LocalWorldBase", new BlockPos(10, 64, 10), "minecraft:overworld"));

		// When activeServer is false (remote multiplayer client), unloadClient clears entries
		CoordinateStorage.unloadClient();
		assertTrue(CoordinateStorage.isEmpty());

		// When activeServer is true (integrated singleplayer server), unloadClient must NOT wipe entries before SERVER_STOPPED
		CoordinateStorage.put(new CoordinateEntry("SingleplayerBase", new BlockPos(10, 64, 10), "minecraft:overworld"));
		java.lang.reflect.Field activeServerField = CoordinateStorage.class.getDeclaredField("activeServer");
		activeServerField.setAccessible(true);
		activeServerField.set(null, true);

		CoordinateStorage.unloadClient();
		assertTrue(CoordinateStorage.exists("SingleplayerBase"), "Integrated server entries must survive client disconnect event");
	}
}
