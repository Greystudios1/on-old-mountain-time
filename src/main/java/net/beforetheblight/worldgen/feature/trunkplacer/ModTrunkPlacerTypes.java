package net.beforetheblight.worldgen.feature.trunkplacer;

import net.beforetheblight.BeforeTheBlight;
import net.beforetheblight.worldgen.feature.foliageplacer.ModFoliagePlacerTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;

public final class ModTrunkPlacerTypes {
	public static final TrunkPlacerType<ForestChestnutTrunkPlacer> FOREST_CHESTNUT_TRUNK_PLACER =
		Registry.register(
			BuiltInRegistries.TRUNK_PLACER_TYPE,
			BeforeTheBlight.id("forest_chestnut_trunk_placer"),
			new TrunkPlacerType<>(ForestChestnutTrunkPlacer.CODEC)
		);

	public static final TrunkPlacerType<HollowChestnutTrunkPlacer> HOLLOW_CHESTNUT_TRUNK_PLACER =
		Registry.register(
			BuiltInRegistries.TRUNK_PLACER_TYPE,
			BeforeTheBlight.id("hollow_chestnut_trunk_placer"),
			new TrunkPlacerType<>(HollowChestnutTrunkPlacer.CODEC)
		);

	public static final TrunkPlacerType<OldGrowthHemlockTrunkPlacer> OLD_GROWTH_HEMLOCK_TRUNK_PLACER =
		Registry.register(
			BuiltInRegistries.TRUNK_PLACER_TYPE,
			BeforeTheBlight.id("old_growth_hemlock_trunk_placer"),
			new TrunkPlacerType<>(OldGrowthHemlockTrunkPlacer.CODEC)
		);

	public static final TrunkPlacerType<TieredHemlockTrunkPlacer> TIERED_HEMLOCK_TRUNK_PLACER =
		Registry.register(
			BuiltInRegistries.TRUNK_PLACER_TYPE,
			BeforeTheBlight.id("tiered_hemlock_trunk_placer"),
			new TrunkPlacerType<>(TieredHemlockTrunkPlacer.CODEC)
		);

	private ModTrunkPlacerTypes() {
	}

	public static void initialize() {
		// Loading this class registers the codec before configured features decode.
		ModFoliagePlacerTypes.initialize();
	}
}
