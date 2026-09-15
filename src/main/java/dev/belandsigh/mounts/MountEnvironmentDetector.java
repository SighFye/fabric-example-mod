package dev.belandsigh.mounts;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Server-authoritative selection of the mount slot appropriate to the player's surroundings. */
public final class MountEnvironmentDetector {
	private MountEnvironmentDetector() {
	}

	public static MountCategory detect(Player player) {
		Level level = player.level();
		BlockPos feet = BlockPos.containing(player.getX(), player.getBoundingBox().minY + 0.1D, player.getZ());
		boolean feetLevelLava = false;
		boolean feetLevelWater = false;
		boolean surfaceLevelLava = false;
		boolean surfaceLevelWater = false;

		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++) {
				if (x == 0 && z == 0) {
					continue;
				}
				// Beside a typical shoreline, the player's feet are in the air block
				// above the fluid surface. Check that layer and the one immediately
				// below it while keeping the search strictly horizontal and local.
				BlockPos adjacent = feet.offset(x, 0, z);
				var feetLevelFluid = level.getFluidState(adjacent);
				var surfaceLevelFluid = level.getFluidState(adjacent.below());
				feetLevelLava |= feetLevelFluid.is(FluidTags.LAVA);
				feetLevelWater |= feetLevelFluid.is(FluidTags.WATER);
				surfaceLevelLava |= surfaceLevelFluid.is(FluidTags.LAVA);
				surfaceLevelWater |= surfaceLevelFluid.is(FluidTags.WATER);
			}
		}

		return chooseFromAdjacentLayers(player.isInLava(), player.isInWater(),
			feetLevelLava, feetLevelWater, surfaceLevelLava, surfaceLevelWater);
	}

	static MountCategory choose(boolean standingLava, boolean standingWater,
			boolean adjacentLava, boolean adjacentWater) {
		if (standingLava) {
			return MountCategory.LAVA;
		}
		if (standingWater) {
			return MountCategory.WATER;
		}
		if (adjacentLava) {
			return MountCategory.LAVA;
		}
		if (adjacentWater) {
			return MountCategory.WATER;
		}
		return MountCategory.LAND;
	}

	static MountCategory chooseFromAdjacentLayers(boolean standingLava, boolean standingWater,
			boolean feetLevelLava, boolean feetLevelWater, boolean surfaceLevelLava, boolean surfaceLevelWater) {
		return choose(standingLava, standingWater,
			feetLevelLava || surfaceLevelLava, feetLevelWater || surfaceLevelWater);
	}
}
