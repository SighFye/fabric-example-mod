package dev.belandsigh.leaves;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.PriorityQueue;
import java.util.function.LongConsumer;

/** Packed block positions awaiting decay, released once their due tick arrives; a leaf is queued at most once. */
final class LeafDecayQueue {
	private final PriorityQueue<Entry> entries = new PriorityQueue<>();
	private final LongOpenHashSet queued = new LongOpenHashSet();

	void add(long pos, long dueTick) {
		if (queued.add(pos)) {
			entries.add(new Entry(pos, dueTick));
		}
	}

	/** Hands at most {@code budget} due positions to {@code action}, earliest due first. */
	void drain(long now, int budget, LongConsumer action) {
		for (int i = 0; i < budget && !entries.isEmpty() && entries.peek().dueTick() <= now; i++) {
			long pos = entries.poll().pos();
			queued.remove(pos);
			action.accept(pos);
		}
	}

	boolean isEmpty() {
		return entries.isEmpty();
	}

	int size() {
		return entries.size();
	}

	private record Entry(long pos, long dueTick) implements Comparable<Entry> {
		@Override
		public int compareTo(Entry other) {
			return Long.compare(dueTick, other.dueTick);
		}
	}
}
