package net.beforetheblight.worldgen.feature;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mojang.serialization.Codec;
import net.beforetheblight.worldgen.feature.configurations.HollowFallenLogConfiguration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.TreeFeature;

/**
 * Places a rare old-growth deadfall with a short open rot pocket.
 *
 * <p>The lower centre log is an unbroken fall-axis spine. Contiguous shoulder
 * runs and a crown cover one deliberately declared two- or three-section rot
 * pocket, then taper monotonically to a one-log distal end. A stripped
 * fall-axis splinter joins that bole face-to-face to the jagged top of a rooted
 * vertical stump, so the break reads clearly without leaving diagonal or
 * floating fragments. The complete final core plan, including bounded buried
 * contacts on a one-block grade, is preflighted before any block or replaceable
 * pocket content changes.</p>
 */
public final class HollowFallenLogFeature extends Feature<HollowFallenLogConfiguration> {
	public static final int OUTER_DIAMETER = 3;
	public static final int MIN_ROT_POCKET_LENGTH = 2;
	public static final int MAX_ROT_POCKET_LENGTH = 3;
	/** @deprecated Use {@link #MIN_ROT_POCKET_LENGTH}. */
	@Deprecated
	public static final int MIN_PASSAGE_LENGTH = MIN_ROT_POCKET_LENGTH;
	/** @deprecated Use {@link #MAX_ROT_POCKET_LENGTH}. */
	@Deprecated
	public static final int MAX_PASSAGE_LENGTH = MAX_ROT_POCKET_LENGTH;
	private static final int OUTER_RADIUS = 1;
	private static final int ROT_POCKET_START = 1;
	private static final int SHELL_HEIGHT = 3;
	private static final int STUMP_OFFSET = -2;
	private static final int ROOT_TOE_OFFSET = -3;
	private static final int BREAK_GAP_OFFSET = -1;
	private static final int BLOCK_UPDATE_FLAGS = 19;

	public HollowFallenLogFeature(Codec<HollowFallenLogConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<HollowFallenLogConfiguration> context) {
		HollowFallenLogConfiguration configuration = context.config();
		WorldGenLevel level = context.level();
		RandomSource random = context.random();
		int length = configuration.logLength().sample(random);
		Direction.Axis axis = random.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z;
		Direction lengthDirection = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
		Direction crossDirection = axis == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
		int openingSide = random.nextBoolean() ? 1 : -1;
		BlockPos unlevelledStart = context.origin().relative(lengthDirection, -(length / 2));
		int baseY = fitBaseHeight(level, unlevelledStart, lengthDirection, crossDirection, length);
		if (baseY == Integer.MIN_VALUE || baseY + SHELL_HEIGHT > level.getMaxY()) {
			return false;
		}

		BlockPos start = new BlockPos(unlevelledStart.getX(), baseY, unlevelledStart.getZ());
		BodyLayout layout = createBodyLayout(length, axis, openingSide);
		List<PlannedLog> plannedLogs = buildFinalLogPlan(
			configuration,
			random,
			start,
			lengthDirection,
			crossDirection,
			axis,
			openingSide,
			layout
		);
		plannedLogs = addGroundingSupports(
			level,
			plannedLogs,
			start,
			lengthDirection,
			crossDirection,
			axis,
			layout,
			configuration.maximumUnsupportedSections()
		);
		if (plannedLogs == null) {
			return false;
		}
		List<BlockPos> clearances = absoluteClearances(
			start,
			lengthDirection,
			crossDirection,
			layout
		);
		if (!canApplyAtomically(level, plannedLogs, clearances)) {
			return false;
		}

		clearReplaceableVolume(level, clearances);
		Set<BlockPos> placedPositions = new HashSet<>();
		for (PlannedLog plannedLog : plannedLogs) {
			BlockState logState = (
				plannedLog.stripped()
					? configuration.strippedTrunkProvider()
					: configuration.trunkProvider()
			)
				.getState(level, random, plannedLog.pos())
				.trySetValue(RotatedPillarBlock.AXIS, plannedLog.axis());
			level.setBlock(plannedLog.pos(), logState, BLOCK_UPDATE_FLAGS);
			this.markAboveForPostProcessing(level, plannedLog.pos());
			placedPositions.add(plannedLog.pos());
		}
		placeSurfaceCover(
			configuration,
			level,
			random,
			plannedLogs,
			placedPositions,
			Set.copyOf(clearances)
		);
		return true;
	}

	/**
	 * Returns the complete undecayed body silhouette relative to the first body
	 * section. It excludes the separate stump and uses the canonical positive
	 * side opening so GameTests can lock the continuous spine, declared pocket,
	 * and monotonic taper contract.
	 */
	public static List<BlockPos> shellOffsets(int length, Direction.Axis axis) {
		return createBodyLayout(length, axis, 1)
			.logs()
			.stream()
			.map(BodyLog::offset)
			.toList();
	}

	/**
	 * Returns every required air block in the open rot pocket. The pocket cuts
	 * through one lower shoulder and the adjacent upper core while leaving the
	 * fall-axis spine intact.
	 */
	public static List<BlockPos> rotPocketOffsets(int length, Direction.Axis axis) {
		return createBodyLayout(length, axis, 1).rotPocketClearances();
	}

	public static int rotPocketStart(int length) {
		validateGeometryArguments(length, Direction.Axis.X);
		return ROT_POCKET_START;
	}

	public static int rotPocketLength(int length) {
		validateGeometryArguments(length, Direction.Axis.X);
		return length >= 10 ? MAX_ROT_POCKET_LENGTH : MIN_ROT_POCKET_LENGTH;
	}

	/** @deprecated Use {@link #rotPocketOffsets(int, Direction.Axis)}. */
	@Deprecated
	public static List<BlockPos> passageOffsets(int length, Direction.Axis axis) {
		return rotPocketOffsets(length, axis);
	}

	/** @deprecated Use {@link #rotPocketStart(int)}. */
	@Deprecated
	public static int passageStart(int length) {
		return rotPocketStart(length);
	}

	/** @deprecated Use {@link #rotPocketLength(int)}. */
	@Deprecated
	public static int passageLength(int length) {
		return rotPocketLength(length);
	}

	private static BodyLayout createBodyLayout(
		int length,
		Direction.Axis axis,
		int openingSide
	) {
		validateGeometryArguments(length, axis);
		if (openingSide != -1 && openingSide != 1) {
			throw new IllegalArgumentException("Opening side must be -1 or 1");
		}
		Direction lengthDirection = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
		Direction crossDirection = axis == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
		int pocketStart = rotPocketStart(length);
		int pocketLength = rotPocketLength(length);
		int pocketEnd = pocketStart + pocketLength - 1;
		int closureSection = pocketEnd + 1;
		int heavyLowerEnd = Math.max(closureSection, length - 3);
		int lightLowerEnd = Math.max(closureSection, length - 4);
		int upperCoreEnd = Math.min(length - 2, closureSection + 1);
		int heavySide = -openingSide;
		List<BodyLog> logs = new ArrayList<>();
		List<BlockPos> clearances = new ArrayList<>();
		List<BlockPos> rotPocketClearances = new ArrayList<>();

		for (int section = 0; section < length; section++) {
			BlockPos sectionBase = BlockPos.ZERO.relative(lengthDirection, section);
			boolean inRotPocket = section >= pocketStart && section <= pocketEnd;
			addBodyLog(logs, sectionBase, crossDirection, 0, 0, true);
			if (section <= heavyLowerEnd) {
				addBodyLog(logs, sectionBase, crossDirection, heavySide, 0, true);
			}
			if (section <= lightLowerEnd && !inRotPocket) {
				addBodyLog(logs, sectionBase, crossDirection, openingSide, 0, true);
			}
			if (section <= upperCoreEnd && !inRotPocket) {
				addBodyLog(
					logs,
					sectionBase,
					crossDirection,
					0,
					1,
					true,
					section == 0
				);
			}
			if (section <= closureSection) {
				addBodyLog(logs, sectionBase, crossDirection, heavySide, 1, true);
			}
			if (section == 0) {
				addBodyLog(
					logs,
					sectionBase,
					crossDirection,
					openingSide,
					1,
					true,
					true
				);
			}
			if (section <= pocketEnd) {
				addBodyLog(
					logs,
					sectionBase,
					crossDirection,
					0,
					2,
					true,
					section == 0
				);
			}
			if (section == 0) {
				addBodyLog(
					logs,
					sectionBase,
					crossDirection,
					heavySide,
					2,
					false,
					true
				);
			}
			if (inRotPocket) {
				BlockPos lowerMouth = sectionBase.relative(crossDirection, openingSide);
				rotPocketClearances.add(lowerMouth);
				rotPocketClearances.add(lowerMouth.above());
				rotPocketClearances.add(sectionBase.above());
			}
		}

		BlockPos breakGap = BlockPos.ZERO.relative(lengthDirection, BREAK_GAP_OFFSET);
		for (int y = 0; y < 2; y++) {
			clearances.add(breakGap.above(y));
		}
		clearances.addAll(rotPocketClearances);
		return new BodyLayout(
			List.copyOf(logs),
			List.copyOf(clearances),
			List.copyOf(rotPocketClearances),
			pocketStart,
			pocketLength,
			openingSide
		);
	}

	private static void addBodyLog(
		List<BodyLog> logs,
		BlockPos base,
		Direction crossDirection,
		int sideways,
		int vertical,
		boolean structural
	) {
		addBodyLog(logs, base, crossDirection, sideways, vertical, structural, false);
	}

	private static void addBodyLog(
		List<BodyLog> logs,
		BlockPos base,
		Direction crossDirection,
		int sideways,
		int vertical,
		boolean structural,
		boolean stripped
	) {
		logs.add(
			new BodyLog(
				base.relative(crossDirection, sideways).above(vertical),
				structural,
				stripped
			)
		);
	}

	private static List<PlannedLog> buildFinalLogPlan(
		HollowFallenLogConfiguration configuration,
		RandomSource random,
		BlockPos start,
		Direction lengthDirection,
		Direction crossDirection,
		Direction.Axis fallAxis,
		int openingSide,
		BodyLayout layout
	) {
		List<PlannedLog> plannedLogs = new ArrayList<>();
		for (BodyLog bodyLog : layout.logs()) {
			if (
				!bodyLog.structural()
					&& random.nextFloat() < configuration.shellDecayChance()
			) {
				continue;
			}
			plannedLogs.add(
				new PlannedLog(
					start.offset(bodyLog.offset()),
					fallAxis,
					bodyLog.stripped()
				)
			);
		}

		BlockPos stumpBase = start.relative(lengthDirection, STUMP_OFFSET);
		plannedLogs.add(new PlannedLog(stumpBase, Direction.Axis.Y, false));
		plannedLogs.add(new PlannedLog(stumpBase.above(), Direction.Axis.Y, false));
		plannedLogs.add(new PlannedLog(stumpBase.above(2), Direction.Axis.Y, true));
		plannedLogs.add(
			new PlannedLog(
				stumpBase.relative(lengthDirection).above(2),
				fallAxis,
				true
			)
		);
		plannedLogs.add(
			new PlannedLog(
				stumpBase.relative(crossDirection, openingSide),
				crossDirection.getAxis(),
				false
			)
		);
		plannedLogs.add(
			new PlannedLog(
				stumpBase.relative(crossDirection, -openingSide),
				crossDirection.getAxis(),
				false
			)
		);
		plannedLogs.add(
			new PlannedLog(
				stumpBase.relative(crossDirection, openingSide).above(),
				Direction.Axis.Y,
				true
			)
		);
		for (int side : new int[] {-1, 1}) {
			plannedLogs.add(
				new PlannedLog(
					stumpBase
						.relative(lengthDirection, -1)
						.relative(crossDirection, side),
					lengthDirection.getAxis(),
					false
				)
			);
		}

		int branchSection = layout.rotPocketStart() + layout.rotPocketLength();
		BlockPos branchBase = start
			.relative(lengthDirection, branchSection)
			.relative(crossDirection, -openingSide)
			.above(2);
		plannedLogs.add(new PlannedLog(branchBase, crossDirection.getAxis(), false));
		plannedLogs.add(
			new PlannedLog(
				branchBase.relative(crossDirection, -openingSide),
				crossDirection.getAxis(),
				true
			)
		);
		return List.copyOf(plannedLogs);
	}

	private static List<PlannedLog> addGroundingSupports(
		WorldGenLevel level,
		List<PlannedLog> initialPlan,
		BlockPos start,
		Direction lengthDirection,
		Direction crossDirection,
		Direction.Axis fallAxis,
		BodyLayout layout,
		int maximumUnsupportedSections
	) {
		List<PlannedLog> groundedPlan = new ArrayList<>(initialPlan);
		Set<BlockPos> occupied = new HashSet<>();
		for (PlannedLog plannedLog : initialPlan) {
			occupied.add(plannedLog.pos());
		}

		int unsupportedRun = 0;
		int bodyLength = maximumLongitudinalOffset(layout.logs(), lengthDirection) + 1;
		for (int section = 0; section < bodyLength; section++) {
			boolean hasBodyBase = false;
			boolean hasDirectContact = false;
			for (BodyLog bodyLog : layout.logs()) {
				if (
					bodyLog.offset().getY() != 0
						|| longitudinalOffset(bodyLog.offset(), lengthDirection) != section
				) {
					continue;
				}
				BlockPos bodyPos = start.offset(bodyLog.offset());
				if (!occupied.contains(bodyPos)) {
					continue;
				}
				hasBodyBase = true;
				GroundContact contact = ensureGroundContact(
					level,
					bodyPos,
					fallAxis,
					groundedPlan,
					occupied
				);
				if (contact == GroundContact.INVALID) {
					return null;
				}
				hasDirectContact |= contact == GroundContact.DIRECT;
			}
			if (!hasBodyBase) {
				return null;
			}
			unsupportedRun = hasDirectContact ? 0 : unsupportedRun + 1;
			if (unsupportedRun > maximumUnsupportedSections) {
				return null;
			}
		}

		for (PlannedLog plannedLog : List.copyOf(groundedPlan)) {
			if (
				plannedLog.pos().getY() == start.getY()
					&& ensureGroundContact(
						level,
						plannedLog.pos(),
						plannedLog.axis(),
						groundedPlan,
						occupied
					) == GroundContact.INVALID
			) {
				return null;
			}
		}
		for (int section = layout.rotPocketStart();
			section < layout.rotPocketStart() + layout.rotPocketLength();
			section++
		) {
			if (
				ensureGroundContact(
					level,
					start.relative(lengthDirection, section),
					fallAxis,
					groundedPlan,
					occupied
				) == GroundContact.INVALID
			) {
				return null;
			}
		}
		if (
			ensureGroundContact(
				level,
				start
					.relative(
						lengthDirection,
						layout.rotPocketStart() + layout.rotPocketLength() / 2
					)
					.relative(crossDirection, layout.openingSide()),
				fallAxis,
				groundedPlan,
				occupied
			) == GroundContact.INVALID
		) {
			return null;
		}
		return List.copyOf(groundedPlan);
	}

	private static GroundContact ensureGroundContact(
		WorldGenLevel level,
		BlockPos basePos,
		Direction.Axis axis,
		List<PlannedLog> groundedPlan,
		Set<BlockPos> occupied
	) {
		BlockPos below = basePos.below();
		if (isNaturalSupport(level.getBlockState(below))) {
			return GroundContact.DIRECT;
		}
		if (
			occupied.contains(below)
				|| (
					canReplaceWithoutFlooding(level, below)
						&& isNaturalSupport(level.getBlockState(below.below()))
				)
		) {
			if (occupied.add(below)) {
				groundedPlan.add(new PlannedLog(below, axis, false));
			}
			return GroundContact.EMBEDDED;
		}
		return GroundContact.INVALID;
	}

	private static List<BlockPos> absoluteClearances(
		BlockPos start,
		Direction lengthDirection,
		Direction crossDirection,
		BodyLayout layout
	) {
		List<BlockPos> result = new ArrayList<>(layout.clearances().size());
		for (BlockPos relative : layout.clearances()) {
			int along = lengthDirection.getAxis() == Direction.Axis.X
				? relative.getX()
				: relative.getZ();
			int sideways = crossDirection.getAxis() == Direction.Axis.X
				? relative.getX()
				: relative.getZ();
			result.add(
				start
					.relative(lengthDirection, along)
					.relative(crossDirection, sideways)
					.above(relative.getY())
			);
		}
		return List.copyOf(result);
	}

	private static int fitBaseHeight(
		WorldGenLevel level,
		BlockPos start,
		Direction lengthDirection,
		Direction crossDirection,
		int length
	) {
		int minimum = Integer.MAX_VALUE;
		int maximum = Integer.MIN_VALUE;
		for (int section = ROOT_TOE_OFFSET; section < length; section++) {
			BlockPos sectionBase = start.relative(lengthDirection, section);
			for (int sideways = -OUTER_RADIUS; sideways <= OUTER_RADIUS; sideways++) {
				BlockPos sample = sectionBase.relative(crossDirection, sideways);
				int height = findNearbyNaturalSurface(level, sample);
				if (height == Integer.MIN_VALUE) {
					return Integer.MIN_VALUE;
				}
				minimum = Math.min(minimum, height);
				maximum = Math.max(maximum, height);
			}
		}
		return maximum - minimum <= 1 ? maximum : Integer.MIN_VALUE;
	}

	private static int maximumLongitudinalOffset(
		List<BodyLog> logs,
		Direction lengthDirection
	) {
		int maximum = 0;
		for (BodyLog log : logs) {
			maximum = Math.max(
				maximum,
				longitudinalOffset(log.offset(), lengthDirection)
			);
		}
		return maximum;
	}

	private static int longitudinalOffset(
		BlockPos offset,
		Direction lengthDirection
	) {
		return lengthDirection.getAxis() == Direction.Axis.X
			? offset.getX()
			: offset.getZ();
	}

	private static int findNearbyNaturalSurface(WorldGenLevel level, BlockPos sample) {
		for (int y = sample.getY() + 2; y >= sample.getY() - 3; y--) {
			BlockPos candidate = new BlockPos(sample.getX(), y, sample.getZ());
			if (
				canReplaceWithoutFlooding(level, candidate)
					&& isNaturalSupport(level.getBlockState(candidate.below()))
			) {
				return y;
			}
		}
		return Integer.MIN_VALUE;
	}

	private static boolean canApplyAtomically(
		WorldGenLevel level,
		List<PlannedLog> plannedLogs,
		List<BlockPos> clearances
	) {
		Set<BlockPos> occupied = new HashSet<>();
		for (PlannedLog plannedLog : plannedLogs) {
			if (
				!occupied.add(plannedLog.pos())
					|| !canReplaceWithoutFlooding(level, plannedLog.pos())
			) {
				return false;
			}
		}
		for (BlockPos clearance : clearances) {
			if (
				occupied.contains(clearance)
					|| !canReplaceWithoutFlooding(level, clearance)
			) {
				return false;
			}
			for (Direction direction : Direction.values()) {
				if (!level.getFluidState(clearance.relative(direction)).isEmpty()) {
					return false;
				}
			}
		}
		return true;
	}

	private static boolean isNaturalSupport(BlockState state) {
		return state.is(BlockTags.DIRT)
			|| state.is(Blocks.GRASS_BLOCK)
			|| state.is(Blocks.PODZOL)
			|| state.is(BlockTags.BASE_STONE_OVERWORLD)
			|| state.is(Blocks.MOSS_BLOCK)
			|| state.is(Blocks.GRAVEL);
	}

	private static boolean canReplaceWithoutFlooding(WorldGenLevel level, BlockPos pos) {
		return level.isInsideBuildHeight(pos)
			&& level.getFluidState(pos).isEmpty()
			&& (
				!level.getBlockState(pos).hasProperty(LeavesBlock.PERSISTENT)
					|| !level.getBlockState(pos).getValue(LeavesBlock.PERSISTENT)
			)
			&& TreeFeature.validTreePos(level, pos);
	}

	private static void clearReplaceableVolume(
		WorldGenLevel level,
		List<BlockPos> clearances
	) {
		for (BlockPos clearance : clearances) {
			if (!level.getBlockState(clearance).isAir()) {
				level.setBlock(clearance, Blocks.AIR.defaultBlockState(), BLOCK_UPDATE_FLAGS);
			}
		}
	}

	private static void placeSurfaceCover(
		HollowFallenLogConfiguration configuration,
		WorldGenLevel level,
		RandomSource random,
		List<PlannedLog> plannedLogs,
		Set<BlockPos> placedPositions,
		Set<BlockPos> clearancePositions
	) {
		for (PlannedLog plannedLog : plannedLogs) {
			// Keep pale stump and splinter faces readable.
			BlockPos coverPos = plannedLog.pos().above();
			if (
				plannedLog.stripped()
					|| placedPositions.contains(coverPos)
					|| clearancePositions.contains(coverPos)
					|| random.nextFloat() >= configuration.surfaceCoverChance()
					|| !level.getBlockState(coverPos).isAir()
					|| !level.getFluidState(coverPos).isEmpty()
			) {
				continue;
			}
			BlockState coverState = configuration.surfaceCoverProvider()
				.getState(level, random, coverPos);
			if (coverState.canSurvive(level, coverPos)) {
				level.setBlock(coverPos, coverState, BLOCK_UPDATE_FLAGS);
			}
		}
	}

	private static void validateGeometryArguments(int length, Direction.Axis axis) {
		if (length < 5 || axis == Direction.Axis.Y) {
			throw new IllegalArgumentException(
				"An old-growth deadfall needs a horizontal axis and length of at least five"
			);
		}
	}

	private record BodyLog(BlockPos offset, boolean structural, boolean stripped) {
	}

	private record BodyLayout(
		List<BodyLog> logs,
		List<BlockPos> clearances,
		List<BlockPos> rotPocketClearances,
		int rotPocketStart,
		int rotPocketLength,
		int openingSide
	) {
	}

	private record PlannedLog(BlockPos pos, Direction.Axis axis, boolean stripped) {
	}

	private enum GroundContact {
		DIRECT,
		EMBEDDED,
		INVALID
	}
}
