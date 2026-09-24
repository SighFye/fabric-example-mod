package dev.belandsigh.mounts;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.belandsigh.BelAndSighMod;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistent last-known chunk index for compatible mounts. It permits a
 * one-shot source chunk load without permanently force-loading any chunk.
 */
public final class MountLocationIndex extends SavedData {
	private static final Codec<Location> LOCATION_CODEC = RecordCodecBuilder.create(instance -> instance.group(
		UUIDUtil.CODEC.fieldOf("uuid").forGetter(Location::uuid),
		Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(Location::dimension),
		Codec.INT.fieldOf("chunk_x").forGetter(Location::chunkX),
		Codec.INT.fieldOf("chunk_z").forGetter(Location::chunkZ),
		UUIDUtil.CODEC.optionalFieldOf("owner_uuid").forGetter(Location::ownerUuid),
		Codec.STRING.optionalFieldOf("display_name", "").forGetter(Location::displayName),
		Codec.STRING.optionalFieldOf("type_name", "").forGetter(Location::typeName),
		Codec.INT.optionalFieldOf("categories", 0).forGetter(Location::categoryMask),
		Codec.BOOL.optionalFieldOf("bound", false).forGetter(Location::bound)
	).apply(instance, Location::new));
	static final Codec<MountLocationIndex> CODEC = LOCATION_CODEC.listOf()
		.fieldOf("mounts")
		.xmap(MountLocationIndex::new, index -> List.copyOf(index.locations.values()))
		.codec();
	private static final SavedDataType<MountLocationIndex> TYPE = new SavedDataType<>(
		Identifier.fromNamespaceAndPath(BelAndSighMod.MOD_ID, "mount_locations"), MountLocationIndex::new, CODEC,
		DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

	private final Map<UUID, Location> locations = new HashMap<>();

	public MountLocationIndex() {
	}

	MountLocationIndex(List<Location> locations) {
		locations.forEach(location -> this.locations.put(location.uuid(), location));
	}

	public static void record(Entity entity, ServerLevel level) {
		if (!entity.isAlive() || !MountCategoryService.isEligibleMount(entity)) {
			return;
		}
		ChunkPos chunk = entity.chunkPosition();
		MountLocationIndex index = get(level.getServer());
		Optional<UUID> ownerUuid = MountOwnershipService.getOwnerUuid(entity);
		if (ownerUuid.isEmpty()) {
			if (index.locations.remove(entity.getUUID()) != null) {
				index.setDirty();
			}
			return;
		}
		String displayName = entity.hasCustomName()
			? entity.getCustomName().getString()
			: entity.getName().getString();
		Location next = new Location(
			entity.getUUID(), level.dimension(), chunk.x(), chunk.z(), ownerUuid,
			displayName, entity.getType().getDescription().getString(),
			MountCategoryService.categoryMask(entity),
			!(entity instanceof OwnableEntity)
				&& ((MountBindingData) entity).belandsigh$getBoundOwnerUuid().isPresent());
		if (!next.equals(index.locations.put(entity.getUUID(), next))) {
			index.setDirty();
		}
	}

	/**
	 * Re-records a loaded mount after its owner or name changes, so the management screen stays current without
	 * scanning every entity. Ignores entities still being deserialized (not yet in their level).
	 */
	public static void refresh(Entity entity) {
		if (entity.level() instanceof ServerLevel level && MountCategoryService.isEligibleMount(entity)
				&& level.getEntity(entity.getUUID()) == entity) {
			record(entity, level);
		}
	}

	public static void remove(Entity entity, ServerLevel level) {
		if (!MountCategoryService.isEligibleMount(entity)) {
			return;
		}
		MountLocationIndex index = get(level.getServer());
		if (index.locations.remove(entity.getUUID()) != null) {
			index.setDirty();
		}
	}

	static MountLocationIndex get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	public static Optional<Location> locate(MinecraftServer server, UUID uuid) {
		return get(server).find(uuid);
	}

	public static List<Location> catalogForOwner(MinecraftServer server, UUID ownerUuid) {
		return get(server).locations.values().stream()
			.filter(location -> location.ownerUuid().filter(ownerUuid::equals).isPresent())
			.filter(Location::hasCatalogData)
			.toList();
	}

	Optional<Location> find(UUID uuid) {
		return Optional.ofNullable(locations.get(uuid));
	}

	public record Location(UUID uuid, ResourceKey<Level> dimension, int chunkX, int chunkZ,
			Optional<UUID> ownerUuid, String displayName, String typeName, int categoryMask, boolean bound) {
		public boolean supports(MountCategory category) {
			return MountCategoryService.supportsMask(categoryMask, category);
		}

		public boolean hasCatalogData() {
			return ownerUuid.isPresent() && !displayName.isBlank() && !typeName.isBlank() && categoryMask != 0;
		}
	}

}
