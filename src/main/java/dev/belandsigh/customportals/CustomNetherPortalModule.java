package dev.belandsigh.customportals;

import dev.belandsigh.BelAndSighMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public final class CustomNetherPortalModule {
	public static final TagKey<Block> FRAME_BLOCKS = TagKey.create(
			Registries.BLOCK,
			Identifier.fromNamespaceAndPath(BelAndSighMod.MOD_ID, "nether_portal_frame_blocks")
	);

	private CustomNetherPortalModule() {
	}

	public static void initialize() {
		// Portal behavior is supplied by the PortalShape and BaseFireBlock mixins.
	}

	public static boolean tryCreateIrregularPortal(Level level, BlockPos ignitionPos) {
		if (level.isClientSide()
				|| level.dimension() != Level.OVERWORLD && level.dimension() != Level.NETHER
				|| !level.getBlockState(ignitionPos).is(BlockTags.FIRE)) {
			return false;
		}

		for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
			var shape = CustomPortalShape.findEmpty(level, ignitionPos, axis);
			if (shape.isEmpty()) {
				continue;
			}

			BlockState portalState = Blocks.NETHER_PORTAL.defaultBlockState()
					.setValue(NetherPortalBlock.AXIS, shape.get().axis());
			for (BlockPos pos : shape.get().interior()) {
				level.setBlock(pos, portalState, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
			}
			return true;
		}
		return false;
	}

	public static boolean isCompleteIrregularPortal(LevelReader level, BlockPos portalPos, Direction.Axis axis) {
		return CustomPortalShape.isComplete(level, portalPos, axis);
	}
}
