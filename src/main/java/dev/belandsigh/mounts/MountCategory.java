package dev.belandsigh.mounts;

public enum MountCategory {
	LAND("land_mounts"),
	WATER("water_mounts"),
	LAVA("lava_mounts");

	private final String tagPath;

	MountCategory(String tagPath) {
		this.tagPath = tagPath;
	}

	public String tagPath() {
		return tagPath;
	}
}
