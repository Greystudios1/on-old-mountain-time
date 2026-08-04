package net.beforetheblight.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * A full-section hewn chestnut beam splice with a visible pegged scarf joint.
 *
 * <p>The inherited {@code axis=x|y|z} state keeps the timber grain and joint
 * aligned with the member being repaired.</p>
 */
public final class PeggedChestnutScarfJointBlock extends RotatedPillarBlock {
	public static final MapCodec<PeggedChestnutScarfJointBlock> CODEC =
		simpleCodec(PeggedChestnutScarfJointBlock::new);

	public PeggedChestnutScarfJointBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	public MapCodec<PeggedChestnutScarfJointBlock> codec() {
		return CODEC;
	}
}
