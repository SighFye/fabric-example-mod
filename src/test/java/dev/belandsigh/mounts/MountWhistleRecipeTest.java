package dev.belandsigh.mounts;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MountWhistleRecipeTest {
	@Test
	void shapedRecipeUsesTheExactWhistlePatternAndIngredients() throws IOException {
		JsonObject recipe;
		try (var reader = new InputStreamReader(
			Objects.requireNonNull(getClass().getResourceAsStream(
				"/data/belandsigh/recipe/mount_whistle.json")),
			StandardCharsets.UTF_8)) {
			recipe = JsonParser.parseReader(reader).getAsJsonObject();
		}

		assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
		assertEquals(" GG", recipe.getAsJsonArray("pattern").get(0).getAsString());
		assertEquals(" EG", recipe.getAsJsonArray("pattern").get(1).getAsString());
		assertEquals("A  ", recipe.getAsJsonArray("pattern").get(2).getAsString());

		JsonObject key = recipe.getAsJsonObject("key");
		assertEquals("minecraft:gold_ingot", key.get("G").getAsString());
		assertEquals("minecraft:echo_shard", key.get("E").getAsString());
		assertEquals("minecraft:amethyst_shard", key.get("A").getAsString());
		assertEquals("belandsigh:mount_whistle",
			recipe.getAsJsonObject("result").get("id").getAsString());
	}
}
