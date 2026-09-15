package dev.belandsigh.mounts;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MountWhistleSoundTest {
	@Test
	void whistleUsesOnlyParrotIdleFour() throws Exception {
		JsonObject sounds;
		try (var reader = new InputStreamReader(
			Objects.requireNonNull(getClass().getResourceAsStream("/assets/belandsigh/sounds.json")),
			StandardCharsets.UTF_8)) {
			sounds = JsonParser.parseReader(reader).getAsJsonObject();
		}

		var entries = sounds.getAsJsonObject("mount_whistle").getAsJsonArray("sounds");
		assertEquals(1, entries.size());
		assertEquals("minecraft:mob/parrot/idle4",
			entries.get(0).getAsJsonObject().get("name").getAsString());
		assertEquals("subtitles.belandsigh.mount_whistle",
			sounds.getAsJsonObject("mount_whistle").get("subtitle").getAsString());
	}
}
