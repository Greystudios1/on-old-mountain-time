package net.beforetheblight.gametest;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.beforetheblight.BeforeTheBlight;
import net.beforetheblight.block.ChestnutSaplingBlock;
import net.beforetheblight.registry.ModBlocks;
import net.beforetheblight.worldgen.feature.ModConfiguredFeatures;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

/**
 * Executes the registered chestnut sapling routes in an isolated server world.
 * These tests deliberately assert voxel/state contracts, not visual realism.
 */
public final class ChestnutTreeGameTests {
	private static final String TREE_ARENA = "before_the_blight_gametest:chestnut_tree_48";
	private static final int TREE_ARENA_SIZE = 48;
	private static final BlockPos TREE_ORIGIN = new BlockPos(24, 4, 24);
	private static final BlockPos NATURAL_TREE_ORIGIN = new BlockPos(24, 6, 24);
	private static final Direction[] CARDINAL_DIRECTIONS = {
		Direction.NORTH,
		Direction.EAST,
		Direction.SOUTH,
		Direction.WEST
	};
	private static final int LARGE_SCAN_RADIUS = 12;
	private static final int LARGE_SCAN_HEIGHT = 40;
	private static final int MAX_BONEMEAL_ATTEMPTS_PER_STAGE = 32;
	private static final CanopyContract FOREST_CANOPY = new CanopyContract(
		500,
		14,
		22,
		48,
		0.22,
		80,
		75,
		1,
		0.43,
		4,
		0.38
	);
	private static final CanopyContract OLD_GROWTH_CANOPY = new CanopyContract(
		1050,
		14,
		64,
		66,
		0.10,
		160,
		105,
		0,
		0.45,
		3,
		0.55
	);

	@GameTest(structure = TREE_ARENA, maxTicks = 200, padding = 4)
	public void chestnutOneByOneGrowsFieldForm(GameTestHelper helper) {
		SeasonGameTestSupport.useGrowingSeasonIfPresent(helper);
		BlockPos ordinaryOrigin = new BlockPos(12, 4, 24);
		BlockPos matureOrigin = new BlockPos(35, 4, 24);
		prepareSoil(helper, ordinaryOrigin, 10);
		prepareSoil(helper, matureOrigin, 10);
		placeReadySapling(helper, ordinaryOrigin);
		placeReadySapling(helper, matureOrigin);

		advanceTree(helper, ordinaryOrigin, 10L);
		advanceTree(helper, matureOrigin, 4096L);

		TreeSnapshot ordinary = scanTree(helper, ordinaryOrigin, 11, 26);
		TreeSnapshot mature = scanTree(helper, matureOrigin, 11, 28);
		assertNoTreeOutsideEnvelopes(
			helper,
			"field routes",
			new TreeEnvelope(ordinaryOrigin, 11, 0, 26),
			new TreeEnvelope(matureOrigin, 11, 0, 28)
		);
		assertFieldForm(helper, ordinaryOrigin, ordinary, 17, 17, "ordinary field seed 10");
		assertFieldForm(helper, matureOrigin, mature, 23, 23, "mature field seed 4096");
		helper.assertTrue(
			occupiedTop(mature, matureOrigin) > occupiedTop(ordinary, ordinaryOrigin),
			"Pinned mature field seed did not exceed the pinned ordinary field result"
		);
		printProbe("field-ordinary", 10L, ordinaryOrigin, ordinary);
		printProbe("field-mature", 4096L, matureOrigin, mature);
		helper.succeed();
	}

	@GameTest(structure = TREE_ARENA, maxTicks = 200, padding = 4)
	public void chestnutTwoByTwoGrowsForestForm(GameTestHelper helper) {
		SeasonGameTestSupport.useGrowingSeasonIfPresent(helper);
		prepareSoil(helper, TREE_ORIGIN, LARGE_SCAN_RADIUS);
		placeReadySquare(helper, TREE_ORIGIN, 2);
		advanceTree(helper, TREE_ORIGIN, 0L);

		TreeSnapshot tree = scanTree(helper, TREE_ORIGIN, LARGE_SCAN_RADIUS, LARGE_SCAN_HEIGHT);
		assertNoTreeOutsideEnvelopes(
			helper,
			"forest route",
			new TreeEnvelope(TREE_ORIGIN, LARGE_SCAN_RADIUS, 2, LARGE_SCAN_HEIGHT)
		);
		assertNoSaplings(helper, TREE_ORIGIN, 2, "forest route");
		assertForestTrunk(helper, TREE_ORIGIN, tree);
		assertForestRootsAndBranches(helper, TREE_ORIGIN, tree);
		assertAllLogsConnected(helper, tree, "forest tree");
		assertLogAxisContinuity(helper, tree, TREE_ORIGIN, "forest tree");
		assertGeneratedLeaves(helper, tree, "forest tree");
		assertCanopyDensity(helper, tree, TREE_ORIGIN, FOREST_CANOPY, "forest tree");
		printProbe("forest", 0L, TREE_ORIGIN, tree);
		helper.succeed();
	}

	@GameTest(structure = TREE_ARENA, maxTicks = 200, padding = 4)
	public void chestnutLargeCanopiesStayDenseAcrossAlternateSeeds(GameTestHelper helper) {
		SeasonGameTestSupport.useGrowingSeasonIfPresent(helper);
		BlockPos forestOrigin = new BlockPos(12, 4, 12);
		BlockPos oldGrowthOrigin = new BlockPos(35, 4, 35);
		prepareSoil(helper, forestOrigin, LARGE_SCAN_RADIUS);
		prepareSoil(helper, oldGrowthOrigin, LARGE_SCAN_RADIUS);
		setSurfaceSoilSquare(helper, forestOrigin, 2, Blocks.FARMLAND.defaultBlockState());
		setSurfaceSoilSquare(
			helper,
			oldGrowthOrigin.offset(-1, 0, -1),
			3,
			Blocks.FARMLAND.defaultBlockState()
		);
		placeReadySquare(helper, forestOrigin, 2);
		placeReadySquare(helper, oldGrowthOrigin.offset(-1, 0, -1), 3);

		advanceTree(helper, forestOrigin, 117L);
		advanceTree(helper, oldGrowthOrigin, 7331L);

		TreeSnapshot forest = scanTree(helper, forestOrigin, LARGE_SCAN_RADIUS, LARGE_SCAN_HEIGHT);
		TreeSnapshot oldGrowth = scanTree(helper, oldGrowthOrigin, LARGE_SCAN_RADIUS, LARGE_SCAN_HEIGHT);
		assertNoTreeOutsideEnvelopes(
			helper,
			"alternate-seed large-tree routes",
			new TreeEnvelope(forestOrigin, LARGE_SCAN_RADIUS, 2, LARGE_SCAN_HEIGHT),
			new TreeEnvelope(oldGrowthOrigin, LARGE_SCAN_RADIUS, 2, LARGE_SCAN_HEIGHT)
		);
		assertNoSaplings(helper, forestOrigin, 2, "alternate-seed forest route");
		assertNoSaplings(
			helper,
			oldGrowthOrigin.offset(-1, 0, -1),
			3,
			"alternate-seed old-growth route"
		);
		assertForestTrunk(helper, forestOrigin, forest);
		assertForestRootsAndBranches(helper, forestOrigin, forest);
		assertAllLogsConnected(helper, forest, "alternate-seed forest tree");
		assertLogAxisContinuity(helper, forest, forestOrigin, "alternate-seed forest tree");
		assertGeneratedLeaves(helper, forest, "alternate-seed forest tree");
		assertCanopyDensity(helper, forest, forestOrigin, FOREST_CANOPY, "alternate-seed forest tree");
		assertOldGrowthCommon(helper, oldGrowthOrigin, oldGrowth, true, "alternate-seed old growth");
		printProbe("forest-alternate", 117L, forestOrigin, forest);
		printProbe("old-growth-alternate", 7331L, oldGrowthOrigin, oldGrowth);
		helper.succeed();
	}

	@GameTest(structure = TREE_ARENA, maxTicks = 200, padding = 4)
	public void chestnutThreeByThreeGrowsThroughPublicBonemealPath(GameTestHelper helper) {
		SeasonGameTestSupport.useGrowingSeasonIfPresent(helper);
		prepareSoil(helper, TREE_ORIGIN, LARGE_SCAN_RADIUS);
		BlockPos northWest = TREE_ORIGIN.offset(-1, 0, -1);
		placeSaplingSquareAtStage(helper, northWest, 3, 0);

		BlockPos invalidWestRootCell = TREE_ORIGIN.west(2).below();
		helper.setBlock(invalidWestRootCell, Blocks.AIR);
		BonemealableBlock bonemealable = (BonemealableBlock) ModBlocks.CHESTNUT_SAPLING;
		ServerLevel level = helper.getLevel();
		for (int dx = 0; dx < 3; dx++) {
			for (int dz = 0; dz < 3; dz++) {
				BlockPos relativePos = northWest.offset(dx, 0, dz);
				BlockPos absolutePos = helper.absolutePos(relativePos);
				BlockState state = level.getBlockState(absolutePos);
				helper.assertTrue(
					bonemealable.isValidBonemealTarget(level, absolutePos, state),
					"3x3 stage-zero sapling rejected bonemeal at " + relativePos
				);
			}
		}

		BlockPos trigger = northWest;
		BlockPos absoluteTrigger = helper.absolutePos(trigger);
		ItemStack boneMeal = new ItemStack(Items.BONE_MEAL, 64);
		int stageZeroAttempts = useBoneMealUntilStageChanges(
			helper,
			level,
			trigger,
			absoluteTrigger,
			boneMeal,
			0,
			"stage-zero 3x3 corner"
		);
		helper.assertValueEqual(
			helper.getBlockState(trigger).getValue(SaplingBlock.STAGE),
			1,
			"real bone-meal item path did not advance the corner sapling to stage one"
		);
		for (int dx = 0; dx < 3; dx++) {
			for (int dz = 0; dz < 3; dz++) {
				BlockPos relativePos = northWest.offset(dx, 0, dz);
				BlockPos absolutePos = helper.absolutePos(relativePos);
				BlockState state = level.getBlockState(absolutePos);
				helper.assertTrue(
					bonemealable.isValidBonemealTarget(level, absolutePos, state),
					"mixed-stage 3x3 sapling rejected bonemeal at " + relativePos
				);
			}
		}

		int stageOneAttempts = useBoneMealUntilSaplingIsReplaced(
			helper,
			level,
			trigger,
			absoluteTrigger,
			boneMeal,
			"stage-one 3x3 corner"
		);
		BeforeTheBlight.LOGGER.info(
			"BTB_BONEMEAL_PROBE stage_zero_attempts={} stage_one_attempts={} consumed={} remaining={}",
			stageZeroAttempts,
			stageOneAttempts,
			64 - boneMeal.getCount(),
			boneMeal.getCount()
		);

		TreeSnapshot tree = scanTree(helper, TREE_ORIGIN, LARGE_SCAN_RADIUS, LARGE_SCAN_HEIGHT);
		helper.assertFalse(tree.logs().isEmpty(), "public 3x3 bonemeal route generated no chestnut logs");
		assertNoTreeOutsideEnvelopes(
			helper,
			"public 3x3 bonemeal route",
			new TreeEnvelope(TREE_ORIGIN, LARGE_SCAN_RADIUS, 2, LARGE_SCAN_HEIGHT)
		);
		assertNoSaplings(helper, northWest, 3, "public 3x3 bonemeal route");
		helper.assertBlockState(invalidWestRootCell, Blocks.AIR.defaultBlockState());
		assertOldGrowthCommon(helper, TREE_ORIGIN, tree, false, "public 3x3 bonemeal route");
		printProbe("old-growth-public-bonemeal-shared-rng", -1L, TREE_ORIGIN, tree);
		helper.succeed();
	}

	@GameTest(structure = TREE_ARENA, maxTicks = 200, padding = 4)
	public void chestnutThreeByThreeGrowsSolidOldGrowth(GameTestHelper helper) {
		SeasonGameTestSupport.useGrowingSeasonIfPresent(helper);
		TreeSnapshot tree = growOldGrowth(helper, 0L);
		assertOldGrowthCommon(helper, TREE_ORIGIN, tree, true, "solid old growth");
		for (int y = 0; y <= 1; y++) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					assertVerticalChestnutLog(
						helper,
						TREE_ORIGIN.offset(dx, y, dz),
						"solid old-growth 3x3 base"
					);
				}
			}
		}
		helper.assertValueEqual(
			countLogsAtY(tree, TREE_ORIGIN.getY() + 1),
			9L,
			"solid old-growth exact y=1 base count"
		);
		printProbe("old-growth-solid", 0L, TREE_ORIGIN, tree);
		helper.succeed();
	}

	@GameTest(structure = TREE_ARENA, maxTicks = 200, padding = 4)
	public void chestnutThreeByThreeGrowsHollowOldGrowth(GameTestHelper helper) {
		SeasonGameTestSupport.useGrowingSeasonIfPresent(helper);
		TreeSnapshot tree = growOldGrowth(helper, 2L);
		assertOldGrowthCommon(helper, TREE_ORIGIN, tree, true, "hollow old growth");

		Direction entrance = null;
		for (Direction direction : CARDINAL_DIRECTIONS) {
			boolean clear = true;
			for (int y = 0; y <= 1; y++) {
				clear &= helper.getBlockState(TREE_ORIGIN.relative(direction).above(y)).isAir();
			}
			if (clear) {
				helper.assertTrue(entrance == null, "Hollow old growth exposed more than one cardinal doorway");
				entrance = direction;
			}
		}
		helper.assertTrue(entrance != null, "Pinned hollow seed did not create a cardinal doorway");
		for (int y = 0; y <= 1; y++) {
			int ringLogs = 0;
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					BlockPos pos = TREE_ORIGIN.offset(dx, y, dz);
					boolean plannedOpening = dx == 0 && dz == 0
						|| dx == entrance.getStepX() && dz == entrance.getStepZ();
					if (plannedOpening) {
						helper.assertTrue(
							helper.getBlockState(pos).isAir(),
							"Hollow opening is blocked at " + pos
						);
					} else {
						assertVerticalChestnutLog(helper, pos, "hollow seven-log ring");
						ringLogs++;
					}
				}
			}
			helper.assertValueEqual(ringLogs, 7, "hollow ring log count at y=" + y);
			helper.assertTrue(
				helper.getBlockState(TREE_ORIGIN.above(y)).isAir(),
				"Hollow center is blocked at y=" + y
			);
			helper.assertTrue(
				helper.getBlockState(TREE_ORIGIN.relative(entrance).above(y)).isAir(),
				"Hollow doorway is blocked at y=" + y
			);
			helper.assertTrue(
				helper.getBlockState(TREE_ORIGIN.relative(entrance, 2).above(y)).isAir(),
				"Hollow approach is blocked at y=" + y
			);
		}
		helper.assertValueEqual(
			countLogsAtY(tree, TREE_ORIGIN.getY() + 1),
			7L,
			"hollow old-growth exact y=1 ring count"
		);
		helper.assertTrue(
			helper.getBlockState(TREE_ORIGIN.below()).is(ModBlocks.CHESTNUT_LOG),
			"Hollow center does not have its planned chestnut-log floor"
		);
		printProbe("old-growth-hollow", 2L, TREE_ORIGIN, tree);
		helper.succeed();
	}

	@GameTest(structure = TREE_ARENA, maxTicks = 200, padding = 4)
	public void naturalForestConfiguredFeatureFollowsSteppedRidgeTerrain(GameTestHelper helper) {
		prepareSteppedRidgeSoil(helper, NATURAL_TREE_ORIGIN, LARGE_SCAN_RADIUS);
		Map<BlockPos, BlockState> sentinels = placeNaturalForestRootPathSentinels(
			helper,
			NATURAL_TREE_ORIGIN
		);

		boolean placed = placeConfiguredTree(
			helper,
			ModConfiguredFeatures.CHESTNUT_FOREST_NATURAL,
			NATURAL_TREE_ORIGIN,
			117L
		);

		helper.assertTrue(placed, "natural forest configured feature rejected ordinary stepped Ridge terrain");
		TreeSnapshot tree = scanNaturalTree(helper, NATURAL_TREE_ORIGIN);
		assertNoTreeOutsideEnvelopes(
			helper,
			"natural forest configured feature",
			new TreeEnvelope(NATURAL_TREE_ORIGIN, LARGE_SCAN_RADIUS, 6, LARGE_SCAN_HEIGHT)
		);
		assertForestTrunk(helper, NATURAL_TREE_ORIGIN, tree);
		assertDownhillFoundationContinuity(
			helper,
			NATURAL_TREE_ORIGIN,
			0,
			1,
			0,
			1,
			"natural forest 2x2 foundation"
		);
		assertAtLeastTerrainFollowingCardinalRootArms(
			helper,
			tree,
			NATURAL_TREE_ORIGIN,
			3,
			2,
			"natural forest roots"
		);
		assertNaturalTreeSentinelsPreserved(helper, sentinels, "natural forest");
		assertAllLogsConnected(helper, tree, "natural forest tree");
		assertGeneratedLeaves(helper, tree, "natural forest tree");
		printProbe("forest-natural-stepped", 117L, NATURAL_TREE_ORIGIN, tree);
		helper.succeed();
	}

	@GameTest(structure = TREE_ARENA, maxTicks = 200, padding = 4)
	public void naturalOldGrowthConfiguredFeatureFollowsSteppedRidgeTerrain(GameTestHelper helper) {
		prepareSteppedRidgeSoil(helper, NATURAL_TREE_ORIGIN, LARGE_SCAN_RADIUS);
		Map<BlockPos, BlockState> sentinels = placeNaturalOldGrowthRootPathSentinels(
			helper,
			NATURAL_TREE_ORIGIN
		);

		boolean placed = placeConfiguredTree(
			helper,
			ModConfiguredFeatures.CHESTNUT_OLD_GROWTH_NATURAL,
			NATURAL_TREE_ORIGIN,
			7331L
		);

		helper.assertTrue(placed, "natural old-growth configured feature rejected ordinary stepped Ridge terrain");
		TreeSnapshot tree = scanNaturalTree(helper, NATURAL_TREE_ORIGIN);
		assertNoTreeOutsideEnvelopes(
			helper,
			"natural old-growth configured feature",
			new TreeEnvelope(NATURAL_TREE_ORIGIN, LARGE_SCAN_RADIUS, 6, LARGE_SCAN_HEIGHT)
		);
		for (int dx : new int[] {-1, 1}) {
			for (int dz : new int[] {-1, 1}) {
				assertVerticalChestnutLog(
					helper,
					NATURAL_TREE_ORIGIN.offset(dx, 0, dz),
					"natural old-growth 3x3 corner"
				);
			}
		}
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				assertVerticalChestnutLog(
					helper,
					NATURAL_TREE_ORIGIN.offset(dx, 2, dz),
					"natural old-growth full y=2 collar"
				);
			}
		}
		assertDownhillFoundationContinuity(
			helper,
			NATURAL_TREE_ORIGIN,
			-1,
			1,
			-1,
			1,
			"natural old-growth 3x3 foundation"
		);
		assertAtLeastTerrainFollowingCardinalRootArms(
			helper,
			tree,
			NATURAL_TREE_ORIGIN,
			3,
			2,
			"natural old-growth roots"
		);
		assertNaturalTreeSentinelsPreserved(helper, sentinels, "natural old growth");
		assertAllLogsConnected(helper, tree, "natural old-growth tree");
		assertGeneratedLeaves(helper, tree, "natural old-growth tree");
		printProbe("old-growth-natural-stepped", 7331L, NATURAL_TREE_ORIGIN, tree);
		helper.succeed();
	}

	@GameTest(structure = TREE_ARENA, maxTicks = 200, padding = 4)
	public void naturalForestConfiguredFeatureFailsAtomicallyWithTooFewRootArms(GameTestHelper helper) {
		prepareSteppedRidgeSoil(helper, NATURAL_TREE_ORIGIN, LARGE_SCAN_RADIUS);
		blockThreeForestRootDirections(helper, NATURAL_TREE_ORIGIN);
		Map<BlockPos, BlockState> before = snapshotNaturalTreeVolume(helper, NATURAL_TREE_ORIGIN);

		boolean placed = placeConfiguredTree(
			helper,
			ModConfiguredFeatures.CHESTNUT_FOREST_NATURAL,
			NATURAL_TREE_ORIGIN,
			117L
		);

		helper.assertFalse(placed, "natural forest placed with only one viable cardinal root arm");
		assertRollbackVolumeUnchanged(helper, before, "failed natural forest configured feature");
		assertNoGeneratedTree(helper, NATURAL_TREE_ORIGIN, "failed natural forest configured feature");
		helper.succeed();
	}

	@GameTest(structure = TREE_ARENA, maxTicks = 200, padding = 4)
	public void naturalOldGrowthConfiguredFeatureFailsAtomicallyWithTooFewRootArms(
		GameTestHelper helper
	) {
		prepareSteppedRidgeSoil(helper, NATURAL_TREE_ORIGIN, LARGE_SCAN_RADIUS);
		blockThreeOldGrowthRootDirections(helper, NATURAL_TREE_ORIGIN);
		Map<BlockPos, BlockState> before = snapshotNaturalTreeVolume(helper, NATURAL_TREE_ORIGIN);

		boolean placed = placeConfiguredTree(
			helper,
			ModConfiguredFeatures.CHESTNUT_OLD_GROWTH_NATURAL,
			NATURAL_TREE_ORIGIN,
			7331L
		);

		helper.assertFalse(placed, "natural old growth placed with only one viable cardinal root arm");
		assertRollbackVolumeUnchanged(helper, before, "failed natural old-growth configured feature");
		assertNoGeneratedTree(helper, NATURAL_TREE_ORIGIN, "failed natural old-growth configured feature");
		helper.succeed();
	}

	@GameTest(structure = TREE_ARENA, maxTicks = 200, padding = 4)
	public void naturalForestLatticeNeighborsBothPlaceAtTwelveBlocks(GameTestHelper helper) {
		BlockPos westOrigin = new BlockPos(18, NATURAL_TREE_ORIGIN.getY(), 24);
		BlockPos eastOrigin = westOrigin.east(12);
		prepareSoil(helper, NATURAL_TREE_ORIGIN, 18);

		boolean westPlaced = placeConfiguredTree(
			helper,
			ModConfiguredFeatures.CHESTNUT_FOREST_NATURAL,
			westOrigin,
			0L
		);
		helper.assertTrue(westPlaced, "first natural forest lattice neighbor did not place");
		TreeSnapshot westBeforeNeighbor = scanEntireArena(helper);
		boolean eastPlaced = placeConfiguredTree(
			helper,
			ModConfiguredFeatures.CHESTNUT_FOREST_NATURAL,
			eastOrigin,
			117L
		);

		helper.assertTrue(
			eastPlaced,
			"second natural forest lattice neighbor was rejected by the first tree at 12-block spacing"
		);
		for (BlockPos origin : List.of(westOrigin, eastOrigin)) {
			for (int dx = 0; dx <= 1; dx++) {
				for (int dz = 0; dz <= 1; dz++) {
					assertVerticalChestnutLog(
						helper,
						origin.offset(dx, 0, dz),
						"12-block natural forest lattice neighbor"
					);
				}
			}
		}
		assertNoTreeOutsideEnvelopes(
			helper,
			"12-block natural forest lattice neighbors",
			new TreeEnvelope(westOrigin, LARGE_SCAN_RADIUS, 6, LARGE_SCAN_HEIGHT),
			new TreeEnvelope(eastOrigin, LARGE_SCAN_RADIUS, 6, LARGE_SCAN_HEIGHT)
		);
		assertAdjacentCanopiesRemainMaterial(
			helper,
			westBeforeNeighbor,
			scanEntireArena(helper),
			FOREST_CANOPY,
			FOREST_CANOPY,
			"12-block natural forest lattice neighbors"
		);
		helper.succeed();
	}

	@GameTest(structure = TREE_ARENA, maxTicks = 200, padding = 4)
	public void naturalForestAndOldGrowthNeighborsBothPlaceAtTwelveBlocks(GameTestHelper helper) {
		BlockPos forestOrigin = new BlockPos(18, NATURAL_TREE_ORIGIN.getY(), 24);
		BlockPos oldGrowthOrigin = forestOrigin.east(12);
		prepareSoil(helper, NATURAL_TREE_ORIGIN, 18);

		boolean forestPlaced = placeConfiguredTree(
			helper,
			ModConfiguredFeatures.CHESTNUT_FOREST_NATURAL,
			forestOrigin,
			0L
		);
		helper.assertTrue(forestPlaced, "natural forest did not place before its old-growth neighbor");
		TreeSnapshot forestBeforeNeighbor = scanEntireArena(helper);

		boolean oldGrowthPlaced = placeConfiguredTree(
			helper,
			ModConfiguredFeatures.CHESTNUT_OLD_GROWTH_NATURAL,
			oldGrowthOrigin,
			7331L
		);
		helper.assertTrue(
			oldGrowthPlaced,
			"natural old growth was rejected by its forest neighbor at 12-block spacing"
		);
		assertForestBase(helper, forestOrigin, "12-block mixed forest neighbor");
		assertOldGrowthBase(helper, oldGrowthOrigin, "12-block mixed old-growth neighbor");
		assertNoTreeOutsideEnvelopes(
			helper,
			"12-block mixed natural large-tree neighbors",
			new TreeEnvelope(forestOrigin, LARGE_SCAN_RADIUS, 6, LARGE_SCAN_HEIGHT),
			new TreeEnvelope(oldGrowthOrigin, LARGE_SCAN_RADIUS, 6, LARGE_SCAN_HEIGHT)
		);
		assertAdjacentCanopiesRemainMaterial(
			helper,
			forestBeforeNeighbor,
			scanEntireArena(helper),
			FOREST_CANOPY,
			OLD_GROWTH_CANOPY,
			"12-block mixed natural large-tree neighbors"
		);
		helper.succeed();
	}

	@GameTest(structure = TREE_ARENA, maxTicks = 200, padding = 4)
	public void naturalOldGrowthAndForestNeighborsBothPlaceOnStaggeredRows(GameTestHelper helper) {
		BlockPos oldGrowthOrigin = new BlockPos(18, NATURAL_TREE_ORIGIN.getY(), 17);
		BlockPos forestOrigin = oldGrowthOrigin.offset(6, 0, 11);
		prepareSoil(helper, NATURAL_TREE_ORIGIN, 20);
		int deltaX = forestOrigin.getX() - oldGrowthOrigin.getX();
		int deltaZ = forestOrigin.getZ() - oldGrowthOrigin.getZ();
		helper.assertValueEqual(
			deltaX * deltaX + deltaZ * deltaZ,
			157,
			"staggered large-tree neighbor squared center distance"
		);

		boolean oldGrowthPlaced = placeConfiguredTree(
			helper,
			ModConfiguredFeatures.CHESTNUT_OLD_GROWTH_NATURAL,
			oldGrowthOrigin,
			7331L
		);
		helper.assertTrue(oldGrowthPlaced, "natural old growth did not place on the first staggered row");
		TreeSnapshot oldGrowthBeforeNeighbor = scanEntireArena(helper);

		boolean forestPlaced = placeConfiguredTree(
			helper,
			ModConfiguredFeatures.CHESTNUT_FOREST_NATURAL,
			forestOrigin,
			117L
		);
		helper.assertTrue(
			forestPlaced,
			"natural forest was rejected by its old-growth neighbor at sqrt(157)-block spacing"
		);
		assertOldGrowthBase(helper, oldGrowthOrigin, "staggered mixed old-growth neighbor");
		assertForestBase(helper, forestOrigin, "staggered mixed forest neighbor");
		assertNoTreeOutsideEnvelopes(
			helper,
			"staggered mixed natural large-tree neighbors",
			new TreeEnvelope(oldGrowthOrigin, LARGE_SCAN_RADIUS, 6, LARGE_SCAN_HEIGHT),
			new TreeEnvelope(forestOrigin, LARGE_SCAN_RADIUS, 6, LARGE_SCAN_HEIGHT)
		);
		assertAdjacentCanopiesRemainMaterial(
			helper,
			oldGrowthBeforeNeighbor,
			scanEntireArena(helper),
			OLD_GROWTH_CANOPY,
			FOREST_CANOPY,
			"staggered mixed natural large-tree neighbors"
		);
		helper.succeed();
	}

	@GameTest(structure = TREE_ARENA, maxTicks = 200, padding = 4)
	public void chestnutOneByOneObstructionRestoresExactly(GameTestHelper helper) {
		SeasonGameTestSupport.useGrowingSeasonIfPresent(helper);
		prepareSoil(helper, TREE_ORIGIN, 10);
		BlockState expected = readySapling();
		helper.setBlock(TREE_ORIGIN, expected);
		BlockPos obstacle = TREE_ORIGIN.above();
		helper.setBlock(obstacle, Blocks.BEDROCK);
		Map<BlockPos, BlockState> before = snapshotRollbackVolume(helper, TREE_ORIGIN);

		advanceTree(helper, TREE_ORIGIN, 10L);

		assertRollbackVolumeUnchanged(helper, before, "blocked 1x1");
		helper.assertBlockState(TREE_ORIGIN, expected);
		helper.assertBlockState(obstacle, Blocks.BEDROCK.defaultBlockState());
		assertNoGeneratedTree(helper, TREE_ORIGIN, "blocked 1x1");
		helper.succeed();
	}

	@GameTest(structure = TREE_ARENA, maxTicks = 200, padding = 4)
	public void chestnutTwoByTwoObstructionRollsBackWithoutLoss(GameTestHelper helper) {
		SeasonGameTestSupport.useGrowingSeasonIfPresent(helper);
		prepareSoil(helper, TREE_ORIGIN, LARGE_SCAN_RADIUS);
		BlockState expected = readySapling();
		placeReadySquare(helper, TREE_ORIGIN, 2);
		Set<BlockPos> obstacles = placeForestTerminalObstructions(helper, TREE_ORIGIN);
		Map<BlockPos, BlockState> before = snapshotRollbackVolume(helper, TREE_ORIGIN);

		advanceTree(helper, TREE_ORIGIN, 0L);

		assertRollbackVolumeUnchanged(helper, before, "blocked 2x2");
		for (int dx = 0; dx <= 1; dx++) {
			for (int dz = 0; dz <= 1; dz++) {
				helper.assertBlockState(TREE_ORIGIN.offset(dx, 0, dz), expected);
			}
		}
		for (BlockPos obstacle : obstacles) {
			helper.assertBlockState(obstacle, Blocks.BEDROCK.defaultBlockState());
		}
		assertNoGeneratedTree(helper, TREE_ORIGIN, "blocked 2x2");
		helper.succeed();
	}

	@GameTest(structure = TREE_ARENA, maxTicks = 200, padding = 4)
	public void chestnutThreeByThreeObstructionRestoresExactly(GameTestHelper helper) {
		SeasonGameTestSupport.useGrowingSeasonIfPresent(helper);
		prepareSoil(helper, TREE_ORIGIN, LARGE_SCAN_RADIUS);
		BlockPos northWest = TREE_ORIGIN.offset(-1, 0, -1);
		Map<BlockPos, BlockState> expected = new LinkedHashMap<>();
		for (int dx = 0; dx < 3; dx++) {
			for (int dz = 0; dz < 3; dz++) {
				BlockPos pos = northWest.offset(dx, 0, dz);
				int stage = (dx + dz) & 1;
				if (pos.equals(TREE_ORIGIN)) {
					stage = 1;
				}
				BlockState state = ModBlocks.CHESTNUT_SAPLING.defaultBlockState()
					.setValue(SaplingBlock.STAGE, stage);
				expected.put(pos, state);
				helper.setBlock(pos, state);
			}
		}
		BlockPos obstacle = TREE_ORIGIN.above(6);
		helper.setBlock(obstacle, Blocks.BEDROCK);
		Map<BlockPos, BlockState> before = snapshotRollbackVolume(helper, TREE_ORIGIN);

		advanceTree(helper, TREE_ORIGIN, 2L);

		assertRollbackVolumeUnchanged(helper, before, "blocked mixed-stage 3x3");
		for (Map.Entry<BlockPos, BlockState> entry : expected.entrySet()) {
			helper.assertBlockState(entry.getKey(), entry.getValue());
		}
		helper.assertBlockState(obstacle, Blocks.BEDROCK.defaultBlockState());
		assertNoGeneratedTree(helper, TREE_ORIGIN, "blocked mixed-stage 3x3");
		helper.succeed();
	}

	@GameTest(structure = TREE_ARENA, maxTicks = 200, padding = 4)
	public void chestnutThreeByThreeTooFewRootArmsRestoresExactly(GameTestHelper helper) {
		SeasonGameTestSupport.useGrowingSeasonIfPresent(helper);
		prepareSoil(helper, TREE_ORIGIN, LARGE_SCAN_RADIUS);
		BlockPos northWest = TREE_ORIGIN.offset(-1, 0, -1);
		placeMixedStageThreeByThree(helper, northWest);
		Map<BlockPos, BlockState> expectedSaplings = snapshotSquareStates(helper, northWest, 3);
		Set<BlockPos> rootObstacles = Set.of(
			TREE_ORIGIN.north(2).below(),
			TREE_ORIGIN.east(2).below(),
			TREE_ORIGIN.south(2).below()
		);
		for (BlockPos obstacle : rootObstacles) {
			helper.setBlock(obstacle, Blocks.BEDROCK);
		}
		Map<BlockPos, BlockState> before = snapshotRollbackVolume(helper, TREE_ORIGIN);

		advanceTree(helper, TREE_ORIGIN, 2L);

		assertRollbackVolumeUnchanged(helper, before, "fewer-than-two viable old-growth root arms");
		for (Map.Entry<BlockPos, BlockState> entry : expectedSaplings.entrySet()) {
			helper.assertBlockState(entry.getKey(), entry.getValue());
		}
		for (BlockPos obstacle : rootObstacles) {
			helper.assertBlockState(obstacle, Blocks.BEDROCK.defaultBlockState());
		}
		assertNoGeneratedTree(helper, TREE_ORIGIN, "fewer-than-two viable old-growth root arms");
		helper.succeed();
	}

	@GameTest(structure = TREE_ARENA, maxTicks = 200, padding = 4)
	public void chestnutThreeByThreeOptionalRootObstacleAdapts(GameTestHelper helper) {
		SeasonGameTestSupport.useGrowingSeasonIfPresent(helper);
		prepareSoil(helper, TREE_ORIGIN, LARGE_SCAN_RADIUS);
		BlockPos northWest = TREE_ORIGIN.offset(-1, 0, -1);
		placeMixedStageThreeByThree(helper, northWest);
		BlockPos obstacle = TREE_ORIGIN.south(4).below();
		helper.setBlock(obstacle, Blocks.BEDROCK);

		advanceTree(helper, TREE_ORIGIN, 2L);

		TreeSnapshot tree = scanTree(helper, TREE_ORIGIN, LARGE_SCAN_RADIUS, LARGE_SCAN_HEIGHT);
		helper.assertFalse(tree.logs().isEmpty(), "optional-root obstacle route generated no chestnut logs");
		assertNoTreeOutsideEnvelopes(
			helper,
			"optional-root obstacle route",
			new TreeEnvelope(TREE_ORIGIN, LARGE_SCAN_RADIUS, 2, LARGE_SCAN_HEIGHT)
		);
		helper.assertBlockState(obstacle, Blocks.BEDROCK.defaultBlockState());
		assertNoSaplings(helper, northWest, 3, "optional-root obstacle route");
		assertAtLeastCardinalRootArms(helper, tree, TREE_ORIGIN, 4, 3, "optional-root obstacle route");
		assertOldGrowthCommon(helper, TREE_ORIGIN, tree, false, "optional-root obstacle route");
		printProbe("old-growth-optional-root-adaptation", 2L, TREE_ORIGIN, tree);
		helper.succeed();
	}

	@GameTest(structure = TREE_ARENA, maxTicks = 200, padding = 4)
	public void chestnutLeavesSupportAndDecay(GameTestHelper helper) {
		SeasonGameTestSupport.useGrowingSeasonIfPresent(helper);
		prepareSoil(helper, TREE_ORIGIN, 10);
		placeReadySapling(helper, TREE_ORIGIN);
		advanceTree(helper, TREE_ORIGIN, 10L);
		TreeSnapshot tree = scanTree(helper, TREE_ORIGIN, 11, 26);
		assertNoTreeOutsideEnvelopes(
			helper,
			"leaf-support field route",
			new TreeEnvelope(TREE_ORIGIN, 11, 0, 26)
		);
		assertGeneratedLeaves(helper, tree, "field generated leaves");
		settleAndAssertGeneratedLeafDistances(helper, tree, "field generated leaves");
		BlockPos supportedLeaf = tree.leaves().iterator().next();
		BlockState supportedState = helper.getBlockState(supportedLeaf);
		helper.randomTick(supportedLeaf);
		helper.assertBlockState(supportedLeaf, supportedState);

		BlockPos isolatedLeaf = new BlockPos(2, 4, 2);
		BlockState decaying = ModBlocks.CHESTNUT_LEAVES.defaultBlockState()
			.setValue(LeavesBlock.PERSISTENT, false)
			.setValue(LeavesBlock.DISTANCE, LeavesBlock.DECAY_DISTANCE);
		helper.setBlock(isolatedLeaf, decaying);
		helper.randomTick(isolatedLeaf);
		helper.assertTrue(
			helper.getBlockState(isolatedLeaf).isAir(),
			"An isolated non-persistent distance-7 chestnut leaf did not decay"
		);
		printProbe("leaf-support", 10L, TREE_ORIGIN, tree);
		helper.succeed();
	}

	private static TreeSnapshot growOldGrowth(GameTestHelper helper, long seed) {
		prepareSoil(helper, TREE_ORIGIN, LARGE_SCAN_RADIUS);
		placeReadySquare(helper, TREE_ORIGIN.offset(-1, 0, -1), 3);
		advanceTree(helper, TREE_ORIGIN, seed);
		assertNoSaplings(helper, TREE_ORIGIN.offset(-1, 0, -1), 3, "old-growth route");
		TreeSnapshot tree = scanTree(helper, TREE_ORIGIN, LARGE_SCAN_RADIUS, LARGE_SCAN_HEIGHT);
		assertNoTreeOutsideEnvelopes(
			helper,
			"old-growth route",
			new TreeEnvelope(TREE_ORIGIN, LARGE_SCAN_RADIUS, 2, LARGE_SCAN_HEIGHT)
		);
		return tree;
	}

	private static void assertAdjacentCanopiesRemainMaterial(
		GameTestHelper helper,
		TreeSnapshot firstBeforeNeighbor,
		TreeSnapshot afterBoth,
		CanopyContract firstContract,
		CanopyContract secondContract,
		String label
	) {
		int initialFirstLeaves = firstBeforeNeighbor.leaves().size();
		long retainedFirstLeaves = firstBeforeNeighbor.leaves().stream()
			.filter(afterBoth.leaves()::contains)
			.count();
		long addedNeighborLeaves = afterBoth.leaves().stream()
			.filter(pos -> !firstBeforeNeighbor.leaves().contains(pos))
			.count();
		long minimumRetainedFirst = Math.round(firstContract.minimumLeaves() * 0.60);
		long minimumAddedNeighbor = Math.round(secondContract.minimumLeaves() * 0.60);

		helper.assertTrue(
			initialFirstLeaves >= firstContract.minimumLeaves(),
			label + " first canopy started with only " + initialFirstLeaves
				+ " leaves; expected at least " + firstContract.minimumLeaves()
		);
		helper.assertTrue(
			retainedFirstLeaves >= minimumRetainedFirst,
			label + " retained only " + retainedFirstLeaves + " first-tree leaves after neighbor placement; expected at least "
				+ minimumRetainedFirst
		);
		helper.assertTrue(
			addedNeighborLeaves >= minimumAddedNeighbor,
			label + " added only " + addedNeighborLeaves + " neighbor-canopy leaves; expected at least "
				+ minimumAddedNeighbor
		);
	}

	private static void assertForestBase(GameTestHelper helper, BlockPos origin, String label) {
		for (int dx = 0; dx <= 1; dx++) {
			for (int dz = 0; dz <= 1; dz++) {
				assertVerticalChestnutLog(helper, origin.offset(dx, 0, dz), label);
			}
		}
	}

	private static void assertOldGrowthBase(GameTestHelper helper, BlockPos origin, String label) {
		for (int dx : new int[] {-1, 1}) {
			for (int dz : new int[] {-1, 1}) {
				assertVerticalChestnutLog(helper, origin.offset(dx, 0, dz), label + " corner");
			}
		}
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				assertVerticalChestnutLog(helper, origin.offset(dx, 2, dz), label + " full y=2 collar");
			}
		}
	}

	private static void assertFieldForm(
		GameTestHelper helper,
		BlockPos origin,
		TreeSnapshot tree,
		int minTop,
		int maxTop,
		String label
	) {
		helper.assertFalse(tree.logs().isEmpty(), label + " generated no chestnut logs");
		helper.assertFalse(tree.leaves().isEmpty(), label + " generated no chestnut leaves");
		helper.assertFalse(helper.getBlockState(origin).is(ModBlocks.CHESTNUT_SAPLING), label + " left its sapling");
		assertChestnutLog(helper, origin.above(3), label + " low trunk");
		int lowSliceLogs = 0;
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (helper.getBlockState(origin.offset(dx, 3, dz)).is(ModBlocks.CHESTNUT_LOG)) {
					lowSliceLogs++;
				}
			}
		}
		helper.assertValueEqual(lowSliceLogs, 1, label + " low single-stem slice");
		int top = occupiedTop(tree, origin);
		helper.assertTrue(
			top >= minTop && top <= maxTop,
			label + " occupied top " + top + " outside " + minTop + ".." + maxTop
		);
		assertLogAxes(helper, tree, label);
		assertGeneratedLeaves(helper, tree, label);
		assertAllLogsTouchConnected(helper, tree, label);
		boolean horizontalBranch = tree.logs().stream().anyMatch(pos -> {
			BlockState state = helper.getBlockState(pos);
			return pos.getY() >= origin.getY() + 4
				&& state.getValue(RotatedPillarBlock.AXIS) != Direction.Axis.Y;
		});
		helper.assertTrue(horizontalBranch, label + " has no lateral branch log");
		assertCrownSpread(helper, tree, origin, 3, label);
	}

	private static void assertForestTrunk(GameTestHelper helper, BlockPos origin, TreeSnapshot tree) {
		int trunkTop = maximumCoreLogY(tree, origin, 0, 1, 0, 1) - origin.getY() + 1;
		helper.assertTrue(
			trunkTop >= 21 && trunkTop <= 28,
			"forest trunk level count " + trunkTop + " outside 21..28"
		);
		for (int y = 0; y < trunkTop; y++) {
			for (int dx = 0; dx <= 1; dx++) {
				for (int dz = 0; dz <= 1; dz++) {
					BlockPos pos = origin.offset(dx, y, dz);
					assertChestnutLog(helper, pos, "forest 2x2 clear bole");
					helper.assertValueEqual(
						helper.getBlockState(pos).getValue(RotatedPillarBlock.AXIS),
						Direction.Axis.Y,
						"forest bole axis at " + pos
					);
				}
			}
		}
		for (int y = 1; y < 15; y++) {
			int absoluteY = origin.getY() + y;
			long lowSliceLogs = tree.logs().stream().filter(pos -> pos.getY() == absoluteY).count();
			helper.assertValueEqual(lowSliceLogs, 4L, "forest clear-bole log count at y=" + y);
		}
		int top = occupiedTop(tree, origin);
		helper.assertTrue(top >= 22 && top <= 29, "forest occupied top " + top + " outside 22..29");
	}

	private static void assertForestRootsAndBranches(GameTestHelper helper, BlockPos origin, TreeSnapshot tree) {
		long depthTwo = tree.logs().stream()
			.filter(pos -> pos.getY() == origin.getY() - 2)
			.peek(pos -> helper.assertValueEqual(
				helper.getBlockState(pos).getValue(RotatedPillarBlock.AXIS),
				Direction.Axis.Y,
				"forest depth-two terminal axis at " + pos
			))
			.count();
		helper.assertValueEqual(depthTwo, 4L, "forest depth-two terminal count");
		assertCardinalExtents(helper, tree, origin, -3, 4, -3, 4, "forest roots");
		assertHighBranches(helper, tree, origin, -4, 5, -4, 5, "forest branches");
	}

	private static void assertOldGrowthCommon(
		GameTestHelper helper,
		BlockPos origin,
		TreeSnapshot tree,
		boolean requireAllCardinalRootArms,
		String label
	) {
		helper.assertFalse(tree.logs().isEmpty(), label + " generated no logs");
		helper.assertFalse(tree.leaves().isEmpty(), label + " generated no leaves");
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				assertVerticalChestnutLog(helper, origin.offset(dx, 2, dz), label + " full y=2 collar");
			}
		}
		helper.assertValueEqual(countLogsAtY(tree, origin.getY() + 2), 9L, label + " exact y=2 collar count");
		int trunkTop = maximumCoreLogY(tree, origin, -1, 1, -1, 1) - origin.getY() + 1;
		helper.assertTrue(
			trunkTop >= 28 && trunkTop <= 35,
			label + " trunk level count " + trunkTop + " outside 28..35"
		);
		Set<BlockPos> crossOffsets = Set.of(
			BlockPos.ZERO,
			new BlockPos(0, 0, -1),
			new BlockPos(1, 0, 0),
			new BlockPos(0, 0, 1),
			new BlockPos(-1, 0, 0)
		);
		int taperStart = trunkTop - 8;
		for (int y = 3; y < taperStart; y++) {
			helper.assertValueEqual(
				verticalCoreLogOffsetsAt(helper, origin, y),
				crossOffsets,
				label + " exact vertical cross bole at y=" + y
			);
			if (y < 15) {
				helper.assertValueEqual(countLogsAtY(tree, origin.getY() + y), 5L, label + " exact clear-bole count at y=" + y);
			}
		}
		Set<BlockPos> taperOffsets = coreLogOffsetsAt(helper, origin, trunkTop - 1);
		helper.assertValueEqual(taperOffsets.size(), 4, label + " top taper footprint size");
		helper.assertTrue(taperOffsets.contains(BlockPos.ZERO), label + " top taper omits the centre log");
		int minTaperX = taperOffsets.stream().mapToInt(BlockPos::getX).min().orElseThrow();
		int maxTaperX = taperOffsets.stream().mapToInt(BlockPos::getX).max().orElseThrow();
		int minTaperZ = taperOffsets.stream().mapToInt(BlockPos::getZ).min().orElseThrow();
		int maxTaperZ = taperOffsets.stream().mapToInt(BlockPos::getZ).max().orElseThrow();
		helper.assertTrue(
			maxTaperX - minTaperX == 1 && maxTaperZ - minTaperZ == 1,
			label + " top taper is not a contiguous 2x2 footprint"
		);
		for (int y = taperStart; y < trunkTop; y++) {
			helper.assertValueEqual(
				verticalCoreLogOffsetsAt(helper, origin, y),
				taperOffsets,
				label + " exact vertical taper bole at y=" + y
			);
		}
		int top = occupiedTop(tree, origin);
		helper.assertTrue(top >= 29 && top <= 36, label + " occupied top " + top + " outside 29..36");
		helper.assertTrue(
			tree.logs().stream().anyMatch(pos -> pos.getY() == origin.getY() - 2),
			label + " has no chestnut root log at y=-2"
		);
		if (requireAllCardinalRootArms) {
			assertCardinalExtents(helper, tree, origin, -4, 4, -4, 4, label + " roots");
		} else {
			assertAtLeastCardinalRootArms(helper, tree, origin, 2, 2, label + " adaptive roots");
		}
		assertHighBranches(helper, tree, origin, -5, 5, -5, 5, label + " branches");
		assertAllLogsConnected(helper, tree, label);
		assertLogAxisContinuity(helper, tree, origin, label);
		assertGeneratedLeaves(helper, tree, label);
		assertCanopyDensity(helper, tree, origin, OLD_GROWTH_CANOPY, label);
	}

	private static void assertCanopyDensity(
		GameTestHelper helper,
		TreeSnapshot tree,
		BlockPos origin,
		CanopyContract contract,
		String label
	) {
		int leafCount = tree.leaves().size();
		int logCount = tree.logs().size();
		double leafLogRatio = logCount == 0 ? 0.0 : (double) leafCount / logCount;

		Map<Integer, Integer> leavesByY = new HashMap<>();
		Set<BlockPos> projectedColumns = new HashSet<>();
		int westLeaves = 0;
		int eastLeaves = 0;
		int northLeaves = 0;
		int southLeaves = 0;
		int crownCenterXTwice = origin.getX() * 2 + contract.centerOffsetTwice();
		int crownCenterZTwice = origin.getZ() * 2 + contract.centerOffsetTwice();
		for (BlockPos leaf : tree.leaves()) {
			leavesByY.merge(leaf.getY(), 1, Integer::sum);
			projectedColumns.add(new BlockPos(leaf.getX(), 0, leaf.getZ()));
			int leafXTwice = leaf.getX() * 2;
			int leafZTwice = leaf.getZ() * 2;
			if (leafXTwice < crownCenterXTwice) {
				westLeaves++;
			} else if (leafXTwice > crownCenterXTwice) {
				eastLeaves++;
			}
			if (leafZTwice < crownCenterZTwice) {
				northLeaves++;
			} else if (leafZTwice > crownCenterZTwice) {
				southLeaves++;
			}
		}

		int minimumLeafY = tree.leaves().stream().mapToInt(BlockPos::getY).min().orElseThrow();
		int maximumLeafY = tree.leaves().stream().mapToInt(BlockPos::getY).max().orElseThrow();
		int emptyLayerCount = 0;
		for (int y = minimumLeafY; y <= maximumLeafY; y++) {
			if (!leavesByY.containsKey(y)) {
				emptyLayerCount++;
			}
		}
		List<Integer> sortedLayerCounts = new ArrayList<>(leavesByY.values());
		sortedLayerCounts.sort(Integer::compareTo);
		int minimumLayerLeaves = sortedLayerCounts.getFirst();
		int lowerQuartileLayerLeaves = sortedLayerCounts.get(sortedLayerCounts.size() / 4);
		int medianLayerLeaves = sortedLayerCounts.get(sortedLayerCounts.size() / 2);
		int maximumLayerLeaves = leavesByY.values().stream().mapToInt(Integer::intValue).max().orElseThrow();
		double maximumLayerDominance = (double) maximumLayerLeaves / leafCount;

		double eastWestBalance = smallerSideShare(westLeaves, eastLeaves);
		double northSouthBalance = smallerSideShare(northLeaves, southLeaves);
		List<Integer> componentSizes = faceConnectedComponentSizes(tree.leaves());
		int largestComponent = componentSizes.stream().mapToInt(Integer::intValue).max().orElseThrow();
		double largestComponentShare = (double) largestComponent / leafCount;
		Set<BlockPos> forkTips = highHorizontalForkTips(helper, tree, origin, contract.minimumCrownBase());
		int minimumTipLeaves = forkTips.stream()
			.mapToInt(tip -> countLeavesInCube(tree, tip, 2, 2))
			.min()
			.orElse(0);

		BeforeTheBlight.LOGGER.info(String.format(
			Locale.ROOT,
			"BTB_CANOPY_DENSITY label=%s logs=%d leaves=%d leaf_log_ratio=%.3f leaf_y=%d..%d "
				+ "empty_layers=%d layer_min_q1_median_max=%d,%d,%d,%d max_layer_dominance=%.5f "
				+ "projection_columns=%d hemisphere_x_z=%.5f,%.5f leaf_components=%d "
				+ "largest_component_share=%.5f fork_tips=%d minimum_tip_cube2_leaves=%d",
			label.replace(' ', '_'),
			logCount,
			leafCount,
			leafLogRatio,
			minimumLeafY - origin.getY(),
			maximumLeafY - origin.getY(),
			emptyLayerCount,
			minimumLayerLeaves,
			lowerQuartileLayerLeaves,
			medianLayerLeaves,
			maximumLayerLeaves,
			maximumLayerDominance,
			projectedColumns.size(),
			eastWestBalance,
			northSouthBalance,
			componentSizes.size(),
			largestComponentShare,
			forkTips.size(),
			minimumTipLeaves
		));

		helper.assertTrue(
			leafCount >= contract.minimumLeaves(),
			label + " leaf count " + leafCount + " below " + contract.minimumLeaves()
		);
		helper.assertTrue(
			minimumLeafY - origin.getY() >= contract.minimumCrownBase(),
			label + " foliage begins below the upper crown at y=" + (minimumLeafY - origin.getY())
		);
		helper.assertValueEqual(emptyLayerCount, 0, label + " empty crown layer count");
		helper.assertTrue(
			lowerQuartileLayerLeaves >= contract.minimumLowerQuartileLayerLeaves(),
			label + " lower-quartile layer " + lowerQuartileLayerLeaves + " below "
				+ contract.minimumLowerQuartileLayerLeaves()
		);
		helper.assertTrue(
			medianLayerLeaves >= contract.minimumMedianLayerLeaves(),
			label + " median layer " + medianLayerLeaves + " below " + contract.minimumMedianLayerLeaves()
		);
		helper.assertTrue(
			maximumLayerDominance <= contract.maximumLayerDominance(),
			label + " densest layer share " + maximumLayerDominance + " above "
				+ contract.maximumLayerDominance()
		);
		helper.assertTrue(
			projectedColumns.size() >= contract.minimumProjectionColumns(),
			label + " projected leaf columns " + projectedColumns.size() + " below "
				+ contract.minimumProjectionColumns()
		);
		helper.assertTrue(
			eastWestBalance >= contract.minimumHemisphereBalance(),
			label + " east/west hemisphere balance " + eastWestBalance + " below "
				+ contract.minimumHemisphereBalance()
		);
		helper.assertTrue(
			northSouthBalance >= contract.minimumHemisphereBalance(),
			label + " north/south hemisphere balance " + northSouthBalance + " below "
				+ contract.minimumHemisphereBalance()
		);
		helper.assertTrue(
			componentSizes.size() <= contract.maximumLeafComponents(),
			label + " has " + componentSizes.size() + " leaf components; expected at most "
				+ contract.maximumLeafComponents()
		);
		helper.assertTrue(
			largestComponentShare >= contract.minimumLargestComponentShare(),
			label + " largest leaf-component share " + largestComponentShare + " below "
				+ contract.minimumLargestComponentShare()
		);
		helper.assertValueEqual(forkTips.size(), 8, label + " high fork-tip count");
		helper.assertTrue(
			minimumTipLeaves >= contract.minimumTipLeaves(),
			label + " sparsest fork-tip 5x5x5 neighborhood has " + minimumTipLeaves + " leaves; expected at least "
				+ contract.minimumTipLeaves()
		);
	}

	private static double smallerSideShare(int negativeSide, int positiveSide) {
		int total = negativeSide + positiveSide;
		return total == 0 ? 0.0 : (double) Math.min(negativeSide, positiveSide) / total;
	}

	private static List<Integer> faceConnectedComponentSizes(Set<BlockPos> positions) {
		Set<BlockPos> remaining = new HashSet<>(positions);
		List<Integer> componentSizes = new ArrayList<>();
		ArrayDeque<BlockPos> pending = new ArrayDeque<>();
		while (!remaining.isEmpty()) {
			BlockPos start = remaining.iterator().next();
			remaining.remove(start);
			pending.add(start);
			int size = 0;
			while (!pending.isEmpty()) {
				BlockPos current = pending.removeFirst();
				size++;
				for (Direction direction : Direction.values()) {
					BlockPos neighbor = current.relative(direction);
					if (remaining.remove(neighbor)) {
						pending.addLast(neighbor);
					}
				}
			}
			componentSizes.add(size);
		}
		return List.copyOf(componentSizes);
	}

	private static Set<BlockPos> highHorizontalForkTips(
		GameTestHelper helper,
		TreeSnapshot tree,
		BlockPos origin,
		int minimumCrownBase
	) {
		Set<BlockPos> forkTips = new HashSet<>();
		for (BlockPos log : tree.logs()) {
			Direction.Axis tipAxis = helper.getBlockState(log).getValue(RotatedPillarBlock.AXIS);
			if (log.getY() - origin.getY() < minimumCrownBase || tipAxis == Direction.Axis.Y) {
				continue;
			}
			BlockPos onlyNeighbor = null;
			int logNeighbors = 0;
			for (Direction direction : Direction.values()) {
				BlockPos neighbor = log.relative(direction);
				if (tree.logs().contains(neighbor)) {
					onlyNeighbor = neighbor;
					logNeighbors++;
				}
			}
			if (logNeighbors == 1
				&& helper.getBlockState(onlyNeighbor).getValue(RotatedPillarBlock.AXIS) != tipAxis) {
				forkTips.add(log);
			}
		}
		return Set.copyOf(forkTips);
	}

	private static int countLeavesInCube(TreeSnapshot tree, BlockPos center, int horizontalRadius, int verticalRadius) {
		int count = 0;
		for (BlockPos leaf : tree.leaves()) {
			if (Math.abs(leaf.getX() - center.getX()) <= horizontalRadius
				&& Math.abs(leaf.getY() - center.getY()) <= verticalRadius
				&& Math.abs(leaf.getZ() - center.getZ()) <= horizontalRadius) {
				count++;
			}
		}
		return count;
	}

	private static void assertCardinalExtents(
		GameTestHelper helper,
		TreeSnapshot tree,
		BlockPos origin,
		int minX,
		int maxX,
		int minZ,
		int maxZ,
		String label
	) {
		Set<BlockPos> roots = new HashSet<>();
		for (BlockPos pos : tree.logs()) {
			if (pos.getY() < origin.getY()) {
				roots.add(pos);
			}
		}
		helper.assertFalse(roots.isEmpty(), label + " has no underground root logs");
		int actualMinX = roots.stream().mapToInt(pos -> pos.getX() - origin.getX()).min().orElseThrow();
		int actualMaxX = roots.stream().mapToInt(pos -> pos.getX() - origin.getX()).max().orElseThrow();
		int actualMinZ = roots.stream().mapToInt(pos -> pos.getZ() - origin.getZ()).min().orElseThrow();
		int actualMaxZ = roots.stream().mapToInt(pos -> pos.getZ() - origin.getZ()).max().orElseThrow();
		helper.assertTrue(actualMinX <= minX, label + " west extent " + actualMinX + " did not reach " + minX);
		helper.assertTrue(actualMaxX >= maxX, label + " east extent " + actualMaxX + " did not reach " + maxX);
		helper.assertTrue(actualMinZ <= minZ, label + " north extent " + actualMinZ + " did not reach " + minZ);
		helper.assertTrue(actualMaxZ >= maxZ, label + " south extent " + actualMaxZ + " did not reach " + maxZ);
	}

	private static void assertHighBranches(
		GameTestHelper helper,
		TreeSnapshot tree,
		BlockPos origin,
		int minX,
		int maxX,
		int minZ,
		int maxZ,
		String label
	) {
		Set<BlockPos> horizontal = new HashSet<>();
		boolean xAxis = false;
		boolean zAxis = false;
		for (BlockPos pos : tree.logs()) {
			int relativeY = pos.getY() - origin.getY();
			Direction.Axis axis = helper.getBlockState(pos).getValue(RotatedPillarBlock.AXIS);
			if (relativeY >= 1 && relativeY < 15) {
				helper.assertValueEqual(axis, Direction.Axis.Y, label + " low log axis at " + pos);
			}
			if (relativeY < 15) {
				continue;
			}
			if (axis != Direction.Axis.Y) {
				horizontal.add(pos);
				xAxis |= axis == Direction.Axis.X;
				zAxis |= axis == Direction.Axis.Z;
			}
		}
		helper.assertTrue(xAxis && zAxis, label + " lacks both X- and Z-axis high branch logs");
		helper.assertTrue(
			horizontal.stream().anyMatch(pos -> pos.getX() - origin.getX() <= minX),
			label + " lacks westward high branch reach"
		);
		helper.assertTrue(
			horizontal.stream().anyMatch(pos -> pos.getX() - origin.getX() >= maxX),
			label + " lacks eastward high branch reach"
		);
		helper.assertTrue(
			horizontal.stream().anyMatch(pos -> pos.getZ() - origin.getZ() <= minZ),
			label + " lacks northward high branch reach"
		);
		helper.assertTrue(
			horizontal.stream().anyMatch(pos -> pos.getZ() - origin.getZ() >= maxZ),
			label + " lacks southward high branch reach"
		);
	}

	private static void assertAtLeastCardinalRootArms(
		GameTestHelper helper,
		TreeSnapshot tree,
		BlockPos origin,
		int minimumReach,
		int minimumArms,
		String label
	) {
		int arms = 0;
		for (Direction direction : CARDINAL_DIRECTIONS) {
			boolean reaches = tree.logs().stream()
				.filter(pos -> pos.getY() < origin.getY())
				.anyMatch(pos -> {
					int dx = pos.getX() - origin.getX();
					int dz = pos.getZ() - origin.getZ();
					int projection = dx * direction.getStepX() + dz * direction.getStepZ();
					int perpendicular = direction.getAxis() == Direction.Axis.X ? Math.abs(dz) : Math.abs(dx);
					return projection >= minimumReach && perpendicular <= 1;
				});
			if (reaches) {
				arms++;
			}
		}
		helper.assertTrue(
			arms >= minimumArms,
			label + " has " + arms + " cardinal root arms reaching " + minimumReach
				+ " blocks; expected at least " + minimumArms
		);
	}

	private static void assertAtLeastTerrainFollowingCardinalRootArms(
		GameTestHelper helper,
		TreeSnapshot tree,
		BlockPos origin,
		int minimumReach,
		int minimumArms,
		String label
	) {
		int arms = 0;
		for (Direction direction : CARDINAL_DIRECTIONS) {
			int maximumProjection = 0;
			Set<Integer> followedSurfaceLevels = new HashSet<>();
			for (BlockPos pos : tree.logs()) {
				int dx = pos.getX() - origin.getX();
				int dz = pos.getZ() - origin.getZ();
				int projection = dx * direction.getStepX() + dz * direction.getStepZ();
				int perpendicular = direction.getAxis() == Direction.Axis.X ? Math.abs(dz) : Math.abs(dx);
				BlockState state = helper.getBlockState(pos);
				if (projection < 2
					|| perpendicular > 1
					|| !state.hasProperty(RotatedPillarBlock.AXIS)
					|| state.getValue(RotatedPillarBlock.AXIS) != direction.getAxis()
					|| pos.getY() != steppedSurfaceY(origin, pos.getX(), pos.getZ())) {
					continue;
				}
				maximumProjection = Math.max(maximumProjection, projection);
				followedSurfaceLevels.add(pos.getY());
			}
			if (maximumProjection >= minimumReach && followedSurfaceLevels.size() >= 2) {
				arms++;
			}
		}
		helper.assertTrue(
			arms >= minimumArms,
			label + " has " + arms + " cardinal arms following at least two stepped surface levels to reach "
				+ minimumReach + "; expected at least " + minimumArms
		);
	}

	private static void assertDownhillFoundationContinuity(
		GameTestHelper helper,
		BlockPos origin,
		int minDx,
		int maxDx,
		int minDz,
		int maxDz,
		String label
	) {
		Set<Integer> surfaceLevels = new HashSet<>();
		for (int dx = minDx; dx <= maxDx; dx++) {
			for (int dz = minDz; dz <= maxDz; dz++) {
				int surfaceY = steppedSurfaceY(origin, origin.getX() + dx, origin.getZ() + dz);
				surfaceLevels.add(surfaceY);
				for (int y = surfaceY; y < origin.getY(); y++) {
					assertVerticalChestnutLog(
						helper,
						new BlockPos(origin.getX() + dx, y, origin.getZ() + dz),
						label + " continuous column"
					);
				}
			}
		}
		helper.assertTrue(
			surfaceLevels.size() >= 3,
			label + " did not span the three pinned downhill terrain levels"
		);
	}

	private static void assertAllLogsConnected(GameTestHelper helper, TreeSnapshot tree, String label) {
		Set<BlockPos> remaining = new HashSet<>(tree.logs());
		helper.assertFalse(remaining.isEmpty(), label + " has no logs to connect");
		ArrayDeque<BlockPos> pending = new ArrayDeque<>();
		BlockPos start = remaining.iterator().next();
		remaining.remove(start);
		pending.add(start);
		while (!pending.isEmpty()) {
			BlockPos current = pending.removeFirst();
			for (Direction direction : Direction.values()) {
				BlockPos neighbor = current.relative(direction);
				if (remaining.remove(neighbor)) {
					pending.addLast(neighbor);
				}
			}
		}
		helper.assertTrue(remaining.isEmpty(), label + " has " + remaining.size() + " non-face-connected logs");
	}

	private static void assertAllLogsTouchConnected(GameTestHelper helper, TreeSnapshot tree, String label) {
		Set<BlockPos> remaining = new HashSet<>(tree.logs());
		helper.assertFalse(remaining.isEmpty(), label + " has no logs to connect");
		ArrayDeque<BlockPos> pending = new ArrayDeque<>();
		BlockPos start = remaining.iterator().next();
		remaining.remove(start);
		pending.add(start);
		while (!pending.isEmpty()) {
			BlockPos current = pending.removeFirst();
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						if (dx == 0 && dy == 0 && dz == 0) {
							continue;
						}
						BlockPos neighbor = current.offset(dx, dy, dz);
						if (remaining.remove(neighbor)) {
							pending.addLast(neighbor);
						}
					}
				}
			}
		}
		helper.assertTrue(remaining.isEmpty(), label + " has " + remaining.size() + " non-touch-connected logs");
	}

	private static void assertLogAxes(GameTestHelper helper, TreeSnapshot tree, String label) {
		for (BlockPos pos : tree.logs()) {
			BlockState state = helper.getBlockState(pos);
			helper.assertTrue(state.hasProperty(RotatedPillarBlock.AXIS), label + " log lacks AXIS at " + pos);
			Direction.Axis axis = state.getValue(RotatedPillarBlock.AXIS);
			helper.assertTrue(
				axis == Direction.Axis.X || axis == Direction.Axis.Y || axis == Direction.Axis.Z,
				label + " log has invalid axis at " + pos
			);
		}
	}

	private static void assertLogAxisContinuity(
		GameTestHelper helper,
		TreeSnapshot tree,
		BlockPos origin,
		String label
	) {
		assertLogAxes(helper, tree, label);
		for (BlockPos pos : tree.logs()) {
			Direction.Axis axis = helper.getBlockState(pos).getValue(RotatedPillarBlock.AXIS);
			boolean alignedNeighbor = switch (axis) {
				case X -> tree.logs().contains(pos.east()) || tree.logs().contains(pos.west());
				case Y -> tree.logs().contains(pos.above()) || tree.logs().contains(pos.below());
				case Z -> tree.logs().contains(pos.north()) || tree.logs().contains(pos.south());
			};
			boolean hollowFloorCenter = axis == Direction.Axis.Y
				&& pos.equals(origin.below());
			helper.assertTrue(
				alignedNeighbor || hollowFloorCenter,
				label + " log axis has no aligned neighbor at " + pos
			);
		}
	}

	private static void assertCrownSpread(
		GameTestHelper helper,
		TreeSnapshot tree,
		BlockPos origin,
		int minimumReach,
		String label
	) {
		helper.assertTrue(
			tree.leaves().stream().anyMatch(pos -> pos.getX() - origin.getX() <= -minimumReach),
			label + " crown lacks westward spread"
		);
		helper.assertTrue(
			tree.leaves().stream().anyMatch(pos -> pos.getX() - origin.getX() >= minimumReach),
			label + " crown lacks eastward spread"
		);
		helper.assertTrue(
			tree.leaves().stream().anyMatch(pos -> pos.getZ() - origin.getZ() <= -minimumReach),
			label + " crown lacks northward spread"
		);
		helper.assertTrue(
			tree.leaves().stream().anyMatch(pos -> pos.getZ() - origin.getZ() >= minimumReach),
			label + " crown lacks southward spread"
		);
	}

	private static Set<BlockPos> placeForestTerminalObstructions(GameTestHelper helper, BlockPos origin) {
		Set<BlockPos> obstacles = new HashSet<>();
		for (int lane = 0; lane <= 1; lane++) {
			for (int reach = 3; reach <= 4; reach++) {
				obstacles.add(origin.offset(lane, -2, -reach));
				obstacles.add(origin.offset(lane, -2, 1 + reach));
				obstacles.add(origin.offset(-reach, -2, lane));
				obstacles.add(origin.offset(1 + reach, -2, lane));
			}
		}
		for (BlockPos pos : obstacles) {
			helper.setBlock(pos, Blocks.BEDROCK);
		}
		return Set.copyOf(obstacles);
	}

	private static Map<BlockPos, BlockState> snapshotRollbackVolume(
		GameTestHelper helper,
		BlockPos origin
	) {
		Map<BlockPos, BlockState> states = new LinkedHashMap<>();
		for (BlockPos mutable : BlockPos.betweenClosed(
			origin.offset(-LARGE_SCAN_RADIUS, -3, -LARGE_SCAN_RADIUS),
			origin.offset(LARGE_SCAN_RADIUS, LARGE_SCAN_HEIGHT, LARGE_SCAN_RADIUS)
		)) {
			BlockPos pos = mutable.immutable();
			states.put(pos, helper.getBlockState(pos));
		}
		return Map.copyOf(states);
	}

	private static Map<BlockPos, BlockState> snapshotNaturalTreeVolume(
		GameTestHelper helper,
		BlockPos origin
	) {
		Map<BlockPos, BlockState> states = new LinkedHashMap<>();
		for (BlockPos mutable : BlockPos.betweenClosed(
			origin.offset(-LARGE_SCAN_RADIUS, -6, -LARGE_SCAN_RADIUS),
			origin.offset(LARGE_SCAN_RADIUS, LARGE_SCAN_HEIGHT, LARGE_SCAN_RADIUS)
		)) {
			BlockPos pos = mutable.immutable();
			states.put(pos, helper.getBlockState(pos));
		}
		return Map.copyOf(states);
	}

	private static Map<BlockPos, BlockState> snapshotSquareStates(
		GameTestHelper helper,
		BlockPos northWest,
		int width
	) {
		Map<BlockPos, BlockState> states = new LinkedHashMap<>();
		for (int dx = 0; dx < width; dx++) {
			for (int dz = 0; dz < width; dz++) {
				BlockPos pos = northWest.offset(dx, 0, dz);
				states.put(pos, helper.getBlockState(pos));
			}
		}
		return Map.copyOf(states);
	}

	private static void assertRollbackVolumeUnchanged(
		GameTestHelper helper,
		Map<BlockPos, BlockState> expected,
		String label
	) {
		for (Map.Entry<BlockPos, BlockState> entry : expected.entrySet()) {
			BlockState actual = helper.getBlockState(entry.getKey());
			helper.assertValueEqual(actual, entry.getValue(), label + " collateral state at " + entry.getKey());
		}
	}

	private static void assertGeneratedLeaves(GameTestHelper helper, TreeSnapshot tree, String label) {
		helper.assertFalse(tree.leaves().isEmpty(), label + " generated no leaves");
		for (BlockPos log : tree.logs()) {
			helper.assertTrue(
				helper.getBlockState(log).is(BlockTags.PREVENTS_NEARBY_LEAF_DECAY),
				label + " chestnut log is missing the leaf-support tag at " + log
			);
		}
		Map<BlockPos, Integer> distance = leafSupportDistances(tree);
		for (BlockPos leaf : tree.leaves()) {
			BlockState state = helper.getBlockState(leaf);
			helper.assertFalse(state.getValue(LeavesBlock.PERSISTENT), label + " generated persistent leaf at " + leaf);
			int actual = state.getValue(LeavesBlock.DISTANCE);
			helper.assertTrue(
				actual >= 1 && actual < LeavesBlock.DECAY_DISTANCE,
				label + " generated unsupported distance-" + actual + " leaf at " + leaf
			);
			Integer expected = distance.get(leaf);
			helper.assertTrue(expected != null, label + " leaf has no <=6 face path to a chestnut log at " + leaf);
			if (expected != null) {
				helper.assertTrue(
					actual == expected || actual == expected + 1,
					label + " placement-time distance " + actual
						+ " is not shortest-path " + expected + " or its one-tick conservative state at " + leaf
				);
			}
		}
	}

	private static void settleAndAssertGeneratedLeafDistances(
		GameTestHelper helper,
		TreeSnapshot tree,
		String label
	) {
		Map<BlockPos, Integer> distance = leafSupportDistances(tree);
		tree.leaves().stream()
			.sorted(Comparator.comparingInt(distance::get))
			.forEach(helper::tickBlock);
		for (BlockPos leaf : tree.leaves()) {
			helper.assertValueEqual(
				helper.getBlockState(leaf).getValue(LeavesBlock.DISTANCE),
				distance.get(leaf),
				label + " settled shortest-path distance at " + leaf
			);
		}
	}

	private static Map<BlockPos, Integer> leafSupportDistances(TreeSnapshot tree) {
		Map<BlockPos, Integer> distance = new HashMap<>();
		ArrayDeque<BlockPos> pending = new ArrayDeque<>();
		for (BlockPos log : tree.logs()) {
			distance.put(log, 0);
			pending.add(log);
		}
		while (!pending.isEmpty()) {
			BlockPos current = pending.removeFirst();
			int nextDistance = distance.get(current) + 1;
			if (nextDistance >= LeavesBlock.DECAY_DISTANCE) {
				continue;
			}
			for (Direction direction : Direction.values()) {
				BlockPos neighbor = current.relative(direction);
				if (tree.leaves().contains(neighbor) && !distance.containsKey(neighbor)) {
					distance.put(neighbor, nextDistance);
					pending.addLast(neighbor);
				}
			}
		}
		return Map.copyOf(distance);
	}

	private static void assertNoGeneratedTree(GameTestHelper helper, BlockPos origin, String label) {
		TreeSnapshot tree = scanEntireArena(helper);
		helper.assertValueEqual(tree.logs().size(), 0, label + " leftover chestnut log count");
		helper.assertValueEqual(tree.leaves().size(), 0, label + " leftover chestnut leaf count");
	}

	private static void assertNoSaplings(GameTestHelper helper, BlockPos northWest, int width, String label) {
		for (int dx = 0; dx < width; dx++) {
			for (int dz = 0; dz < width; dz++) {
				helper.assertFalse(
					helper.getBlockState(northWest.offset(dx, 0, dz)).is(ModBlocks.CHESTNUT_SAPLING),
					label + " left a sapling at " + northWest.offset(dx, 0, dz)
				);
			}
		}
	}

	private static void assertChestnutLog(GameTestHelper helper, BlockPos pos, String label) {
		helper.assertTrue(helper.getBlockState(pos).is(ModBlocks.CHESTNUT_LOG), label + " missing log at " + pos);
	}

	private static Set<BlockPos> coreLogOffsetsAt(GameTestHelper helper, BlockPos origin, int relativeY) {
		Set<BlockPos> offsets = new HashSet<>();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				BlockPos pos = origin.offset(dx, relativeY, dz);
				if (helper.getBlockState(pos).is(ModBlocks.CHESTNUT_LOG)) {
					offsets.add(new BlockPos(dx, 0, dz));
				}
			}
		}
		return Set.copyOf(offsets);
	}

	private static Set<BlockPos> verticalCoreLogOffsetsAt(
		GameTestHelper helper,
		BlockPos origin,
		int relativeY
	) {
		Set<BlockPos> offsets = new HashSet<>();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				BlockState state = helper.getBlockState(origin.offset(dx, relativeY, dz));
				if (state.is(ModBlocks.CHESTNUT_LOG)
					&& state.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y) {
					offsets.add(new BlockPos(dx, 0, dz));
				}
			}
		}
		return Set.copyOf(offsets);
	}

	private static long countLogsAtY(TreeSnapshot tree, int absoluteY) {
		return tree.logs().stream().filter(pos -> pos.getY() == absoluteY).count();
	}

	private static void assertVerticalChestnutLog(GameTestHelper helper, BlockPos pos, String label) {
		assertChestnutLog(helper, pos, label);
		helper.assertValueEqual(
			helper.getBlockState(pos).getValue(RotatedPillarBlock.AXIS),
			Direction.Axis.Y,
			label + " axis at " + pos
		);
	}

	private static int maximumCoreLogY(
		TreeSnapshot tree,
		BlockPos origin,
		int minDx,
		int maxDx,
		int minDz,
		int maxDz
	) {
		return tree.logs().stream()
			.filter(pos -> pos.getX() - origin.getX() >= minDx && pos.getX() - origin.getX() <= maxDx)
			.filter(pos -> pos.getZ() - origin.getZ() >= minDz && pos.getZ() - origin.getZ() <= maxDz)
			.mapToInt(BlockPos::getY)
			.max()
			.orElseThrow();
	}

	private static int occupiedTop(TreeSnapshot tree, BlockPos origin) {
		return tree.allBlocks().stream().mapToInt(BlockPos::getY).max().orElseThrow() - origin.getY();
	}

	private static void prepareSoil(GameTestHelper helper, BlockPos origin, int radius) {
		for (int x = origin.getX() - radius; x <= origin.getX() + radius; x++) {
			for (int z = origin.getZ() - radius; z <= origin.getZ() + radius; z++) {
				for (int y = origin.getY() - 3; y < origin.getY(); y++) {
					helper.setBlock(new BlockPos(x, y, z), Blocks.DIRT);
				}
			}
		}
	}

	private static void prepareSteppedRidgeSoil(GameTestHelper helper, BlockPos origin, int radius) {
		for (int x = origin.getX() - radius; x <= origin.getX() + radius; x++) {
			for (int z = origin.getZ() - radius; z <= origin.getZ() + radius; z++) {
				int surfaceY = steppedSurfaceY(origin, x, z);
				for (int y = 0; y <= surfaceY; y++) {
					helper.setBlock(new BlockPos(x, y, z), Blocks.DIRT);
				}
				for (int y = surfaceY + 1; y <= origin.getY() + 3; y++) {
					helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
				}
			}
		}
	}

	private static int steppedSurfaceY(BlockPos origin, int x, int z) {
		int eastwardSteps = Math.max(0, Math.floorDiv(x - origin.getX() + 1, 2));
		int southwardSteps = Math.max(0, Math.floorDiv(z - origin.getZ() + 1, 2));
		int downhillSteps = Math.min(3, eastwardSteps + southwardSteps);
		return origin.getY() - 1 - downhillSteps;
	}

	private static Map<BlockPos, BlockState> placeNaturalForestRootPathSentinels(
		GameTestHelper helper,
		BlockPos origin
	) {
		Map<BlockPos, BlockState> sentinels = new LinkedHashMap<>();
		// Both north lanes begin here, so these are real planned root collisions rather than diagonal bystanders.
		addNaturalTreeSentinel(helper, sentinels, origin, 0, -1, Blocks.STONE.defaultBlockState());
		addNaturalTreeSentinel(helper, sentinels, origin, 1, -1, Blocks.OAK_LOG.defaultBlockState());
		return Map.copyOf(sentinels);
	}

	private static Map<BlockPos, BlockState> placeNaturalOldGrowthRootPathSentinels(
		GameTestHelper helper,
		BlockPos origin
	) {
		Map<BlockPos, BlockState> sentinels = new LinkedHashMap<>();
		// Old-growth arms start at radius two and may choose side offset -1, 0, or 1 before retrying at zero.
		addNaturalTreeSentinel(helper, sentinels, origin, -1, -2, Blocks.STONE.defaultBlockState());
		addNaturalTreeSentinel(helper, sentinels, origin, 0, -2, Blocks.BEDROCK.defaultBlockState());
		addNaturalTreeSentinel(helper, sentinels, origin, 1, -2, Blocks.OAK_LOG.defaultBlockState());
		return Map.copyOf(sentinels);
	}

	private static void addNaturalTreeSentinel(
		GameTestHelper helper,
		Map<BlockPos, BlockState> sentinels,
		BlockPos origin,
		int dx,
		int dz,
		BlockState state
	) {
		BlockPos pos = new BlockPos(
			origin.getX() + dx,
			steppedSurfaceY(origin, origin.getX() + dx, origin.getZ() + dz),
			origin.getZ() + dz
		);
		sentinels.put(pos, state);
		helper.setBlock(pos, state);
	}

	private static void assertNaturalTreeSentinelsPreserved(
		GameTestHelper helper,
		Map<BlockPos, BlockState> sentinels,
		String label
	) {
		for (Map.Entry<BlockPos, BlockState> sentinel : sentinels.entrySet()) {
			helper.assertValueEqual(
				helper.getBlockState(sentinel.getKey()),
				sentinel.getValue(),
				label + " sentinel at " + sentinel.getKey()
			);
		}
	}

	private static void blockThreeForestRootDirections(GameTestHelper helper, BlockPos origin) {
		int[][] columns = {
			{0, -1},
			{1, -1},
			{2, 0},
			{2, 1},
			{0, 2},
			{1, 2}
		};
		for (int[] column : columns) {
			int x = origin.getX() + column[0];
			int z = origin.getZ() + column[1];
			helper.setBlock(
				new BlockPos(x, steppedSurfaceY(origin, x, z), z),
				Blocks.BEDROCK
			);
		}
	}

	private static void blockThreeOldGrowthRootDirections(GameTestHelper helper, BlockPos origin) {
		for (Direction direction : List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH)) {
			Direction side = direction.getClockWise();
			for (int sideOffset = -1; sideOffset <= 1; sideOffset++) {
				int dx = direction.getStepX() * 2 + side.getStepX() * sideOffset;
				int dz = direction.getStepZ() * 2 + side.getStepZ() * sideOffset;
				int x = origin.getX() + dx;
				int z = origin.getZ() + dz;
				helper.setBlock(
					new BlockPos(x, steppedSurfaceY(origin, x, z), z),
					Blocks.BEDROCK
				);
			}
		}
	}

	private static void setSurfaceSoilSquare(
		GameTestHelper helper,
		BlockPos northWest,
		int width,
		BlockState soil
	) {
		for (int dx = 0; dx < width; dx++) {
			for (int dz = 0; dz < width; dz++) {
				helper.setBlock(northWest.offset(dx, -1, dz), soil);
			}
		}
	}

	private static void placeReadySapling(GameTestHelper helper, BlockPos pos) {
		helper.setBlock(pos, readySapling());
	}

	private static void placeReadySquare(GameTestHelper helper, BlockPos northWest, int width) {
		for (int dx = 0; dx < width; dx++) {
			for (int dz = 0; dz < width; dz++) {
				placeReadySapling(helper, northWest.offset(dx, 0, dz));
			}
		}
	}

	private static void placeSaplingSquareAtStage(
		GameTestHelper helper,
		BlockPos northWest,
		int width,
		int stage
	) {
		BlockState state = ModBlocks.CHESTNUT_SAPLING.defaultBlockState().setValue(SaplingBlock.STAGE, stage);
		for (int dx = 0; dx < width; dx++) {
			for (int dz = 0; dz < width; dz++) {
				helper.setBlock(northWest.offset(dx, 0, dz), state);
			}
		}
	}

	private static void placeMixedStageThreeByThree(GameTestHelper helper, BlockPos northWest) {
		for (int dx = 0; dx < 3; dx++) {
			for (int dz = 0; dz < 3; dz++) {
				BlockPos pos = northWest.offset(dx, 0, dz);
				int stage = pos.equals(TREE_ORIGIN) ? 1 : (dx + dz) & 1;
				helper.setBlock(
					pos,
					ModBlocks.CHESTNUT_SAPLING.defaultBlockState().setValue(SaplingBlock.STAGE, stage)
				);
			}
		}
	}

	private static BlockState readySapling() {
		return ModBlocks.CHESTNUT_SAPLING.defaultBlockState().setValue(SaplingBlock.STAGE, 1);
	}

	private static void advanceTree(GameTestHelper helper, BlockPos relativePos, long seed) {
		ServerLevel level = helper.getLevel();
		BlockPos absolutePos = helper.absolutePos(relativePos);
		BlockState state = level.getBlockState(absolutePos);
		SeasonGameTestSupport.advanceSaplingForGeometry(
			(ChestnutSaplingBlock) ModBlocks.CHESTNUT_SAPLING,
			level,
			absolutePos,
			state,
			RandomSource.create(seed)
		);
	}

	private static boolean placeConfiguredTree(
		GameTestHelper helper,
		ResourceKey<ConfiguredFeature<?, ?>> key,
		BlockPos relativeOrigin,
		long seed
	) {
		ServerLevel level = helper.getLevel();
		ConfiguredFeature<?, ?> feature = level.registryAccess()
			.lookupOrThrow(Registries.CONFIGURED_FEATURE)
			.getOrThrow(key)
			.value();
		return feature.place(
			level,
			level.getChunkSource().getGenerator(),
			RandomSource.create(seed),
			helper.absolutePos(relativeOrigin)
		);
	}

	private static int useBoneMealUntilStageChanges(
		GameTestHelper helper,
		ServerLevel level,
		BlockPos relativePos,
		BlockPos absolutePos,
		ItemStack boneMeal,
		int stage,
		String label
	) {
		int attempts = 0;
		while (attempts < MAX_BONEMEAL_ATTEMPTS_PER_STAGE) {
			BlockState state = level.getBlockState(absolutePos);
			if (!state.is(ModBlocks.CHESTNUT_SAPLING) || state.getValue(SaplingBlock.STAGE) != stage) {
				break;
			}
			assertBoneMealItemUse(helper, level, absolutePos, boneMeal, label);
			attempts++;
		}
		BlockState result = helper.getBlockState(relativePos);
		helper.assertTrue(attempts > 0, label + " performed no bone-meal item uses");
		helper.assertTrue(result.is(ModBlocks.CHESTNUT_SAPLING), label + " unexpectedly replaced its sapling");
		helper.assertTrue(
			result.getValue(SaplingBlock.STAGE) != stage,
			label + " remained at stage " + stage + " after " + attempts + " item uses"
		);
		return attempts;
	}

	private static int useBoneMealUntilSaplingIsReplaced(
		GameTestHelper helper,
		ServerLevel level,
		BlockPos relativePos,
		BlockPos absolutePos,
		ItemStack boneMeal,
		String label
	) {
		int attempts = 0;
		while (
			attempts < MAX_BONEMEAL_ATTEMPTS_PER_STAGE
				&& level.getBlockState(absolutePos).is(ModBlocks.CHESTNUT_SAPLING)
		) {
			assertBoneMealItemUse(helper, level, absolutePos, boneMeal, label);
			attempts++;
		}
		helper.assertTrue(attempts > 0, label + " performed no bone-meal item uses");
		helper.assertFalse(
			helper.getBlockState(relativePos).is(ModBlocks.CHESTNUT_SAPLING),
			label + " remained a sapling after " + attempts + " item uses"
		);
		return attempts;
	}

	private static void assertBoneMealItemUse(
		GameTestHelper helper,
		ServerLevel level,
		BlockPos absolutePos,
		ItemStack boneMeal,
		String label
	) {
		int countBefore = boneMeal.getCount();
		helper.assertTrue(countBefore > 0, label + " exhausted the bone-meal test stack");
		helper.assertTrue(
			BoneMealItem.growCrop(boneMeal, level, absolutePos),
			label + " was rejected by BoneMealItem.growCrop"
		);
		helper.assertValueEqual(
			boneMeal.getCount(),
			countBefore - 1,
			label + " bone-meal stack consumption"
		);
	}

	private static TreeSnapshot scanTree(
		GameTestHelper helper,
		BlockPos origin,
		int horizontalRadius,
		int heightAbove
	) {
		Set<BlockPos> logs = new HashSet<>();
		Set<BlockPos> leaves = new HashSet<>();
		for (BlockPos mutable : BlockPos.betweenClosed(
			origin.offset(-horizontalRadius, -2, -horizontalRadius),
			origin.offset(horizontalRadius, heightAbove, horizontalRadius)
		)) {
			BlockPos pos = mutable.immutable();
			BlockState state = helper.getBlockState(pos);
			if (state.is(ModBlocks.CHESTNUT_LOG)) {
				logs.add(pos);
			} else if (state.is(ModBlocks.CHESTNUT_LEAVES)) {
				leaves.add(pos);
			}
		}
		return new TreeSnapshot(Set.copyOf(logs), Set.copyOf(leaves));
	}

	private static TreeSnapshot scanNaturalTree(GameTestHelper helper, BlockPos origin) {
		Set<BlockPos> logs = new HashSet<>();
		Set<BlockPos> leaves = new HashSet<>();
		for (BlockPos mutable : BlockPos.betweenClosed(
			origin.offset(-LARGE_SCAN_RADIUS, -6, -LARGE_SCAN_RADIUS),
			origin.offset(LARGE_SCAN_RADIUS, LARGE_SCAN_HEIGHT, LARGE_SCAN_RADIUS)
		)) {
			BlockPos pos = mutable.immutable();
			BlockState state = helper.getBlockState(pos);
			if (state.is(ModBlocks.CHESTNUT_LOG)) {
				logs.add(pos);
			} else if (state.is(ModBlocks.CHESTNUT_LEAVES)) {
				leaves.add(pos);
			}
		}
		return new TreeSnapshot(Set.copyOf(logs), Set.copyOf(leaves));
	}

	private static TreeSnapshot scanEntireArena(GameTestHelper helper) {
		Set<BlockPos> logs = new HashSet<>();
		Set<BlockPos> leaves = new HashSet<>();
		for (BlockPos mutable : BlockPos.betweenClosed(
			BlockPos.ZERO,
			new BlockPos(TREE_ARENA_SIZE - 1, TREE_ARENA_SIZE - 1, TREE_ARENA_SIZE - 1)
		)) {
			BlockPos pos = mutable.immutable();
			BlockState state = helper.getBlockState(pos);
			if (state.is(ModBlocks.CHESTNUT_LOG)) {
				logs.add(pos);
			} else if (state.is(ModBlocks.CHESTNUT_LEAVES)) {
				leaves.add(pos);
			}
		}
		return new TreeSnapshot(Set.copyOf(logs), Set.copyOf(leaves));
	}

	private static void assertNoTreeOutsideEnvelopes(
		GameTestHelper helper,
		String label,
		TreeEnvelope... envelopes
	) {
		for (BlockPos pos : scanEntireArena(helper).allBlocks()) {
			boolean contained = false;
			for (TreeEnvelope envelope : envelopes) {
				contained |= envelope.contains(pos);
			}
			helper.assertTrue(contained, label + " placed a chestnut tree block outside its bounded envelope at " + pos);
		}
	}

	private static void printProbe(
		String form,
		long seed,
		BlockPos origin,
		TreeSnapshot tree
	) {
		BeforeTheBlight.LOGGER.info(String.format(
			Locale.ROOT,
			"BTB_TREE_PROBE form=%s seed=%d logs=%d leaves=%d occupied_top=%d%n",
			form,
			seed,
			tree.logs().size(),
			tree.leaves().size(),
			occupiedTop(tree, origin)
		).stripTrailing());
	}

	private record CanopyContract(
		int minimumLeaves,
		int minimumCrownBase,
		int minimumLowerQuartileLayerLeaves,
		int minimumMedianLayerLeaves,
		double maximumLayerDominance,
		int minimumProjectionColumns,
		int minimumTipLeaves,
		int centerOffsetTwice,
		double minimumHemisphereBalance,
		int maximumLeafComponents,
		double minimumLargestComponentShare
	) {
	}

	private record TreeSnapshot(Set<BlockPos> logs, Set<BlockPos> leaves) {
		private Set<BlockPos> allBlocks() {
			Set<BlockPos> all = new HashSet<>(logs);
			all.addAll(leaves);
			return all;
		}
	}

	private record TreeEnvelope(BlockPos origin, int horizontalRadius, int depthBelow, int heightAbove) {
		private boolean contains(BlockPos pos) {
			return Math.abs(pos.getX() - origin.getX()) <= horizontalRadius
				&& Math.abs(pos.getZ() - origin.getZ()) <= horizontalRadius
				&& pos.getY() >= origin.getY() - depthBelow
				&& pos.getY() <= origin.getY() + heightAbove;
		}
	}
}
