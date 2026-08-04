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

/**
 * A compact hand sheller: load one dried ear, then use an empty hand to recover
 * four kernels. The loaded state is ordinary persistent blockstate data.
 */
public final class HandCornShellerBlock extends HorizontalDirectionalBlock {
	public static final MapCodec<HandCornShellerBlock> CODEC =
		simpleCodec(HandCornShellerBlock::new);
	public static final BooleanProperty LOADED = BooleanProperty.create("loaded");

	private static final VoxelShape NORTH_SHAPE = Shapes.or(
		Block.box(2.0, 0.0, 3.0, 14.0, 9.0, 14.0),
		Block.box(5.0, 9.0, 5.0, 11.0, 14.0, 11.0),
		Block.box(11.0, 7.0, 6.0, 16.0, 10.0, 10.0)
	);
	private static final Map<Direction, VoxelShape> SHAPES =
		Shapes.rotateHorizontal(NORTH_SHAPE);

	public HandCornShellerBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(
			stateDefinition.any()
				.setValue(FACING, Direction.NORTH)
				.setValue(LOADED, false)
		);
	}

	@Override
	public MapCodec<HandCornShellerBlock> codec() {
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
		if (!stack.is(ModItems.DRIED_EAR_OF_CORN) || state.getValue(LOADED)) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		ServerLevel serverLevel = (ServerLevel) level;
		BlockState current = serverLevel.getBlockState(pos);
		if (!current.is(this) || current.getValue(LOADED)) {
			return InteractionResult.PASS;
		}
		BlockState loaded = current.setValue(LOADED, true);
		if (!serverLevel.setBlock(pos, loaded, Block.UPDATE_ALL)) {
			return InteractionResult.FAIL;
		}
		stack.consume(1, player);
		serverLevel.playSound(
			null,
			pos,
			SoundEvents.WOOD_PLACE,
			SoundSource.BLOCKS,
			0.75F,
			0.8F
		);
		serverLevel.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, loaded));
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
		if (!state.getValue(LOADED)) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		ServerLevel serverLevel = (ServerLevel) level;
		BlockState current = serverLevel.getBlockState(pos);
		if (!current.is(this) || !current.getValue(LOADED)) {
			return InteractionResult.PASS;
		}
		BlockState unloaded = current.setValue(LOADED, false);
		if (!serverLevel.setBlock(pos, unloaded, Block.UPDATE_ALL)) {
			return InteractionResult.FAIL;
		}
		Block.popResource(serverLevel, pos, new ItemStack(ModItems.CORN_KERNELS, 4));
		serverLevel.playSound(
			null,
			pos,
			SoundEvents.WOOD_BREAK,
			SoundSource.BLOCKS,
			0.8F,
			1.1F
		);
		serverLevel.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, unloaded));
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
		builder.add(FACING, LOADED);
	}
}
