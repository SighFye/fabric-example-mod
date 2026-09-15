package dev.belandsigh.mounts;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Optional;
import java.util.UUID;

/** Owns all reads, writes, validation, and lazy cleanup of player mount slots. */
public final class PlayerMountSelectionService {
	private PlayerMountSelectionService() {
	}

	public static Optional<UUID> getSelectedMountUuid(ServerPlayer player, MountCategory category) {
		return data(player).belandsigh$getSelectedMountUuid(category);
	}

	public static SelectionResult setSelectedMount(ServerPlayer player, MountCategory category, UUID mountUuid) {
		if (mountUuid == null) {
			clearSelection(player, category);
			return SelectionResult.CLEARED;
		}

		Entity mount = player.level().getEntityInAnyDimension(mountUuid);
		if (mount == null) {
			var catalogEntry = MountLocationIndex.locate(
				((net.minecraft.server.level.ServerLevel) player.level()).getServer(), mountUuid);
			SelectionResult unloadedResult = validateCatalogFacts(
				catalogEntry.isPresent(),
				catalogEntry.flatMap(MountLocationIndex.Location::ownerUuid)
					.filter(player.getUUID()::equals).isPresent(),
				catalogEntry.filter(entry -> entry.supports(category)).isPresent());
			if (unloadedResult == SelectionResult.SUCCESS) {
				data(player).belandsigh$setSelectedMountUuid(category, mountUuid);
			}
			return unloadedResult;
		}
		SelectionResult result = validateFacts(
			mount != null,
			mount != null && mount.isAlive(),
			mount != null && MountOwnershipService.doesPlayerOwnMount(player, mount),
			mount != null && MountCategoryService.supports(mount, category)
		);
		if (result == SelectionResult.SUCCESS) {
			data(player).belandsigh$setSelectedMountUuid(category, mountUuid);
			MountLocationIndex.record(mount, (net.minecraft.server.level.ServerLevel) mount.level());
		}
		return result;
	}

	/**
	 * Resolves and revalidates a saved selection. An unresolved UUID is retained
	 * because its entity may simply be in an unloaded chunk.
	 */
	public static SelectionLookup validateSelection(ServerPlayer player, MountCategory category) {
		Optional<UUID> selectedUuid = getSelectedMountUuid(player, category);
		if (selectedUuid.isEmpty()) {
			return new SelectionLookup(SelectionResult.NO_SELECTION, null);
		}

		Entity mount = player.level().getEntityInAnyDimension(selectedUuid.get());
		if (mount == null) {
			return new SelectionLookup(SelectionResult.NOT_FOUND, null);
		}

		SelectionResult result = validateFacts(
			true,
			mount.isAlive(),
			MountOwnershipService.doesPlayerOwnMount(player, mount),
			MountCategoryService.supports(mount, category)
		);
		if (result != SelectionResult.SUCCESS) {
			clearSelection(player, category);
			return new SelectionLookup(result, null);
		}
		return new SelectionLookup(SelectionResult.SUCCESS, mount);
	}

	public static void clearSelection(ServerPlayer player, MountCategory category) {
		data(player).belandsigh$setSelectedMountUuid(category, null);
	}

	public static void clearMountFromAllSelections(ServerPlayer player, UUID mountUuid) {
		for (MountCategory category : MountCategory.values()) {
			if (getSelectedMountUuid(player, category).filter(mountUuid::equals).isPresent()) {
				clearSelection(player, category);
			}
		}
	}

	public static void copySelections(ServerPlayer source, ServerPlayer target) {
		copySelections(data(source), data(target));
	}

	static void copySelections(PlayerMountSelectionData source, PlayerMountSelectionData target) {
		for (MountCategory category : MountCategory.values()) {
			target.belandsigh$setSelectedMountUuid(
				category,
				source.belandsigh$getSelectedMountUuid(category).orElse(null)
			);
		}
	}

	static SelectionResult validateFacts(boolean found, boolean alive, boolean owned, boolean compatible) {
		if (!found) {
			return SelectionResult.NOT_FOUND;
		}
		if (!alive) {
			return SelectionResult.DEAD;
		}
		if (!owned) {
			return SelectionResult.NOT_OWNED;
		}
		return compatible ? SelectionResult.SUCCESS : SelectionResult.INCOMPATIBLE;
	}

	static SelectionResult validateCatalogFacts(boolean found, boolean owned, boolean compatible) {
		if (!found) return SelectionResult.NOT_FOUND;
		if (!owned) return SelectionResult.NOT_OWNED;
		return compatible ? SelectionResult.SUCCESS : SelectionResult.INCOMPATIBLE;
	}

	private static PlayerMountSelectionData data(ServerPlayer player) {
		return (PlayerMountSelectionData) player;
	}

	public enum SelectionResult {
		SUCCESS,
		CLEARED,
		NO_SELECTION,
		NOT_FOUND,
		DEAD,
		NOT_OWNED,
		INCOMPATIBLE
	}

	public record SelectionLookup(SelectionResult result, Entity mount) {
		public Optional<Entity> resolvedMount() {
			return Optional.ofNullable(mount);
		}
	}
}
