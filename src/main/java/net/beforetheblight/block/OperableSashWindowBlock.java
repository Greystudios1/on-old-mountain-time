package net.beforetheblight.block;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A manually opened wooden sash in a self-contained one-block frame.
 *
 * <p>The closed state has the same two-pixel depth as the modeled glass and
 * frame. Opening lifts the lower sash behind the upper lights, clearing the
 * lower opening while retaining the frame and raised sash collision.</p>
 */
public final class OperableSashWindowBlock extends HorizontalDirectionalBlock
	implements SimpleWaterloggedBlock {
	public static final MapCodec<OperableSashWindowBlock> CODEC =
		simpleCodec(OperableSashWindowBlock::new);
	public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

	private static final Map<Direction, VoxelShape> CLOSED_SHAPES =
		Shapes.rotateHorizontal(Block.box(0.0, 0.0, 7.0, 16.0, 16.0, 9.0));
	private static final Map<Direction, VoxelShape> OPEN_SHAPES =
		Shapes.rotateHorizontal(
			Shapes.or(
				Block.box(0.0, 0.0, 7.0, 2.0, 16.0, 9.0),
				Block.box(14.0, 0.0, 7.0, 16.0, 16.0, 9.0),
				Block.box(2.0, 0.0, 7.0, 14.0, 2.0, 9.0),
				Block.box(2.0, 14.0, 7.0, 14.0, 16.0, 9.0),
				Block.box(2.0, 7.0, 7.0, 14.0, 14.0, 9.0)
			)
		);

	public OperableSashWindowBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(
			this.stateDefinition
				.any()
				.setValue(FACING, Direction.NORTH)
				.setValue(OPEN, false)
				.setValue(WATERLOGGED, false)
		);
	}

	@Override
	protected MapCodec<OperableSashWindowBlock> codec() {
		return CODEC;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState()
			.setValue(FACING, context.getHorizontalDirection().getOpposite())
			.setValue(
				WATERLOGGED,
				context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER)
			);
	}

	@Override
	protected VoxelShape getShape(
		BlockState state,
		BlockGetter level,
		BlockPos pos,
		CollisionContext context
	) {
		return (state.getValue(OPEN) ? OPEN_SHAPES : CLOSED_SHAPES)
			.get(state.getValue(FACING));
	}

	@Override
	protected InteractionResult useWithoutItem(
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		BlockHitResult hit
	) {
		BlockState changed = state.cycle(OPEN);
		level.setBlock(pos, changed, Block.UPDATE_ALL);
		boolean opened = changed.getValue(OPEN);
		level.playSound(
			player,
			pos,
			opened ? SoundEvents.WOODEN_TRAPDOOR_OPEN : SoundEvents.WOODEN_TRAPDOOR_CLOSE,
			SoundSource.BLOCKS,
			0.75F,
			opened ? 1.05F : 0.95F
		);
		level.gameEvent(
			opened ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE,
			pos,
			GameEvent.Context.of(player, changed)
		);
		return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED)
			? Fluids.WATER.getSource(false)
			: super.getFluidState(state);
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
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, OPEN, WATERLOGGED);
	}
}
