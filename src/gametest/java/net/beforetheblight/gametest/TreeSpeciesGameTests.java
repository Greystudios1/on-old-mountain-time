package net.beforetheblight.gametest;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

/**
 * Runtime growth contracts for the non-chestnut tree species.
 *
 * <p>The legacy configured-feature key named {@code beech} is intentionally
 * exercised through the public American beech sapling. These tests therefore
 * fail if any player-growth route silently falls back to a vanilla
 * spruce/birch/dark-oak proxy.</p>
 */
public final class TreeSpeciesGameTests {
	private static final String TREE_ARENA = "before_the_blight_gametest:chestnut_tree_48";
	private static final String OLD_GROWTH_ARENA = "before_the_blight_gametest:hemlock_tree_64";
	private static final BlockPos TREE_ORIGIN = new BlockPos(24, 4, 24);
	private static final BlockPos OLD_GROWTH_ORIGIN = new BlockPos(32, 4, 32);
	private static final int SCAN_RADIUS = 9;
	private static final int SCAN_HEIGHT = 32;
	private static final int MORPHOLOGY_SCAN_RADIUS = 13;
	private static final int MORPHOLOGY_SCAN_HEIGHT = 32;
	private static final int OLD_GROWTH_SCAN_RADIUS = 14;
	private static final int OLD_GROWTH_SCAN_HEIGHT = 56;

	@GameTest(structure = TREE_ARENA, maxTicks = 200, padding = 4)
	public void hemlockSaplingGrowsCustomHemlock(GameTestHelper helper) {
		SeasonGameTestSupport.useGrowingSeasonIfPresent(helper);
		growAndAssert(
			helper,
			ModBlocks.HEMLOCK_SAPLING,
			ModBlocks.HEMLOCK_LOG,
			ModBlocks.HEMLOCK_FOLIAGE,
			Blocks.SPRUCE_LOG,
			Blocks.SPRUCE_LEAVES,
			4107L,
			"Eastern hemlock"
		);
		helper.succeed();
	}

	@GameTest(structure = TREE_ARENA, maxTicks = 200, padding = 4)
	public void americanBeechSaplingGrowsCustomAmericanBeech(GameTestHelper helper) {
		SeasonGameTestSupport.useGrowingSeasonIfPresent(helper);
		growAndAssert(
			helper,
			ModBlocks.AMERICAN_BEECH_SAPLING,
			ModBlocks.AMERICAN_BEECH_LOG,
			ModBlocks.AMERICAN_BEECH_LEAVES,
			Blocks.BIRCH_LOG,
			Blocks.BIRCH_LEAVES,
			9013L,
			"American beech"
		);
		helper.succeed();
	}

	@GameTest(structure = TREE_ARENA, maxTicks = 200, padding = 4)
	public void blackWalnutSaplingGrowsCustomBlackWalnut(GameTestHelper helper) {
		SeasonGameTestSupport.useGrowingSeasonIfPresent(helper);
		growAndAssert(
			helper,
			ModBlocks.BLACK_WALNUT_SAPLING,
			ModBlocks.BLACK_WALNUT_LOG,
			ModBlocks.BLACK_WALNUT_LEAVES,
			Blocks.DARK_OAK_LOG,
			Blocks.DARK_OAK_LEAVES,
			12019L,
			"Black walnut"
		);
		helper.succeed();
	}

	@GameTest(structure = TREE_ARENA, maxTicks = 200, padding = 4)
	public void blackWalnutConfiguredFeatureBuildsBranchedDeepCrown(GameTestHelper helper) {
		helper.setBlock(TREE_ORIGIN.below(), Blocks.DIRT);
		boolean placed = placeConfiguredTree(
			helper,
			ModConfiguredFeatures.BLACK_WALNUT,
			TREE_ORIGIN,
			12019L
		);
		helper.assertTrue(placed, "Black walnut configured feature failed in a clear arena");

		SpeciesTree tree = scanSpeciesTree(
			helper,
			TREE_ORIGIN,
			ModBlocks.BLACK_WALNUT_LOG,
			ModBlocks.BLACK_WALNUT_LEAVES
		);
		helper.assertTrue(tree.logs().size() >= 10, "Black walnut generated too few logs");
		helper.assertTrue(tree.leaves().size() >= 80, "Black walnut generated too few leaves");
		int occupiedTop = maximumY(tree.allBlocks()) - TREE_ORIGIN.getY();
		helper.assertTrue(
			occupiedTop >= 12 && occupiedTop <= 25,
			"Black walnut escaped its relative-Y 12-25 envelope: " + occupiedTop
		);
		int horizontalRadius = maximumHorizontalRadius(tree.allBlocks(), TREE_ORIGIN);
		helper.assertTrue(
			horizontalRadius >= 3 && horizontalRadius <= 13,
			"Black walnut crown radius escaped 3-13 blocks: " + horizontalRadius
		);

		long lateralBranches = tree.logs().stream()
			.filter(pos -> pos.getY() - TREE_ORIGIN.getY() >= 4)
			.filter(pos -> helper.getBlockState(pos).getValue(RotatedPillarBlock.AXIS) != Direction.Axis.Y)
			.count();
		helper.assertTrue(lateralBranches > 0, "Black walnut retained a branchless straight pole");
		int crownDepth = maximumY(tree.leaves()) - minimumY(tree.leaves());
		helper.assertTrue(crownDepth >= 6, "Black walnut crown is still cap-flat: depth " + crownDepth);

		assertAllLogsTouchConnected(helper, tree, "Black walnut");
		Set<BlockPos> branchTips = terminalOffTrunkLogs(tree, TREE_ORIGIN, true);
		helper.assertTrue(!branchTips.isEmpty(), "Black walnut generated no lateral branch tips");
		assertBranchTipsHaveLeaves(helper, tree, branchTips, "Black walnut");
		assertSupportedLeaves(helper, tree, "Black walnut");
		assertEveryLeafComponentTouchesLog(helper, tree, false, "Black walnut");
		helper.succeed();
	}

	@GameTest(structure = TREE_ARENA, maxTicks = 200, padding = 4)
	public void tallHemlockConfiguredFeatureBuildsSupportedTieredCrown(GameTestHelper helper) {
		helper.setBlock(TREE_ORIGIN.below(), Blocks.DIRT);
		boolean placed = placeConfiguredTree(
			helper,
			ModConfiguredFeatures.HEMLOCK_TALL,
			TREE_ORIGIN,
			4107L
		);
		helper.assertTrue(placed, "tall hemlock configured feature failed in a clear arena");

		SpeciesTree tree = scanSpeciesTree(
			helper,
			TREE_ORIGIN,
			ModBlocks.HEMLOCK_LOG,
			ModBlocks.HEMLOCK_FOLIAGE
		);
		helper.assertTrue(tree.logs().size() >= 30, "tiered tall hemlock generated too few logs");
		helper.assertTrue(
			tree.leaves().size() >= 250 && tree.leaves().size() <= 450,
			"tiered tall hemlock escaped its 250-450 leaf envelope: " + tree.leaves().size()
		);

		int trunkHeight = tree.logs().stream()
			.mapToInt(BlockPos::getY)
			.max()
			.orElseThrow() - TREE_ORIGIN.getY() + 1;
		helper.assertTrue(
			trunkHeight >= 18 && trunkHeight <= 27,
			"tiered tall hemlock escaped its 18-27 log-height envelope: " + trunkHeight
		);
		int occupiedTop = maximumY(tree.allBlocks()) - TREE_ORIGIN.getY();
		helper.assertTrue(
			occupiedTop >= 19 && occupiedTop <= 28,
			"tiered tall hemlock escaped its relative-Y 19-28 crown envelope: " + occupiedTop
		);
		helper.assertTrue(
			maximumHorizontalRadius(tree.allBlocks(), TREE_ORIGIN) <= 5,
			"tiered tall hemlock exceeded its five-block crown radius"
		);

		Set<Integer> branchTiers = new HashSet<>();
		int xBranches = 0;
		int zBranches = 0;
		for (BlockPos log : tree.logs()) {
			Direction.Axis axis = helper.getBlockState(log).getValue(RotatedPillarBlock.AXIS);
			if (axis == Direction.Axis.X) {
				xBranches++;
				branchTiers.add(log.getY());
			} else if (axis == Direction.Axis.Z) {
				zBranches++;
				branchTiers.add(log.getY());
			}
		}
		helper.assertTrue(xBranches > 0 && zBranches > 0, "tall hemlock lacks alternating X/Z limbs");
		helper.assertTrue(
			branchTiers.size() >= 4 && branchTiers.size() <= 6,
			"tall hemlock escaped its four-to-six branch-tier envelope: " + branchTiers.size()
		);

		assertAllLogsFaceConnected(helper, tree, "tiered tall hemlock");
		Set<BlockPos> branchTips = terminalOffTrunkLogs(tree, TREE_ORIGIN, false);
		helper.assertTrue(branchTips.size() >= 8, "tiered tall hemlock generated too few branch tips");
		assertBranchTipsHaveLeaves(helper, tree, branchTips, "tiered tall hemlock");
		assertSupportedLeaves(helper, tree, "tiered tall hemlock");
		assertEveryLeafComponentTouchesLog(helper, tree, true, "tiered tall hemlock");
		assertNoSolidThreeByThreeLeafSlab(helper, tree, "tiered tall hemlock");
		helper.succeed();
	}

	@GameTest(structure = TREE_ARENA, maxTicks = 200, padding = 4)
	public void blockedTallHemlockConfiguredFeatureFailsAtomically(GameTestHelper helper) {
		helper.setBlock(TREE_ORIGIN.below(), Blocks.DIRT);
		BlockPos obstruction = TREE_ORIGIN.above(5);
		helper.setBlock(obstruction, Blocks.OAK_LOG);

		boolean placed = placeConfiguredTree(
			helper,
			ModConfiguredFeatures.HEMLOCK_TALL,
			TREE_ORIGIN,
			4107L
		);
		helper.assertFalse(placed, "blocked tall hemlock configured feature reported success");
		helper.assertBlockState(obstruction, Blocks.OAK_LOG.defaultBlockState());
		SpeciesTree tree = scanSpeciesTree(
			helper,
			TREE_ORIGIN,
			ModBlocks.HEMLOCK_LOG,
			ModBlocks.HEMLOCK_FOLIAGE
		);
		helper.assertValueEqual(tree.logs().size(), 0, "blocked tall hemlock partial-log count");
		helper.assertValueEqual(tree.leaves().size(), 0, "blocked tall hemlock partial-leaf count");
		helper.succeed();
	}

	@GameTest(structure = TREE_ARENA, maxTicks = 200, padding = 4)
	public void americanBeechConfiguredFeatureBuildsBranchedSupportedCrown(GameTestHelper helper) {
		helper.setBlock(TREE_ORIGIN.below(), Blocks.DIRT);
		boolean placed = placeConfiguredTree(
			helper,
			ModConfiguredFeatures.BEECH,
			TREE_ORIGIN,
			9013L
		);
		helper.assertTrue(placed, "American beech configured feature failed in a clear arena");

		SpeciesTree tree = scanSpeciesTree(
			helper,
			TREE_ORIGIN,
			ModBlocks.AMERICAN_BEECH_LOG,
			ModBlocks.AMERICAN_BEECH_LEAVES
		);
		helper.assertTrue(tree.logs().size() >= 10, "American beech generated too few logs");
		helper.assertTrue(tree.leaves().size() >= 100, "American beech generated too few leaves");
		int occupiedTop = maximumY(tree.allBlocks()) - TREE_ORIGIN.getY();
		helper.assertTrue(
			occupiedTop >= 16 && occupiedTop <= 24,
			"American beech escaped its relative-Y 16-24 crown envelope: " + occupiedTop
		);
		int horizontalRadius = maximumHorizontalRadius(tree.allBlocks(), TREE_ORIGIN);
		helper.assertTrue(
			horizontalRadius >= 3 && horizontalRadius <= 13,
			"American beech crown radius escaped 3-13 blocks: " + horizontalRadius
		);

		long lateralBranches = tree.logs().stream()
			.filter(pos -> pos.getY() - TREE_ORIGIN.getY() >= 4)
			.filter(pos -> helper.getBlockState(pos).getValue(RotatedPillarBlock.AXIS) != Direction.Axis.Y)
			.count();
		helper.assertTrue(lateralBranches > 0, "American beech retained a branchless straight pole");
		int crownDepth = maximumY(tree.leaves()) - minimumY(tree.leaves());
		helper.assertTrue(crownDepth >= 7, "American beech crown is too flat: depth " + crownDepth);

		assertAllLogsTouchConnected(helper, tree, "American beech");
		Set<BlockPos> branchTips = terminalOffTrunkLogs(tree, TREE_ORIGIN, true);
		helper.assertTrue(!branchTips.isEmpty(), "American beech generated no lateral branch tips");
		assertBranchTipsHaveLeaves(helper, tree, branchTips, "American beech");
		assertSupportedLeaves(helper, tree, "American beech");
		assertEveryLeafComponentTouchesLog(helper, tree, false, "American beech");
		helper.succeed();
	}

	@GameTest(structure = OLD_GROWTH_ARENA, maxTicks = 400, padding = 4)
	public void hemlockTwoByTwoGrowsExceptionalOldGrowth(GameTestHelper helper) {
		SeasonGameTestSupport.useGrowingSeasonIfPresent(helper);
		prepareSoil(helper, OLD_GROWTH_ORIGIN, OLD_GROWTH_SCAN_RADIUS);
		placeReadySquare(helper, OLD_GROWTH_ORIGIN);
		advanceHemlock(helper, OLD_GROWTH_ORIGIN, 1701L);

		int logs = 0;
		int leaves = 0;
		int lowestLeafY = Integer.MAX_VALUE;
		int highestOccupiedY = Integer.MIN_VALUE;
		int maximumLeafDistance = 0;
		Set<BlockPos> logPositions = new HashSet<>();
		for (BlockPos pos : oldGrowthScanVolume()) {
			BlockState state = helper.getBlockState(pos);
			if (state.is(ModBlocks.HEMLOCK_LOG)) {
				logs++;
				logPositions.add(pos.immutable());
				highestOccupiedY = Math.max(highestOccupiedY, pos.getY());
			}
			if (state.is(ModBlocks.HEMLOCK_FOLIAGE)) {
				leaves++;
				lowestLeafY = Math.min(lowestLeafY, pos.getY());
				highestOccupiedY = Math.max(highestOccupiedY, pos.getY());
				maximumLeafDistance = Math.max(
					maximumLeafDistance,
					state.getValue(LeavesBlock.DISTANCE)
				);
			}
		}

		helper.assertTrue(logs >= 100, "2x2 hemlock generated too few old-growth logs: " + logs);
		helper.assertTrue(leaves >= 400, "2x2 hemlock generated a sparse old-growth crown: " + leaves);
		int aboveGroundHeight = highestOccupiedY - OLD_GROWTH_ORIGIN.getY() + 1;
		helper.assertTrue(
			aboveGroundHeight >= 32 && aboveGroundHeight <= 52,
			"2x2 hemlock above-ground height escaped the 32-52 block contract: "
				+ aboveGroundHeight
		);
		helper.assertTrue(
			lowestLeafY - OLD_GROWTH_ORIGIN.getY() >= 10,
			"2x2 hemlock foliage crowded the navigable root zone at relative Y "
				+ (lowestLeafY - OLD_GROWTH_ORIGIN.getY())
		);
		helper.assertTrue(
			maximumLeafDistance <= 6,
			"2x2 hemlock generated unsupported foliage at distance " + maximumLeafDistance
		);
		assertAllLogsConnected(helper, logPositions);

		for (int dx = 0; dx <= 1; dx++) {
			for (int dz = 0; dz <= 1; dz++) {
				helper.assertBlockPresent(
					ModBlocks.HEMLOCK_LOG,
					OLD_GROWTH_ORIGIN.offset(dx, 0, dz)
				);
			}
		}
		for (int y = 1; y <= 13; y++) {
			for (BlockPos pos : BlockPos.betweenClosed(
				OLD_GROWTH_ORIGIN.offset(-OLD_GROWTH_SCAN_RADIUS, y, -OLD_GROWTH_SCAN_RADIUS),
				OLD_GROWTH_ORIGIN.offset(OLD_GROWTH_SCAN_RADIUS, y, OLD_GROWTH_SCAN_RADIUS)
			)) {
				if (!helper.getBlockState(pos).is(ModBlocks.HEMLOCK_LOG)) {
					continue;
				}
				int dx = pos.getX() - OLD_GROWTH_ORIGIN.getX();
				int dz = pos.getZ() - OLD_GROWTH_ORIGIN.getZ();
				helper.assertTrue(
					dx >= 0 && dx <= 1 && dz >= 0 && dz <= 1,
					"upright old-growth hemlock bole exceeded its 2x2 maximum at " + pos
				);
			}
		}

		int topCoreLogs = 0;
		for (int dx = 0; dx <= 1; dx++) {
			for (int dz = 0; dz <= 1; dz++) {
				if (helper.getBlockState(new BlockPos(
					OLD_GROWTH_ORIGIN.getX() + dx,
					highestOccupiedY,
					OLD_GROWTH_ORIGIN.getZ() + dz
				)).is(ModBlocks.HEMLOCK_LOG)) {
					topCoreLogs++;
				}
			}
		}
		helper.assertValueEqual(topCoreLogs, 1, "old-growth hemlock one-block terminal leader");
		assertNoHemlockSaplings(helper, OLD_GROWTH_ORIGIN, "successful 2x2 growth");
		helper.succeed();
	}

	@GameTest(structure = OLD_GROWTH_ARENA, maxTicks = 200, padding = 4)
	public void blockedHemlockTwoByTwoRestoresAllFourSaplings(GameTestHelper helper) {
		SeasonGameTestSupport.useGrowingSeasonIfPresent(helper);
		prepareSoil(helper, OLD_GROWTH_ORIGIN, OLD_GROWTH_SCAN_RADIUS);
		placeReadySquare(helper, OLD_GROWTH_ORIGIN);
		// TreeFeature's generic clearance scan never visits Y=-1. The north
		// buried root always occupies distance one in either X lane, so this
		// two-cell bar guarantees a custom-plan obstruction without replacing
		// the soil beneath any sapling.
		BlockPos westObstruction = OLD_GROWTH_ORIGIN.offset(0, -1, -1);
		BlockPos eastObstruction = OLD_GROWTH_ORIGIN.offset(1, -1, -1);
		helper.setBlock(westObstruction, Blocks.BEDROCK);
		helper.setBlock(eastObstruction, Blocks.BEDROCK);

		advanceHemlock(helper, OLD_GROWTH_ORIGIN.offset(1, 0, 1), 1701L);

		BlockState readySapling = ModBlocks.HEMLOCK_SAPLING
			.defaultBlockState()
			.setValue(SaplingBlock.STAGE, 1);
		for (int dx = 0; dx <= 1; dx++) {
			for (int dz = 0; dz <= 1; dz++) {
				helper.assertBlockState(OLD_GROWTH_ORIGIN.offset(dx, 0, dz), readySapling);
			}
		}
		helper.assertBlockState(westObstruction, Blocks.BEDROCK.defaultBlockState());
		helper.assertBlockState(eastObstruction, Blocks.BEDROCK.defaultBlockState());
		for (BlockPos pos : oldGrowthScanVolume()) {
			BlockState state = helper.getBlockState(pos);
			helper.assertFalse(
				state.is(ModBlocks.HEMLOCK_LOG) || state.is(ModBlocks.HEMLOCK_FOLIAGE),
				"blocked 2x2 hemlock left a partial tree at " + pos
			);
		}
		helper.succeed();
	}

	private static void growAndAssert(
		GameTestHelper helper,
		Block sapling,
		Block expectedLog,
		Block expectedLeaves,
		Block forbiddenProxyLog,
		Block forbiddenProxyLeaves,
		long seed,
		String label
	) {
		helper.setBlock(TREE_ORIGIN.below(), Blocks.DIRT);
		BlockState readySapling = sapling
			.defaultBlockState()
			.setValue(SaplingBlock.STAGE, 1);
		helper.setBlock(TREE_ORIGIN, readySapling);

		ServerLevel level = helper.getLevel();
		BlockPos absoluteOrigin = helper.absolutePos(TREE_ORIGIN);
		SeasonGameTestSupport.advanceSaplingForGeometry(
			(SaplingBlock) sapling,
			level,
			absoluteOrigin,
			readySapling,
			RandomSource.create(seed)
		);

		int logs = 0;
		int leaves = 0;
		int proxyBlocks = 0;
		for (BlockPos mutable : BlockPos.betweenClosed(
			TREE_ORIGIN.offset(-SCAN_RADIUS, 0, -SCAN_RADIUS),
			TREE_ORIGIN.offset(SCAN_RADIUS, SCAN_HEIGHT, SCAN_RADIUS)
		)) {
			BlockState state = helper.getBlockState(mutable);
			if (state.is(expectedLog)) {
				logs++;
			}
			if (state.is(expectedLeaves)) {
				leaves++;
			}
			if (state.is(forbiddenProxyLog) || state.is(forbiddenProxyLeaves)) {
				proxyBlocks++;
			}
		}

		helper.assertTrue(logs > 0, label + " sapling generated no custom logs");
		helper.assertTrue(leaves > 0, label + " sapling generated no custom leaves");
		helper.assertValueEqual(proxyBlocks, 0, label + " vanilla-proxy block count");
		helper.assertTrue(
			expectedLog.defaultBlockState().is(BlockTags.LOGS),
			label + " logs are missing from minecraft:logs"
		);
		helper.assertTrue(
			expectedLeaves.defaultBlockState().is(BlockTags.LEAVES),
			label + " foliage is missing from minecraft:leaves"
		);
		helper.assertTrue(
			sapling.defaultBlockState().is(BlockTags.SAPLINGS),
			label + " sapling is missing from minecraft:saplings"
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

	private static SpeciesTree scanSpeciesTree(
		GameTestHelper helper,
		BlockPos origin,
		Block logBlock,
		Block leafBlock
	) {
		Set<BlockPos> logs = new HashSet<>();
		Set<BlockPos> leaves = new HashSet<>();
		for (BlockPos mutable : BlockPos.betweenClosed(
			origin.offset(-MORPHOLOGY_SCAN_RADIUS, -1, -MORPHOLOGY_SCAN_RADIUS),
			origin.offset(MORPHOLOGY_SCAN_RADIUS, MORPHOLOGY_SCAN_HEIGHT, MORPHOLOGY_SCAN_RADIUS)
		)) {
			BlockPos pos = mutable.immutable();
			BlockState state = helper.getBlockState(pos);
			if (state.is(logBlock)) {
				logs.add(pos);
			} else if (state.is(leafBlock)) {
				leaves.add(pos);
			}
		}
		return new SpeciesTree(Set.copyOf(logs), Set.copyOf(leaves));
	}

	private static void assertAllLogsFaceConnected(
		GameTestHelper helper,
		SpeciesTree tree,
		String label
	) {
		Set<BlockPos> remaining = new HashSet<>(tree.logs());
		helper.assertFalse(remaining.isEmpty(), label + " generated no logs");
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
		helper.assertTrue(remaining.isEmpty(), label + " has disconnected face-log components");
	}

	private static void assertAllLogsTouchConnected(
		GameTestHelper helper,
		SpeciesTree tree,
		String label
	) {
		Set<BlockPos> remaining = new HashSet<>(tree.logs());
		helper.assertFalse(remaining.isEmpty(), label + " generated no logs");
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
		helper.assertTrue(remaining.isEmpty(), label + " has disconnected touching-log components");
	}

	private static Set<BlockPos> terminalOffTrunkLogs(
		SpeciesTree tree,
		BlockPos origin,
		boolean touchConnectivity
	) {
		Set<BlockPos> tips = new HashSet<>();
		for (BlockPos log : tree.logs()) {
			if (log.getX() == origin.getX() && log.getZ() == origin.getZ()) {
				continue;
			}
			int neighbors = 0;
			for (BlockPos candidate : tree.logs()) {
				if (candidate.equals(log)) {
					continue;
				}
				int dx = Math.abs(candidate.getX() - log.getX());
				int dy = Math.abs(candidate.getY() - log.getY());
				int dz = Math.abs(candidate.getZ() - log.getZ());
				boolean adjacent = touchConnectivity
					? dx <= 1 && dy <= 1 && dz <= 1
					: dx + dy + dz == 1;
				if (adjacent) {
					neighbors++;
				}
			}
			if (neighbors <= 1) {
				tips.add(log);
			}
		}
		return Set.copyOf(tips);
	}

	private static void assertBranchTipsHaveLeaves(
		GameTestHelper helper,
		SpeciesTree tree,
		Set<BlockPos> tips,
		String label
	) {
		for (BlockPos tip : tips) {
			boolean nearbyLeaf = false;
			for (BlockPos leaf : tree.leaves()) {
				nearbyLeaf |= Math.abs(leaf.getX() - tip.getX()) <= 1
					&& Math.abs(leaf.getY() - tip.getY()) <= 1
					&& Math.abs(leaf.getZ() - tip.getZ()) <= 1;
			}
			helper.assertTrue(nearbyLeaf, label + " branch tip lacks foliage at " + tip);
		}
	}

	private static void assertSupportedLeaves(
		GameTestHelper helper,
		SpeciesTree tree,
		String label
	) {
		Map<BlockPos, Integer> distances = leafSupportDistances(tree);
		for (BlockPos leaf : tree.leaves()) {
			BlockState state = helper.getBlockState(leaf);
			helper.assertFalse(state.getValue(LeavesBlock.PERSISTENT), label + " persistent leaf at " + leaf);
			int actual = state.getValue(LeavesBlock.DISTANCE);
			helper.assertTrue(
				actual >= 1 && actual < LeavesBlock.DECAY_DISTANCE,
				label + " unsupported distance-" + actual + " leaf at " + leaf
			);
			Integer expected = distances.get(leaf);
			helper.assertTrue(expected != null, label + " leaf lacks a <=6 face path to logs at " + leaf);
			if (expected != null) {
				helper.assertTrue(
					actual == expected || actual == expected + 1,
					label + " leaf distance " + actual + " disagrees with path " + expected + " at " + leaf
				);
			}
		}
	}

	private static Map<BlockPos, Integer> leafSupportDistances(SpeciesTree tree) {
		Map<BlockPos, Integer> distances = new HashMap<>();
		ArrayDeque<BlockPos> pending = new ArrayDeque<>();
		for (BlockPos log : tree.logs()) {
			distances.put(log, 0);
			pending.add(log);
		}
		while (!pending.isEmpty()) {
			BlockPos current = pending.removeFirst();
			int nextDistance = distances.get(current) + 1;
			if (nextDistance >= LeavesBlock.DECAY_DISTANCE) {
				continue;
			}
			for (Direction direction : Direction.values()) {
				BlockPos neighbor = current.relative(direction);
				if (tree.leaves().contains(neighbor) && !distances.containsKey(neighbor)) {
					distances.put(neighbor, nextDistance);
					pending.addLast(neighbor);
				}
			}
		}
		return Map.copyOf(distances);
	}

	private static void assertEveryLeafComponentTouchesLog(
		GameTestHelper helper,
		SpeciesTree tree,
		boolean requireOneComponent,
		String label
	) {
		Set<BlockPos> remaining = new HashSet<>(tree.leaves());
		int components = 0;
		while (!remaining.isEmpty()) {
			components++;
			ArrayDeque<BlockPos> pending = new ArrayDeque<>();
			BlockPos start = remaining.iterator().next();
			remaining.remove(start);
			pending.add(start);
			boolean touchesLog = false;
			while (!pending.isEmpty()) {
				BlockPos current = pending.removeFirst();
				for (Direction direction : Direction.values()) {
					BlockPos neighbor = current.relative(direction);
					touchesLog |= tree.logs().contains(neighbor);
					if (remaining.remove(neighbor)) {
						pending.addLast(neighbor);
					}
				}
			}
			helper.assertTrue(touchesLog, label + " has a floating leaf component");
		}
		if (requireOneComponent) {
			helper.assertValueEqual(components, 1, label + " leaf-component count");
		}
	}

	private static void assertNoSolidThreeByThreeLeafSlab(
		GameTestHelper helper,
		SpeciesTree tree,
		String label
	) {
		for (BlockPos leaf : tree.leaves()) {
			boolean solid = true;
			for (int dx = 0; dx < 3; dx++) {
				for (int dz = 0; dz < 3; dz++) {
					solid &= tree.leaves().contains(leaf.offset(dx, 0, dz));
				}
			}
			helper.assertFalse(solid, label + " contains a solid 3x3 leaf slab at " + leaf);
		}
	}

	private static int maximumHorizontalRadius(Set<BlockPos> positions, BlockPos origin) {
		return positions.stream()
			.mapToInt(pos -> Math.max(
				Math.abs(pos.getX() - origin.getX()),
				Math.abs(pos.getZ() - origin.getZ())
			))
			.max()
			.orElse(0);
	}

	private static int minimumY(Set<BlockPos> positions) {
		return positions.stream().mapToInt(BlockPos::getY).min().orElseThrow();
	}

	private static int maximumY(Set<BlockPos> positions) {
		return positions.stream().mapToInt(BlockPos::getY).max().orElseThrow();
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

	private static void placeReadySquare(GameTestHelper helper, BlockPos northWest) {
		BlockState readySapling = ModBlocks.HEMLOCK_SAPLING
			.defaultBlockState()
			.setValue(SaplingBlock.STAGE, 1);
		for (int dx = 0; dx <= 1; dx++) {
			for (int dz = 0; dz <= 1; dz++) {
				helper.setBlock(northWest.offset(dx, 0, dz), readySapling);
			}
		}
	}

	private static void advanceHemlock(GameTestHelper helper, BlockPos relativePos, long seed) {
		ServerLevel level = helper.getLevel();
		BlockPos absolutePos = helper.absolutePos(relativePos);
		BlockState state = level.getBlockState(absolutePos);
		SeasonGameTestSupport.advanceSaplingForGeometry(
			(SaplingBlock) ModBlocks.HEMLOCK_SAPLING,
			level,
			absolutePos,
			state,
			RandomSource.create(seed)
		);
	}

	private static Iterable<BlockPos> oldGrowthScanVolume() {
		return BlockPos.betweenClosed(
			OLD_GROWTH_ORIGIN.offset(-OLD_GROWTH_SCAN_RADIUS, -2, -OLD_GROWTH_SCAN_RADIUS),
			OLD_GROWTH_ORIGIN.offset(
				OLD_GROWTH_SCAN_RADIUS,
				OLD_GROWTH_SCAN_HEIGHT,
				OLD_GROWTH_SCAN_RADIUS
			)
		);
	}

	private static void assertNoHemlockSaplings(
		GameTestHelper helper,
		BlockPos northWest,
		String label
	) {
		for (int dx = 0; dx <= 1; dx++) {
			for (int dz = 0; dz <= 1; dz++) {
				helper.assertFalse(
					helper.getBlockState(northWest.offset(dx, 0, dz)).is(ModBlocks.HEMLOCK_SAPLING),
					label + " retained a hemlock sapling at " + northWest.offset(dx, 0, dz)
				);
			}
		}
	}

	private static void assertAllLogsConnected(GameTestHelper helper, Set<BlockPos> logs) {
		helper.assertFalse(logs.isEmpty(), "old-growth hemlock generated no log graph");
		Set<BlockPos> visited = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(logs.iterator().next());

		while (!queue.isEmpty()) {
			BlockPos current = queue.removeFirst();
			if (!visited.add(current)) {
				continue;
			}
			for (Direction direction : Direction.values()) {
				BlockPos neighbor = current.relative(direction);
				if (logs.contains(neighbor) && !visited.contains(neighbor)) {
					queue.addLast(neighbor);
				}
			}
		}

		helper.assertValueEqual(
			visited.size(),
			logs.size(),
			"old-growth hemlock six-connected log count"
		);
	}

	private record SpeciesTree(Set<BlockPos> logs, Set<BlockPos> leaves) {
		private Set<BlockPos> allBlocks() {
			Set<BlockPos> all = new HashSet<>(logs);
			all.addAll(leaves);
			return Set.copyOf(all);
		}
	}

	public TreeSpeciesGameTests() {
	}
}
