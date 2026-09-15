package dev.belandsigh.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla's Camel hardcodes isTamed() to true and never registers the
 * AbstractHorse taming goal, so anyone can saddle and ride one for free.
 * This restores real taming: mounting an untamed camel has a temper-based
 * chance each roll to either tame it (owned by the rider) or buck the rider
 * off, exactly like AbstractHorse's RunAroundLikeCrazyGoal does for horses.
 * AbstractHorseAccessor invokes the protected tame flag method on its declaring
 * class, avoiding an inherited shadow that production Mixin cannot resolve.
 */
@Mixin(Camel.class)
public abstract class CamelTamingMixin {
	private static final int FLAG_TAME = 2;
	private static final int TAME_ROLL_INTERVAL_TICKS = 50;
	private static final int TEMPER_GAIN_PER_FAILED_ROLL = 5;

	@Inject(method = "isTamed", at = @At("HEAD"), cancellable = true)
	private void belandsigh$useRealTameFlag(CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(((AbstractHorseAccessor) this).belandsigh$getFlag(FLAG_TAME));
	}

	@Inject(method = "customServerAiStep", at = @At("TAIL"))
	private void belandsigh$rollTaming(ServerLevel level, CallbackInfo ci) {
		Camel self = (Camel) (Object) this;
		if (self.isTamed() || !self.isVehicle() || self.isMobControlled()) {
			return;
		}
		if (self.getRandom().nextInt(TAME_ROLL_INTERVAL_TICKS) != 0) {
			return;
		}
		if (!(self.getFirstPassenger() instanceof Player player)) {
			return;
		}

		int temper = self.getTemper();
		int maxTemper = self.getMaxTemper();
		if (maxTemper > 0 && self.getRandom().nextInt(maxTemper) < temper) {
			self.tameWithName(player);
			return;
		}

		self.modifyTemper(TEMPER_GAIN_PER_FAILED_ROLL);
		self.ejectPassengers();
		self.makeMad();
		level.broadcastEntityEvent(self, (byte) 6);
	}
}
