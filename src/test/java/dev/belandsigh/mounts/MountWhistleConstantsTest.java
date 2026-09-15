package dev.belandsigh.mounts;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MountWhistleConstantsTest {
	@Test
	void cooldownIsThreeSeconds() {
		assertEquals(3.0F, MountWhistleConstants.USE_COOLDOWN_SECONDS);
		assertEquals(60, MountWhistleConstants.USE_COOLDOWN_TICKS);
	}

	@Test
	void arrivalEffectStaysRestrained() {
		assertEquals(8, MountWhistleConstants.ARRIVAL_PARTICLE_COUNT);
	}
}
