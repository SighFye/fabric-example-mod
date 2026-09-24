package dev.belandsigh.death;

import com.mojang.serialization.Codec;
import dev.belandsigh.BelAndSighMod;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server-wide record of every player's still-active death-location countdowns. Unlike per-player
 * save data, this is reachable without that player's {@code ServerPlayer} instance being loaded, so
 * it stays authoritative and up to date even while its owner is offline, and survives a crash
 * without depending on the owner having reconnected since the last change.
 */
public final class DeathLocationSavedData extends SavedData {
	private static final SavedDataType<DeathLocationSavedData> TYPE = new SavedDataType<>(
		Identifier.fromNamespaceAndPath(BelAndSighMod.MOD_ID, "death_locations"),
		DeathLocationSavedData::new,
		Codec.unboundedMap(UUIDUtil.STRING_CODEC, DeathRecord.CODEC.listOf())
			.xmap(map -> new DeathLocationSavedData(new HashMap<>(map)), data -> data.records),
		DataFixTypes.LEVEL);

	private final Map<UUID, List<DeathRecord>> records;

	private DeathLocationSavedData() {
		this(new HashMap<>());
	}

	private DeathLocationSavedData(Map<UUID, List<DeathRecord>> records) {
		this.records = records;
	}

	static DeathLocationSavedData get(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	List<DeathRecord> get(UUID playerId) {
		return records.getOrDefault(playerId, List.of());
	}

	void set(UUID playerId, List<DeathRecord> playerRecords) {
		if (playerRecords.isEmpty()) {
			records.remove(playerId);
		} else {
			records.put(playerId, playerRecords);
		}
		setDirty();
	}
}
