package net.beforetheblight.compat.seasons;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import sereneseasons.api.season.ISeasonState;
import sereneseasons.api.season.SeasonHelper;
import sereneseasons.init.ModConfig;
import sereneseasons.init.ModFertility;
import sereneseasons.init.ModTags;

/**
 * The only main-source class that links the optional Serene Seasons API.
 * Instantiation is guarded by FabricLoader in {@link SeasonalPlantClock}.
 */
final class SereneSeasonsSeasonProvider implements SeasonProvider {
	private SereneSeasonsSeasonProvider() {
	}

	static SeasonProvider create() {
		return new SereneSeasonsSeasonProvider();
	}

	@Override
	public Optional<SeasonalPlantClock.Snapshot> snapshot(Level level, BlockPos pos) {
		if (!ModConfig.seasons.isDimensionWhitelisted(level.dimension())) {
			return Optional.empty();
		}

		var biome = level.getBiome(pos);
		if (biome.is(ModTags.Biomes.BLACKLISTED_BIOMES)
			|| SeasonHelper.usesTropicalSeasons(biome)) {
			return Optional.empty();
		}

		ISeasonState state = SeasonHelper.getSeasonState(level);
		int subseasonDuration = state.getSubSeasonDuration();
		if (subseasonDuration <= 0) {
			return Optional.empty();
		}

		int index = state.getSubSeason().ordinal();
		int withinSubseason = Math.floorMod(state.getSeasonCycleTicks(), subseasonDuration);
		double calendarPhase = index + (double)withinSubseason / subseasonDuration;
		double jitter = SeasonalPlantClock.stablePositionJitter(pos);
		double elevationDelay = SeasonalPlantClock.elevationDelay(pos.getY());
		double effectivePhase = SeasonalPlantClock.wrapPhase(calendarPhase + jitter - elevationDelay);

		return Optional.of(new SeasonalPlantClock.Snapshot(
			SeasonalPlantClock.Subseason.fromIndex(index),
			SeasonalPlantClock.wrapPhase(calendarPhase),
			effectivePhase,
			jitter,
			elevationDelay
		));
	}

	@Override
	public boolean seasonalCropGrowthEnabled() {
		return ModConfig.fertility.seasonalCrops;
	}

	@Override
	public boolean isCropFertile(
		Level level,
		BlockPos pos,
		SeasonalPlantClock.Plant plant
	) {
		return ModFertility.isCropFertile(plant.blockIdentifier(), level, pos);
	}

	@Override
	public boolean isUndergroundFertilityExempt(Level level, BlockPos pos) {
		return pos.getY() < ModConfig.fertility.undergroundFertilityLevel
			&& !level.canSeeSky(pos);
	}

	@Override
	public boolean seasonalFoliageColorEnabled() {
		return ModConfig.seasons.changeFoliageColor;
	}
}
