package dev.belandsigh.mixin;

import dev.belandsigh.furnacexp.FurnaceXpModule;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Furnace menus inherit clickMenuButton from AbstractContainerMenu without overriding it, so the
// hook has to live on the base class and filter down to furnaces inside FurnaceXpModule.
@Mixin(AbstractContainerMenu.class)
public abstract class ContainerMenuButtonMixin {
	@Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true)
	private void belandsigh$collectFurnaceXp(Player player, int buttonId, CallbackInfoReturnable<Boolean> cir) {
		if (FurnaceXpModule.handleMenuButton((AbstractContainerMenu) (Object) this, player, buttonId)) {
			cir.setReturnValue(true);
		}
	}
}
