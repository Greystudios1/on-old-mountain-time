package net.beforetheblight.item;

import java.util.Optional;

import net.beforetheblight.interaction.BulkTimberProcessing;
import net.beforetheblight.interaction.TimberProcessingRegistry;
import net.beforetheblight.interaction.TimberProcessingRegistry.Transition;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * A deliberately heavy finishing axe reserved for staged timber hewing.
 */
public final class BroadAxeItem extends AxeItem {
	private static final ToolMaterial BROAD_AXE_MATERIAL = new ToolMaterial(
		BlockTags.INCORRECT_FOR_IRON_TOOL,
		256,
		6.0F,
		2.0F,
		14,
		ItemTags.IRON_TOOL_MATERIALS
	);
	private static final float ATTACK_DAMAGE_BASELINE = 7.0F;
	private static final float ATTACK_SPEED_BASELINE = -3.3F;
	private static final int CHIP_PARTICLE_COUNT = 8;

	public BroadAxeItem(Item.Properties properties) {
		super(BROAD_AXE_MATERIAL, ATTACK_DAMAGE_BASELINE, ATTACK_SPEED_BASELINE, properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState currentState = level.getBlockState(pos);
		Optional<Transition> transition = TimberProcessingRegistry.transition(currentState);

		if (transition.isEmpty()) {
			// Broad axes are reserved for registered timber work. In particular,
			// do not delegate to AxeItem's ordinary stripping/scraping behavior.
			return InteractionResult.PASS;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		Player player = context.getPlayer();
		if (player == null) {
			return InteractionResult.PASS;
		}
		Transition selectedTransition = transition.get();
		BlockState nextState = selectedTransition.nextState();
		if (!level.setBlock(pos, nextState, Block.UPDATE_ALL_IMMEDIATE)) {
			return InteractionResult.PASS;
		}

		ItemStack broadAxe = context.getItemInHand();
		var batch = selectedTransition.strike() == 4
			? BulkTimberProcessing.consumeAdditionalInputs(
				player,
				broadAxe,
				selectedTransition.process().sourceBlock().asItem(),
				4
			)
			: BulkTimberProcessing.single();
		if (batch.additionalInputs() > 0) {
			BulkTimberProcessing.popOutput(
				level,
				pos,
				selectedTransition.process().finalBlock(),
				batch.additionalInputs()
			);
		}
		// The client branch does not play locally, so exclude nobody from the
		// authoritative broadcast; the acting player must hear the strike too.
		level.playSound(null, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1.0F, 0.85F);
		level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, nextState));

		if (level instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(
				new BlockParticleOption(ParticleTypes.BLOCK, currentState),
				pos.getX() + 0.5,
				pos.getY() + 0.5,
				pos.getZ() + 0.5,
				CHIP_PARTICLE_COUNT,
				0.3,
				0.3,
				0.3,
				0.04
			);
		}
		if (player instanceof ServerPlayer serverPlayer) {
			CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(serverPlayer, pos, broadAxe);
		}
		// This is the sole durability attempt for the successful strike and
		// any additional logs completed by its terminal bulk transaction.
		broadAxe.hurtAndBreak(
			batch.durabilityCost(),
			player,
			context.getHand().asEquipmentSlot()
		);

		return InteractionResult.SUCCESS;
	}
}
