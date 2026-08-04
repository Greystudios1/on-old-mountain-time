package net.beforetheblight.block;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Shared low stump shape, facing, and held-item interaction routing. */
public abstract class AbstractSplittingStumpBlock extends HorizontalDirectionalBlock {
	protected static final VoxelShape EMPTY_SHAPE = Shapes.or(
		Block.box(2, 0, 2, 14, 8, 14),
		Block.box(3, 8, 3, 13, 10, 13)
	);
	protected static final Map<Direction, VoxelShape> EMPTY_SHAPES =
		Shapes.rotateHorizontal(EMPTY_SHAPE);
	protected static final Map<Direction, VoxelShape> LOADED_SHAPES =
		Shapes.rotateHorizontal(Shapes.or(
			EMPTY_SHAPE,
			Block.box(1, 10, 5, 15, 14, 11)
		));

	protected AbstractSplittingStumpBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
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
		return EMPTY_SHAPES.get(state.getValue(FACING));
	}

	@Override
	protected final InteractionResult useItemOn(
		ItemStack itemStack,
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		InteractionHand hand,
		BlockHitResult hitResult
	) {
		return itemStack.isEmpty()
			? InteractionResult.TRY_WITH_EMPTY_HAND
			: this.useWithItem(itemStack, state, level, pos, player, hand, hitResult);
	}

	protected InteractionResult useWithItem(
		ItemStack itemStack,
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		InteractionHand hand,
		BlockHitResult hitResult
	) {
		return InteractionResult.PASS;
	}

	@Override
	protected final InteractionResult useWithoutItem(
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		BlockHitResult hitResult
	) {
		return this.useWithEmptyHand(state, level, pos, player, hitResult);
	}

	protected InteractionResult useWithEmptyHand(
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		BlockHitResult hitResult
	) {
		return InteractionResult.PASS;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}
}
