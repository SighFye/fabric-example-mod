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
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

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

	static Item conversionFor(Item item) {
		return CONVERSIONS.get(item);
	}

	public static void initialize() {
		UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
			BlockState cauldron = level.getBlockState(hitResult.getBlockPos());
			if (player.isShiftKeyDown() || player.isSpectator() || !cauldron.is(Blocks.WATER_CAULDRON)) {
				return InteractionResult.PASS;
			}

			ItemStack heldStack = player.getItemInHand(hand);
			Item convertedItem = CONVERSIONS.get(heldStack.getItem());
			if (convertedItem == null) {
				return InteractionResult.PASS;
			}

			if (level instanceof ServerLevel serverLevel) {
				// transmuteCopy intentionally keeps the stack's components (custom name, lore, etc.).
				player.setItemInHand(hand, heldStack.transmuteCopy(convertedItem, heldStack.getCount()));
				// Each conversion consumes one water level, emptying the cauldron after the last.
				LayeredCauldronBlock.lowerFillLevel(cauldron, level, hitResult.getBlockPos());
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
