# Done

# Unlock All Recipes Audit

## Status

Healthy with low, infrequent cost.

## Current design

- All recipes are awarded when a player joins.
- After a successful data-pack reload, all recipes are awarded to every online player.
- There is no recurring tick handler.

## Findings

The work is proportional to the number of recipes and online players, but occurs only at login and reload. Minecraft's recipe-award path handles recipes that are already known, so an additional custom cache is unlikely to justify its complexity.

The feature also makes Mini Blocks' individual recipe-discovery advancements redundant.

## Recommended changes

1. Keep the current event-driven implementation.
2. Remove redundant Mini Blocks discovery advancements if this feature will always remain enabled.
3. Add a login/reload integration test.

## Relevant code

- `src/main/java/dev/belandsigh/recipes/UnlockAllRecipesModule.java`

## Discussion decisions

- Will Unlock All Recipes always be enabled?
- If features become configurable, should Mini Blocks restore normal recipe discovery when this module is disabled?

