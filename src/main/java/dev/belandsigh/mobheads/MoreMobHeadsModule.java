package dev.belandsigh.mobheads;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;

import java.util.Set;

public final class MoreMobHeadsModule {
	private static final String ENTITY_TABLE_PREFIX = "entities/";
	private static final Set<String> SUPPORTED_MOBS = Set.of(
		"allay", "armadillo", "axolotl", "bat", "bee", "blaze", "bogged", "breeze",
		"camel", "camel_husk", "cat", "cave_spider", "chicken", "cod", "copper_golem",
		"cow", "creaking", "creeper", "dolphin", "donkey", "drowned", "elder_guardian",
		"enderman", "endermite", "evoker", "fox", "frog", "ghast", "glow_squid", "goat",
		"guardian", "happy_ghast", "hoglin", "horse", "husk", "illusioner", "iron_golem",
		"llama", "magma_cube", "mooshroom", "mule", "nautilus", "ocelot", "panda",
		"parched", "parrot", "phantom", "pig", "piglin_brute", "pillager", "polar_bear",
		"pufferfish", "rabbit", "ravager", "salmon", "sheep", "shulker", "silverfish",
		"skeleton_horse", "slime", "sniffer", "snow_golem", "spider", "squid", "stray",
		"strider", "sulfur_cube", "tadpole", "trader_llama", "tropical_fish", "turtle", "vex", "villager",
		"vindicator", "wandering_trader", "warden", "witch", "wither", "wolf", "zoglin",
		"zombie_horse", "zombie_nautilus", "zombie_villager", "zombified_piglin"
	);

	private MoreMobHeadsModule() {
	}

	public static void initialize() {
		LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
			Identifier id = key.identifier();
			if (!source.isBuiltin() || !id.getNamespace().equals("minecraft")
				|| !id.getPath().startsWith(ENTITY_TABLE_PREFIX)) {
				return;
			}

			String mob = id.getPath().substring(ENTITY_TABLE_PREFIX.length());
			if (!SUPPORTED_MOBS.contains(mob)) {
				return;
			}

			ResourceKey<LootTable> headTable = ResourceKey.create(
				Registries.LOOT_TABLE,
				Identifier.fromNamespaceAndPath("more_mob_heads", ENTITY_TABLE_PREFIX + mob)
			);
			tableBuilder.withPool(LootPool.lootPool().add(NestedLootTable.lootTableReference(headTable)));
		});
	}
}
