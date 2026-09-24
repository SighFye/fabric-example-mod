package dev.belandsigh.mobheads;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoreMobHeadsModuleTest {
	private static final Path HEAD_TABLES = Path.of("src/main/resources/data/more_mob_heads/loot_table/entities");

	@Test
	void supportedMobsMatchBundledHeadTables() throws IOException {
		Set<String> bundled;
		try (Stream<Path> files = Files.list(HEAD_TABLES)) {
			bundled = files
				.map(path -> path.getFileName().toString())
				.filter(name -> name.endsWith(".json"))
				.map(name -> name.substring(0, name.length() - ".json".length()))
				.collect(Collectors.toSet());
		}
		assertEquals(bundled, MoreMobHeadsModule.SUPPORTED_MOBS);
	}
}
