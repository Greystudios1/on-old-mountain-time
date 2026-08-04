package net.beforetheblight.block.domestic;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Semantic specialization for authored food, tool, fuel, and material piles.
 *
 * <p>It intentionally keeps the same compact state contract as a horizontal
 * prop, while giving registries and future data generation a distinct type to
 * target.</p>
 */
public final class DomesticPileBlock extends HorizontalPropBlock {
	public static final MapCodec<DomesticPileBlock> CODEC =
		RecordCodecBuilder.mapCodec(instance -> instance.group(
			Profile.CODEC.fieldOf("profile").forGetter(DomesticPileBlock::profile),
			propertiesCodec()
		).apply(instance, DomesticPileBlock::new));

	public DomesticPileBlock(Profile profile, BlockBehaviour.Properties properties) {
		super(profile, properties);
	}

	@Override
	public MapCodec<DomesticPileBlock> codec() {
		return CODEC;
	}
}
