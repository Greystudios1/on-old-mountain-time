package net.beforetheblight.compat.seasons;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

@FunctionalInterface
interface SeasonProvider {
	Optional<SeasonalPlantClock.Snapshot> snapshot(Level level, BlockPos pos);

	default boolean seasonalCropGrowthEnabled() {
		return false;
	}

	default boolean isCropFertile(
		Level level,
		BlockPos pos,
		SeasonalPlantClock.Plant plant
	) {
		return true;
	}

	default boolean isUndergroundFertilityExempt(Level level, BlockPos pos) {
		return false;
	}

	default boolean seasonalFoliageColorEnabled() {
		return false;
	}
}
