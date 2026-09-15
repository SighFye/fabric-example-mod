package dev.belandsigh.mounts;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafeMountPositionFinderTest {
	@Test
	void searchesPlayerHeightBeforeNearbyVerticalLevels() {
		assertArrayEquals(new int[] {0, 1, -1, 2, -2}, SafeMountPositionFinder.verticalOffsets(2));
	}

	@Test
	void horizontalCandidatesAreOrderedByClosestDistance() {
		var offsets = SafeMountPositionFinder.horizontalOffsets(3);
		for (int i = 1; i < offsets.size(); i++) {
			assertTrue(offsets.get(i - 1).distanceSquared() <= offsets.get(i).distanceSquared());
		}
	}
}
