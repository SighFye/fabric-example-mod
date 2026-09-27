package dev.belandsigh.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.belandsigh.BelAndSighMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

public final class ModSettings {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("belandsigh.json");
	private static final Map<Feature, Boolean> ENABLED = new EnumMap<>(Feature.class);

	private ModSettings() {
	}

	public static synchronized void load() {
		resetDefaults();
		if (!Files.isRegularFile(PATH)) {
			save();
			return;
		}
		try (Reader reader = Files.newBufferedReader(PATH)) {
			JsonObject root = GSON.fromJson(reader, JsonObject.class);
			if (root == null) {
				return;
			}
			JsonObject features = root.has("features") && root.get("features").isJsonObject()
				? root.getAsJsonObject("features") : root;
			for (Feature feature : Feature.values()) {
				JsonElement value = features.get(feature.key());
				if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()) {
					ENABLED.put(feature, value.getAsBoolean());
				}
			}
		} catch (IOException | RuntimeException exception) {
			BelAndSighMod.LOGGER.error("Could not read {}. Using defaults.", PATH, exception);
		}
	}

	public static synchronized boolean enabled(Feature feature) {
		return ENABLED.getOrDefault(feature, defaultEnabled(feature));
	}

	public static synchronized void setEnabled(Feature feature, boolean enabled) {
		ENABLED.put(feature, enabled);
		save();
	}

	public static synchronized void setAll(boolean enabled) {
		for (Feature feature : Feature.values()) {
			ENABLED.put(feature, enabled);
		}
		save();
	}

	public static synchronized void restoreDefaults() {
		resetDefaults();
		save();
	}

	public static synchronized long mask() {
		long mask = 0L;
		for (Feature feature : Feature.values()) {
			if (enabled(feature)) {
				mask |= 1L << feature.ordinal();
			}
		}
		return mask;
	}

	public static boolean defaultEnabled(Feature feature) {
		return feature.defaultEnabled();
	}

	private static void resetDefaults() {
		ENABLED.clear();
		for (Feature feature : Feature.values()) {
			ENABLED.put(feature, defaultEnabled(feature));
		}
	}

	private static void save() {
		JsonObject features = new JsonObject();
		for (Feature feature : Feature.values()) {
			features.addProperty(feature.key(), enabled(feature));
		}
		JsonObject root = new JsonObject();
		root.addProperty("version", 1);
		root.add("features", features);
		try {
			Files.createDirectories(PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(PATH)) {
				GSON.toJson(root, writer);
			}
		} catch (IOException exception) {
			BelAndSighMod.LOGGER.error("Could not save {}.", PATH, exception);
		}
	}
}
