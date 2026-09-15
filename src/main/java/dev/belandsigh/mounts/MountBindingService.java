package dev.belandsigh.mounts;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/** Assigns first-rider ownership only to eligible mounts without native ownership. */
public final class MountBindingService {
	private MountBindingService() {
	}

	public static boolean bindOnSuccessfulRide(Player rider, Entity mount) {
		MountBindingData binding = (MountBindingData) mount;
		BindingDecision decision = decideBinding(
			MountCategoryService.isEligibleMount(mount),
			mount instanceof OwnableEntity,
			binding.belandsigh$getBoundOwnerUuid().orElse(null),
			rider.getUUID()
		);
		if (decision != BindingDecision.BIND) {
			return false;
		}

		binding.belandsigh$setBoundOwnerUuid(rider.getUUID());
		if (rider instanceof ServerPlayer serverPlayer) {
			MountLocationIndex.record(mount, (net.minecraft.server.level.ServerLevel) serverPlayer.level());
		}
		return true;
	}

	public static UnbindResult unbindOwnedMount(ServerPlayer player, Entity mount) {
		MountBindingData binding = (MountBindingData) mount;
		UUID ownerUuid = binding.belandsigh$getBoundOwnerUuid().orElse(null);
		UnbindResult result = validateUnbindFacts(
			MountCategoryService.isEligibleMount(mount),
			mount instanceof OwnableEntity,
			ownerUuid,
			player.getUUID());
		if (result != UnbindResult.SUCCESS) {
			return result;
		}

		binding.belandsigh$setBoundOwnerUuid(null);
		PlayerMountSelectionService.clearMountFromAllSelections(player, mount.getUUID());
		MountLocationIndex.remove(mount, (net.minecraft.server.level.ServerLevel) mount.level());
		return UnbindResult.SUCCESS;
	}

	static UnbindResult validateUnbindFacts(boolean eligible, boolean nativeOwnership,
			UUID boundOwnerUuid, UUID playerUuid) {
		if (!eligible) {
			return UnbindResult.INELIGIBLE;
		}
		if (nativeOwnership) {
			return UnbindResult.NATIVE_OWNERSHIP;
		}
		return playerUuid.equals(boundOwnerUuid) ? UnbindResult.SUCCESS : UnbindResult.NOT_OWNER;
	}

	static BindingDecision decideBinding(boolean eligible, boolean hasNativeOwnershipSystem,
			UUID boundOwnerUuid, UUID riderUuid) {
		if (!eligible) {
			return BindingDecision.INELIGIBLE;
		}
		if (hasNativeOwnershipSystem) {
			return BindingDecision.NATIVE_OWNERSHIP;
		}
		return boundOwnerUuid == null ? BindingDecision.BIND : BindingDecision.ALREADY_BOUND;
	}

	enum BindingDecision {
		BIND,
		ALREADY_BOUND,
		NATIVE_OWNERSHIP,
		INELIGIBLE
	}

	public enum UnbindResult {
		SUCCESS,
		INELIGIBLE,
		NATIVE_OWNERSHIP,
		NOT_OWNER,
		NOT_FOUND
	}
}
