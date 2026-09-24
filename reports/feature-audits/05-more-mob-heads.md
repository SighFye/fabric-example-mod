# More Mob Heads Audit

## Status

Generally good. The primary issue is compatibility with replaced loot tables.

## Current design

- Fabric's loot-table modification event detects supported vanilla entity tables.
- One nested head loot table is added to the existing entity loot table.
- Variant selection and drop chances are data-driven.
- Existing vanilla loot pools remain intact.

## Findings

All 84 supported top-level mob IDs have matching bundled loot-table resources, and those resources load successfully.

The event handler requires `source.isBuiltin()`. If another data pack replaces a vanilla mob's loot table, Fabric may report that source as non-builtin and the head pool will not be added. This reduces compatibility with other loot packs.

The variant tables are large, but evaluation occurs only when the relevant mob dies. Their size is more of a maintenance and reload concern than a regular server-tick concern.

## Recommended changes

1. Decide whether head pools should be added to externally replaced vanilla loot tables.
2. If yes, remove or relax the `source.isBuiltin()` restriction while retaining the `minecraft:entities/*` and supported-mob checks.
3. Generate the supported-mob set and repetitive tables from one source of truth during development.
4. Add loot tests for common, rare, Looting, charged, named, and variant-aware cases.

## Relevant code

- `src/main/java/dev/belandsigh/mobheads/MoreMobHeadsModule.java`
- `src/main/resources/data/more_mob_heads/loot_table/entities/`

## Discussion decisions

- Should this module compose with replacement loot tables from other packs?
- Are the current Vanilla Tweaks drop rates intended to remain exact?

