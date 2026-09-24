package dev.belandsigh.furnacexp;

import dev.belandsigh.BelAndSighMod;
import dev.belandsigh.mixin.AbstractFurnaceBlockEntityAccessor;
import dev.belandsigh.mixin.AbstractFurnaceMenuAccessor;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Shows the experience a furnace, blast furnace, or smoker has stored and lets whoever has it
 * open collect it with a button. Vanilla already records every smelted recipe in the furnace's
 * {@code recipesUsed} map and only pays it out when the result slot is taken from or the block
 * is broken; this module reads that map for display and triggers the same vanilla payout, so the
 * experience still arrives as orbs at the player (Mending applies) and the map is cleared.
 *
 * <p>Collection goes through vanilla's menu button packet (id {@link #COLLECT_BUTTON_ID}), which
 * already validates the container id and {@code stillValid} range check server side.
 */
public final class FurnaceXpModule {
	public static final int COLLECT_BUTTON_ID = 0;
	private static final Map<UUID, SentState> LAST_SENT = new HashMap<>();

	private FurnaceXpModule() {
	}

	public static void initialize() {
		PayloadTypeRegistry.clientboundPlay().register(StoredXpPayload.TYPE, StoredXpPayload.CODEC);

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				syncOpenFurnace(player);
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> LAST_SENT.remove(handler.getPlayer().getUUID()));
	}

	/** Called from the menu button hook; returns true when the click was ours and was handled. */
	public static boolean handleMenuButton(AbstractContainerMenu menu, Player player, int buttonId) {
		if (buttonId != COLLECT_BUTTON_ID || !(player instanceof ServerPlayer serverPlayer)) {
			return false;
		}
		AbstractFurnaceBlockEntity furnace = furnaceFor(menu);
		if (furnace == null) {
			return false;
		}
		furnace.awardUsedRecipesAndPopExperience(serverPlayer);
		furnace.setChanged();
		return true;
	}

	private static void syncOpenFurnace(ServerPlayer player) {
		AbstractFurnaceBlockEntity furnace = furnaceFor(player.containerMenu);
		if (furnace == null || !(furnace.getLevel() instanceof ServerLevel level)) {
			LAST_SENT.remove(player.getUUID());
			return;
		}
		if (!ServerPlayNetworking.canSend(player, StoredXpPayload.TYPE)) {
			return;
		}
		int containerId = player.containerMenu.containerId;
		int tenths = FurnaceXpMath.toTenths(storedExperience(furnace, level));
		SentState sent = new SentState(containerId, tenths);
		if (!sent.equals(LAST_SENT.get(player.getUUID()))) {
			LAST_SENT.put(player.getUUID(), sent);
			ServerPlayNetworking.send(player, new StoredXpPayload(containerId, tenths));
		}
	}

	private static AbstractFurnaceBlockEntity furnaceFor(AbstractContainerMenu menu) {
		// Server-side furnace menus hold the block entity itself as their container; client-side
		// menus hold a plain SimpleContainer, so this also filters out anything not server-backed.
		if (menu instanceof AbstractFurnaceMenu furnaceMenu
				&& ((AbstractFurnaceMenuAccessor) furnaceMenu).belandsigh$getContainer() instanceof AbstractFurnaceBlockEntity furnace) {
			return furnace;
		}
		return null;
	}

	/** Sums the experience vanilla would award for this furnace's recorded recipes. */
	public static double storedExperience(AbstractFurnaceBlockEntity furnace, ServerLevel level) {
		RecipeManager recipes = level.recipeAccess();
		double total = 0.0;
		for (Reference2IntMap.Entry<ResourceKey<Recipe<?>>> entry
				: ((AbstractFurnaceBlockEntityAccessor) furnace).belandsigh$getRecipesUsed().reference2IntEntrySet()) {
			Optional<RecipeHolder<?>> holder = recipes.byKey(entry.getKey());
			// Recipes removed by a datapack since smelting are skipped, just as vanilla skips them.
			if (holder.isPresent() && holder.get().value() instanceof AbstractCookingRecipe cooking) {
				total += FurnaceXpMath.experienceFor(entry.getIntValue(), cooking.experience());
			}
		}
		return total;
	}

	private record SentState(int containerId, int tenths) {
	}

	public record StoredXpPayload(int containerId, int tenths) implements CustomPacketPayload {
		public static final Type<StoredXpPayload> TYPE = new Type<>(
			Identifier.fromNamespaceAndPath(BelAndSighMod.MOD_ID, "furnace_stored_xp"));
		public static final StreamCodec<RegistryFriendlyByteBuf, StoredXpPayload> CODEC =
			CustomPacketPayload.codec(StoredXpPayload::write, StoredXpPayload::new);

		private StoredXpPayload(RegistryFriendlyByteBuf buffer) {
			this(buffer.readVarInt(), Math.max(0, buffer.readVarInt()));
		}

		private void write(RegistryFriendlyByteBuf buffer) {
			buffer.writeVarInt(containerId);
			buffer.writeVarInt(tenths);
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
}
