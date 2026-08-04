package net.beforetheblight.block.stonehearth;

import java.util.Map;

import com.mojang.serialization.MapCodec;
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
 * A codec-safe directional masonry piece whose authored north shape is rotated
 * into the four cardinal placements. The same shape drives outline and
 * collision, so narrow chimney and rubble pieces never behave like full cubes.
 */
public final class StoneHearthFacingBlock extends HorizontalDirectionalBlock {
	private final MapCodec<StoneHearthFacingBlock> codec;
	private final Map<Direction, VoxelShape> shapes;

	public StoneHearthFacingBlock(
		BlockBehaviour.Properties properties,
		VoxelShape northShape
	) {
		super(properties);
		this.codec = simpleCodec(
			newProperties -> new StoneHearthFacingBlock(newProperties, northShape)
		);
		this.shapes = Shapes.rotateHorizontal(northShape);
		this.registerDefaultState(
			this.stateDefinition.any().setValue(FACING, Direction.NORTH)
		);
	}

	@Override
	protected MapCodec<StoneHearthFacingBlock> codec() {
		return this.codec;
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
