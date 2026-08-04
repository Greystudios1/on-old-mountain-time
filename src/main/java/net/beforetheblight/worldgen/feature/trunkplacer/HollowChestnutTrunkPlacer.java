package net.beforetheblight.worldgen.feature.trunkplacer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;

/**
 * Builds the rare three-by-three old-growth chestnut. One quarter of viable
 * trees receive a standing hollow; every form has a tapered bole, high scaffold
 * limbs, and buttress roots that spread through two soil layers.
 */
public final class HollowChestnutTrunkPlacer extends TrunkPlacer {
	public static final MapCodec<HollowChestnutTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(
		instance -> trunkPlacerParts(instance)
			.and(Codec.BOOL.optionalFieldOf("ridge_terrain_adaptive", false)
				.forGetter(placer -> placer.ridgeTerrainAdaptive))
			.apply(instance, HollowChestnutTrunkPlacer::new)
	);

	private static final Direction[] CARDINAL_DIRECTIONS = {
		Direction.NORTH,
		Direction.EAST,
		Direction.SOUTH,
		Direction.WEST
	};
	private static final int FOOTPRINT_RADIUS = 1;
	private static final int HOLLOW_HEIGHT = 2;
	private static final int HOLLOW_ROLL_BOUND = 4;
	private static final int ROOT_FLARE_TOP = 2;
	private static final int TOP_TAPER_HEIGHT = 8;
	private static final int CROWN_BASE_HEIGHT = 15;
	private static final int CROWN_DEPTH = 13;
	private static final int CROWN_TIER_COUNT = 4;
	private static final int CROWN_TIER_STEP = 3;
	private static final int MIN_SUPPORTED_TREE_HEIGHT = 28;
	private static final int MIN_BRANCH_LENGTH = 5;
	private static final int BRANCH_LENGTH_VARIATION = 2;
	private static final int MIN_ROOT_RUN = 3;
	private static final int ROOT_RUN_VARIATION = 3;
	private static final int ROOT_SURFACE_RADIUS = 2;
	private static final int MIN_CARDINAL_SPREAD_ARMS = 2;
	private static final int OUTSIDE_ENTRANCE_DISTANCE = 2;
	private static final int MAX_FOUNDATION_DROP = 4;
	private static final int MAX_ROOT_DROP = 4;
	private static final int MAX_ROOT_RISE = 3;
	private static final int CLEAR_WITHOUT_DROPS = Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS;

	private final boolean ridgeTerrainAdaptive;

	public HollowChestnutTrunkPlacer(int baseHeight, int heightRandA, int heightRandB) {
		this(baseHeight, heightRandA, heightRandB, false);
	}

	public HollowChestnutTrunkPlacer(
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
		return ModTrunkPlacerTypes.HOLLOW_CHESTNUT_TRUNK_PLACER;
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
		if (treeHeight < MIN_SUPPORTED_TREE_HEIGHT) {
			return List.of();
		}
		TrunkPlan plan = ridgeTerrainAdaptive
			? findTerrainAdaptivePlan(level, origin, treeHeight, random)
			: findPlan(level, origin, treeHeight, random);
		if (plan == null) {
			return List.of();
		}

		if (plan.hollow()) {
			clearHollowAndApproach(level, origin, plan.facing());
		}
		placePlannedLogs(level, trunkSetter, random, origin, configuration, plan.logs());
		return createFoliageAttachments(origin, plan.foliagePoints());
	}

	private TrunkPlan findPlan(WorldGenLevel level, BlockPos origin, int treeHeight, RandomSource random) {
		if (!canSupportSurfaceLogs(level, origin)) {
			return null;
		}

		boolean wantsHollow = random.nextInt(HOLLOW_ROLL_BOUND) == 0;
		List<RootSpec> rootSpecs = createRootSpecs(random);
		List<BranchSpec> branchSpecs = createBranchSpecs(treeHeight, random);
		List<TrunkPlan> candidates = collectViablePlans(
			level,
			origin,
			treeHeight,
			wantsHollow,
			rootSpecs,
			branchSpecs
		);

		if (candidates.isEmpty()) {
			candidates = collectViablePlans(
				level,
				origin,
				treeHeight,
				!wantsHollow,
				rootSpecs,
				branchSpecs
			);
		}

		return candidates.isEmpty() ? null : candidates.get(random.nextInt(candidates.size()));
	}

	private TrunkPlan findTerrainAdaptivePlan(
		WorldGenLevel level,
		BlockPos origin,
		int treeHeight,
		RandomSource random
	) {
		boolean wantsHollow = random.nextInt(HOLLOW_ROLL_BOUND) == 0;
		List<RootSpec> rootSpecs = createRootSpecs(random);
		List<BranchSpec> branchSpecs = createBranchSpecs(treeHeight, random);
		List<TrunkPlan> candidates = collectTerrainAdaptivePlans(
			level,
			origin,
			treeHeight,
			wantsHollow,
			rootSpecs,
			branchSpecs
		);

		if (candidates.isEmpty()) {
			candidates = collectTerrainAdaptivePlans(
				level,
				origin,
				treeHeight,
				!wantsHollow,
				rootSpecs,
				branchSpecs
			);
		}

		return candidates.isEmpty() ? null : candidates.get(random.nextInt(candidates.size()));
	}

	private List<TrunkPlan> collectTerrainAdaptivePlans(
		WorldGenLevel level,
		BlockPos origin,
		int treeHeight,
		boolean hollow,
		List<RootSpec> rootSpecs,
		List<BranchSpec> branchSpecs
	) {
		List<TrunkPlan> candidates = new ArrayList<>();
		for (Direction facing : CARDINAL_DIRECTIONS) {
			if (hollow && !canUseHollowAndApproach(level, origin, facing)) {
				continue;
			}

			Direction clockwise = facing.getClockWise();
			Direction counterClockwise = facing.getCounterClockWise();
			for (Direction perpendicular : List.of(clockwise, counterClockwise)) {
				TrunkPlan candidate = createTerrainAdaptivePlan(
					level,
					origin,
					treeHeight,
					hollow,
					facing,
					perpendicular,
					rootSpecs,
					branchSpecs
				);
				if (candidate != null && canPlaceTerrainAdaptivePlan(level, origin, candidate)) {
					candidates.add(candidate);
				}
			}
		}
		return candidates;
	}

	private List<TrunkPlan> collectViablePlans(
		WorldGenLevel level,
		BlockPos origin,
		int treeHeight,
		boolean hollow,
		List<RootSpec> rootSpecs,
		List<BranchSpec> branchSpecs
	) {
		List<TrunkPlan> candidates = new ArrayList<>();
		for (Direction facing : CARDINAL_DIRECTIONS) {
			if (hollow && !canUseHollowAndApproach(level, origin, facing)) {
				continue;
			}

			Direction clockwise = facing.getClockWise();
			Direction counterClockwise = facing.getCounterClockWise();
			for (Direction perpendicular : List.of(clockwise, counterClockwise)) {
				TrunkPlan candidate = createPlan(
					level,
					origin,
					treeHeight,
					hollow,
					facing,
					perpendicular,
					rootSpecs,
					branchSpecs
				);
				if (candidate != null && canPlacePlan(level, origin, candidate.logs())) {
					candidates.add(candidate);
				}
			}
		}
		return candidates;
	}

	private static List<RootSpec> createRootSpecs(RandomSource random) {
		List<RootSpec> specs = new ArrayList<>(CARDINAL_DIRECTIONS.length);
		for (Direction direction : CARDINAL_DIRECTIONS) {
			int run = MIN_ROOT_RUN + random.nextInt(ROOT_RUN_VARIATION);
			int toeOffset = random.nextInt(3) - 1;
			specs.add(new RootSpec(direction, run, toeOffset));
		}
		return List.copyOf(specs);
	}

	private static List<BranchSpec> createBranchSpecs(int treeHeight, RandomSource random) {
		List<BranchSpec> specs = new ArrayList<>(CROWN_TIER_COUNT);
		int crownBase = Math.max(CROWN_BASE_HEIGHT, treeHeight - CROWN_DEPTH);
		for (int tier = 0; tier < CROWN_TIER_COUNT; tier++) {
			int y = crownBase + tier * CROWN_TIER_STEP;
			int length = MIN_BRANCH_LENGTH + random.nextInt(BRANCH_LENGTH_VARIATION);
			specs.add(new BranchSpec(y, length, tier));
		}
		return List.copyOf(specs);
	}

	private TrunkPlan createPlan(
		WorldGenLevel level,
		BlockPos origin,
		int treeHeight,
		boolean hollow,
		Direction facing,
		Direction perpendicular,
		List<RootSpec> rootSpecs,
		List<BranchSpec> branchSpecs
	) {
		Map<RelativePos, Direction.Axis> logs = new LinkedHashMap<>();
		addCoreRoots(logs);
		addTrunk(logs, treeHeight, hollow, facing, perpendicular);
		int spreadArms = addAdaptiveRoots(level, origin, logs, rootSpecs, hollow, facing);
		if (spreadArms < MIN_CARDINAL_SPREAD_ARMS) {
			return null;
		}
		List<FoliagePoint> foliagePoints = addBranches(
			logs,
			branchSpecs,
			facing,
			perpendicular
		);
		foliagePoints.add(topFoliagePoint(treeHeight, facing, perpendicular));

		List<LogPlacement> placements = logs.entrySet()
			.stream()
			.map(entry -> new LogPlacement(entry.getKey(), entry.getValue()))
			.toList();
		return new TrunkPlan(
			hollow,
			facing,
			List.copyOf(placements),
			List.copyOf(foliagePoints)
		);
	}

	private TrunkPlan createTerrainAdaptivePlan(
		WorldGenLevel level,
		BlockPos origin,
		int treeHeight,
		boolean hollow,
		Direction facing,
		Direction perpendicular,
		List<RootSpec> rootSpecs,
		List<BranchSpec> branchSpecs
	) {
		Map<RelativePos, Direction.Axis> logs = new LinkedHashMap<>();
		if (!addTerrainAdaptiveFoundation(level, origin, logs)) {
			return null;
		}

		addTrunk(logs, treeHeight, hollow, facing, perpendicular);
		int spreadArms = addTerrainFollowingRoots(
			level,
			origin,
			logs,
			rootSpecs,
			hollow,
			facing
		);
		if (spreadArms < MIN_CARDINAL_SPREAD_ARMS) {
			return null;
		}

		List<FoliagePoint> foliagePoints = addBranches(
			logs,
			branchSpecs,
			facing,
			perpendicular
		);
		foliagePoints.add(topFoliagePoint(treeHeight, facing, perpendicular));

		List<LogPlacement> placements = logs.entrySet()
			.stream()
			.map(entry -> new LogPlacement(entry.getKey(), entry.getValue()))
			.toList();
		return new TrunkPlan(
			hollow,
			facing,
			List.copyOf(placements),
			List.copyOf(foliagePoints)
		);
	}

	private boolean addTerrainAdaptiveFoundation(
		WorldGenLevel level,
		BlockPos origin,
		Map<RelativePos, Direction.Axis> logs
	) {
		for (int dx = -FOOTPRINT_RADIUS; dx <= FOOTPRINT_RADIUS; dx++) {
			for (int dz = -FOOTPRINT_RADIUS; dz <= FOOTPRINT_RADIUS; dz++) {
				SurfaceSupport support = findSurfaceSupport(
					level,
					origin,
					dx,
					dz,
					-1,
					MAX_FOUNDATION_DROP
				);
				if (support == null) {
					return false;
				}

				for (int y = -1; y >= support.relativeY(); y--) {
					addLog(logs, dx, y, dz, Direction.Axis.Y);
				}
			}
		}
		return true;
	}

	private int addTerrainFollowingRoots(
		WorldGenLevel level,
		BlockPos origin,
		Map<RelativePos, Direction.Axis> logs,
		List<RootSpec> rootSpecs,
		boolean hollow,
		Direction entrance
	) {
		int viableArms = 0;
		for (RootSpec spec : rootSpecs) {
			List<LogPlacement> arm = findTerrainFollowingRootArm(
				level,
				origin,
				spec.direction(),
				spec.toeOffset(),
				spec.run(),
				hollow,
				entrance
			);
			if (arm == null && spec.toeOffset() != 0) {
				arm = findTerrainFollowingRootArm(
					level,
					origin,
					spec.direction(),
					0,
					spec.run(),
					hollow,
					entrance
				);
			}
			if (arm == null && spec.run() > MIN_ROOT_RUN) {
				arm = findTerrainFollowingRootArm(
					level,
					origin,
					spec.direction(),
					spec.toeOffset(),
					MIN_ROOT_RUN,
					hollow,
					entrance
				);
			}
			if (arm == null && spec.run() > MIN_ROOT_RUN && spec.toeOffset() != 0) {
				arm = findTerrainFollowingRootArm(
					level,
					origin,
					spec.direction(),
					0,
					MIN_ROOT_RUN,
					hollow,
					entrance
				);
			}
			if (arm == null) {
				continue;
			}

			for (LogPlacement placement : arm) {
				RelativePos pos = placement.pos();
				addLog(logs, pos.dx(), pos.dy(), pos.dz(), placement.axis());
			}
			viableArms++;
		}
		return viableArms;
	}

	private List<LogPlacement> findTerrainFollowingRootArm(
		WorldGenLevel level,
		BlockPos origin,
		Direction direction,
		int sideOffset,
		int run,
		boolean hollow,
		Direction entrance
	) {
		Map<RelativePos, Direction.Axis> arm = new LinkedHashMap<>();
		Direction side = direction.getClockWise();
		int terminalRadius = FOOTPRINT_RADIUS + run;
		int previousRootY = -1;

		for (int radius = ROOT_SURFACE_RADIUS; radius <= terminalRadius; radius++) {
			int dx = direction.getStepX() * radius + side.getStepX() * sideOffset;
			int dz = direction.getStepZ() * radius + side.getStepZ() * sideOffset;
			SurfaceSupport support = findSurfaceSupport(
				level,
				origin,
				dx,
				dz,
				MAX_ROOT_RISE,
				MAX_ROOT_DROP
			);
			if (support == null) {
				return null;
			}

			int rootY = support.relativeY();
			int connectorStep = Integer.compare(rootY, previousRootY);
			for (int y = previousRootY; y != rootY; y += connectorStep) {
				RelativePos connector = new RelativePos(dx, y, dz);
				if (isHollowClearancePosition(connector, hollow, entrance)) {
					return null;
				}
				putAdaptiveLog(arm, connector, Direction.Axis.Y);
			}

			RelativePos surfaceRoot = new RelativePos(dx, rootY, dz);
			if (isHollowClearancePosition(surfaceRoot, hollow, entrance)) {
				return null;
			}
			putAdaptiveLog(arm, surfaceRoot, direction.getAxis());

			if (radius == ROOT_SURFACE_RADIUS) {
				RelativePos exposedButtress = new RelativePos(dx, rootY + 1, dz);
				if (!isHollowClearancePosition(exposedButtress, hollow, entrance)
					&& canPlaceTerrainAdaptiveLog(level, origin, exposedButtress)) {
					putAdaptiveLog(arm, exposedButtress, direction.getAxis());
				}
			}
			previousRootY = rootY;
		}

		List<LogPlacement> plannedArm = arm.entrySet()
			.stream()
			.map(entry -> new LogPlacement(entry.getKey(), entry.getValue()))
			.toList();
		return canPlaceTerrainAdaptiveLogs(level, origin, plannedArm) ? plannedArm : null;
	}

	private SurfaceSupport findSurfaceSupport(
		WorldGenLevel level,
		BlockPos origin,
		int dx,
		int dz,
		int highestSupportY,
		int maximumDrop
	) {
		BlockPos aboveSearch = origin.offset(dx, highestSupportY + 1, dz);
		if (!canPassThroughForAdaptiveRoot(level, aboveSearch)) {
			return null;
		}

		int lowestSupportY = -1 - maximumDrop;
		for (int y = highestSupportY; y >= lowestSupportY; y--) {
			BlockPos pos = origin.offset(dx, y, dz);
			BlockState state = level.getBlockState(pos);
			if (!level.getFluidState(pos).isEmpty() || state.is(BlockTags.LOGS)) {
				return null;
			}
			if (state.is(BlockTags.SUPPORTS_VEGETATION)) {
				return new SurfaceSupport(y);
			}
			if (!validTreePos(level, pos)) {
				return null;
			}
		}
		return null;
	}

	private boolean canPassThroughForAdaptiveRoot(WorldGenLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return level.getFluidState(pos).isEmpty()
			&& !state.is(BlockTags.LOGS)
			&& validTreePos(level, pos);
	}

	private static void putAdaptiveLog(
		Map<RelativePos, Direction.Axis> logs,
		RelativePos pos,
		Direction.Axis axis
	) {
		logs.putIfAbsent(pos, axis);
	}

	private boolean canPlaceTerrainAdaptivePlan(
		WorldGenLevel level,
		BlockPos origin,
		TrunkPlan plan
	) {
		if (plan.hollow() && plan.logs().stream().anyMatch(
			placement -> isHollowClearancePosition(placement.pos(), true, plan.facing())
		)) {
			return false;
		}
		return canPlaceTerrainAdaptiveLogs(level, origin, plan.logs());
	}

	private boolean canPlaceTerrainAdaptiveLogs(
		WorldGenLevel level,
		BlockPos origin,
		List<LogPlacement> logs
	) {
		for (LogPlacement placement : logs) {
			if (!canPlaceTerrainAdaptiveLog(level, origin, placement.pos())) {
				return false;
			}
		}
		return true;
	}

	private boolean canPlaceTerrainAdaptiveLog(
		WorldGenLevel level,
		BlockPos origin,
		RelativePos relativePos
	) {
		BlockPos pos = origin.offset(relativePos.dx(), relativePos.dy(), relativePos.dz());
		BlockState state = level.getBlockState(pos);
		return level.getFluidState(pos).isEmpty()
			&& !state.is(BlockTags.LOGS)
			&& (state.is(BlockTags.SUPPORTS_VEGETATION) || validTreePos(level, pos));
	}

	private static boolean isHollowClearancePosition(
		RelativePos pos,
		boolean hollow,
		Direction entrance
	) {
		if (!hollow || pos.dy() < 0 || pos.dy() >= HOLLOW_HEIGHT) {
			return false;
		}
		for (int distance = 0; distance <= OUTSIDE_ENTRANCE_DISTANCE; distance++) {
			if (pos.dx() == entrance.getStepX() * distance
				&& pos.dz() == entrance.getStepZ() * distance) {
				return true;
			}
		}
		return false;
	}

	private static void addCoreRoots(Map<RelativePos, Direction.Axis> logs) {
		addLog(logs, 0, -1, 0, Direction.Axis.Y);
		for (Direction direction : CARDINAL_DIRECTIONS) {
			addLog(logs, direction.getStepX(), -1, direction.getStepZ(), direction.getAxis());
		}
	}

	private int addAdaptiveRoots(
		WorldGenLevel level,
		BlockPos origin,
		Map<RelativePos, Direction.Axis> logs,
		List<RootSpec> rootSpecs,
		boolean hollow,
		Direction entrance
	) {
		int spreadArms = 0;
		for (RootSpec spec : rootSpecs) {
			Direction direction = spec.direction();
			Direction side = direction.getClockWise();
			int dx = direction.getStepX() * ROOT_SURFACE_RADIUS;
			int dz = direction.getStepZ() * ROOT_SURFACE_RADIUS;

			tryAddOptionalRoot(
				level,
				origin,
				logs,
				new RelativePos(dx + side.getStepX(), 0, dz + side.getStepZ()),
				direction.getAxis()
			);
			tryAddOptionalRoot(
				level,
				origin,
				logs,
				new RelativePos(dx - side.getStepX(), 0, dz - side.getStepZ()),
				direction.getAxis()
			);
			if (!hollow || direction != entrance) {
				tryAddOptionalRoot(
					level,
					origin,
					logs,
					new RelativePos(dx, 0, dz),
					direction.getAxis()
				);
			}

			if (addAxialRootPrefix(level, origin, logs, spec)) {
				spreadArms++;
			}
		}
		return spreadArms;
	}

	private boolean addAxialRootPrefix(
		WorldGenLevel level,
		BlockPos origin,
		Map<RelativePos, Direction.Axis> logs,
		RootSpec spec
	) {
		Direction direction = spec.direction();
		Direction side = direction.getClockWise();
		int terminalRadius = spec.run() + FOOTPRINT_RADIUS;
		int lastSurfaceRadius = spec.run() == MIN_ROOT_RUN
			? terminalRadius
			: terminalRadius - 1;
		boolean spreadOutsideFootprint = false;

		for (int radius = ROOT_SURFACE_RADIUS; radius <= lastSurfaceRadius; radius++) {
			RelativePos pos = new RelativePos(
				direction.getStepX() * radius,
				-1,
				direction.getStepZ() * radius
			);
			if (!tryAddOptionalRoot(level, origin, logs, pos, direction.getAxis())) {
				return spreadOutsideFootprint;
			}
			spreadOutsideFootprint = true;
		}

		int dropRadius = spec.run() == MIN_ROOT_RUN ? terminalRadius : terminalRadius - 1;
		RelativePos drop = new RelativePos(
			direction.getStepX() * dropRadius,
			-2,
			direction.getStepZ() * dropRadius
		);
		if (!tryAddOptionalRoot(level, origin, logs, drop, Direction.Axis.Y)) {
			return spreadOutsideFootprint;
		}

		if (spec.run() != MIN_ROOT_RUN) {
			RelativePos terminal = new RelativePos(
				direction.getStepX() * terminalRadius,
				-2,
				direction.getStepZ() * terminalRadius
			);
			if (!tryAddOptionalRoot(level, origin, logs, terminal, direction.getAxis())) {
				return spreadOutsideFootprint;
			}
		}

		if (spec.toeOffset() != 0) {
			int toeY = spec.run() == MIN_ROOT_RUN ? -1 : -2;
			tryAddOptionalRoot(
				level,
				origin,
				logs,
				new RelativePos(
					direction.getStepX() * terminalRadius + side.getStepX() * spec.toeOffset(),
					toeY,
					direction.getStepZ() * terminalRadius + side.getStepZ() * spec.toeOffset()
				),
				side.getAxis()
			);
		}
		return spreadOutsideFootprint;
	}

	private boolean tryAddOptionalRoot(
		WorldGenLevel level,
		BlockPos origin,
		Map<RelativePos, Direction.Axis> logs,
		RelativePos relativePos,
		Direction.Axis axis
	) {
		if (logs.containsKey(relativePos)) {
			return false;
		}

		BlockPos pos = origin.offset(relativePos.dx(), relativePos.dy(), relativePos.dz());
		BlockState state = level.getBlockState(pos);
		if (state.is(BlockTags.LOGS)) {
			return false;
		}

		boolean replaceable = relativePos.dy() < 0
			? isDrySubstrate(level, pos)
			: validTreePos(level, pos) && level.getFluidState(pos).isEmpty();
		if (!replaceable
			|| relativePos.dy() == 0 && !isDrySubstrate(level, pos.below())) {
			return false;
		}

		logs.put(relativePos, axis);
		return true;
	}

	private static void addTrunk(
		Map<RelativePos, Direction.Axis> logs,
		int treeHeight,
		boolean hollow,
		Direction facing,
		Direction perpendicular
	) {
		for (int y = 0; y < treeHeight; y++) {
			for (int dx = -FOOTPRINT_RADIUS; dx <= FOOTPRINT_RADIUS; dx++) {
				for (int dz = -FOOTPRINT_RADIUS; dz <= FOOTPRINT_RADIUS; dz++) {
					if (shouldPlaceTrunkLog(dx, dz, y, treeHeight, hollow, facing, perpendicular)) {
						addLog(logs, dx, y, dz, Direction.Axis.Y);
					}
				}
			}
		}
	}

	private static List<FoliagePoint> addBranches(
		Map<RelativePos, Direction.Axis> logs,
		List<BranchSpec> branchSpecs,
		Direction facing,
		Direction perpendicular
	) {
		Direction[] directions = {
			facing,
			perpendicular,
			facing.getOpposite(),
			perpendicular.getOpposite()
		};
		List<FoliagePoint> foliagePoints = new ArrayList<>(branchSpecs.size() * 2);

		for (BranchSpec spec : branchSpecs) {
			Direction direction = directions[spec.tier() % directions.length];
			Direction thickSide = spec.tier() % 2 == 0
				? direction.getClockWise()
				: direction.getCounterClockWise();
			int dx = 0;
			int dz = 0;
			int y = spec.y();

			for (int step = 1; step <= spec.length(); step++) {
				dx += direction.getStepX();
				dz += direction.getStepZ();
				addLog(logs, dx, y, dz, direction.getAxis());
				if (step <= 2) {
					addLog(
						logs,
						dx + thickSide.getStepX(),
						y,
						dz + thickSide.getStepZ(),
						direction.getAxis()
					);
				}
				if (step % 4 == 0 && step < spec.length()) {
					y++;
					addLog(logs, dx, y, dz, Direction.Axis.Y);
				}
			}

			Direction leftFork = direction.getClockWise();
			Direction rightFork = direction.getCounterClockWise();
			boolean raiseLeftFork = spec.tier() % 2 == 0;
			RelativePos leftTip = new RelativePos(
				dx + leftFork.getStepX(),
				y + (raiseLeftFork ? 1 : 0),
				dz + leftFork.getStepZ()
			);
			RelativePos rightTip = new RelativePos(
				dx + rightFork.getStepX(),
				y + (raiseLeftFork ? 0 : 1),
				dz + rightFork.getStepZ()
			);
			addLog(logs, dx, y + 1, dz, Direction.Axis.Y);
			addLog(logs, leftTip.dx(), leftTip.dy(), leftTip.dz(), leftFork.getAxis());
			addLog(logs, rightTip.dx(), rightTip.dy(), rightTip.dz(), rightFork.getAxis());
			foliagePoints.add(new FoliagePoint(leftTip, false));
			foliagePoints.add(new FoliagePoint(rightTip, false));
		}
		return foliagePoints;
	}

	private static boolean shouldPlaceTrunkLog(
		int dx,
		int dz,
		int y,
		int treeHeight,
		boolean hollow,
		Direction facing,
		Direction perpendicular
	) {
		if (hollow && y < HOLLOW_HEIGHT) {
			return !isOffset(dx, dz, 0, 0)
				&& !isOffset(dx, dz, facing.getStepX(), facing.getStepZ());
		}
		if (y <= ROOT_FLARE_TOP) {
			return true;
		}
		if (y >= treeHeight - TOP_TAPER_HEIGHT) {
			return isTopTaperOffset(dx, dz, facing, perpendicular);
		}
		return Math.abs(dx) + Math.abs(dz) <= 1;
	}

	private static boolean isTopTaperOffset(
		int dx,
		int dz,
		Direction facing,
		Direction perpendicular
	) {
		int awayX = -facing.getStepX();
		int awayZ = -facing.getStepZ();
		int sideX = perpendicular.getStepX();
		int sideZ = perpendicular.getStepZ();
		return isOffset(dx, dz, 0, 0)
			|| isOffset(dx, dz, awayX, awayZ)
			|| isOffset(dx, dz, sideX, sideZ)
			|| isOffset(dx, dz, awayX + sideX, awayZ + sideZ);
	}

	private static boolean isOffset(int dx, int dz, int expectedX, int expectedZ) {
		return dx == expectedX && dz == expectedZ;
	}

	private static void addLog(
		Map<RelativePos, Direction.Axis> logs,
		int dx,
		int dy,
		int dz,
		Direction.Axis axis
	) {
		logs.putIfAbsent(new RelativePos(dx, dy, dz), axis);
	}

	private static boolean canSupportSurfaceLogs(WorldGenLevel level, BlockPos origin) {
		for (int dx = -FOOTPRINT_RADIUS; dx <= FOOTPRINT_RADIUS; dx++) {
			for (int dz = -FOOTPRINT_RADIUS; dz <= FOOTPRINT_RADIUS; dz++) {
				if (!isDrySubstrate(level, origin.offset(dx, -1, dz))) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean canUseHollowAndApproach(WorldGenLevel level, BlockPos origin, Direction entrance) {
		for (int distance = 0; distance <= OUTSIDE_ENTRANCE_DISTANCE; distance++) {
			if (!canClearForPlayer(level, origin.relative(entrance, distance))) {
				return false;
			}
		}
		return true;
	}

	private boolean canClearForPlayer(WorldGenLevel level, BlockPos feetPos) {
		return canClearForPlayerAt(level, feetPos) && canClearForPlayerAt(level, feetPos.above());
	}

	private boolean canClearForPlayerAt(WorldGenLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return validTreePos(level, pos)
			&& level.getFluidState(pos).isEmpty()
			&& state.getCollisionShape(level, pos).isEmpty();
	}

	private boolean canPlacePlan(WorldGenLevel level, BlockPos origin, List<LogPlacement> logs) {
		for (LogPlacement placement : logs) {
			BlockPos pos = origin.offset(placement.pos().dx(), placement.pos().dy(), placement.pos().dz());
			if (placement.pos().dy() < 0) {
				if (!isDrySubstrate(level, pos)) {
					return false;
				}
			} else if (!validTreePos(level, pos) || !level.getFluidState(pos).isEmpty()) {
				return false;
			}

			if (placement.pos().dy() == 0 && !isDrySubstrate(level, pos.below())) {
				return false;
			}
		}
		return true;
	}

	private static boolean isDrySubstrate(WorldGenLevel level, BlockPos pos) {
		return level.getBlockState(pos).is(BlockTags.SUPPORTS_VEGETATION)
			&& level.getFluidState(pos).isEmpty();
	}

	private static void clearHollowAndApproach(WorldGenLevel level, BlockPos origin, Direction entrance) {
		for (int distance = 0; distance <= OUTSIDE_ENTRANCE_DISTANCE; distance++) {
			BlockPos feetPos = origin.relative(entrance, distance);
			for (int y = 0; y < HOLLOW_HEIGHT; y++) {
				clearWithoutDrops(level, feetPos.above(y));
			}
		}
	}

	private static void clearWithoutDrops(WorldGenLevel level, BlockPos pos) {
		if (!level.getBlockState(pos).isAir()) {
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), CLEAR_WITHOUT_DROPS);
		}
	}

	private static void placePlannedLogs(
		WorldGenLevel level,
		BiConsumer<BlockPos, BlockState> trunkSetter,
		RandomSource random,
		BlockPos origin,
		TreeConfiguration configuration,
		List<LogPlacement> logs
	) {
		for (LogPlacement placement : logs) {
			BlockPos pos = origin.offset(placement.pos().dx(), placement.pos().dy(), placement.pos().dz());
			BlockState state = configuration.trunkProvider
				.getState(level, random, pos)
				.trySetValue(RotatedPillarBlock.AXIS, placement.axis());
			trunkSetter.accept(pos, state);
		}
	}

	private static List<FoliagePlacer.FoliageAttachment> createFoliageAttachments(
		BlockPos origin,
		List<FoliagePoint> foliagePoints
	) {
		return foliagePoints.stream()
			.map(point -> new FoliagePlacer.FoliageAttachment(
				origin.offset(point.pos().dx(), point.pos().dy(), point.pos().dz()),
				0,
				point.doubleTrunk()
			))
			.toList();
	}

	private static FoliagePoint topFoliagePoint(
		int treeHeight,
		Direction facing,
		Direction perpendicular
	) {
		int awayX = -facing.getStepX();
		int awayZ = -facing.getStepZ();
		int sideX = perpendicular.getStepX();
		int sideZ = perpendicular.getStepZ();
		int minX = Math.min(Math.min(0, awayX), Math.min(sideX, awayX + sideX));
		int minZ = Math.min(Math.min(0, awayZ), Math.min(sideZ, awayZ + sideZ));
		return new FoliagePoint(new RelativePos(minX, treeHeight - 1, minZ), true);
	}

	private record RelativePos(int dx, int dy, int dz) {
	}

	private record LogPlacement(RelativePos pos, Direction.Axis axis) {
	}

	private record FoliagePoint(RelativePos pos, boolean doubleTrunk) {
	}

	private record SurfaceSupport(int relativeY) {
	}

	private record RootSpec(Direction direction, int run, int toeOffset) {
	}

	private record BranchSpec(int y, int length, int tier) {
	}

	private record TrunkPlan(
		boolean hollow,
		Direction facing,
		List<LogPlacement> logs,
		List<FoliagePoint> foliagePoints
	) {
	}
}
