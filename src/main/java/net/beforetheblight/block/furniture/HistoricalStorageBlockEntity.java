package net.beforetheblight.block.furniture;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Barrel persistence with a style-specific translated furniture title. */
public final class HistoricalStorageBlockEntity extends BarrelBlockEntity {
	public HistoricalStorageBlockEntity(BlockPos pos, BlockState state) {
		super(pos, state);
	}

	@Override
	protected Component getDefaultName() {
		return this.getBlockState().getBlock() instanceof HistoricalStorageBlock storage
			? Component.translatable(storage.style().containerTranslationKey())
			: Component.translatable("container.barrel");
	}
}
