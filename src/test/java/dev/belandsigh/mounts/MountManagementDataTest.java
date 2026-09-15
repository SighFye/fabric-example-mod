package dev.belandsigh.mounts;

import dev.belandsigh.mounts.MountNetworking.MountSummary;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MountManagementDataTest {
	@Test
	void categoryMaskSupportsMultipleCategoriesWithoutAddingOthers() {
		int mask = (1 << MountCategory.LAND.ordinal()) | (1 << MountCategory.WATER.ordinal());
		MountSummary summary = new MountSummary(UUID.randomUUID(), "Mount", "Test Mount",
			"minecraft:overworld", mask, true);

		assertTrue(summary.supports(MountCategory.LAND));
		assertTrue(summary.supports(MountCategory.WATER));
		assertFalse(summary.supports(MountCategory.LAVA));
	}
}
