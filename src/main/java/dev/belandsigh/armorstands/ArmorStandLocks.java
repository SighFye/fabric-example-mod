package dev.belandsigh.armorstands;

/** Pure lock rules, kept free of Minecraft state so they can be unit tested without a bootstrap. */
final class ArmorStandLocks {
	private ArmorStandLocks() {
	}

	static boolean isLockedAgainst(boolean locked, boolean owner, boolean bypass) {
		return locked && !owner && !bypass;
	}

	static boolean canEdit(boolean validTarget, boolean lockedByOther) {
		return validTarget && !lockedByOther;
	}
}
