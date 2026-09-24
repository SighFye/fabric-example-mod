package dev.belandsigh.furnacexp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FurnaceXpMathTest {
	@Test
	void emptyFurnaceStoresNothing() {
		assertEquals(0, FurnaceXpMath.toTenths(0.0));
		assertEquals(0, FurnaceXpMath.toTenths(-1.0));
		assertEquals(0, FurnaceXpMath.toTenths(Double.NaN));
	}

	@Test
	void fractionalRecipeExperienceKeepsTenths() {
		// Seven cooked kelp at 0.1 XP each must show 0.7, not 0.6 from float error.
		assertEquals(7, FurnaceXpMath.toTenths(FurnaceXpMath.experienceFor(7, 0.1F)));
		// Iron ingots at 0.7 XP each.
		assertEquals(49, FurnaceXpMath.toTenths(FurnaceXpMath.experienceFor(7, 0.7F)));
	}

	@Test
	void displayNeverRoundsUp() {
		assertEquals(12, FurnaceXpMath.toTenths(1.29));
	}

	@Test
	void hugeCountsClampInsteadOfOverflowing() {
		double huge = FurnaceXpMath.experienceFor(Integer.MAX_VALUE, 1.0F);
		assertEquals(Integer.MAX_VALUE, FurnaceXpMath.toTenths(huge));
		assertEquals("99999+", FurnaceXpMath.format(Integer.MAX_VALUE));
	}

	@Test
	void formatShowsDecimalOnlyForSmallAmounts() {
		assertEquals("0.0", FurnaceXpMath.format(0));
		assertEquals("0.7", FurnaceXpMath.format(7));
		assertEquals("9.9", FurnaceXpMath.format(99));
		assertEquals("10", FurnaceXpMath.format(100));
		assertEquals("37", FurnaceXpMath.format(375));
	}

	@Test
	void levelsGainedFollowsVanillaCurve() {
		// Level 0 needs 7 points, level 1 needs 9: 16 points is exactly two levels.
		assertEquals(2.0, FurnaceXpMath.levelsGained(0, 0.0F, 16.0), 1.0E-9);
		// Halfway through level 0 (3.5 of 7), another 3.5 finishes the level.
		assertEquals(0.5, FurnaceXpMath.levelsGained(0, 0.5F, 3.5), 1.0E-9);
		// Level 30 needs 112 points.
		assertEquals(0.5, FurnaceXpMath.levelsGained(30, 0.0F, 56.0), 1.0E-9);
		assertEquals(0.0, FurnaceXpMath.levelsGained(5, 0.2F, 0.0), 1.0E-9);
	}
}
