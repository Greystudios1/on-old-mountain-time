package net.beforetheblight.block.stonehearth;

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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A minimally raised, non-coplanar floor-clutter layer.
 *
 * <p>The model and outline begin above the supporting face rather than sharing
 * its plane. Rotation keeps asymmetric ash and soot patches from looking like
 * a repeated square decal.</p>
 */
public final class StoneHearthLayerBlock extends HorizontalDirectionalBlock {
	private static final VoxelShape NORTH_SHAPE = Shapes.or(
		Block.box(1.0, 0.35, 2.0, 10.0, 0.75, 9.0),
		Block.box(7.0, 0.40, 6.0, 15.0, 0.90, 14.0),
		Block.box(2.0, 0.45, 11.0, 7.0, 0.80, 15.0)
	);
	private static final Map<Direction, VoxelShape> SHAPES =
		Shapes.rotateHorizontal(NORTH_SHAPE);
	public static final MapCodec<StoneHearthLayerBlock> CODEC =
		simpleCodec(StoneHearthLayerBlock::new);

	public StoneHearthLayerBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(
			this.stateDefinition.any().setValue(FACING, Direction.NORTH)
		);
	}

	@Override
	protected MapCodec<StoneHearthLayerBlock> codec() {
		return CODEC;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = this.defaultBlockState().setValue(
			FACING,
			context.getHorizontalDirection().getOpposite()
		);
		return state.canSurvive(context.getLevel(), context.getClickedPos())
			? state
			: null;
	}

	@Override
	protected boolean canSurvive(
		BlockState state,
		LevelReader level,
		BlockPos pos
	) {
		return Block.canSupportCenter(level, pos.below(), Direction.UP);
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
		if (direction == Direction.DOWN && !state.canSurvive(level, pos)) {
			return Blocks.AIR.defaultBlockState();
		}
		return super.updateShape(
			state,
			level,
			ticks,
			pos,
			direction,
			neighborPos,
			neighborState,
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
	protected void createBlockStateDefinition(
		StateDefinition.Builder<Block, BlockState> builder
	) {
		builder.add(FACING);
	}
}
