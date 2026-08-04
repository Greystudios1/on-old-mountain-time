package net.beforetheblight.block.furniture;

import java.util.Map;

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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** One-block drop-leaf table with raised/lowered leaf states. */
public final class OpenFurnitureBlock extends DirectionalFurnitureBlock {
	public static final MapCodec<OpenFurnitureBlock> CODEC =
		simpleCodec(OpenFurnitureBlock::new);
	public static final BooleanProperty OPEN = BooleanProperty.create("open");

	private static final VoxelShape CLOSED_NORTH = Shapes.or(
		Block.box(3, 0, 3, 5, 11, 5),
		Block.box(11, 0, 11, 13, 11, 13),
		Block.box(4, 11, 1, 12, 12.5, 15)
	);
	private static final VoxelShape OPEN_NORTH = Shapes.or(
		Block.box(3, 0, 3, 5, 11, 5),
		Block.box(11, 0, 11, 13, 11, 13),
		Block.box(0, 11, 1, 16, 12.5, 15)
	);
	private static final Map<Direction, VoxelShape> CLOSED_SHAPES =
		Shapes.rotateHorizontal(CLOSED_NORTH);
	private static final Map<Direction, VoxelShape> OPEN_SHAPES =
		Shapes.rotateHorizontal(OPEN_NORTH);

	public OpenFurnitureBlock(BlockBehaviour.Properties properties) {
		super(properties, CLOSED_NORTH);
		this.registerDefaultState(this.stateDefinition.any()
			.setValue(FACING, Direction.NORTH)
			.setValue(OPEN, false));
	}

	@Override
	public MapCodec<OpenFurnitureBlock> codec() {
		return CODEC;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		return state != null && state.canSurvive(
			context.getLevel(),
			context.getClickedPos()
		) ? state.setValue(OPEN, false) : null;
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
		return directionToNeighbour == Direction.DOWN
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

	@Override
	protected InteractionResult useWithoutItem(
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		BlockHitResult hit
	) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		BlockState current = level.getBlockState(pos);
		if (!current.is(this)) {
			return InteractionResult.PASS;
		}
		boolean open = !current.getValue(OPEN);
		BlockState changed = current.setValue(OPEN, open);
		if (!level.setBlock(pos, changed, Block.UPDATE_ALL)) {
			return InteractionResult.FAIL;
		}
		level.playSound(
			null,
			pos,
			SoundEvents.WOOD_PLACE,
			SoundSource.BLOCKS,
			0.4F,
			open ? 1.05F : 0.9F
		);
		var event = open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE;
		level.gameEvent(event, pos, GameEvent.Context.of(player, changed));
		return InteractionResult.SUCCESS_SERVER;
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
	protected void createBlockStateDefinition(
		StateDefinition.Builder<Block, BlockState> builder
	) {
		super.createBlockStateDefinition(builder);
		builder.add(OPEN);
	}
}
