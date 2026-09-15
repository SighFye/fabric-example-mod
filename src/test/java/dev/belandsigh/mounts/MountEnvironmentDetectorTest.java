package dev.belandsigh.mounts;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MountEnvironmentDetectorTest {
	@Test
	void standingInWaterUsesWater() {
		assertEquals(MountCategory.WATER, MountEnvironmentDetector.choose(false, true, false, false));
	}

	@Test
	void standingInLavaUsesLava() {
		assertEquals(MountCategory.LAVA, MountEnvironmentDetector.choose(true, false, false, false));
	}

	@Test
	void adjacentWaterUsesWater() {
		assertEquals(MountCategory.WATER, MountEnvironmentDetector.choose(false, false, false, true));
	}

	@Test
	void adjacentLavaUsesLava() {
		assertEquals(MountCategory.LAVA, MountEnvironmentDetector.choose(false, false, true, false));
	}

	@Test
	void adjacentLavaWinsOverAdjacentWater() {
		assertEquals(MountCategory.LAVA, MountEnvironmentDetector.choose(false, false, true, true));
	}

	@Test
	void standingWaterWinsOverAdjacentLava() {
		assertEquals(MountCategory.WATER, MountEnvironmentDetector.choose(false, true, true, false));
	}

	@Test
	void standingLavaWinsOverAdjacentWater() {
		assertEquals(MountCategory.LAVA, MountEnvironmentDetector.choose(true, false, false, true));
	}

	@Test
	void noFluidUsesLand() {
		assertEquals(MountCategory.LAND, MountEnvironmentDetector.choose(false, false, false, false));
	}

	@Test
	void waterSurfaceOneBlockBelowFeetCountsAsAdjacent() {
		assertEquals(MountCategory.WATER,
			MountEnvironmentDetector.chooseFromAdjacentLayers(false, false, false, false, false, true));
	}

	@Test
	void lavaSurfaceOneBlockBelowFeetCountsAsAdjacent() {
		assertEquals(MountCategory.LAVA,
			MountEnvironmentDetector.chooseFromAdjacentLayers(false, false, false, false, true, false));
	}
}
