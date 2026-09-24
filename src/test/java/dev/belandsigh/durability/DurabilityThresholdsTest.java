package dev.belandsigh.durability;

import org.junit.jupiter.api.Test;

import static dev.belandsigh.durability.DurabilityThresholds.crossed;
import static dev.belandsigh.durability.DurabilityThresholds.offCooldown;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DurabilityThresholdsTest {
	@Test
	void crossingEachThresholdWarns() {
		assertTrue(crossed(101, 100));
		assertTrue(crossed(51, 50));
		assertTrue(crossed(26, 25));
		assertTrue(crossed(11, 10));
	}

	@Test
	void lossesBetweenThresholdsAreSilent() {
		assertFalse(crossed(100, 99));
		assertFalse(crossed(49, 26));
		assertFalse(crossed(9, 8));
	}

	@Test
	void multiPointDamageThatSkipsPastAThresholdStillWarns() {
		assertTrue(crossed(103, 97));
		assertTrue(crossed(60, 5));
	}

	@Test
	void repairsAndMendingNeverWarn() {
		assertFalse(crossed(10, 11));
		assertFalse(crossed(90, 120));
		assertFalse(crossed(50, 50));
	}

	@Test
	void breakingIsNotReportedAsLowDurability() {
		assertFalse(crossed(3, 0));
	}

	@Test
	void cooldownBlocksRepeatPingsForThreeSeconds() {
		assertTrue(offCooldown(0, -DurabilityThresholds.COOLDOWN_TICKS));
		assertFalse(offCooldown(1059, 1000));
		assertTrue(offCooldown(1060, 1000));
	}
}
