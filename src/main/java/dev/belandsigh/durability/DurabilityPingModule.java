package dev.belandsigh.durability;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class DurabilityPingModule {
	private static final int COOLDOWN_TICKS = 60;
	private static final Map<UUID, PlayerState> STATES = new HashMap<>();
	private static final EquipmentSlot[] SLOTS = {
		EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
		EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
	};

	private DurabilityPingModule() {
	}

	public static void initialize() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				checkPlayer(player);
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> STATES.remove(handler.getPlayer().getUUID()));

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			dispatcher.register(Commands.literal("durabilityping")
				.executes(context -> showSettings(context.getSource().getPlayerOrException()))
				.then(Commands.literal("hand").then(Commands.argument("enabled", BoolArgumentType.bool())
					.executes(context -> setHand(context.getSource().getPlayerOrException(), BoolArgumentType.getBool(context, "enabled")))))
				.then(Commands.literal("armor").then(Commands.argument("enabled", BoolArgumentType.bool())
					.executes(context -> setArmor(context.getSource().getPlayerOrException(), BoolArgumentType.getBool(context, "enabled")))))
				.then(Commands.literal("sound").then(Commands.argument("enabled", BoolArgumentType.bool())
					.executes(context -> setSound(context.getSource().getPlayerOrException(), BoolArgumentType.getBool(context, "enabled")))))
				.then(Commands.literal("display").then(Commands.argument("mode", StringArgumentType.word())
					.suggests((context, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
						new String[]{"hidden", "subtitle", "title", "chat", "actionbar"}, builder))
					.executes(context -> setDisplay(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "mode")))))
			)
		);
	}

	private static void checkPlayer(ServerPlayer player) {
		PlayerState state = STATES.computeIfAbsent(player.getUUID(), ignored -> new PlayerState());
		if (state.handCooldown > 0) state.handCooldown--;
		if (state.armorCooldown > 0) state.armorCooldown--;

		DurabilityPingPreferences preferences = (DurabilityPingPreferences) player;
		for (int index = 0; index < SLOTS.length; index++) {
			EquipmentSlot slot = SLOTS[index];
			ItemStack stack = player.getItemBySlot(slot);
			Snapshot previous = state.slots[index];
			Snapshot current = Snapshot.of(stack);
			state.slots[index] = current;

			if (previous == null || current == null || previous.item != current.item || current.damage <= previous.damage) {
				continue;
			}
			int remaining = stack.getMaxDamage() - stack.getDamageValue();
			if (remaining < 1 || remaining * 10 > stack.getMaxDamage()) {
				continue;
			}

			boolean hand = slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND;
			if (hand && preferences.belandsigh$handPingsEnabled() && state.handCooldown == 0) {
				ping(player, stack, remaining, preferences);
				state.handCooldown = COOLDOWN_TICKS;
			} else if (!hand && preferences.belandsigh$armorPingsEnabled() && state.armorCooldown == 0) {
				ping(player, stack, remaining, preferences);
				state.armorCooldown = COOLDOWN_TICKS;
			}
		}
	}

	private static void ping(ServerPlayer player, ItemStack stack, int remaining, DurabilityPingPreferences preferences) {
		Component itemName = stack.getHoverName().copy().withStyle(ChatFormatting.GOLD);
		Component warning = itemName.copy()
			.append(Component.literal(" durability low! ").withStyle(ChatFormatting.RED))
			.append(Component.literal(Integer.toString(remaining)).withStyle(ChatFormatting.GOLD))
			.append(Component.literal(" of " + stack.getMaxDamage() + " remaining.").withStyle(ChatFormatting.RED));

		if (preferences.belandsigh$pingSoundEnabled()) {
			player.connection.send(new ClientboundSoundPacket(
				Holder.direct(SoundEvents.ANVIL_LAND), SoundSource.MASTER,
				player.getX(), player.getY(), player.getZ(), 1.0F, 2.0F, player.getRandom().nextLong()
			));
		}

		switch (preferences.belandsigh$pingDisplay()) {
			case HIDDEN -> { }
			case SUBTITLE -> {
				player.connection.send(new ClientboundClearTitlesPacket(true));
				player.connection.send(new ClientboundSetTitleTextPacket(Component.empty()));
				player.connection.send(new ClientboundSetSubtitleTextPacket(warning));
			}
			case TITLE -> {
				player.connection.send(new ClientboundClearTitlesPacket(true));
				player.connection.send(new ClientboundSetTitleTextPacket(
					itemName.copy().append(Component.literal(" durability low!").withStyle(ChatFormatting.RED))));
				player.connection.send(new ClientboundSetSubtitleTextPacket(
					Component.literal(remaining + " of " + stack.getMaxDamage() + " remaining.").withStyle(ChatFormatting.RED)));
			}
			case CHAT -> player.sendSystemMessage(warning);
			case ACTIONBAR -> player.sendOverlayMessage(warning);
		}
	}

	private static int showSettings(ServerPlayer player) {
		DurabilityPingPreferences preferences = (DurabilityPingPreferences) player;
		player.sendSystemMessage(Component.literal("Durability Ping settings").withStyle(ChatFormatting.GOLD));
		player.sendSystemMessage(Component.literal("Hand: " + onOff(preferences.belandsigh$handPingsEnabled())
			+ ", armor: " + onOff(preferences.belandsigh$armorPingsEnabled())
			+ ", sound: " + onOff(preferences.belandsigh$pingSoundEnabled())
			+ ", display: " + preferences.belandsigh$pingDisplay().name().toLowerCase(Locale.ROOT)));
		player.sendSystemMessage(Component.literal("Use /durabilityping hand|armor|sound <true|false> or /durabilityping display <mode>.")
			.withStyle(ChatFormatting.GRAY));
		return 1;
	}

	private static int setHand(ServerPlayer player, boolean enabled) {
		((DurabilityPingPreferences) player).belandsigh$setHandPingsEnabled(enabled);
		return confirm(player, "Hand-item pings", enabled);
	}

	private static int setArmor(ServerPlayer player, boolean enabled) {
		((DurabilityPingPreferences) player).belandsigh$setArmorPingsEnabled(enabled);
		return confirm(player, "Armor pings", enabled);
	}

	private static int setSound(ServerPlayer player, boolean enabled) {
		((DurabilityPingPreferences) player).belandsigh$setPingSoundEnabled(enabled);
		return confirm(player, "Ping sound", enabled);
	}

	private static int setDisplay(ServerPlayer player, String name) {
		try {
			Display display = Display.valueOf(name.toUpperCase(Locale.ROOT));
			((DurabilityPingPreferences) player).belandsigh$setPingDisplay(display);
			player.sendSystemMessage(Component.literal("Durability Ping display set to " + name.toLowerCase(Locale.ROOT) + ".")
				.withStyle(ChatFormatting.GREEN));
			return 1;
		} catch (IllegalArgumentException exception) {
			player.sendSystemMessage(Component.literal("Unknown display mode. Use hidden, subtitle, title, chat, or actionbar.")
				.withStyle(ChatFormatting.RED));
			return 0;
		}
	}

	private static int confirm(ServerPlayer player, String setting, boolean enabled) {
		player.sendSystemMessage(Component.literal(setting + " " + (enabled ? "enabled." : "disabled."))
			.withStyle(ChatFormatting.GREEN));
		return 1;
	}

	private static String onOff(boolean value) {
		return value ? "on" : "off";
	}

	public enum Display {
		HIDDEN, SUBTITLE, TITLE, CHAT, ACTIONBAR
	}

	private static final class PlayerState {
		private final Snapshot[] slots = new Snapshot[SLOTS.length];
		private int handCooldown;
		private int armorCooldown;
	}

	private record Snapshot(net.minecraft.world.item.Item item, int damage) {
		private static Snapshot of(ItemStack stack) {
			return stack.isDamageableItem() ? new Snapshot(stack.getItem(), stack.getDamageValue()) : null;
		}
	}
}
