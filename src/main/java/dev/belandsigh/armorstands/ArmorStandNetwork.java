package dev.belandsigh.armorstands;

import dev.belandsigh.BelAndSighMod;
import dev.belandsigh.armorstands.ArmorStandActions.Action;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;

public final class ArmorStandNetwork {
	private ArmorStandNetwork() {
	}

	public static void initialize() {
		PayloadTypeRegistry.serverboundPlay().register(ActionPayload.TYPE, ActionPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenPayload.TYPE, OpenPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(StatePayload.TYPE, StatePayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(ActionPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			Entity entity = player.level().getEntity(payload.entityId());
			if (!(entity instanceof ArmorStand stand)
					|| !ArmorStandActions.isValidTarget(player, stand)
					|| ArmorStandActions.isLockedByOther(player, stand)) {
				return;
			}

			ArmorStandActions.apply(player, stand, payload.action(), payload.first(), payload.second(), payload.value());
			ServerPlayNetworking.send(player, StatePayload.from(stand));
		});
	}

	public static void open(ServerPlayer player, ArmorStand stand) {
		ServerPlayNetworking.send(player, OpenPayload.from(stand));
	}

	public record ActionPayload(int entityId, Action action, int first, int second, double value)
			implements CustomPacketPayload {
		public static final Type<ActionPayload> TYPE = new Type<>(
				Identifier.fromNamespaceAndPath(BelAndSighMod.MOD_ID, "armor_stand_action"));
		public static final StreamCodec<RegistryFriendlyByteBuf, ActionPayload> CODEC =
				CustomPacketPayload.codec(ActionPayload::write, ActionPayload::new);

		private ActionPayload(RegistryFriendlyByteBuf buffer) {
			this(buffer.readVarInt(), Action.fromWireName(buffer.readUtf(32)), buffer.readVarInt(), buffer.readVarInt(), buffer.readDouble());
		}

		private void write(RegistryFriendlyByteBuf buffer) {
			buffer.writeVarInt(entityId);
			buffer.writeUtf(action.wireName(), 32);
			buffer.writeVarInt(first);
			buffer.writeVarInt(second);
			buffer.writeDouble(value);
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record OpenPayload(int entityId, StatePayload state) implements CustomPacketPayload {
		public static final Type<OpenPayload> TYPE = new Type<>(
				Identifier.fromNamespaceAndPath(BelAndSighMod.MOD_ID, "open_armor_stand"));
		public static final StreamCodec<RegistryFriendlyByteBuf, OpenPayload> CODEC =
				CustomPacketPayload.codec(OpenPayload::write, OpenPayload::new);

		private OpenPayload(RegistryFriendlyByteBuf buffer) {
			this(buffer.readVarInt(), new StatePayload(buffer));
		}

		private void write(RegistryFriendlyByteBuf buffer) {
			buffer.writeVarInt(entityId);
			state.write(buffer);
		}

		public static OpenPayload from(ArmorStand stand) {
			return new OpenPayload(stand.getId(), StatePayload.from(stand));
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record StatePayload(boolean basePlate, boolean arms, boolean small, boolean gravity,
			boolean visible, boolean nameVisible, boolean locked, boolean invulnerable) implements CustomPacketPayload {
		public static final Type<StatePayload> TYPE = new Type<>(
				Identifier.fromNamespaceAndPath(BelAndSighMod.MOD_ID, "armor_stand_state"));
		public static final StreamCodec<RegistryFriendlyByteBuf, StatePayload> CODEC =
				CustomPacketPayload.codec(StatePayload::write, StatePayload::new);

		private StatePayload(RegistryFriendlyByteBuf buffer) {
			this(buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(),
					buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean());
		}

		private void write(RegistryFriendlyByteBuf buffer) {
			buffer.writeBoolean(basePlate);
			buffer.writeBoolean(arms);
			buffer.writeBoolean(small);
			buffer.writeBoolean(gravity);
			buffer.writeBoolean(visible);
			buffer.writeBoolean(nameVisible);
			buffer.writeBoolean(locked);
			buffer.writeBoolean(invulnerable);
		}

		public static StatePayload from(ArmorStand stand) {
			return new StatePayload(stand.showBasePlate(), stand.showArms(), stand.isSmall(), !stand.isNoGravity(),
					!stand.isInvisible(), stand.isCustomNameVisible(), ArmorStandActions.isLocked(stand), stand.isInvulnerable());
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
}
