package net.beforetheblight.block.furniture;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Vanilla barrel inventory behavior with a furniture-specific fallback title.
 */
public final class SixBoardChestBlockEntity extends BarrelBlockEntity {
	private static final Component DEFAULT_NAME =
		Component.translatable("container.before_the_blight.six_board_chest");

	public SixBoardChestBlockEntity(BlockPos pos, BlockState state) {
		super(pos, state);
	}

	@Override
	protected Component getDefaultName() {
		return DEFAULT_NAME;
	}
}
