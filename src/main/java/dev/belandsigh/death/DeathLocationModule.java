package dev.belandsigh.death;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks how much longer a player's death location has been loaded in, and shows it on a boss bar
 * HUD from the moment the countdown starts. The countdown only ticks down while the death chunk is
 * loaded, and pauses while it is unloaded. When it reaches zero, any item entities still sitting
 * near the death position are cleared out - this works regardless of which code path actually
 * spawned the drops, since it doesn't rely on tagging individual items.
 */
public final class DeathLocationModule {
	private static final double SWEEP_RADIUS = 8.0;
	private static final Map<UUID, Countdown> COUNTDOWNS = new HashMap<>();

	private DeathLocationModule() {
	}

	public static void initialize() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			Iterator<Countdown> iterator = COUNTDOWNS.values().iterator();
			while (iterator.hasNext()) {
				Countdown countdown = iterator.next();
				ServerLevel level = server.getLevel(countdown.dimension);
				boolean loaded = level != null && level.getChunkSource().hasChunk(countdown.chunkX, countdown.chunkZ);
				if (loaded) {
					countdown.remainingTicks--;
				}
				if (level == null || countdown.remainingTicks <= 0) {
					if (loaded) {
						sweepItems(level, countdown.deathPos);
					}
					countdown.bossEvent.removeAllPlayers();
					iterator.remove();
				} else {
					updateBossBar(countdown, true);
				}
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> COUNTDOWNS.clear());
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			Countdown countdown = COUNTDOWNS.get(handler.getPlayer().getUUID());
			if (countdown != null) {
				countdown.bossEvent.removePlayer(handler.getPlayer());
			}
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			Countdown countdown = COUNTDOWNS.get(handler.getPlayer().getUUID());
			if (countdown != null) {
				countdown.bossEvent.addPlayer(handler.getPlayer());
			}
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			dispatcher.register(Commands.literal("deathlocation")
				.executes(context -> showStatus(context.getSource().getPlayerOrException()))));
	}

	public static void registerDeath(ServerPlayer player, ServerLevel level, BlockPos pos, int despawnTicks) {
		Countdown existing = COUNTDOWNS.remove(player.getUUID());
		if (existing != null) {
			existing.bossEvent.removeAllPlayers();
		}

		ChunkPos chunk = ChunkPos.containing(pos);
		ServerBossEvent bossEvent = new ServerBossEvent(UUID.randomUUID(), Component.literal("Death items"),
			BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
		bossEvent.addPlayer(player);

		Countdown countdown = new Countdown(level.dimension(), pos, chunk.x(), chunk.z(), despawnTicks, bossEvent);
		COUNTDOWNS.put(player.getUUID(), countdown);
		updateBossBar(countdown, true);
	}

	private static void sweepItems(ServerLevel level, BlockPos deathPos) {
		AABB area = new AABB(deathPos).inflate(SWEEP_RADIUS);
		level.getEntities(EntityTypeTest.forClass(ItemEntity.class), area, item -> true)
			.forEach(ItemEntity::discard);
	}

	private static void updateBossBar(Countdown countdown, boolean loaded) {
		float progress = countdown.remainingTicks / (float) countdown.totalTicks;
		countdown.bossEvent.setProgress(Math.max(0f, Math.min(1f, progress)));
		countdown.bossEvent.setColor(loaded ? BossEvent.BossBarColor.YELLOW : BossEvent.BossBarColor.WHITE);
		countdown.bossEvent.setName(Component.literal("Death items despawn in " + formatTime(countdown.remainingTicks))
			.append(Component.literal(loaded ? "" : " (paused, chunk unloaded)").withStyle(ChatFormatting.GRAY)));
	}

	private static int showStatus(ServerPlayer player) {
		Countdown countdown = COUNTDOWNS.get(player.getUUID());
		if (countdown == null) {
			player.sendSystemMessage(Component.literal("No death-item countdown is active.").withStyle(ChatFormatting.GRAY));
			return 1;
		}
		ServerLevel level = player.level().getServer().getLevel(countdown.dimension);
		boolean loaded = level != null && level.getChunkSource().hasChunk(countdown.chunkX, countdown.chunkZ);
		player.sendSystemMessage(Component.literal("Death items despawn in " + formatTime(countdown.remainingTicks) + " of loaded time")
			.withStyle(ChatFormatting.GOLD)
			.append(Component.literal(loaded ? " (counting down)." : " (paused, chunk unloaded).").withStyle(ChatFormatting.GRAY)));
		return 1;
	}

	private static String formatTime(int ticks) {
		int totalSeconds = Math.max(0, ticks) / 20;
		return (totalSeconds / 60) + ":" + String.format("%02d", totalSeconds % 60);
	}

	private static final class Countdown {
		private final ResourceKey<Level> dimension;
		private final BlockPos deathPos;
		private final int chunkX;
		private final int chunkZ;
		private final int totalTicks;
		private final ServerBossEvent bossEvent;
		private int remainingTicks;

		private Countdown(ResourceKey<Level> dimension, BlockPos deathPos, int chunkX, int chunkZ, int totalTicks,
				ServerBossEvent bossEvent) {
			this.dimension = dimension;
			this.deathPos = deathPos;
			this.chunkX = chunkX;
			this.chunkZ = chunkZ;
			this.totalTicks = totalTicks;
			this.remainingTicks = totalTicks;
			this.bossEvent = bossEvent;
		}
	}
}
