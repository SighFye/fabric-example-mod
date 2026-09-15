package dev.belandsigh.customportals;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;

final class CustomPortalShape {
	private static final int MIN_FRAME_BLOCKS = 10;
	private static final int MAX_FRAME_BLOCKS = 84;
	private static final int MAX_INTERIOR_BLOCKS = 1024;
	private static final int MAX_VISITED_BLOCKS = MAX_INTERIOR_BLOCKS + MAX_FRAME_BLOCKS;

	private final Direction.Axis axis;
	private final Set<BlockPos> interior;

	private CustomPortalShape(Direction.Axis axis, Set<BlockPos> interior) {
		this.axis = axis;
		this.interior = Set.copyOf(interior);
	}

	static Optional<CustomPortalShape> findEmpty(LevelReader level, BlockPos origin, Direction.Axis axis) {
		return find(level, origin, axis, false);
	}

	static boolean isComplete(LevelReader level, BlockPos origin, Direction.Axis axis) {
		return find(level, origin, axis, true).isPresent();
	}

	Direction.Axis axis() {
		return axis;
	}

	Set<BlockPos> interior() {
		return interior;
	}

	private static Optional<CustomPortalShape> find(
			LevelReader level,
			BlockPos origin,
			Direction.Axis axis,
			boolean existingPortal
	) {
		if (!axis.isHorizontal()) {
			return Optional.empty();
		}

		Direction[] directions = axis == Direction.Axis.X
				? new Direction[] {Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST}
				: new Direction[] {Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH};
		ArrayDeque<BlockPos> pending = new ArrayDeque<>();
		Set<BlockPos> visited = new HashSet<>();
		Set<BlockPos> interior = new LinkedHashSet<>();
		Set<BlockPos> frame = new HashSet<>();
		pending.add(origin.immutable());

		while (!pending.isEmpty()) {
			BlockPos pos = pending.removeFirst();
			if (!visited.add(pos)) {
				continue;
			}
			if (visited.size() > MAX_VISITED_BLOCKS
					|| !level.isInsideBuildHeight(pos)
					|| !level.hasChunkAt(pos)) {
				return Optional.empty();
			}

			BlockState state = level.getBlockState(pos);
			if (state.is(CustomNetherPortalModule.FRAME_BLOCKS)) {
				frame.add(pos);
				if (frame.size() > MAX_FRAME_BLOCKS) {
					return Optional.empty();
				}
				continue;
			}

			boolean isInterior = existingPortal
					? state.is(Blocks.NETHER_PORTAL) && state.getValue(NetherPortalBlock.AXIS) == axis
					: state.isAir() || state.is(BlockTags.FIRE);
			if (!isInterior) {
				return Optional.empty();
			}

			interior.add(pos);
			if (interior.size() > MAX_INTERIOR_BLOCKS) {
				return Optional.empty();
			}
			for (Direction direction : directions) {
				pending.add(pos.relative(direction));
			}
		}

		if (interior.isEmpty() || frame.size() < MIN_FRAME_BLOCKS) {
			return Optional.empty();
		}
		return Optional.of(new CustomPortalShape(axis, interior));
	}
}
