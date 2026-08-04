package net.beforetheblight.block.furniture;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Common four-direction placement and collision handling for one-block
 * furniture.
 *
 * <p>North-facing shapes use the same convention as the existing rocking
 * chair: the sitter or user looks north and the back of the object is toward
 * the south edge of its block.</p>
 */
public abstract class DirectionalFurnitureBlock extends HorizontalDirectionalBlock {
	private final Map<Direction, VoxelShape> shapes;

	protected DirectionalFurnitureBlock(
		BlockBehaviour.Properties properties,
		VoxelShape northShape
	) {
		super(properties);
		this.shapes = Shapes.rotateHorizontal(northShape);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = this.defaultBlockState()
			.setValue(FACING, context.getHorizontalDirection().getOpposite());
		return state.canSurvive(context.getLevel(), context.getClickedPos())
			? state
			: null;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		BlockPos below = pos.below();
		return level.getBlockState(below).isFaceSturdy(
			level,
			below,
			Direction.UP
		);
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
		return directionToNeighbour == Direction.DOWN
			&& !state.canSurvive(level, pos)
				? Blocks.AIR.defaultBlockState()
				: super.updateShape(
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
		return this.shapes.get(state.getValue(FACING));
	}

	@Override
	protected void createBlockStateDefinition(
		StateDefinition.Builder<Block, BlockState> builder
	) {
		builder.add(FACING);
	}
}
