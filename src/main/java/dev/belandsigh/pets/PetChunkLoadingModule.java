package dev.belandsigh.pets;

import dev.belandsigh.BelAndSighMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.level.ChunkPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Keeps actively-following tameable animals ticking when they fall outside player-loaded chunks. */
public final class PetChunkLoadingModule {
	private static final int ENTITY_TICKING_RADIUS = 2;
	private static final int RECONCILE_INTERVAL_TICKS = 20;
	private static final TicketType PET_TICKET = Registry.register(
		BuiltInRegistries.TICKET_TYPE,
		Identifier.fromNamespaceAndPath(BelAndSighMod.MOD_ID, "following_pet"),
		new TicketType(TicketType.NO_TIMEOUT,
			TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION | TicketType.FLAG_KEEP_DIMENSION_ACTIVE)
	);
	private static final Map<ServerLevel, Set<Long>> TICKETED_CHUNKS = new HashMap<>();

	private PetChunkLoadingModule() {
	}

	public static void initialize() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % RECONCILE_INTERVAL_TICKS != 0) {
				return;
			}
			for (ServerLevel level : server.getAllLevels()) {
				updateTickets(level);
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> TICKETED_CHUNKS.clear());
	}

	private static void updateTickets(ServerLevel level) {
		Set<Long> desiredChunks = new HashSet<>();
		for (Entity entity : level.getAllEntities()) {
			if (entity instanceof TamableAnimal pet
				&& isFollowingPetType(pet)
				&& pet.isAlive()
				&& pet.isTame()
				&& !pet.isInSittingPose()
				&& !pet.isLeashed()) {
				desiredChunks.add(pet.chunkPosition().pack());
			}
		}

		Set<Long> currentChunks = TICKETED_CHUNKS.computeIfAbsent(level, ignored -> new HashSet<>());
		for (long packedPos : desiredChunks) {
			if (!currentChunks.contains(packedPos)) {
				level.getChunkSource().addTicketWithRadius(PET_TICKET, ChunkPos.unpack(packedPos), ENTITY_TICKING_RADIUS);
			}
		}
		for (long packedPos : currentChunks) {
			if (!desiredChunks.contains(packedPos)) {
				level.getChunkSource().removeTicketWithRadius(PET_TICKET, ChunkPos.unpack(packedPos), ENTITY_TICKING_RADIUS);
			}
		}

		if (desiredChunks.isEmpty()) {
			TICKETED_CHUNKS.remove(level);
		} else {
			TICKETED_CHUNKS.put(level, desiredChunks);
		}
	}

	private static boolean isFollowingPetType(TamableAnimal pet) {
		return pet instanceof Wolf || pet instanceof Cat || pet instanceof Parrot;
	}
}
