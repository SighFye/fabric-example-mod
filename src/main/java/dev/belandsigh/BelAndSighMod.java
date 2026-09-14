package dev.belandsigh;

import dev.belandsigh.armorstands.ArmorStandModule;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BelAndSighMod implements ModInitializer {
	public static final String MOD_ID = "belandsigh";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ArmorStandModule.initialize();
		LOGGER.info("BelAndSigh initialized (Armor Stands module enabled)");
	}
}
