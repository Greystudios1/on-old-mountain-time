package net.beforetheblight.block.furniture;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

/**
 * Functional six-board chest backed by vanilla's barrel inventory contract.
 *
 * <p>Using the established barrel block entity supplies a 27-slot persistent
 * inventory, open-count synchronization, comparator output, menu handling,
 * sounds, and safe save/reload behavior without a second container
 * implementation. The custom block model still presents a historically
 * appropriate board chest rather than a barrel.</p>
 */
public final class SixBoardChestBlock extends BarrelBlock {
	public static final boolean HAS_INVENTORY = true;

	private static final VoxelShape NORTH_CLOSED_SHAPE = Shapes.or(
		Block.box(0.75, 0, 1.75, 15.25, 11, 14.25),
		Block.box(0.6, 10, 1.5, 15.4, 13, 14.5)
	);
	private static final VoxelShape NORTH_OPEN_SHAPE = Shapes.or(
		Block.box(0.75, 0, 1.75, 15.25, 11, 14.25),
		// The open lid folds upright inside the block's height, avoiding a
		// hidden collision with a ceiling block.
		Block.box(0.6, 10, 12, 15.4, 16, 15)
	);
	private static final Map<net.minecraft.core.Direction, VoxelShape> CLOSED_SHAPES =
		Shapes.rotateHorizontal(NORTH_CLOSED_SHAPE);
	private static final Map<net.minecraft.core.Direction, VoxelShape> OPEN_SHAPES =
		Shapes.rotateHorizontal(NORTH_OPEN_SHAPE);

	public SixBoardChestBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = this.defaultBlockState()
			.setValue(FACING, context.getHorizontalDirection().getOpposite())
			.setValue(OPEN, false);
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
	protected void neighborChanged(
		BlockState state,
		Level level,
		BlockPos pos,
		Block neighbourBlock,
		@Nullable Orientation orientation,
		boolean movedByPiston
	) {
		if (!state.canSurvive(level, pos)) {
			level.destroyBlock(pos, true);
			return;
		}
		super.neighborChanged(
			state,
			level,
			pos,
			neighbourBlock,
			orientation,
			movedByPiston
		);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SixBoardChestBlockEntity(pos, state);
	}

	@Override
	protected VoxelShape getShape(
		BlockState state,
		BlockGetter level,
		BlockPos pos,
		CollisionContext context
	) {
		if (state.getValue(OPEN)) {
			return OPEN_SHAPES.getOrDefault(state.getValue(FACING), NORTH_OPEN_SHAPE);
		}
		return CLOSED_SHAPES.getOrDefault(
			state.getValue(FACING),
			NORTH_CLOSED_SHAPE
		);
	}

	public boolean hasInventory() {
		return HAS_INVENTORY;
	}
}
