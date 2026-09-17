package dev.belandsigh.mixin;

import dev.belandsigh.client.MountManagementScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
	// Recipe book toggle sits at (leftPos + 104, height/2 - 22), 20x18, with empty
	// space to its right before the crafting result slot at local x=154. leftPos
	// itself shifts right whenever the recipe book is opened/closed, so our
	// button has to track it every frame rather than being placed once in init().
	private static final int RECIPE_BOOK_BUTTON_LOCAL_X = 104;
	private static final int RECIPE_BOOK_BUTTON_WIDTH = 20;
	private static final int RECIPE_BOOK_BUTTON_GAP = 2;

	private Button belandsigh$mountManagementButton;

	protected InventoryScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void belandsigh$addMountManagementButton(CallbackInfo ci) {
		Component label = Component.literal("M");
		belandsigh$mountManagementButton = Button.builder(label, button ->
			minecraft.setScreenAndShow(new MountManagementScreen((Screen) (Object) this)))
			.bounds(belandsigh$buttonX(), belandsigh$buttonY(), 20, 18)
			.tooltip(Tooltip.create(Component.translatable(
				"screen.belandsigh.mount_management.inventory_button")))
			.build();
		addRenderableWidget(belandsigh$mountManagementButton);
	}

	@Inject(method = "extractRenderState", at = @At("HEAD"))
	private void belandsigh$followRecipeBookButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (belandsigh$mountManagementButton != null) {
			belandsigh$mountManagementButton.setPosition(belandsigh$buttonX(), belandsigh$buttonY());
		}
	}

	private int belandsigh$buttonX() {
		int leftPos = ((AbstractContainerScreenAccessor) (Object) this).belandsigh$getLeftPos();
		return leftPos + RECIPE_BOOK_BUTTON_LOCAL_X + RECIPE_BOOK_BUTTON_WIDTH + RECIPE_BOOK_BUTTON_GAP;
	}

	private int belandsigh$buttonY() {
		return height / 2 - 22;
	}
}
