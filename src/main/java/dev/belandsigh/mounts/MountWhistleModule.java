package dev.belandsigh.mounts;

import dev.belandsigh.BelAndSighMod;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;

/** Entry point and registry for the Mount Whistle feature. */
public final class MountWhistleModule {
	public static final Identifier MOUNT_WHISTLE_SOUND_ID = Identifier.fromNamespaceAndPath(
		BelAndSighMod.MOD_ID, "mount_whistle");
	public static final SoundEvent MOUNT_WHISTLE_SOUND = Registry.register(
		BuiltInRegistries.SOUND_EVENT,
		MOUNT_WHISTLE_SOUND_ID,
		SoundEvent.createVariableRangeEvent(MOUNT_WHISTLE_SOUND_ID)
	);
	public static final ResourceKey<Item> MOUNT_WHISTLE_KEY = ResourceKey.create(
		Registries.ITEM,
		Identifier.fromNamespaceAndPath(BelAndSighMod.MOD_ID, "mount_whistle")
	);
	public static final Item MOUNT_WHISTLE = Registry.register(
		BuiltInRegistries.ITEM,
		MOUNT_WHISTLE_KEY,
		new MountWhistleItem(new Item.Properties()
			.setId(MOUNT_WHISTLE_KEY)
			.stacksTo(1)
			.useCooldown(MountWhistleConstants.USE_COOLDOWN_SECONDS))
	);

	private MountWhistleModule() {
	}

	public static void initialize() {
		MountNetworking.initialize();
		MountRecallService.initialize();
		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) ->
			PlayerMountSelectionService.copySelections(oldPlayer, newPlayer));
		ServerPlayerEvents.JOIN.register(MountNetworking::syncSelections);
		ServerEntityEvents.ENTITY_LOAD.register(MountLocationIndex::record);
		ServerEntityEvents.ENTITY_UNLOAD.register(MountLocationIndex::record);
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
			if (!(entity.level() instanceof ServerLevel level)) {
				return;
			}
			MountLocationIndex.remove(entity, level);
			MountOwnershipService.getOwnerUuid(entity).ifPresent(ownerUuid -> {
				var owner = level.getServer().getPlayerList().getPlayer(ownerUuid);
				if (owner != null) {
					PlayerMountSelectionService.clearMountFromAllSelections(owner, entity.getUUID());
					MountNetworking.syncSelections(owner);
				}
			});
		});
	}
}
