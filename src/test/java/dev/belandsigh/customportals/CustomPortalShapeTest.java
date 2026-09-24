package dev.belandsigh.customportals;

import dev.belandsigh.customportals.CustomPortalShape.Cell;
import dev.belandsigh.customportals.CustomPortalShape.Cells;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomPortalShapeTest {
	@BeforeAll
	static void bootstrap() {
		net.minecraft.SharedConstants.tryDetectVersion();
		net.minecraft.server.Bootstrap.bootStrap();
	}

	/** Sparse world on the X/Y plane at z = 0; anything unset is {@code fallback}. Counts lookups. */
	private static final class Grid implements Cells {
		private final Map<BlockPos, Cell> cells = new HashMap<>();
		private final Cell fallback;
		int lookups;

		Grid(Cell fallback) {
			this.fallback = fallback;
		}

		void set(int x, int y, Cell cell) {
			cells.put(new BlockPos(x, y, 0), cell);
		}

		@Override
		public Cell at(BlockPos pos) {
			lookups++;
			return cells.getOrDefault(pos.immutable(), fallback);
		}
	}

	/** Frames a w-by-h rectangle of {@code fill} whose bottom-left interior block is (0, 0). */
	private static Grid rectangle(int width, int height, Cell fill, Cell outside) {
		Grid grid = new Grid(outside);
		for (int x = -1; x <= width; x++) {
			for (int y = -1; y <= height; y++) {
				boolean edge = x == -1 || x == width || y == -1 || y == height;
				boolean corner = (x == -1 || x == width) && (y == -1 || y == height);
				if (corner) continue;
				grid.set(x, y, edge ? Cell.FRAME : fill);
			}
		}
		return grid;
	}

	/** An L-shape: a 4x3 base with a 2x3 column on its left, framed without corners. */
	private static Grid lShape(Cell fill) {
		Grid grid = new Grid(Cell.BLOCKED);
		int[][] interior = {
			{0, 0}, {1, 0}, {2, 0}, {3, 0}, {0, 1}, {1, 1}, {2, 1}, {3, 1}, {0, 2}, {1, 2}, {2, 2}, {3, 2},
			{0, 3}, {1, 3}, {0, 4}, {1, 4}, {0, 5}, {1, 5}
		};
		for (int[] pos : interior) grid.set(pos[0], pos[1], fill);
		for (int[] pos : interior) {
			for (int[] offset : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
				int x = pos[0] + offset[0];
				int y = pos[1] + offset[1];
				if (!grid.cells.containsKey(new BlockPos(x, y, 0))) grid.set(x, y, Cell.FRAME);
			}
		}
		return grid;
	}

	@Test
	void irregularFrameIsDetected() {
		Grid grid = lShape(Cell.EMPTY);
		var shape = CustomPortalShape.findEmpty(grid, new BlockPos(1, 1, 0));
		assertTrue(shape.isPresent());
		assertEquals(Direction.Axis.X, shape.get().axis());
		assertEquals(18, shape.get().interior().size());
	}

	@Test
	void fireOnOpenGroundIsRejectedInOneLookup() {
		Grid grid = new Grid(Cell.EMPTY);
		grid.set(0, -1, Cell.BLOCKED); // the ground under the fire
		assertTrue(CustomPortalShape.findEmpty(grid, BlockPos.ZERO).isEmpty());
		assertEquals(1, grid.lookups);
	}

	@Test
	void fireUnderOpenSkyStopsAtTheSpanLimit() {
		Grid grid = new Grid(Cell.EMPTY);
		grid.set(0, -1, Cell.FRAME); // fire on a lone obsidian block
		assertTrue(CustomPortalShape.findEmpty(grid, BlockPos.ZERO).isEmpty());
		assertTrue(grid.lookups <= 2 + CustomPortalShape.MAX_SPAN, "lookups: " + grid.lookups);
	}

	@Test
	void largestAllowedSpanIsStillAccepted() {
		// 41 wide x 1 tall uses 84 frame blocks, exactly the budget; probing from one end crosses the whole width.
		Grid grid = rectangle(41, 1, Cell.EMPTY, Cell.BLOCKED);
		assertTrue(CustomPortalShape.findEmpty(grid, BlockPos.ZERO).isPresent());
	}

	@Test
	void maximumSizePortalValidatesWithinBudget() {
		// 21x21 = 441 interior blocks and 84 frame blocks: the largest square the frame budget allows.
		Grid grid = rectangle(21, 21, Cell.PORTAL_X, Cell.BLOCKED);
		assertTrue(CustomPortalShape.isComplete(grid, BlockPos.ZERO, Direction.Axis.X, null));
		assertTrue(grid.lookups <= 21 * 21 + CustomPortalShape.MAX_FRAME_BLOCKS, "lookups: " + grid.lookups);
	}

	@Test
	void brokenPortalCascadeCostsAboutOneFloodFill() {
		Grid grid = rectangle(21, 21, Cell.PORTAL_X, Cell.BLOCKED);
		grid.set(-1, 10, Cell.EMPTY); // a frame block was mined
		LongOpenHashSet broken = new LongOpenHashSet();

		// Simulate the removal cascade: every portal block asks whether it's still valid.
		int searches = 0;
		grid.lookups = 0;
		for (int x = 0; x < 21; x++) {
			for (int y = 0; y < 21; y++) {
				BlockPos pos = new BlockPos(x, y, 0);
				if (broken.contains(pos.asLong())) continue;
				searches++;
				assertFalse(CustomPortalShape.isComplete(grid, pos, Direction.Axis.X, broken));
			}
		}
		assertEquals(1, searches);
		assertEquals(21 * 21, broken.size());
		assertTrue(grid.lookups <= 21 * 21 + CustomPortalShape.MAX_FRAME_BLOCKS, "lookups: " + grid.lookups);
	}

	@Test
	void validPortalIsNotMarkedBroken() {
		Grid grid = lShape(Cell.PORTAL_X);
		LongOpenHashSet broken = new LongOpenHashSet();
		assertTrue(CustomPortalShape.isComplete(grid, BlockPos.ZERO, Direction.Axis.X, broken));
		assertTrue(broken.isEmpty());
	}

	@Test
	void tooFewFrameBlocksIsRejected() {
		Grid grid = rectangle(1, 1, Cell.EMPTY, Cell.BLOCKED); // 4 frame blocks
		assertTrue(CustomPortalShape.findEmpty(grid, BlockPos.ZERO).isEmpty());
	}
}
