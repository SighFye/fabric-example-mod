package dev.belandsigh.mounts;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MountOwnershipServiceTest {
	private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID OTHER_PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000002");

	@Test
	void nativeOwnerIsRecognised() {
		assertTrue(MountOwnershipService.isOwnedBy(OWNER, OWNER, null, true));
	}

	@Test
	void anotherPlayerIsNotNativeOwner() {
		assertFalse(MountOwnershipService.isOwnedBy(OTHER_PLAYER, OWNER, null, true));
	}

	@Test
	void boundOwnerIsRecognised() {
		assertTrue(MountOwnershipService.isOwnedBy(OWNER, null, OWNER, false));
	}

	@Test
	void differentPlayerIsNotBoundOwner() {
		assertFalse(MountOwnershipService.isOwnedBy(OTHER_PLAYER, null, OWNER, false));
	}

	@Test
	void nativeOwnershipSystemDoesNotFallBackToBinding() {
		assertFalse(MountOwnershipService.isOwnedBy(OWNER, null, OWNER, true));
	}
}
