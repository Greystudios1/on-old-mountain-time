package net.beforetheblight.block;

import com.mojang.serialization.MapCodec;
import net.beforetheblight.interaction.SawingTrestleStateMachine;
import net.beforetheblight.interaction.TimberProcessingRegistry;
import net.beforetheblight.interaction.TimberType;
import net.beforetheblight.registry.ModCriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
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

/**
 * Empty frame-saw trestles. A loaded timber is represented by the separate
 * {@link LoadedSawingTrestlesBlock}, so no block entity is required.
 */
public final class SawingTrestlesBlock extends AbstractSawingTrestlesBlock {
	public static final MapCodec<SawingTrestlesBlock> CODEC = simpleCodec(SawingTrestlesBlock::new);

	public SawingTrestlesBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	public MapCodec<SawingTrestlesBlock> codec() {
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
		var process = TimberProcessingRegistry.byBeam(itemStack);
		var loadedBlock = SawingTrestleStateMachine.registeredLoadedBlock();
		if (process.isEmpty() || loadedBlock.isEmpty()) {
			return InteractionResult.PASS;
		}

		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		// Re-read after client prediction. Another interaction may already have
		// replaced the workstation before this packet reached the server.
		BlockState currentState = level.getBlockState(pos);
		if (!SawingTrestleStateMachine.isEmpty(currentState)) {
			return InteractionResult.PASS;
		}
		var currentProcess = TimberProcessingRegistry.byBeam(itemStack);
		if (currentProcess.isEmpty() || currentProcess.get().type() != process.get().type()) {
			return InteractionResult.PASS;
		}

		BlockState nextState = SawingTrestleStateMachine.load(
			currentState,
			loadedBlock.get(),
			currentProcess.get().type()
		);
		if (!level.setBlock(pos, nextState, Block.UPDATE_ALL)) {
			return InteractionResult.PASS;
		}

		itemStack.consume(1, player);
		level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.9F, 0.9F);
		level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, nextState));
		if (player instanceof ServerPlayer serverPlayer
			&& currentProcess.get().type() == TimberType.CHESTNUT) {
			ModCriteriaTriggers.LOAD_CHESTNUT_TRESTLES.trigger(serverPlayer);
		}
		return InteractionResult.SUCCESS_SERVER;
	}
}
