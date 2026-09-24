package dev.belandsigh.customportals;

import dev.belandsigh.BelAndSighMod;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
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

	/** Portal positions already found to be part of a broken irregular portal this tick. Breaking one frame block
	 *  makes every portal block re-validate as the removal cascades; this keeps that to roughly one flood fill. */
	private static final LongOpenHashSet BROKEN_THIS_TICK = new LongOpenHashSet();
	private static ServerLevel brokenLevel;

	private CustomNetherPortalModule() {
	}

	public static void initialize() {
		// Portal behavior is supplied by the PortalShape, BaseFireBlock and NetherPortalBlock mixins.
		ServerTickEvents.END_SERVER_TICK.register(server -> clearBrokenCache());
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> clearBrokenCache());
	}

	public static boolean tryCreateIrregularPortal(Level level, BlockPos ignitionPos) {
		if (level.isClientSide()
				|| level.dimension() != Level.OVERWORLD && level.dimension() != Level.NETHER
				|| !level.getBlockState(ignitionPos).is(BlockTags.FIRE)) {
			return false;
		}

		var shape = CustomPortalShape.findEmpty(CustomPortalShape.cells(level), ignitionPos);
		if (shape.isEmpty()) {
			return false;
		}

		BlockState portalState = Blocks.NETHER_PORTAL.defaultBlockState()
				.setValue(NetherPortalBlock.AXIS, shape.get().axis());
		for (BlockPos pos : shape.get().interior()) {
			level.setBlock(pos, portalState, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
		}
		return true;
	}

	public static boolean isCompleteIrregularPortal(LevelReader level, BlockPos portalPos, Direction.Axis axis) {
		// Only the server thread's own levels share the cache; client and world-gen readers search directly.
		if (!(level instanceof ServerLevel serverLevel)) {
			return CustomPortalShape.isComplete(CustomPortalShape.cells(level), portalPos, axis, null);
		}
		if (brokenLevel != serverLevel) {
			BROKEN_THIS_TICK.clear();
			brokenLevel = serverLevel;
		}
		if (BROKEN_THIS_TICK.contains(portalPos.asLong())) {
			return false;
		}
		return CustomPortalShape.isComplete(CustomPortalShape.cells(level), portalPos, axis, BROKEN_THIS_TICK);
	}

	private static void clearBrokenCache() {
		if (!BROKEN_THIS_TICK.isEmpty()) BROKEN_THIS_TICK.clear();
		brokenLevel = null;
	}
}
