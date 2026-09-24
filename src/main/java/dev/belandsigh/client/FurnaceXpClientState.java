package dev.belandsigh.client;

import dev.belandsigh.furnacexp.FurnaceXpModule.StoredXpPayload;

/** Last stored-XP value the server reported for the furnace menu the player has open. */
public final class FurnaceXpClientState {
	private static int containerId = -1;
	private static int tenths;

	private FurnaceXpClientState() {
	}

	public static void update(StoredXpPayload payload) {
		containerId = payload.containerId();
		tenths = payload.tenths();
	}

	/** Returns the stored tenths for this container, or -1 if the server hasn't reported it. */
	public static int tenthsFor(int menuContainerId) {
		return menuContainerId == containerId ? tenths : -1;
	}

	/** Optimistically empties the display after a click; the server confirms with its next sync. */
	public static void clear(int menuContainerId) {
		if (menuContainerId == containerId) {
			tenths = 0;
		}
	}
}
