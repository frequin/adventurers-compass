package io.github.frequin.adventurerscompass.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

/** Every language names every vanilla Group, read from the structure sets of the Minecraft jar, and the catch-all Group. */
class GroupNamesTest {

	private static final String STRUCTURE_SETS = "data/minecraft/worldgen/structure_set/";

	private static List<String> groupKeys() throws IOException, URISyntaxException {
		URI villages = GroupNamesTest.class.getClassLoader().getResource(STRUCTURE_SETS + "villages.json").toURI();
		List<String> keys = new ArrayList<>(List.of("group.adventurerscompass.none"));
		try (FileSystem jar = FileSystems.newFileSystem(villages, Map.of()); Stream<Path> sets = Files.list(jar.getPath(STRUCTURE_SETS))) {
			sets.map(set -> set.getFileName().toString().replace(".json", "")).sorted().forEach(set -> keys.add("group.minecraft." + set));
		}
		return keys;
	}

	@Test
	void theJarListsTheVanillaGroups() throws IOException, URISyntaxException {
		assertTrue(groupKeys().contains("group.minecraft.nether_complexes"));
	}

	@Test
	void everyLanguageNamesEveryVanillaGroup() throws IOException, URISyntaxException {
		List<String> keys = groupKeys();
		List<String> missing = new ArrayList<>();
		for (Path file : LanguageFiles.all()) {
			JsonObject texts = LanguageFiles.read(file);
			keys.stream().filter(key -> !texts.has(key)).forEach(key -> missing.add(file.getFileName() + ": " + key));
		}
		assertEquals(List.of(), missing);
	}

}
