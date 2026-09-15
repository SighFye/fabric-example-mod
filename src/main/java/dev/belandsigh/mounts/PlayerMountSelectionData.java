package dev.belandsigh.mounts;

import java.util.Optional;
import java.util.UUID;

/** Implemented on server players by the persistence mixin. */
public interface PlayerMountSelectionData {
	Optional<UUID> belandsigh$getSelectedMountUuid(MountCategory category);

	void belandsigh$setSelectedMountUuid(MountCategory category, UUID mountUuid);
}
