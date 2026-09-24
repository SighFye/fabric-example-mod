package dev.belandsigh.durability;

/** Per-player game time of the last hand and armor pings; held on the player so nothing needs cleaning up. */
public interface DurabilityPingCooldowns {
	long belandsigh$lastHandPing();

	void belandsigh$setLastHandPing(long gameTime);

	long belandsigh$lastArmorPing();

	void belandsigh$setLastArmorPing(long gameTime);
}
