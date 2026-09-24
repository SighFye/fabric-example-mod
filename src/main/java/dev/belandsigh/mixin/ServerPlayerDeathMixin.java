package dev.belandsigh.mixin;

import dev.belandsigh.death.DeathLocationModule;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ends a death-drop capture once {@code die} has fully run, by which point any drops held back by
 * other mods (see {@link ServerLevelAddEntityMixin}) have been released into the world.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerDeathMixin {
	@Inject(method = "die", at = @At("TAIL"))
	private void belandsigh$registerDeathDrop(DamageSource source, CallbackInfo ci) {
		DeathLocationModule.finishDeathDropCapture((ServerPlayer) (Object) this);
	}
}
