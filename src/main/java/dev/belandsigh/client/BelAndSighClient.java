package dev.belandsigh.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.belandsigh.BelAndSighMod;
import dev.belandsigh.armorstands.ArmorStandNetwork.OpenPayload;
import dev.belandsigh.armorstands.ArmorStandNetwork.StatePayload;
import dev.belandsigh.furnacexp.FurnaceXpModule.StoredXpPayload;
import dev.belandsigh.mounts.MountNetworking.ManagementDataPayload;
import dev.belandsigh.mounts.MountNetworking.SelectionStatePayload;
import dev.belandsigh.config.Feature;
import dev.belandsigh.config.SettingsNetwork;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class BelAndSighClient implements ClientModInitializer {
	private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(
		Identifier.fromNamespaceAndPath(BelAndSighMod.MOD_ID, "general"));
	private static final KeyMapping OPEN_SETTINGS = KeyMappingHelper.registerKeyMapping(new KeyMapping(
		"key.belandsigh.open_settings", InputConstants.Type.KEYBOARD, InputConstants.KEY_B, KEY_CATEGORY));

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (OPEN_SETTINGS.consumeClick()) {
				client.setScreenAndShow(new BelAndSighSettingsScreen(null));
			}
		});
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
		ClientPlayNetworking.registerGlobalReceiver(SettingsNetwork.StatePayload.TYPE, (payload, context) ->
				SettingsClientState.update(payload));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			FurnaceXpClientState.reset();
			SettingsClientState.reset();
		});

		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			if (!SettingsClientState.enabled(Feature.DURABILITY_DISPLAY) || !stack.isDamageableItem()) {
				return;
			}
			int remaining = stack.getMaxDamage() - stack.getDamageValue();
			lines.add(Component.translatable("tooltip.belandsigh.durability", remaining, stack.getMaxDamage())
				.withStyle(ChatFormatting.GRAY));
		});
	}
}
