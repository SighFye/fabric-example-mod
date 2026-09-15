package dev.belandsigh.mounts;

import java.util.Optional;
import java.util.UUID;

/** Implemented on entities by the persistence mixin. */
public interface MountBindingData {
	Optional<UUID> belandsigh$getBoundOwnerUuid();

	void belandsigh$setBoundOwnerUuid(UUID ownerUuid);
}
