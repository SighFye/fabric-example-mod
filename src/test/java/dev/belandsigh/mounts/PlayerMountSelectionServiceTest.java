package dev.belandsigh.mounts;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static dev.belandsigh.mounts.PlayerMountSelectionService.SelectionResult.DEAD;
import static dev.belandsigh.mounts.PlayerMountSelectionService.SelectionResult.INCOMPATIBLE;
import static dev.belandsigh.mounts.PlayerMountSelectionService.SelectionResult.NOT_FOUND;
import static dev.belandsigh.mounts.PlayerMountSelectionService.SelectionResult.NOT_OWNED;
import static dev.belandsigh.mounts.PlayerMountSelectionService.SelectionResult.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerMountSelectionServiceTest {
	private static final UUID MOUNT = UUID.fromString("00000000-0000-0000-0000-000000000010");

	@Test
	void playerCannotSelectSomeoneElsesMount() {
		assertEquals(NOT_OWNED, PlayerMountSelectionService.validateFacts(true, true, false, true));
	}

	@Test
	void playerCannotSelectIncompatibleCategory() {
		assertEquals(INCOMPATIBLE, PlayerMountSelectionService.validateFacts(true, true, true, false));
	}

	@Test
	void deadMountFailsValidation() {
		assertEquals(DEAD, PlayerMountSelectionService.validateFacts(true, false, true, true));
	}

	@Test
	void missingMountFailsValidation() {
		assertEquals(NOT_FOUND, PlayerMountSelectionService.validateFacts(false, true, true, true));
	}

	@Test
	void validOwnedCompatibleMountSucceeds() {
		assertEquals(SUCCESS, PlayerMountSelectionService.validateFacts(true, true, true, true));
	}

	@Test
	void cataloguedUnloadedOwnedMountCanBeSelected() {
		assertEquals(SUCCESS, PlayerMountSelectionService.validateCatalogFacts(true, true, true));
	}

	@Test
	void cataloguedUnloadedMountStillRequiresOwnership() {
		assertEquals(NOT_OWNED, PlayerMountSelectionService.validateCatalogFacts(true, false, true));
	}

	@Test
	void cataloguedUnloadedMountStillRequiresCategory() {
		assertEquals(INCOMPATIBLE, PlayerMountSelectionService.validateCatalogFacts(true, true, false));
	}

	@Test
	void selectionsCopyAcrossPlayerRespawn() {
		FakeSelectionData source = new FakeSelectionData();
		FakeSelectionData target = new FakeSelectionData();
		source.belandsigh$setSelectedMountUuid(MountCategory.LAND, MOUNT);
		source.belandsigh$setSelectedMountUuid(MountCategory.WATER, MOUNT);

		PlayerMountSelectionService.copySelections(source, target);

		assertEquals(Optional.of(MOUNT), target.belandsigh$getSelectedMountUuid(MountCategory.LAND));
		assertEquals(Optional.of(MOUNT), target.belandsigh$getSelectedMountUuid(MountCategory.WATER));
		assertEquals(Optional.empty(), target.belandsigh$getSelectedMountUuid(MountCategory.LAVA));
	}

	private static final class FakeSelectionData implements PlayerMountSelectionData {
		private final Map<MountCategory, UUID> selections = new EnumMap<>(MountCategory.class);

		@Override
		public Optional<UUID> belandsigh$getSelectedMountUuid(MountCategory category) {
			return Optional.ofNullable(selections.get(category));
		}

		@Override
		public void belandsigh$setSelectedMountUuid(MountCategory category, UUID mountUuid) {
			if (mountUuid == null) {
				selections.remove(category);
			} else {
				selections.put(category, mountUuid);
			}
		}
	}
}
