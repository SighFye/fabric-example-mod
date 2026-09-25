package dev.belandsigh.playerheads;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;

import java.util.List;
import java.util.regex.Pattern;

public final class PlayerHeadsModule {
	private static final Pattern VALID_PLAYER_NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");

	private PlayerHeadsModule() {
	}

	public static void initialize() {
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
			if (entity instanceof ServerPlayer player) {
				dropHead(player, damageSource.getEntity());
			}
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			dispatcher.register(Commands.literal("playerhead")
				.requires(source -> source.isPlayer()
					&& source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
				.then(Commands.argument("player", StringArgumentType.word())
					.executes(context -> giveHead(
						context.getSource(),
						StringArgumentType.getString(context, "player")
					))))
		);
	}

	private static void dropHead(ServerPlayer player, net.minecraft.world.entity.Entity killer) {
		ItemStack head = new ItemStack(Items.PLAYER_HEAD);
		head.set(DataComponents.PROFILE, player.getProfile());

		if (killer instanceof Player killingPlayer) {
			Component lore = Component.literal("Killed by ")
				.withStyle(ChatFormatting.GOLD)
				.append(Component.literal(killingPlayer.getName().getString()).withStyle(ChatFormatting.YELLOW));
			head.set(DataComponents.LORE, new ItemLore(List.of(lore)));
		}

		ItemEntity drop = player.createItemStackToDrop(head, true, false);
		if (drop != null) {
			player.level().addFreshEntity(drop);
		}
	}

	private static int giveHead(CommandSourceStack source, String playerName) {
		if (!VALID_PLAYER_NAME.matcher(playerName).matches()) {
			source.sendFailure(Component.literal(
				"Player names may contain only letters, numbers, and underscores and must be at most 16 characters long."
			));
			return 0;
		}

		var server = source.getServer();
		var recipient = source.getPlayer();
		if (recipient == null) {
			source.sendFailure(Component.literal("This command must be run by a player."));
			return 0;
		}
		var requestedProfile = ResolvableProfile.createUnresolved(playerName);
		requestedProfile.resolveProfile(server.services().profileResolver())
			.whenCompleteAsync((profile, error) -> {
				if (error != null) {
					source.sendFailure(Component.literal("Could not look up the player " + playerName + "."));
					return;
				}

				ItemStack head = new ItemStack(Items.PLAYER_HEAD);
				head.set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile));
				if (!recipient.getInventory().add(head)) {
					ItemEntity drop = recipient.createItemStackToDrop(head, false, false);
					if (drop != null) {
						recipient.level().addFreshEntity(drop);
					}
				}
				source.sendSuccess(
					() -> Component.literal("Gave you " + playerName + "'s head.").withStyle(ChatFormatting.GREEN),
					false
				);
			}, server);

		source.sendSystemMessage(Component.literal("Looking up " + playerName + "...").withStyle(ChatFormatting.GRAY));
		return 1;
	}
}
