package net.beforetheblight.item;

import java.util.function.Consumer;

import net.beforetheblight.block.LoadedSplittingStumpBlock;
import net.beforetheblight.interaction.SplittingStateMachine;
import net.beforetheblight.interaction.TimberProcessingRegistry;
import net.beforetheblight.interaction.TimberSplitKind;
import net.beforetheblight.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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

/** Places a froe and selects shingles from a vertical face or rails from a side. */
public final class FroeItem extends Item {
	public FroeItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState previewState = level.getBlockState(pos);
		if (!SplittingStateMachine.isLoaded(previewState)) {
			return InteractionResult.PASS;
		}

		TimberSplitKind selectedKind = context.getClickedFace().getAxis() == Direction.Axis.Y
			? TimberSplitKind.SHINGLES
			: TimberSplitKind.RAILS;
		var previewProcess = TimberProcessingRegistry.byType(
			previewState.getValue(LoadedSplittingStumpBlock.WOOD_TYPE)
		);
		if (previewProcess.isEmpty()
			|| !previewProcess.get().splitOutputs().containsKey(selectedKind)) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		Player player = context.getPlayer();
		if (player == null) {
			return InteractionResult.PASS;
		}

		BlockState currentState = level.getBlockState(pos);
		if (!SplittingStateMachine.isLoaded(currentState)) {
			return InteractionResult.PASS;
		}
		var process = TimberProcessingRegistry.byType(
			currentState.getValue(LoadedSplittingStumpBlock.WOOD_TYPE)
		);
		if (process.isEmpty() || !process.get().splitOutputs().containsKey(selectedKind)) {
			return InteractionResult.PASS;
		}

		var transition = SplittingStateMachine.setFroe(currentState, selectedKind);
		if (!transition.changed()) {
			return InteractionResult.SUCCESS_SERVER;
		}
		if (!level.setBlock(pos, transition.nextState(), Block.UPDATE_ALL)) {
			return InteractionResult.PASS;
		}

		ItemStack froe = context.getItemInHand();
		level.playSound(null, pos, ModSounds.FROE_SET, SoundSource.BLOCKS, 0.55F, 1.45F);
		level.gameEvent(
			GameEvent.BLOCK_CHANGE,
			pos,
			GameEvent.Context.of(player, transition.nextState())
		);
		if (player instanceof ServerPlayer serverPlayer) {
			CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(serverPlayer, pos, froe);
		}
		froe.hurtAndBreak(1, player, context.getHand().asEquipmentSlot());
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
			"item.before_the_blight.froe.tooltip",
			"Set on a loaded stump: top for shingles, side for rails."
		).withStyle(ChatFormatting.GRAY));
	}
}
