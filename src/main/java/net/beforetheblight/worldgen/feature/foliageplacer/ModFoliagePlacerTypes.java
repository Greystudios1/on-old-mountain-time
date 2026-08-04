package net.beforetheblight.worldgen.feature.foliageplacer;

import net.beforetheblight.BeforeTheBlight;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;

public final class ModFoliagePlacerTypes {
	public static final FoliagePlacerType<TieredHemlockFoliagePlacer>
		TIERED_HEMLOCK_FOLIAGE_PLACER = Registry.register(
			BuiltInRegistries.FOLIAGE_PLACER_TYPE,
			BeforeTheBlight.id("tiered_hemlock_foliage_placer"),
			new FoliagePlacerType<>(TieredHemlockFoliagePlacer.CODEC)
		);

	private ModFoliagePlacerTypes() {
	}

	public static void initialize() {
		// Loading this class registers the codec before configured features decode.
	}
}
