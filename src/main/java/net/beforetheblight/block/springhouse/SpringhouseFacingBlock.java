package net.beforetheblight.block.springhouse;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Shared placement and shape rotation for single-block springhouse furnishings.
 *
 * <p>North is the authored/model-facing direction. A placed furnishing faces
 * the player, matching vanilla directional furniture placement. Subclasses
 * remain responsible for their codecs and any additional block-state
 * properties.</p>
 */
public abstract class SpringhouseFacingBlock extends HorizontalDirectionalBlock {
	private final Map<Direction, VoxelShape> shapes;

	protected SpringhouseFacingBlock(
		BlockBehaviour.Properties properties,
		VoxelShape northShape
	) {
		super(properties);
		this.shapes = Shapes.rotateHorizontal(northShape);
		this.registerDefaultState(
			this.stateDefinition.any().setValue(FACING, Direction.NORTH)
		);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(
			FACING,
			context.getHorizontalDirection().getOpposite()
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
		builder.add(FACING);
	}
}
