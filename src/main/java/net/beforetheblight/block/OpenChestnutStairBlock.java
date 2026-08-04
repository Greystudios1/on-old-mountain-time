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

/**
 * A straight, open-riser stair assembled from four plank treads and two plain
 * timber stringers.
 *
 * <p>The quarter-block rises preserve a usable stair collision without the
 * solid wedge beneath a vanilla stair. Direction changes require a separate
 * landing instead of automatically turning into a modern mitred corner.</p>
 */
public final class OpenChestnutStairBlock extends HorizontalDirectionalBlock
	implements SimpleWaterloggedBlock {
	public static final MapCodec<OpenChestnutStairBlock> CODEC =
		simpleCodec(OpenChestnutStairBlock::new);
	public static final BooleanProperty WATERLOGGED =
		BlockStateProperties.WATERLOGGED;

	/*
	 * North-facing stair: rise from south to north in four 4-pixel steps.
	 * Side-only supports approximate the two visible diagonal stringers used by
	 * the authored model while keeping the walking center completely open.
	 */
	private static final VoxelShape NORTH = Shapes.or(
		Block.box(0.0, 3.0, 12.0, 16.0, 4.0, 16.0),
		Block.box(0.0, 7.0, 8.0, 16.0, 8.0, 12.0),
		Block.box(0.0, 11.0, 4.0, 16.0, 12.0, 8.0),
		Block.box(0.0, 15.0, 0.0, 16.0, 16.0, 4.0),
		Block.box(0.0, 1.0, 12.0, 2.0, 3.0, 16.0),
		Block.box(0.0, 1.0, 11.0, 2.0, 7.0, 13.0),
		Block.box(0.0, 5.0, 8.0, 2.0, 7.0, 12.0),
		Block.box(0.0, 5.0, 7.0, 2.0, 11.0, 9.0),
		Block.box(0.0, 9.0, 4.0, 2.0, 11.0, 8.0),
		Block.box(0.0, 9.0, 3.0, 2.0, 15.0, 5.0),
		Block.box(0.0, 13.0, 0.0, 2.0, 15.0, 4.0),
		Block.box(14.0, 1.0, 12.0, 16.0, 3.0, 16.0),
		Block.box(14.0, 1.0, 11.0, 16.0, 7.0, 13.0),
		Block.box(14.0, 5.0, 8.0, 16.0, 7.0, 12.0),
		Block.box(14.0, 5.0, 7.0, 16.0, 11.0, 9.0),
		Block.box(14.0, 9.0, 4.0, 16.0, 11.0, 8.0),
		Block.box(14.0, 9.0, 3.0, 16.0, 15.0, 5.0),
		Block.box(14.0, 13.0, 0.0, 16.0, 15.0, 4.0)
	);
	private static final Map<Direction, VoxelShape> SHAPES =
		Shapes.rotateHorizontal(NORTH);

	public OpenChestnutStairBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(
			stateDefinition.any()
				.setValue(FACING, Direction.NORTH)
				.setValue(WATERLOGGED, false)
		);
	}

	@Override
	protected MapCodec<? extends OpenChestnutStairBlock> codec() {
		return CODEC;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
		return defaultBlockState()
			.setValue(FACING, context.getHorizontalDirection())
			.setValue(WATERLOGGED, fluid.is(Fluids.WATER));
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
	protected VoxelShape getShape(
		BlockState state,
		BlockGetter level,
		BlockPos pos,
		CollisionContext context
	) {
		return SHAPES.get(state.getValue(FACING));
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED)
			? Fluids.WATER.getSource(false)
			: super.getFluidState(state);
	}

	@Override
	protected boolean isPathfindable(
		BlockState state,
		PathComputationType type
	) {
		return false;
	}

	@Override
	protected void createBlockStateDefinition(
		StateDefinition.Builder<Block, BlockState> builder
	) {
		builder.add(FACING, WATERLOGGED);
	}
}
