package dev.belandsigh.pets;

import org.junit.jupiter.api.Test;

import static dev.belandsigh.pets.PetFollowPolicy.Action.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PetFollowPolicyTest {
	private static final int SIM = 10;
	private static final double FAR = 100.0 * 100.0;

	@Test
	void sittingOrLeashedPetStays() {
		assertEquals(STAY, PetFollowPolicy.decide(false, true, true, FAR, false, 20, SIM));
	}

	@Test
	void ownerInAnotherDimensionWaits() {
		assertEquals(WAIT, PetFollowPolicy.decide(true, false, true, FAR, false, 0, SIM));
	}

	@Test
	void deadOrSpectatingOwnerWaits() {
		assertEquals(WAIT, PetFollowPolicy.decide(true, true, false, FAR, false, 20, SIM));
	}

	@Test
	void petWithinTwelveBlocksIsWithOwner() {
		assertEquals(WITH_OWNER, PetFollowPolicy.decide(true, true, true, 11.9 * 11.9, false, 0, SIM));
	}

	@Test
	void tickingPetWellInsideSimulationRangeIsLeftToVanilla() {
		assertEquals(VANILLA, PetFollowPolicy.decide(true, true, true, FAR, true, SIM - 2, SIM));
	}

	@Test
	void tickingPetAtSimulationEdgeCatchesUp() {
		assertEquals(CATCH_UP, PetFollowPolicy.decide(true, true, true, FAR, true, SIM - 1, SIM));
	}

	@Test
	void nonTickingPetCatchesUp() {
		assertEquals(CATCH_UP, PetFollowPolicy.decide(true, true, true, FAR, false, 3, SIM));
	}

	@Test
	void exactlyTwelveBlocksCatchesUpWhenNotTicking() {
		assertEquals(CATCH_UP, PetFollowPolicy.decide(true, true, true, 144.0, false, 1, SIM));
	}
}
