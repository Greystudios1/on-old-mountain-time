package net.beforetheblight.item;

import java.util.function.Consumer;

import net.beforetheblight.block.LoadedSplittingStumpBlock;
import net.beforetheblight.interaction.BulkTimberProcessing;
import net.beforetheblight.interaction.SplittingStateMachine;
import net.beforetheblight.interaction.TimberProcessingRegistry;
import net.beforetheblight.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/** Three server-authoritative blows complete the froe-selected split. */
public final class WoodenMaulItem extends Item {
	private static final int STRIKE_COOLDOWN_TICKS = 8;
	private static final int CHIP_PARTICLE_COUNT = 12;

	public WoodenMaulItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState previewState = level.getBlockState(pos);
		if (!SplittingStateMachine.isLoaded(previewState)
			|| !previewState.getValue(LoadedSplittingStumpBlock.FROE_SET)) {
			return InteractionResult.PASS;
		}

		var previewProcess = TimberProcessingRegistry.byType(
			previewState.getValue(LoadedSplittingStumpBlock.WOOD_TYPE)
		);
		var emptyBlock = SplittingStateMachine.registeredEmptyBlock();
		if (previewProcess.isEmpty()
			|| emptyBlock.isEmpty()
			|| !previewProcess.get().splitOutputs().containsKey(
				previewState.getValue(LoadedSplittingStumpBlock.SPLIT_KIND)
			)) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		Player player = context.getPlayer();
		if (player == null) {
			return InteractionResult.PASS;
		}
		ItemStack maul = context.getItemInHand();
		if (player.getCooldowns().isOnCooldown(maul)) {
			return InteractionResult.FAIL;
		}

		BlockState currentState = level.getBlockState(pos);
		if (!SplittingStateMachine.isLoaded(currentState)
			|| !currentState.getValue(LoadedSplittingStumpBlock.FROE_SET)) {
			return InteractionResult.PASS;
		}
		var process = TimberProcessingRegistry.byType(
			currentState.getValue(LoadedSplittingStumpBlock.WOOD_TYPE)
		);
		var selectedKind = currentState.getValue(LoadedSplittingStumpBlock.SPLIT_KIND);
		if (process.isEmpty() || !process.get().splitOutputs().containsKey(selectedKind)) {
			return InteractionResult.PASS;
		}

		var transition = SplittingStateMachine.strike(currentState, emptyBlock.get());
		if (!level.setBlock(pos, transition.nextState(), Block.UPDATE_ALL)) {
			return InteractionResult.PASS;
		}

		var batch = transition.completed()
			? BulkTimberProcessing.consumeAdditionalInputs(
				player,
				maul,
				process.get().finalBlock().asItem(),
				3
			)
			: BulkTimberProcessing.single();
		if (transition.completed()) {
			var output = process.get().splitOutputs().get(transition.splitKind());
			BulkTimberProcessing.popOutput(
				level,
				pos,
				output.item(),
				output.count() * batch.totalBatches()
			);
		}
		level.playSound(
			null,
			pos,
			transition.completed() ? ModSounds.SPLIT_COMPLETE : ModSounds.MAUL_STRIKE,
			SoundSource.BLOCKS,
			1.05F,
			transition.completed() ? 0.82F : 0.92F
		);
		level.gameEvent(
			GameEvent.BLOCK_CHANGE,
			pos,
			GameEvent.Context.of(player, transition.nextState())
		);
		if (level instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(
				new BlockParticleOption(
					ParticleTypes.BLOCK,
					process.get().finalBlock().defaultBlockState()
				),
				pos.getX() + 0.5,
				pos.getY() + 0.78,
				pos.getZ() + 0.5,
				CHIP_PARTICLE_COUNT,
				0.30,
				0.16,
				0.30,
				0.035
			);
		}
		if (player instanceof ServerPlayer serverPlayer) {
			CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(serverPlayer, pos, maul);
		}
		player.getCooldowns().addCooldown(maul, STRIKE_COOLDOWN_TICKS);
		maul.hurtAndBreak(
			batch.durabilityCost(),
			player,
			context.getHand().asEquipmentSlot()
		);
		return InteractionResult.SUCCESS_SERVER;
	}

	@Override
	public void appendHoverText(
		ItemStack itemStack,
		Item.TooltipContext context,
		TooltipDisplay display,
		Consumer<Component> builder,
		TooltipFlag tooltipFlag
	) {
		builder.accept(Component.translatableWithFallback(
			"item.before_the_blight.wooden_maul.tooltip",
			"Strike three times; the final blow batches up to 64 matching beams."
		).withStyle(ChatFormatting.GRAY));
	}
}
