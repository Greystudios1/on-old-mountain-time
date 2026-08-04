package net.beforetheblight.gametest;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.beforetheblight.registry.ModBlocks;
import net.beforetheblight.worldgen.feature.HollowFallenLogFeature;
import net.beforetheblight.worldgen.feature.ModConfiguredFeatures;
import net.beforetheblight.worldgen.feature.SparseUnderstoryFeature;
import net.beforetheblight.worldgen.feature.configurations.HollowFallenLogConfiguration;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

/** Deterministic geometry and navigability contracts for forest-floor features. */
public final class ForestFloorFeatureGameTests {
	private static final String TREE_ARENA = "before_the_blight_gametest:chestnut_tree_48";
	private static final BlockPos FEATURE_ORIGIN = new BlockPos(24, 4, 24);
	private static final BlockPos HEMLOCK_FEATURE_ORIGIN = new BlockPos(10, 4, 10);

	@GameTest(maxTicks = 20)
	public void hollowDeadfallGeometryAndLegacyCodecStayContinuous(GameTestHelper helper) {
		for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
			for (int length = 6; length <= 11; length++) {
				List<BlockPos> shell = HollowFallenLogFeature.shellOffsets(length, axis);
				List<BlockPos> rotPocket = HollowFallenLogFeature.rotPocketOffsets(
					length,
					axis
				);
				Set<BlockPos> unique = new HashSet<>(shell);
				helper.assertValueEqual(
					unique.size(),
					shell.size(),
					"hollow-log shell uniqueness for " + axis + " length " + length
				);
				Direction lengthDirection =
					axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
				Direction crossDirection =
					axis == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
				int pocketStart = HollowFallenLogFeature.rotPocketStart(length);
				int pocketLength = HollowFallenLogFeature.rotPocketLength(length);
				int pocketEnd = pocketStart + pocketLength - 1;
				int closureSection = pocketEnd + 1;
				helper.assertTrue(
					pocketLength >= HollowFallenLogFeature.MIN_ROT_POCKET_LENGTH
						&& pocketLength <= HollowFallenLogFeature.MAX_ROT_POCKET_LENGTH,
					"hollow-log rot-pocket length for " + axis + " length " + length
				);

				for (int section = 0; section < length; section++) {
					BlockPos base = BlockPos.ZERO.relative(lengthDirection, section);
					helper.assertTrue(
						unique.contains(base),
						"hollow-log lost its unbroken fall-axis core in section "
							+ section + " for " + axis + " length " + length
					);
					if (section >= pocketStart && section <= pocketEnd) {
						BlockPos mouth = base.relative(crossDirection);
						helper.assertTrue(
							rotPocket.contains(mouth)
								&& rotPocket.contains(mouth.above())
								&& rotPocket.contains(base.above()),
							"hollow-log rot pocket lost a declared clearance in section "
								+ section + " for " + axis + " length " + length
						);
						helper.assertTrue(
							!unique.contains(mouth)
								&& !unique.contains(mouth.above())
								&& !unique.contains(base.above()),
							"hollow-log rot pocket intersects its shell in section "
								+ section + " for " + axis + " length " + length
						);
					}
				}

				assertConnected(helper, unique, "hollow-log body " + axis + " length " + length);
				assertOnlyDeclaredLongitudinalGaps(
					helper,
					unique,
					new HashSet<>(rotPocket),
					lengthDirection,
					crossDirection,
					length,
					axis
				);
				int previousCount = sectionCountsFor(shell, axis, closureSection);
				for (int section = closureSection + 1; section < length; section++) {
					int currentCount = sectionCountsFor(shell, axis, section);
					helper.assertTrue(
						currentCount <= previousCount,
						"hollow-log tail widened after closure at section "
							+ section + " for " + axis + " length " + length
					);
					previousCount = currentCount;
				}
				helper.assertValueEqual(
					sectionCountsFor(shell, axis, length - 1),
					1,
					"hollow-log distal taper block count for " + axis + " length " + length
				);
			}
		}

		ConfiguredFeature<?, ?> configuredFeature = helper.getLevel()
			.registryAccess()
			.lookupOrThrow(Registries.CONFIGURED_FEATURE)
			.getOrThrow(ModConfiguredFeatures.CHESTNUT_HOLLOW_FALLEN)
			.value();
		helper.assertTrue(
			configuredFeature.config() instanceof HollowFallenLogConfiguration,
			"hollow chestnut configured feature uses the wrong configuration codec"
		);
		HollowFallenLogConfiguration currentConfiguration =
			(HollowFallenLogConfiguration) configuredFeature.config();
		var registryOps = RegistryOps.create(
			JsonOps.INSTANCE,
			helper.getLevel().registryAccess()
		);
		JsonObject legacyPayload = HollowFallenLogConfiguration.CODEC
			.encodeStart(registryOps, currentConfiguration)
			.getOrThrow()
			.getAsJsonObject();
		legacyPayload.remove("stripped_trunk_provider");
		HollowFallenLogConfiguration decodedLegacy =
			HollowFallenLogConfiguration.CODEC
				.parse(registryOps, legacyPayload)
				.getOrThrow();
		helper.assertTrue(
			decodedLegacy.strippedTrunkProvider() == decodedLegacy.trunkProvider(),
			"legacy hollow-log JSON did not fall back to its trunk provider"
		);
		helper.assertTrue(
			HollowFallenLogConfiguration.CODEC
				.encodeStart(registryOps, decodedLegacy)
				.getOrThrow()
				.getAsJsonObject()
				.has("stripped_trunk_provider"),
			"hollow-log datagen stopped emitting the explicit stripped provider"
		);
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void understorySpacingPreservesWalkableGaps(GameTestHelper helper) {
		List<BlockPos> occupied = List.of(
			new BlockPos(0, 0, 0),
			new BlockPos(4, 0, 0)
		);
		helper.assertTrue(
			!SparseUnderstoryFeature.isSeparated(new BlockPos(1, 0, 0), occupied, 2),
			"understory accepted a plant inside the two-block spacing guard"
		);
		helper.assertTrue(
			SparseUnderstoryFeature.isSeparated(new BlockPos(2, 0, 2), occupied, 2),
			"understory rejected a plant outside the two-block spacing guard"
		);
		helper.assertTrue(
			!SparseUnderstoryFeature.isSeparated(new BlockPos(3, 8, 0), occupied, 2),
			"understory spacing incorrectly ignored horizontal crowding at another Y"
		);
		helper.succeed();
	}

	@GameTest(structure = TREE_ARENA, maxTicks = 200, padding = 4)
	public void configuredHollowDeadfallsPlaceContinuousRootedBoles(GameTestHelper helper) {
		prepareFlatForestFloor(helper);
		long seed = 20260729L;
		HollowFallenLogConfiguration configuration = hollowChestnutConfiguration(helper);
		RandomSource geometryRandom = RandomSource.create(seed);
		int length = configuration.logLength().sample(geometryRandom);
		Direction.Axis axis =
			geometryRandom.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z;
		Direction lengthDirection =
			axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
		Direction crossDirection =
			axis == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
		int openingSide = geometryRandom.nextBoolean() ? 1 : -1;
		int minimumLong =
			(axis == Direction.Axis.X ? FEATURE_ORIGIN.getX() : FEATURE_ORIGIN.getZ())
				- length / 2;
		BlockPos bodyStart = FEATURE_ORIGIN.relative(lengthDirection, -(length / 2));
		BlockPos replaceableRotPocketWitness = bodyStart
			.relative(lengthDirection, HollowFallenLogFeature.rotPocketStart(length))
			.relative(crossDirection, openingSide);
		BlockState replaceableLeaves = Blocks.OAK_LEAVES.defaultBlockState()
			.setValue(LeavesBlock.PERSISTENT, false);
		helper.setBlock(replaceableRotPocketWitness, replaceableLeaves);
		helper.setBlock(replaceableRotPocketWitness.above(), replaceableLeaves);
		boolean placed = placeConfigured(
			helper,
			ModConfiguredFeatures.CHESTNUT_HOLLOW_FALLEN,
			FEATURE_ORIGIN,
			seed
		);
		helper.assertTrue(placed, "hollow chestnut rejected a clear, flat forest floor");
		helper.assertTrue(
			helper.getBlockState(replaceableRotPocketWitness).isAir()
				&& helper.getBlockState(replaceableRotPocketWitness.above()).isAir(),
			"hollow chestnut did not clear replaceable rot-pocket foliage"
		);

		int xAxisLogs = 0;
		int yAxisLogs = 0;
		int zAxisLogs = 0;
		int strippedLogs = 0;
		for (int x = 15; x <= 33; x++) {
			for (int y = 4; y <= 7; y++) {
				for (int z = 15; z <= 33; z++) {
					BlockState state = helper.getBlockState(new BlockPos(x, y, z));
					if (!isChestnutDeadfallLog(state)) {
						continue;
					}
					Direction.Axis logAxis = state.getValue(RotatedPillarBlock.AXIS);
					xAxisLogs += logAxis == Direction.Axis.X ? 1 : 0;
					yAxisLogs += logAxis == Direction.Axis.Y ? 1 : 0;
					zAxisLogs += logAxis == Direction.Axis.Z ? 1 : 0;
					strippedLogs += state.is(ModBlocks.STRIPPED_CHESTNUT_LOG) ? 1 : 0;
					if (state.is(ModBlocks.STRIPPED_CHESTNUT_LOG)) {
						assertNoCoverOnFracture(
							helper,
							new BlockPos(x, y, z),
							"chestnut"
						);
					}
				}
			}
		}
		Direction.Axis observedAxis =
			xAxisLogs > zAxisLogs ? Direction.Axis.X : Direction.Axis.Z;
		helper.assertValueEqual(observedAxis, axis, "hollow chestnut sampled body axis");
		Direction.Axis crossAxis = axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
		helper.assertTrue(
			Math.max(xAxisLogs, zAxisLogs) > Math.min(xAxisLogs, zAxisLogs),
			"hollow chestnut body axis is not distinguishable from roots and branch"
		);
		helper.assertTrue(yAxisLogs >= 4, "hollow chestnut lacks a varied-height rooted stump");
		helper.assertTrue(
			(axis == Direction.Axis.X ? zAxisLogs : xAxisLogs) >= 4,
			"hollow chestnut lacks cross-grain root flare and branch blocks"
		);
		helper.assertTrue(strippedLogs >= 5, "hollow chestnut lacks stripped snapped splinters");

		helper.assertTrue(
			length >= 6 && length <= 10,
			"hollow chestnut length left its 6..10 contract"
		);

		int pocketStart = HollowFallenLogFeature.rotPocketStart(length);
		int pocketLength = HollowFallenLogFeature.rotPocketLength(length);
		int closureSection = pocketStart + pocketLength;
		for (int section = pocketStart; section < closureSection; section++) {
			BlockPos base = bodyBase(axis, minimumLong, section);
			BlockState core = helper.getBlockState(base);
			helper.assertTrue(
				isChestnutDeadfallLog(core)
					&& core.getValue(RotatedPillarBlock.AXIS) == axis,
				"hollow chestnut lost its continuous horizontal core at section " + section
			);
			BlockPos mouth = base.relative(crossDirection, openingSide);
			helper.assertTrue(
				helper.getBlockState(mouth).isAir()
					&& helper.getBlockState(mouth.above()).isAir()
					&& helper.getBlockState(base.above()).isAir(),
				"hollow chestnut rot pocket was blocked at section " + section
			);
			helper.assertBlockPresent(Blocks.GRASS_BLOCK, mouth.below());
		}

		helper.assertTrue(
			isChestnutDeadfallLog(helper.getBlockState(bodyBase(axis, minimumLong, 0))),
			"hollow chestnut proximal fracture lost its horizontal core"
		);
		helper.assertTrue(
			isChestnutDeadfallLog(
				helper.getBlockState(bodyBase(axis, minimumLong, closureSection))
			),
			"hollow chestnut closure no longer bounds its rot pocket"
		);
		int previousCount = Integer.MAX_VALUE;
		for (int section = 0; section < length; section++) {
			BlockPos corePos = bodyBase(axis, minimumLong, section);
			BlockState core = helper.getBlockState(corePos);
			helper.assertTrue(
				isChestnutDeadfallLog(core)
					&& core.getValue(RotatedPillarBlock.AXIS) == axis,
				"hollow chestnut core is missing or rotated at section " + section
			);
			int count = countBodySectionLogs(
				helper,
				axis,
				minimumLong,
				section,
				-2,
				2
			);
			if (section > closureSection) {
				helper.assertTrue(
					count <= previousCount,
					"hollow chestnut widened after the rot-pocket closure at section "
						+ section
				);
			}
			if (section >= closureSection) {
				previousCount = count;
			}
		}
		for (BlockPos offset : HollowFallenLogFeature.shellOffsets(length, axis)) {
			BlockState bodyState = helper.getBlockState(bodyStart.offset(offset));
			if (isChestnutDeadfallLog(bodyState)) {
				helper.assertValueEqual(
					bodyState.getValue(RotatedPillarBlock.AXIS),
					axis,
					"hollow chestnut body log axis at " + offset
				);
			}
		}
		helper.assertValueEqual(
			countBodySectionLogs(helper, axis, minimumLong, length - 1, -2, 2),
			1,
			"hollow chestnut distal taper block count"
		);

		BlockPos stumpBase = bodyBase(axis, minimumLong, -2);
		helper.assertTrue(
			isChestnutDeadfallLog(helper.getBlockState(stumpBase))
				&& isChestnutDeadfallLog(helper.getBlockState(stumpBase.above()))
				&& helper.getBlockState(stumpBase.above(2)).is(ModBlocks.STRIPPED_CHESTNUT_LOG),
			"hollow chestnut snapped stump height/material contract"
		);
		for (int side : new int[] {-1, 1}) {
			BlockState root = helper.getBlockState(stumpBase.relative(crossDirection, side));
			helper.assertTrue(
				isChestnutDeadfallLog(root)
					&& root.getValue(RotatedPillarBlock.AXIS) == crossAxis,
				"hollow chestnut three-wide root flare"
			);
			helper.assertTrue(
				isChestnutDeadfallLog(
					helper.getBlockState(
						stumpBase.relative(lengthDirection, -1).relative(crossDirection, side)
					)
				),
				"hollow chestnut root toe"
			);
		}
		BlockState stumpSplinter = helper.getBlockState(
			stumpBase.relative(lengthDirection).above(2)
		);
		BlockState bodyFracture = helper.getBlockState(
			bodyBase(axis, minimumLong, 0).above(2)
		);
		helper.assertTrue(
			stumpSplinter.is(ModBlocks.STRIPPED_CHESTNUT_LOG)
				&& stumpSplinter.getValue(RotatedPillarBlock.AXIS) == axis,
			"hollow chestnut stump lacks a fall-axis splinter toward the break"
		);
		helper.assertTrue(
			bodyFracture.is(ModBlocks.STRIPPED_CHESTNUT_LOG)
				&& bodyFracture.getValue(RotatedPillarBlock.AXIS) == axis,
			"hollow chestnut bole lacks a stripped fracture facing the stump"
		);
		Set<BlockPos> chestnutPlan = collectChestnutDeadfallLogs(
			helper,
			15,
			33,
			4,
			7,
			15,
			33
		);
		assertConnected(helper, chestnutPlan, "configured hollow chestnut plan");

		preparePodzolPatch(helper, 2, 18, 2, 18);
		long hemlockSeed = 20260730L;
		HollowFallenLogConfiguration hemlockConfiguration = hollowConfiguration(
			helper,
			ModConfiguredFeatures.COVE_HOLLOW_FALLEN_HEMLOCK
		);
		RandomSource hemlockGeometryRandom = RandomSource.create(hemlockSeed);
		int hemlockLength = hemlockConfiguration.logLength().sample(hemlockGeometryRandom);
		Direction.Axis hemlockAxis =
			hemlockGeometryRandom.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z;
		Direction hemlockLengthDirection =
			hemlockAxis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
		Direction hemlockCrossDirection =
			hemlockAxis == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
		BlockPos hemlockStart = HEMLOCK_FEATURE_ORIGIN.relative(
			hemlockLengthDirection,
			-(hemlockLength / 2)
		);
		int loweredSection = hemlockLength - 2;
		for (int side = -1; side <= 1; side++) {
			BlockPos lowerContact = hemlockStart
				.relative(hemlockLengthDirection, loweredSection)
				.relative(hemlockCrossDirection, side);
			helper.setBlock(lowerContact.below(), Blocks.AIR);
			helper.setBlock(lowerContact.below(2), Blocks.PODZOL);
		}
		boolean hemlockPlaced = placeConfigured(
			helper,
			ModConfiguredFeatures.COVE_HOLLOW_FALLEN_HEMLOCK,
			HEMLOCK_FEATURE_ORIGIN,
			hemlockSeed
		);
		helper.assertTrue(
			hemlockPlaced,
			"hollow hemlock rejected a bounded one-block podzol slope"
		);
		int hemlockLogs = 0;
		int strippedHemlockLogs = 0;
		int foreignChestnutLogs = 0;
		for (int x = 1; x <= 19; x++) {
			for (int y = 3; y <= 7; y++) {
				for (int z = 1; z <= 19; z++) {
					BlockPos pos = new BlockPos(x, y, z);
					BlockState state = helper.getBlockState(pos);
					hemlockLogs += isHemlockDeadfallLog(state) ? 1 : 0;
					strippedHemlockLogs +=
						state.is(ModBlocks.STRIPPED_HEMLOCK_LOG) ? 1 : 0;
					foreignChestnutLogs += isChestnutDeadfallLog(state) ? 1 : 0;
					if (state.is(ModBlocks.STRIPPED_HEMLOCK_LOG)) {
						assertNoCoverOnFracture(helper, pos, "hemlock");
					}
				}
			}
		}
		helper.assertTrue(hemlockLogs > 0, "hollow hemlock placed no custom hemlock logs");
		helper.assertTrue(
			strippedHemlockLogs >= 5,
			"hollow hemlock lacks species-correct stripped splinters"
		);
		helper.assertValueEqual(
			foreignChestnutLogs,
			0,
			"hollow hemlock mixed chestnut into its species-specific plan"
		);
		helper.assertTrue(
			isHemlockDeadfallLog(
				helper.getBlockState(
					hemlockStart
						.relative(hemlockLengthDirection, loweredSection)
						.below()
				)
			),
			"hollow hemlock one-block slope lacks a buried grounding log"
		);
		helper.succeed();
	}

	@GameTest(structure = TREE_ARENA, maxTicks = 200, padding = 4)
	public void configuredHollowDeadfallsRejectUnsafeSitesAtomically(GameTestHelper helper) {
		prepareFlatForestFloor(helper);
		BlockPos cavityFloorWitness = FEATURE_ORIGIN;
		BlockPos cavityHeadWitness = FEATURE_ORIGIN.above();
		BlockState persistentLeaves = Blocks.OAK_LEAVES.defaultBlockState()
			.setValue(LeavesBlock.PERSISTENT, true);
		helper.setBlock(cavityFloorWitness, persistentLeaves);
		helper.setBlock(cavityHeadWitness, persistentLeaves);
		Map<BlockPos, BlockState> obstructionSnapshot = snapshotEnvelope(
			helper,
			FEATURE_ORIGIN
		);
		boolean placed = placeConfigured(
			helper,
			ModConfiguredFeatures.CHESTNUT_HOLLOW_FALLEN,
			FEATURE_ORIGIN,
			20260729L
		);
		helper.assertTrue(
			!placed,
			"hollow chestnut destroyed player-like persistent foliage"
		);
		assertSnapshotUnchanged(helper, obstructionSnapshot, "persistent obstruction");
		helper.assertValueEqual(
			countChestnutDeadfallLogs(helper),
			0,
			"failed hollow chestnut left partial logs"
		);
		helper.assertBlockProperty(cavityFloorWitness, LeavesBlock.PERSISTENT, true);
		helper.assertBlockPresent(Blocks.OAK_LEAVES, cavityFloorWitness);
		helper.assertBlockProperty(cavityHeadWitness, LeavesBlock.PERSISTENT, true);
		helper.assertBlockPresent(Blocks.OAK_LEAVES, cavityHeadWitness);

		helper.setBlock(cavityFloorWitness, Blocks.AIR);
		helper.setBlock(cavityHeadWitness, Blocks.AIR);
		long seed = 20260729L;
		HollowFallenLogConfiguration configuration = hollowChestnutConfiguration(helper);
		RandomSource geometryRandom = RandomSource.create(seed);
		int length = configuration.logLength().sample(geometryRandom);
		Direction.Axis axis =
			geometryRandom.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z;
		Direction lengthDirection =
			axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
		Direction crossDirection =
			axis == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
		int openingSide = geometryRandom.nextBoolean() ? 1 : -1;
		BlockPos bodyStart = FEATURE_ORIGIN.relative(lengthDirection, -(length / 2));
		int pocketSection =
			HollowFallenLogFeature.rotPocketStart(length)
				+ HollowFallenLogFeature.rotPocketLength(length) / 2;
		BlockPos adjacentWater = bodyStart
			.relative(lengthDirection, pocketSection)
			.relative(crossDirection, openingSide * 2);
		helper.setBlock(adjacentWater, Blocks.WATER);
		Map<BlockPos, BlockState> waterSnapshot = snapshotEnvelope(helper, FEATURE_ORIGIN);
		boolean floodedPlacement = placeConfigured(
			helper,
			ModConfiguredFeatures.CHESTNUT_HOLLOW_FALLEN,
			FEATURE_ORIGIN,
			seed
		);
		helper.assertTrue(
			!floodedPlacement,
			"hollow chestnut opened its rot pocket beside water"
		);
		assertSnapshotUnchanged(helper, waterSnapshot, "adjacent-fluid obstruction");
		helper.assertBlockPresent(Blocks.WATER, adjacentWater);
		helper.assertValueEqual(
			countChestnutDeadfallLogs(helper),
			0,
			"fluid-halo rejection left partial hollow chestnut logs"
		);

		helper.setBlock(adjacentWater, Blocks.AIR);
		BlockPos unsupportedOrigin = new BlockPos(38, 4, 10);
		long unsupportedSeed = 20260731L;
		RandomSource unsupportedRandom = RandomSource.create(unsupportedSeed);
		int unsupportedLength = configuration.logLength().sample(unsupportedRandom);
		Direction.Axis unsupportedAxis =
			unsupportedRandom.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z;
		Direction unsupportedLengthDirection =
			unsupportedAxis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
		Direction unsupportedCrossDirection =
			unsupportedAxis == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
		BlockPos unsupportedStart = unsupportedOrigin.relative(
			unsupportedLengthDirection,
			-(unsupportedLength / 2)
		);
		for (int section = 1; section <= 3; section++) {
			for (int side = -1; side <= 1; side++) {
				BlockPos lowered = unsupportedStart
					.relative(unsupportedLengthDirection, section)
					.relative(unsupportedCrossDirection, side);
				helper.setBlock(lowered.below(), Blocks.AIR);
				helper.setBlock(lowered.below(2), Blocks.GRASS_BLOCK);
			}
		}
		Map<BlockPos, BlockState> unsupportedSnapshot = snapshotEnvelope(
			helper,
			unsupportedOrigin
		);
		boolean unsupportedPlacement = placeConfigured(
			helper,
			ModConfiguredFeatures.CHESTNUT_HOLLOW_FALLEN,
			unsupportedOrigin,
			unsupportedSeed
		);
		helper.assertTrue(
			!unsupportedPlacement,
			"hollow chestnut exceeded its two-section buried-support limit"
		);
		assertSnapshotUnchanged(helper, unsupportedSnapshot, "over-limit slope");
		helper.assertValueEqual(
			countChestnutDeadfallLogs(helper),
			0,
			"over-limit slope rejection left partial logs"
		);

		BlockPos voidOrigin = new BlockPos(10, 4, 38);
		long voidSeed = 20260732L;
		RandomSource voidRandom = RandomSource.create(voidSeed);
		int voidLength = configuration.logLength().sample(voidRandom);
		Direction.Axis voidAxis =
			voidRandom.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z;
		Direction voidLengthDirection =
			voidAxis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
		Direction voidCrossDirection =
			voidAxis == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
		BlockPos voidStart = voidOrigin.relative(
			voidLengthDirection,
			-(voidLength / 2)
		);
		for (int side = -1; side <= 1; side++) {
			BlockPos deepDip = voidStart
				.relative(voidLengthDirection, 1)
				.relative(voidCrossDirection, side);
			helper.setBlock(deepDip.below(), Blocks.AIR);
			helper.setBlock(deepDip.below(2), Blocks.AIR);
			helper.setBlock(deepDip.below(3), Blocks.GRASS_BLOCK);
		}
		Map<BlockPos, BlockState> voidSnapshot = snapshotEnvelope(helper, voidOrigin);
		boolean voidPlacement = placeConfigured(
			helper,
			ModConfiguredFeatures.CHESTNUT_HOLLOW_FALLEN,
			voidOrigin,
			voidSeed
		);
		helper.assertTrue(
			!voidPlacement,
			"hollow chestnut bridged a two-block terrain void"
		);
		assertSnapshotUnchanged(helper, voidSnapshot, "two-block terrain void");
		helper.assertValueEqual(
			countChestnutDeadfallLogs(helper),
			0,
			"two-block void rejection left partial logs"
		);
		helper.succeed();
	}

	private static int sectionCountsFor(
		List<BlockPos> offsets,
		Direction.Axis axis,
		int section
	) {
		int count = 0;
		for (BlockPos offset : offsets) {
			int longitudinal = axis == Direction.Axis.X ? offset.getX() : offset.getZ();
			count += longitudinal == section ? 1 : 0;
		}
		return count;
	}

	private static void assertOnlyDeclaredLongitudinalGaps(
		GameTestHelper helper,
		Set<BlockPos> shell,
		Set<BlockPos> declaredClearances,
		Direction lengthDirection,
		Direction crossDirection,
		int length,
		Direction.Axis axis
	) {
		for (int side = -1; side <= 1; side++) {
			for (int y = 0; y <= 2; y++) {
				int first = Integer.MAX_VALUE;
				int last = Integer.MIN_VALUE;
				for (int section = 0; section < length; section++) {
					BlockPos offset = BlockPos.ZERO
						.relative(lengthDirection, section)
						.relative(crossDirection, side)
						.above(y);
					if (shell.contains(offset)) {
						first = Math.min(first, section);
						last = Math.max(last, section);
					}
				}
				if (first == Integer.MAX_VALUE) {
					continue;
				}
				for (int section = first; section <= last; section++) {
					BlockPos offset = BlockPos.ZERO
						.relative(lengthDirection, section)
						.relative(crossDirection, side)
						.above(y);
					if (!shell.contains(offset)) {
						helper.assertTrue(
							declaredClearances.contains(offset),
							"hollow-log undeclared longitudinal gap at "
								+ offset + " for " + axis + " length " + length
						);
					}
				}
			}
		}
	}

	private static void assertConnected(
		GameTestHelper helper,
		Set<BlockPos> positions,
		String description
	) {
		helper.assertTrue(!positions.isEmpty(), description + " placed no logs");
		Set<BlockPos> remaining = new HashSet<>(positions);
		ArrayDeque<BlockPos> frontier = new ArrayDeque<>();
		BlockPos first = remaining.iterator().next();
		remaining.remove(first);
		frontier.add(first);
		int visited = 0;
		while (!frontier.isEmpty()) {
			BlockPos current = frontier.removeFirst();
			visited++;
			for (Direction direction : Direction.values()) {
				BlockPos adjacent = current.relative(direction);
				if (remaining.remove(adjacent)) {
					frontier.addLast(adjacent);
				}
			}
		}
		helper.assertValueEqual(
			visited,
			positions.size(),
			description + " six-connected log count"
		);
	}

	private static Set<BlockPos> collectChestnutDeadfallLogs(
		GameTestHelper helper,
		int minimumX,
		int maximumX,
		int minimumY,
		int maximumY,
		int minimumZ,
		int maximumZ
	) {
		Set<BlockPos> result = new HashSet<>();
		for (int x = minimumX; x <= maximumX; x++) {
			for (int y = minimumY; y <= maximumY; y++) {
				for (int z = minimumZ; z <= maximumZ; z++) {
					BlockPos pos = new BlockPos(x, y, z);
					if (isChestnutDeadfallLog(helper.getBlockState(pos))) {
						result.add(pos);
					}
				}
			}
		}
		return Set.copyOf(result);
	}

	private static BlockPos bodyBase(
		Direction.Axis axis,
		int minimumLong,
		int section
	) {
		int longitudinal = minimumLong + section;
		return axis == Direction.Axis.X
			? new BlockPos(longitudinal, FEATURE_ORIGIN.getY(), FEATURE_ORIGIN.getZ())
			: new BlockPos(FEATURE_ORIGIN.getX(), FEATURE_ORIGIN.getY(), longitudinal);
	}

	private static int countBodySectionLogs(
		GameTestHelper helper,
		Direction.Axis axis,
		int minimumLong,
		int section,
		int minimumSide,
		int maximumSide
	) {
		Direction crossDirection = axis == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
		BlockPos base = bodyBase(axis, minimumLong, section);
		int count = 0;
		for (int side = minimumSide; side <= maximumSide; side++) {
			for (int y = 0; y <= 3; y++) {
				count += isChestnutDeadfallLog(
					helper.getBlockState(base.relative(crossDirection, side).above(y))
				) ? 1 : 0;
			}
		}
		return count;
	}

	private static int countCrownLogs(
		GameTestHelper helper,
		Direction.Axis axis,
		int minimumLong,
		int section
	) {
		Direction crossDirection = axis == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
		BlockPos crownCenter = bodyBase(axis, minimumLong, section).above(2);
		int count = 0;
		for (int side = -1; side <= 1; side++) {
			count += isChestnutDeadfallLog(
				helper.getBlockState(crownCenter.relative(crossDirection, side))
			) ? 1 : 0;
		}
		return count;
	}

	private static boolean isChestnutDeadfallLog(BlockState state) {
		return state.is(ModBlocks.CHESTNUT_LOG)
			|| state.is(ModBlocks.STRIPPED_CHESTNUT_LOG);
	}

	private static boolean isHemlockDeadfallLog(BlockState state) {
		return state.is(ModBlocks.HEMLOCK_LOG)
			|| state.is(ModBlocks.STRIPPED_HEMLOCK_LOG);
	}

	private static void assertNoCoverOnFracture(
		GameTestHelper helper,
		BlockPos fracture,
		String species
	) {
		BlockState above = helper.getBlockState(fracture.above());
		helper.assertTrue(
			!above.is(Blocks.MOSS_CARPET) && !above.is(Blocks.LEAF_LITTER),
			"hollow " + species + " surface cover hid a stripped fracture face"
		);
	}

	private static HollowFallenLogConfiguration hollowChestnutConfiguration(
		GameTestHelper helper
	) {
		return hollowConfiguration(
			helper,
			ModConfiguredFeatures.CHESTNUT_HOLLOW_FALLEN
		);
	}

	private static HollowFallenLogConfiguration hollowConfiguration(
		GameTestHelper helper,
		net.minecraft.resources.ResourceKey<ConfiguredFeature<?, ?>> key
	) {
		ConfiguredFeature<?, ?> feature = helper.getLevel()
			.registryAccess()
			.lookupOrThrow(Registries.CONFIGURED_FEATURE)
			.getOrThrow(key)
			.value();
		return (HollowFallenLogConfiguration) feature.config();
	}

	private static int countChestnutDeadfallLogs(GameTestHelper helper) {
		int count = 0;
		for (int x = 1; x <= 46; x++) {
			for (int y = 2; y <= 8; y++) {
				for (int z = 1; z <= 46; z++) {
					count += isChestnutDeadfallLog(
						helper.getBlockState(new BlockPos(x, y, z))
					) ? 1 : 0;
				}
			}
		}
		return count;
	}

	private static Map<BlockPos, BlockState> snapshotEnvelope(
		GameTestHelper helper,
		BlockPos origin
	) {
		Map<BlockPos, BlockState> snapshot = new LinkedHashMap<>();
		for (int x = origin.getX() - 9; x <= origin.getX() + 9; x++) {
			for (int y = origin.getY() - 3; y <= origin.getY() + 4; y++) {
				for (int z = origin.getZ() - 9; z <= origin.getZ() + 9; z++) {
					BlockPos pos = new BlockPos(x, y, z);
					snapshot.put(pos, helper.getBlockState(pos));
				}
			}
		}
		return Map.copyOf(snapshot);
	}

	private static void assertSnapshotUnchanged(
		GameTestHelper helper,
		Map<BlockPos, BlockState> snapshot,
		String scenario
	) {
		for (Map.Entry<BlockPos, BlockState> entry : snapshot.entrySet()) {
			helper.assertValueEqual(
				helper.getBlockState(entry.getKey()),
				entry.getValue(),
				"hollow-deadfall atomic snapshot for "
					+ scenario + " at " + entry.getKey()
			);
		}
	}

	private static void prepareFlatForestFloor(GameTestHelper helper) {
		for (int x = 1; x <= 46; x++) {
			for (int z = 1; z <= 46; z++) {
				helper.setBlock(new BlockPos(x, 3, z), Blocks.GRASS_BLOCK);
				for (int y = 4; y <= 8; y++) {
					helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
				}
			}
		}
	}

	private static void preparePodzolPatch(
		GameTestHelper helper,
		int minimumX,
		int maximumX,
		int minimumZ,
		int maximumZ
	) {
		for (int x = minimumX; x <= maximumX; x++) {
			for (int z = minimumZ; z <= maximumZ; z++) {
				helper.setBlock(new BlockPos(x, 3, z), Blocks.PODZOL);
				for (int y = 4; y <= 8; y++) {
					helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
				}
			}
		}
	}

	private static boolean placeConfigured(
		GameTestHelper helper,
		net.minecraft.resources.ResourceKey<ConfiguredFeature<?, ?>> key,
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
}
