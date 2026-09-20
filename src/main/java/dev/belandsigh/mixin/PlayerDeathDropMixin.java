package dev.belandsigh.mixin;

import dev.belandsigh.death.DeathDropModule;
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
 * Marks a death-drop despawn location whenever a dying player's items are about to hit the ground.
 * {@code dropEquipment} is the sole call site for a player's death-inventory drop, so entering it
 * (with keepInventory off) reliably means items are landing at the player's current position -
 * regardless of which code path actually spawns the item entities.
 */
@Mixin(Player.class)
public abstract class PlayerDeathDropMixin {
	@Inject(method = "dropEquipment", at = @At("HEAD"))
	private void belandsigh$registerDeathDrop(ServerLevel level, CallbackInfo ci) {
		boolean keepInventory = level.getGameRules().get(GameRules.KEEP_INVENTORY);
		if (!keepInventory && (Object) this instanceof ServerPlayer player) {
			DeathLocationModule.registerDeath(player, level, player.blockPosition(), DeathDropModule.despawnTicks());
		}
	}
}
