package dev.belandsigh.customportals;

import dev.belandsigh.BelAndSighMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
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

import java.util.HashMap;
import java.util.Map;

public final class CustomNetherPortalModule {
	public static final TagKey<Block> FRAME_BLOCKS = TagKey.create(
			Registries.BLOCK,
			Identifier.fromNamespaceAndPath(BelAndSighMod.MOD_ID, "nether_portal_frame_blocks")
	);

	/** How long a chunk that just failed a portal-shape search is skipped, so spreading fire can't
	 *  repeatedly re-trigger the expensive flood-fill search across the same open area. */
	private static final int RETRY_COOLDOWN_TICKS = 20;
	private static final int STALE_ENTRY_SWEEP_THRESHOLD = 512;
	private static final Map<ChunkKey, Long> RECENT_FAILURES = new HashMap<>();

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

		ChunkKey chunk = ChunkKey.of(level.dimension(), ignitionPos);
		long now = level.getGameTime();
		Long lastFailure = RECENT_FAILURES.get(chunk);
		if (lastFailure != null && now - lastFailure < RETRY_COOLDOWN_TICKS) {
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
			RECENT_FAILURES.remove(chunk);
			return true;
		}

		recordFailure(chunk, now);
		return false;
	}

	private static void recordFailure(ChunkKey chunk, long now) {
		if (RECENT_FAILURES.size() >= STALE_ENTRY_SWEEP_THRESHOLD) {
			RECENT_FAILURES.entrySet().removeIf(entry -> now - entry.getValue() >= RETRY_COOLDOWN_TICKS);
		}
		RECENT_FAILURES.put(chunk, now);
	}

	private record ChunkKey(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
		static ChunkKey of(ResourceKey<Level> dimension, BlockPos pos) {
			return new ChunkKey(dimension, pos.getX() >> 4, pos.getZ() >> 4);
		}
	}

	public static boolean isCompleteIrregularPortal(LevelReader level, BlockPos portalPos, Direction.Axis axis) {
		return CustomPortalShape.isComplete(level, portalPos, axis);
	}
}
