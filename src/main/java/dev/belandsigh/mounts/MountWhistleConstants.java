package dev.belandsigh.mounts;

public final class MountWhistleConstants {
	public static final float USE_COOLDOWN_SECONDS = 3.0F;
	public static final int USE_COOLDOWN_TICKS = Math.round(USE_COOLDOWN_SECONDS * 20.0F);
	public static final int SAFE_ARRIVAL_HORIZONTAL_RADIUS = 8;
	public static final int SAFE_ARRIVAL_VERTICAL_RADIUS = 2;
	public static final int ARRIVAL_PARTICLE_COUNT = 8;

	private MountWhistleConstants() {
	}
}
