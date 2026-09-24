package dev.belandsigh.customportals;

import it.unimi.dsi.fastutil.longs.LongSet;
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
	static final int MIN_FRAME_BLOCKS = 10;
	static final int MAX_FRAME_BLOCKS = 84;
	static final int MAX_INTERIOR_BLOCKS = 1024;
	private static final int MAX_VISITED_BLOCKS = MAX_INTERIOR_BLOCKS + MAX_FRAME_BLOCKS;
	/** Every interior column needs a frame block above and below it (and every row one at each end), so a
	 *  valid shape is never wider or taller than half the frame budget. */
	static final int MAX_SPAN = MAX_FRAME_BLOCKS / 2;

	/** What the shape search cares about at a position. Unloaded or out-of-world positions are {@link #BLOCKED}. */
	enum Cell { EMPTY, FRAME, PORTAL_X, PORTAL_Z, BLOCKED }

	/** Block lookup used by the search; abstracted so shapes can be tested without a live world. */
	@FunctionalInterface
	interface Cells {
		Cell at(BlockPos pos);
	}

	private final Direction.Axis axis;
	private final Set<BlockPos> interior;

	private CustomPortalShape(Direction.Axis axis, Set<BlockPos> interior) {
		this.axis = axis;
		this.interior = Set.copyOf(interior);
	}

	static Cells cells(LevelReader level) {
		return pos -> {
			if (!level.isInsideBuildHeight(pos) || !level.hasChunkAt(pos)) return Cell.BLOCKED;
			BlockState state = level.getBlockState(pos);
			if (state.is(CustomNetherPortalModule.FRAME_BLOCKS)) return Cell.FRAME;
			if (state.isAir() || state.is(BlockTags.FIRE)) return Cell.EMPTY;
			if (state.is(Blocks.NETHER_PORTAL)) {
				return state.getValue(NetherPortalBlock.AXIS) == Direction.Axis.X ? Cell.PORTAL_X : Cell.PORTAL_Z;
			}
			return Cell.BLOCKED;
		};
	}

	/**
	 * Finds an empty irregular frame around a fire on either horizontal axis. Straight probes from the origin reject
	 * the common case (fire on open ground) in a handful of lookups before paying for a flood fill.
	 */
	static Optional<CustomPortalShape> findEmpty(Cells cells, BlockPos origin) {
		if (!hitsFrame(cells, origin, Direction.DOWN) || !hitsFrame(cells, origin, Direction.UP)) {
			return Optional.empty();
		}
		for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
			Direction side = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
			if (hitsFrame(cells, origin, side) && hitsFrame(cells, origin, side.getOpposite())) {
				Optional<CustomPortalShape> shape = find(cells, origin, axis, false, null);
				if (shape.isPresent()) return shape;
			}
		}
		return Optional.empty();
	}

	/** Checks an existing portal; on failure every portal position visited is added to {@code brokenOut}, if given. */
	static boolean isComplete(Cells cells, BlockPos origin, Direction.Axis axis, LongSet brokenOut) {
		return find(cells, origin, axis, true, brokenOut).isPresent();
	}

	Direction.Axis axis() {
		return axis;
	}

	Set<BlockPos> interior() {
		return interior;
	}

	/** Walks from the origin until a frame block (true) or anything other than open space (false). */
	private static boolean hitsFrame(Cells cells, BlockPos origin, Direction direction) {
		BlockPos.MutableBlockPos pos = origin.mutable();
		for (int step = 0; step <= MAX_SPAN; step++) {
			switch (cells.at(pos.move(direction))) {
				case FRAME -> {
					return true;
				}
				case EMPTY -> { }
				default -> {
					return false;
				}
			}
		}
		return false;
	}

	private static Optional<CustomPortalShape> find(
			Cells cells,
			BlockPos origin,
			Direction.Axis axis,
			boolean existingPortal,
			LongSet brokenOut
	) {
		if (!axis.isHorizontal()) {
			return Optional.empty();
		}

		Cell interiorCell = !existingPortal ? Cell.EMPTY : axis == Direction.Axis.X ? Cell.PORTAL_X : Cell.PORTAL_Z;
		Direction[] directions = axis == Direction.Axis.X
				? new Direction[] {Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST}
				: new Direction[] {Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH};
		ArrayDeque<BlockPos> pending = new ArrayDeque<>();
		Set<BlockPos> visited = new HashSet<>();
		Set<BlockPos> interior = new LinkedHashSet<>();
		int frameBlocks = 0;
		pending.add(origin.immutable());

		// When recording a broken region, keep filling after the first failure so the whole region is recorded
		// in one pass; the visit cap still bounds the work.
		boolean exhaustive = brokenOut != null;
		boolean valid = true;
		while (!pending.isEmpty()) {
			BlockPos pos = pending.removeFirst();
			if (!visited.add(pos)) {
				continue;
			}
			if (visited.size() > MAX_VISITED_BLOCKS) {
				valid = false;
				break;
			}

			Cell cell = cells.at(pos);
			if (cell == Cell.FRAME) {
				if (++frameBlocks > MAX_FRAME_BLOCKS) {
					valid = false;
					if (!exhaustive) break;
				}
				continue;
			}
			if (cell != interiorCell) {
				valid = false;
				if (!exhaustive) break;
				continue;
			}

			interior.add(pos);
			if (interior.size() > MAX_INTERIOR_BLOCKS) {
				valid = false;
				if (!exhaustive) break;
			}
			for (Direction direction : directions) {
				pending.add(pos.relative(direction));
			}
		}

		if (valid && !interior.isEmpty() && frameBlocks >= MIN_FRAME_BLOCKS) {
			return Optional.of(new CustomPortalShape(axis, interior));
		}
		// Everything reached belongs to the same connected region, so it fails the same way from any start.
		if (brokenOut != null) {
			for (BlockPos pos : interior) brokenOut.add(pos.asLong());
		}
		return Optional.empty();
	}
}
