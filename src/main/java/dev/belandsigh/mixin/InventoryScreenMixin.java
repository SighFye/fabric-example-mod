package dev.belandsigh.mixin;

import dev.belandsigh.client.MountManagementScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends Screen {
	private static final int VANILLA_INVENTORY_WIDTH = 176;
	private static final int VANILLA_INVENTORY_HEIGHT = 166;

	protected InventoryScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void belandsigh$addMountManagementButton(CallbackInfo ci) {
		int inventoryRight = (width + VANILLA_INVENTORY_WIDTH) / 2;
		int inventoryTop = (height - VANILLA_INVENTORY_HEIGHT) / 2;
		Component label = Component.literal("M");
		addRenderableWidget(Button.builder(label, button ->
			minecraft.setScreenAndShow(new MountManagementScreen((Screen) (Object) this)))
			.bounds(inventoryRight + 4, inventoryTop + 4, 20, 20)
			.tooltip(Tooltip.create(Component.translatable(
				"screen.belandsigh.mount_management.inventory_button")))
			.build());
	}
}
