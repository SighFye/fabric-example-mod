# Mount Whistle Audit

## Status

Strong overall design with one correctness bug and one avoidable server-wide scan.

## Current design

- Mount categories are data-driven through entity-type tags.
- Ownership is resolved through native tame ownership or a persistent first-rider binding.
- A persistent last-known chunk index supports recalling unloaded mounts.
- Unloaded source chunks receive a temporary ticket during recall.
- Destination placement checks the mount's real bounding box and habitat.
- Selections and management data are server-authoritative.

## Findings

### Opening management can clear an unloaded selection

`syncManagementData()` calls `validateSelection()` for every category. `validateSelection()` treats an unresolved entity as `NOT_FOUND`, even when its persistent catalog entry exists, and clears it. Opening the management screen can therefore erase valid unloaded-mount selections.

Validate unloaded UUIDs against `MountLocationIndex`, as `setSelectedMount()` already does.

### Management requests scan every loaded entity

`collectOwnedMounts()` walks every entity in every level before reading the persistent catalog. Client requests are not rate-limited, so a modified client can repeatedly trigger global scans.

The load/unload index should already contain the required data. Remove the refresh scan and update the index at specific events such as ownership, binding, renaming, category changes, and entity movement/unload.

### Safe-position offsets are rebuilt

The same horizontal offsets are allocated, sorted, and copied for every recall. Cache the immutable list for the configured radius.

### Broad base-entity persistence mixin

Mount binding data is injected into every entity. Most entities never use it, yet all entity loads execute the optional child-data lookup. Add an eligibility fast path or consider a sparse Fabric data attachment.

### Camel behavior is globally changed

The whistle feature changes vanilla camel taming so camel ownership can work. This is a significant gameplay change and should be explicitly documented or separated into configuration.

## Recommended changes

1. Preserve unloaded selections using catalog validation.
2. Remove the management-screen entity scan.
3. Rate-limit management-data requests.
4. Cache safe-position offsets.
5. Reduce unused mount-binding persistence work.
6. Document or configure the camel taming change.

## Relevant code

- `src/main/java/dev/belandsigh/mounts/MountNetworking.java`
- `src/main/java/dev/belandsigh/mounts/PlayerMountSelectionService.java`
- `src/main/java/dev/belandsigh/mounts/MountLocationIndex.java`
- `src/main/java/dev/belandsigh/mounts/MountRecallService.java`
- `src/main/java/dev/belandsigh/mounts/SafeMountPositionFinder.java`
- `src/main/java/dev/belandsigh/mixin/EntityMountBindingMixin.java`
- `src/main/java/dev/belandsigh/mixin/CamelTamingMixin.java`

## Existing verification

All 61 current automated tests cover Mount Whistle services and resources and are passing. Integration coverage is still needed for real chunk loading, networking abuse, and persistence across a server restart.

## Discussion decisions

- Should unloaded mounts remain selectable indefinitely?
- Should recall work across dimensions?
- Is altered camel taming part of the intended public feature?

