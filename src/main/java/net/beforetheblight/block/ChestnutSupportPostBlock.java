package net.beforetheblight.block;

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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Vertical tapered or forked support post with directional top geometry. */
public final class ChestnutSupportPostBlock
	extends HorizontalDirectionalBlock
	implements SimpleWaterloggedBlock {
	public static final MapCodec<ChestnutSupportPostBlock> FORKED_CODEC =
		simpleCodec(ChestnutSupportPostBlock::forked);
	public static final MapCodec<ChestnutSupportPostBlock> TAPERED_CODEC =
		simpleCodec(ChestnutSupportPostBlock::tapered);
	public static final BooleanProperty WATERLOGGED =
		BlockStateProperties.WATERLOGGED;

	private static final VoxelShape TAPERED_SHAPE =
		Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);
	private static final VoxelShape FORKED_NORTH_SHAPE = Shapes.or(
		TAPERED_SHAPE,
		Block.box(2.0, 11.0, 4.0, 6.0, 16.0, 12.0),
		Block.box(10.0, 11.0, 4.0, 14.0, 16.0, 12.0)
	);
	private static final Map<Direction, VoxelShape> FORKED_SHAPES =
		Shapes.rotateHorizontal(FORKED_NORTH_SHAPE);

	private final boolean forked;
	private final MapCodec<ChestnutSupportPostBlock> postCodec;

	private ChestnutSupportPostBlock(
		BlockBehaviour.Properties properties,
		boolean forked,
		MapCodec<ChestnutSupportPostBlock> postCodec
	) {
		super(properties);
		this.forked = forked;
		this.postCodec = postCodec;
		this.registerDefaultState(
			this.stateDefinition
				.any()
				.setValue(FACING, Direction.NORTH)
				.setValue(WATERLOGGED, false)
		);
	}

	public static ChestnutSupportPostBlock forked(
		BlockBehaviour.Properties properties
	) {
		return new ChestnutSupportPostBlock(properties, true, FORKED_CODEC);
	}

	public static ChestnutSupportPostBlock tapered(
		BlockBehaviour.Properties properties
	) {
		return new ChestnutSupportPostBlock(properties, false, TAPERED_CODEC);
	}

	@Override
	protected MapCodec<ChestnutSupportPostBlock> codec() {
		return this.postCodec;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockPos pos = context.getClickedPos();
		return this.defaultBlockState()
			.setValue(FACING, context.getHorizontalDirection().getOpposite())
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
		return this.forked
			? FORKED_SHAPES.get(state.getValue(FACING))
			: TAPERED_SHAPE;
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
}
