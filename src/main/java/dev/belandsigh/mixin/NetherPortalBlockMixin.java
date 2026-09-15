package dev.belandsigh.mixin;

import dev.belandsigh.customportals.CustomNetherPortalModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NetherPortalBlock.class)
public abstract class NetherPortalBlockMixin {
	@Inject(method = "updateShape", at = @At("RETURN"), cancellable = true)
	private void belandsigh$preserveValidIrregularPortal(
			BlockState state,
			LevelReader level,
			ScheduledTickAccess ticks,
			BlockPos pos,
			Direction directionToNeighbour,
			BlockPos neighbourPos,
			BlockState neighbourState,
			RandomSource random,
			CallbackInfoReturnable<BlockState> cir
	) {
		if (cir.getReturnValue().is(Blocks.AIR)
				&& CustomNetherPortalModule.isCompleteIrregularPortal(
						level, pos, state.getValue(NetherPortalBlock.AXIS))) {
			cir.setReturnValue(state);
		}
	}
}
