# Mini Blocks Audit

## Status

Working and compatible with unmodded clients, but carrying redundant advancement data.

## Current design

- 204 vanilla stonecutting recipes create textured player heads.
- Each result includes its texture profile and display name in vanilla item components.
- A replacement player-head block loot table copies block-entity components when broken.
- Because all recipe types and outputs are vanilla, unmodded clients can use the feature.

## Findings

All 204 recipes parse and load successfully.

There are 201 individual recipe-discovery advancements plus a root advancement. They are redundant while Unlock All Recipes always awards every recipe directly.

Three recipes do not have matching discovery advancements:

- `pale_moss`
- `pale_oak_log`
- `pale_oak_planks`

This mismatch is currently hidden by Unlock All Recipes.

The 204 recipe files are justified because every output has distinct components. Replacing them with a custom recipe serializer could reduce files, but would risk losing unmodded-client compatibility.

## Recommended changes

1. If Unlock All Recipes is permanently enabled, remove the Mini Blocks recipe-discovery advancement tree.
2. If modules will be independently configurable, retain the advancements and add the three missing files.
3. Generate recipes from a development-time source list to prevent ingredient, texture, and naming drift.
4. Add validation for duplicate ingredients and duplicate textures.

## Relevant resources

- `src/main/resources/data/mini_blocks/recipe/`
- `src/main/resources/data/mini_blocks/advancement/recipes/`
- `src/main/resources/data/minecraft/loot_table/blocks/player_head.json`

## Discussion decisions

- Must Unlock All Recipes remain permanently enabled?
- Are multiple visual results from one ingredient, such as lit/unlit lamps, intentional stonecutter choices?

