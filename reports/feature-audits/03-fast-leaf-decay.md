# Fast Leaf Decay Audit

## Status

Functionally sound, but capable of producing excessive scheduled-tick work.

## Current design

- A mixin runs after a leaf scheduled tick.
- Persistent/player-placed leaves are ignored.
- Unsupported leaves at maximum distance receive a 7.5% decay roll.
- On failure, the leaf schedules another tick one game tick later.
- Actual destruction uses vanilla random-tick behavior, retaining normal loot and gamerule handling.

## Findings

Each eligible leaf averages roughly 13 scheduled ticks before the 7.5% roll succeeds. Removing a large tree or forest can therefore create a burst of thousands of one-tick reschedules.

The repeated `getBlockState` call is small compared with the scheduler pressure. The main issue is polling every tick rather than scheduling one meaningful future operation.

## Recommended changes

Choose one of these designs:

1. Schedule one randomized decay time and decay on that tick.
2. Maintain a server queue and process a bounded number of leaves per tick.
3. Decay immediately when distance reaches the unsupported state if very fast decay is acceptable.

The bounded queue offers the best protection against forest-sized spikes.

## Relevant code

- `src/main/java/dev/belandsigh/mixin/LeavesBlockMixin.java`

## Testing gaps

- Player-placed persistent leaves
- Large connected canopies
- `doTileDrops=false`
- Leaves crossing chunk boundaries
- Server tick time during mass log removal

## Discussion decisions

- Is the desired behavior instant decay, visually gradual decay, or a strict per-tick work budget?

