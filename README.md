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

## Cauldron conversions

Right-click a water cauldron while holding concrete powder to harden the entire
held stack into concrete. Dirt, coarse dirt, and rooted dirt similarly convert
into mud. Each conversion consumes one level of water. Sneak-right-click bypasses the
conversion and retains normal block placement behavior.

## Fast Leaf Decay

Naturally generated leaves decay rapidly after losing their supporting logs.
Each unsupported leaf waits a random 1–25 ticks, so canopies thin out one leaf
at a time. At most 64 leaves decay per dimension each tick, so felling a large
forest spreads the work out instead of spiking the server. Decay itself uses vanilla leaf behavior, so loot tables and the
`doTileDrops` gamerule are respected.
Player-placed leaves remain protected by their vanilla `persistent` block state.

## Custom Nether Portals

Nether portals can use non-rectangular frames and any mixture of obsidian and
crying obsidian. Vanilla handles rectangular portals directly; irregular frames
use a bounded native flood-fill during activation and validation. Frames require
10 to 84 edge blocks. Frame blocks are controlled by the
`belandsigh:nether_portal_frame_blocks` block tag so datapacks and other mods can
extend the allowed materials.

## Unlock All Recipes

Every recipe is added to a player's recipe book when they join the server.
Online players are also granted any newly loaded recipes after a successful
datapack reload. Mini Blocks relies on this module for recipe-book entries and
ships no discovery advancements of its own.

## Durability Ping

Damageable items in either hand and equipped armor produce an anvil ping and a
subtitle warning when they reach 10% durability. Hand and armor alerts have
independent three-second cooldowns. Each player's settings persist with their
player data and can be changed with `/durabilityping`.

## Furnace XP

Furnaces, blast furnaces, and smokers show their stored experience on a button
below the result slot. Clicking it releases that experience as orbs at the
player, exactly as taking from the result slot would, so hopper-fed furnaces
can be emptied of XP without touching their output. Anyone who can open the
furnace can collect it.

## Ender Chest Always Drops

Ender chests always drop themselves when broken, even without Silk Touch.

## Mini Blocks

Put a supported full-size block into a stonecutter to turn it into eight
matching mini blocks. Mini blocks are textured player heads, can be placed in
all the usual head orientations, and retain their appearance when broken.

Recipes cover more than 200 building, natural, storage, ore, workstation, and
decorative blocks. Because the recipes are shipped as server data, unmodded
clients can craft, place, and see the mini blocks when playing on a modded
server.

## Player Heads

The mod drops a player's head whenever that player dies, including deaths caused
by mobs or the environment. Operators can use `/playerhead <player>` to get the
head of any Java Edition player by name; the named player does not need to be
online or to have joined the server before.

## More Mob Heads

Supported mobs have a chance to drop their custom head when killed by a player.
Drop rates vary by mob and Looting increases the chance. Variant-aware mobs,
including cats, cows, frogs, sheep, villagers, and wolves, drop the matching
variant head. Existing vanilla loot remains unchanged.
