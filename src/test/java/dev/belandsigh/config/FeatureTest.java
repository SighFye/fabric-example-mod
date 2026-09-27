package dev.belandsigh.config;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeatureTest {
	@Test
	void featureKeysAreUniqueAndRoundTrip() {
		long uniqueKeys = Arrays.stream(Feature.values()).map(Feature::key).distinct().count();
		assertEquals(Feature.values().length, uniqueKeys);
		for (Feature feature : Feature.values()) {
			assertEquals(feature, Feature.fromKey(feature.key()));
		}
	}

	@Test
	void mobHeadsDefaultOffWhileOtherFeaturesDefaultOn() {
		assertFalse(Feature.MORE_MOB_HEADS.defaultEnabled());
		for (Feature feature : Feature.values()) {
			if (feature != Feature.MORE_MOB_HEADS) {
				assertTrue(feature.defaultEnabled(), feature.key());
			}
		}
	}
}
