package dev.belandsigh.enderchest;

import java.util.Optional;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

/** Ender chests always drop themselves: no Silk Touch needed, and explosions don't destroy them. */
public final class EnderChestDropsModule {
	private EnderChestDropsModule() {
	}

	public static void initialize() {
		LootTableEvents.REPLACE.register((key, original, source, registries) -> {
			// Only swap out vanilla's own table: a data pack or mod that supplies its own ender chest table wins,
			// and other mods' LootTableEvents.MODIFY listeners still run on top of ours.
			if (source != LootTableSource.VANILLA || !Blocks.ENDER_CHEST.getLootTable().equals(Optional.of(key))) {
				return null;
			}
			return LootTable.lootTable()
					.setParamSet(LootContextParamSets.BLOCK)
					.withPool(LootPool.lootPool()
							.setRolls(ConstantValue.exactly(1))
							.add(LootItem.lootTableItem(Items.ENDER_CHEST)))
					.setRandomSequence(key.identifier())
					.build();
		});
	}
}
