package net.beforetheblight.block;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Shared horizontal placement and interaction routing for both trestle forms.
 */
public abstract class AbstractSawingTrestlesBlock extends HorizontalDirectionalBlock {
	protected static final VoxelShape EMPTY_SHAPE = Shapes.or(
		Block.box(2, 0, 2, 5, 7, 4),
		Block.box(11, 0, 2, 14, 7, 4),
		Block.box(1, 7, 1, 15, 9, 5),
		Block.box(2, 0, 12, 5, 7, 14),
		Block.box(11, 0, 12, 14, 7, 14),
		Block.box(1, 7, 11, 15, 9, 15),
		Block.box(7, 3, 3, 9, 5, 13)
	);
	protected static final Map<Direction, VoxelShape> EMPTY_SHAPES =
		Shapes.rotateHorizontal(EMPTY_SHAPE);
	protected static final Map<Direction, VoxelShape> LOADED_SHAPES =
		Shapes.rotateHorizontal(Shapes.or(EMPTY_SHAPE, Block.box(5, 9, 0, 11, 13, 16)));

	protected AbstractSawingTrestlesBlock(BlockBehaviour.Properties properties) {
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

	/**
	 * A held item must be allowed to run its own {@code useOn} behavior. In
	 * particular, a frame saw must never fall through into the loaded block's
	 * empty-hand unload path.
	 */
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

	/**
	 * Lets the empty form intercept a supported beam while the loaded form
	 * continues to pass held-item interactions through to the item itself.
	 */
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

	/**
	 * Keeps empty-hand behavior in a separate overridable hook. The foundation
	 * has no inventory or unloading side effects yet.
	 */
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
