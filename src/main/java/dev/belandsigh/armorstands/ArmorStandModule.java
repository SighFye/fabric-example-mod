package dev.belandsigh.armorstands;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.decoration.ArmorStand;

public final class ArmorStandModule {
	private ArmorStandModule() {
	}

	public static void initialize() {
		UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			if (!level.isClientSide() && entity instanceof ArmorStand armorStand
					&& ArmorStandActions.rejectLockedInteraction(player, armorStand)) {
				return InteractionResult.FAIL;
			}

			if (level.isClientSide()
					|| hand != InteractionHand.MAIN_HAND
					|| !player.isShiftKeyDown()
					|| player.isSpectator()
					|| !(player instanceof ServerPlayer serverPlayer)
					|| !(entity instanceof ArmorStand armorStand)) {
				return InteractionResult.PASS;
			}

			if (ServerPlayNetworking.canSend(serverPlayer, ArmorStandNetwork.OpenPayload.TYPE)) {
				ArmorStandNetwork.open(serverPlayer, armorStand);
			} else {
				serverPlayer.openMenu(new SimpleMenuProvider(
						(containerId, inventory, ignored) -> new ArmorStandMenu(containerId, inventory, armorStand),
						Component.literal("Armor Stand Editor")
				));
			}
			return InteractionResult.SUCCESS;
		});

		AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			if (!level.isClientSide() && entity instanceof ArmorStand armorStand
					&& ArmorStandActions.rejectLockedInteraction(player, armorStand)) {
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		});
	}
}
