# Armour Statues Audit

## Status

Good implementation with minor correctness and cleanup work.

## Current design

- Sneak-use opens either the custom networked editor or a vanilla inventory-based fallback.
- Actions are authoritative on the server.
- Network actions validate entity type, range, life state, and lock ownership.
- Poses, equipment swaps, movement, visibility, locking, and presets are event-driven and have no background tick cost.

## Findings

### Fallback editor can bypass a newly applied lock

`ArmorStandMenu.validTarget()` checks only range and life state. If two players open an unlocked stand, one can lock it while the other continues using the already-open fallback menu.

Add `!ArmorStandActions.isLockedByOther(player, stand)` to the fallback validation and close or reject the menu when it fails.

### Pose clipboards are never cleared

The static `CLIPBOARDS` map retains one pose per player UUID indefinitely and can carry poses between integrated-server sessions. Clear the entry on disconnect and clear the map at server shutdown.

### Fallback labels contain mojibake

Degree labels currently contain `Â°`. Replace these with `°` or a Unicode escape and ensure source compilation remains UTF-8.

## Efficiency assessment

The feature is already efficient. Actions occur only in response to player interaction. The clipboard cleanup is primarily lifecycle hygiene rather than a significant performance optimization.

## Recommended changes

1. Enforce lock ownership in `ArmorStandMenu.validTarget()`.
2. Clear clipboard entries on disconnect and shutdown.
3. Correct the degree labels.
4. Add tests for simultaneous editors and fallback-menu lock enforcement.

## Relevant code

- `src/main/java/dev/belandsigh/armorstands/ArmorStandModule.java`
- `src/main/java/dev/belandsigh/armorstands/ArmorStandNetwork.java`
- `src/main/java/dev/belandsigh/armorstands/ArmorStandActions.java`
- `src/main/java/dev/belandsigh/armorstands/ArmorStandMenu.java`

## Discussion decisions

- Should operators bypass stand locks?
- Should copied poses persist only for the connection, for the server session, or permanently?

