package dev.belandsigh.mixin;

import dev.belandsigh.leaves.FastLeafDecayModule;
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
	@Inject(method = "tick", at = @At("TAIL"))
	private void belandsigh$queueDecay(
			BlockState state,
			ServerLevel level,
			BlockPos pos,
			RandomSource random,
			CallbackInfo ci
	) {
		// Vanilla's tick has just recalculated distance, so read the updated state rather than the argument.
		if (FastLeafDecayModule.isUnsupportedNaturalLeaf(level.getBlockState(pos))) {
			FastLeafDecayModule.enqueue(level, pos);
		}
	}
}
