package dev.belandsigh.furnacexp;

/**
 * Pure arithmetic for Furnace XP, kept free of Minecraft types so it can be unit tested.
 * Amounts are passed around as tenths of an XP point so fractional recipe experience
 * (e.g. 0.1 per cooked kelp) survives the trip to the client as an integer.
 */
public final class FurnaceXpMath {
	/** Caps the label so it always fits in the button below the result slot. */
	public static final int MAX_DISPLAY_TENTHS = 999_990;

	private FurnaceXpMath() {
	}

	/** Matches vanilla's {@code count * experience} float product used when popping furnace XP. */
	public static float experienceFor(int count, float experiencePerRecipe) {
		return count * experiencePerRecipe;
	}

	/** Rounds down to whole tenths so the display never promises more than vanilla can award. */
	public static int toTenths(double experience) {
		if (!(experience > 0.0)) {
			return 0;
		}
		// The epsilon absorbs float error such as 0.7F * 10 = 6.9999...
		double tenths = Math.floor(experience * 10.0 + 1.0E-4);
		return tenths >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) tenths;
	}

	/** Formats a tenths amount: one decimal below 10 XP, whole points above, capped for width. */
	public static String format(int tenths) {
		if (tenths >= MAX_DISPLAY_TENTHS) {
			return (MAX_DISPLAY_TENTHS / 10) + "+";
		}
		if (tenths < 100) {
			return (tenths / 10) + "." + (tenths % 10);
		}
		return Integer.toString(tenths / 10);
	}

	/** Vanilla's per-level cost, mirroring {@code Player#getXpNeededForNextLevel}. */
	public static int xpNeededForNextLevel(int level) {
		if (level >= 30) {
			return 112 + (level - 30) * 9;
		}
		if (level >= 15) {
			return 37 + (level - 15) * 5;
		}
		return 7 + level * 2;
	}

	/**
	 * How many levels (fractional) a player at {@code level} with {@code progress} towards the
	 * next level would gain from {@code points} of experience.
	 */
	public static double levelsGained(int level, float progress, double points) {
		if (!(points > 0.0)) {
			return 0.0;
		}
		double remaining = points;
		int currentLevel = Math.max(0, level);
		double currentProgress = Math.clamp(progress, 0.0F, 1.0F);
		double gained = 0.0;
		while (remaining > 0.0) {
			int needed = xpNeededForNextLevel(currentLevel);
			double toNext = needed * (1.0 - currentProgress);
			if (remaining < toNext) {
				gained += remaining / needed;
				break;
			}
			remaining -= toNext;
			gained += 1.0 - currentProgress;
			currentProgress = 0.0;
			currentLevel++;
		}
		return gained;
	}
}
