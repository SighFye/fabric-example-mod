package dev.belandsigh.death;

import dev.belandsigh.BelAndSighMod;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks how much longer each of a player's death locations still has loose death-drop items lying
 * around, showing a boss bar HUD per death only while it is actually counting down. Every death is
 * tracked independently and simultaneously - a new death doesn't replace an older, still-active one.
 * A countdown only progresses while its death chunk is loaded; its bar hides itself while paused and
 * reappears once loaded again.
 *
 * <p>Each death tracks the exact set of item entities it spawned (by UUID), not an area sweep, so
 * unrelated items - other players' drops, mob/farm drops, another death's items - are never touched
 * and a genuine death item that drifts away (water, hoppers, explosions) is still tracked correctly.
 * The countdown ends the moment every tracked item is gone, whether picked up or naturally despawned
 * by vanilla; this module never deletes an item itself and never gives death items a custom lifespan
 * - vanilla alone decides when they disappear. The remaining time shown on the boss bar is only an
 * estimate read from the tracked items' own vanilla age.
 *
 * <p>Every active countdown is mirrored into {@link DeathLocationSavedData}, a server-wide store
 * that isn't tied to any particular {@code ServerPlayer} instance, so it keeps updating (and stays
 * crash-safe) even while its owner is offline.
 */
public final class DeathLocationModule {
	/** Vanilla's own hardcoded item despawn age (see ItemEntity#tick); used only to estimate the
	 * boss bar's remaining time, never to control when an item actually disappears. */
	private static final int VANILLA_ITEM_LIFETIME_TICKS = 6000;
	/** How often (in ticks) to re-check tracked items and refresh the boss bar - once per second is
	 * plenty and avoids an area query and text rebuild on every single tick. */
	private static final int CHECK_INTERVAL_TICKS = 20;
	private static final Map<UUID, List<Countdown>> COUNTDOWNS = new HashMap<>();
	private static final Map<UUID, Integer> DEATH_NUMBERS = new HashMap<>();
	/** Players whose countdowns changed outside the tick loop (item merges) and still need saving. */
	private static final Set<UUID> UNSAVED_PLAYERS = new HashSet<>();
	private static ServerPlayer capturingPlayer;
	private static ServerLevel capturingLevel;
	private static BlockPos capturingPos;
	private static Set<UUID> capturedItemIds;

	private DeathLocationModule() {
	}

	public static void initialize() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			Iterator<Map.Entry<UUID, List<Countdown>>> playerIterator = COUNTDOWNS.entrySet().iterator();
			while (playerIterator.hasNext()) {
				Map.Entry<UUID, List<Countdown>> entry = playerIterator.next();
				List<Countdown> countdowns = entry.getValue();
				ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
				boolean changed = UNSAVED_PLAYERS.remove(entry.getKey());

				Iterator<Countdown> countdownIterator = countdowns.iterator();
				while (countdownIterator.hasNext()) {
					Countdown countdown = countdownIterator.next();
					ServerLevel level = server.getLevel(countdown.dimension);
					if (level == null) {
						countdown.bossEvent.removeAllPlayers();
						countdownIterator.remove();
						changed = true;
						continue;
					}

					boolean loaded = level.isPositionTickingWithEntitiesLoaded(countdown.chunkKey);
					if (!loaded) {
						updateBossBar(countdown, false, null, player);
						continue;
					}

					if (countdown.ticksUntilCheck > 0) {
						countdown.ticksUntilCheck--;
						continue;
					}
					countdown.ticksUntilCheck = CHECK_INTERVAL_TICKS;

					changed |= countdown.itemIds.removeIf(id -> level.getEntity(id) == null);
					if (countdown.itemIds.isEmpty()) {
						BelAndSighMod.LOGGER.info("[death-loc] clearing countdown for {}: all tracked items are gone", entry.getKey());
						countdown.bossEvent.removeAllPlayers();
						countdownIterator.remove();
						changed = true;
						continue;
					}

					updateBossBar(countdown, true, level, player);
				}

				if (countdowns.isEmpty()) {
					playerIterator.remove();
				}
				// Only rewrite saved data when the tracked set actually changed, not every tick.
				if (changed) {
					persist(server, entry.getKey(), countdowns);
				}
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			COUNTDOWNS.clear();
			DEATH_NUMBERS.clear();
			UNSAVED_PLAYERS.clear();
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			List<Countdown> countdowns = COUNTDOWNS.get(handler.getPlayer().getUUID());
			if (countdowns != null) {
				for (Countdown countdown : countdowns) {
					countdown.bossEvent.removePlayer(handler.getPlayer());
				}
			}
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			List<Countdown> existing = COUNTDOWNS.get(player.getUUID());
			if (existing != null) {
				for (Countdown countdown : existing) {
					countdown.bossEvent.addPlayer(player);
				}
				return;
			}
			List<DeathRecord> records = DeathLocationSavedData.get(server).get(player.getUUID());
			if (!records.isEmpty()) {
				List<Countdown> resumed = new ArrayList<>(records.size());
				int maxNumber = 0;
				for (DeathRecord record : records) {
					Countdown countdown = resumeCountdown(record);
					countdown.bossEvent.addPlayer(player);
					resumed.add(countdown);
					maxNumber = Math.max(maxNumber, record.deathNumber());
				}
				COUNTDOWNS.put(player.getUUID(), resumed);
				DEATH_NUMBERS.merge(player.getUUID(), maxNumber, Integer::max);
			}
		});
		// Respawning (or an end-portal dimension change) discards the old ServerPlayer instance and
		// builds a new one; boss-bar membership is tied to that instance, so it has to move across by
		// hand. The tracked records themselves live in DeathLocationSavedData keyed by player UUID,
		// so they don't need to be copied at all.
		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
			List<Countdown> countdowns = COUNTDOWNS.get(oldPlayer.getUUID());
			if (countdowns != null) {
				for (Countdown countdown : countdowns) {
					countdown.bossEvent.removePlayer(oldPlayer);
					countdown.bossEvent.addPlayer(newPlayer);
				}
			}
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			dispatcher.register(Commands.literal("deathlocation")
				.executes(context -> showStatus(context.getSource().getPlayerOrException()))));
	}

	/** Starts recording every item entity added to {@code level} until the player's death has been
	 * fully processed. Death drops all happen synchronously on the server thread, so a single
	 * in-progress capture is enough. */
	public static void beginDeathDropCapture(ServerPlayer player, ServerLevel level, BlockPos pos) {
		capturingPlayer = player;
		capturingLevel = level;
		capturingPos = pos;
		capturedItemIds = new HashSet<>();
	}

	public static void captureDeathDrop(ServerLevel level, ItemEntity item) {
		if (capturingPlayer != null && level == capturingLevel) {
			capturedItemIds.add(item.getUUID());
		}
	}

	/** Ends the capture begun by {@link #beginDeathDropCapture} and registers the death, if one was
	 * being captured for {@code player}. */
	public static void finishDeathDropCapture(ServerPlayer player) {
		if (player != capturingPlayer) {
			return;
		}
		ServerLevel level = capturingLevel;
		BlockPos pos = capturingPos;
		Set<UUID> itemIds = capturedItemIds;
		capturingPlayer = null;
		capturingLevel = null;
		capturingPos = null;
		capturedItemIds = null;
		registerDeath(player, level, pos, itemIds);
	}

	/** Registers a death at {@code pos}, tracking exactly the item entities in {@code itemIds} - no
	 * others. A death with no drops (an empty {@code itemIds}) is not tracked at all. */
	public static void registerDeath(ServerPlayer player, ServerLevel level, BlockPos pos, Set<UUID> itemIds) {
		if (itemIds.isEmpty()) {
			return;
		}

		ServerBossEvent bossEvent = newBossEvent();
		int deathNumber = DEATH_NUMBERS.merge(player.getUUID(), 1, Integer::sum);
		long chunkKey = ChunkPos.containing(pos).pack();
		Countdown countdown = new Countdown(level.dimension(), pos, chunkKey, deathNumber, bossEvent, new HashSet<>(itemIds));
		List<Countdown> countdowns = COUNTDOWNS.computeIfAbsent(player.getUUID(), ignored -> new ArrayList<>());
		countdowns.add(countdown);
		updateBossBar(countdown, true, level, player);
		BelAndSighMod.LOGGER.info("[death-loc] registered death countdown for {} at {} ({} items)",
			player.getName().getString(), pos, itemIds.size());

		whisperDeathLocation(player, deathNumber, pos);

		persist(level.getServer(), player.getUUID(), countdowns);
	}

	/** A tracked item was merged into {@code survivorId} and discarded; its items now live on in the
	 * survivor, so the countdown follows that entity instead of treating the items as gone. The
	 * survivor inherits the younger age of the two, so the time estimate stays accurate. */
	public static void onItemMerged(UUID discardedId, UUID survivorId) {
		for (Map.Entry<UUID, List<Countdown>> entry : COUNTDOWNS.entrySet()) {
			for (Countdown countdown : entry.getValue()) {
				if (countdown.itemIds.remove(discardedId)) {
					countdown.itemIds.add(survivorId);
					UNSAVED_PLAYERS.add(entry.getKey());
				}
			}
		}
	}

	private static void whisperDeathLocation(ServerPlayer player, int deathNumber, BlockPos pos) {
		player.sendSystemMessage(Component.literal("Death " + deathNumber + " location: " + formatCoordinates(pos))
			.withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
	}

	private static Countdown resumeCountdown(DeathRecord record) {
		ServerBossEvent bossEvent = newBossEvent();
		long chunkKey = ChunkPos.containing(record.pos()).pack();
		return new Countdown(record.dimension(), record.pos(), chunkKey, record.deathNumber(),
			bossEvent, new HashSet<>(record.itemIds()));
	}

	private static void persist(MinecraftServer server, UUID playerId, List<Countdown> countdowns) {
		DeathLocationSavedData.get(server).set(playerId, toRecords(countdowns));
	}

	private static List<DeathRecord> toRecords(List<Countdown> countdowns) {
		List<DeathRecord> records = new ArrayList<>(countdowns.size());
		for (Countdown countdown : countdowns) {
			records.add(new DeathRecord(countdown.dimension, countdown.deathPos, countdown.deathNumber, List.copyOf(countdown.itemIds)));
		}
		return records;
	}

	private static ServerBossEvent newBossEvent() {
		return new ServerBossEvent(UUID.randomUUID(), Component.literal("Death items"),
			BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
	}

	/** The soonest any still-tracked item will naturally despawn, estimated from its own vanilla age. */
	private static int estimateRemainingTicks(ServerLevel level, Set<UUID> itemIds) {
		int minRemaining = VANILLA_ITEM_LIFETIME_TICKS;
		for (UUID id : itemIds) {
			if (level.getEntity(id) instanceof ItemEntity item) {
				int remaining = VANILLA_ITEM_LIFETIME_TICKS - item.getAge();
				minRemaining = Math.min(minRemaining, Math.max(0, remaining));
			}
		}
		return minRemaining;
	}

	private static void updateBossBar(Countdown countdown, boolean loaded, ServerLevel level, ServerPlayer player) {
		// The bar's own visible flag has proven unreliable for hiding/showing it on some clients in
		// practice, so pausing/resuming is done purely via boss-bar player membership instead - that
		// set-based add/remove is the same primitive vanilla boss fights rely on to show/hide bars
		// across dimension changes, and doesn't depend on toggling a separate visibility flag.
		if (!loaded) {
			if (player != null) {
				countdown.bossEvent.removePlayer(player);
			}
			return;
		}
		if (player != null) {
			countdown.bossEvent.addPlayer(player);
		}
		int remainingTicks = estimateRemainingTicks(level, countdown.itemIds);
		float progress = remainingTicks / (float) VANILLA_ITEM_LIFETIME_TICKS;
		countdown.bossEvent.setProgress(Math.max(0f, Math.min(1f, progress)));
		countdown.bossEvent.setName(Component.literal("Death " + countdown.deathNumber
			+ " - " + formatLocation(countdown, player) + " - " + formatTime(remainingTicks)));
	}

	private static int showStatus(ServerPlayer player) {
		List<Countdown> countdowns = COUNTDOWNS.get(player.getUUID());
		if (countdowns == null || countdowns.isEmpty()) {
			player.sendSystemMessage(Component.literal("No death-item countdown is active.").withStyle(ChatFormatting.GRAY));
			return 1;
		}
		MinecraftServer server = player.level().getServer();
		for (Countdown countdown : countdowns) {
			ServerLevel level = server.getLevel(countdown.dimension);
			boolean loaded = level != null && level.isPositionTickingWithEntitiesLoaded(countdown.chunkKey);
			String timeText = loaded ? formatTime(estimateRemainingTicks(level, countdown.itemIds)) : "paused";
			player.sendSystemMessage(Component.literal("Death " + countdown.deathNumber + " - " + formatCoordinates(countdown.deathPos)
					+ " - " + formatLocation(countdown, player) + " - " + timeText)
				.withStyle(ChatFormatting.GOLD)
				.append(Component.literal(loaded ? " (counting down)." : " (paused, chunk unloaded).").withStyle(ChatFormatting.GRAY)));
		}
		return countdowns.size();
	}

	private static String formatCoordinates(BlockPos pos) {
		return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
	}

	/** Direct-line distance is meaningless across dimensions, so this names the dimension instead. */
	private static String formatLocation(Countdown countdown, ServerPlayer player) {
		if (player == null) {
			return formatCoordinates(countdown.deathPos);
		}
		if (player.level().dimension() != countdown.dimension) {
			return countdown.dimension.identifier().toString();
		}
		return formatDistance(countdown.deathPos, player);
	}

	private static String formatDistance(BlockPos pos, ServerPlayer player) {
		double distance = Math.sqrt(player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
		return Math.round(distance) + "m";
	}

	private static String formatTime(int ticks) {
		int totalSeconds = Math.max(0, ticks) / 20;
		return (totalSeconds / 60) + ":" + String.format("%02d", totalSeconds % 60);
	}

	private static final class Countdown {
		private final ResourceKey<Level> dimension;
		private final BlockPos deathPos;
		/** Packed {@link ChunkPos} of the death chunk, for the entity-ticking check. */
		private final long chunkKey;
		private final int deathNumber;
		private final ServerBossEvent bossEvent;
		private final Set<UUID> itemIds;
		/** Ticks left before the next item-presence check and boss-bar refresh; starts at 0 so a
		 * freshly registered or resumed countdown gets an accurate display on the very next tick. */
		private int ticksUntilCheck;

		private Countdown(ResourceKey<Level> dimension, BlockPos deathPos, long chunkKey, int deathNumber,
				ServerBossEvent bossEvent, Set<UUID> itemIds) {
			this.dimension = dimension;
			this.deathPos = deathPos;
			this.chunkKey = chunkKey;
			this.deathNumber = deathNumber;
			this.bossEvent = bossEvent;
			this.itemIds = itemIds;
		}
	}
}
