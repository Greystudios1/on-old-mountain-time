package net.beforetheblight.block.furniture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Directional fixture that must remain attached to a solid wall behind it.
 */
public abstract class WallMountedFurnitureBlock extends DirectionalFurnitureBlock {
	protected WallMountedFurnitureBlock(
		BlockBehaviour.Properties properties,
		VoxelShape northShape
	) {
		super(properties, northShape);
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return level.getBlockState(
			pos.relative(state.getValue(FACING).getOpposite())
		).isSolid();
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = this.defaultBlockState();
		for (Direction direction : context.getNearestLookingDirections()) {
			if (!direction.getAxis().isHorizontal()) {
				continue;
			}

			BlockState candidate = state.setValue(FACING, direction.getOpposite());
			if (candidate.canSurvive(context.getLevel(), context.getClickedPos())) {
				return candidate;
			}
		}
		return null;
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
		return directionToNeighbour.getOpposite() == state.getValue(FACING)
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
}
