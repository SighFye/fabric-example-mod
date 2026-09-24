package dev.belandsigh.mixin;

import dev.belandsigh.client.FurnaceXpClientState;
import dev.belandsigh.furnacexp.FurnaceXpMath;
import dev.belandsigh.furnacexp.FurnaceXpModule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

@Mixin(AbstractFurnaceScreen.class)
public abstract class FurnaceScreenMixin extends Screen {
	// Centred under the 26x26 result slot frame (local x 111-137, y 30-56) and clear of the
	// "Inventory" label at local y 72. Like the inventory button, leftPos moves when the recipe
	// book is toggled, so the position is refreshed every frame rather than only in init().
	private static final int BUTTON_LOCAL_X = 102;
	private static final int BUTTON_LOCAL_Y = 58;
	private static final int BUTTON_WIDTH = 44;
	private static final int BUTTON_HEIGHT = 12;

	private Button belandsigh$collectXpButton;
	private int belandsigh$shownTenths = -1;

	protected FurnaceScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void belandsigh$addCollectXpButton(CallbackInfo ci) {
		belandsigh$shownTenths = -1;
		belandsigh$collectXpButton = Button.builder(Component.empty(), button -> {
				int containerId = belandsigh$containerId();
				minecraft.gameMode.handleInventoryButtonClick(containerId, FurnaceXpModule.COLLECT_BUTTON_ID);
				FurnaceXpClientState.clear(containerId);
			})
			.bounds(belandsigh$buttonX(), belandsigh$buttonY(), BUTTON_WIDTH, BUTTON_HEIGHT)
			.build();
		belandsigh$collectXpButton.visible = false;
		addRenderableWidget(belandsigh$collectXpButton);
	}

	@Inject(method = "extractBackground", at = @At("HEAD"))
	private void belandsigh$refreshCollectXpButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (belandsigh$collectXpButton == null) {
			return;
		}
		belandsigh$collectXpButton.setPosition(belandsigh$buttonX(), belandsigh$buttonY());

		// Stays hidden until the server reports a value, so an unmodded server shows nothing.
		int tenths = FurnaceXpClientState.tenthsFor(belandsigh$containerId());
		belandsigh$collectXpButton.visible = tenths >= 0;
		if (tenths < 0 || tenths == belandsigh$shownTenths) {
			return;
		}
		belandsigh$shownTenths = tenths;
		belandsigh$collectXpButton.active = tenths > 0;
		belandsigh$collectXpButton.setMessage(Component.translatable(
			"screen.belandsigh.furnace_xp.button", FurnaceXpMath.format(tenths)));
		belandsigh$collectXpButton.setTooltip(Tooltip.create(belandsigh$tooltip(tenths)));
	}

	private Component belandsigh$tooltip(int tenths) {
		if (tenths == 0) {
			return Component.translatable("screen.belandsigh.furnace_xp.tooltip_empty");
		}
		LocalPlayer player = minecraft.player;
		double levels = player == null ? 0.0 : FurnaceXpMath.levelsGained(
			player.experienceLevel, player.experienceProgress, tenths / 10.0);
		return Component.translatable("screen.belandsigh.furnace_xp.tooltip",
			FurnaceXpMath.format(tenths), String.format(Locale.ROOT, "%.1f", levels));
	}

	private int belandsigh$containerId() {
		return ((AbstractContainerScreen<?>) (Object) this).getMenu().containerId;
	}

	private int belandsigh$buttonX() {
		return ((AbstractContainerScreenAccessor) (Object) this).belandsigh$getLeftPos() + BUTTON_LOCAL_X;
	}

	private int belandsigh$buttonY() {
		return ((AbstractContainerScreenAccessor) (Object) this).belandsigh$getTopPos() + BUTTON_LOCAL_Y;
	}
}
