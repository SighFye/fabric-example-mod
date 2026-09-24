package dev.belandsigh.pets;

import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Runtime index of loaded, server-side follow-capable pets grouped by owner, maintained from entity
 * load/unload and owner changes so the follow check never has to scan every entity.
 */
final class PetOwnerIndex {
	private static final Set<TamableAnimal> LOADED = new HashSet<>();
	private static final Map<TamableAnimal, UUID> OWNER_BY_PET = new HashMap<>();
	private static final Map<UUID, Set<TamableAnimal>> PETS_BY_OWNER = new HashMap<>();

	private PetOwnerIndex() {
	}

	static void add(TamableAnimal pet) {
		LOADED.add(pet);
		reindex(pet);
	}

	static void remove(TamableAnimal pet) {
		LOADED.remove(pet);
		unlink(pet);
	}

	/** Re-files a loaded pet under its current owner; ignores pets not (yet) in a server level. */
	static void reindex(TamableAnimal pet) {
		if (!LOADED.contains(pet)) {
			return;
		}
		unlink(pet);
		EntityReference<LivingEntity> owner = pet.getOwnerReference();
		if (owner != null) {
			OWNER_BY_PET.put(pet, owner.getUUID());
			PETS_BY_OWNER.computeIfAbsent(owner.getUUID(), ignored -> new HashSet<>()).add(pet);
		}
	}

	static List<TamableAnimal> petsOf(UUID ownerId) {
		Set<TamableAnimal> pets = PETS_BY_OWNER.get(ownerId);
		return pets == null ? List.of() : List.copyOf(pets);
	}

	static List<TamableAnimal> ownedPets() {
		return List.copyOf(OWNER_BY_PET.keySet());
	}

	static void clear() {
		LOADED.clear();
		OWNER_BY_PET.clear();
		PETS_BY_OWNER.clear();
	}

	private static void unlink(TamableAnimal pet) {
		UUID previousOwner = OWNER_BY_PET.remove(pet);
		if (previousOwner == null) {
			return;
		}
		Set<TamableAnimal> pets = PETS_BY_OWNER.get(previousOwner);
		if (pets != null) {
			pets.remove(pet);
			if (pets.isEmpty()) {
				PETS_BY_OWNER.remove(previousOwner);
			}
		}
	}
}
