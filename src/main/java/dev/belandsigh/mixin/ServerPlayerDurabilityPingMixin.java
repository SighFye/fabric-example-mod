package dev.belandsigh.mixin;

import dev.belandsigh.durability.DurabilityPingCooldowns;
import dev.belandsigh.durability.DurabilityPingModule;
import dev.belandsigh.durability.DurabilityPingPreferences;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerDurabilityPingMixin implements DurabilityPingPreferences, DurabilityPingCooldowns {
	// Cooldowns are session-only (not saved); starting a full cooldown in the past lets the first ping through.
	@Unique private long belandsigh$lastHandPing = -DurabilityPingModule.COOLDOWN_TICKS;
	@Unique private long belandsigh$lastArmorPing = -DurabilityPingModule.COOLDOWN_TICKS;
	@Unique private boolean belandsigh$handPings = true;
	@Unique private boolean belandsigh$armorPings = true;
	@Unique private boolean belandsigh$pingSound = true;
	@Unique private DurabilityPingModule.Display belandsigh$display = DurabilityPingModule.Display.SUBTITLE;

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void belandsigh$saveDurabilityPing(ValueOutput output, CallbackInfo ci) {
		ValueOutput settings = output.child("BelAndSighDurabilityPing");
		settings.putBoolean("Hand", belandsigh$handPings);
		settings.putBoolean("Armor", belandsigh$armorPings);
		settings.putBoolean("Sound", belandsigh$pingSound);
		settings.putString("Display", belandsigh$display.name());
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void belandsigh$loadDurabilityPing(ValueInput input, CallbackInfo ci) {
		input.child("BelAndSighDurabilityPing").ifPresent(settings -> {
			belandsigh$handPings = settings.getBooleanOr("Hand", true);
			belandsigh$armorPings = settings.getBooleanOr("Armor", true);
			belandsigh$pingSound = settings.getBooleanOr("Sound", true);
			try {
				belandsigh$display = DurabilityPingModule.Display.valueOf(settings.getStringOr("Display", "SUBTITLE"));
			} catch (IllegalArgumentException ignored) {
				belandsigh$display = DurabilityPingModule.Display.SUBTITLE;
			}
		});
	}

	@Override public boolean belandsigh$handPingsEnabled() { return belandsigh$handPings; }
	@Override public void belandsigh$setHandPingsEnabled(boolean enabled) { belandsigh$handPings = enabled; }
	@Override public boolean belandsigh$armorPingsEnabled() { return belandsigh$armorPings; }
	@Override public void belandsigh$setArmorPingsEnabled(boolean enabled) { belandsigh$armorPings = enabled; }
	@Override public boolean belandsigh$pingSoundEnabled() { return belandsigh$pingSound; }
	@Override public void belandsigh$setPingSoundEnabled(boolean enabled) { belandsigh$pingSound = enabled; }
	@Override public DurabilityPingModule.Display belandsigh$pingDisplay() { return belandsigh$display; }
	@Override public void belandsigh$setPingDisplay(DurabilityPingModule.Display display) { belandsigh$display = display; }
	@Override public long belandsigh$lastHandPing() { return belandsigh$lastHandPing; }
	@Override public void belandsigh$setLastHandPing(long gameTime) { belandsigh$lastHandPing = gameTime; }
	@Override public long belandsigh$lastArmorPing() { return belandsigh$lastArmorPing; }
	@Override public void belandsigh$setLastArmorPing(long gameTime) { belandsigh$lastArmorPing = gameTime; }
}
