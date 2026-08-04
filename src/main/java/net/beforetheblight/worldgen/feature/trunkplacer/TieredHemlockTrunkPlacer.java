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
 * Builds the ordinary tall eastern-hemlock form around a one-block leader.
 *
 * <p>Alternating branch tiers create the species' narrow, tapering crown.
 * Every foliage attachment is an actual trunk or branch-tip log, including
 * the bounded drooping tips. The small set of plan variants changes the crown
 * orientation and lower-tier asymmetry without changing its height or radius
 * envelope.</p>
 */
public final class TieredHemlockTrunkPlacer extends TrunkPlacer {
	public static final MapCodec<TieredHemlockTrunkPlacer> CODEC =
		RecordCodecBuilder.mapCodec(
			instance -> trunkPlacerParts(instance).apply(
				instance,
				TieredHemlockTrunkPlacer::new
			)
		);

	private static final Direction[] CARDINAL_DIRECTIONS = {
		Direction.NORTH,
		Direction.EAST,
		Direction.SOUTH,
		Direction.WEST
	};
	public static final int MINIMUM_TREE_HEIGHT = 18;
	public static final int MINIMUM_CROWN_START = 8;
	public static final int CROWN_DEPTH = 14;
	public static final int CROWN_TOP_CLEARANCE = 3;
	public static final int TIER_SPACING = 2;
	public static final int LOWER_BRANCH_REACH = 3;
	public static final int UPPER_BRANCH_REACH = 2;
	public static final int MAX_LOG_RADIUS = LOWER_BRANCH_REACH;
	public static final int MAX_FOLIAGE_RADIUS = 5;

	public TieredHemlockTrunkPlacer(int baseHeight, int heightRandA, int heightRandB) {
		super(baseHeight, heightRandA, heightRandB);
	}

	@Override
	protected TrunkPlacerType<?> type() {
		return ModTrunkPlacerTypes.TIERED_HEMLOCK_TRUNK_PLACER;
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

		placeBelowTrunkBlock(level, trunkSetter, random, origin.below(), configuration);
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
		int rotation = random.nextInt(CARDINAL_DIRECTIONS.length);
		int asymmetry = random.nextInt(2);
		Map<BlockPos, Direction.Axis> logs = new LinkedHashMap<>();
		Set<BlockPos> foliagePoints = new LinkedHashSet<>();

		for (int y = 0; y < treeHeight; y++) {
			addLog(logs, new BlockPos(0, y, 0), Direction.Axis.Y);
		}

		int crownStart = Math.max(MINIMUM_CROWN_START, treeHeight - CROWN_DEPTH);
		int crownEnd = treeHeight - CROWN_TOP_CLEARANCE;
		int crownMidpoint = crownStart + (crownEnd - crownStart) / 2;
		int tier = 0;
		for (int y = crownStart; y <= crownEnd; y += TIER_SPACING) {
			Direction first = CARDINAL_DIRECTIONS[(rotation + tier) % CARDINAL_DIRECTIONS.length];
			Direction second = first.getOpposite();
			int nominalReach = y <= crownMidpoint
				? LOWER_BRANCH_REACH
				: UPPER_BRANCH_REACH;
			int secondReach = nominalReach;
			if (nominalReach > UPPER_BRANCH_REACH && (tier + asymmetry) % 2 == 0) {
				secondReach--;
			}

			BlockPos tierOrigin = new BlockPos(0, y, 0);
			addBranch(
				logs,
				foliagePoints,
				tierOrigin,
				first,
				nominalReach,
				shouldDroop(tier + asymmetry, nominalReach)
			);
			addBranch(
				logs,
				foliagePoints,
				tierOrigin,
				second,
				secondReach,
				shouldDroop(tier + asymmetry + 1, secondReach)
			);
			foliagePoints.add(tierOrigin);
			tier++;
		}

		foliagePoints.add(new BlockPos(0, treeHeight - 1, 0));
		List<PlannedLog> plannedLogs = logs.entrySet()
			.stream()
			.map(entry -> new PlannedLog(entry.getKey(), entry.getValue()))
			.toList();
		return new TreePlan(plannedLogs, List.copyOf(foliagePoints));
	}

	private boolean canPlacePlan(WorldGenLevel level, BlockPos origin, TreePlan plan) {
		for (PlannedLog log : plan.logs()) {
			BlockPos pos = origin.offset(log.offset());
			if (!level.getFluidState(pos).isEmpty()
				|| level.getBlockState(pos).is(BlockTags.LOGS)
				|| !validTreePos(level, pos)) {
				return false;
			}
		}
		return true;
	}

	private static boolean shouldDroop(int branchIndex, int reach) {
		return reach == LOWER_BRANCH_REACH && branchIndex % 3 == 0;
	}

	private static void addBranch(
		Map<BlockPos, Direction.Axis> logs,
		Set<BlockPos> foliagePoints,
		BlockPos start,
		Direction direction,
		int reach,
		boolean droops
	) {
		BlockPos tip = start;
		for (int distance = 1; distance <= reach; distance++) {
			tip = start.relative(direction, distance);
			addLog(logs, tip, direction.getAxis());
		}
		if (droops) {
			tip = tip.below();
			addLog(logs, tip, Direction.Axis.Y);
		}
		foliagePoints.add(tip);
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
