package dev.belandsigh.mounts;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TargetedMountResolverTest {
	@Test
	void rangeCoversFarCornerOfServerViewDistance() {
		double range = TargetedMountResolver.rangeForViewDistance(10);

		assertEquals(249.0D, range);
		assertTrue(range >= Math.hypot(11 * 16, 11 * 16));
	}

	@Test
	void minimumViewDistanceStillHasFullChunkCoverage() {
		assertEquals(68.0D, TargetedMountResolver.rangeForViewDistance(2));
	}
}
