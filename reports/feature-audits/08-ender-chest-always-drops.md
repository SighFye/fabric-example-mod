# Ender Chest Always Drops Audit

## Status

Healthy with a data-pack compatibility caveat.

## Current design

The mod replaces the vanilla ender-chest block loot table so a surviving break result is always an ender chest, without requiring Silk Touch.

## Findings

The feature has effectively no Java runtime cost and loads correctly.

Because it replaces the complete `minecraft:blocks/ender_chest` loot table, another mod or data pack targeting that table may override it or be overridden depending on resource-pack order.

The `survives_explosion` condition remains, so “always” means normal breaking without Silk Touch, not guaranteed recovery from explosions.

## Recommended changes

1. Keep the current resource override if simple, deterministic behavior is the priority.
2. Consider a Fabric loot modification or narrowly targeted code hook if composition with other loot packs is important.
3. Clarify explosion behavior in documentation.

## Relevant resource

- `src/main/resources/data/minecraft/loot_table/blocks/ender_chest.json`

## Discussion decisions

- Should explosions also always drop the chest?
- Is interoperability with other ender-chest loot modifications required?

