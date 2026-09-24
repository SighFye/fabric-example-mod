package dev.belandsigh.leaves;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeafDecayQueueTest {
	@Test
	void drainNeverExceedsBudget() {
		LeafDecayQueue queue = new LeafDecayQueue();
		for (long pos = 0; pos < 1000; pos++) queue.add(pos, 0);

		List<Long> drained = new ArrayList<>();
		queue.drain(0, 64, drained::add);

		assertEquals(64, drained.size());
		assertEquals(936, queue.size());
	}

	@Test
	void leavesWaitUntilTheirDueTick() {
		LeafDecayQueue queue = new LeafDecayQueue();
		queue.add(1, 5);
		queue.add(2, 10);

		List<Long> drained = new ArrayList<>();
		queue.drain(4, 64, drained::add);
		assertTrue(drained.isEmpty());

		queue.drain(5, 64, drained::add);
		assertEquals(List.of(1L), drained);

		queue.drain(10, 64, drained::add);
		assertEquals(List.of(1L, 2L), drained);
		assertTrue(queue.isEmpty());
	}

	@Test
	void earliestDueDrainsFirst() {
		LeafDecayQueue queue = new LeafDecayQueue();
		queue.add(30, 3);
		queue.add(10, 1);
		queue.add(20, 2);

		List<Long> drained = new ArrayList<>();
		queue.drain(3, 64, drained::add);

		assertEquals(List.of(10L, 20L, 30L), drained);
	}

	@Test
	void overBudgetLeavesCarryIntoLaterTicks() {
		LeafDecayQueue queue = new LeafDecayQueue();
		for (long pos = 0; pos < 1000; pos++) queue.add(pos, 0);

		int ticks = 0;
		while (!queue.isEmpty()) {
			queue.drain(ticks, FastLeafDecayModule.DECAYS_PER_TICK, ignored -> { });
			ticks++;
		}
		assertEquals(16, ticks);
	}

	@Test
	void requeueingKeepsOriginalDueTick() {
		LeafDecayQueue queue = new LeafDecayQueue();
		queue.add(5, 3);
		queue.add(5, 100);

		assertEquals(1, queue.size());
		List<Long> drained = new ArrayList<>();
		queue.drain(3, 64, drained::add);
		assertEquals(List.of(5L), drained);
	}
}
