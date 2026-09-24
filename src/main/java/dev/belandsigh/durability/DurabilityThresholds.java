package dev.belandsigh.durability;

/** Pure alert rules, kept free of Minecraft state so they can be unit tested. */
final class DurabilityThresholds {
	static final int COOLDOWN_TICKS = 60;
	/** Absolute remaining-durability counts that trigger a warning when crossed. */
	private static final int[] THRESHOLDS = {100, 50, 25, 10};

	private DurabilityThresholds() {
	}

	/** True when a loss from {@code previousRemaining} to {@code remaining} passes a threshold without breaking. */
	static boolean crossed(int previousRemaining, int remaining) {
		if (remaining >= previousRemaining || remaining < 1) {
			return false;
		}
		for (int threshold : THRESHOLDS) {
			if (previousRemaining > threshold && remaining <= threshold) {
				return true;
			}
		}
		return false;
	}

	static boolean offCooldown(long now, long lastPing) {
		return now - lastPing >= COOLDOWN_TICKS;
	}
}
