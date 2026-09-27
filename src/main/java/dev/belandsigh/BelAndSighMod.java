package dev.belandsigh;

import dev.belandsigh.armorstands.ArmorStandModule;
import dev.belandsigh.armorstands.ArmorStandNetwork;
import dev.belandsigh.cauldrons.CauldronConversionModule;
import dev.belandsigh.customportals.CustomNetherPortalModule;
import dev.belandsigh.config.ModSettings;
import dev.belandsigh.config.MiniBlocksResourceCondition;
import dev.belandsigh.config.SettingsNetwork;
import dev.belandsigh.death.DeathLocationModule;
import dev.belandsigh.durability.DurabilityPingModule;
import dev.belandsigh.enderchest.EnderChestDropsModule;
import dev.belandsigh.furnacexp.FurnaceXpModule;
import dev.belandsigh.leaves.FastLeafDecayModule;
import dev.belandsigh.mobheads.MoreMobHeadsModule;
import dev.belandsigh.mounts.MountWhistleModule;
import dev.belandsigh.pets.PetFollowModule;
import dev.belandsigh.playerheads.PlayerHeadsModule;
import dev.belandsigh.recipes.UnlockAllRecipesModule;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BelAndSighMod implements ModInitializer {
	public static final String MOD_ID = "belandsigh";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModSettings.load();
		MiniBlocksResourceCondition.initialize();
		SettingsNetwork.initialize();
		ArmorStandNetwork.initialize();
		ArmorStandModule.initialize();
		CauldronConversionModule.initialize();
		CustomNetherPortalModule.initialize();
		DeathLocationModule.initialize();
		DurabilityPingModule.initialize();
		EnderChestDropsModule.initialize();
		FastLeafDecayModule.initialize();
		FurnaceXpModule.initialize();
		MoreMobHeadsModule.initialize();
		MountWhistleModule.initialize();
		PetFollowModule.initialize();
		PlayerHeadsModule.initialize();
		UnlockAllRecipesModule.initialize();
		LOGGER.info("BelAndSigh initialized; feature states loaded from config/belandsigh.json");
	}
}
