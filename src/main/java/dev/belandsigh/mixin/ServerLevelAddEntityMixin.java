package dev.belandsigh.mixin;

import dev.belandsigh.death.DeathLocationModule;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Records item entities as they actually enter the world during a death-drop capture. Hooking the
 * world insertion rather than {@code LivingEntity#drop} matters: libraries such as PuzzlesLib hold
 * death drops back in a list (Forge-style drop capture) and only add them to the world after the
 * death-loot code has finished, so {@code drop} alone never sees them.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelAddEntityMixin {
	@Inject(method = "addFreshEntity", at = @At("RETURN"))
	private void belandsigh$captureDeathDrop(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ() && entity instanceof ItemEntity item) {
			DeathLocationModule.captureDeathDrop((ServerLevel) (Object) this, item);
		}
	}
}
