package dev.belandsigh.mounts;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static dev.belandsigh.mounts.MountRecallService.Validation.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MountRecallServiceTest {
	@Test
	void otherRiderRejectsRecall() {
		assertEquals(OCCUPIED, validate(true, true, true, true, false, true, true));
	}

	@Test
	void anotherDimensionRejectsRecall() {
		assertEquals(WRONG_DIMENSION, validate(true, true, true, false, false, false, true));
	}

	@Test
	void ownershipMismatchRejectsRecall() {
		assertEquals(NOT_OWNED, validate(true, false, true, true, false, false, true));
	}

	@Test
	void incompatibleCategoryRejectsRecall() {
		assertEquals(INCOMPATIBLE, validate(true, true, false, true, false, false, true));
	}

	@Test
	void deadMountRejectsRecall() {
		assertEquals(DEAD, validate(false, true, true, true, false, false, true));
	}

	@Test
	void callingWhileAlreadyRidingDoesNothing() {
		assertEquals(ALREADY_RIDING, validate(true, true, true, true, true, false, true));
	}

	@Test
	void missingSafePositionRejectsRecall() {
		assertEquals(NO_SAFE_POSITION, validate(true, true, true, true, false, false, false));
	}

	@Test
	void validRecallIsReady() {
		assertEquals(READY, validate(true, true, true, true, false, false, true));
	}

	@Test
	void targetedWaterOnlyMountStaysInWaterOnLand() {
		assertEquals(MountCategory.WATER, MountRecallService.chooseTargetCategory(
			EnumSet.of(MountCategory.WATER), MountCategory.LAND));
	}

	@Test
	void targetedLavaOnlyMountStaysInLavaOnLand() {
		assertEquals(MountCategory.LAVA, MountRecallService.chooseTargetCategory(
			EnumSet.of(MountCategory.LAVA), MountCategory.LAND));
	}

	@Test
	void multiCategoryTargetUsesCurrentEnvironment() {
		assertEquals(MountCategory.WATER, MountRecallService.chooseTargetCategory(
			EnumSet.of(MountCategory.LAND, MountCategory.WATER), MountCategory.WATER));
	}

	private static MountRecallService.Validation validate(boolean alive, boolean owned, boolean compatible,
			boolean sameDimension, boolean callingPlayerRiding, boolean occupiedByOther, boolean safePosition) {
		return MountRecallService.validateFacts(alive, owned, compatible, sameDimension,
			callingPlayerRiding, occupiedByOther, safePosition);
	}
}
