package dev.belandsigh.durability;

public interface DurabilityPingPreferences {
	boolean belandsigh$handPingsEnabled();

	void belandsigh$setHandPingsEnabled(boolean enabled);

	boolean belandsigh$armorPingsEnabled();

	void belandsigh$setArmorPingsEnabled(boolean enabled);

	boolean belandsigh$pingSoundEnabled();

	void belandsigh$setPingSoundEnabled(boolean enabled);

	DurabilityPingModule.Display belandsigh$pingDisplay();

	void belandsigh$setPingDisplay(DurabilityPingModule.Display display);
}
