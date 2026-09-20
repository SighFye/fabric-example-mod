package dev.belandsigh.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphicsExtractor.class)
public abstract class ItemDurabilityDecorationMixin {
	private static final int MAX_DISPLAYED_DURABILITY = 25;

	@Inject(method = "itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At("TAIL"))
	private void belandsigh$drawDurabilityCount(Font font, ItemStack stack, int x, int y, String countLabel, CallbackInfo ci) {
		if (!belandsigh$showsDurabilityCount(stack)) {
			return;
		}
		int remaining = stack.getMaxDamage() - stack.getDamageValue();
		GuiGraphicsExtractor self = (GuiGraphicsExtractor) (Object) this;
		String text = Integer.toString(remaining);
		self.text(font, text, x + 19 - 2 - font.width(text), y + 6 + 3, ARGB.opaque(stack.getBarColor()), true);
	}

	@Inject(method = "itemBar(Lnet/minecraft/world/item/ItemStack;II)V", at = @At("HEAD"), cancellable = true)
	private void belandsigh$hideBarWhenCountShown(ItemStack stack, int x, int y, CallbackInfo ci) {
		if (belandsigh$showsDurabilityCount(stack)) {
			ci.cancel();
		}
	}

	private static boolean belandsigh$showsDurabilityCount(ItemStack stack) {
		if (!stack.isDamageableItem()) {
			return false;
		}
		int remaining = stack.getMaxDamage() - stack.getDamageValue();
		return remaining >= 1 && remaining <= MAX_DISPLAYED_DURABILITY;
	}
}
