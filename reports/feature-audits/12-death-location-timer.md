# DONE

# Death Location Timer Audit

## Status

Release blocker. The current proximity-based design can delete unrelated items and does not reliably control the lifespan of actual death drops.

## Current design

- A countdown is registered at the start of `Player.dropEquipment` when `keepInventory` is disabled.
- Multiple deaths per player are stored independently.
- Each countdown records dimension, position, total ticks, remaining ticks, and death number.
- The countdown advances while its death chunk reports as loaded.
- A boss bar shows number, distance, and remaining time.
- Every tick, an eight-block-radius entity query checks for loose items.
- At timeout, every item entity in that radius is discarded.
- Records are mirrored into player data for reconnect/restart persistence.

## Release-blocking findings

### Unrelated items can be deleted

The system treats every `ItemEntity` within eight blocks as a death drop. At timeout it discards all of them. This can delete:

- Items manually dropped by another player
- Mob and farm drops
- Items from another player's death
- Items from a newer death at the same location
- Existing items that were present before the death

### Nearby countdowns interfere

Two deaths with overlapping eight-block areas observe the same entities. The first countdown to expire can delete the items associated with both, after which the second countdown clears early.

### Genuine death items can escape tracking

Water, explosions, hoppers, or other movement can carry death drops outside the radius. They then stop contributing to the countdown and will not be controlled at timeout.

## Other correctness findings

### Configured lifespan is not applied to items

The feature maintains an independent counter but never changes an item's age or lifespan. A duration longer than vanilla's item lifetime will not preserve the item. A shorter duration depends on the unsafe area sweep.

### Loaded does not necessarily mean entity-ticking

`hasChunk()` can report a chunk as present when its entities are not actively ticking. The custom countdown may therefore advance while vanilla item age is paused.

### Empty deaths remain for the full timer

Early clearing requires `seenItems` to become true. A player dying with no drops can retain a countdown for the complete duration.

### Persistence omits runtime state

`seenItems` and `loadedTicksElapsed` are not stored in `DeathRecord`. A restart resets the grace behavior.

Countdowns can also continue changing in memory while their player is offline, but updated values are copied to persistent player data only while the player is online. A crash or restart can restore stale remaining time.

### Cross-dimension distance is misleading

The boss bar and command calculate direct coordinate distance even when the player and death are in different dimensions.

### Configuration can overflow

`despawnSeconds * 20` has no upper bound. A sufficiently large configured value can overflow to a negative tick count.

## Performance findings

Every active death performs an area entity query every server tick. It also reconstructs boss-bar text, formats time, calculates player distance, and updates membership/progress each tick.

With multiple players and multiple retained deaths, the cost scales linearly with active countdowns. Item detection and text refresh only need to occur about once per second.

## Recommended redesign

1. Assign a unique death ID before inventory drops spawn.
2. Associate each spawned death-item entity with that ID, preferably using persistent entity data or a sparse data attachment.
3. Store the identified item UUIDs or discover them by death ID, never by unrestricted proximity.
4. Drive completion from those entities' existence.
5. Apply the configured lifespan directly to those items, or explicitly manage their age while their entity-ticking chunk is active.
6. Store countdowns in server-level `SavedData` so offline state is authoritative and crash-safe.
7. Update item status and boss-bar text once per second rather than every tick.
8. Show dimension information and only calculate distance in the same dimension.
9. Clamp configuration to a documented maximum.
10. Add a configurable maximum number of retained deaths per player.

## Positive aspects to preserve

- `keepInventory` is respected.
- Multiple deaths are represented independently.
- Records use a structured codec.
- Boss-bar ownership moves to the replacement player after respawn.
- Runtime maps are cleared at server shutdown.
- Invalid configuration falls back to defaults.
- The new mixin and codec load successfully on a dedicated server.

## Relevant code

- `src/main/java/dev/belandsigh/death/DeathDropConfig.java`
- `src/main/java/dev/belandsigh/death/DeathDropModule.java`
- `src/main/java/dev/belandsigh/death/DeathRecord.java`
- `src/main/java/dev/belandsigh/death/DeathLocationData.java`
- `src/main/java/dev/belandsigh/death/DeathLocationModule.java`
- `src/main/java/dev/belandsigh/mixin/PlayerDeathDropMixin.java`
- `src/main/java/dev/belandsigh/mixin/ServerPlayerDeathLocationMixin.java`

## Required tests

- Empty-inventory death
- Two deaths at the same location
- Two different players dying nearby
- Unrelated items inside the radius
- Death items moved outside the radius
- Hopper collection and water movement
- Timer shorter and longer than vanilla item lifespan
- Loaded but non-entity-ticking chunks
- Logout, reconnect, graceful restart, and crash recovery
- Same-dimension and cross-dimension boss-bar display
- Invalid, huge, and overflowing configuration values

## Discussion decisions

- Must each individual death item receive the custom lifespan, or is the feature only intended as a location reminder?
- Should the timer continue while the owner is offline?
- How many previous deaths should be retained per player?
- Should another player collecting the items end the countdown immediately?

