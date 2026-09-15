package dev.belandsigh.cauldrons;

import java.util.HashMap;
import java.util.Map;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public final class CauldronConversionModule {
	private static final Map<Item, Item> CONVERSIONS = createConversions();

	private CauldronConversionModule() {
	}

	private static Map<Item, Item> createConversions() {
		Map<Item, Item> conversions = new HashMap<>();
		for (DyeColor color : DyeColor.VALUES) {
			conversions.put(Items.CONCRETE_POWDER.pick(color), Items.CONCRETE.pick(color));
		}
		conversions.put(Items.DIRT, Items.MUD);
		conversions.put(Items.COARSE_DIRT, Items.MUD);
		conversions.put(Items.ROOTED_DIRT, Items.MUD);
		return Map.copyOf(conversions);
	}

	public static void initialize() {
		UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
			if (player.isShiftKeyDown() || player.isSpectator()
					|| !level.getBlockState(hitResult.getBlockPos()).is(Blocks.WATER_CAULDRON)) {
				return InteractionResult.PASS;
			}

			ItemStack heldStack = player.getItemInHand(hand);
			Item convertedItem = CONVERSIONS.get(heldStack.getItem());
			if (convertedItem == null) {
				return InteractionResult.PASS;
			}

			if (level instanceof ServerLevel serverLevel) {
				player.setItemInHand(hand, heldStack.transmuteCopy(convertedItem, heldStack.getCount()));
				serverLevel.playSound(null, hitResult.getBlockPos(), SoundEvents.GENERIC_SPLASH,
						SoundSource.BLOCKS, 1.0F, 1.0F);
				serverLevel.sendParticles(ParticleTypes.SPLASH,
						hitResult.getLocation().x(), hitResult.getLocation().y() + 0.2D, hitResult.getLocation().z(),
						8, 0.25D, 0.1D, 0.25D, 0.05D);
			}

			return InteractionResult.SUCCESS;
		});
	}
}
