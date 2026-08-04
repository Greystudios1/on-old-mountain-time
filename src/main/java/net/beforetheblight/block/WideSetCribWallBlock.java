package net.beforetheblight.block;

import java.util.Map;

import com.mojang.serialization.MapCodec;
import net.beforetheblight.BeforeTheBlight;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
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
 * One widely spaced horizontal crib-wall course.
 *
 * <p>The four connection bits select true end, straight, corner, T, and cross
 * models. Course parity alternates which axis receives the thicker, slightly
 * higher pair of horizontal rails through a junction. The authored models
 * meet the central post and neighboring blocks without overlapping them.</p>
 */
public final class WideSetCribWallBlock extends Block implements SimpleWaterloggedBlock {
	public static final MapCodec<WideSetCribWallBlock> CODEC =
		simpleCodec(WideSetCribWallBlock::new);
	public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
	public static final BooleanProperty EAST = BlockStateProperties.EAST;
	public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
	public static final BooleanProperty WEST = BlockStateProperties.WEST;
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	public static final EnumProperty<Course> COURSE = EnumProperty.create("course", Course.class);
	public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = Map.of(
		Direction.NORTH, NORTH,
		Direction.EAST, EAST,
		Direction.SOUTH, SOUTH,
		Direction.WEST, WEST
	);
	public static final TagKey<Block> CONNECTORS = TagKey.create(
		Registries.BLOCK,
		BeforeTheBlight.id("corn_crib_wall_connectors")
	);

	private static final VoxelShape CORE = Block.box(6.0, 0.0, 6.0, 10.0, 16.0, 10.0);
	private static final VoxelShape ISOLATED_X =
		Block.box(0.0, 4.0, 5.0, 16.0, 12.0, 11.0);
	private static final VoxelShape ISOLATED_Z =
		Block.box(5.0, 4.0, 0.0, 11.0, 12.0, 16.0);
	private static final Map<Course, Map<Direction, VoxelShape>> ARMS = Map.of(
		Course.X_OVER_Z,
		buildArms(3.0, 6.0, 10.0, 13.0, 2.25, 4.75, 9.25, 11.75),
		Course.Z_OVER_X,
		buildArms(2.25, 4.75, 9.25, 11.75, 3.0, 6.0, 10.0, 13.0)
	);

	public WideSetCribWallBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(
			stateDefinition.any()
				.setValue(NORTH, false)
				.setValue(EAST, false)
				.setValue(SOUTH, false)
				.setValue(WEST, false)
				.setValue(WATERLOGGED, false)
				.setValue(COURSE, Course.X_OVER_Z)
		);
	}

	@Override
	public MapCodec<WideSetCribWallBlock> codec() {
		return CODEC;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockGetter level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState state = defaultBlockState()
			.setValue(COURSE, Course.atY(pos.getY()))
			.setValue(WATERLOGGED, level.getFluidState(pos).is(Fluids.WATER));
		for (Map.Entry<Direction, BooleanProperty> entry : PROPERTY_BY_DIRECTION.entrySet()) {
			state = state.setValue(
				entry.getValue(),
				connectsTo(level.getBlockState(pos.relative(entry.getKey())))
			);
		}
		return state;
	}

	@Override
	protected BlockState updateShape(
		BlockState state,
		LevelReader level,
		ScheduledTickAccess ticks,
		BlockPos pos,
		Direction direction,
		BlockPos neighborPos,
		BlockState neighborState,
		RandomSource random
	) {
		if (state.getValue(WATERLOGGED)) {
			ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}
		if (direction.getAxis().isHorizontal()) {
			return state.setValue(PROPERTY_BY_DIRECTION.get(direction), connectsTo(neighborState));
		}
		return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
	}

	@Override
	protected VoxelShape getShape(
		BlockState state,
		BlockGetter level,
		BlockPos pos,
		CollisionContext context
	) {
		VoxelShape shape = CORE;
		boolean connected = false;
		Map<Direction, VoxelShape> courseArms = ARMS.get(state.getValue(COURSE));
		for (Map.Entry<Direction, BooleanProperty> entry : PROPERTY_BY_DIRECTION.entrySet()) {
			if (state.getValue(entry.getValue())) {
				connected = true;
				shape = Shapes.or(shape, courseArms.get(entry.getKey()));
			}
		}
		if (connected) {
			return shape;
		}
		return state.getValue(COURSE) == Course.X_OVER_Z ? ISOLATED_X : ISOLATED_Z;
	}

	@Override
	protected VoxelShape getCollisionShape(
		BlockState state,
		BlockGetter level,
		BlockPos pos,
		CollisionContext context
	) {
		return getShape(state, level, pos, context);
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED)
			? Fluids.WATER.getSource(false)
			: super.getFluidState(state);
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return switch (rotation) {
			case CLOCKWISE_180 -> state
				.setValue(NORTH, state.getValue(SOUTH))
				.setValue(EAST, state.getValue(WEST))
				.setValue(SOUTH, state.getValue(NORTH))
				.setValue(WEST, state.getValue(EAST));
			case COUNTERCLOCKWISE_90 -> state
				.setValue(NORTH, state.getValue(EAST))
				.setValue(EAST, state.getValue(SOUTH))
				.setValue(SOUTH, state.getValue(WEST))
				.setValue(WEST, state.getValue(NORTH))
				.setValue(COURSE, state.getValue(COURSE).rotated());
			case CLOCKWISE_90 -> state
				.setValue(NORTH, state.getValue(WEST))
				.setValue(EAST, state.getValue(NORTH))
				.setValue(SOUTH, state.getValue(EAST))
				.setValue(WEST, state.getValue(SOUTH))
				.setValue(COURSE, state.getValue(COURSE).rotated());
			default -> state;
		};
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return switch (mirror) {
			case LEFT_RIGHT -> state
				.setValue(NORTH, state.getValue(SOUTH))
				.setValue(SOUTH, state.getValue(NORTH));
			case FRONT_BACK -> state
				.setValue(EAST, state.getValue(WEST))
				.setValue(WEST, state.getValue(EAST));
			default -> state;
		};
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(NORTH, EAST, SOUTH, WEST, WATERLOGGED, COURSE);
	}

	private static boolean connectsTo(BlockState neighbor) {
		return neighbor.getBlock() instanceof WideSetCribWallBlock
			|| neighbor.is(CONNECTORS);
	}

	private static Map<Direction, VoxelShape> buildArms(
		double xLowOne,
		double xHighOne,
		double xLowTwo,
		double xHighTwo,
		double zLowOne,
		double zHighOne,
		double zLowTwo,
		double zHighTwo
	) {
		return Map.of(
			Direction.NORTH,
			Shapes.or(
				Block.box(6.5, zLowOne, 0.0, 9.5, zHighOne, 6.0),
				Block.box(6.5, zLowTwo, 0.0, 9.5, zHighTwo, 6.0)
			),
			Direction.EAST,
			Shapes.or(
				Block.box(10.0, xLowOne, 6.5, 16.0, xHighOne, 9.5),
				Block.box(10.0, xLowTwo, 6.5, 16.0, xHighTwo, 9.5)
			),
			Direction.SOUTH,
			Shapes.or(
				Block.box(6.5, zLowOne, 10.0, 9.5, zHighOne, 16.0),
				Block.box(6.5, zLowTwo, 10.0, 9.5, zHighTwo, 16.0)
			),
			Direction.WEST,
			Shapes.or(
				Block.box(0.0, xLowOne, 6.5, 6.0, xHighOne, 9.5),
				Block.box(0.0, xLowTwo, 6.5, 6.0, xHighTwo, 9.5)
			)
		);
	}

	public enum Course implements StringRepresentable {
		X_OVER_Z("x_over_z"),
		Z_OVER_X("z_over_x");

		private final String serializedName;

		Course(String serializedName) {
			this.serializedName = serializedName;
		}

		public static Course atY(int y) {
			return Math.floorMod(y, 2) == 0 ? X_OVER_Z : Z_OVER_X;
		}

		public Course rotated() {
			return this == X_OVER_Z ? Z_OVER_X : X_OVER_Z;
		}

		@Override
		public String getSerializedName() {
			return serializedName;
		}
	}
}
