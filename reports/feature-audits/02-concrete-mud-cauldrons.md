# Concrete & Mud Cauldrons Audit

## Status

Healthy. No meaningful performance problem was found.

## Current design

- A `UseBlockCallback` checks for a water cauldron.
- A constant conversion map covers all concrete-powder colours plus dirt, coarse dirt, and rooted dirt.
- The server transmutes the entire held stack and emits sound and particles.
- Sneaking bypasses conversion.
- Water is intentionally not consumed.

## Findings

The implementation is server-authoritative, constant-time, and has no recurring tick cost. Its whole-stack conversion and reusable water match the README.

The immutable map contains only 19 entries, so replacing it with a specialized collection would not provide a meaningful benefit.

## Recommended changes

1. Keep the current runtime design.
2. Add interaction tests covering both hands, creative mode, sneaking, and non-water cauldrons.
3. Document whether custom stack components are intentionally retained by `transmuteCopy`.

## Relevant code

- `src/main/java/dev/belandsigh/cauldrons/CauldronConversionModule.java`

## Discussion decisions

- Should conversion continue to process the entire stack?
- Should water remain reusable, or should each conversion consume a cauldron level?

