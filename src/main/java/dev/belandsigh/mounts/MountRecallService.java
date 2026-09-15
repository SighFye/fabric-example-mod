package dev.belandsigh.mounts;

import dev.belandsigh.BelAndSighMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Coordinates UUID resolution, authoritative validation, safe placement, and recall. */
public final class MountRecallService {
	private static final int SOURCE_CHUNK_TICKET_RADIUS = 2;
	private static final int SOURCE_CHUNK_TIMEOUT_TICKS = 100;
	private static TicketType recallTicket;
	private static final Map<UUID, PendingRecall> PENDING_RECALLS = new HashMap<>();

	private MountRecallService() {
	}

	public static void initialize() {
		recallTicket = Registry.register(
			BuiltInRegistries.TICKET_TYPE,
			Identifier.fromNamespaceAndPath(BelAndSighMod.MOD_ID, "mount_recall"),
			new TicketType(SOURCE_CHUNK_TIMEOUT_TICKS,
				TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION | TicketType.FLAG_KEEP_DIMENSION_ACTIVE)
		);
		ServerTickEvents.END_SERVER_TICK.register(MountRecallService::tickPendingRecalls);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> PENDING_RECALLS.clear());
	}

	public static RecallResult recallSelectedMount(ServerPlayer player, MountCategory category) {
		Optional<java.util.UUID> selected = PlayerMountSelectionService.getSelectedMountUuid(player, category);
		if (selected.isEmpty()) {
			return RecallResult.noSelection(category);
		}

		UUID mountUuid = selected.get();
		Entity mount = player.level().getEntityInAnyDimension(mountUuid);
		if (mount != null) {
			return recallMount(player, mount, category, true);
		}

		MinecraftServer server = ((ServerLevel) player.level()).getServer();
		Optional<MountLocationIndex.Location> location = MountLocationIndex.locate(server, mountUuid);
		if (location.isEmpty()) {
			return RecallResult.of(Status.NOT_FOUND);
		}
		if (!location.get().dimension().equals(player.level().dimension())) {
			return RecallResult.of(Status.WRONG_DIMENSION);
		}
		ServerLevel sourceLevel = server.getLevel(location.get().dimension());
		if (sourceLevel == null) {
			return RecallResult.of(Status.NOT_FOUND);
		}

		ChunkPos sourceChunk = new ChunkPos(location.get().chunkX(), location.get().chunkZ());
		sourceLevel.getChunkSource().addTicketWithRadius(recallTicket, sourceChunk, SOURCE_CHUNK_TICKET_RADIUS);
		PENDING_RECALLS.put(player.getUUID(), new PendingRecall(
			player.getUUID(), mountUuid, category, location.get().dimension(),
			server.getTickCount() + SOURCE_CHUNK_TIMEOUT_TICKS));
		return RecallResult.of(Status.LOADING);
	}

	public static RecallResult recallTargetedMount(ServerPlayer player, Entity mount, MountCategory environment) {
		MountCategory category = chooseTargetCategory(MountCategoryService.getMountCategories(mount), environment);
		return recallMount(player, mount, category, false);
	}

	private static RecallResult recallMount(ServerPlayer player, Entity mount, MountCategory category,
			boolean clearInvalidSelection) {
		boolean sameDimension = mount.level() == player.level();
		boolean callingPlayerRiding = player.getVehicle() == mount;
		boolean occupiedByOther = mount.getPassengers().stream().anyMatch(passenger -> passenger != player);
		Validation validation = validateFacts(
			mount.isAlive(), MountOwnershipService.doesPlayerOwnMount(player, mount),
			MountCategoryService.supports(mount, category), sameDimension,
			callingPlayerRiding, occupiedByOther, true);
		if (validation != Validation.READY) {
			if (clearInvalidSelection && (validation == Validation.DEAD
					|| validation == Validation.NOT_OWNED || validation == Validation.INCOMPATIBLE)) {
				PlayerMountSelectionService.clearSelection(player, category);
				MountNetworking.syncSelections(player);
			}
			return RecallResult.of(statusFor(validation));
		}

		ServerLevel level = (ServerLevel) player.level();
		var destination = SafeMountPositionFinder.find(level, mount, player.blockPosition(), category);
		if (destination.isEmpty()) {
			return RecallResult.of(Status.NO_SAFE_POSITION);
		}
		var pos = destination.get();
		boolean teleported = mount.teleportTo(level, pos.x, pos.y, pos.z, Set.of(), mount.getYRot(), mount.getXRot(), false);
		if (teleported) {
			MountLocationIndex.record(mount, level);
			playArrivalEffect(level, mount);
		}
		return RecallResult.of(teleported ? Status.SUCCESS : Status.TELEPORT_FAILED);
	}

	private static void tickPendingRecalls(MinecraftServer server) {
		Iterator<PendingRecall> iterator = PENDING_RECALLS.values().iterator();
		while (iterator.hasNext()) {
			PendingRecall pending = iterator.next();
			ServerPlayer player = server.getPlayerList().getPlayer(pending.playerUuid());
			if (player == null) {
				iterator.remove();
				continue;
			}

			ServerLevel sourceLevel = server.getLevel(pending.dimension());
			Entity mount = sourceLevel == null ? null : sourceLevel.getEntityInAnyDimension(pending.mountUuid());
			if (mount != null) {
				RecallResult result = recallMount(player, mount, pending.category(), true);
				player.sendOverlayMessage(result.message());
				iterator.remove();
			} else if (server.getTickCount() >= pending.expiresAtTick()) {
				player.sendOverlayMessage(RecallResult.of(Status.NOT_FOUND).message());
				iterator.remove();
			}
		}
	}

	private static void playArrivalEffect(ServerLevel level, Entity mount) {
		double horizontalSpread = Math.max(0.2D, mount.getBbWidth() * 0.35D);
		level.sendParticles(
			ParticleTypes.POOF,
			mount.getX(), mount.getY() + Math.min(0.75D, mount.getBbHeight() * 0.35D), mount.getZ(),
			MountWhistleConstants.ARRIVAL_PARTICLE_COUNT,
			horizontalSpread, Math.min(0.4D, mount.getBbHeight() * 0.2D), horizontalSpread, 0.015D);
	}

	/** Keeps habitat-only mounts in their fluid while allowing multi-category mounts to match the caller's environment. */
	static MountCategory chooseTargetCategory(Set<MountCategory> categories, MountCategory environment) {
		if (categories.contains(environment)) {
			return environment;
		}
		if (categories.contains(MountCategory.WATER) && !categories.contains(MountCategory.LAND)) {
			return MountCategory.WATER;
		}
		if (categories.contains(MountCategory.LAVA) && !categories.contains(MountCategory.LAND)) {
			return MountCategory.LAVA;
		}
		if (categories.contains(MountCategory.LAND)) {
			return MountCategory.LAND;
		}
		if (categories.contains(MountCategory.WATER)) {
			return MountCategory.WATER;
		}
		if (categories.contains(MountCategory.LAVA)) {
			return MountCategory.LAVA;
		}
		throw new IllegalArgumentException("Targeted entity is not a compatible mount");
	}

	static Validation validateFacts(boolean alive, boolean owned, boolean compatible,
			boolean sameDimension, boolean callingPlayerRiding, boolean occupiedByOther, boolean safePosition) {
		if (!alive) return Validation.DEAD;
		if (!owned) return Validation.NOT_OWNED;
		if (!compatible) return Validation.INCOMPATIBLE;
		if (!sameDimension) return Validation.WRONG_DIMENSION;
		if (callingPlayerRiding) return Validation.ALREADY_RIDING;
		if (occupiedByOther) return Validation.OCCUPIED;
		return safePosition ? Validation.READY : Validation.NO_SAFE_POSITION;
	}

	private static Status statusFor(Validation validation) {
		return switch (validation) {
			case DEAD -> Status.DEAD;
			case NOT_OWNED -> Status.NOT_OWNED;
			case INCOMPATIBLE -> Status.INCOMPATIBLE;
			case WRONG_DIMENSION -> Status.WRONG_DIMENSION;
			case ALREADY_RIDING -> Status.ALREADY_RIDING;
			case OCCUPIED -> Status.OCCUPIED;
			case NO_SAFE_POSITION -> Status.NO_SAFE_POSITION;
			case READY -> throw new IllegalArgumentException("READY is not a failure");
		};
	}

	public enum Validation { READY, DEAD, NOT_OWNED, INCOMPATIBLE, WRONG_DIMENSION, ALREADY_RIDING, OCCUPIED, NO_SAFE_POSITION }
	public enum Status { SUCCESS, LOADING, NO_SELECTION, NOT_FOUND, DEAD, NOT_OWNED, INCOMPATIBLE, WRONG_DIMENSION, ALREADY_RIDING, OCCUPIED, NO_SAFE_POSITION, TELEPORT_FAILED }

	private record PendingRecall(UUID playerUuid, UUID mountUuid, MountCategory category,
			ResourceKey<Level> dimension, int expiresAtTick) {
	}

	public record RecallResult(Status status, Component message) {
		static RecallResult noSelection(MountCategory category) {
			return new RecallResult(Status.NO_SELECTION, Component.translatable(
				"message.belandsigh.mount_recall.no_selection." + category.name().toLowerCase(java.util.Locale.ROOT)));
		}

		static RecallResult of(Status status) {
			return new RecallResult(status, Component.translatable("message.belandsigh.mount_recall." + status.name().toLowerCase(java.util.Locale.ROOT)));
		}

		public boolean successful() {
			return status == Status.SUCCESS;
		}
	}
}
