package dev.belandsigh.config;

import dev.belandsigh.BelAndSighMod;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

public final class SettingsNetwork {
	private SettingsNetwork() {
	}

	public static void initialize() {
		PayloadTypeRegistry.serverboundPlay().register(RequestPayload.TYPE, RequestPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(UpdatePayload.TYPE, UpdatePayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(StatePayload.TYPE, StatePayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(RequestPayload.TYPE,
			(payload, context) -> sendState(context.player()));
		ServerPlayNetworking.registerGlobalReceiver(UpdatePayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			if (!canEdit(player)) {
				sendState(player);
				return;
			}
			if (payload.operation() == Operation.SET_FEATURE) {
				Feature feature = Feature.fromKey(payload.featureKey());
				if (feature != null) {
					ModSettings.setEnabled(feature, payload.enabled());
				}
			} else if (payload.operation() == Operation.SET_ALL) {
				ModSettings.setAll(payload.enabled());
			} else if (payload.operation() == Operation.RESTORE_DEFAULTS) {
				ModSettings.restoreDefaults();
			}
			sendState(player);
		});
	}

	private static boolean canEdit(ServerPlayer player) {
		return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
	}

	private static void sendState(ServerPlayer player) {
		ServerPlayNetworking.send(player, new StatePayload(ModSettings.mask(), canEdit(player)));
	}

	public enum Operation {
		SET_FEATURE, SET_ALL, RESTORE_DEFAULTS
	}

	public record RequestPayload() implements CustomPacketPayload {
		public static final Type<RequestPayload> TYPE = new Type<>(id("settings_request"));
		public static final StreamCodec<RegistryFriendlyByteBuf, RequestPayload> CODEC =
			CustomPacketPayload.codec((payload, buffer) -> { }, buffer -> new RequestPayload());

		@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
	}

	public record UpdatePayload(Operation operation, String featureKey, boolean enabled)
			implements CustomPacketPayload {
		public static final Type<UpdatePayload> TYPE = new Type<>(id("settings_update"));
		public static final StreamCodec<RegistryFriendlyByteBuf, UpdatePayload> CODEC =
			CustomPacketPayload.codec(UpdatePayload::write, UpdatePayload::new);

		private UpdatePayload(RegistryFriendlyByteBuf buffer) {
			this(Operation.values()[buffer.readVarInt()], buffer.readUtf(64), buffer.readBoolean());
		}

		private void write(RegistryFriendlyByteBuf buffer) {
			buffer.writeVarInt(operation.ordinal());
			buffer.writeUtf(featureKey, 64);
			buffer.writeBoolean(enabled);
		}

		public static UpdatePayload feature(Feature feature, boolean enabled) {
			return new UpdatePayload(Operation.SET_FEATURE, feature.key(), enabled);
		}

		public static UpdatePayload all(boolean enabled) {
			return new UpdatePayload(Operation.SET_ALL, "", enabled);
		}

		public static UpdatePayload defaults() {
			return new UpdatePayload(Operation.RESTORE_DEFAULTS, "", false);
		}

		@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
	}

	public record StatePayload(long enabledMask, boolean editable) implements CustomPacketPayload {
		public static final Type<StatePayload> TYPE = new Type<>(id("settings_state"));
		public static final StreamCodec<RegistryFriendlyByteBuf, StatePayload> CODEC =
			CustomPacketPayload.codec(StatePayload::write, StatePayload::new);

		private StatePayload(RegistryFriendlyByteBuf buffer) {
			this(buffer.readLong(), buffer.readBoolean());
		}

		private void write(RegistryFriendlyByteBuf buffer) {
			buffer.writeLong(enabledMask);
			buffer.writeBoolean(editable);
		}

		public boolean enabled(Feature feature) {
			return (enabledMask & 1L << feature.ordinal()) != 0;
		}

		@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(BelAndSighMod.MOD_ID, path);
	}
}
