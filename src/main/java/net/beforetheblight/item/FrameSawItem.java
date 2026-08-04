package net.beforetheblight.item;

import java.util.function.Consumer;

import net.beforetheblight.block.LoadedSawingTrestlesBlock;
import net.beforetheblight.interaction.BulkTimberProcessing;
import net.beforetheblight.interaction.SawingTrestleStateMachine;
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

/**
 * A frame saw whose only in-world action is advancing a loaded sawing trestle.
 * The persisted block state is authoritative, so a second interaction after a
 * terminal cut sees empty trestles and cannot create another output.
 */
public final class FrameSawItem extends Item {
	private static final int SAWDUST_PARTICLE_COUNT = 10;
	private static final int STROKE_COOLDOWN_TICKS = 8;

	public FrameSawItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState previewState = level.getBlockState(pos);
		if (!SawingTrestleStateMachine.isLoaded(previewState)) {
			return InteractionResult.PASS;
		}

		var previewProcess = TimberProcessingRegistry.byType(
			previewState.getValue(LoadedSawingTrestlesBlock.WOOD_TYPE)
		);
		var emptyBlock = SawingTrestleStateMachine.registeredEmptyBlock();
		if (previewProcess.isEmpty() || emptyBlock.isEmpty()) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		Player player = context.getPlayer();
		if (player == null) {
			// A stroke without an acting player cannot pay the durability cost, so
			// it is not accepted as a workstation transition.
			return InteractionResult.PASS;
		}

		// Re-read on the authoritative side. This is the duplication guard for
		// two users whose clients both observed the same terminal cut stage.
		BlockState currentState = level.getBlockState(pos);
		if (!SawingTrestleStateMachine.isLoaded(currentState)) {
			return InteractionResult.PASS;
		}
		var process = TimberProcessingRegistry.byType(
			currentState.getValue(LoadedSawingTrestlesBlock.WOOD_TYPE)
		);
		if (process.isEmpty()) {
			return InteractionResult.PASS;
		}

		var transition = SawingTrestleStateMachine.advance(currentState, emptyBlock.get());
		if (!level.setBlock(pos, transition.nextState(), Block.UPDATE_ALL)) {
			return InteractionResult.PASS;
		}

		ItemStack frameSaw = context.getItemInHand();
		var batch = transition.completed()
			? BulkTimberProcessing.consumeAdditionalInputs(
				player,
				frameSaw,
				process.get().finalBlock().asItem(),
				4
			)
			: BulkTimberProcessing.single();
		if (transition.completed()) {
			var output = process.get().roughBoards();
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
			transition.completed() ? ModSounds.SAW_COMPLETE : ModSounds.SAW_STROKE,
			SoundSource.BLOCKS,
			1.0F,
			transition.completed() ? 0.9F : 1.15F
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
				pos.getY() + 0.7,
				pos.getZ() + 0.5,
				SAWDUST_PARTICLE_COUNT,
				0.35,
				0.12,
				0.35,
				0.03
			);
		}
		if (player instanceof ServerPlayer serverPlayer) {
			CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(serverPlayer, pos, frameSaw);
		}
		// A short rhythm prevents accidental double strokes while keeping a
		// complete beam comfortably below a few seconds.
		player.getCooldowns().addCooldown(frameSaw, STROKE_COOLDOWN_TICKS);
		// Exactly one durability attempt follows each successful state swap,
		// including the four strokes represented by every additional beam.
		frameSaw.hurtAndBreak(
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
			"item.before_the_blight.frame_saw.tooltip.load",
			"Load a hewn beam onto sawing trestles."
		).withStyle(ChatFormatting.GRAY));
		builder.accept(Component.translatableWithFallback(
			"item.before_the_blight.frame_saw.tooltip.cut",
			"Saw four times; the final stroke batches up to 64 matching beams."
		).withStyle(ChatFormatting.GRAY));
	}
}
