# Better Pet Following Audit

## Status

Implemented (September 2026), with a change after in-game testing. The saved-location reunion described below did not bring a pet back after a 10,000-block teleport. It was replaced by this rule: **every standing, unleashed, owned pet keeps its own chunk loaded at all times, with no exceptions for offline owners or owners in another dimension.** The ticket is `belandsigh:pet_follow`: loading only, radius 0, no timeout, and pets sharing a chunk share one ticket. It is released when the pet sits, is leashed, rides, or unloads. The pet index and the catch-up teleport below are kept. `PetLocationSavedData` was removed. Tickets are not persisted, so after a server restart a pet stays unloaded until a player comes near it, the same as the original module.

Original finding: major performance concern. This was the largest continuous server-cost feature.

## Current design

- Once per second, every loaded entity in every server level is scanned.
- Tame, alive, non-sitting, non-leashed wolves, cats, and parrots qualify.
- Each qualifying pet's current chunk receives a radius-two loading/simulation ticket.
- Tickets are reconciled as pets move and removed when no qualifying pet remains in the chunk.

## Findings

### Global entity scan

The module scans all loaded entities, not just tracked pets. Cost therefore grows with farms, mobs, projectiles, vehicles, and other unrelated entities.

### Standing does not mean following

Every standing tame pet qualifies even if it is parked, wandering, unable to path, or owned by an offline player.

### Chunk footprint is large

A radius-two ticket can keep roughly a 5×5 chunk region loaded and simulated. Separated pets multiply that footprint; ten widely separated pets could retain around 250 chunks.

### No owner proximity or online check

The system can keep remote chunks active indefinitely without benefiting an active player.

## Recommended redesign

1. Track eligible pets from entity load/unload and relevant state transitions.
2. Require the owner to be online and in the same dimension.
3. Only ticket when the pet is outside normal player-loaded range but actively attempting to follow its owner.
4. Use the smallest radius that allows pet AI to advance safely.
5. Add a per-player and global cap on forced pet chunks.
6. Release tickets immediately when the owner logs out, changes dimension, or the pet sits/is leashed.
7. Measure ticket counts and tick time in a pet-heavy test world.

## Relevant code

- `src/main/java/dev/belandsigh/pets/` (`PetFollowModule`, `PetFollowPolicy`, `PetOwnerIndex`)
- `src/main/java/dev/belandsigh/mixin/TamableAnimalOwnerMixin.java`

## Discussion decisions

- Should pets follow across dimensions, or stop at portals?
- How many simultaneously followed pets should one player be allowed?
- Is chunk loading required, or would safe pet teleportation after a distance threshold provide better behavior?


## Proposed solution: catch-up teleport plus saved pet locations

### Key observations

1. Vanilla already teleports pets. `FollowOwnerGoal` (and the tamed panic goal) call `TamableAnimal.tryToTeleportToOwner()` once the owner is 12+ blocks away, even when the owner is far away. `getOwner()` only finds an owner in the pet's own dimension. So when the owner comes back to that dimension, any pet that is loaded and ticking teleports to them. This is why the current module keeps pet chunks simulated.
2. The pet only needs to be **in memory** for that to happen, not simulated. `tryToTeleportToOwner()` is public, and our own server tick can call it for a pet in a loaded chunk that is not ticking.
3. We don't need to keep a chunk loaded while the owner is away. We need to know **where** the pet is, so we can load its chunk when the owner comes back. Saved pet locations do that across dimension changes, logouts, and restarts, with no chunks held in the meantime.

Note on the audit finding "standing does not mean following": in vanilla, a standing tame pet whose owner is loaded does follow. `unableToMoveToOwner()` covers sitting, leashed, riding, and spectator owners. The real problem is that the current design keeps chunks simulated while the owner is offline or in another dimension, where the pet can't do anything.

### Which pets are saved

We save the location of every tame pet that can teleport, meaning `!unableToMoveToOwner()`: not sitting, not leashed, and not riding. Sitting or leashed pets never teleport in vanilla, so there is nothing to reunite and they get no record. This follows vanilla's rule exactly: a standing pet follows its owner, and players sit a pet to leave it behind.

### Components

**`PetOwnerIndex`** (runtime) tracks loaded, owned pets without scanning.
- `Map<UUID ownerId, Set<TamableAnimal>>` plus a reverse map, so re-indexing is O(1).
- Filled from `ServerEntityEvents.ENTITY_LOAD` for `Wolf`, `Cat`, and `Parrot` that have an owner. Entries are removed on `ENTITY_UNLOAD`.
- A mixin on `TamableAnimal.setOwner` / `setOwnerReference` (TAIL, server side only) re-indexes a pet when it is tamed or changes owner after loading.

**`PetLocationSavedData`** (persistent, stored in overworld data storage) records where unloaded pets that can teleport are.
- `Location(petId, ownerId, dimension, chunkX, chunkZ)`, indexed by pet and by owner.
- **Written on `ENTITY_UNLOAD`** of an owned pet with `!unableToMoveToOwner()`. This is the one moment the pet leaves memory, so the saved position is always the one it was stored to disk at. It covers every case: the owner going through a portal, the owner logging out, the owner outrunning the pet, and server shutdown.
- **Removed** when the pet reaches the owner, when it is found sitting, leashed or riding after it loads, when it dies or is discarded (unload reason `KILLED`/`DISCARDED`, which includes a parrot landing on a shoulder), when it changes dimension (`CHANGED_DIMENSION`; it is recorded again on its next unload), when it is tamed by someone else, or after it is not found (see below). A pet that unloads while sitting or leashed also has any old record removed.
- No cap. A standing, unleashed pet always follows its owner, so every saved pet was travelling with the owner and unloaded where the owner left it. Their records share one chunk, give or take a boundary. Reunion loads one ticket per **distinct** chunk, not per pet, so in practice it is a single chunk load. A record is a few bytes.

**`PetFollowPolicy`** is a pure decision function, unit-testable like Mount Whistle. It returns `STAY`, `WAIT`, `WITH_OWNER`, `VANILLA`, or `CATCH_UP` for a loaded pet.

**`PetFollowModule`** replaces `PetChunkLoadingModule`. On `SERVER_STOPPING` it also records every loaded pet that can teleport, because entity unloads during shutdown can happen after saved data is written.

### Runtime flow

**Every 10 ticks, for each online player (loaded pets):**

```
for pet in index.get(player):
    if pet.unableToMoveToOwner()               -> drop record; skip (sat, leashed, riding)
    if pet.level() != player.level()           -> skip (its record resolves on return)
    if distSqr(pet, player) < 144              -> skip (vanilla range)
    ticking  = level.isPositionEntityTicking(pet.blockPosition())
    nearEdge = chunkDistance(pet, player) >= simDistance - 1
    if ticking && !nearEdge                    -> skip (vanilla goal handles it)
    pet.tryToTeleportToOwner()
    if moved -> drop record; release ticket
    else if !ticking -> hold ticket; retry next check
```

**Reunion (unloaded pets).** This runs in the same 10-tick check for every online owner, so joining and changing dimension need no separate hooks:

```
for record in savedData.get(player) where record.dimension == player.level():
    if pet already in index -> handled by the loop above
    else collect record.chunkPos
add one pet_follow ticket per distinct chunk (radius 0, loading only)
```

When the chunk loads, `ENTITY_LOAD` puts the pet in the index. The next 10-tick check teleports it to the owner and removes the record and ticket.

**Pet not found.** If the pet doesn't appear within about 100 ticks, the record's position is stale. That can happen if the pet wandered across a chunk boundary while another player had it loaded, or after a crash. Retry once with radius 1 (3×3 chunks). If it's still missing, drop the record.

**Tickets.** One ticket type, `belandsigh:pet_follow`, is used for both holding and reunion. It uses `FLAG_LOADING` only: no simulation and no `KEEP_DIMENSION_ACTIVE`. It has a 100-tick timeout that is refreshed on each retry, so it lapses on its own about 5 s after it is no longer needed. A hold has no time limit while the owner stays online in that dimension, because it costs one idle chunk. The pet only has to be in memory; our code teleports it.

### Worked examples

| Scenario | Result |
| --- | --- |
| Owner flies away with elytra; dog falls outside simulation range | `nearEdge` → teleport. If the owner is mid-air, the dog's chunk is held until they land. If the chunk unloads anyway, `ENTITY_UNLOAD` saves a record and a reunion follows within about a second. |
| Owner enters the Nether; dog left at the overworld portal | The dog's chunk unloads normally and `ENTITY_UNLOAD` saves its location. Nothing is held while the owner is in the Nether. When the owner returns through a portal miles away, the chunk loads, the dog loads, and it teleports to the owner. |
| Owner logs out mid-flight, dog far behind | The dog's chunk unloads and its location is saved. On join, the chunk loads and the dog teleports. |
| Server restarts | Records are saved data, so the reunion runs on join. |
| Dog left sitting or leashed at base | No record; the dog stays put. |

### Cost comparison

| | Current | Proposed |
| --- | --- | --- |
| Work per check | Every loaded entity in every level | Only indexed pets of online owners |
| Steady-state chunk footprint | ~25 simulated chunks per standing pet, forever | 0 |
| While owner is in another dimension or offline | Chunks simulated, dimension kept active | 0 chunks; one small saved record per pet |
| On owner return | Nothing extra | One chunk load per distinct recorded chunk (usually 1), for ~5 s |

### Answers to the discussion questions

- **Across dimensions?** Pets don't travel through portals with their owner, so no wolves in the Nether. They wait at the portal and teleport to the owner when the owner returns to their dimension, from any portal.
- **How many pets per player?** No limit. Pets travelling with their owner share a chunk, so reunion is usually a single chunk load.
- **Chunk loading or teleport?** Both, briefly. We load chunks only to get the pet into memory, then teleport it with vanilla's own `tryToTeleportToOwner()`, which does the same safety checks (walkable, no leaves for non-flyers, no collision).

### Known limitations

- Vanilla's teleport picks a spot within ±3 blocks of the owner. When the owner is mid-air or in water it fails and we keep retrying. Parrots are treated the same way.
- If the server crashes before an unload, the pet's saved position may be out of date. The radius-1 retry covers most of these cases.

### Implementation steps

1. Add `PetOwnerIndex`, `PetLocationSavedData`, `PetFollowPolicy`, and `PetFollowModule`, plus `TamableAnimalOwnerMixin`. Delete `PetChunkLoadingModule`. The old ticket type has no `FLAG_PERSIST`, so no migration is needed.
2. Unit-test `PetFollowPolicy` and the saved data's encode/decode round trip and grouping of records by chunk.
3. Test in-game with 10 wolves across two players at simulation distance 6: sprint, ride a horse, fly with elytra and rockets, the portal round trip from different portals, log out in the Nether and log back in, restart the server with pending records, and dogs left sitting and leashed at base (these should not teleport). Record `/debug` tick time and loaded chunk count before and after.
4. Consider a 5-tick interval only if step 3 shows pets lost during rocket flight.
