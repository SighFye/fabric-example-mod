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

	/** Forgets the last server's value, so container ids reused on another server can't show a stale button. */
	public static void reset() {
		containerId = -1;
		tenths = 0;
	}
}
