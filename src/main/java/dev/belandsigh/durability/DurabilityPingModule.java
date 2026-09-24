package dev.belandsigh.durability;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
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

import java.util.Locale;

public final class DurabilityPingModule {
	public static final int COOLDOWN_TICKS = DurabilityThresholds.COOLDOWN_TICKS;
	private static final EquipmentSlot[] SLOTS = {
		EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
		EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
	};

	private DurabilityPingModule() {
	}

	public static void initialize() {
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

	/** Called as a player's item takes durability damage; only the stack actually being damaged is inspected. */
	public static void onDamage(ServerPlayer player, ItemStack stack, int oldDamage, int newDamage) {
		int maxDamage = stack.getMaxDamage();
		if (!DurabilityThresholds.crossed(maxDamage - oldDamage, maxDamage - newDamage)) {
			return;
		}

		EquipmentSlot slot = equippedSlot(player, stack);
		if (slot == null) {
			return;
		}
		boolean hand = slot.getType() == EquipmentSlot.Type.HAND;
		DurabilityPingPreferences preferences = (DurabilityPingPreferences) player;
		if (!(hand ? preferences.belandsigh$handPingsEnabled() : preferences.belandsigh$armorPingsEnabled())) {
			return;
		}

		DurabilityPingCooldowns cooldowns = (DurabilityPingCooldowns) player;
		long now = player.level().getGameTime();
		long lastPing = hand ? cooldowns.belandsigh$lastHandPing() : cooldowns.belandsigh$lastArmorPing();
		if (!DurabilityThresholds.offCooldown(now, lastPing)) {
			return;
		}

		ping(player, stack, maxDamage - newDamage, preferences);
		if (hand) {
			cooldowns.belandsigh$setLastHandPing(now);
		} else {
			cooldowns.belandsigh$setLastArmorPing(now);
		}
	}

	/** Identity match, so a same-type tool elsewhere in the inventory is never confused with the equipped one. */
	private static EquipmentSlot equippedSlot(ServerPlayer player, ItemStack stack) {
		for (EquipmentSlot slot : SLOTS) {
			if (player.getItemBySlot(slot) == stack) {
				return slot;
			}
		}
		return null;
	}

	private static void ping(ServerPlayer player, ItemStack stack, int remaining, DurabilityPingPreferences preferences) {
		Component itemName = stack.getHoverName().copy().withStyle(ChatFormatting.GOLD);
		Component header = itemName.copy().append(Component.literal(" durability low!").withStyle(ChatFormatting.RED));
		Component remainingSuffix = Component.literal(" of " + stack.getMaxDamage() + " remaining.").withStyle(ChatFormatting.RED);
		Component warning = header.copy()
			.append(Component.literal(" "))
			.append(Component.literal(Integer.toString(remaining)).withStyle(ChatFormatting.GOLD))
			.append(remainingSuffix);

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
				player.connection.send(new ClientboundSetTitleTextPacket(header));
				player.connection.send(new ClientboundSetSubtitleTextPacket(
					Component.literal(Integer.toString(remaining)).withStyle(ChatFormatting.RED).append(remainingSuffix)));
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

}
