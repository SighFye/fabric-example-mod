package dev.belandsigh.mobheads;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.item.component.ResolvableProfile;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

	@Test
	void sheepUseOneMutuallyExclusiveDropPool() throws IOException {
		JsonObject sheep = JsonParser.parseString(Files.readString(HEAD_TABLES.resolve("sheep.json"))).getAsJsonObject();
		assertEquals(1, sheep.getAsJsonArray("pools").size());
		assertEquals("minecraft:alternatives", sheep.getAsJsonArray("pools").get(0).getAsJsonObject()
			.getAsJsonArray("entries").get(0).getAsJsonObject().get("type").getAsString());
	}

	@Test
	void everyHeadHasAStableProfileIdAndRemoteTexture() throws IOException {
		Map<String, String> textureById = new HashMap<>();
		try (Stream<Path> files = Files.walk(HEAD_TABLES)) {
			for (Path path : files.filter(file -> file.toString().endsWith(".json")).toList()) {
				List<JsonObject> profiles = new ArrayList<>();
				collectProfiles(JsonParser.parseString(Files.readString(path)), profiles);
				for (JsonObject json : profiles) {
					ResolvableProfile profile = ResolvableProfile.CODEC.parse(
						JsonOps.INSTANCE, json
					).getOrThrow();
					assertTrue(profile.partialProfile().id().getMostSignificantBits() != 0
						|| profile.partialProfile().id().getLeastSignificantBits() != 0, path.toString());

					String value = profile.partialProfile().properties().get("textures").iterator().next().value();
					JsonObject payload = JsonParser.parseString(new String(
						Base64.getDecoder().decode(value), StandardCharsets.UTF_8
					)).getAsJsonObject();
					String url = payload.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
					assertTrue(url.startsWith("http://textures.minecraft.net/texture/"), path.toString());

					String id = profile.partialProfile().id().toString();
					String previous = textureById.putIfAbsent(id, url);
					assertTrue(previous == null || previous.equals(url), "Profile ID reused by different textures: " + id);
				}
			}
		}
		assertTrue(textureById.size() > 100, "Expected the complete mob-head texture set");
	}

	private static void collectProfiles(JsonElement element, List<JsonObject> profiles) {
		if (element.isJsonArray()) {
			element.getAsJsonArray().forEach(child -> collectProfiles(child, profiles));
		} else if (element.isJsonObject()) {
			JsonObject object = element.getAsJsonObject();
			if (object.has("minecraft:profile")) {
				profiles.add(object.getAsJsonObject("minecraft:profile"));
			}
			for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
				collectProfiles(entry.getValue(), profiles);
			}
		}
	}
}
