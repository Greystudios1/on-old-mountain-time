package net.beforetheblight.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * A full-depth board wall whose authored exterior face follows placement.
 *
 * <p>The facing points toward the player so battens, patches, and weathering
 * remain on the intended outside face instead of rotating independently from
 * the wall. The block deliberately remains a full structural cube; thin loose
 * boards and roof trim use dedicated shape-aware blocks.</p>
 */
public final class FacingBoardworkBlock extends HorizontalDirectionalBlock {
	public static final MapCodec<FacingBoardworkBlock> CODEC = simpleCodec(FacingBoardworkBlock::new);

	public FacingBoardworkBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	public MapCodec<FacingBoardworkBlock> codec() {
		return CODEC;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(
			FACING,
			context.getHorizontalDirection().getOpposite()
		);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
		builder.add(FACING);
	}
}
