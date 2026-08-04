package net.beforetheblight.worldgen.feature.trunkplacer;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;

/**
 * Builds the exceptional two-by-two eastern-hemlock form.
 *
 * <p>The upright bole never exceeds a two-by-two footprint. It tapers first to
 * a three-log shoulder, one-by-two, and then a one-block leader, while shallow
 * cardinal roots make the base read as anchored instead of as a square
 * cylinder. The tiered crown starts high enough to keep the forest floor
 * navigable.</p>
 *
 * <p>Every root, trunk, and branch log is planned and checked before the first
 * block changes. Returning no attachments without writing logs makes the tree
 * feature fail cleanly, allowing {@link net.minecraft.world.level.block.grower.TreeGrower}
 * to restore all four mega-saplings.</p>
 */
public final class OldGrowthHemlockTrunkPlacer extends TrunkPlacer {
	public static final MapCodec<OldGrowthHemlockTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(
		instance -> trunkPlacerParts(instance).apply(instance, OldGrowthHemlockTrunkPlacer::new)
	);

	private static final Direction[] CARDINAL_DIRECTIONS = {
		Direction.NORTH,
		Direction.EAST,
		Direction.SOUTH,
		Direction.WEST
	};
	public static final int MAX_UPRIGHT_TRUNK_WIDTH = 2;
	private static final int MINIMUM_TREE_HEIGHT = 32;
	private static final int BUTTRESS_TOP = 5;
	private static final int MINIMUM_CROWN_Y = 14;
	private static final int CROWN_DEPTH = 28;
	private static final int CROWN_TOP_CLEARANCE = 3;
	private static final int TIER_SPACING = 3;
	private static final int LOWER_BRANCH_REACH = 8;
	private static final int UPPER_BRANCH_REACH = 3;
	private static final int BURIED_ROOT_REACH = 4;
	private static final int BURIED_ROOT_VARIATION = 2;
	private static final int SURFACE_ROOT_REACH = 1;
	private static final int SURFACE_ROOT_VARIATION = 2;

	public OldGrowthHemlockTrunkPlacer(int baseHeight, int heightRandA, int heightRandB) {
		super(baseHeight, heightRandA, heightRandB);
	}

	@Override
	protected TrunkPlacerType<?> type() {
		return ModTrunkPlacerTypes.OLD_GROWTH_HEMLOCK_TRUNK_PLACER;
	}

	@Override
	public List<FoliagePlacer.FoliageAttachment> placeTrunk(
		WorldGenLevel level,
		BiConsumer<BlockPos, BlockState> trunkSetter,
		RandomSource random,
		int treeHeight,
		BlockPos origin,
		TreeConfiguration configuration
	) {
		if (treeHeight < MINIMUM_TREE_HEIGHT) {
			return List.of();
		}

		TreePlan plan = createPlan(random, treeHeight);
		if (!canPlacePlan(level, origin, plan)) {
			return List.of();
		}

		for (PlannedLog log : plan.logs()) {
			BlockPos pos = origin.offset(log.offset());
			BlockState state = configuration.trunkProvider
				.getState(level, random, pos)
				.trySetValue(RotatedPillarBlock.AXIS, log.axis());
			trunkSetter.accept(pos, state);
		}

		return plan.foliagePoints()
			.stream()
			.map(pos -> new FoliagePlacer.FoliageAttachment(origin.offset(pos), 0, false))
			.toList();
	}

	private static TreePlan createPlan(RandomSource random, int treeHeight) {
		Map<BlockPos, Direction.Axis> logs = new LinkedHashMap<>();
		Set<BlockPos> foliagePoints = new LinkedHashSet<>();
		int leaderX = random.nextInt(2);
		int leaderZ = random.nextInt(2);
		Direction.Axis middleAxis = random.nextBoolean()
			? Direction.Axis.X
			: Direction.Axis.Z;

		addRoots(logs, random);
		addTaperedTrunk(logs, treeHeight, leaderX, leaderZ, middleAxis);
		addCrown(logs, foliagePoints, random, treeHeight, leaderX, leaderZ);
		foliagePoints.add(new BlockPos(leaderX, treeHeight - 1, leaderZ));

		List<PlannedLog> plannedLogs = logs.entrySet()
			.stream()
			.map(entry -> new PlannedLog(entry.getKey(), entry.getValue()))
			.toList();
		return new TreePlan(plannedLogs, List.copyOf(foliagePoints));
	}

	private static void addRoots(
		Map<BlockPos, Direction.Axis> logs,
		RandomSource random
	) {
		for (int x = 0; x < MAX_UPRIGHT_TRUNK_WIDTH; x++) {
			for (int z = 0; z < MAX_UPRIGHT_TRUNK_WIDTH; z++) {
				addLog(logs, new BlockPos(x, -1, z), Direction.Axis.Y);
			}
		}

		for (Direction direction : CARDINAL_DIRECTIONS) {
			int lane = random.nextInt(2);
			int surfaceReach = SURFACE_ROOT_REACH + random.nextInt(SURFACE_ROOT_VARIATION);
			int buriedReach = BURIED_ROOT_REACH + random.nextInt(BURIED_ROOT_VARIATION);
			BlockPos face = facePosition(direction, lane, 0);

			for (int distance = 1; distance <= surfaceReach; distance++) {
				addLog(
					logs,
					face.relative(direction, distance),
					direction.getAxis()
				);
			}
			for (int distance = 1; distance <= buriedReach; distance++) {
				addLog(
					logs,
					face.relative(direction, distance).below(),
					direction.getAxis()
				);
			}
			addLog(
				logs,
				face.relative(direction, buriedReach).below(2),
				Direction.Axis.Y
			);
		}
	}

	private static void addTaperedTrunk(
		Map<BlockPos, Direction.Axis> logs,
		int treeHeight,
		int leaderX,
		int leaderZ,
		Direction.Axis middleAxis
	) {
		int fullBoleTop = Math.max(BUTTRESS_TOP + 1, treeHeight * 11 / 20);
		int threeColumnBoleTop = Math.max(fullBoleTop + 3, treeHeight * 2 / 3);
		int twoColumnBoleTop = Math.max(threeColumnBoleTop + 4, treeHeight * 4 / 5);

		for (int y = 0; y < fullBoleTop; y++) {
			for (int x = 0; x < MAX_UPRIGHT_TRUNK_WIDTH; x++) {
				for (int z = 0; z < MAX_UPRIGHT_TRUNK_WIDTH; z++) {
					addLog(logs, new BlockPos(x, y, z), Direction.Axis.Y);
				}
			}
		}

		for (int y = fullBoleTop; y < threeColumnBoleTop; y++) {
			addLog(logs, new BlockPos(leaderX, y, leaderZ), Direction.Axis.Y);
			addLog(logs, new BlockPos(1 - leaderX, y, leaderZ), Direction.Axis.Y);
			addLog(logs, new BlockPos(leaderX, y, 1 - leaderZ), Direction.Axis.Y);
		}

		for (int y = threeColumnBoleTop; y < twoColumnBoleTop; y++) {
			addLog(logs, new BlockPos(leaderX, y, leaderZ), Direction.Axis.Y);
			BlockPos second = middleAxis == Direction.Axis.X
				? new BlockPos(1 - leaderX, y, leaderZ)
				: new BlockPos(leaderX, y, 1 - leaderZ);
			addLog(logs, second, Direction.Axis.Y);
		}

		for (int y = twoColumnBoleTop; y < treeHeight; y++) {
			addLog(logs, new BlockPos(leaderX, y, leaderZ), Direction.Axis.Y);
		}
	}

	private static void addCrown(
		Map<BlockPos, Direction.Axis> logs,
		Set<BlockPos> foliagePoints,
		RandomSource random,
		int treeHeight,
		int leaderX,
		int leaderZ
	) {
		int crownStart = Math.max(MINIMUM_CROWN_Y, treeHeight - CROWN_DEPTH);
		int crownEnd = treeHeight - CROWN_TOP_CLEARANCE;
		int crownSpan = Math.max(1, crownEnd - crownStart);
		int tier = 0;

		for (int y = crownStart; y <= crownEnd; y += TIER_SPACING) {
			Direction first = tier % 2 == 0 ? Direction.NORTH : Direction.EAST;
			if ((tier / 2) % 2 != 0) {
				first = first.getOpposite();
			}
			Direction second = first.getOpposite();
			int progress = y - crownStart;
			int taper = progress * (LOWER_BRANCH_REACH - UPPER_BRANCH_REACH) / crownSpan;
			int nominalReach = Math.max(UPPER_BRANCH_REACH, LOWER_BRANCH_REACH - taper);

			addBranch(
				logs,
				foliagePoints,
				random,
				new BlockPos(leaderX, y, leaderZ),
				first,
				nominalReach,
				tier
			);
			addBranch(
				logs,
				foliagePoints,
				random,
				new BlockPos(leaderX, y, leaderZ),
				second,
				nominalReach,
				tier + 1
			);
			foliagePoints.add(new BlockPos(leaderX, y, leaderZ));
			tier++;
		}
	}

	private static void addBranch(
		Map<BlockPos, Direction.Axis> logs,
		Set<BlockPos> foliagePoints,
		RandomSource random,
		BlockPos start,
		Direction direction,
		int nominalReach,
		int branchIndex
	) {
		int reach = Math.max(UPPER_BRANCH_REACH, nominalReach - random.nextInt(2));
		boolean droops = reach >= 5 && branchIndex % 3 != 2;
		int levelReach = droops ? reach - 1 : reach;
		BlockPos lastLevel = start;

		for (int distance = 1; distance <= levelReach; distance++) {
			lastLevel = start.relative(direction, distance);
			addLog(logs, lastLevel, direction.getAxis());
		}

		BlockPos tip = lastLevel;
		if (droops) {
			BlockPos drop = lastLevel.below();
			addLog(logs, drop, Direction.Axis.Y);
			tip = drop.relative(direction);
			addLog(logs, tip, direction.getAxis());
		}

		if (reach >= 5) {
			foliagePoints.add(start.relative(direction, reach / 2));
		}
		foliagePoints.add(tip);
	}

	private boolean canPlacePlan(WorldGenLevel level, BlockPos origin, TreePlan plan) {
		for (PlannedLog log : plan.logs()) {
			BlockPos pos = origin.offset(log.offset());
			BlockState state = level.getBlockState(pos);
			if (!level.getFluidState(pos).isEmpty() || state.is(BlockTags.LOGS)) {
				return false;
			}

			if (log.offset().getY() < 0) {
				if (!state.is(BlockTags.SUPPORTS_VEGETATION)) {
					return false;
				}
			} else if (!validTreePos(level, pos)) {
				return false;
			}
		}
		return true;
	}

	private static BlockPos facePosition(Direction direction, int lane, int y) {
		return switch (direction) {
			case NORTH -> new BlockPos(lane, y, 0);
			case SOUTH -> new BlockPos(lane, y, 1);
			case WEST -> new BlockPos(0, y, lane);
			case EAST -> new BlockPos(1, y, lane);
			default -> throw new IllegalArgumentException("Hemlock roots require a horizontal direction");
		};
	}

	private static void addLog(
		Map<BlockPos, Direction.Axis> logs,
		BlockPos offset,
		Direction.Axis axis
	) {
		logs.putIfAbsent(offset, axis);
	}

	private record PlannedLog(BlockPos offset, Direction.Axis axis) {
	}

	private record TreePlan(
		List<PlannedLog> logs,
		List<BlockPos> foliagePoints
	) {
	}
}
