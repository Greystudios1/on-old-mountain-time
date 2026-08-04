package net.beforetheblight.block;

import java.util.Map;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A half-block-wide squared timber that can be placed along any axis.
 *
 * <p>The full hewn beam remains the heavy structural member. This post is a
 * deliberately smaller detailing piece for porch posts, braces, loft framing,
 * crib framing, and similar timber work. Its collision and outline match the
 * visible eight-by-eight-pixel cross-section.</p>
 */
public final class HewnChestnutPostBlock extends RotatedPillarBlock implements SimpleWaterloggedBlock {
	public static final MapCodec<HewnChestnutPostBlock> CODEC = simpleCodec(HewnChestnutPostBlock::new);
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	public static final EnumProperty<PostConnection> NORTH = EnumProperty.create("north", PostConnection.class);
	public static final EnumProperty<PostConnection> EAST = EnumProperty.create("east", PostConnection.class);
	public static final EnumProperty<PostConnection> SOUTH = EnumProperty.create("south", PostConnection.class);
	public static final EnumProperty<PostConnection> WEST = EnumProperty.create("west", PostConnection.class);
	public static final Map<Direction, EnumProperty<PostConnection>> PROPERTY_BY_DIRECTION = Map.of(
		Direction.NORTH, NORTH,
		Direction.EAST, EAST,
		Direction.SOUTH, SOUTH,
		Direction.WEST, WEST
	);

	private static final Map<Direction.Axis, VoxelShape> CORE_SHAPES = Map.of(
		Direction.Axis.X, Block.box(0.0, 4.0, 4.0, 16.0, 12.0, 12.0),
		Direction.Axis.Y, Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0),
		Direction.Axis.Z, Block.box(4.0, 4.0, 0.0, 12.0, 12.0, 16.0)
	);
	private static final Map<Direction, VoxelShape> LOW_WALL_CONNECTION_SHAPES =
		Shapes.rotateHorizontal(Block.boxZ(6.0, 0.0, 14.0, 0.0, 4.0));
	private static final Map<Direction, VoxelShape> TALL_WALL_CONNECTION_SHAPES =
		Shapes.rotateHorizontal(Block.boxZ(6.0, 0.0, 16.0, 0.0, 4.0));
	private static final Map<Direction, VoxelShape> RAIL_CONNECTION_SHAPES =
		Shapes.rotateHorizontal(
			Shapes.or(
				Block.boxZ(2.0, 6.0, 9.0, 0.0, 4.0),
				Block.boxZ(2.0, 12.0, 15.0, 0.0, 4.0)
			)
		);

	public HewnChestnutPostBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(
			this.stateDefinition
				.any()
				.setValue(AXIS, Direction.Axis.Y)
				.setValue(WATERLOGGED, false)
				.setValue(NORTH, PostConnection.NONE)
				.setValue(EAST, PostConnection.NONE)
				.setValue(SOUTH, PostConnection.NONE)
				.setValue(WEST, PostConnection.NONE)
		);
	}

	@Override
	public MapCodec<HewnChestnutPostBlock> codec() {
		return CODEC;
	}

	@Override
	protected VoxelShape getShape(
		BlockState state,
		BlockGetter level,
		BlockPos pos,
		CollisionContext context
	) {
		VoxelShape shape = CORE_SHAPES.get(state.getValue(AXIS));
		if (state.getValue(AXIS) != Direction.Axis.Y) {
			return shape;
		}
		for (Map.Entry<Direction, EnumProperty<PostConnection>> entry : PROPERTY_BY_DIRECTION.entrySet()) {
			PostConnection connection = state.getValue(entry.getValue());
			if (connection == PostConnection.WALL_LOW) {
				shape = Shapes.or(shape, LOW_WALL_CONNECTION_SHAPES.get(entry.getKey()));
			} else if (connection == PostConnection.WALL_TALL) {
				shape = Shapes.or(shape, TALL_WALL_CONNECTION_SHAPES.get(entry.getKey()));
			} else if (connection == PostConnection.RAIL) {
				shape = Shapes.or(shape, RAIL_CONNECTION_SHAPES.get(entry.getKey()));
			}
		}
		return shape;
	}

	@Override
	protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
		/*
		 * Walls and fences decide whether to grow an arm from the neighbour's
		 * support face. A vertical post remains an 8x8 collision/outline, but
		 * exposes a full attachment face so those arms reach this block
		 * boundary; the connection models above then bridge the remaining four
		 * pixels. Horizontal posts retain their original support silhouette.
		 */
		return state.getValue(AXIS) == Direction.Axis.Y
			? Shapes.block()
			: CORE_SHAPES.get(state.getValue(AXIS));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockGetter level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		FluidState replacedFluidState = context.getLevel().getFluidState(context.getClickedPos());
		BlockState state = super.getStateForPlacement(context)
			.setValue(WATERLOGGED, replacedFluidState.is(Fluids.WATER));
		if (state.getValue(AXIS) != Direction.Axis.Y) {
			return state;
		}
		for (Map.Entry<Direction, EnumProperty<PostConnection>> entry : PROPERTY_BY_DIRECTION.entrySet()) {
			state = state.setValue(
				entry.getValue(),
				connectionTo(
					level.getBlockState(pos.relative(entry.getKey())),
					entry.getKey()
				)
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
		Direction directionToNeighbour,
		BlockPos neighbourPos,
		BlockState neighbourState,
		RandomSource random
	) {
		if (state.getValue(WATERLOGGED)) {
			ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}
		if (directionToNeighbour.getAxis().isHorizontal()) {
			return state.setValue(
				PROPERTY_BY_DIRECTION.get(directionToNeighbour),
				state.getValue(AXIS) == Direction.Axis.Y
					? connectionTo(neighbourState, directionToNeighbour)
					: PostConnection.NONE
			);
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
		BlockState rotated = super.rotate(state, rotation);
		return switch (rotation) {
			case CLOCKWISE_180 -> rotated
				.setValue(NORTH, state.getValue(SOUTH))
				.setValue(EAST, state.getValue(WEST))
				.setValue(SOUTH, state.getValue(NORTH))
				.setValue(WEST, state.getValue(EAST));
			case COUNTERCLOCKWISE_90 -> rotated
				.setValue(NORTH, state.getValue(EAST))
				.setValue(EAST, state.getValue(SOUTH))
				.setValue(SOUTH, state.getValue(WEST))
				.setValue(WEST, state.getValue(NORTH));
			case CLOCKWISE_90 -> rotated
				.setValue(NORTH, state.getValue(WEST))
				.setValue(EAST, state.getValue(NORTH))
				.setValue(SOUTH, state.getValue(EAST))
				.setValue(WEST, state.getValue(SOUTH));
			default -> rotated;
		};
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return switch (mirror) {
			case LEFT_RIGHT -> super.mirror(state, mirror)
				.setValue(NORTH, state.getValue(SOUTH))
				.setValue(SOUTH, state.getValue(NORTH));
			case FRONT_BACK -> super.mirror(state, mirror)
				.setValue(EAST, state.getValue(WEST))
				.setValue(WEST, state.getValue(EAST));
			default -> super.mirror(state, mirror);
		};
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
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AXIS, WATERLOGGED, NORTH, EAST, SOUTH, WEST);
	}

	private static PostConnection connectionTo(
		BlockState neighbourState,
		Direction directionToNeighbour
	) {
		if (neighbourState.getBlock() instanceof WallBlock || neighbourState.is(BlockTags.WALLS)) {
			EnumProperty<WallSide> sideTowardPost =
				WallBlock.PROPERTY_BY_DIRECTION.get(directionToNeighbour.getOpposite());
			return neighbourState.hasProperty(sideTowardPost)
					&& neighbourState.getValue(sideTowardPost) == WallSide.TALL
				? PostConnection.WALL_TALL
				: PostConnection.WALL_LOW;
		}
		if (
			neighbourState.getBlock() instanceof FenceBlock
				|| neighbourState.is(BlockTags.WOODEN_FENCES)
		) {
			return PostConnection.RAIL;
		}
		return PostConnection.NONE;
	}

	public enum PostConnection implements StringRepresentable {
		NONE("none"),
		WALL_LOW("wall_low"),
		WALL_TALL("wall_tall"),
		RAIL("rail");

		private final String serializedName;

		PostConnection(String serializedName) {
			this.serializedName = serializedName;
		}

		@Override
		public String getSerializedName() {
			return this.serializedName;
		}
	}
}
