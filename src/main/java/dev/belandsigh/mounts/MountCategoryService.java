package dev.belandsigh.mounts;

import dev.belandsigh.BelAndSighMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** The single source of truth for Mount Whistle eligibility and categories. */
public final class MountCategoryService {
	private static final Map<MountCategory, TagKey<EntityType<?>>> CATEGORY_TAGS = createTags();
	private static final MountCategory[] CATEGORIES = MountCategory.values();

	private MountCategoryService() {
	}

	public static Set<MountCategory> getMountCategories(Entity entity) {
		return categoriesMatching(category -> isInCategory(entity, category));
	}

	/** Allocation-free: this runs for every entity load and unload on the server. */
	public static boolean isEligibleMount(Entity entity) {
		return categoryMask(entity) != 0;
	}

	public static boolean supports(Entity entity, MountCategory category) {
		return isInCategory(entity, category);
	}

	public static int categoryMask(Entity entity) {
		int mask = 0;
		for (MountCategory category : CATEGORIES) {
			if (isInCategory(entity, category)) {
				mask |= 1 << category.ordinal();
			}
		}
		return mask;
	}

	public static boolean supportsMask(int mask, MountCategory category) {
		return (mask & (1 << category.ordinal())) != 0;
	}

	private static boolean isInCategory(Entity entity, MountCategory category) {
		return BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(entity.getType()).is(CATEGORY_TAGS.get(category));
	}

	static Set<MountCategory> categoriesMatching(Predicate<MountCategory> membership) {
		EnumSet<MountCategory> categories = EnumSet.noneOf(MountCategory.class);
		for (MountCategory category : MountCategory.values()) {
			if (membership.test(category)) {
				categories.add(category);
			}
		}
		return Collections.unmodifiableSet(categories);
	}

	private static Map<MountCategory, TagKey<EntityType<?>>> createTags() {
		EnumMap<MountCategory, TagKey<EntityType<?>>> tags = new EnumMap<>(MountCategory.class);
		for (MountCategory category : MountCategory.values()) {
			tags.put(category, TagKey.create(
				Registries.ENTITY_TYPE,
				Identifier.fromNamespaceAndPath(BelAndSighMod.MOD_ID, category.tagPath())
			));
		}
		return Collections.unmodifiableMap(tags);
	}
}
