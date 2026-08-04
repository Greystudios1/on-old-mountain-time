package net.beforetheblight.worldgen.feature.trunkplacer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import com.mojang.serialization.Codec;
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
 * Builds the two-by-two forest form of an American chestnut. The tree keeps a
 * long clear bole, then opens into four broad scaffold limbs. Its buttresses
 * continue into shallow, spreading roots instead of ending at a flat trunk
 * cylinder.
 *
 * <p>The complete tree is planned and checked before the first block changes.
 * This lets a failed mega-sapling attempt return without leaving partial logs
 * or preventing vanilla's sapling rollback.</p>
 */
public final class ForestChestnutTrunkPlacer extends TrunkPlacer {
	public static final MapCodec<ForestChestnutTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(
		instance -> trunkPlacerParts(instance)
			.and(Codec.BOOL.optionalFieldOf("ridge_terrain_adaptive", false)
				.forGetter(placer -> placer.ridgeTerrainAdaptive))
			.apply(instance, ForestChestnutTrunkPlacer::new)
	);

	private static final Direction[] CARDINAL_DIRECTIONS = {
		Direction.NORTH,
		Direction.EAST,
		Direction.SOUTH,
		Direction.WEST
	};
	private static final int MINIMUM_TREE_HEIGHT = 21;
	private static final int MINIMUM_BRANCH_Y = 15;
	private static final int CROWN_DEPTH = 8;
	private static final int BRANCH_REACH_MIN = 4;
	private static final int BRANCH_REACH_VARIATION = 2;
	private static final int BRANCH_RUN_BEFORE_RISE = 4;
	private static final int ROOT_REACH_MIN = 3;
	private static final int ROOT_REACH_VARIATION = 2;
	private static final int SURFACE_BUTTRESS_REACH_MIN = 1;
	private static final int SURFACE_BUTTRESS_REACH_VARIATION = 2;
	private static final int MAX_FOUNDATION_DROP = 4;
	private static final int MAX_FOUNDATION_RISE = 1;
	private static final int MAX_ROOT_DROP = 4;
	private static final int MAX_ROOT_RISE = 3;
	private static final int MIN_TERRAIN_ROOT_ARMS = 2;

	private final boolean ridgeTerrainAdaptive;

	public ForestChestnutTrunkPlacer(int baseHeight, int heightRandA, int heightRandB) {
		this(baseHeight, heightRandA, heightRandB, false);
	}

	public ForestChestnutTrunkPlacer(
		int baseHeight,
		int heightRandA,
		int heightRandB,
		boolean ridgeTerrainAdaptive
	) {
		super(baseHeight, heightRandA, heightRandB);
		this.ridgeTerrainAdaptive = ridgeTerrainAdaptive;
	}

	@Override
	protected TrunkPlacerType<?> type() {
		return ModTrunkPlacerTypes.FOREST_CHESTNUT_TRUNK_PLACER;
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

		if (ridgeTerrainAdaptive) {
			TreePlan plan = createTerrainAdaptivePlan(level, origin, random, treeHeight);
			if (plan == null || !canPlaceTerrainAdaptivePlan(level, origin, plan)) {
				return List.of();
			}

			placeLogs(level, trunkSetter, random, origin, configuration, plan.rootLogs());
			placeLogs(level, trunkSetter, random, origin, configuration, plan.trunkLogs());
			placeLogs(level, trunkSetter, random, origin, configuration, plan.branchLogs());
			return createFoliageAttachments(origin, treeHeight, plan.branchAttachments());
		}

		TreePlan plan = createPlan(random, treeHeight);
		if (!canPlacePlan(level, origin, plan)) {
			return List.of();
		}

		placeLogs(level, trunkSetter, random, origin, configuration, plan.rootLogs());
		placeLogs(level, trunkSetter, random, origin, configuration, plan.trunkLogs());
		placeLogs(level, trunkSetter, random, origin, configuration, plan.branchLogs());
		return createFoliageAttachments(origin, treeHeight, plan.branchAttachments());
	}

	private static TreePlan createPlan(RandomSource random, int treeHeight) {
		List<PlannedLog> rootLogs = new ArrayList<>();
		List<PlannedLog> trunkLogs = new ArrayList<>();
		List<PlannedLog> branchLogs = new ArrayList<>();
		List<BlockPos> branchAttachments = new ArrayList<>();
		List<BlockPos> untouchedSupports = new ArrayList<>();

		addTrunk(trunkLogs, treeHeight);
		addRoots(random, rootLogs, untouchedSupports);
		addBranches(random, treeHeight, branchLogs, branchAttachments);

		return new TreePlan(rootLogs, trunkLogs, branchLogs, branchAttachments, untouchedSupports);
	}

	private TreePlan createTerrainAdaptivePlan(
		WorldGenLevel level,
		BlockPos origin,
		RandomSource random,
		int treeHeight
	) {
		Map<BlockPos, Direction.Axis> rootLogs = new LinkedHashMap<>();
		List<PlannedLog> trunkLogs = new ArrayList<>();
		List<PlannedLog> branchLogs = new ArrayList<>();
		List<BlockPos> branchAttachments = new ArrayList<>();

		addTrunk(trunkLogs, treeHeight);
		if (!addTerrainAdaptiveFoundation(level, origin, rootLogs)) {
			return null;
		}
		if (addTerrainFollowingRoots(level, origin, random, rootLogs) < MIN_TERRAIN_ROOT_ARMS) {
			return null;
		}
		addBranches(random, treeHeight, branchLogs, branchAttachments);

		List<PlannedLog> plannedRoots = rootLogs.entrySet()
			.stream()
			.map(entry -> new PlannedLog(entry.getKey(), entry.getValue()))
			.toList();
		return new TreePlan(plannedRoots, trunkLogs, branchLogs, branchAttachments, List.of());
	}

	private boolean addTerrainAdaptiveFoundation(
		WorldGenLevel level,
		BlockPos origin,
		Map<BlockPos, Direction.Axis> rootLogs
	) {
		for (int x = 0; x <= 1; x++) {
			for (int z = 0; z <= 1; z++) {
				SurfaceSupport support = findSurfaceSupport(
					level,
					origin,
					x,
					z,
					MAX_FOUNDATION_DROP,
					MAX_FOUNDATION_RISE
				);
				if (support == null) {
					return false;
				}

				int surfaceAirY = support.relativeY() + 1;
				if (surfaceAirY < -MAX_FOUNDATION_DROP || surfaceAirY > MAX_FOUNDATION_RISE) {
					return false;
				}

				int lowestFoundationY = Math.min(-1, support.relativeY());
				for (int y = -1; y >= lowestFoundationY; y--) {
					putAdaptiveLog(rootLogs, new BlockPos(x, y, z), Direction.Axis.Y);
				}
			}
		}
		return true;
	}

	private int addTerrainFollowingRoots(
		WorldGenLevel level,
		BlockPos origin,
		RandomSource random,
		Map<BlockPos, Direction.Axis> rootLogs
	) {
		int viableArms = 0;
		for (Direction direction : CARDINAL_DIRECTIONS) {
			int preferredLane = random.nextInt(2);
			int preferredReach = ROOT_REACH_MIN + random.nextInt(ROOT_REACH_VARIATION);
			int preferredSurfaceReach = SURFACE_BUTTRESS_REACH_MIN
				+ random.nextInt(SURFACE_BUTTRESS_REACH_VARIATION);
			List<PlannedLog> arm = findTerrainFollowingRootArm(
				level,
				origin,
				direction,
				preferredLane,
				preferredReach,
				preferredSurfaceReach
			);
			if (arm == null) {
				arm = findTerrainFollowingRootArm(
					level,
					origin,
					direction,
					1 - preferredLane,
					preferredReach,
					preferredSurfaceReach
				);
			}
			if (arm == null && preferredReach > ROOT_REACH_MIN) {
				arm = findTerrainFollowingRootArm(
					level,
					origin,
					direction,
					preferredLane,
					ROOT_REACH_MIN,
					SURFACE_BUTTRESS_REACH_MIN
				);
			}
			if (arm == null && preferredReach > ROOT_REACH_MIN) {
				arm = findTerrainFollowingRootArm(
					level,
					origin,
					direction,
					1 - preferredLane,
					ROOT_REACH_MIN,
					SURFACE_BUTTRESS_REACH_MIN
				);
			}
			if (arm == null) {
				continue;
			}

			for (PlannedLog log : arm) {
				putAdaptiveLog(rootLogs, log.offset(), log.axis());
			}
			viableArms++;
		}
		return viableArms;
	}

	private List<PlannedLog> findTerrainFollowingRootArm(
		WorldGenLevel level,
		BlockPos origin,
		Direction direction,
		int lane,
		int reach,
		int surfaceReach
	) {
		Map<BlockPos, Direction.Axis> arm = new LinkedHashMap<>();
		BlockPos face = facePosition(direction, lane, 0);
		int previousRootY = 0;
		int terminalRootY = 0;

		for (int distance = 1; distance <= reach; distance++) {
			BlockPos column = face.relative(direction, distance);
			SurfaceSupport support = findSurfaceSupport(
				level,
				origin,
				column.getX(),
				column.getZ(),
				MAX_ROOT_DROP,
				MAX_ROOT_RISE
			);
			if (support == null) {
				return null;
			}

			int rootY = support.relativeY();
			int surfaceAirY = rootY + 1;
			if (surfaceAirY < -MAX_ROOT_DROP || surfaceAirY > MAX_ROOT_RISE) {
				return null;
			}
			int connectorStep = Integer.compare(rootY, previousRootY);
			for (int connectorY = previousRootY; connectorY != rootY; connectorY += connectorStep) {
				putAdaptiveLog(
					arm,
					new BlockPos(column.getX(), connectorY, column.getZ()),
					Direction.Axis.Y
				);
			}
			putAdaptiveLog(
				arm,
				new BlockPos(column.getX(), rootY, column.getZ()),
				direction.getAxis()
			);

			if (distance <= surfaceReach) {
				putAdaptiveLog(
					arm,
					new BlockPos(column.getX(), rootY + 1, column.getZ()),
					direction.getAxis()
				);
			}
			previousRootY = rootY;
			terminalRootY = rootY;
		}

		BlockPos terminal = face.relative(direction, reach);
		BlockPos terminalDrop = new BlockPos(terminal.getX(), terminalRootY - 1, terminal.getZ());
		BlockPos terminalDropWorld = absolutePosition(origin, terminalDrop);
		if (isDrySubstrate(level, terminalDropWorld)) {
			putAdaptiveLog(arm, terminalDrop, Direction.Axis.Y);
		}

		List<PlannedLog> plannedArm = arm.entrySet()
			.stream()
			.map(entry -> new PlannedLog(entry.getKey(), entry.getValue()))
			.toList();
		return canPlaceTerrainAdaptiveLogs(level, origin, plannedArm, new HashSet<>())
			? plannedArm
			: null;
	}

	private SurfaceSupport findSurfaceSupport(
		WorldGenLevel level,
		BlockPos origin,
		int relativeX,
		int relativeZ,
		int maximumDrop,
		int maximumRise
	) {
		BlockPos aboveSearch = origin.offset(relativeX, maximumRise + 1, relativeZ);
		BlockState aboveState = level.getBlockState(aboveSearch);
		if (!level.getFluidState(aboveSearch).isEmpty()
			|| aboveState.is(BlockTags.LOGS)
			|| !validTreePos(level, aboveSearch)) {
			return null;
		}

		for (int relativeY = maximumRise; relativeY >= -maximumDrop - 1; relativeY--) {
			BlockPos pos = origin.offset(relativeX, relativeY, relativeZ);
			BlockState state = level.getBlockState(pos);
			if (!level.getFluidState(pos).isEmpty() || state.is(BlockTags.LOGS)) {
				return null;
			}
			if (state.is(BlockTags.SUPPORTS_VEGETATION)) {
				return new SurfaceSupport(relativeY);
			}
			if (!validTreePos(level, pos)) {
				return null;
			}
		}
		return null;
	}

	private static void putAdaptiveLog(
		Map<BlockPos, Direction.Axis> logs,
		BlockPos offset,
		Direction.Axis axis
	) {
		logs.putIfAbsent(offset, axis);
	}

	private boolean canPlaceTerrainAdaptivePlan(WorldGenLevel level, BlockPos origin, TreePlan plan) {
		Set<BlockPos> plannedOffsets = new HashSet<>();
		return canPlaceTerrainAdaptiveLogs(level, origin, plan.rootLogs(), plannedOffsets)
			&& canPlaceTerrainAdaptiveLogs(level, origin, plan.trunkLogs(), plannedOffsets)
			&& canPlaceTerrainAdaptiveLogs(level, origin, plan.branchLogs(), plannedOffsets);
	}

	private boolean canPlaceTerrainAdaptiveLogs(
		WorldGenLevel level,
		BlockPos origin,
		List<PlannedLog> logs,
		Set<BlockPos> plannedOffsets
	) {
		for (PlannedLog log : logs) {
			if (!plannedOffsets.add(log.offset())) {
				return false;
			}

			BlockPos pos = absolutePosition(origin, log.offset());
			BlockState state = level.getBlockState(pos);
			if (!level.getFluidState(pos).isEmpty() || state.is(BlockTags.LOGS)) {
				return false;
			}
			if (!validTreePos(level, pos) && !state.is(BlockTags.SUPPORTS_VEGETATION)) {
				return false;
			}
		}
		return true;
	}

	private static boolean isDrySubstrate(WorldGenLevel level, BlockPos pos) {
		return level.getBlockState(pos).is(BlockTags.SUPPORTS_VEGETATION)
			&& level.getFluidState(pos).isEmpty();
	}

	private static void addTrunk(List<PlannedLog> trunkLogs, int treeHeight) {
		for (int y = 0; y < treeHeight; y++) {
			trunkLogs.add(new PlannedLog(new BlockPos(0, y, 0), Direction.Axis.Y));
			trunkLogs.add(new PlannedLog(new BlockPos(1, y, 0), Direction.Axis.Y));
			trunkLogs.add(new PlannedLog(new BlockPos(0, y, 1), Direction.Axis.Y));
			trunkLogs.add(new PlannedLog(new BlockPos(1, y, 1), Direction.Axis.Y));
		}
	}

	private static void addRoots(
		RandomSource random,
		List<PlannedLog> rootLogs,
		List<BlockPos> untouchedSupports
	) {
		for (int x = 0; x <= 1; x++) {
			for (int z = 0; z <= 1; z++) {
				rootLogs.add(new PlannedLog(new BlockPos(x, -1, z), Direction.Axis.Y));
			}
		}

		for (Direction direction : CARDINAL_DIRECTIONS) {
			int lane = random.nextInt(2);
			int rootReach = ROOT_REACH_MIN + random.nextInt(ROOT_REACH_VARIATION);
			int surfaceReach = SURFACE_BUTTRESS_REACH_MIN
				+ random.nextInt(SURFACE_BUTTRESS_REACH_VARIATION);
			BlockPos face = facePosition(direction, lane, 0);
			Direction.Axis rootAxis = direction.getAxis();

			for (int distance = 1; distance <= surfaceReach; distance++) {
				rootLogs.add(new PlannedLog(face.relative(direction, distance), rootAxis));
			}
			for (int distance = 1; distance <= rootReach; distance++) {
				rootLogs.add(new PlannedLog(face.relative(direction, distance).below(), rootAxis));
			}

			BlockPos terminal = face.relative(direction, rootReach).below(2);
			rootLogs.add(new PlannedLog(terminal, Direction.Axis.Y));
			untouchedSupports.add(terminal.below());
		}
	}

	private static void addBranches(
		RandomSource random,
		int treeHeight,
		List<PlannedLog> branchLogs,
		List<BlockPos> branchAttachments
	) {
		Direction[] branchOrder = CARDINAL_DIRECTIONS.clone();
		shuffleDirections(random, branchOrder);
		int firstBranchY = Math.max(MINIMUM_BRANCH_Y, treeHeight - CROWN_DEPTH);

		for (int branchIndex = 0; branchIndex < branchOrder.length; branchIndex++) {
			Direction direction = branchOrder[branchIndex];
			int branchY = firstBranchY + branchIndex;
			int lane = random.nextInt(2);
			int reach = BRANCH_REACH_MIN + random.nextInt(BRANCH_REACH_VARIATION);
			BlockPos face = facePosition(direction, lane, branchY);
			Direction.Axis branchAxis = direction.getAxis();

			for (int distance = 1; distance <= Math.min(reach, BRANCH_RUN_BEFORE_RISE); distance++) {
				branchLogs.add(new PlannedLog(face.relative(direction, distance), branchAxis));
			}

			BlockPos risenJoint = face.relative(direction, BRANCH_RUN_BEFORE_RISE).above();
			branchLogs.add(new PlannedLog(risenJoint, Direction.Axis.Y));
			for (int distance = BRANCH_RUN_BEFORE_RISE + 1; distance <= reach; distance++) {
				branchLogs.add(new PlannedLog(face.relative(direction, distance).above(), branchAxis));
			}

			BlockPos terminal = face.relative(direction, reach).above();
			Direction raisedSide = branchIndex % 2 == 0
				? direction.getClockWise()
				: direction.getCounterClockWise();
			Direction levelSide = raisedSide.getOpposite();
			BlockPos levelTip = terminal.relative(levelSide);
			BlockPos raisedJoint = terminal.above();
			BlockPos raisedTip = raisedJoint.relative(raisedSide);

			branchLogs.add(new PlannedLog(levelTip, levelSide.getAxis()));
			branchLogs.add(new PlannedLog(raisedJoint, Direction.Axis.Y));
			branchLogs.add(new PlannedLog(raisedTip, raisedSide.getAxis()));
			branchAttachments.add(levelTip);
			branchAttachments.add(raisedTip);
		}
	}

	private static BlockPos facePosition(Direction direction, int lane, int y) {
		return switch (direction) {
			case NORTH -> new BlockPos(lane, y, 0);
			case SOUTH -> new BlockPos(lane, y, 1);
			case WEST -> new BlockPos(0, y, lane);
			case EAST -> new BlockPos(1, y, lane);
			default -> throw new IllegalArgumentException("Chestnut roots and branches require a horizontal direction");
		};
	}

	private static void shuffleDirections(RandomSource random, Direction[] directions) {
		for (int index = directions.length - 1; index > 0; index--) {
			int swapIndex = random.nextInt(index + 1);
			Direction held = directions[index];
			directions[index] = directions[swapIndex];
			directions[swapIndex] = held;
		}
	}

	private boolean canPlacePlan(WorldGenLevel level, BlockPos origin, TreePlan plan) {
		Set<BlockPos> plannedOffsets = new HashSet<>();
		if (!canPlaceLogs(level, origin, plan.rootLogs(), plannedOffsets)
			|| !canPlaceLogs(level, origin, plan.trunkLogs(), plannedOffsets)
			|| !canPlaceLogs(level, origin, plan.branchLogs(), plannedOffsets)) {
			return false;
		}

		for (BlockPos supportOffset : plan.untouchedSupports()) {
			BlockPos supportPos = absolutePosition(origin, supportOffset);
			BlockState supportState = level.getBlockState(supportPos);
			if (!level.getFluidState(supportPos).isEmpty()
				|| !supportState.isFaceSturdy(level, supportPos, Direction.UP)) {
				return false;
			}
		}
		return true;
	}

	private boolean canPlaceLogs(
		WorldGenLevel level,
		BlockPos origin,
		List<PlannedLog> logs,
		Set<BlockPos> plannedOffsets
	) {
		for (PlannedLog log : logs) {
			if (!plannedOffsets.add(log.offset())) {
				return false;
			}

			BlockPos pos = absolutePosition(origin, log.offset());
			if (log.offset().getY() >= 0) {
				if (!validTreePos(level, pos) || !level.getFluidState(pos).isEmpty()) {
					return false;
				}
			} else {
				BlockState state = level.getBlockState(pos);
				if (!state.is(BlockTags.SUPPORTS_VEGETATION) || !level.getFluidState(pos).isEmpty()) {
					return false;
				}
			}
		}
		return true;
	}

	private static void placeLogs(
		WorldGenLevel level,
		BiConsumer<BlockPos, BlockState> trunkSetter,
		RandomSource random,
		BlockPos origin,
		TreeConfiguration configuration,
		List<PlannedLog> logs
	) {
		for (PlannedLog log : logs) {
			BlockPos pos = absolutePosition(origin, log.offset());
			BlockState state = configuration.trunkProvider
				.getState(level, random, pos)
				.trySetValue(RotatedPillarBlock.AXIS, log.axis());
			trunkSetter.accept(pos, state);
		}
	}

	private static List<FoliagePlacer.FoliageAttachment> createFoliageAttachments(
		BlockPos origin,
		int treeHeight,
		List<BlockPos> branchAttachments
	) {
		List<FoliagePlacer.FoliageAttachment> attachments = new ArrayList<>(branchAttachments.size() + 1);
		for (BlockPos attachmentOffset : branchAttachments) {
			attachments.add(new FoliagePlacer.FoliageAttachment(
				absolutePosition(origin, attachmentOffset),
				0,
				false
			));
		}
		attachments.add(new FoliagePlacer.FoliageAttachment(origin.above(treeHeight - 1), 0, true));
		return List.copyOf(attachments);
	}

	private static BlockPos absolutePosition(BlockPos origin, BlockPos offset) {
		return origin.offset(offset.getX(), offset.getY(), offset.getZ());
	}

	private record PlannedLog(BlockPos offset, Direction.Axis axis) {
	}

	private record SurfaceSupport(int relativeY) {
	}

	private record TreePlan(
		List<PlannedLog> rootLogs,
		List<PlannedLog> trunkLogs,
		List<PlannedLog> branchLogs,
		List<BlockPos> branchAttachments,
		List<BlockPos> untouchedSupports
	) {
		private TreePlan {
			rootLogs = List.copyOf(rootLogs);
			trunkLogs = List.copyOf(trunkLogs);
			branchLogs = List.copyOf(branchLogs);
			branchAttachments = List.copyOf(branchAttachments);
			untouchedSupports = List.copyOf(untouchedSupports);
		}
	}
}
