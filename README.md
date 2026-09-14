# BelAndSigh

A Fabric mod for Minecraft 26.2 that replaces the selected Vanilla Tweaks
datapacks with native Java implementations.

## Armor Stands module

Sneak-right-click an armor stand to open its editor. Players with the mod
installed get a native tabbed editor with checkboxes and buttons. Players
without the client mod retain the vanilla container editor as a compatibility
fallback.

Current controls include:

- Base plate, arms, size, gravity, visibility, and name visibility
- Per-axis pose editing for every body part with 1, 5, 15, and 45 degree steps
- Position nudging, block centering, yaw adjustment, and facing the player
- Twenty pose presets and random poses
- Pointing body parts toward the player's eyes or feet
- Pose copy/paste, mirroring, and full-pose flipping
- Main/off-hand and main-hand/head equipment swaps
- Persistent owner locks and invulnerability

The implementation is event-driven. It does not use datapack functions,
scoreboards, marker entities, commands, or a posing book.

## Requirements

- Minecraft Java Edition 26.2
- Java 25
- Fabric Loader 0.19.3 or newer
- Fabric API 0.158.0+26.2 or newer

Build with `./gradlew build`. The distributable JAR is written to `build/libs`.

Armor Stand behavior and presets are based on Vanilla Tweaks Armor Statues
v2.8.23 by Stick God and the Vanilla Tweaks team.
