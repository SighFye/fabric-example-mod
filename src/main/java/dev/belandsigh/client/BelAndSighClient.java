package dev.belandsigh.client;

import dev.belandsigh.armorstands.ArmorStandNetwork.OpenPayload;
import dev.belandsigh.armorstands.ArmorStandNetwork.StatePayload;
import dev.belandsigh.furnacexp.FurnaceXpModule.StoredXpPayload;
import dev.belandsigh.mounts.MountNetworking.ManagementDataPayload;
import dev.belandsigh.mounts.MountNetworking.SelectionStatePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class BelAndSighClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(OpenPayload.TYPE, (payload, context) ->
				context.client().setScreenAndShow(new ArmorStandEditorScreen(payload.entityId(), payload.state())));
		ClientPlayNetworking.registerGlobalReceiver(StatePayload.TYPE, (payload, context) ->
				ArmorStandEditorScreen.updateCurrentState(payload));
		ClientPlayNetworking.registerGlobalReceiver(ManagementDataPayload.TYPE, (payload, context) ->
				MountManagementScreen.updateManagementData(payload));
		ClientPlayNetworking.registerGlobalReceiver(SelectionStatePayload.TYPE, (payload, context) ->
				MountManagementScreen.updateSelections(payload));
		ClientPlayNetworking.registerGlobalReceiver(StoredXpPayload.TYPE, (payload, context) ->
				FurnaceXpClientState.update(payload));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> FurnaceXpClientState.reset());

		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			if (!stack.isDamageableItem()) {
				return;
			}
			int remaining = stack.getMaxDamage() - stack.getDamageValue();
			lines.add(Component.translatable("tooltip.belandsigh.durability", remaining, stack.getMaxDamage())
				.withStyle(ChatFormatting.GRAY));
		});
	}
}
