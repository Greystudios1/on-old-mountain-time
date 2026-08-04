package net.beforetheblight.block;

import java.util.List;

import com.mojang.serialization.MapCodec;
import net.beforetheblight.compat.seasons.SeasonalPlantClock;
import net.beforetheblight.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A reusable freestanding rack that air-dries up to one stack of field-corn ears.
 *
 * <p>The age transition and harvest mutation happen only on the logical server.
 * Placing the item creates an empty rack, while the registered default retains
 * four ears so old block-state palettes that predate {@link #COUNT} continue to
 * load as the historical four-ear bundle. Breaking returns both the rack and
 * every stored ear; empty-hand harvesting a mature load leaves the rack in
	 * place. Empty-hand use unloads either fresh or fully dried ears without
	 * destroying the frame.</p>
 */
public final class DryingCornBundleBlock extends Block {
	public static final MapCodec<DryingCornBundleBlock> CODEC = simpleCodec(DryingCornBundleBlock::new);
	public static final int MAX_AGE = 3;
	public static final int LEGACY_EAR_COUNT = 4;
	public static final int MAX_EAR_COUNT = 64;
	public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
	public static final IntegerProperty COUNT = IntegerProperty.create("count", 0, MAX_EAR_COUNT);

	private static final VoxelShape SHAPE = Shapes.or(
		Block.box(1.0, 0.0, 1.0, 3.0, 15.0, 3.0),
		Block.box(13.0, 0.0, 1.0, 15.0, 15.0, 3.0),
		Block.box(1.0, 0.0, 13.0, 3.0, 15.0, 15.0),
		Block.box(13.0, 0.0, 13.0, 15.0, 15.0, 15.0),
		Block.box(1.0, 13.0, 1.0, 15.0, 15.0, 3.0),
		Block.box(1.0, 13.0, 13.0, 15.0, 15.0, 15.0),
		Block.box(1.0, 13.0, 3.0, 3.0, 15.0, 13.0),
		Block.box(13.0, 13.0, 3.0, 15.0, 15.0, 13.0)
	);

	public DryingCornBundleBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(
			stateDefinition.any()
				.setValue(AGE, 0)
				.setValue(COUNT, LEGACY_EAR_COUNT)
		);
	}

	@Override
	public MapCodec<DryingCornBundleBlock> codec() {
		return CODEC;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState()
			.setValue(AGE, 0)
			.setValue(COUNT, 0);
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return state.getValue(COUNT) > 0 && state.getValue(AGE) < MAX_AGE;
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (random.nextInt(SeasonalPlantClock.dryingInterval(level, pos)) != 0) {
			return;
		}

		BlockState advanced = advanceAge(state);
		if (advanced != state) {
			if (level.setBlock(pos, advanced, Block.UPDATE_CLIENTS)) {
				level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(advanced));
			}
		}
	}

	/** Returns the next persisted drying state without mutating the level. */
	public static BlockState advanceAge(BlockState state) {
		if (state.getValue(COUNT) == 0) {
			return state;
		}
		int age = state.getValue(AGE);
		return age >= MAX_AGE ? state : state.setValue(AGE, age + 1);
	}

	@Override
	protected InteractionResult useItemOn(
		ItemStack stack,
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		InteractionHand hand,
		BlockHitResult hitResult
	) {
		if (stack.isEmpty()) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!stack.is(ModItems.EAR_OF_CORN)) {
			return InteractionResult.PASS;
		}

		int capacity = MAX_EAR_COUNT - state.getValue(COUNT);
		int transfer = Math.min(stack.getCount(), capacity);
		if (transfer <= 0) {
			return InteractionResult.CONSUME;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		BlockState current = level.getBlockState(pos);
		if (!current.is(this)) {
			return InteractionResult.PASS;
		}
		capacity = MAX_EAR_COUNT - current.getValue(COUNT);
		transfer = Math.min(stack.getCount(), capacity);
		if (transfer <= 0) {
			return InteractionResult.CONSUME;
		}

		BlockState loaded = current
			.setValue(COUNT, current.getValue(COUNT) + transfer)
			.setValue(AGE, 0);
		if (!level.setBlock(pos, loaded, Block.UPDATE_ALL)) {
			return InteractionResult.FAIL;
		}
		stack.consume(transfer, player);
		level.playSound(
			null,
			pos,
			SoundEvents.WOOD_PLACE,
			SoundSource.BLOCKS,
			0.8F,
			0.9F
		);
		level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, loaded));
		return InteractionResult.SUCCESS_SERVER;
	}

	@Override
	protected InteractionResult useWithoutItem(
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		BlockHitResult hitResult
	) {
		if (state.getValue(COUNT) == 0) {
			return InteractionResult.PASS;
		}

		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		if (level instanceof ServerLevel serverLevel) {
			BlockState current = serverLevel.getBlockState(pos);
			if (!current.is(this) || current.getValue(COUNT) == 0) {
				return InteractionResult.PASS;
			}
			int harvestedCount = current.getValue(COUNT);
			boolean dried = current.getValue(AGE) == MAX_AGE;
			if (!serverLevel.setBlock(
				pos,
				current.setValue(COUNT, 0).setValue(AGE, 0),
				Block.UPDATE_ALL
			)) {
				return InteractionResult.FAIL;
			}
			Block.popResource(
				serverLevel,
				pos,
				new ItemStack(
					dried ? ModItems.DRIED_EAR_OF_CORN : ModItems.EAR_OF_CORN,
					harvestedCount
				)
			);
			serverLevel.playSound(
				null,
				pos,
				SoundEvents.CROP_BREAK,
				SoundSource.BLOCKS,
				1.0F,
				0.8F + serverLevel.getRandom().nextFloat() * 0.2F
			);
			serverLevel.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, current));
		}

		return InteractionResult.SUCCESS_SERVER;
	}

	@Override
	protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
		ItemStack rack = new ItemStack(this);
		int count = state.getValue(COUNT);
		if (count == 0) {
			return List.of(rack);
		}
		ItemStack ears = new ItemStack(
			state.getValue(AGE) == MAX_AGE ? ModItems.DRIED_EAR_OF_CORN : ModItems.EAR_OF_CORN,
			count
		);
		return List.of(rack, ears);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AGE, COUNT);
	}
}
