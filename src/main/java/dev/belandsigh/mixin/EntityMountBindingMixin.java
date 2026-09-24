package dev.belandsigh.mixin;

import dev.belandsigh.mounts.MountBindingData;
import dev.belandsigh.mounts.MountBindingService;
import dev.belandsigh.mounts.MountLocationIndex;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.UUID;

/** Persists optional mount binding and handles the successful ride transition. */
@Mixin(Entity.class)
public abstract class EntityMountBindingMixin implements MountBindingData {
	@Unique private static final String BELANDSIGH_MOUNT_DATA = "BelAndSighMountWhistle";
	@Unique private static final String BELANDSIGH_BOUND_OWNER = "BoundOwner";
	@Unique private UUID belandsigh$boundOwnerUuid;

	@Inject(method = "saveWithoutId", at = @At("TAIL"))
	private void belandsigh$saveMountBinding(ValueOutput output, CallbackInfo ci) {
		if (belandsigh$boundOwnerUuid != null) {
			output.child(BELANDSIGH_MOUNT_DATA)
				.store(BELANDSIGH_BOUND_OWNER, UUIDUtil.CODEC, belandsigh$boundOwnerUuid);
		}
	}

	@Inject(method = "load", at = @At("TAIL"))
	private void belandsigh$loadMountBinding(ValueInput input, CallbackInfo ci) {
		belandsigh$boundOwnerUuid = input.child(BELANDSIGH_MOUNT_DATA)
			.flatMap(data -> data.read(BELANDSIGH_BOUND_OWNER, UUIDUtil.CODEC))
			.orElse(null);
	}

	/** Name tags rename mounts; keep the management screen's catalog current. */
	@Inject(method = "setCustomName", at = @At("TAIL"))
	private void belandsigh$refreshMountCatalogName(Component name, CallbackInfo ci) {
		MountLocationIndex.refresh((Entity) (Object) this);
	}

	@Inject(method = "startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z", at = @At("RETURN"))
	private void belandsigh$bindMountAfterRideStarts(Entity vehicle, boolean force, boolean emitEvent,
			CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ() && (Object) this instanceof ServerPlayer player) {
			MountBindingService.bindOnSuccessfulRide(player, vehicle);
		}
	}

	@Override
	public Optional<UUID> belandsigh$getBoundOwnerUuid() {
		return Optional.ofNullable(belandsigh$boundOwnerUuid);
	}

	@Override
	public void belandsigh$setBoundOwnerUuid(UUID ownerUuid) {
		belandsigh$boundOwnerUuid = ownerUuid;
	}
}
