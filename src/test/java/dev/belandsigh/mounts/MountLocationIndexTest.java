package dev.belandsigh.mounts;

import com.mojang.serialization.JsonOps;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MountLocationIndexTest {
	@Test
	void lastKnownChunkSurvivesCodecRoundTrip() {
		net.minecraft.SharedConstants.tryDetectVersion();
		net.minecraft.server.Bootstrap.bootStrap();
		UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000099");
		UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000100");
		MountLocationIndex original = new MountLocationIndex(List.of(
			new MountLocationIndex.Location(uuid, Level.NETHER, -12, 34,
				Optional.of(owner), "Lava Taxi", "Strider", 1 << MountCategory.LAVA.ordinal(), true)));

		var encoded = MountLocationIndex.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
		MountLocationIndex decoded = MountLocationIndex.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();

		assertEquals(new MountLocationIndex.Location(uuid, Level.NETHER, -12, 34,
			Optional.of(owner), "Lava Taxi", "Strider", 1 << MountCategory.LAVA.ordinal(), true),
			decoded.find(uuid).orElseThrow());
	}

	@Test
	void legacyLocationWithoutCatalogFieldsStillDecodes() {
		net.minecraft.SharedConstants.tryDetectVersion();
		net.minecraft.server.Bootstrap.bootStrap();
		String legacy = "{\"mounts\":[{\"uuid\":[0,0,0,99],"
			+ "\"dimension\":\"minecraft:overworld\",\"chunk_x\":1,\"chunk_z\":2}]}";

		MountLocationIndex decoded = MountLocationIndex.CODEC.parse(
			JsonOps.INSTANCE, com.google.gson.JsonParser.parseString(legacy)).getOrThrow();

		var entry = decoded.find(new UUID(0L, 99L)).orElseThrow();
		assertEquals(Optional.empty(), entry.ownerUuid());
	}
}
