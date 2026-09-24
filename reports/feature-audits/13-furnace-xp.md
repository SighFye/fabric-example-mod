# Furnace XP Audit

## Status

Healthy; small performance wins available, plus two client-state correctness faults. Performance is not a real concern at any realistic player count. Every improvement below is cheap, but none of them will show up in a profiler on a normal server.

## Current design

- Every server tick, every online player is checked for an open furnace, blast furnace, or smoker menu.
- For each player with one open (and a modded client), the furnace's `recipesUsed` map is read. Each entry is resolved through `RecipeManager.byKey` and summed as `count * experience`.
- The sum is converted to tenths and sent in a custom payload, but only when `(containerId, tenths)` differs from the last value sent to that player.
- The client stores the last payload in `FurnaceXpClientState`. The screen mixin refreshes the button every frame and rebuilds its label and tooltip only when the value changes.
- Clicking sends vanilla's menu-button packet (id 0). The server calls vanilla `awardUsedRecipesAndPopExperience`, which pops orbs at the player and clears the map.

## Cost profile

| Path | Frequency | Work |
| --- | --- | --- |
| Player scan | Every tick × every player | One `instanceof` + one `HashMap.remove(UUID)` per player without a furnace open |
| Stored-XP sum | Every tick × every player with a furnace open | One hash lookup per distinct recipe in the furnace (usually 1–3), one `SentState` allocation, one map `get` |
| Network | Only on change | About 6 bytes; roughly one packet per smelted item (every 100–200 ticks per furnace) |
| Client frame | Every frame while the screen is open | `setPosition` + one int compare; label and tooltip rebuilt only on change |

`recipesUsed` holds one entry per distinct recipe, so it stays tiny. Even 50 players each with a furnace open cost around 150 hash lookups per tick, a few microseconds. The only mutations of `recipesUsed` in vanilla are `setRecipeUsed` (one per completed smelt), `loadAdditional`, and the `clear()` at the end of `awardUsedRecipesAndPopExperience`. So 99% of the per-tick sums recompute a value that cannot have changed.

## Performance findings

### P1: Stored XP is recomputed every tick although it changes only on smelt or collect

`FurnaceXpModule.syncOpenFurnace` (line 74) re-sums the map every tick for every viewer. Two options:

- **Change counter (preferred).** Add a mixin field `belandsigh$xpRevision` on `AbstractFurnaceBlockEntity` and increment it at `setRecipeUsed` TAIL, `awardUsedRecipesAndPopExperience` TAIL, and `loadAdditional` TAIL. Track `(containerId, revision)` per player and recompute only when the revision differs. The per-tick cost drops to one int compare. Updates are still instant.
- **Throttle (simplest).** Sync every 5–10 ticks instead of every tick. Smelts take 100–200 ticks, so the delay is invisible, *provided the collect click forces an immediate resync* (see C1).

The change counter adds three injections into vanilla furnace code, which is a small compatibility risk with other furnace mods. The throttle adds none.

### P2: Viewers of the same furnace each compute the same sum

Several players with one furnace open each run `storedExperience` separately. With P1's change counter this disappears naturally. Without it, a per-tick `IdentityHashMap<AbstractFurnaceBlockEntity, Integer>` cache would work. This is only worth doing if P1 is skipped.

### P3: Per-tick housekeeping for players without a furnace open

`LAST_SENT.remove(player.getUUID())` (line 77) runs every tick for every player, including the vast majority who never open a furnace. Check `LAST_SENT.isEmpty()` first, or only remove when the player's previous state was tracked. The gain is trivial, but so is the fix.

### P4: `SentState` allocated every tick per viewer

Line 85 allocates a record just to compare against the map. Either compare fields against the existing value before allocating, or store two primitive fields (or one packed `long`) per player. The JIT's escape analysis likely removes this already, so it is low priority.

### P5: Collect click marks the furnace dirty even when there is nothing to collect

`handleMenuButton` always calls `furnace.setChanged()` (line 70). In vanilla this marks the chunk for saving and runs `updateNeighbourForOutputSignal`, which is a comparator neighbour update. A player spam-clicking an empty furnace (the button is disabled at 0.0, but a modified client can still send the packet) triggers a neighbour update per packet. Guard it with `if (!recipesUsed.isEmpty())`. This is the most tangible server-side improvement in the list.

### P6: Client tooltip level calculation is O(levels)

`FurnaceXpMath.levelsGained` loops one iteration per level crossed. At the display cap this is a few hundred iterations, and at `Integer.MAX_VALUE` tenths around 7,000. It runs only when the value changes, so this is negligible. Clamping the input to `MAX_DISPLAY_TENTHS` before the call would bound it permanently.

## Correctness findings

### C1: A rejected click leaves the button stuck at 0.0

The click handler optimistically calls `FurnaceXpClientState.clear` (screen mixin line 44). If the server ignores the click, `LAST_SENT` still holds the real value, so the server never resends and the client shows "0.0" (disabled) until the next smelt changes the amount. Vanilla ignores the click for spectators (`handleContainerButtonClick` rejects them), and spectators can open furnaces. A `stillValid` failure also ignores the click.

Fix: either drop the optimistic clear, or have the server invalidate `LAST_SENT` for that player whenever a furnace button packet arrives, so the true value is resent next tick. Hiding the button for spectators is a sensible addition either way.

### C2: Stale client state can show the button on an unmodded server

`FurnaceXpClientState` is never reset. Container ids cycle 1–100 per player (`containerCounter % 100 + 1`) and restart at 1 on every server. After leaving a modded server, the next vanilla server or singleplayer world can open a furnace whose id matches the stored one. The button then appears with the old value, although the code comment promises it "stays hidden until the server reports a value". Clicking it is harmless because vanilla returns false for button 0 on furnaces, but the display is wrong.

Fix: reset the state on `ClientPlayConnectionEvents.DISCONNECT` (or `JOIN`). Ideally also reset it when a furnace screen's `init` runs for a new container id.

### C3: Tooltip "levels gained" can go stale

The tooltip is rebuilt only when `tenths` changes, but it depends on the player's current level and progress. If the player gains XP from another source while the screen is open, the tooltip is outdated. Fix: include `experienceLevel` and `experienceProgress` in the change check, or rebuild the tooltip on hover.

### C4: Server map not cleared on shutdown

Other modules clear their static maps on `SERVER_STOPPED`. `LAST_SENT` relies only on `DISCONNECT`. That is probably fine in practice, but for consistency add `ServerLifecycleEvents.SERVER_STOPPED.register(s -> LAST_SENT.clear())`.

## Recommended changes (in order)

1. Guard `setChanged()` behind a non-empty check (P5).
2. Reset client state on disconnect (C2).
3. Force a resync after any furnace button click, and hide the button for spectators (C1).
4. Throttle the sync to every 5 ticks, or add the change counter (P1). Either one also covers P2 and P4.
5. Skip `LAST_SENT.remove` when there is nothing to remove (P3), and add a `SERVER_STOPPED` clear (C4).
6. Clamp `levelsGained` input and refresh the tooltip when the player's level changes (P6, C3).

## Positive aspects to preserve

- Uses vanilla's own payout (`awardUsedRecipesAndPopExperience`), so orbs, Mending, recipe unlocks, and advancement triggers behave exactly as when taking from the result slot.
- Uses vanilla's menu-button packet, which already validates container id, spectator status, and `stillValid` range.
- Degrades cleanly for vanilla clients (`canSend` check comes before any computation).
- Sends only on change, with a compact varint payload.
- The client label and tooltip are rebuilt only on change, not per frame.
- Arithmetic is isolated in `FurnaceXpMath` and unit tested (6 tests pass), including float-error and overflow cases.

## Relevant code

- `src/main/java/dev/belandsigh/furnacexp/FurnaceXpModule.java`
- `src/main/java/dev/belandsigh/furnacexp/FurnaceXpMath.java`
- `src/main/java/dev/belandsigh/client/FurnaceXpClientState.java`
- `src/main/java/dev/belandsigh/mixin/FurnaceScreenMixin.java`
- `src/main/java/dev/belandsigh/mixin/ContainerMenuButtonMixin.java`
- `src/main/java/dev/belandsigh/mixin/AbstractFurnaceBlockEntityAccessor.java`
- `src/main/java/dev/belandsigh/mixin/AbstractFurnaceMenuAccessor.java`

## Required tests

- Collect from furnace, blast furnace, and smoker; the display resets to 0.0
- Two players viewing the same furnace; both update after one collects
- Spectator opens a furnace and clicks; the display must not stick at 0.0
- Leave a modded server, join a vanilla server, open furnaces until the container id wraps; the button must stay hidden
- Hopper-fed furnace running while the screen is open; the value climbs per smelt
- Datapack removes a recipe that is already in `recipesUsed`; it is skipped in both display and payout
- Unmodded client on a modded server; no payload is sent and no errors occur

## Discussion decisions

- Throttle or change counter for P1? The throttle is simpler and has no compatibility risk. The counter is exact and free per tick.
- Should spectators see the button at all?
- Should the button stay enabled when stored XP is under 0.1 but recipes are pending (recipe unlocks still happen on collect)?
