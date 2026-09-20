package dev.belandsigh.death;

/** Coordinates the configurable despawn timer for items dropped on player death. */
public final class DeathDropModule {
	private static final DeathDropConfig CONFIG = DeathDropConfig.load();

	private DeathDropModule() {
	}

	public static void initialize() {
		DeathLocationModule.initialize();
	}

	public static int despawnTicks() {
		return CONFIG.despawnTicks();
	}
}
