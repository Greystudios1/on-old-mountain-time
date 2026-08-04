package net.beforetheblight.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Internal placed-log state used while a timber is being squared by hand.
 * The source log is strike zero; these three persisted states are strikes one
 * through three, and the fourth strike replaces this block with the final beam.
 */
public final class HewingLogBlock extends RotatedPillarBlock {
	public static final IntegerProperty HEWING_STAGE = IntegerProperty.create("hewing_stage", 1, 3);
	public static final MapCodec<HewingLogBlock> CODEC = simpleCodec(HewingLogBlock::new);

	public HewingLogBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.defaultBlockState().setValue(HEWING_STAGE, 1));
	}

	@Override
	public MapCodec<HewingLogBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(HEWING_STAGE);
	}
}
