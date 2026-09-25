# Custom Nether Portals Audit

## Status

Works, but activation and validation need efficiency and lifecycle improvements.

## Current design

- Tagged frame blocks are accepted by vanilla rectangular portal detection.
- Irregular shapes use a bounded flood fill on the two horizontal axes.
- Searches allow up to 1,024 interior blocks and 84 frame blocks.
- Existing irregular portals are revalidated when vanilla would remove a portal block.
- Failed activation searches are cached per dimension/chunk for 20 ticks.

## Findings

### Fire placement cost

Every placed fire can cause two shape searches, each visiting up to 1,108 positions. A chunk cooldown bounds repeated failures, but unrelated fire still pays the first search.

### Portal update cost

When vanilla proposes replacing a portal block with air, the mixin can flood-fill the complete irregular portal. Large shapes therefore make neighbour updates comparatively expensive.

### Failure cache crosses world sessions

`RECENT_FAILURES` is static, keyed only by dimension and chunk, and is not cleared on shutdown. An integrated server opening another world can inherit stale entries. Because game times differ, a stale timestamp can suppress activation longer than the intended 20 ticks.

### Cooldown can suppress a legitimate portal

A failed search caused by unrelated fire prevents another activation search in the same chunk for one second.

## Recommended changes

1. Store failure state per `ServerLevel`, or include server/world identity in the key.
2. Clear the cache on server shutdown.
3. Key failures more locally than an entire chunk, such as ignition position plus axis.
4. Cache activated irregular portal membership or a shape identifier so neighbour validation does not repeatedly flood-fill the whole interior.
5. Add performance tests for maximum-size shapes and mass fire placement.

## Relevant code

- `src/main/java/dev/belandsigh/customportals/CustomNetherPortalModule.java`
- `src/main/java/dev/belandsigh/customportals/CustomPortalShape.java`
- `src/main/java/dev/belandsigh/mixin/BaseFireBlockMixin.java`
- `src/main/java/dev/belandsigh/mixin/NetherPortalBlockMixin.java`
- `src/main/resources/data/minecraft/tags/block/nether_portal_frame.json` (26.3+: adds the frame tag to vanilla's portal frame tag; replaced the old PortalShapeMixin)

## Discussion decisions

- What maximum irregular portal size is actually required?
- Is a one-second activation cooldown acceptable to players?
- Should valid irregular shapes be persisted or reconstructed after reload?

