package io.github.frequin.adventurerscompass.gui;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** The mod's language files. A key a file lacks falls back to en_us, as in the game. */
final class LanguageFiles {

	private static final Path DIRECTORY = Path.of("src/main/resources/assets/adventurerscompass/lang");

	private LanguageFiles() {
	}

	static List<Path> all() {
		try (Stream<Path> files = Files.list(DIRECTORY)) {
			return files.sorted().toList();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	static JsonObject read(Path file) {
		try (Reader reader = Files.newBufferedReader(file)) {
			return JsonParser.parseReader(reader).getAsJsonObject();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	static JsonObject english() {
		return read(DIRECTORY.resolve("en_us.json"));
	}

}
