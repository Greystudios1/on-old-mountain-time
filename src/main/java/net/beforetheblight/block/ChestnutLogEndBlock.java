package net.beforetheblight.block;

import java.util.EnumMap;
import java.util.Map;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
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
 * A signed log-end detail. Unlike an axis-only pole, {@link #FACING} records
 * which of the two end-grain faces is exposed, so projecting, flush, and
 * notched ends survive structure rotation without turning inside-out.
 */
public final class ChestnutLogEndBlock
	extends Block
	implements SimpleWaterloggedBlock {
	public static final MapCodec<ChestnutLogEndBlock> PROJECTING_CODEC =
		simpleCodec(ChestnutLogEndBlock::projecting);
	public static final MapCodec<ChestnutLogEndBlock> FLUSH_CODEC =
		simpleCodec(ChestnutLogEndBlock::flush);
	public static final MapCodec<ChestnutLogEndBlock> NOTCHED_CODEC =
		simpleCodec(ChestnutLogEndBlock::notched);
	public static final EnumProperty<Direction> FACING =
		BlockStateProperties.FACING;
	public static final BooleanProperty WATERLOGGED =
		BlockStateProperties.WATERLOGGED;

	private static final Map<Direction, VoxelShape> PROJECTING_SHAPES =
		axisShapes(8.0);
	private static final Map<Direction, VoxelShape> FLUSH_SHAPES =
		axisShapes(12.0);
	private static final Map<Direction, VoxelShape> NOTCHED_SHAPES =
		createNotchedShapes();

	private final MapCodec<ChestnutLogEndBlock> endCodec;
	private final Map<Direction, VoxelShape> shapes;

	private ChestnutLogEndBlock(
		BlockBehaviour.Properties properties,
		MapCodec<ChestnutLogEndBlock> endCodec,
		Map<Direction, VoxelShape> shapes
	) {
		super(properties);
		this.endCodec = endCodec;
		this.shapes = shapes;
		this.registerDefaultState(
			this.stateDefinition
				.any()
				.setValue(FACING, Direction.NORTH)
				.setValue(WATERLOGGED, false)
		);
	}

	public static ChestnutLogEndBlock projecting(
		BlockBehaviour.Properties properties
	) {
		return new ChestnutLogEndBlock(
			properties,
			PROJECTING_CODEC,
			PROJECTING_SHAPES
		);
	}

	public static ChestnutLogEndBlock flush(
		BlockBehaviour.Properties properties
	) {
		return new ChestnutLogEndBlock(
			properties,
			FLUSH_CODEC,
			FLUSH_SHAPES
		);
	}

	public static ChestnutLogEndBlock notched(
		BlockBehaviour.Properties properties
	) {
		return new ChestnutLogEndBlock(
			properties,
			NOTCHED_CODEC,
			NOTCHED_SHAPES
		);
	}

	@Override
	protected MapCodec<ChestnutLogEndBlock> codec() {
		return this.endCodec;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockPos pos = context.getClickedPos();
		return this.defaultBlockState()
			.setValue(FACING, context.getClickedFace())
			.setValue(
				WATERLOGGED,
				context.getLevel().getFluidState(pos).is(Fluids.WATER)
			);
	}

	@Override
	protected VoxelShape getShape(
		BlockState state,
		BlockGetter level,
		BlockPos pos,
		CollisionContext context
	) {
		return this.shapes.get(state.getValue(FACING));
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
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
	protected void createBlockStateDefinition(
		StateDefinition.Builder<Block, BlockState> builder
	) {
		builder.add(FACING, WATERLOGGED);
	}

	private static Map<Direction, VoxelShape> axisShapes(double width) {
		double inset = (16.0 - width) / 2.0;
		Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
		VoxelShape x =
			Block.box(0.0, inset, inset, 16.0, 16.0 - inset, 16.0 - inset);
		VoxelShape y =
			Block.box(inset, 0.0, inset, 16.0 - inset, 16.0, 16.0 - inset);
		VoxelShape z =
			Block.box(inset, inset, 0.0, 16.0 - inset, 16.0 - inset, 16.0);
		shapes.put(Direction.WEST, x);
		shapes.put(Direction.EAST, x);
		shapes.put(Direction.DOWN, y);
		shapes.put(Direction.UP, y);
		shapes.put(Direction.NORTH, z);
		shapes.put(Direction.SOUTH, z);
		return Map.copyOf(shapes);
	}

	private static Map<Direction, VoxelShape> createNotchedShapes() {
		Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
		shapes.put(
			Direction.NORTH,
			Shapes.or(
				Block.box(4.0, 4.0, 4.0, 12.0, 12.0, 16.0),
				Block.box(4.0, 4.0, 0.0, 7.0, 12.0, 4.0),
				Block.box(9.0, 4.0, 0.0, 12.0, 12.0, 4.0)
			)
		);
		shapes.put(
			Direction.SOUTH,
			Shapes.or(
				Block.box(4.0, 4.0, 0.0, 12.0, 12.0, 12.0),
				Block.box(4.0, 4.0, 12.0, 7.0, 12.0, 16.0),
				Block.box(9.0, 4.0, 12.0, 12.0, 12.0, 16.0)
			)
		);
		shapes.put(
			Direction.WEST,
			Shapes.or(
				Block.box(4.0, 4.0, 4.0, 16.0, 12.0, 12.0),
				Block.box(0.0, 4.0, 4.0, 4.0, 12.0, 7.0),
				Block.box(0.0, 4.0, 9.0, 4.0, 12.0, 12.0)
			)
		);
		shapes.put(
			Direction.EAST,
			Shapes.or(
				Block.box(0.0, 4.0, 4.0, 12.0, 12.0, 12.0),
				Block.box(12.0, 4.0, 4.0, 16.0, 12.0, 7.0),
				Block.box(12.0, 4.0, 9.0, 16.0, 12.0, 12.0)
			)
		);
		shapes.put(
			Direction.DOWN,
			Shapes.or(
				Block.box(4.0, 4.0, 4.0, 12.0, 16.0, 12.0),
				Block.box(4.0, 0.0, 4.0, 7.0, 4.0, 12.0),
				Block.box(9.0, 0.0, 4.0, 12.0, 4.0, 12.0)
			)
		);
		shapes.put(
			Direction.UP,
			Shapes.or(
				Block.box(4.0, 0.0, 4.0, 12.0, 12.0, 12.0),
				Block.box(4.0, 12.0, 4.0, 7.0, 16.0, 12.0),
				Block.box(9.0, 12.0, 4.0, 12.0, 16.0, 12.0)
			)
		);
		return Map.copyOf(shapes);
	}
}
