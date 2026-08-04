package net.beforetheblight.block;

import java.util.Map;

import com.mojang.serialization.MapCodec;
import net.beforetheblight.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A one-bushel splint basket with an honest empty/ear-corn-filled state. */
public final class BushelBasketBlock extends HorizontalDirectionalBlock {
	public static final MapCodec<BushelBasketBlock> CODEC =
		simpleCodec(BushelBasketBlock::new);
	public static final BooleanProperty FILLED = BooleanProperty.create("filled");
	public static final int EARS_PER_FILL = 4;

	private static final VoxelShape NORTH_SHAPE = Shapes.or(
		Block.box(2.0, 0.0, 2.0, 14.0, 9.0, 14.0),
		Block.box(1.0, 4.0, 7.0, 3.0, 13.0, 9.0),
		Block.box(13.0, 4.0, 7.0, 15.0, 13.0, 9.0),
		Block.box(2.0, 11.0, 7.0, 14.0, 13.0, 9.0)
	);
	private static final Map<Direction, VoxelShape> SHAPES =
		Shapes.rotateHorizontal(NORTH_SHAPE);

	public BushelBasketBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(
			stateDefinition.any()
				.setValue(FACING, Direction.NORTH)
				.setValue(FILLED, false)
		);
	}

	@Override
	public MapCodec<BushelBasketBlock> codec() {
		return CODEC;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(
			FACING,
			context.getHorizontalDirection().getOpposite()
		);
	}

	@Override
	protected InteractionResult useItemOn(
		ItemStack stack,
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		InteractionHand hand,
		BlockHitResult hit
	) {
		if (stack.isEmpty()) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!stack.is(ModItems.DRIED_EAR_OF_CORN) || state.getValue(FILLED)) {
			return InteractionResult.PASS;
		}
		if (!player.hasInfiniteMaterials() && stack.getCount() < EARS_PER_FILL) {
			return InteractionResult.CONSUME;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		ServerLevel serverLevel = (ServerLevel) level;
		BlockState current = serverLevel.getBlockState(pos);
		if (!current.is(this) || current.getValue(FILLED)) {
			return InteractionResult.PASS;
		}
		BlockState filled = current.setValue(FILLED, true);
		if (!serverLevel.setBlock(pos, filled, Block.UPDATE_ALL)) {
			return InteractionResult.FAIL;
		}
		stack.consume(EARS_PER_FILL, player);
		serverLevel.playSound(
			null,
			pos,
			SoundEvents.WOOD_PLACE,
			SoundSource.BLOCKS,
			0.7F,
			0.9F
		);
		serverLevel.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, filled));
		return InteractionResult.SUCCESS_SERVER;
	}

	@Override
	protected InteractionResult useWithoutItem(
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		BlockHitResult hit
	) {
		if (!state.getValue(FILLED)) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		ServerLevel serverLevel = (ServerLevel) level;
		BlockState current = serverLevel.getBlockState(pos);
		if (!current.is(this) || !current.getValue(FILLED)) {
			return InteractionResult.PASS;
		}
		BlockState emptied = current.setValue(FILLED, false);
		if (!serverLevel.setBlock(pos, emptied, Block.UPDATE_ALL)) {
			return InteractionResult.FAIL;
		}
		Block.popResource(
			serverLevel,
			pos,
			new ItemStack(ModItems.DRIED_EAR_OF_CORN, EARS_PER_FILL)
		);
		serverLevel.playSound(
			null,
			pos,
			SoundEvents.CROP_BREAK,
			SoundSource.BLOCKS,
			0.7F,
			0.9F
		);
		serverLevel.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, emptied));
		return InteractionResult.SUCCESS_SERVER;
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
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, FILLED);
	}
}
