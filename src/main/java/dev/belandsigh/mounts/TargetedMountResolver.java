package dev.belandsigh.mounts;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** Resolves the compatible mount under the server player's crosshair without trusting client input. */
public final class TargetedMountResolver {
	private TargetedMountResolver() {
	}

	public static Optional<Entity> find(ServerPlayer player) {
		int viewDistance = ((ServerLevel) player.level()).getServer().getPlayerList().getViewDistance();
		double range = rangeForViewDistance(viewDistance);
		Vec3 from = player.getEyePosition();
		Vec3 ray = player.getLookAngle().scale(range);
		Vec3 to = from.add(ray);

		HitResult blockHit = player.pick(range, 1.0F, false);
		if (blockHit.getType() != HitResult.Type.MISS) {
			to = blockHit.getLocation();
		}

		var hit = ProjectileUtil.getEntityHitResult(
			player,
			from,
			to,
			player.getBoundingBox().expandTowards(to.subtract(from)).inflate(1.0D),
			entity -> entity.isAlive() && entity.isPickable() && MountCategoryService.isEligibleMount(entity),
			from.distanceToSqr(to)
		);
		return Optional.ofNullable(hit).map(result -> result.getEntity());
	}

	/** Covers the far corner of every chunk the server can send around this player. */
	static double rangeForViewDistance(int viewDistanceChunks) {
		int loadedRadius = Math.max(2, viewDistanceChunks) + 1;
		return Math.ceil(loadedRadius * 16.0D * Math.sqrt(2.0D));
	}
}
