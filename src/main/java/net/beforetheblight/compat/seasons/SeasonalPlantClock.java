package net.beforetheblight.compat.seasons;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

import com.mojang.serialization.Codec;
import net.beforetheblight.BeforeTheBlight;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Optional, temperate plant-season policy shared by gameplay and rendering.
 *
 * <p>The twelve-point calendar is a playable phenology proxy. It deliberately
 * does not claim to persist chilling hours or any other per-plant physiology.
 * When Serene Seasons is absent, or when the plant is outside the temperate
 * Overworld, all runtime helpers return the pre-compatibility behavior.</p>
 */
public final class SeasonalPlantClock {
	public static final String SERENE_SEASONS_MOD_ID = "sereneseasons";
	public static final String GLITCHCORE_MOD_ID = "glitchcore";
	public static final String SUPPORTED_SERENE_SEASONS_VERSION = "26.1.2.0.4";
	public static final String SUPPORTED_GLITCHCORE_VERSION = "26.1.2.0.2";
	public static final int SUBSEASON_COUNT = 12;
	public static final int BASELINE_DRYING_INTERVAL = 3;
	public static final int GREENHOUSE_SEARCH_HEIGHT = 16;
	public static final int ELEVATION_BASE_Y = 64;
	public static final int ELEVATION_FULL_DELAY_Y = 192;
	public static final double MAX_POSITION_JITTER = 0.25D;
	public static final double MAX_ELEVATION_DELAY = 1.0D;

	public static final TagKey<Block> GREENHOUSE_GLASS = TagKey.create(
		Registries.BLOCK,
		Identifier.fromNamespaceAndPath(SERENE_SEASONS_MOD_ID, "greenhouse_glass")
	);

	private static final double[][] GROWTH_MULTIPLIERS = {
		// Early spring through late winter. These relative chance gates are
		// phenology-informed gameplay weights, not measured physiological rates.
		{0.00D, 0.20D, 0.65D, 1.00D, 1.00D, 0.90D, 0.55D, 0.15D, 0.00D, 0.00D, 0.00D, 0.00D},
		{0.00D, 0.25D, 0.75D, 1.00D, 1.00D, 0.70D, 0.00D, 0.00D, 0.00D, 0.00D, 0.00D, 0.00D},
		{0.00D, 0.20D, 0.75D, 1.00D, 0.90D, 0.50D, 0.00D, 0.00D, 0.00D, 0.00D, 0.00D, 0.00D},
		{0.00D, 0.30D, 0.75D, 1.00D, 0.95D, 0.55D, 0.00D, 0.00D, 0.00D, 0.00D, 0.00D, 0.00D},
		{0.00D, 0.20D, 0.70D, 1.00D, 0.95D, 0.60D, 0.00D, 0.00D, 0.00D, 0.00D, 0.00D, 0.00D}
	};

	private static final SeasonProvider BASELINE_PROVIDER = (level, pos) -> Optional.empty();
	private static final SeasonProviderHealth PROVIDER_HEALTH = new SeasonProviderHealth(
		BASELINE_PROVIDER,
		() -> BeforeTheBlight.LOGGER.info(
			"Enabled the exact-qualified Serene Seasons plant phenology bridge."
		),
		exception -> BeforeTheBlight.LOGGER.error(
			"The exact-qualified Serene Seasons bridge failed and has been permanently disabled for this process; retaining baseline plant behavior.",
			exception
		)
	);
	private static volatile boolean initialized;

	private SeasonalPlantClock() {
	}

	/**
	 * Installs the API-backed provider only when Fabric reports Serene Seasons.
	 * The implementation class is kept separate so its optional API types are
	 * never resolved on a base installation.
	 */
	public static synchronized void initialize() {
		if (initialized) {
			return;
		}
		initialized = true;

		FabricLoader loader = FabricLoader.getInstance();
		if (!loader.isModLoaded(SERENE_SEASONS_MOD_ID)) {
			BeforeTheBlight.LOGGER.info("Serene Seasons not present; retaining baseline plant behavior.");
			return;
		}
		Function<String, Optional<String>> versionLookup = modId -> loadedVersion(
			loader,
			modId
		);
		if (!supportsExactVersions(versionLookup)) {
			BeforeTheBlight.LOGGER.warn(
				"Serene Seasons was detected, but Before the Blight supports only sereneseasons {} with glitchcore {}; found sereneseasons {} and glitchcore {}. The optional bridge will remain disabled.",
				SUPPORTED_SERENE_SEASONS_VERSION,
				SUPPORTED_GLITCHCORE_VERSION,
				versionLookup.apply(SERENE_SEASONS_MOD_ID).orElse("missing"),
				versionLookup.apply(GLITCHCORE_MOD_ID).orElse("missing")
			);
			return;
		}

		try {
			PROVIDER_HEALTH.detect(SereneSeasonsSeasonProvider.create());
			BeforeTheBlight.LOGGER.info(
				"Detected the exact-qualified Serene Seasons and GlitchCore pair; awaiting the first successful season snapshot before enabling plant phenology."
			);
		} catch (LinkageError | RuntimeException exception) {
			PROVIDER_HEALTH.failDetection(exception);
		}
	}

	public static boolean isEnabled() {
		return PROVIDER_HEALTH.isOperational();
	}

	static boolean supportsExactVersions(
		Function<String, Optional<String>> versionLookup
	) {
		return versionLookup.apply(SERENE_SEASONS_MOD_ID)
			.filter(SUPPORTED_SERENE_SEASONS_VERSION::equals)
			.isPresent()
			&& versionLookup.apply(GLITCHCORE_MOD_ID)
				.filter(SUPPORTED_GLITCHCORE_VERSION::equals)
				.isPresent();
	}

	private static Optional<String> loadedVersion(FabricLoader loader, String modId) {
		return loader.getModContainer(modId)
			.map(container -> container.getMetadata().getVersion().getFriendlyString());
	}

	/**
	 * Returns a seasonal snapshot only for temperate Overworld positions.
	 */
	public static Optional<Snapshot> snapshot(Level level, BlockPos pos) {
		if (level == null || pos == null || !Level.OVERWORLD.equals(level.dimension())) {
			return Optional.empty();
		}

		return PROVIDER_HEALTH.snapshot(level, pos);
	}

	/**
	 * Applies the species curve as a probability gate. Tagged greenhouse glass
	 * directly above the plant bypasses the finer Before the Blight gate; Serene
	 * Seasons can still apply its own public configuration and crop-tag policy.
	 */
	public static boolean allowsGrowth(
		Level level,
		BlockPos pos,
		Plant plant,
		RandomSource random
	) {
		Optional<Snapshot> snapshot = snapshot(level, pos);
		if (snapshot.isEmpty()) {
			return true;
		}

		if (!PROVIDER_HEALTH.seasonalCropGrowthEnabled()) {
			return true;
		}
		if (hasGreenhouseGlass(level, pos)) {
			return true;
		}
		if (PROVIDER_HEALTH.isUndergroundFertilityExempt(level, pos)) {
			return true;
		}
		if (!PROVIDER_HEALTH.isCropFertile(level, pos, plant)) {
			return false;
		}

		double multiplier = growthMultiplier(plant, snapshot.get().effectivePhase());
		if (multiplier <= 0.0D) {
			return false;
		}
		return multiplier >= 1.0D || random.nextDouble() < multiplier;
	}

	/**
	 * Bonemeal is deterministic at the compatibility boundary: it remains a
	 * valid target in any non-dormant phase and is rejected without consuming
	 * the item during a zero-growth phase. Natural growth retains the full
	 * probability curve in {@link #allowsGrowth(Level, BlockPos, Plant, RandomSource)}.
	 */
	public static boolean allowsBonemeal(Level level, BlockPos pos, Plant plant) {
		Optional<Snapshot> snapshot = snapshot(level, pos);
		if (snapshot.isEmpty()) {
			return true;
		}

		if (!PROVIDER_HEALTH.seasonalCropGrowthEnabled()) {
			return true;
		}
		if (hasGreenhouseGlass(level, pos)) {
			return true;
		}
		if (PROVIDER_HEALTH.isUndergroundFertilityExempt(level, pos)) {
			return true;
		}
		if (!PROVIDER_HEALTH.isCropFertile(level, pos, plant)) {
			return false;
		}

		return growthMultiplier(plant, snapshot.get().effectivePhase()) > 0.0D;
	}

	/**
	 * Legacy drying uses one success roll in three. Warm summer and harvest
	 * weather use one in two; winter uses one in five.
	 */
	public static int dryingInterval(Level level, BlockPos pos) {
		return snapshot(level, pos)
			.map(value -> dryingInterval(value.effectivePhase()))
			.orElse(BASELINE_DRYING_INTERVAL);
	}

	/**
	 * Keeps legacy all-season chestnut loot when no temperate clock is active.
	 */
	public static boolean chestnutMastAvailable(Level level, BlockPos pos) {
		return snapshot(level, pos)
			.map(value -> isChestnutMastAvailable(value.calendarPhase()))
			.orElse(true);
	}

	public static boolean hasGreenhouseGlass(Level level, BlockPos pos) {
		if (level == null || pos == null) {
			return false;
		}
		for (int dy = 1; dy <= GREENHOUSE_SEARCH_HEIGHT; dy++) {
			if (level.getBlockState(pos.above(dy)).is(GREENHOUSE_GLASS)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Uses the exact configured Serene Seasons underground threshold through
	 * the optional adapter. Base installations remain false and unchanged.
	 */
	public static boolean isShelteredUnderground(Level level, BlockPos pos) {
		if (snapshot(level, pos).isEmpty()) {
			return false;
		}
		return PROVIDER_HEALTH.isUndergroundFertilityExempt(level, pos);
	}

	/**
	 * Compatibility alias retained for focused tests written while this API was
	 * being integrated.
	 */
	public static boolean isConservativelyUnderground(Level level, BlockPos pos) {
		return isShelteredUnderground(level, pos);
	}

	public static boolean seasonalFoliageColorEnabled(Level level, BlockPos pos) {
		if (snapshot(level, pos).isEmpty()) {
			return false;
		}
		return PROVIDER_HEALTH.seasonalFoliageColorEnabled();
	}

	/**
	 * Linearly interpolates the twelve relative growth weights.
	 */
	public static double growthMultiplier(Plant plant, double effectivePhase) {
		double phase = wrapPhase(effectivePhase);
		int current = (int)Math.floor(phase);
		int next = (current + 1) % SUBSEASON_COUNT;
		double progress = phase - current;
		double[] profile = GROWTH_MULTIPLIERS[plant.ordinal()];
		return profile[current] + (profile[next] - profile[current]) * progress;
	}

	public static boolean isChestnutMastAvailable(double effectivePhase) {
		int subseason = (int)Math.floor(wrapPhase(effectivePhase));
		return subseason == Subseason.EARLY_AUTUMN.ordinal()
			|| subseason == Subseason.MID_AUTUMN.ordinal();
	}

	public static int dryingInterval(double effectivePhase) {
		int subseason = (int)Math.floor(wrapPhase(effectivePhase));
		if (subseason >= Subseason.EARLY_SUMMER.ordinal()
			&& subseason <= Subseason.MID_AUTUMN.ordinal()) {
			return 2;
		}
		if (subseason >= Subseason.EARLY_WINTER.ordinal()) {
			return 5;
		}
		return BASELINE_DRYING_INTERVAL;
	}

	/**
	 * Stable micro-site timing in the half-subseason-wide interval
	 * [-0.25, 0.25). The SplitMix64 finalizer avoids visible coordinate bands.
	 */
	public static double stablePositionJitter(BlockPos pos) {
		long mixed = pos.asLong() + 0x9E3779B97F4A7C15L;
		mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
		mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
		mixed ^= mixed >>> 31;
		double unit = (mixed >>> 11) * 0x1.0p-53;
		return (unit - 0.5D) * (MAX_POSITION_JITTER * 2.0D);
	}

	/**
	 * A bounded Appalachian elevation proxy: no delay at/below y=64 and at
	 * most one full subseason at/above y=192.
	 */
	public static double elevationDelay(int y) {
		if (y <= ELEVATION_BASE_Y) {
			return 0.0D;
		}
		if (y >= ELEVATION_FULL_DELAY_Y) {
			return MAX_ELEVATION_DELAY;
		}
		return (double)(y - ELEVATION_BASE_Y) / (ELEVATION_FULL_DELAY_Y - ELEVATION_BASE_Y);
	}

	public static double wrapPhase(double phase) {
		double wrapped = phase % SUBSEASON_COUNT;
		return wrapped < 0.0D ? wrapped + SUBSEASON_COUNT : wrapped;
	}

	public enum Plant implements StringRepresentable {
		CORN("before_the_blight:corn"),
		CHESTNUT("before_the_blight:chestnut_sapling"),
		HEMLOCK("before_the_blight:hemlock_sapling"),
		AMERICAN_BEECH("before_the_blight:american_beech_sapling"),
		BLACK_WALNUT("before_the_blight:black_walnut_sapling");

		public static final Codec<Plant> CODEC = StringRepresentable.fromEnum(Plant::values);
		private final String blockIdentifier;

		Plant(String blockIdentifier) {
			this.blockIdentifier = blockIdentifier;
		}

		String blockIdentifier() {
			return this.blockIdentifier;
		}

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	public enum Subseason {
		EARLY_SPRING,
		MID_SPRING,
		LATE_SPRING,
		EARLY_SUMMER,
		MID_SUMMER,
		LATE_SUMMER,
		EARLY_AUTUMN,
		MID_AUTUMN,
		LATE_AUTUMN,
		EARLY_WINTER,
		MID_WINTER,
		LATE_WINTER;

		public static Subseason fromIndex(int index) {
			return values()[Math.floorMod(index, SUBSEASON_COUNT)];
		}
	}

	public record Snapshot(
		Subseason calendarSubseason,
		double calendarPhase,
		double effectivePhase,
		double positionJitter,
		double elevationDelay
	) {
	}
}
