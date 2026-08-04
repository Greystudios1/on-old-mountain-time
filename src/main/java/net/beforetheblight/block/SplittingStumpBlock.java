package net.beforetheblight.block;

import com.mojang.serialization.MapCodec;
import net.beforetheblight.interaction.SplittingStateMachine;
import net.beforetheblight.interaction.TimberProcessingRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

/** Empty hand-splitting stump. Supported hewn beams become persisted state. */
public final class SplittingStumpBlock extends AbstractSplittingStumpBlock {
	public static final MapCodec<SplittingStumpBlock> CODEC = simpleCodec(SplittingStumpBlock::new);

	public SplittingStumpBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	public MapCodec<SplittingStumpBlock> codec() {
		return CODEC;
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
		var process = TimberProcessingRegistry.byBeam(itemStack)
			.filter(candidate -> !candidate.splitOutputs().isEmpty());
		var loadedBlock = SplittingStateMachine.registeredLoadedBlock();
		if (process.isEmpty() || loadedBlock.isEmpty()) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		BlockState currentState = level.getBlockState(pos);
		if (!SplittingStateMachine.isEmpty(currentState)) {
			return InteractionResult.PASS;
		}
		var currentProcess = TimberProcessingRegistry.byBeam(itemStack)
			.filter(candidate -> !candidate.splitOutputs().isEmpty());
		if (currentProcess.isEmpty() || currentProcess.get().type() != process.get().type()) {
			return InteractionResult.PASS;
		}

		BlockState nextState = SplittingStateMachine.load(
			currentState,
			loadedBlock.get(),
			currentProcess.get().type()
		);
		if (!level.setBlock(pos, nextState, Block.UPDATE_ALL)) {
			return InteractionResult.PASS;
		}

		itemStack.consume(1, player);
		level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.9F, 0.84F);
		level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, nextState));
		return InteractionResult.SUCCESS_SERVER;
	}
}
