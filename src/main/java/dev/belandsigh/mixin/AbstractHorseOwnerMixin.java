package dev.belandsigh.mixin;

import dev.belandsigh.config.Feature;
import dev.belandsigh.config.ModSettings;
import dev.belandsigh.mounts.MountLocationIndex;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Catalogs a horse-like mount (including camels) as soon as it is tamed. */
@Mixin(AbstractHorse.class)
public abstract class AbstractHorseOwnerMixin {
	@Inject(method = "setOwner", at = @At("TAIL"))
	private void belandsigh$refreshMountCatalog(LivingEntity owner, CallbackInfo ci) {
		if (ModSettings.enabled(Feature.MOUNT_WHISTLE)) {
			MountLocationIndex.refresh((AbstractHorse) (Object) this);
		}
	}
}
