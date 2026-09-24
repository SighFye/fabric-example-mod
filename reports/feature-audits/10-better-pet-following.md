# Better Pet Following Audit

## Status

Major performance concern. This is currently the largest continuous server-cost feature.

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

- `src/main/java/dev/belandsigh/pets/PetChunkLoadingModule.java`

## Discussion decisions

- Should pets follow across dimensions, or stop at portals?
- How many simultaneously followed pets should one player be allowed?
- Is chunk loading required, or would safe pet teleportation after a distance threshold provide better behavior?

