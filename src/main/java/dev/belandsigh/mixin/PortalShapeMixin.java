package dev.belandsigh.mixin;

import dev.belandsigh.customportals.CustomNetherPortalModule;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PortalShape.class)
public abstract class PortalShapeMixin {
	@Redirect(
			method = {
					"getDistanceUntilEdgeAboveFrame",
					"hasTopFrame",
					"getDistanceUntilTop"
			},
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/block/state/BlockBehaviour$StatePredicate;test(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Z"
			)
	)
	private static boolean belandsigh$acceptTaggedPortalFrame(
			BlockBehaviour.StatePredicate originalPredicate,
			BlockState state,
			BlockGetter level,
			BlockPos pos
	) {
		return state.is(CustomNetherPortalModule.FRAME_BLOCKS);
	}
}
