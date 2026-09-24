# BelAndSigh Feature Audit Index

These reports split the September 2026 audit into one discussion document per feature. The findings are based on source review, a clean Gradle build, 61 passing tests, and a successful dedicated-server startup.

No finding in these documents has been implemented yet.

| Feature | Status | Report |
| --- | --- | --- |
| Armour Statues | Good; minor correctness fixes | [01-armour-statues.md](01-armour-statues.md) |
| Concrete & Mud Cauldrons | Healthy | [02-concrete-mud-cauldrons.md](02-concrete-mud-cauldrons.md) |
| Fast Leaf Decay | Correct but tick-heavy | [03-fast-leaf-decay.md](03-fast-leaf-decay.md) |
| Unlock All Recipes | Healthy | [04-unlock-all-recipes.md](04-unlock-all-recipes.md) |
| More Mob Heads | Generally good | [05-more-mob-heads.md](05-more-mob-heads.md) |
| Custom Nether Portals | Needs optimization | [06-custom-nether-portals.md](06-custom-nether-portals.md) |
| Mini Blocks | Works; data can be reduced | [07-mini-blocks.md](07-mini-blocks.md) |
| Ender Chest Always Drops | Healthy; compatibility caveat | [08-ender-chest-always-drops.md](08-ender-chest-always-drops.md) |
| Durability Ping | Needs correctness and allocation fixes | [09-durability-ping.md](09-durability-ping.md) |
| Better Pet Following | Major performance concern | [10-better-pet-following.md](10-better-pet-following.md) |
| Mount Whistle | Good structure; two important faults | [11-mount-whistle.md](11-mount-whistle.md) |
| Death Location Timer | Release blocker | [12-death-location-timer.md](12-death-location-timer.md) |

## Suggested discussion order

1. Death Location Timer
2. Better Pet Following
3. Mount Whistle
4. Durability Ping
5. Custom Nether Portals
6. Fast Leaf Decay
7. Mini Blocks
8. Armour Statues
9. More Mob Heads
10. Ender Chest Always Drops
11. Unlock All Recipes
12. Concrete & Mud Cauldrons

## Verification baseline

- `gradlew clean test build`: successful
- Automated tests: 61 passed, 0 failed
- Dedicated server: starts and stops successfully
- Loaded resources: 1,790 recipes and 1,893 advancements
- Current automated tests cover Mount Whistle only
- One compile warning remains for deprecated API usage in `CustomPortalShape`

