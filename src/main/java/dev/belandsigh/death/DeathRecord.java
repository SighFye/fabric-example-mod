package dev.belandsigh.death;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

/**
 * A single still-tracked death: where it happened and which of its dropped item entities are still
 * being watched. Presence/absence of those entities in the world - never a custom timer - is what
 * drives the countdown, so no despawn duration needs to be stored here.
 */
public record DeathRecord(ResourceKey<Level> dimension, BlockPos pos, int deathNumber, List<UUID> itemIds) {
	public static final Codec<DeathRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(DeathRecord::dimension),
		BlockPos.CODEC.fieldOf("pos").forGetter(DeathRecord::pos),
		Codec.INT.optionalFieldOf("death_number", 1).forGetter(DeathRecord::deathNumber),
		UUIDUtil.CODEC.listOf().fieldOf("item_ids").forGetter(DeathRecord::itemIds)
	).apply(instance, DeathRecord::new));
}
