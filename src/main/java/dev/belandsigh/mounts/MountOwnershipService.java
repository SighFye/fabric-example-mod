package dev.belandsigh.mounts;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.OwnableEntity;

import java.util.UUID;
import java.util.Optional;

/** Resolves native tame ownership and Mount Whistle binding through one API. */
public final class MountOwnershipService {
	private MountOwnershipService() {
	}

	public static boolean doesPlayerOwnMount(ServerPlayer player, Entity entity) {
		if (!MountCategoryService.isEligibleMount(entity)) {
			return false;
		}
		return getOwnerUuid(entity).filter(player.getUUID()::equals).isPresent();
	}

	public static Optional<UUID> getOwnerUuid(Entity entity) {
		if (!MountCategoryService.isEligibleMount(entity)) {
			return Optional.empty();
		}
		if (entity instanceof OwnableEntity ownable) {
			EntityReference<?> owner = ownable.getOwnerReference();
			return Optional.ofNullable(owner == null ? null : owner.getUUID());
		}

		return ((MountBindingData) entity).belandsigh$getBoundOwnerUuid();
	}

	static boolean isOwnedBy(UUID playerUuid, UUID nativeOwnerUuid, UUID boundOwnerUuid,
			boolean hasNativeOwnershipSystem) {
		UUID effectiveOwner = hasNativeOwnershipSystem ? nativeOwnerUuid : boundOwnerUuid;
		return playerUuid.equals(effectiveOwner);
	}
}
