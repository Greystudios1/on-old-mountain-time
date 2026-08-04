package net.beforetheblight.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A native connected horizontal log course.
 *
 * <p>All three obtainable forms share connection semantics: the ordinary
 * straight course, a door-jamb termination, and a window-jamb termination.
 * Their distinct registry IDs keep the model graph readable while allowing
 * every form to connect to every other form. Placement never selects the
 * vertical axis, and stacked courses alternate automatically. Each model
 * owns one clay half-joint above and below its wood body, so two stacked
 * courses meet without an empty collision or rendering band. Perpendicular
 * neighbours add only the short arm needed to close their shared corner.</p>
 */
public final class ConnectedChestnutLogWallBlock
	extends Block
	implements SimpleWaterloggedBlock {
	public static final MapCodec<ConnectedChestnutLogWallBlock> PLAIN_CODEC =
		simpleCodec(ConnectedChestnutLogWallBlock::plain);
	public static final MapCodec<ConnectedChestnutLogWallBlock> DOOR_CODEC =
		simpleCodec(ConnectedChestnutLogWallBlock::door);
	public static final MapCodec<ConnectedChestnutLogWallBlock> WINDOW_CODEC =
		simpleCodec(ConnectedChestnutLogWallBlock::window);

	public static final EnumProperty<Direction.Axis> AXIS =
		BlockStateProperties.HORIZONTAL_AXIS;
	public static final EnumProperty<Course> COURSE =
		EnumProperty.create("course", Course.class);
	public static final BooleanProperty NEGATIVE_CONNECTED =
		BooleanProperty.create("negative_connected");
	public static final BooleanProperty POSITIVE_CONNECTED =
		BooleanProperty.create("positive_connected");
	public static final BooleanProperty NEGATIVE_PERPENDICULAR_CONNECTED =
		BooleanProperty.create("negative_perpendicular_connected");
	public static final BooleanProperty POSITIVE_PERPENDICULAR_CONNECTED =
		BooleanProperty.create("positive_perpendicular_connected");
	public static final BooleanProperty WATERLOGGED =
		BlockStateProperties.WATERLOGGED;
	private static final VoxelShape X_SHAPE =
		Block.box(0.0, 0.0, 2.0, 16.0, 16.0, 14.0);
	private static final VoxelShape X_NEGATIVE_PERPENDICULAR_ARM =
		Block.box(2.0, 0.0, 0.0, 14.0, 16.0, 2.0);
	private static final VoxelShape X_POSITIVE_PERPENDICULAR_ARM =
		Block.box(2.0, 0.0, 14.0, 14.0, 16.0, 16.0);
	private static final VoxelShape Z_SHAPE =
		Block.box(2.0, 0.0, 0.0, 14.0, 16.0, 16.0);
	private static final VoxelShape Z_NEGATIVE_PERPENDICULAR_ARM =
		Block.box(0.0, 0.0, 2.0, 2.0, 16.0, 14.0);
	private static final VoxelShape Z_POSITIVE_PERPENDICULAR_ARM =
		Block.box(14.0, 0.0, 2.0, 16.0, 16.0, 14.0);
	private static final VoxelShape[] X_SHAPES = connectionShapes(
		X_SHAPE,
		X_NEGATIVE_PERPENDICULAR_ARM,
		X_POSITIVE_PERPENDICULAR_ARM
	);
	private static final VoxelShape[] Z_SHAPES = connectionShapes(
		Z_SHAPE,
		Z_NEGATIVE_PERPENDICULAR_ARM,
		Z_POSITIVE_PERPENDICULAR_ARM
	);

	private final MapCodec<ConnectedChestnutLogWallBlock> wallCodec;
	private final Termination termination;

	private ConnectedChestnutLogWallBlock(
		BlockBehaviour.Properties properties,
		Termination termination,
		MapCodec<ConnectedChestnutLogWallBlock> wallCodec
	) {
		super(properties);
		this.termination = termination;
		this.wallCodec = wallCodec;
		this.registerDefaultState(
			this.stateDefinition
				.any()
				.setValue(AXIS, Direction.Axis.X)
				.setValue(COURSE, Course.LOWER)
				.setValue(NEGATIVE_CONNECTED, false)
				.setValue(POSITIVE_CONNECTED, false)
				.setValue(NEGATIVE_PERPENDICULAR_CONNECTED, false)
				.setValue(POSITIVE_PERPENDICULAR_CONNECTED, false)
				.setValue(WATERLOGGED, false)
		);
	}

	public static ConnectedChestnutLogWallBlock plain(
		BlockBehaviour.Properties properties
	) {
		return new ConnectedChestnutLogWallBlock(
			properties,
			Termination.PLAIN,
			PLAIN_CODEC
		);
	}

	public static ConnectedChestnutLogWallBlock door(
		BlockBehaviour.Properties properties
	) {
		return new ConnectedChestnutLogWallBlock(
			properties,
			Termination.DOOR,
			DOOR_CODEC
		);
	}

	public static ConnectedChestnutLogWallBlock window(
		BlockBehaviour.Properties properties
	) {
		return new ConnectedChestnutLogWallBlock(
			properties,
			Termination.WINDOW,
			WINDOW_CODEC
		);
	}

	@Override
	protected MapCodec<ConnectedChestnutLogWallBlock> codec() {
		return this.wallCodec;
	}

	public Termination termination() {
		return this.termination;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockPos pos = context.getClickedPos();
		Direction.Axis clickedAxis = context.getClickedFace().getAxis();
		Direction.Axis axis = clickedAxis.isHorizontal()
			? clickedAxis
			: context.getHorizontalDirection().getAxis();
		BlockState below = context.getLevel().getBlockState(pos.below());
		Course course =
			below.getBlock() instanceof ConnectedChestnutLogWallBlock
					&& below.getValue(AXIS) == axis
				? below.getValue(COURSE).opposite()
				: ((pos.getY() & 1) == 0 ? Course.LOWER : Course.UPPER);
		BlockState state = this.defaultBlockState()
			.setValue(AXIS, axis)
			.setValue(COURSE, course)
			.setValue(
				WATERLOGGED,
				context.getLevel().getFluidState(pos).is(Fluids.WATER)
			);
		return refreshConnections(state, context.getLevel(), pos);
	}

	@Override
	protected BlockState updateShape(
		BlockState state,
		LevelReader level,
		ScheduledTickAccess ticks,
		BlockPos pos,
		Direction directionToNeighbour,
		BlockPos neighbourPos,
		BlockState neighbourState,
		RandomSource random
	) {
		if (state.getValue(WATERLOGGED)) {
			ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}
		if (directionToNeighbour.getAxis().isHorizontal()) {
			return refreshConnections(state, level, pos);
		}
		return super.updateShape(
			state,
			level,
			ticks,
			pos,
			directionToNeighbour,
			neighbourPos,
			neighbourState,
			random
		);
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		Direction.Axis oldAxis = state.getValue(AXIS);
		if (oldAxis == Direction.Axis.Y || rotation == Rotation.NONE) {
			return state;
		}
		boolean negative = state.getValue(NEGATIVE_CONNECTED);
		boolean positive = state.getValue(POSITIVE_CONNECTED);
		boolean negativePerpendicular = state.getValue(
			NEGATIVE_PERPENDICULAR_CONNECTED
		);
		boolean positivePerpendicular = state.getValue(
			POSITIVE_PERPENDICULAR_CONNECTED
		);
		return switch (rotation) {
			case CLOCKWISE_180 -> state
				.setValue(NEGATIVE_CONNECTED, positive)
				.setValue(POSITIVE_CONNECTED, negative)
				.setValue(
					NEGATIVE_PERPENDICULAR_CONNECTED,
					positivePerpendicular
				)
				.setValue(
					POSITIVE_PERPENDICULAR_CONNECTED,
					negativePerpendicular
				);
			case CLOCKWISE_90 -> state
				.setValue(
					AXIS,
					oldAxis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X
				)
				.setValue(
					NEGATIVE_CONNECTED,
					oldAxis == Direction.Axis.X ? negative : positive
				)
				.setValue(
					POSITIVE_CONNECTED,
					oldAxis == Direction.Axis.X ? positive : negative
				)
				.setValue(
					NEGATIVE_PERPENDICULAR_CONNECTED,
					oldAxis == Direction.Axis.X
						? positivePerpendicular
						: negativePerpendicular
				)
				.setValue(
					POSITIVE_PERPENDICULAR_CONNECTED,
					oldAxis == Direction.Axis.X
						? negativePerpendicular
						: positivePerpendicular
				);
			case COUNTERCLOCKWISE_90 -> state
				.setValue(
					AXIS,
					oldAxis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X
				)
				.setValue(
					NEGATIVE_CONNECTED,
					oldAxis == Direction.Axis.X ? positive : negative
				)
				.setValue(
					POSITIVE_CONNECTED,
					oldAxis == Direction.Axis.X ? negative : positive
				)
				.setValue(
					NEGATIVE_PERPENDICULAR_CONNECTED,
					oldAxis == Direction.Axis.X
						? negativePerpendicular
						: positivePerpendicular
				)
				.setValue(
					POSITIVE_PERPENDICULAR_CONNECTED,
					oldAxis == Direction.Axis.X
						? positivePerpendicular
						: negativePerpendicular
				);
			default -> state;
		};
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		Direction.Axis axis = state.getValue(AXIS);
		boolean swapAlong =
			(mirror == Mirror.FRONT_BACK && axis == Direction.Axis.X)
				|| (mirror == Mirror.LEFT_RIGHT && axis == Direction.Axis.Z);
		boolean swapPerpendicular =
			(mirror == Mirror.LEFT_RIGHT && axis == Direction.Axis.X)
				|| (mirror == Mirror.FRONT_BACK && axis == Direction.Axis.Z);
		if (swapAlong) {
			boolean negative = state.getValue(NEGATIVE_CONNECTED);
			state = state
				.setValue(
					NEGATIVE_CONNECTED,
					state.getValue(POSITIVE_CONNECTED)
				)
				.setValue(POSITIVE_CONNECTED, negative);
		}
		if (swapPerpendicular) {
			boolean negative = state.getValue(
				NEGATIVE_PERPENDICULAR_CONNECTED
			);
			state = state
				.setValue(
					NEGATIVE_PERPENDICULAR_CONNECTED,
					state.getValue(POSITIVE_PERPENDICULAR_CONNECTED)
				)
				.setValue(POSITIVE_PERPENDICULAR_CONNECTED, negative);
		}
		return state;
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED)
			? Fluids.WATER.getSource(false)
			: super.getFluidState(state);
	}

	@Override
	protected VoxelShape getShape(
		BlockState state,
		BlockGetter level,
		BlockPos pos,
		CollisionContext context
	) {
		int shapeIndex =
			(state.getValue(NEGATIVE_PERPENDICULAR_CONNECTED) ? 1 : 0)
				| (state.getValue(POSITIVE_PERPENDICULAR_CONNECTED) ? 2 : 0);
		return state.getValue(AXIS) == Direction.Axis.Z
			? Z_SHAPES[shapeIndex]
			: X_SHAPES[shapeIndex];
	}

	@Override
	protected VoxelShape getCollisionShape(
		BlockState state,
		BlockGetter level,
		BlockPos pos,
		CollisionContext context
	) {
		return this.getShape(state, level, pos, context);
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}

	@Override
	protected void createBlockStateDefinition(
		StateDefinition.Builder<Block, BlockState> builder
	) {
		builder.add(
			AXIS,
			COURSE,
			NEGATIVE_CONNECTED,
			POSITIVE_CONNECTED,
			NEGATIVE_PERPENDICULAR_CONNECTED,
			POSITIVE_PERPENDICULAR_CONNECTED,
			WATERLOGGED
		);
	}

	private static BlockState refreshConnections(
		BlockState state,
		BlockGetter level,
		BlockPos pos
	) {
		Direction.Axis axis = state.getValue(AXIS);
		if (!axis.isHorizontal()) {
			return state
				.setValue(NEGATIVE_CONNECTED, false)
				.setValue(POSITIVE_CONNECTED, false)
				.setValue(NEGATIVE_PERPENDICULAR_CONNECTED, false)
				.setValue(POSITIVE_PERPENDICULAR_CONNECTED, false);
		}
		Direction negativeDirection = Direction.fromAxisAndDirection(
			axis,
			Direction.AxisDirection.NEGATIVE
		);
		Direction positiveDirection = negativeDirection.getOpposite();
		Direction.Axis perpendicularAxis = axis == Direction.Axis.X
			? Direction.Axis.Z
			: Direction.Axis.X;
		Direction negativePerpendicularDirection = Direction.fromAxisAndDirection(
			perpendicularAxis,
			Direction.AxisDirection.NEGATIVE
		);
		Direction positivePerpendicularDirection =
			negativePerpendicularDirection.getOpposite();
		return state
			.setValue(
				NEGATIVE_CONNECTED,
				connectsAtCourseEnd(
					level.getBlockState(pos.relative(negativeDirection))
				)
			)
			.setValue(
				POSITIVE_CONNECTED,
				connectsAtCourseEnd(
					level.getBlockState(pos.relative(positiveDirection))
				)
			)
			.setValue(
				NEGATIVE_PERPENDICULAR_CONNECTED,
				connectsPerpendicularly(
					level.getBlockState(
						pos.relative(negativePerpendicularDirection)
					),
					axis
				)
			)
			.setValue(
				POSITIVE_PERPENDICULAR_CONNECTED,
				connectsPerpendicularly(
					level.getBlockState(
						pos.relative(positivePerpendicularDirection)
					),
					axis
				)
			);
	}

	private static boolean connectsAtCourseEnd(BlockState neighbour) {
		return neighbour.getBlock() instanceof ConnectedChestnutLogWallBlock;
	}

	private static boolean connectsPerpendicularly(
		BlockState neighbour,
		Direction.Axis axis
	) {
		return neighbour.getBlock() instanceof ConnectedChestnutLogWallBlock
			&& neighbour.getValue(AXIS) != axis;
	}

	private static VoxelShape[] connectionShapes(
		VoxelShape center,
		VoxelShape negativeArm,
		VoxelShape positiveArm
	) {
		return new VoxelShape[] {
			center,
			Shapes.or(center, negativeArm),
			Shapes.or(center, positiveArm),
			Shapes.or(center, negativeArm, positiveArm),
		};
	}

	public enum Course implements StringRepresentable {
		LOWER("lower"),
		UPPER("upper");

		private final String serializedName;

		Course(String serializedName) {
			this.serializedName = serializedName;
		}

		public Course opposite() {
			return this == LOWER ? UPPER : LOWER;
		}

		@Override
		public String getSerializedName() {
			return this.serializedName;
		}
	}

	public enum Termination {
		PLAIN,
		DOOR,
		WINDOW
	}
}
