package io.github.frequin.adventurerscompass.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/** The compass domain module must not depend on the Minecraft runtime, Fabric or the mod's adapters. */
class DomainIsPureJavaTest {

	private static final Path DOMAIN = Path.of("src/main/java/io/github/frequin/adventurerscompass/domain");
	/** Any use of Minecraft, Fabric or Mojang code, imported or fully qualified, and any import of the mod's adapters. */
	private static final Pattern FORBIDDEN = Pattern.compile("\\b(net\\.minecraft|net\\.fabricmc|com\\.mojang)\\.|^import\\s+(static\\s+)?io\\.github\\.frequin\\.adventurerscompass\\.(?!domain)", Pattern.MULTILINE);

	@Test
	void usesNoMinecraftFabricOrAdapterCode() throws IOException {
		List<Path> sources;
		try (Stream<Path> files = Files.walk(DOMAIN)) {
			sources = files.filter(file -> file.toString().endsWith(".java")).toList();
		}
		assertFalse(sources.isEmpty(), "no domain sources found under " + DOMAIN.toAbsolutePath());

		List<String> offenders = sources.stream().filter(file -> {
			try {
				return FORBIDDEN.matcher(Files.readString(file)).find();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}).map(Path::toString).toList();
		assertEquals(List.of(), offenders);
	}

}
