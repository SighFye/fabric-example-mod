package dev.belandsigh.leaves;

import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Decays unsupported natural leaves from a per-level queue with a fixed per-tick budget, so felling a forest
 * spreads the work over several ticks instead of flooding the block scheduler.
 */
public final class FastLeafDecayModule {
	static final int DECAYS_PER_TICK = 64;
	/** Each leaf waits a random 1..MAX_DELAY_TICKS (average ~13) so canopies thin out one leaf at a time. */
	static final int MAX_DELAY_TICKS = 25;
	private static final Map<ServerLevel, LeafDecayQueue> QUEUES = new HashMap<>();

	private FastLeafDecayModule() {
	}

	public static void initialize() {
		ServerTickEvents.END_LEVEL_TICK.register(FastLeafDecayModule::processQueue);
		ServerLevelEvents.UNLOAD.register((server, level) -> QUEUES.remove(level));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> QUEUES.clear());
	}

	public static boolean isUnsupportedNaturalLeaf(BlockState state) {
		return state.getBlock() instanceof LeavesBlock
				&& !state.getValue(LeavesBlock.PERSISTENT)
				&& state.getValue(LeavesBlock.DISTANCE) == LeavesBlock.DECAY_DISTANCE;
	}

	public static void enqueue(ServerLevel level, BlockPos pos) {
		long dueTick = level.getGameTime() + 1 + level.getRandom().nextInt(MAX_DELAY_TICKS);
		QUEUES.computeIfAbsent(level, ignored -> new LeafDecayQueue()).add(pos.asLong(), dueTick);
	}

	private static void processQueue(ServerLevel level) {
		LeafDecayQueue queue = QUEUES.get(level);
		if (queue == null) return;

		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		queue.drain(level.getGameTime(), DECAYS_PER_TICK, packed -> {
			pos.set(packed);
			// Skip leaves in unloaded chunks; vanilla random ticks will finish them once loaded.
			if (!level.isLoaded(pos)) return;
			BlockState state = level.getBlockState(pos);
			// Re-check: the leaf may have been reconnected to a log, broken, or replaced since queueing.
			if (isUnsupportedNaturalLeaf(state)) {
				// Vanilla random-tick decay keeps loot tables and the doTileDrops gamerule intact.
				state.randomTick(level, pos, level.getRandom());
			}
		});
		if (queue.isEmpty()) QUEUES.remove(level);
	}
}
