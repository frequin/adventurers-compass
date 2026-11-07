package io.github.frequin.adventurerscompass.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.frequin.adventurerscompass.util.OverlaySide;

class AdventurersCompassConfigTest {

	@TempDir
	Path dir;

	@Test
	void aFileWithoutItsClientBlockKeepsItsCommonOptionsAndTakesTheClientDefaults() throws IOException {
		Path file = write("{\"common\": {\"maxRadius\": 4000}}");

		AdventurersCompassConfig.load(file);

		assertEquals(4000, AdventurersCompassConfig.maxRadius);
		assertEquals(3, AdventurersCompassConfig.compassDurability);
		assertEquals(OverlaySide.LEFT, AdventurersCompassConfig.overlaySide);
		assertTrue(Files.readString(file).contains("\"overlaySide\""));
	}

	@Test
	void aFileWithoutItsCommonBlockTakesTheCommonDefaults() throws IOException {
		Path file = write("{\"client\": {\"overlaySide\": \"RIGHT\"}}");

		AdventurersCompassConfig.load(file);

		assertEquals(2500, AdventurersCompassConfig.maxRadius);
		assertEquals(OverlaySide.RIGHT, AdventurersCompassConfig.overlaySide);
		assertTrue(Files.readString(file).contains("\"maxRadius\": 2500"));
	}

	@Test
	void anEmptyFileTakesEveryDefault() throws IOException {
		Path file = write("");

		AdventurersCompassConfig.load(file);

		assertEquals(2500, AdventurersCompassConfig.maxRadius);
		assertEquals(OverlaySide.LEFT, AdventurersCompassConfig.overlaySide);
		assertTrue(Files.readString(file).contains("\"common\""));
	}

	private Path write(String json) throws IOException {
		Path file = dir.resolve("adventurerscompass.json");
		Files.writeString(file, json);
		return file;
	}

}
