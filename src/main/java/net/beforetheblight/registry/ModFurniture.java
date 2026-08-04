package net.beforetheblight.registry;

import net.beforetheblight.BeforeTheBlight;
import net.beforetheblight.entity.RockingChairSeatEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Registration surface for furniture-only technical entities.
 *
 * <p>The visible rocking-chair block and its item deliberately live with the
 * other obtainable blocks in {@link ModBlocks}. This separate registry owns
 * only the transient seat vehicle, keeping that implementation detail out of
 * the ordinary block and item lists.</p>
 */
public final class ModFurniture {
	public static final EntityType<RockingChairSeatEntity> ROCKING_CHAIR_SEAT =
		registerSeatEntity();

	private ModFurniture() {
	}

	private static EntityType<RockingChairSeatEntity> registerSeatEntity() {
		ResourceKey<EntityType<?>> key = ResourceKey.create(
			Registries.ENTITY_TYPE,
			BeforeTheBlight.id("rocking_chair_seat")
		);
		EntityType<RockingChairSeatEntity> type = EntityType.Builder
			.<RockingChairSeatEntity>of(RockingChairSeatEntity::new, MobCategory.MISC)
			.sized(0.01F, 0.01F)
			.clientTrackingRange(8)
			.updateInterval(1)
			.noSummon()
			.noLootTable()
			.build(key);
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, type);
	}

	public static void initialize() {
		// Calling this method forces the technical seat entity registration.
	}
}
