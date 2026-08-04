package net.beforetheblight.interaction;

import java.util.Objects;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

/**
 * Bounded, immediate bulk completion for the hand-tool timber processes.
 *
 * <p>The workstation still persists one physical anchor timber and its normal
 * progress state. On the terminal action, up to 63 matching inventory inputs
 * join that anchor, so one completed operation handles at most one vanilla
 * stack. No queue, block entity, config file, or new save-data shape is
 * required.</p>
 */
public final class BulkTimberProcessing {
	public static final int MAX_BATCH_SIZE = 64;
	private static final int CURRENT_ACTION_DURABILITY = 1;

	private BulkTimberProcessing() {
	}

	/**
	 * Consumes the largest safe bounded group of additional matching inputs.
	 *
	 * <p>Survival batches are limited by raw remaining tool durability. This is
	 * deliberately conservative for enchanted tools: Unbreaking may reduce the
	 * eventual damage, but the operation never promises work the tool could not
	 * pay for without it. Creative players neither consume inputs nor damage
	 * tools.</p>
	 */
	public static Batch consumeAdditionalInputs(
		Player player,
		ItemStack tool,
		Item input,
		int durabilityPerAdditionalInput
	) {
		Objects.requireNonNull(player, "player");
		Objects.requireNonNull(tool, "tool");
		Objects.requireNonNull(input, "input");
		if (durabilityPerAdditionalInput < 1) {
			throw new IllegalArgumentException(
				"Additional-input durability must be positive."
			);
		}

		int inventoryLimit = countMatchingInputs(
			player.getInventory(),
			input,
			MAX_BATCH_SIZE - 1
		);
		int toolLimit = additionalInputsAllowedByTool(
			player,
			tool,
			durabilityPerAdditionalInput
		);
		int requestedAdditional = Math.min(inventoryLimit, toolLimit);
		int consumedAdditional = player.hasInfiniteMaterials()
			? requestedAdditional
			: removeMatchingInputs(player, input, requestedAdditional);
		return new Batch(
			consumedAdditional,
			1 + consumedAdditional,
			CURRENT_ACTION_DURABILITY
				+ consumedAdditional * durabilityPerAdditionalInput
		);
	}

	/**
	 * The ordinary nonterminal action: one physical timber and one tool use.
	 */
	public static Batch single() {
		return new Batch(0, 1, CURRENT_ACTION_DURABILITY);
	}

	/**
	 * Drops an arbitrarily sized result as legal item stacks.
	 */
	public static void popOutput(
		Level level,
		BlockPos pos,
		ItemLike output,
		int count
	) {
		Objects.requireNonNull(level, "level");
		Objects.requireNonNull(pos, "pos");
		Objects.requireNonNull(output, "output");
		if (count < 1) {
			throw new IllegalArgumentException("Bulk output count must be positive.");
		}

		ItemStack prototype = new ItemStack(output);
		int maximumStackSize = prototype.getMaxStackSize();
		int remaining = count;
		while (remaining > 0) {
			int nextStackSize = Math.min(remaining, maximumStackSize);
			ItemEntity dropped = new ItemEntity(
				level,
				pos.getX() + 0.5,
				pos.getY() + 0.5,
				pos.getZ() + 0.5,
				new ItemStack(output, nextStackSize)
			);
			dropped.setDefaultPickUpDelay();
			// This is a completed crafting result, not a block-break drop.
			// Spawn it directly so doBlockDrops=false cannot delete already-paid
			// inputs after a successful terminal workstation transition.
			level.addFreshEntity(dropped);
			remaining -= nextStackSize;
		}
	}

	private static int additionalInputsAllowedByTool(
		Player player,
		ItemStack tool,
		int durabilityPerAdditionalInput
	) {
		if (player.hasInfiniteMaterials() || !tool.isDamageableItem()) {
			return MAX_BATCH_SIZE - 1;
		}
		int remainingDurability = Math.max(
			0,
			tool.getMaxDamage() - tool.getDamageValue()
		);
		if (remainingDurability <= CURRENT_ACTION_DURABILITY) {
			return 0;
		}
		return Math.min(
			MAX_BATCH_SIZE - 1,
			(remainingDurability - CURRENT_ACTION_DURABILITY)
				/ durabilityPerAdditionalInput
		);
	}

	private static int countMatchingInputs(
		Inventory inventory,
		Item input,
		int limit
	) {
		int count = 0;
		for (int slot = 0; slot < inventory.getContainerSize() && count < limit; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.is(input)) {
				count += Math.min(stack.getCount(), limit - count);
			}
		}
		return count;
	}

	private static int removeMatchingInputs(
		Player player,
		Item input,
		int limit
	) {
		Inventory inventory = player.getInventory();
		int removed = 0;
		for (int slot = 0; slot < inventory.getContainerSize() && removed < limit; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!stack.is(input)) {
				continue;
			}
			int amount = Math.min(stack.getCount(), limit - removed);
			removed += inventory.removeItem(slot, amount).getCount();
		}
		if (removed > 0) {
			inventory.setChanged();
			player.containerMenu.broadcastChanges();
		}
		return removed;
	}

	/**
	 * One completed bounded transaction, including the physical anchor timber.
	 */
	public record Batch(
		int additionalInputs,
		int totalBatches,
		int durabilityCost
	) {
		public Batch {
			if (additionalInputs < 0 || additionalInputs >= MAX_BATCH_SIZE) {
				throw new IllegalArgumentException(
					"Additional inputs must be in the range 0..63."
				);
			}
			if (totalBatches != additionalInputs + 1) {
				throw new IllegalArgumentException(
					"Total batches must include exactly one physical anchor."
				);
			}
			if (durabilityCost < CURRENT_ACTION_DURABILITY) {
				throw new IllegalArgumentException(
					"Every successful action must cost at least one durability."
				);
			}
		}
	}
}
