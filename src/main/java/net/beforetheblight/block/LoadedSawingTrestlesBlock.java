package net.beforetheblight.block;

import com.mojang.serialization.MapCodec;
import net.beforetheblight.interaction.SawingTrestleStateMachine;
import net.beforetheblight.interaction.TimberProcessingRegistry;
import net.beforetheblight.interaction.TimberType;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Persisted loaded-timber form of the frame-saw trestles.
 */
public final class LoadedSawingTrestlesBlock extends AbstractSawingTrestlesBlock {
	public static final EnumProperty<TimberType> WOOD_TYPE = EnumProperty.create(
		"wood_type",
		TimberType.class
	);
	public static final IntegerProperty CUT_STAGE = IntegerProperty.create(
		"cut_stage",
		SawingTrestleStateMachine.INITIAL_CUT_STAGE,
		SawingTrestleStateMachine.FINAL_CUT_STAGE
	);
	public static final MapCodec<LoadedSawingTrestlesBlock> CODEC = simpleCodec(
		LoadedSawingTrestlesBlock::new
	);

	public LoadedSawingTrestlesBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.defaultBlockState()
			.setValue(WOOD_TYPE, TimberType.CHESTNUT)
			.setValue(CUT_STAGE, SawingTrestleStateMachine.INITIAL_CUT_STAGE));
	}

	@Override
	public MapCodec<LoadedSawingTrestlesBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(WOOD_TYPE, CUT_STAGE);
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
		// A second vanilla BlockItem beam would otherwise place beside/above the
		// loaded station after the block passes the interaction to the item.
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
		var emptyBlock = SawingTrestleStateMachine.registeredEmptyBlock();
		var process = TimberProcessingRegistry.byType(state.getValue(WOOD_TYPE));
		if (emptyBlock.isEmpty() || process.isEmpty()) {
			return InteractionResult.PASS;
		}

		if (level.isClientSide()) {
			return InteractionResult.SUCCESS.withoutItem();
		}

		// The server owns recovery. Re-read the persisted timber identity before
		// returning an item so two near-simultaneous users cannot duplicate it.
		BlockState currentState = level.getBlockState(pos);
		if (!SawingTrestleStateMachine.isLoaded(currentState)) {
			return InteractionResult.PASS;
		}
		var currentProcess = TimberProcessingRegistry.byType(currentState.getValue(WOOD_TYPE));
		if (currentProcess.isEmpty()) {
			return InteractionResult.PASS;
		}

		var transition = SawingTrestleStateMachine.unload(currentState, emptyBlock.get());
		if (!level.setBlock(pos, transition.nextState(), Block.UPDATE_ALL)) {
			return InteractionResult.PASS;
		}

		ItemStack returnedBeam = new ItemStack(currentProcess.get().finalBlock());
		player.getInventory().placeItemBackInInventory(returnedBeam);
		level.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 0.8F, 1.1F);
		level.gameEvent(
			GameEvent.BLOCK_CHANGE,
			pos,
			GameEvent.Context.of(player, transition.nextState())
		);
		return InteractionResult.SUCCESS_SERVER.withoutItem();
	}
}
