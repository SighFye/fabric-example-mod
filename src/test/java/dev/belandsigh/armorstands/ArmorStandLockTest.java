package dev.belandsigh.armorstands;

import org.junit.jupiter.api.Test;

import static dev.belandsigh.armorstands.ArmorStandLocks.canEdit;
import static dev.belandsigh.armorstands.ArmorStandLocks.isLockedAgainst;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArmorStandLockTest {
	@Test
	void unlockedStandIsOpenToEveryone() {
		assertFalse(isLockedAgainst(false, false, false));
	}

	@Test
	void lockedStandRejectsOtherPlayers() {
		assertTrue(isLockedAgainst(true, false, false));
	}

	@Test
	void ownerCanEditTheirLockedStand() {
		assertFalse(isLockedAgainst(true, true, false));
	}

	@Test
	void operatorBypassesLock() {
		assertFalse(isLockedAgainst(true, false, true));
	}

	@Test
	void openEditorClosesOnceAnotherPlayerLocksTheStand() {
		// Both editors start valid; after the other player locks it, the same check must fail.
		assertTrue(canEdit(true, isLockedAgainst(false, false, false)));
		assertFalse(canEdit(true, isLockedAgainst(true, false, false)));
	}

	@Test
	void invalidTargetCannotBeEdited() {
		assertFalse(canEdit(false, false));
	}
}
