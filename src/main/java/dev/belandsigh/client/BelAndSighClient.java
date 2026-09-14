package dev.belandsigh.client;

import dev.belandsigh.armorstands.ArmorStandNetwork.OpenPayload;
import dev.belandsigh.armorstands.ArmorStandNetwork.StatePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class BelAndSighClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(OpenPayload.TYPE, (payload, context) ->
				context.client().setScreenAndShow(new ArmorStandEditorScreen(payload.entityId(), payload.state())));
		ClientPlayNetworking.registerGlobalReceiver(StatePayload.TYPE, (payload, context) ->
				ArmorStandEditorScreen.updateCurrentState(payload));
	}
}
