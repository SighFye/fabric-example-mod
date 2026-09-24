package dev.belandsigh.mixin;

import dev.belandsigh.death.DeathLocationModule;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Starts capturing a player's death drops. {@code dropEquipment} is the sole call site for a
 * player's death-inventory drop, so entering it (with keepInventory off) marks the start of the
 * death's drops; {@link ServerLevelAddEntityMixin} records each item entity as it enters the world
 * and {@link ServerPlayerDeathMixin} registers the death once the whole death has been processed.
 */
@Mixin(Player.class)
public abstract class PlayerDeathDropMixin {
	@Inject(method = "dropEquipment", at = @At("HEAD"))
	private void belandsigh$beginDeathDropCapture(ServerLevel level, CallbackInfo ci) {
		if (!level.getGameRules().get(GameRules.KEEP_INVENTORY) && (Object) this instanceof ServerPlayer player) {
			DeathLocationModule.beginDeathDropCapture(player, level, player.blockPosition());
		}
	}
}
