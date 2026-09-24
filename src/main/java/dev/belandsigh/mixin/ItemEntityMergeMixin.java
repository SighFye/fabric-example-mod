package dev.belandsigh.mixin;

import dev.belandsigh.death.DeathLocationModule;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps death-drop tracking attached to the items themselves when vanilla merges stacks. A merge
 * empties and discards the smaller entity, so without this a death item absorbed into an untracked
 * neighbour (e.g. a leftover drop of the same type) would look "gone" and end its countdown while
 * the items are still lying on the ground.
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMergeMixin {
	@Inject(method = "merge(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;)V",
		at = @At("TAIL"))
	private static void belandsigh$followMergedDeathItem(ItemEntity toItem, ItemStack toStack, ItemEntity fromItem,
			ItemStack fromStack, CallbackInfo ci) {
		if (fromItem.isRemoved()) {
			DeathLocationModule.onItemMerged(fromItem.getUUID(), toItem.getUUID());
		}
	}
}
