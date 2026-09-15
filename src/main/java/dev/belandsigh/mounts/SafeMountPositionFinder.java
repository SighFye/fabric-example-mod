package dev.belandsigh.mounts;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Searches a small area around the caller using the recalled entity's real bounding box. */
public final class SafeMountPositionFinder {
	private SafeMountPositionFinder() {
	}

	public static Optional<Vec3> find(ServerLevel level, Entity mount, BlockPos origin, MountCategory category) {
		int radius = MountWhistleConstants.SAFE_ARRIVAL_HORIZONTAL_RADIUS;
		int verticalRadius = MountWhistleConstants.SAFE_ARRIVAL_VERTICAL_RADIUS;
		for (Offset offset : horizontalOffsets(radius)) {
			for (int y : verticalOffsets(verticalRadius)) {
				Vec3 destination = new Vec3(origin.getX() + offset.x() + 0.5D,
					origin.getY() + y, origin.getZ() + offset.z() + 0.5D);
				if (isSafe(level, mount, destination, category)) {
					return Optional.of(destination);
				}
			}
		}
		return Optional.empty();
	}

	private static boolean isSafe(ServerLevel level, Entity mount, Vec3 destination, MountCategory category) {
		AABB box = mount.getBoundingBox().move(
			destination.x - mount.getX(), destination.y - mount.getY(), destination.z - mount.getZ());
		// A strider's collision context treats the lava surface as supporting
		// collision. Use neutral physical clearance here, then validate the
		// required water/lava habitat explicitly below.
		if (!level.getWorldBorder().isWithinBounds(box) || !level.noCollision(null, box)) {
			return false;
		}
		return switch (category) {
			case LAND -> hasSafeLandSupport(level, box) && fluidsAreEmpty(level, box);
			case WATER -> lowerBodyIsFluid(level, box, FluidTags.WATER);
			case LAVA -> lowerBodyIsFluid(level, box, FluidTags.LAVA);
		};
	}

	private static boolean hasSafeLandSupport(ServerLevel level, AABB box) {
		int minX = (int) Math.floor(box.minX + 1.0E-4D);
		int maxX = (int) Math.floor(box.maxX - 1.0E-4D);
		int minZ = (int) Math.floor(box.minZ + 1.0E-4D);
		int maxZ = (int) Math.floor(box.maxZ - 1.0E-4D);
		int supportY = (int) Math.floor(box.minY - 1.0E-4D);
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				BlockPos support = new BlockPos(x, supportY, z);
				if (!level.getBlockState(support).isFaceSturdy(level, support, Direction.UP)
						|| level.getFluidState(support).is(FluidTags.LAVA)) {
					return false;
				}
			}
		}
		return true;
	}

	private static boolean fluidsAreEmpty(ServerLevel level, AABB box) {
		return visitBlocks(box, pos -> level.getFluidState(pos).isEmpty());
	}

	private static boolean lowerBodyIsFluid(ServerLevel level, AABB box, TagKey<Fluid> requiredFluid) {
		AABB lowerBody = new AABB(box.minX, box.minY, box.minZ, box.maxX,
			Math.min(box.maxY, box.minY + 1.0D), box.maxZ);
		return visitBlocks(lowerBody, pos -> level.getFluidState(pos).is(requiredFluid));
	}

	private static boolean visitBlocks(AABB box, java.util.function.Predicate<BlockPos> predicate) {
		int minX = (int) Math.floor(box.minX + 1.0E-4D);
		int maxX = (int) Math.floor(box.maxX - 1.0E-4D);
		int minY = (int) Math.floor(box.minY + 1.0E-4D);
		int maxY = (int) Math.floor(box.maxY - 1.0E-4D);
		int minZ = (int) Math.floor(box.minZ + 1.0E-4D);
		int maxZ = (int) Math.floor(box.maxZ - 1.0E-4D);
		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z <= maxZ; z++) {
					if (!predicate.test(new BlockPos(x, y, z))) {
						return false;
					}
				}
			}
		}
		return true;
	}

	static int[] verticalOffsets(int radius) {
		int[] offsets = new int[radius * 2 + 1];
		offsets[0] = 0;
		for (int distance = 1; distance <= radius; distance++) {
			offsets[distance * 2 - 1] = distance;
			offsets[distance * 2] = -distance;
		}
		return offsets;
	}

	static List<Offset> horizontalOffsets(int radius) {
		List<Offset> offsets = new ArrayList<>();
		for (int x = -radius; x <= radius; x++) {
			for (int z = -radius; z <= radius; z++) {
				if (x != 0 || z != 0) {
					offsets.add(new Offset(x, z));
				}
			}
		}
		offsets.sort(Comparator.comparingInt(Offset::distanceSquared));
		return List.copyOf(offsets);
	}

	record Offset(int x, int z) {
		int distanceSquared() {
			return x * x + z * z;
		}
	}
}
