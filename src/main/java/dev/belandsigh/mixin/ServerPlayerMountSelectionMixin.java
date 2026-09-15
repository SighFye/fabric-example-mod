package dev.belandsigh.mixin;

import dev.belandsigh.mounts.MountCategory;
import dev.belandsigh.mounts.PlayerMountSelectionData;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMountSelectionMixin implements PlayerMountSelectionData {
	@Unique private static final String BELANDSIGH_SELECTIONS = "BelAndSighMountWhistleSelections";
	@Unique private final Map<MountCategory, UUID> belandsigh$mountSelections = new EnumMap<>(MountCategory.class);

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void belandsigh$saveMountSelections(ValueOutput output, CallbackInfo ci) {
		if (belandsigh$mountSelections.isEmpty()) {
			return;
		}
		ValueOutput selections = output.child(BELANDSIGH_SELECTIONS);
		belandsigh$mountSelections.forEach((category, uuid) ->
			selections.store(category.name(), UUIDUtil.CODEC, uuid));
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void belandsigh$loadMountSelections(ValueInput input, CallbackInfo ci) {
		belandsigh$mountSelections.clear();
		input.child(BELANDSIGH_SELECTIONS).ifPresent(selections -> {
			for (MountCategory category : MountCategory.values()) {
				selections.read(category.name(), UUIDUtil.CODEC)
					.ifPresent(uuid -> belandsigh$mountSelections.put(category, uuid));
			}
		});
	}

	@Override
	public Optional<UUID> belandsigh$getSelectedMountUuid(MountCategory category) {
		return Optional.ofNullable(belandsigh$mountSelections.get(category));
	}

	@Override
	public void belandsigh$setSelectedMountUuid(MountCategory category, UUID mountUuid) {
		if (mountUuid == null) {
			belandsigh$mountSelections.remove(category);
		} else {
			belandsigh$mountSelections.put(category, mountUuid);
		}
	}
}
