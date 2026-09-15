package dev.belandsigh.mounts;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MountWhistleAdvancementTest {
	@Test
	void obtainingWhistleAwardsIntegratedAdventureAdvancement() throws Exception {
		JsonObject advancement;
		try (var reader = new InputStreamReader(
			Objects.requireNonNull(getClass().getResourceAsStream(
				"/data/belandsigh/advancement/mount_whistle.json")),
			StandardCharsets.UTF_8)) {
			advancement = JsonParser.parseReader(reader).getAsJsonObject();
		}

		assertEquals("minecraft:adventure/root", advancement.get("parent").getAsString());
		assertEquals("belandsigh:mount_whistle",
			advancement.getAsJsonObject("display").getAsJsonObject("icon").get("id").getAsString());
		assertFalse(advancement.getAsJsonObject("display").get("announce_to_chat").getAsBoolean());
		JsonObject criterion = advancement.getAsJsonObject("criteria").getAsJsonObject("has_mount_whistle");
		assertEquals("minecraft:inventory_changed", criterion.get("trigger").getAsString());
		assertEquals("belandsigh:mount_whistle", criterion.getAsJsonObject("conditions")
			.getAsJsonArray("items").get(0).getAsJsonObject().get("items").getAsString());
	}
}
