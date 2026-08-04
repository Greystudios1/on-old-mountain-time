package net.beforetheblight.block;

import com.mojang.serialization.MapCodec;
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
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A manually operated, wall-mounted board shutter.
 *
 * <p>The closed panel occupies a centered two-pixel plane. Opening folds it
 * against the selected jamb, so collision follows both facing and hinge side.
 * Redstone is intentionally omitted: this is a hand-latched farmstead shutter,
 * not a disguised modern powered door.</p>
 */
public final class BoardShutterBlock extends HorizontalDirectionalBlock
	implements SimpleWaterloggedBlock {
	public static final MapCodec<BoardShutterBlock> CODEC =
		simpleCodec(BoardShutterBlock::new);
	public static final EnumProperty<DoorHingeSide> HINGE = BlockStateProperties.DOOR_HINGE;
	public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

	private static final VoxelShape CLOSED_NORTH_SOUTH =
		Block.box(0.0, 0.0, 7.0, 16.0, 16.0, 9.0);
	private static final VoxelShape CLOSED_EAST_WEST =
		Block.box(7.0, 0.0, 0.0, 9.0, 16.0, 16.0);
	private static final VoxelShape WEST_FOLDED =
		Block.box(0.0, 0.0, 0.0, 2.0, 16.0, 16.0);
	private static final VoxelShape EAST_FOLDED =
		Block.box(14.0, 0.0, 0.0, 16.0, 16.0, 16.0);
	private static final VoxelShape NORTH_FOLDED =
		Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 2.0);
	private static final VoxelShape SOUTH_FOLDED =
		Block.box(0.0, 0.0, 14.0, 16.0, 16.0, 16.0);

	public BoardShutterBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(
			this.stateDefinition
				.any()
				.setValue(FACING, Direction.NORTH)
				.setValue(HINGE, DoorHingeSide.LEFT)
				.setValue(OPEN, false)
				.setValue(WATERLOGGED, false)
		);
	}

	@Override
	protected MapCodec<BoardShutterBlock> codec() {
		return CODEC;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction facing = context.getHorizontalDirection().getOpposite();
		Vec3 hit = context.getClickLocation();
		BlockPos pos = context.getClickedPos();
		double localX = hit.x - pos.getX() - 0.5;
		double localZ = hit.z - pos.getZ() - 0.5;
		Direction right = facing.getClockWise();
		double side = localX * right.getStepX() + localZ * right.getStepZ();

		return this.defaultBlockState()
			.setValue(FACING, facing)
			.setValue(HINGE, side > 0.0 ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT)
			.setValue(
				WATERLOGGED,
				context.getLevel().getFluidState(pos).is(Fluids.WATER)
			);
	}

	@Override
	protected VoxelShape getShape(
		BlockState state,
		BlockGetter level,
		BlockPos pos,
		CollisionContext context
	) {
		Direction facing = state.getValue(FACING);
		if (!state.getValue(OPEN)) {
			return facing.getAxis() == Direction.Axis.Z
				? CLOSED_NORTH_SOUTH
				: CLOSED_EAST_WEST;
		}

		Direction foldedSide = state.getValue(HINGE) == DoorHingeSide.LEFT
			? facing.getCounterClockWise()
			: facing.getClockWise();
		return switch (foldedSide) {
			case WEST -> WEST_FOLDED;
			case EAST -> EAST_FOLDED;
			case NORTH -> NORTH_FOLDED;
			case SOUTH -> SOUTH_FOLDED;
			default -> throw new IllegalStateException("A shutter cannot fold vertically");
		};
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
			0.8F,
			opened ? 0.95F : 0.85F
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
	protected BlockState mirror(BlockState state, Mirror mirror) {
		if (mirror == Mirror.NONE) {
			return state;
		}
		return super.mirror(state, mirror).cycle(HINGE);
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return state.getValue(OPEN);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, HINGE, OPEN, WATERLOGGED);
	}
}
