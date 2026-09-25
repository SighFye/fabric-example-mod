package dev.belandsigh.mixin;

import dev.belandsigh.durability.DurabilityPingModule;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public abstract class ItemStackDurabilityPingMixin {
	/** Every player durability loss funnels through here, after Unbreaking has been applied. */
	@Inject(method = "applyDamage", at = @At("HEAD"))
	private void belandsigh$pingOnDamage(int newDamage, ServerPlayer player, Consumer<ItemStack> onBreak, CallbackInfo ci) {
		if (player != null) {
			ItemStack stack = (ItemStack) (Object) this;
			DurabilityPingModule.onDamage(player, stack, stack.getDamageValue(), newDamage);
		}
	}
}
