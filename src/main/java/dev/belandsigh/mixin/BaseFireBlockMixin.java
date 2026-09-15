package dev.belandsigh.mixin;

import dev.belandsigh.customportals.CustomNetherPortalModule;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BaseFireBlock.class)
public abstract class BaseFireBlockMixin {
	@Inject(method = "onPlace", at = @At("TAIL"))
	private void belandsigh$createIrregularPortal(
			BlockState state,
			Level level,
			BlockPos pos,
			BlockState oldState,
			boolean movedByPiston,
			CallbackInfo ci
	) {
		CustomNetherPortalModule.tryCreateIrregularPortal(level, pos);
	}

	@Redirect(
			method = "isPortal",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/block/state/BlockState;is(Ljava/lang/Object;)Z"
			)
	)
	private static boolean belandsigh$recognizeTaggedPortalFrame(BlockState state, Object vanillaFrameBlock) {
		return state.is(CustomNetherPortalModule.FRAME_BLOCKS);
	}
}
