package net.beforetheblight.gametest;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.beforetheblight.compat.seasons.SeasonalPlantClock;
import net.beforetheblight.compat.seasons.SeasonalPlantClock.Plant;
import net.beforetheblight.compat.seasons.SeasonalPlantClock.Snapshot;
import net.beforetheblight.compat.seasons.SeasonalPlantClock.Subseason;
import net.beforetheblight.registry.ModBlocks;
import net.beforetheblight.registry.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Deterministic compatibility contracts shared by the required-mod-free and
 * pinned Serene Seasons GameTest profiles.
 *
 * <p>This class deliberately has no link to Serene Seasons classes. Merely
 * discovering and executing it in the base profile therefore proves that the
 * optional integration does not become a hidden class-loading dependency.</p>
 */
public final class SeasonCompatibilityGameTests {
	private static final double EPSILON = 1.0E-9;
	private static final BlockPos CLOCK_POS = new BlockPos(2, 2, 2);
	private static final String REQUIRED_PROFILE_PROPERTY =
		"before_the_blight.gametest.serene_seasons";
	private static final String SERENE_SEASONS_MOD_ID = "sereneseasons";
	private static final String GLITCHCORE_MOD_ID = "glitchcore";
	private static final String PINNED_SERENE_SEASONS_VERSION = "26.1.2.0.4";
	private static final String PINNED_GLITCHCORE_VERSION = "26.1.2.0.2";

	private static final TagKey<Block> SPRING_CROPS_BLOCK =
		blockTag("spring_crops");
	private static final TagKey<Block> SUMMER_CROPS_BLOCK =
		blockTag("summer_crops");
	private static final TagKey<Block> AUTUMN_CROPS_BLOCK =
		blockTag("autumn_crops");
	private static final TagKey<Block> WINTER_CROPS_BLOCK =
		blockTag("winter_crops");
	private static final TagKey<Block> YEAR_ROUND_CROPS_BLOCK =
		blockTag("year_round_crops");
	private static final TagKey<Block> UNBREAKABLE_INFERTILE_CROPS_BLOCK =
		blockTag("unbreakable_infertile_crops");

	private static final TagKey<Item> SPRING_CROPS_ITEM =
		itemTag("spring_crops");
	private static final TagKey<Item> SUMMER_CROPS_ITEM =
		itemTag("summer_crops");
	private static final TagKey<Item> AUTUMN_CROPS_ITEM =
		itemTag("autumn_crops");
	private static final TagKey<Item> WINTER_CROPS_ITEM =
		itemTag("winter_crops");
	private static final TagKey<Item> YEAR_ROUND_CROPS_ITEM =
		itemTag("year_round_crops");
	@GameTest(maxTicks = 20)
	public void optionalProfileModeMatchesLoadedMods(GameTestHelper helper) {
		boolean requiredProfile = Boolean.getBoolean(REQUIRED_PROFILE_PROPERTY);
		FabricLoader loader = FabricLoader.getInstance();
		boolean sereneLoaded = loader.isModLoaded(SERENE_SEASONS_MOD_ID);
		boolean glitchCoreLoaded = loader.isModLoaded(GLITCHCORE_MOD_ID);

		helper.assertValueEqual(
			sereneLoaded,
			requiredProfile,
			"Serene Seasons loaded state for the selected GameTest profile"
		);
		helper.assertValueEqual(
			glitchCoreLoaded,
			requiredProfile,
			"GlitchCore loaded state for the selected GameTest profile"
		);
		if (requiredProfile) {
			assertVersion(
				helper,
				SERENE_SEASONS_MOD_ID,
				PINNED_SERENE_SEASONS_VERSION
			);
			assertVersion(helper, GLITCHCORE_MOD_ID, PINNED_GLITCHCORE_VERSION);
			SeasonalPlantClock.snapshot(
				helper.getLevel(),
				helper.absolutePos(CLOCK_POS)
			);
		}
		helper.assertValueEqual(
			SeasonalPlantClock.isEnabled(),
			requiredProfile,
			"BTB seasonal API bridge health after the profile snapshot probe"
		);

		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void cropAndSaplingTagsMatchTheSeasonalGrowthWindows(GameTestHelper helper) {
		assertBlockWindows(
			helper,
			"corn",
			ModBlocks.CORN,
			true,
			true,
			true,
			false,
			false
		);

		assertBlockWindows(
			helper,
			"black walnut sapling",
			ModBlocks.BLACK_WALNUT_SAPLING,
			true,
			true,
			false,
			false,
			false
		);
		assertItemWindows(
			helper,
			"black walnut sapling",
			ModBlocks.BLACK_WALNUT_SAPLING.asItem(),
			true,
			true,
			false,
			false,
			false
		);
		assertItemWindows(
			helper,
			"corn kernels",
			ModItems.CORN_KERNELS,
			true,
			true,
			true,
			false,
			false
		);

		assertBlockWindows(
			helper,
			"chestnut sapling",
			ModBlocks.CHESTNUT_SAPLING,
			true,
			true,
			false,
			false,
			false
		);
		assertItemWindows(
			helper,
			"chestnut sapling",
			ModBlocks.CHESTNUT_SAPLING.asItem(),
			true,
			true,
			false,
			false,
			false
		);

		assertBlockWindows(
			helper,
			"hemlock sapling",
			ModBlocks.HEMLOCK_SAPLING,
			true,
			true,
			false,
			false,
			false
		);
		assertItemWindows(
			helper,
			"hemlock sapling",
			ModBlocks.HEMLOCK_SAPLING.asItem(),
			true,
			true,
			false,
			false,
			false
		);

		assertBlockWindows(
			helper,
			"American beech sapling",
			ModBlocks.AMERICAN_BEECH_SAPLING,
			true,
			true,
			false,
			false,
			false
		);
		assertItemWindows(
			helper,
			"American beech sapling",
			ModBlocks.AMERICAN_BEECH_SAPLING.asItem(),
			true,
			true,
			false,
			false,
			false
		);

		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void dormantSaplingsAreProtectedAndDryingCornIsNotACrop(GameTestHelper helper) {
		for (Block sapling : List.of(
			ModBlocks.CHESTNUT_SAPLING,
			ModBlocks.HEMLOCK_SAPLING,
			ModBlocks.AMERICAN_BEECH_SAPLING,
			ModBlocks.BLACK_WALNUT_SAPLING
		)) {
			helper.assertTrue(
				sapling.defaultBlockState().is(UNBREAKABLE_INFERTILE_CROPS_BLOCK),
				"Seasonally infertile sapling was not protected from destructive crop behavior: "
					+ sapling
			);
		}

		helper.assertTrue(
			!ModBlocks.CORN.defaultBlockState().is(UNBREAKABLE_INFERTILE_CROPS_BLOCK),
			"Corn was made unbreakable while infertile"
		);
		for (TagKey<Block> cropTag : List.of(
			SPRING_CROPS_BLOCK,
			SUMMER_CROPS_BLOCK,
			AUTUMN_CROPS_BLOCK,
			WINTER_CROPS_BLOCK,
			YEAR_ROUND_CROPS_BLOCK
		)) {
			helper.assertTrue(
				!ModBlocks.DRYING_CORN_BUNDLE.defaultBlockState().is(cropTag),
				"Drying corn bundle was incorrectly classified as a crop in " + cropTag
			);
		}

		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void pureGrowthClocksMatchTheTwelveSubseasonBiologyTables(GameTestHelper helper) {
		double[][] expected = {
			{0.00, 0.20, 0.65, 1.00, 1.00, 0.90, 0.55, 0.15, 0.00, 0.00, 0.00, 0.00},
			{0.00, 0.25, 0.75, 1.00, 1.00, 0.70, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00},
			{0.00, 0.20, 0.75, 1.00, 0.90, 0.50, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00},
			{0.00, 0.30, 0.75, 1.00, 0.95, 0.55, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00},
			{0.00, 0.20, 0.70, 1.00, 0.95, 0.60, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00}
		};

		for (Plant plant : Plant.values()) {
			for (int phase = 0; phase < Subseason.values().length; phase++) {
				assertClose(
					helper,
					SeasonalPlantClock.growthMultiplier(plant, phase),
					expected[plant.ordinal()][phase],
					plant + " growth multiplier at " + Subseason.values()[phase]
				);
			}
		}

		assertClose(
			helper,
			SeasonalPlantClock.growthMultiplier(Plant.CORN, 2.5),
			0.825,
			"corn linear transition from late spring to early summer"
		);
		assertClose(
			helper,
			SeasonalPlantClock.growthMultiplier(Plant.CHESTNUT, 1.5),
			0.50,
			"chestnut mid-to-late spring transition is linear"
		);
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void mastDryingAndMicroclimateFunctionsAreBoundedAndStable(GameTestHelper helper) {
		assertClose(helper, SeasonalPlantClock.wrapPhase(-0.25), 11.75, "negative phase wrap");
		assertClose(helper, SeasonalPlantClock.wrapPhase(12.25), 0.25, "positive phase wrap");
		assertClose(helper, SeasonalPlantClock.wrapPhase(24.0), 0.0, "full-cycle wrap");

		for (int phase = 0; phase < Subseason.values().length; phase++) {
			boolean expectedMast = phase == Subseason.EARLY_AUTUMN.ordinal()
				|| phase == Subseason.MID_AUTUMN.ordinal();
			helper.assertValueEqual(
				SeasonalPlantClock.isChestnutMastAvailable(phase + 0.25),
				expectedMast,
				"chestnut mast window at " + Subseason.values()[phase]
			);

			int expectedDryingInterval = phase >= Subseason.EARLY_SUMMER.ordinal()
				&& phase <= Subseason.MID_AUTUMN.ordinal()
					? 2
					: phase >= Subseason.EARLY_WINTER.ordinal() ? 5 : 3;
			helper.assertValueEqual(
				SeasonalPlantClock.dryingInterval(phase + 0.25),
				expectedDryingInterval,
				"corn drying interval at " + Subseason.values()[phase]
			);
		}
		helper.assertTrue(
			SeasonalPlantClock.isChestnutMastAvailable(18.25),
			"chestnut mast window did not wrap into early autumn"
		);
		helper.assertTrue(
			!SeasonalPlantClock.isChestnutMastAvailable(20.0),
			"chestnut mast window leaked into late autumn"
		);

		Set<Double> observedJitter = new java.util.HashSet<>();
		for (int x = -8; x <= 8; x++) {
			for (int z = -8; z <= 8; z++) {
				BlockPos pos = new BlockPos(x * 17, 64, z * 31);
				double first = SeasonalPlantClock.stablePositionJitter(pos);
				double second = SeasonalPlantClock.stablePositionJitter(pos);
				assertClose(helper, second, first, "stable position jitter at " + pos);
				helper.assertTrue(
					first >= -0.25 - EPSILON && first <= 0.25 + EPSILON,
					"position jitter escaped [-0.25, 0.25] at " + pos + ": " + first
				);
				observedJitter.add(first);
			}
		}
		helper.assertTrue(
			observedJitter.size() >= 32,
			"position jitter did not vary across the deterministic sample"
		);

		assertClose(helper, SeasonalPlantClock.elevationDelay(-64), 0.0, "low elevation delay");
		assertClose(helper, SeasonalPlantClock.elevationDelay(64), 0.0, "sea-level delay");
		assertClose(helper, SeasonalPlantClock.elevationDelay(128), 0.5, "mid-elevation delay");
		assertClose(helper, SeasonalPlantClock.elevationDelay(192), 1.0, "high-elevation delay");
		assertClose(helper, SeasonalPlantClock.elevationDelay(320), 1.0, "clamped elevation delay");
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void runtimeBridgeIsAbsentSafeOrMapsAllOfficialSubseasons(GameTestHelper helper) {
		boolean requiredProfile = Boolean.getBoolean(REQUIRED_PROFILE_PROPERTY);
		BlockPos absolutePos = helper.absolutePos(CLOCK_POS);

		if (!requiredProfile) {
			helper.assertTrue(
				SeasonalPlantClock.snapshot(helper.getLevel(), absolutePos).isEmpty(),
				"Base profile unexpectedly exposed a Serene Seasons clock"
			);
			for (Plant plant : Plant.values()) {
				helper.assertTrue(
					SeasonalPlantClock.allowsGrowth(
						helper.getLevel(),
						absolutePos,
						plant,
						RandomSource.create(0x5EA50A5L + plant.ordinal())
					),
					"Base profile changed " + plant + " growth"
				);
			}
			helper.assertValueEqual(
				SeasonalPlantClock.dryingInterval(helper.getLevel(), absolutePos),
				3,
				"base drying interval"
			);
			helper.assertTrue(
				SeasonalPlantClock.chestnutMastAvailable(helper.getLevel(), absolutePos),
				"Base profile removed legacy year-round chestnut drops"
			);
			helper.succeed();
			return;
		}

		for (Subseason expected : Subseason.values()) {
			setOfficialSeason(helper, expected);
			Snapshot snapshot = SeasonalPlantClock.snapshot(helper.getLevel(), absolutePos)
				.orElseThrow(() -> new IllegalStateException(
					"Required Serene Seasons profile returned no clock snapshot"
				));
			helper.assertValueEqual(
				snapshot.calendarSubseason(),
				expected,
				"official-to-BTB subseason mapping"
			);
			helper.assertTrue(
				snapshot.calendarPhase() >= expected.ordinal()
					&& snapshot.calendarPhase() < expected.ordinal() + 1.0,
				"calendar phase escaped " + expected + ": " + snapshot.calendarPhase()
			);
			helper.assertTrue(
				snapshot.positionJitter() >= -0.25 - EPSILON
					&& snapshot.positionJitter() <= 0.25 + EPSILON,
				"runtime position jitter escaped its contract"
			);
			helper.assertTrue(
				snapshot.elevationDelay() >= -EPSILON
					&& snapshot.elevationDelay() <= 1.0 + EPSILON,
				"runtime elevation delay escaped its contract"
			);
			assertClose(
				helper,
				snapshot.effectivePhase(),
				SeasonalPlantClock.wrapPhase(
					snapshot.calendarPhase()
						+ snapshot.positionJitter()
						- snapshot.elevationDelay()
				),
				"runtime effective phenology phase"
			);
		}

		setOfficialSeason(helper, Subseason.MID_SUMMER);
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void winterDormancyIsBypassedOnlyByOverheadGreenhouseGlass(GameTestHelper helper) {
		if (!Boolean.getBoolean(REQUIRED_PROFILE_PROPERTY)) {
			helper.succeed();
			return;
		}

		BlockPos cropPos = helper.absolutePos(CLOCK_POS);
		setOfficialSeason(helper, Subseason.EARLY_WINTER);
		helper.getLevel().setBlock(
			cropPos.below(),
			Blocks.FARMLAND.defaultBlockState(),
			Block.UPDATE_ALL
		);
		helper.getLevel().setBlock(
			cropPos,
			ModBlocks.CORN.defaultBlockState(),
			Block.UPDATE_ALL
		);

		helper.getLevel().setBlock(cropPos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_NONE);
		helper.getLevel().setBlock(cropPos.east(), Blocks.GLASS.defaultBlockState(), Block.UPDATE_NONE);
		helper.assertTrue(
			!SeasonalPlantClock.hasGreenhouseGlass(helper.getLevel(), cropPos),
			"Side glass incorrectly counted as a greenhouse roof"
		);
		for (Plant plant : Plant.values()) {
			helper.assertTrue(
				!SeasonalPlantClock.allowsGrowth(
					helper.getLevel(),
					cropPos,
					plant,
					RandomSource.create(0xD04A47L + plant.ordinal())
				),
				plant + " ignored early-winter dormancy"
			);
		}
		helper.assertTrue(
			!ModBlocks.CORN.isValidBonemealTarget(
				helper.getLevel(),
				cropPos,
				helper.getLevel().getBlockState(cropPos)
			),
			"Actual corn bonemeal target remained valid during winter dormancy"
		);
		ModBlocks.CORN.performBonemeal(
			helper.getLevel(),
			RandomSource.create(0xB04E5EEDL),
			cropPos,
			helper.getLevel().getBlockState(cropPos)
		);
		helper.assertValueEqual(
			ModBlocks.CORN.getAge(helper.getLevel().getBlockState(cropPos)),
			0,
			"Direct corn bonemeal bypassed winter dormancy"
		);

		helper.assertTrue(
			!SeasonalPlantClock.isShelteredUnderground(helper.getLevel(), cropPos),
			"Above-sea-level crop was incorrectly classified as underground"
		);
		BlockPos undergroundPos = new BlockPos(
			cropPos.getX(),
			helper.getLevel().getMinY(),
			cropPos.getZ()
		);
		helper.assertTrue(
			undergroundPos.getY() < helper.getLevel().getSeaLevel(),
			"GameTest underground probe was not below sea level"
		);
		helper.getLevel().setBlock(
			undergroundPos,
			Blocks.DIRT.defaultBlockState(),
			Block.UPDATE_ALL
		);
		helper.assertTrue(
			helper.getLevel().setBlock(
				undergroundPos.above(),
				Blocks.STONE.defaultBlockState(),
				Block.UPDATE_ALL
			),
			"Underground-test roof could not be placed"
		);
		helper.assertTrue(
			helper.getLevel().getBlockState(undergroundPos.above()).is(Blocks.STONE),
			"Underground-test roof was not present after placement"
		);
		helper.assertTrue(
			SeasonalPlantClock.isShelteredUnderground(helper.getLevel(), undergroundPos),
			"Below-sea-level crop with no sky access was not classified as underground"
				+ " (y=" + undergroundPos.getY()
				+ ", seaLevel=" + helper.getLevel().getSeaLevel()
				+ ", canSeeSky=" + helper.getLevel().canSeeSky(undergroundPos) + ")"
		);
		for (Plant plant : Plant.values()) {
			helper.assertTrue(
				SeasonalPlantClock.allowsGrowth(
					helper.getLevel(),
					undergroundPos,
					plant,
					RandomSource.create(0x67A0D5L + plant.ordinal())
				),
				"Underground exemption did not bypass " + plant + " winter dormancy"
			);
		}
		helper.getLevel().setBlock(
			undergroundPos.above(),
			Blocks.AIR.defaultBlockState(),
			Block.UPDATE_ALL
		);

		helper.getLevel().setBlock(cropPos.east(), Blocks.AIR.defaultBlockState(), Block.UPDATE_NONE);
		helper.getLevel().setBlock(cropPos.above(16), Blocks.GLASS.defaultBlockState(), Block.UPDATE_NONE);
		helper.assertTrue(
			SeasonalPlantClock.hasGreenhouseGlass(helper.getLevel(), cropPos),
			"Greenhouse glass at the inclusive sixteen-block limit was not detected"
		);
		for (Plant plant : Plant.values()) {
			helper.assertTrue(
				SeasonalPlantClock.allowsGrowth(
					helper.getLevel(),
					cropPos,
					plant,
					RandomSource.create(0x9A44E1L + plant.ordinal())
				),
				"Greenhouse did not bypass " + plant + " winter dormancy"
			);
		}
		helper.assertTrue(
			ModBlocks.CORN.isValidBonemealTarget(
				helper.getLevel(),
				cropPos,
				helper.getLevel().getBlockState(cropPos)
			),
			"Actual corn bonemeal target did not honor greenhouse fertility"
		);
		ModBlocks.CORN.performBonemeal(
			helper.getLevel(),
			RandomSource.create(0xB04E5EEDL),
			cropPos,
			helper.getLevel().getBlockState(cropPos)
		);
		helper.assertTrue(
			ModBlocks.CORN.getAge(helper.getLevel().getBlockState(cropPos)) > 0,
			"Actual corn bonemeal did not grow under greenhouse glass"
		);

		helper.getLevel().setBlock(cropPos.above(16), Blocks.AIR.defaultBlockState(), Block.UPDATE_NONE);
		helper.getLevel().setBlock(cropPos.above(17), Blocks.GLASS.defaultBlockState(), Block.UPDATE_NONE);
		helper.assertTrue(
			!SeasonalPlantClock.hasGreenhouseGlass(helper.getLevel(), cropPos),
			"Glass above the sixteen-block greenhouse limit was accepted"
		);
		setOfficialSeason(helper, Subseason.MID_SUMMER);
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void pinnedConfigFlagsAndY47Y48BoundaryAreAuthoritative(GameTestHelper helper) {
		if (!Boolean.getBoolean(REQUIRED_PROFILE_PROPERTY)) {
			helper.succeed();
			return;
		}

		Object fertility = requiredConfigSection("fertility");
		Object seasons = requiredConfigSection("seasons");
		boolean originalSeasonalCrops = (boolean)readField(fertility, "seasonalCrops");
		int originalUndergroundLevel = (int)readField(
			fertility,
			"undergroundFertilityLevel"
		);
		boolean originalFoliageColor = (boolean)readField(
			seasons,
			"changeFoliageColor"
		);
		Object originalDimensions = readField(seasons, "whitelistedDimensions");
		BlockPos clockPos = helper.absolutePos(CLOCK_POS);

		try {
			setOfficialSeason(helper, Subseason.EARLY_WINTER);
			helper.assertTrue(
				SeasonalPlantClock.snapshot(helper.getLevel(), clockPos).isPresent(),
				"Pinned profile could not produce the precondition snapshot"
			);

			writeField(fertility, "seasonalCrops", false);
			for (Plant plant : Plant.values()) {
				helper.assertTrue(
					SeasonalPlantClock.allowsGrowth(
						helper.getLevel(),
						clockPos,
						plant,
						RandomSource.create(0x5EA50C0L + plant.ordinal())
					),
					"seasonalCrops=false did not restore baseline growth for " + plant
				);
				helper.assertTrue(
					SeasonalPlantClock.allowsBonemeal(helper.getLevel(), clockPos, plant),
					"seasonalCrops=false did not restore baseline bonemeal for " + plant
				);
			}
			writeField(fertility, "seasonalCrops", true);

			writeField(seasons, "changeFoliageColor", false);
			helper.assertTrue(
				!SeasonalPlantClock.seasonalFoliageColorEnabled(
					helper.getLevel(),
					clockPos
				),
				"changeFoliageColor=false did not disable BTB seasonal tints"
			);
			writeField(seasons, "changeFoliageColor", true);
			helper.assertTrue(
				SeasonalPlantClock.seasonalFoliageColorEnabled(
					helper.getLevel(),
					clockPos
				),
				"changeFoliageColor=true did not enable BTB seasonal tints"
			);

			writeField(seasons, "whitelistedDimensions", List.of());
			helper.assertTrue(
				SeasonalPlantClock.snapshot(helper.getLevel(), clockPos).isEmpty(),
				"Removing the Overworld from the Serene whitelist did not restore baseline behavior"
			);
			writeField(seasons, "whitelistedDimensions", originalDimensions);
			helper.assertTrue(
				SeasonalPlantClock.snapshot(helper.getLevel(), clockPos).isPresent(),
				"Restoring the Serene dimension whitelist did not restore the snapshot"
			);

			BlockPos y47 = new BlockPos(clockPos.getX(), 47, clockPos.getZ());
			BlockPos y48 = y47.above();
			BlockPos roofPos = y48.above();

			// Keep the boundary probe in the GameTest's already-loaded column. The
			// previous horizontal offsets could cross into the next chunk. Also let
			// the threaded skylight engine consume the verified roof placement before
			// asking canSeeSky; that query intentionally reads propagated sky light.
			helper.getLevel().getChunkAt(y47);
			for (BlockPos pos : List.of(y47, y48, roofPos)) {
				helper.getLevel().setBlock(
					pos,
					Blocks.AIR.defaultBlockState(),
					Block.UPDATE_ALL
				);
			}
			helper.assertTrue(
				helper.getLevel().setBlock(
					y47,
					Blocks.DIRT.defaultBlockState(),
					Block.UPDATE_ALL
				),
				"Y47 underground boundary probe could not be placed"
			);
			helper.assertTrue(
				helper.getLevel().setBlock(
					y48,
					Blocks.DIRT.defaultBlockState(),
					Block.UPDATE_ALL
				),
				"Y48 underground boundary probe could not be placed"
			);
			helper.assertTrue(
				helper.getLevel().setBlock(
					roofPos,
					Blocks.STONE.defaultBlockState(),
					Block.UPDATE_ALL
				),
				"Underground boundary roof could not be placed"
			);
			helper.assertTrue(
				helper.getLevel().getBlockState(roofPos).is(Blocks.STONE),
				"Underground boundary roof was not present after placement"
			);
			helper.runAfterDelay(2, () -> {
				try {
					writeField(fertility, "seasonalCrops", true);
					writeField(fertility, "undergroundFertilityLevel", 48);
					writeField(seasons, "whitelistedDimensions", originalDimensions);
					setOfficialSeason(helper, Subseason.EARLY_WINTER);

					helper.assertTrue(
						helper.getLevel().getBlockState(roofPos).is(Blocks.STONE),
						"Underground boundary roof disappeared before skylight settled"
					);
					for (BlockPos probe : List.of(y47, y48)) {
						helper.assertTrue(
							!helper.getLevel().canSeeSky(probe),
							"Roofed underground boundary probe retained sky access at " + probe
						);
					}

					helper.assertTrue(
						SeasonalPlantClock.isShelteredUnderground(helper.getLevel(), y47),
						"Y47 was not exempt below the configured exclusive threshold 48"
					);
					helper.assertTrue(
						!SeasonalPlantClock.isShelteredUnderground(helper.getLevel(), y48),
						"Y48 was incorrectly exempt at the configured exclusive threshold 48"
					);
					for (Plant plant : Plant.values()) {
						helper.assertTrue(
							SeasonalPlantClock.allowsGrowth(
								helper.getLevel(),
								y47,
								plant,
								RandomSource.create(0x470000L + plant.ordinal())
							),
							"Y47 underground exemption did not bypass winter dormancy for " + plant
						);
						helper.assertTrue(
							!SeasonalPlantClock.allowsGrowth(
								helper.getLevel(),
								y48,
								plant,
								RandomSource.create(0x480000L + plant.ordinal())
							),
							"Y48 boundary incorrectly bypassed winter dormancy for " + plant
						);
					}
				} finally {
					writeField(fertility, "seasonalCrops", originalSeasonalCrops);
					writeField(
						fertility,
						"undergroundFertilityLevel",
						originalUndergroundLevel
					);
					writeField(seasons, "changeFoliageColor", originalFoliageColor);
					writeField(seasons, "whitelistedDimensions", originalDimensions);
					setOfficialSeason(helper, Subseason.MID_SUMMER);
				}
				helper.succeed();
			});
		} finally {
			writeField(fertility, "seasonalCrops", originalSeasonalCrops);
			writeField(
				fertility,
				"undergroundFertilityLevel",
				originalUndergroundLevel
			);
			writeField(seasons, "changeFoliageColor", originalFoliageColor);
			writeField(seasons, "whitelistedDimensions", originalDimensions);
			setOfficialSeason(helper, Subseason.MID_SUMMER);
		}
	}

	@GameTest(maxTicks = 20)
	public void pinnedOutOfSeasonModesCoverTicksAndPlayerBonemeal(GameTestHelper helper) {
		if (!Boolean.getBoolean(REQUIRED_PROFILE_PROPERTY)) {
			helper.succeed();
			return;
		}

		Object fertility = requiredConfigSection("fertility");
		boolean originalSeasonalCrops = (boolean)readField(fertility, "seasonalCrops");
		int originalBehavior = (int)readField(fertility, "outOfSeasonCropBehavior");
		BlockPos cropPos = helper.absolutePos(CLOCK_POS);
		BlockPos saplingPos = cropPos.east(2);

		try {
			writeField(fertility, "seasonalCrops", true);
			setOfficialSeason(helper, Subseason.EARLY_WINTER);
			for (int dy = 1; dy <= 16; dy++) {
				helper.getLevel().setBlock(
					cropPos.above(dy),
					Blocks.AIR.defaultBlockState(),
					Block.UPDATE_NONE
				);
			}
			helper.getLevel().setBlock(
				cropPos.below(),
				Blocks.FARMLAND.defaultBlockState(),
				Block.UPDATE_ALL
			);
			helper.getLevel().setBlock(
				cropPos,
				ModBlocks.CORN.defaultBlockState(),
				Block.UPDATE_ALL
			);

			writeField(fertility, "outOfSeasonCropBehavior", 0);
			helper.getLevel().getRandom().setSeed(0x5EA500L);
			int cancelled = 0;
			int allowed = 0;
			for (int attempt = 0; attempt < 96; attempt++) {
				CallbackInfo callback = invokeSeasonalCropGrowthHandler(
					helper.getLevel(),
					cropPos,
					helper.getLevel().getBlockState(cropPos)
				);
				if (callback.isCancelled()) {
					cancelled++;
				} else {
					allowed++;
				}
			}
			helper.assertTrue(
				cancelled > 0 && allowed > 0,
				"Mode 0 did not exercise both the five-in-six suppression and one-in-six growth paths"
			);

			writeField(fertility, "outOfSeasonCropBehavior", 1);
			CallbackInfo modeOneTick = invokeSeasonalCropGrowthHandler(
				helper.getLevel(),
				cropPos,
				helper.getLevel().getBlockState(cropPos)
			);
			helper.assertTrue(modeOneTick.isCancelled(), "Mode 1 did not cancel growth");
			helper.assertTrue(
				helper.getLevel().getBlockState(cropPos).is(ModBlocks.CORN),
				"Mode 1 destroyed dormant corn"
			);
			BonemealInvocation modeOneBonemeal = invokeSeasonalBonemealHandler(
				helper,
				cropPos
			);
			helper.assertTrue(modeOneBonemeal.cancelled(), "Mode 1 bonemeal was not cancelled");
			helper.assertValueEqual(
				modeOneBonemeal.result(),
				InteractionResult.FAIL,
				"Mode 1 bonemeal result"
			);
			helper.assertValueEqual(
				modeOneBonemeal.remainingBonemeal(),
				2,
				"Mode 1 bonemeal consumption"
			);

			writeField(fertility, "outOfSeasonCropBehavior", 2);
			helper.getLevel().setBlock(
				cropPos,
				ModBlocks.CORN.defaultBlockState(),
				Block.UPDATE_ALL
			);
			BonemealInvocation destructiveBonemeal = invokeSeasonalBonemealHandler(
				helper,
				cropPos
			);
			helper.assertTrue(
				destructiveBonemeal.cancelled(),
				"Mode 2 corn bonemeal was not cancelled"
			);
			helper.assertValueEqual(
				destructiveBonemeal.result(),
				InteractionResult.SUCCESS,
				"Mode 2 destructive bonemeal result"
			);
			helper.assertTrue(
				helper.getLevel().getBlockState(cropPos).isAir(),
				"Mode 2 did not destroy out-of-season corn"
			);

			helper.getLevel().setBlock(
				saplingPos.below(),
				Blocks.DIRT.defaultBlockState(),
				Block.UPDATE_ALL
			);
			helper.getLevel().setBlock(
				saplingPos,
				ModBlocks.BLACK_WALNUT_SAPLING.defaultBlockState(),
				Block.UPDATE_ALL
			);
			BonemealInvocation protectedSapling = invokeSeasonalBonemealHandler(
				helper,
				saplingPos
			);
			helper.assertTrue(
				protectedSapling.cancelled(),
				"Mode 2 protected sapling interaction was not cancelled"
			);
			helper.assertTrue(
				helper.getLevel().getBlockState(saplingPos).is(
					ModBlocks.BLACK_WALNUT_SAPLING
				),
				"Mode 2 destroyed a sapling in unbreakable_infertile_crops"
			);
		} finally {
			writeField(fertility, "seasonalCrops", originalSeasonalCrops);
			writeField(fertility, "outOfSeasonCropBehavior", originalBehavior);
			setOfficialSeason(helper, Subseason.MID_SUMMER);
		}
		helper.succeed();
	}

	private static Object requiredConfigSection(String fieldName) {
		try {
			Class<?> modConfig = Class.forName("sereneseasons.init.ModConfig");
			return modConfig.getField(fieldName).get(null);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException(
				"Cannot access pinned Serene Seasons config section " + fieldName,
				exception
			);
		}
	}

	private static Object readField(Object target, String fieldName) {
		try {
			return target.getClass().getField(fieldName).get(target);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException(
				"Cannot read pinned Serene Seasons config field " + fieldName,
				exception
			);
		}
	}

	private static void writeField(Object target, String fieldName, Object value) {
		try {
			Field field = target.getClass().getField(fieldName);
			field.set(target, value);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException(
				"Cannot write pinned Serene Seasons config field " + fieldName,
				exception
			);
		}
	}

	private static CallbackInfo invokeSeasonalCropGrowthHandler(
		net.minecraft.world.level.Level level,
		BlockPos pos,
		BlockState state
	) {
		CallbackInfo callback = new CallbackInfo("before_the_blight_test", true);
		try {
			Class<?> handler = Class.forName(
				"sereneseasons.season.SeasonalCropGrowthHandler"
			);
			Method method = handler.getMethod(
				"onCropGrowth",
				net.minecraft.world.level.Level.class,
				BlockPos.class,
				BlockState.class,
				CallbackInfo.class
			);
			method.invoke(null, level, pos, state, callback);
			return callback;
		} catch (ReflectiveOperationException exception) {
			throw rethrowOptionalInvocation("crop growth", exception);
		}
	}

	private static BonemealInvocation invokeSeasonalBonemealHandler(
		GameTestHelper helper,
		BlockPos pos
	) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE_MEAL, 3));
		BlockHitResult hit = new BlockHitResult(
			Vec3.atCenterOf(pos),
			Direction.UP,
			pos,
			false
		);
		try {
			Class<?> eventType = Class.forName(
				"glitchcore.event.player.PlayerInteractEvent$UseBlock"
			);
			Constructor<?> constructor = eventType.getConstructor(
				Player.class,
				InteractionHand.class,
				BlockHitResult.class
			);
			Object event = constructor.newInstance(player, InteractionHand.MAIN_HAND, hit);
			Class<?> handler = Class.forName(
				"sereneseasons.season.SeasonalCropGrowthHandler"
			);
			handler.getMethod("applyBonemeal", eventType).invoke(null, event);
			boolean cancelled = (boolean)eventType.getMethod("isCancelled").invoke(event);
			InteractionResult result = (InteractionResult)eventType
				.getMethod("getCancelResult")
				.invoke(event);
			return new BonemealInvocation(
				cancelled,
				result,
				player.getItemInHand(InteractionHand.MAIN_HAND).getCount()
			);
		} catch (ReflectiveOperationException exception) {
			throw rethrowOptionalInvocation("player bonemeal", exception);
		}
	}

	private static RuntimeException rethrowOptionalInvocation(
		String operation,
		ReflectiveOperationException exception
	) {
		Throwable cause = exception instanceof InvocationTargetException invocation
			? invocation.getCause()
			: exception;
		if (cause instanceof RuntimeException runtimeException) {
			return runtimeException;
		}
		if (cause instanceof Error error) {
			throw error;
		}
		return new IllegalStateException(
			"Pinned Serene Seasons " + operation + " invocation failed",
			cause
		);
	}

	private record BonemealInvocation(
		boolean cancelled,
		InteractionResult result,
		int remainingBonemeal
	) {
	}

	private static void setOfficialSeason(GameTestHelper helper, Subseason subseason) {
		String command = "season set " + subseason.name().toLowerCase(Locale.ROOT);
		helper.getLevel().getServer().getCommands().performPrefixedCommand(
			helper.getLevel().getServer().createCommandSourceStack().withLevel(helper.getLevel()),
			command
		);
	}

	private static void assertClose(
		GameTestHelper helper,
		double actual,
		double expected,
		String label
	) {
		helper.assertTrue(
			Math.abs(actual - expected) <= EPSILON,
			label + " expected " + expected + " but got " + actual
		);
	}

	private static void assertVersion(GameTestHelper helper, String modId, String expected) {
		ModContainer container = FabricLoader.getInstance()
			.getModContainer(modId)
			.orElseThrow(() -> new IllegalStateException("Missing required test mod " + modId));
		helper.assertValueEqual(
			container.getMetadata().getVersion().getFriendlyString(),
			expected,
			modId + " pinned compatibility version"
		);
	}

	private static void assertBlockWindows(
		GameTestHelper helper,
		String label,
		Block block,
		boolean spring,
		boolean summer,
		boolean autumn,
		boolean winter,
		boolean yearRound
	) {
		helper.assertValueEqual(
			block.defaultBlockState().is(SPRING_CROPS_BLOCK),
			spring,
			label + " spring growth tag"
		);
		helper.assertValueEqual(
			block.defaultBlockState().is(SUMMER_CROPS_BLOCK),
			summer,
			label + " summer growth tag"
		);
		helper.assertValueEqual(
			block.defaultBlockState().is(AUTUMN_CROPS_BLOCK),
			autumn,
			label + " autumn growth tag"
		);
		helper.assertValueEqual(
			block.defaultBlockState().is(WINTER_CROPS_BLOCK),
			winter,
			label + " winter growth tag"
		);
		helper.assertValueEqual(
			block.defaultBlockState().is(YEAR_ROUND_CROPS_BLOCK),
			yearRound,
			label + " year-round growth tag"
		);
	}

	private static void assertItemWindows(
		GameTestHelper helper,
		String label,
		Item item,
		boolean spring,
		boolean summer,
		boolean autumn,
		boolean winter,
		boolean yearRound
	) {
		ItemStack stack = new ItemStack(item);
		helper.assertValueEqual(stack.is(SPRING_CROPS_ITEM), spring, label + " spring item tag");
		helper.assertValueEqual(stack.is(SUMMER_CROPS_ITEM), summer, label + " summer item tag");
		helper.assertValueEqual(stack.is(AUTUMN_CROPS_ITEM), autumn, label + " autumn item tag");
		helper.assertValueEqual(stack.is(WINTER_CROPS_ITEM), winter, label + " winter item tag");
		helper.assertValueEqual(
			stack.is(YEAR_ROUND_CROPS_ITEM),
			yearRound,
			label + " year-round item tag"
		);
	}

	private static TagKey<Block> blockTag(String path) {
		return TagKey.create(
			Registries.BLOCK,
			Identifier.fromNamespaceAndPath(SERENE_SEASONS_MOD_ID, path)
		);
	}

	private static TagKey<Item> itemTag(String path) {
		return TagKey.create(
			Registries.ITEM,
			Identifier.fromNamespaceAndPath(SERENE_SEASONS_MOD_ID, path)
		);
	}
}
