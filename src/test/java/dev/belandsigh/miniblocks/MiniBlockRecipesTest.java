package dev.belandsigh.miniblocks;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the hand-maintained mini block recipes against ingredient, texture and naming drift. */
class MiniBlockRecipesTest {
	private static final Path RECIPES = Path.of("src/main/resources/data/mini_blocks/recipe");
	private static List<Recipe> recipes;

	private record Recipe(String file, JsonObject json, String ingredient, String texture, String name) {
	}

	@BeforeAll
	static void load() throws IOException {
		net.minecraft.SharedConstants.tryDetectVersion();
		net.minecraft.server.Bootstrap.bootStrap();

		recipes = new ArrayList<>();
		try (Stream<Path> files = Files.list(RECIPES)) {
			for (Path path : files.sorted().toList()) {
				JsonObject json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
				JsonObject components = json.getAsJsonObject("result").getAsJsonObject("components");
				String texture = components.getAsJsonObject("minecraft:profile").getAsJsonArray("properties")
					.get(0).getAsJsonObject().get("value").getAsString();
				recipes.add(new Recipe(path.getFileName().toString(), json, json.get("ingredient").getAsString(),
					texture, components.get("minecraft:item_name").getAsString()));
			}
		}
	}

	@Test
	void everyRecipeIsAVanillaStonecutterHead() {
		for (Recipe recipe : recipes) {
			JsonObject result = recipe.json().getAsJsonObject("result");
			assertEquals("minecraft:stonecutting", recipe.json().get("type").getAsString(), recipe.file());
			assertEquals("minecraft:player_head", result.get("id").getAsString(), recipe.file());
			assertTrue(result.get("count").getAsInt() > 0, recipe.file());
		}
	}

	@Test
	void everyIngredientIsARealItem() {
		List<String> missing = recipes.stream()
			.filter(recipe -> !BuiltInRegistries.ITEM.containsKey(Identifier.parse(recipe.ingredient())))
			.map(recipe -> recipe.file() + " -> " + recipe.ingredient())
			.toList();
		assertTrue(missing.isEmpty(), "Unknown ingredients: " + missing);
	}

	@Test
	void everyNameIsAMiniName() {
		List<String> bad = recipes.stream()
			.filter(recipe -> !recipe.name().startsWith("Mini ") || recipe.name().isBlank())
			.map(recipe -> recipe.file() + " -> " + recipe.name())
			.toList();
		assertTrue(bad.isEmpty(), "Bad names: " + bad);
	}

	@Test
	void texturesAreUnique() {
		assertNoDuplicates("texture", Recipe::texture);
	}

	@Test
	void namesAreUnique() {
		assertNoDuplicates("name", Recipe::name);
	}

	@Test
	void ingredientsMapToOneResultUnlessIntentional() {
		assertNoDuplicates("ingredient", Recipe::ingredient, LIT_AND_UNLIT_CHOICES);
	}

	/** Blocks that intentionally offer both a lit and an unlit mini block in the stonecutter. */
	private static final Set<String> LIT_AND_UNLIT_CHOICES = Set.of(
		"minecraft:copper_bulb", "minecraft:exposed_copper_bulb", "minecraft:weathered_copper_bulb",
		"minecraft:oxidized_copper_bulb", "minecraft:redstone_lamp"
	);

	private static void assertNoDuplicates(String what, Function<Recipe, String> key) {
		assertNoDuplicates(what, key, Set.of());
	}

	private static void assertNoDuplicates(String what, Function<Recipe, String> key, Set<String> allowed) {
		Map<String, List<String>> duplicates = recipes.stream()
			.collect(Collectors.groupingBy(key, TreeMap::new, Collectors.mapping(Recipe::file, Collectors.toList())));
		duplicates.entrySet().removeIf(entry -> entry.getValue().size() < 2
			|| allowed.contains(entry.getKey()) && entry.getValue().size() == 2);
		assertTrue(duplicates.isEmpty(), "Duplicate " + what + "s: " + duplicates.values());
	}
}
