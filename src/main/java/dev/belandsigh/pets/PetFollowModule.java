package dev.belandsigh.pets;

import dev.belandsigh.BelAndSighMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.level.ChunkPos;

import java.util.HashMap;
import java.util.Map;

/**
 * Keeps every standing, unleashed pet's chunk loaded (never unloaded, not simulated) regardless of
 * where its owner is, and teleports the pet to its owner with vanilla's own
 * {@code tryToTeleportToOwner} whenever the owner is in the same dimension but out of vanilla's reach.
 */
public final class PetFollowModule {
	private static final int CHECK_INTERVAL_TICKS = 10;
	private static final TicketType PET_TICKET = Registry.register(
		BuiltInRegistries.TICKET_TYPE,
		Identifier.fromNamespaceAndPath(BelAndSighMod.MOD_ID, "pet_follow"),
		new TicketType(TicketType.NO_TIMEOUT, TicketType.FLAG_LOADING)
	);
	/** The chunk each ticketed pet currently holds. */
	private static final Map<TamableAnimal, TicketedChunk> TICKETED_PETS = new HashMap<>();
	/** How many pets hold each chunk; pets sharing a chunk share one vanilla ticket. */
	private static final Map<TicketedChunk, Integer> CHUNK_HOLDERS = new HashMap<>();

	private PetFollowModule() {
	}

	public static void initialize() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof TamableAnimal pet && isFollowingPetType(pet)) {
				PetOwnerIndex.add(pet);
				updateTicket(pet);
			}
		});
		ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
			if (entity instanceof TamableAnimal pet && isFollowingPetType(pet)) {
				PetOwnerIndex.remove(pet);
				releaseTicket(pet);
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % CHECK_INTERVAL_TICKS == 0) {
				tick(server);
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			PetOwnerIndex.clear();
			TICKETED_PETS.clear();
			CHUNK_HOLDERS.clear();
		});
	}

	/** Called from the owner-setter mixin when a loaded pet is tamed or changes owner. */
	public static void onOwnerChanged(TamableAnimal pet) {
		PetOwnerIndex.reindex(pet);
	}

	private static void tick(MinecraftServer server) {
		// Every owned pet keeps its chunk, whether or not its owner is online.
		for (TamableAnimal pet : PetOwnerIndex.ownedPets()) {
			if (!pet.isRemoved()) {
				updateTicket(pet);
			}
		}

		int simulationDistance = server.getPlayerList().getSimulationDistance();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			for (TamableAnimal pet : PetOwnerIndex.petsOf(player.getUUID())) {
				if (!pet.isRemoved()) {
					follow(pet, player, simulationDistance);
				}
			}
		}
	}

	private static void follow(TamableAnimal pet, ServerPlayer player, int simulationDistance) {
		ServerLevel level = (ServerLevel) pet.level();
		boolean sameLevel = level == player.level();
		double distanceSqr = sameLevel ? pet.distanceToSqr(player) : Double.MAX_VALUE;
		boolean ticking = sameLevel && level.isPositionEntityTicking(pet.blockPosition());
		int chunkDistance = pet.chunkPosition().getChessboardDistance(player.chunkPosition());
		PetFollowPolicy.Action action = PetFollowPolicy.decide(
			!pet.unableToMoveToOwner(), sameLevel, player.isAlive() && !player.isSpectator(),
			distanceSqr, ticking, chunkDistance, simulationDistance);
		if (action == PetFollowPolicy.Action.CATCH_UP) {
			// If there is no safe spot near the owner yet (e.g. mid-air on an elytra) this does
			// nothing; the pet's chunk stays loaded and the next check tries again.
			pet.tryToTeleportToOwner();
			updateTicket(pet);
		}
	}

	/** Moves the pet's ticket to its current chunk, or drops it if the pet is sitting or leashed. */
	private static void updateTicket(TamableAnimal pet) {
		TicketedChunk current = TICKETED_PETS.get(pet);
		if (pet.getOwnerReference() == null || pet.unableToMoveToOwner()) {
			if (current != null) {
				releaseTicket(pet);
			}
			return;
		}
		TicketedChunk desired = new TicketedChunk((ServerLevel) pet.level(), pet.chunkPosition());
		if (desired.equals(current)) {
			return;
		}
		if (current != null) {
			releaseTicket(pet);
		}
		TICKETED_PETS.put(pet, desired);
		if (CHUNK_HOLDERS.merge(desired, 1, Integer::sum) == 1) {
			desired.level().getChunkSource().addTicketWithRadius(PET_TICKET, desired.chunk(), 0);
		}
	}

	private static void releaseTicket(TamableAnimal pet) {
		TicketedChunk held = TICKETED_PETS.remove(pet);
		if (held == null) {
			return;
		}
		int remaining = CHUNK_HOLDERS.merge(held, -1, Integer::sum);
		if (remaining <= 0) {
			CHUNK_HOLDERS.remove(held);
			held.level().getChunkSource().removeTicketWithRadius(PET_TICKET, held.chunk(), 0);
		}
	}

	private static boolean isFollowingPetType(TamableAnimal pet) {
		return pet instanceof Wolf || pet instanceof Cat || pet instanceof Parrot;
	}

	private record TicketedChunk(ServerLevel level, ChunkPos chunk) {
	}
}
