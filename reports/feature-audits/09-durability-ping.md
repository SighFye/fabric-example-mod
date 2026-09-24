# Durability Ping Audit

## Status

Needs correctness fixes and a more efficient observation mechanism.

## Current design

- Every server tick checks both hands and all four armour slots for every player.
- Previous item type and damage are stored per slot.
- Alerts occur after crossing one of four absolute remaining-durability thresholds.
- Hand and armour alerts have separate 60-tick cooldowns.
- Per-player preferences persist in player data.
- Client mixins display exact durability counts at 25 durability or less.

## Findings

### Documentation and implementation disagree

The README says an alert occurs at 10% durability. The implementation instead checks absolute remaining values of 100, 50, 25, and 10.

### Per-tick allocation

Each damageable equipped stack creates a new `Snapshot` record every tick. At six slots per player, this creates avoidable garbage even when no item changes.

### Same-item switching can create false alerts

Snapshots identify only the item type and damage. Switching between two separate tools of the same type can appear to be sudden durability loss and cross a threshold.

### Polling is more frequent than necessary

Equipment does not need to be inspected 20 times per second to provide a useful warning.

## Recommended changes

Preferred design:

1. Hook the server-side operation where a stack receives durability damage.
2. Evaluate percentage or configured thresholds at that point.
3. Preserve cooldown and display preferences.

Lower-risk alternative:

1. Poll every 5–10 ticks.
2. Store primitive damage values and a stable stack identity/reference without allocating records.
3. Reset slot state when the actual stack instance changes.

## Relevant code

- `src/main/java/dev/belandsigh/durability/DurabilityPingModule.java`
- `src/main/java/dev/belandsigh/durability/DurabilityPingPreferences.java`
- `src/main/java/dev/belandsigh/mixin/ServerPlayerDurabilityPingMixin.java`
- `src/main/java/dev/belandsigh/mixin/ItemDurabilityDecorationMixin.java`

## Testing gaps

- Threshold semantics
- Tool switching with the same item type
- Mending and repairs
- Unbreaking and multi-point damage events
- Death/respawn preference persistence

## Discussion decisions

- Should warnings be percentage-based or fixed remaining counts?
- Should all four current thresholds remain?

