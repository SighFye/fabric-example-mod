package dev.belandsigh.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LeavesBlock.class)
public abstract class LeavesBlockMixin {
	private static final float BELANDSIGH_DECAY_CHANCE = 0.075F;

	@Inject(method = "tick", at = @At("TAIL"))
	private void belandsigh$accelerateDecay(
			BlockState state,
			ServerLevel level,
			BlockPos pos,
			RandomSource random,
			CallbackInfo ci
	) {
		BlockState currentState = level.getBlockState(pos);
		if (!(currentState.getBlock() instanceof LeavesBlock)
				|| currentState.getValue(LeavesBlock.PERSISTENT)
				|| currentState.getValue(LeavesBlock.DISTANCE) != LeavesBlock.DECAY_DISTANCE) {
			return;
		}

		if (random.nextFloat() < BELANDSIGH_DECAY_CHANCE) {
			currentState.randomTick(level, pos, random);
		} else {
			level.scheduleTick(pos, currentState.getBlock(), 1);
		}
	}
}
