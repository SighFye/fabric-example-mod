package dev.belandsigh.cauldrons;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static dev.belandsigh.cauldrons.CauldronConversionModule.conversionFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CauldronConversionModuleTest {
	@BeforeAll
	static void bootstrap() {
		net.minecraft.SharedConstants.tryDetectVersion();
		net.minecraft.server.Bootstrap.bootStrap();
	}

	@Test
	void everyConcretePowderHardensToMatchingConcrete() {
		for (DyeColor color : DyeColor.VALUES) {
			assertEquals(Items.CONCRETE.pick(color), conversionFor(Items.CONCRETE_POWDER.pick(color)), color.getName());
		}
	}

	@Test
	void dirtVariantsBecomeMud() {
		assertEquals(Items.MUD, conversionFor(Items.DIRT));
		assertEquals(Items.MUD, conversionFor(Items.COARSE_DIRT));
		assertEquals(Items.MUD, conversionFor(Items.ROOTED_DIRT));
	}

	@Test
	void unrelatedItemsAreNotConverted() {
		assertNull(conversionFor(Items.SAND));
		assertNull(conversionFor(Items.MUD));
		assertNull(conversionFor(Items.CONCRETE.pick(DyeColor.WHITE)));
	}
}
