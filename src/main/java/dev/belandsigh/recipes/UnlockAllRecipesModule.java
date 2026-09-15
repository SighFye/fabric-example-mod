package dev.belandsigh.recipes;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public final class UnlockAllRecipesModule {
	private UnlockAllRecipesModule() {
	}

	public static void initialize() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
			handler.getPlayer().awardRecipes(server.getRecipeManager().getRecipes())
		);

		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
			if (!success) {
				return;
			}

			var recipes = server.getRecipeManager().getRecipes();
			for (var player : server.getPlayerList().getPlayers()) {
				player.awardRecipes(recipes);
			}
		});
	}
}
