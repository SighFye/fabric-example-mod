package dev.belandsigh.client;

import dev.belandsigh.config.SettingsNetwork.StatePayload;
import dev.belandsigh.config.Feature;
import dev.belandsigh.config.ModSettings;

public final class SettingsClientState {
	private static StatePayload state;

	private SettingsClientState() {
	}

	public static void update(StatePayload newState) {
		state = newState;
		BelAndSighSettingsScreen.updateCurrent(newState);
	}

	public static StatePayload state() {
		return state;
	}

	public static boolean enabled(Feature feature) {
		return state == null ? ModSettings.enabled(feature) : state.enabled(feature);
	}

	public static void reset() {
		state = null;
		BelAndSighSettingsScreen.updateCurrent(null);
	}
}
