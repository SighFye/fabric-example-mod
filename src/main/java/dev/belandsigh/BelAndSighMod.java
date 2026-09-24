package dev.belandsigh;

import dev.belandsigh.armorstands.ArmorStandModule;
import dev.belandsigh.armorstands.ArmorStandNetwork;
import dev.belandsigh.cauldrons.CauldronConversionModule;
import dev.belandsigh.customportals.CustomNetherPortalModule;
import dev.belandsigh.death.DeathLocationModule;
import dev.belandsigh.durability.DurabilityPingModule;
import dev.belandsigh.furnacexp.FurnaceXpModule;
import dev.belandsigh.mobheads.MoreMobHeadsModule;
import dev.belandsigh.mounts.MountWhistleModule;
import dev.belandsigh.pets.PetChunkLoadingModule;
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
		ArmorStandNetwork.initialize();
		ArmorStandModule.initialize();
		CauldronConversionModule.initialize();
		CustomNetherPortalModule.initialize();
		DeathLocationModule.initialize();
		DurabilityPingModule.initialize();
		FurnaceXpModule.initialize();
		MoreMobHeadsModule.initialize();
		MountWhistleModule.initialize();
		PetChunkLoadingModule.initialize();
		PlayerHeadsModule.initialize();
		UnlockAllRecipesModule.initialize();
		LOGGER.info("BelAndSigh initialized (Armor Stands, Cauldron Conversions, Custom Nether Portals, Death Item Location, Durability Ping, Ender Chest Always Drops, Fast Leaf Decay, Furnace XP, Mini Blocks, More Mob Heads, Mount Whistle, Pet Chunk Loading, Player Heads, and Unlock All Recipes modules enabled)");
	}
}
