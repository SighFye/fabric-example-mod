package dev.belandsigh.mounts;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MountCategoryServiceTest {
	@Test
	void landMountResolvesAsLand() {
		assertEquals(Set.of(MountCategory.LAND),
			MountCategoryService.categoriesMatching(category -> category == MountCategory.LAND));
	}

	@Test
	void waterMountResolvesAsWater() {
		assertEquals(Set.of(MountCategory.WATER),
			MountCategoryService.categoriesMatching(category -> category == MountCategory.WATER));
	}

	@Test
	void lavaMountResolvesAsLava() {
		assertEquals(Set.of(MountCategory.LAVA),
			MountCategoryService.categoriesMatching(category -> category == MountCategory.LAVA));
	}

	@Test
	void multiCategoryMountResolvesEveryMatchingCategory() {
		assertEquals(Set.of(MountCategory.LAND, MountCategory.WATER),
			MountCategoryService.categoriesMatching(category -> category != MountCategory.LAVA));
	}

	@Test
	void unsupportedEntityHasNoCategory() {
		assertTrue(MountCategoryService.categoriesMatching(category -> false).isEmpty());
	}
}
