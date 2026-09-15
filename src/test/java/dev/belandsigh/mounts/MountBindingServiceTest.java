package dev.belandsigh.mounts;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static dev.belandsigh.mounts.MountBindingService.BindingDecision.ALREADY_BOUND;
import static dev.belandsigh.mounts.MountBindingService.BindingDecision.BIND;
import static dev.belandsigh.mounts.MountBindingService.BindingDecision.INELIGIBLE;
import static dev.belandsigh.mounts.MountBindingService.BindingDecision.NATIVE_OWNERSHIP;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MountBindingServiceTest {
	private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID OTHER_PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000002");

	@Test
	void unboundEligibleNonTameableMountBindsToFirstRider() {
		assertEquals(BIND, MountBindingService.decideBinding(true, false, null, OWNER));
	}

	@Test
	void boundOwnerRidingAgainLeavesOwnershipUnchanged() {
		assertEquals(ALREADY_BOUND, MountBindingService.decideBinding(true, false, OWNER, OWNER));
	}

	@Test
	void anotherPlayerCannotStealBoundMount() {
		assertEquals(ALREADY_BOUND, MountBindingService.decideBinding(true, false, OWNER, OTHER_PLAYER));
	}

	@Test
	void nativeTameableMountNeverReceivesRedundantBinding() {
		assertEquals(NATIVE_OWNERSHIP, MountBindingService.decideBinding(true, true, null, OWNER));
	}

	@Test
	void unsupportedEntityCannotBeBound() {
		assertEquals(INELIGIBLE, MountBindingService.decideBinding(false, false, null, OWNER));
	}

	@Test
	void boundOwnerCanUnbindMount() {
		assertEquals(MountBindingService.UnbindResult.SUCCESS,
			MountBindingService.validateUnbindFacts(true, false, OWNER, OWNER));
	}

	@Test
	void anotherPlayerCannotUnbindMount() {
		assertEquals(MountBindingService.UnbindResult.NOT_OWNER,
			MountBindingService.validateUnbindFacts(true, false, OWNER, OTHER_PLAYER));
	}

	@Test
	void nativeMountCannotBeUnbound() {
		assertEquals(MountBindingService.UnbindResult.NATIVE_OWNERSHIP,
			MountBindingService.validateUnbindFacts(true, true, OWNER, OWNER));
	}

	@Test
	void unsupportedEntityCannotBeUnbound() {
		assertEquals(MountBindingService.UnbindResult.INELIGIBLE,
			MountBindingService.validateUnbindFacts(false, false, OWNER, OWNER));
	}
}
