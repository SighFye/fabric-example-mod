package dev.belandsigh.config;

import java.util.Locale;

public enum Feature {
	ARMOR_STANDS("Armour Stands", "Edit, pose, lock, and reposition armour stands."),
	CAULDRON_CONVERSIONS("Cauldron Conversions", "Convert concrete powder and dirt using water cauldrons."),
	CUSTOM_NETHER_PORTALS("Custom Nether Portals", "Allow non-obsidian and irregular Nether portal frames."),
	DEATH_LOCATION("Death Location", "Track dropped death items with coordinates and a countdown."),
	DURABILITY_PING("Durability Ping", "Warn when equipped tools or armour are close to breaking."),
	ENDER_CHEST_DROPS("Ender Chest Drops", "Make ender chests drop without Silk Touch."),
	FAST_LEAF_DECAY("Fast Leaf Decay", "Make unsupported leaves decay quickly."),
	FURNACE_XP("Furnace XP", "Store and collect furnace experience from the furnace screen."),
	MINI_BLOCKS("Mini Blocks", "Craft decorative miniature blocks in a stonecutter."),
	MORE_MOB_HEADS("More Mob Heads", "Let supported mobs drop decorative heads."),
	MOUNT_WHISTLE("Mount Whistle", "Bind, manage, and recall owned mounts."),
	PET_FOLLOWING("Pet Following", "Help standing cats, wolves, and parrots catch up to owners."),
	PLAYER_HEADS("Player Heads", "Drop player heads on death and enable the player-head command."),
	UNLOCK_ALL_RECIPES("Unlock All Recipes", "Automatically unlock every loaded crafting recipe."),
	DURABILITY_DISPLAY("Durability Display", "Show exact durability values on damaged items.");

	private final String title;
	private final String description;

	Feature(String title, String description) {
		this.title = title;
		this.description = description;
	}

	public String title() {
		return title;
	}

	public String description() {
		return description;
	}

	public String key() {
		return name().toLowerCase(Locale.ROOT);
	}

	public boolean defaultEnabled() {
		return this != MORE_MOB_HEADS;
	}

	public static Feature fromKey(String key) {
		for (Feature feature : values()) {
			if (feature.key().equals(key)) {
				return feature;
			}
		}
		return null;
	}
}
