package net.beforetheblight.block;

import com.mojang.serialization.MapCodec;
import net.beforetheblight.interaction.SplittingStateMachine;
import net.beforetheblight.interaction.TimberProcessingRegistry;
import net.beforetheblight.interaction.TimberSplitKind;
import net.beforetheblight.interaction.TimberType;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Persisted beam, froe orientation, and maul progress on a splitting stump. */
public final class LoadedSplittingStumpBlock extends AbstractSplittingStumpBlock {
	public static final EnumProperty<TimberType> WOOD_TYPE = EnumProperty.create(
		"wood_type",
		TimberType.class
	);
	public static final EnumProperty<TimberSplitKind> SPLIT_KIND = EnumProperty.create(
		"split_kind",
		TimberSplitKind.class
	);
	public static final BooleanProperty FROE_SET = BooleanProperty.create("froe_set");
	public static final IntegerProperty STRIKE_STAGE = IntegerProperty.create(
		"strike_stage",
		SplittingStateMachine.INITIAL_STRIKE_STAGE,
		SplittingStateMachine.FINAL_STRIKE_STAGE
	);
	public static final MapCodec<LoadedSplittingStumpBlock> CODEC = simpleCodec(
		LoadedSplittingStumpBlock::new
	);

	public LoadedSplittingStumpBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.defaultBlockState()
			.setValue(WOOD_TYPE, TimberType.CHESTNUT)
			.setValue(SPLIT_KIND, TimberSplitKind.SHINGLES)
			.setValue(FROE_SET, false)
			.setValue(STRIKE_STAGE, SplittingStateMachine.INITIAL_STRIKE_STAGE));
	}

	@Override
	public MapCodec<LoadedSplittingStumpBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(WOOD_TYPE, SPLIT_KIND, FROE_SET, STRIKE_STAGE);
	}

	@Override
	protected VoxelShape getShape(
		BlockState state,
		BlockGetter level,
		BlockPos pos,
		CollisionContext context
	) {
		return LOADED_SHAPES.get(state.getValue(FACING));
	}

	@Override
	protected InteractionResult useWithItem(
		ItemStack itemStack,
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		InteractionHand hand,
		BlockHitResult hitResult
	) {
		return TimberProcessingRegistry.byBeam(itemStack).isPresent()
			? InteractionResult.CONSUME
			: InteractionResult.PASS;
	}

	@Override
	protected InteractionResult useWithEmptyHand(
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		BlockHitResult hitResult
	) {
		var emptyBlock = SplittingStateMachine.registeredEmptyBlock();
		var process = TimberProcessingRegistry.byType(state.getValue(WOOD_TYPE));
		if (emptyBlock.isEmpty() || process.isEmpty()) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS.withoutItem();
		}

		BlockState currentState = level.getBlockState(pos);
		if (!SplittingStateMachine.isLoaded(currentState)) {
			return InteractionResult.PASS;
		}
		var currentProcess = TimberProcessingRegistry.byType(currentState.getValue(WOOD_TYPE));
		if (currentProcess.isEmpty()) {
			return InteractionResult.PASS;
		}

		var transition = SplittingStateMachine.unload(currentState, emptyBlock.get());
		if (!level.setBlock(pos, transition.nextState(), Block.UPDATE_ALL)) {
			return InteractionResult.PASS;
		}

		player.getInventory().placeItemBackInInventory(
			new ItemStack(currentProcess.get().finalBlock())
		);
		level.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 0.8F, 1.08F);
		level.gameEvent(
			GameEvent.BLOCK_CHANGE,
			pos,
			GameEvent.Context.of(player, transition.nextState())
		);
		return InteractionResult.SUCCESS_SERVER.withoutItem();
	}
}
