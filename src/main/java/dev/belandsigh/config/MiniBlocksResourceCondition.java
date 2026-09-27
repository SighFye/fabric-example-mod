package dev.belandsigh.config;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import org.jspecify.annotations.Nullable;

public record MiniBlocksResourceCondition() implements ResourceCondition {
	public static final ResourceConditionType<MiniBlocksResourceCondition> TYPE = ResourceConditionType.create(
		Identifier.fromNamespaceAndPath("belandsigh", "mini_blocks_enabled"),
		MapCodec.unit(MiniBlocksResourceCondition::new));

	public static void initialize() {
		ResourceConditions.register(TYPE);
	}

	@Override
	public ResourceConditionType<?> getType() {
		return TYPE;
	}

	@Override
	public boolean test(RegistryOps.@Nullable RegistryInfoLookup registryInfo) {
		return ModSettings.enabled(Feature.MINI_BLOCKS);
	}
}
