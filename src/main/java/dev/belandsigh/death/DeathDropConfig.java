package dev.belandsigh.death;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.belandsigh.BelAndSighMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Configurable despawn timer for items dropped on player death, stored as JSON in the config directory. */
final class DeathDropConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("belandsigh-death-drops.json");
	private static final int DEFAULT_DESPAWN_SECONDS = 300;

	private int despawnSeconds = DEFAULT_DESPAWN_SECONDS;

	static DeathDropConfig load() {
		DeathDropConfig config = null;
		if (Files.exists(FILE)) {
			try (var reader = Files.newBufferedReader(FILE)) {
				config = GSON.fromJson(reader, DeathDropConfig.class);
			} catch (IOException | RuntimeException exception) {
				BelAndSighMod.LOGGER.warn("Failed to read {}, using defaults", FILE, exception);
			}
		}
		if (config == null) {
			config = new DeathDropConfig();
		}
		if (config.despawnSeconds < 1) {
			config.despawnSeconds = DEFAULT_DESPAWN_SECONDS;
		}
		config.save();
		return config;
	}

	private void save() {
		try {
			Files.createDirectories(FILE.getParent());
			Files.writeString(FILE, GSON.toJson(this));
		} catch (IOException exception) {
			BelAndSighMod.LOGGER.warn("Failed to write {}", FILE, exception);
		}
	}

	int despawnTicks() {
		return despawnSeconds * 20;
	}
}
