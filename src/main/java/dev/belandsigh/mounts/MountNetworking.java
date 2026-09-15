package dev.belandsigh.mounts;

import dev.belandsigh.BelAndSighMod;
import dev.belandsigh.mounts.PlayerMountSelectionService.SelectionResult;
import dev.belandsigh.mounts.MountBindingService.UnbindResult;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** Server-authoritative selection networking shared with the Stage 4 UI. */
public final class MountNetworking {
	private static final int MAX_MOUNT_SUMMARIES = 512;
	private static final int MAX_DISPLAY_TEXT_LENGTH = 128;

	private MountNetworking() {
	}

	public static void initialize() {
		PayloadTypeRegistry.serverboundPlay().register(SetSelectionPayload.TYPE, SetSelectionPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(RequestManagementDataPayload.TYPE, RequestManagementDataPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(UnbindMountPayload.TYPE, UnbindMountPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SelectionStatePayload.TYPE, SelectionStatePayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ManagementDataPayload.TYPE, ManagementDataPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(SetSelectionPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			SelectionResult result = PlayerMountSelectionService.setSelectedMount(
				player, payload.category(), payload.mountUuid().orElse(null));
			player.sendOverlayMessage(resultMessage(result, payload.category()));
			syncSelections(player);
		});

		ServerPlayNetworking.registerGlobalReceiver(RequestManagementDataPayload.TYPE, (payload, context) ->
			syncManagementData(context.player()));

		ServerPlayNetworking.registerGlobalReceiver(UnbindMountPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			Entity mount = player.level().getEntityInAnyDimension(payload.mountUuid());
			UnbindResult result = mount == null
				? UnbindResult.NOT_FOUND
				: MountBindingService.unbindOwnedMount(player, mount);
			player.sendOverlayMessage(unbindResultMessage(result));
			syncManagementData(player);
		});
	}

	public static void syncSelections(ServerPlayer player) {
		if (ServerPlayNetworking.canSend(player, SelectionStatePayload.TYPE)) {
			ServerPlayNetworking.send(player, SelectionStatePayload.from(player));
		}
	}

	public static void syncManagementData(ServerPlayer player) {
		if (!ServerPlayNetworking.canSend(player, ManagementDataPayload.TYPE)) {
			return;
		}
		for (MountCategory category : MountCategory.values()) {
			PlayerMountSelectionService.validateSelection(player, category);
		}
		ServerPlayNetworking.send(player, new ManagementDataPayload(
			SelectionStatePayload.from(player), collectOwnedMounts(player)));
	}

	private static List<MountSummary> collectOwnedMounts(ServerPlayer player) {
		// Refresh catalog metadata for everything currently loaded before reading
		// the persistent list, so newly tamed or renamed mounts appear immediately.
		for (ServerLevel level : player.level().getServer().getAllLevels()) {
			for (Entity entity : level.getAllEntities()) {
				MountLocationIndex.record(entity, level);
			}
		}

		List<MountSummary> mounts = new ArrayList<>();
		for (MountLocationIndex.Location mount : MountLocationIndex.catalogForOwner(
				player.level().getServer(), player.getUUID())) {
			mounts.add(new MountSummary(
				mount.uuid(), truncate(mount.displayName()), truncate(mount.typeName()),
				truncate(mount.dimension().identifier().toString()), mount.categoryMask(), mount.bound()));
		}
		mounts.sort(Comparator.comparing(MountSummary::displayName, String.CASE_INSENSITIVE_ORDER)
			.thenComparing(MountSummary::mountUuid));
		return mounts.stream().limit(MAX_MOUNT_SUMMARIES).toList();
	}

	private static String truncate(String value) {
		return value.length() <= MAX_DISPLAY_TEXT_LENGTH
			? value
			: value.substring(0, MAX_DISPLAY_TEXT_LENGTH);
	}

	private static Component resultMessage(SelectionResult result, MountCategory category) {
		String categoryName = category.name().toLowerCase(Locale.ROOT);
		return switch (result) {
			case SUCCESS -> Component.translatable("message.belandsigh.mount_selection.success", categoryName);
			case CLEARED -> Component.translatable("message.belandsigh.mount_selection.cleared", categoryName);
			case DEAD -> Component.translatable("message.belandsigh.mount_selection.dead");
			case NOT_OWNED -> Component.translatable("message.belandsigh.mount_selection.not_owned");
			case INCOMPATIBLE -> Component.translatable("message.belandsigh.mount_selection.incompatible", categoryName);
			case NOT_FOUND, NO_SELECTION -> Component.translatable("message.belandsigh.mount_selection.not_found");
		};
	}

	private static Component unbindResultMessage(UnbindResult result) {
		return switch (result) {
			case SUCCESS -> Component.translatable("message.belandsigh.mount_unbind.success");
			case NATIVE_OWNERSHIP -> Component.translatable("message.belandsigh.mount_unbind.native");
			case NOT_OWNER -> Component.translatable("message.belandsigh.mount_unbind.not_owner");
			case NOT_FOUND, INELIGIBLE -> Component.translatable("message.belandsigh.mount_unbind.not_found");
		};
	}

	public record RequestManagementDataPayload() implements CustomPacketPayload {
		public static final Type<RequestManagementDataPayload> TYPE = new Type<>(
			Identifier.fromNamespaceAndPath(BelAndSighMod.MOD_ID, "request_mount_management_data"));
		public static final StreamCodec<RegistryFriendlyByteBuf, RequestManagementDataPayload> CODEC =
			CustomPacketPayload.codec(RequestManagementDataPayload::write, RequestManagementDataPayload::new);

		private RequestManagementDataPayload(RegistryFriendlyByteBuf ignored) {
			this();
		}

		private void write(RegistryFriendlyByteBuf ignored) {
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record UnbindMountPayload(UUID mountUuid) implements CustomPacketPayload {
		public static final Type<UnbindMountPayload> TYPE = new Type<>(
			Identifier.fromNamespaceAndPath(BelAndSighMod.MOD_ID, "unbind_mount"));
		public static final StreamCodec<RegistryFriendlyByteBuf, UnbindMountPayload> CODEC =
			CustomPacketPayload.codec(UnbindMountPayload::write, UnbindMountPayload::new);

		private UnbindMountPayload(RegistryFriendlyByteBuf buffer) {
			this(buffer.readUUID());
		}

		private void write(RegistryFriendlyByteBuf buffer) {
			buffer.writeUUID(mountUuid);
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record SetSelectionPayload(MountCategory category, Optional<UUID> mountUuid)
			implements CustomPacketPayload {
		public static final Type<SetSelectionPayload> TYPE = new Type<>(
			Identifier.fromNamespaceAndPath(BelAndSighMod.MOD_ID, "set_mount_selection"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SetSelectionPayload> CODEC =
			CustomPacketPayload.codec(SetSelectionPayload::write, SetSelectionPayload::new);

		private SetSelectionPayload(RegistryFriendlyByteBuf buffer) {
			this(MountCategory.valueOf(buffer.readUtf(16)), readOptionalUuid(buffer));
		}

		private void write(RegistryFriendlyByteBuf buffer) {
			buffer.writeUtf(category.name());
			writeOptionalUuid(buffer, mountUuid);
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record SelectionStatePayload(Optional<UUID> landMountUuid, Optional<UUID> waterMountUuid,
			Optional<UUID> lavaMountUuid) implements CustomPacketPayload {
		public static final Type<SelectionStatePayload> TYPE = new Type<>(
			Identifier.fromNamespaceAndPath(BelAndSighMod.MOD_ID, "mount_selection_state"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SelectionStatePayload> CODEC =
			CustomPacketPayload.codec(SelectionStatePayload::write, SelectionStatePayload::new);

		private SelectionStatePayload(RegistryFriendlyByteBuf buffer) {
			this(readOptionalUuid(buffer), readOptionalUuid(buffer), readOptionalUuid(buffer));
		}

		private void write(RegistryFriendlyByteBuf buffer) {
			writeOptionalUuid(buffer, landMountUuid);
			writeOptionalUuid(buffer, waterMountUuid);
			writeOptionalUuid(buffer, lavaMountUuid);
		}

		public static SelectionStatePayload from(ServerPlayer player) {
			return new SelectionStatePayload(
				PlayerMountSelectionService.getSelectedMountUuid(player, MountCategory.LAND),
				PlayerMountSelectionService.getSelectedMountUuid(player, MountCategory.WATER),
				PlayerMountSelectionService.getSelectedMountUuid(player, MountCategory.LAVA)
			);
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record ManagementDataPayload(SelectionStatePayload selections, List<MountSummary> mounts)
			implements CustomPacketPayload {
		public static final Type<ManagementDataPayload> TYPE = new Type<>(
			Identifier.fromNamespaceAndPath(BelAndSighMod.MOD_ID, "mount_management_data"));
		public static final StreamCodec<RegistryFriendlyByteBuf, ManagementDataPayload> CODEC =
			CustomPacketPayload.codec(ManagementDataPayload::write, ManagementDataPayload::new);

		public ManagementDataPayload {
			mounts = List.copyOf(mounts);
		}

		private ManagementDataPayload(RegistryFriendlyByteBuf buffer) {
			this(new SelectionStatePayload(buffer), readMountSummaries(buffer));
		}

		private void write(RegistryFriendlyByteBuf buffer) {
			selections.write(buffer);
			buffer.writeVarInt(mounts.size());
			mounts.forEach(mount -> mount.write(buffer));
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record MountSummary(UUID mountUuid, String displayName, String typeName,
			String dimensionName, int categoryMask, boolean bound) {
		private MountSummary(RegistryFriendlyByteBuf buffer) {
			this(buffer.readUUID(), buffer.readUtf(MAX_DISPLAY_TEXT_LENGTH),
				buffer.readUtf(MAX_DISPLAY_TEXT_LENGTH), buffer.readUtf(MAX_DISPLAY_TEXT_LENGTH),
				buffer.readVarInt(), buffer.readBoolean());
		}

		private void write(RegistryFriendlyByteBuf buffer) {
			buffer.writeUUID(mountUuid);
			buffer.writeUtf(displayName, MAX_DISPLAY_TEXT_LENGTH);
			buffer.writeUtf(typeName, MAX_DISPLAY_TEXT_LENGTH);
			buffer.writeUtf(dimensionName, MAX_DISPLAY_TEXT_LENGTH);
			buffer.writeVarInt(categoryMask);
			buffer.writeBoolean(bound);
		}

		public boolean supports(MountCategory category) {
			return (categoryMask & (1 << category.ordinal())) != 0;
		}
	}

	private static List<MountSummary> readMountSummaries(RegistryFriendlyByteBuf buffer) {
		int size = buffer.readVarInt();
		if (size < 0 || size > MAX_MOUNT_SUMMARIES) {
			throw new IllegalArgumentException("Invalid mount summary count: " + size);
		}
		List<MountSummary> mounts = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			mounts.add(new MountSummary(buffer));
		}
		return List.copyOf(mounts);
	}

	private static Optional<UUID> readOptionalUuid(RegistryFriendlyByteBuf buffer) {
		return buffer.readBoolean() ? Optional.of(buffer.readUUID()) : Optional.empty();
	}

	private static void writeOptionalUuid(RegistryFriendlyByteBuf buffer, Optional<UUID> uuid) {
		buffer.writeBoolean(uuid.isPresent());
		uuid.ifPresent(buffer::writeUUID);
	}
}
