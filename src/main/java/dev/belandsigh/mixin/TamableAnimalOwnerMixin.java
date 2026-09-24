package dev.belandsigh.mixin;

import dev.belandsigh.mounts.MountLocationIndex;
import dev.belandsigh.pets.PetFollowModule;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps the pet follow index and mount catalog in step when an already-loaded animal is tamed or changes owner. */
@Mixin(TamableAnimal.class)
public abstract class TamableAnimalOwnerMixin {
	@Inject(method = "setOwner", at = @At("TAIL"))
	private void belandsigh$reindexOnSetOwner(LivingEntity owner, CallbackInfo ci) {
		PetFollowModule.onOwnerChanged((TamableAnimal) (Object) this);
		MountLocationIndex.refresh((TamableAnimal) (Object) this);
	}

	@Inject(method = "setOwnerReference", at = @At("TAIL"))
	private void belandsigh$reindexOnSetOwnerReference(EntityReference<LivingEntity> owner, CallbackInfo ci) {
		PetFollowModule.onOwnerChanged((TamableAnimal) (Object) this);
		MountLocationIndex.refresh((TamableAnimal) (Object) this);
	}
}
