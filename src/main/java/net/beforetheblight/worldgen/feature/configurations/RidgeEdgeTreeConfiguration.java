package net.beforetheblight.worldgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/** Data-owned one-by-one chestnut choice for the Ridge boundary stream. */
public record RidgeEdgeTreeConfiguration(
	Holder<PlacedFeature> edgeChestnut,
	int edgeRadius
) implements FeatureConfiguration {
	public static final Codec<RidgeEdgeTreeConfiguration> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			PlacedFeature.CODEC.fieldOf("edge_chestnut").forGetter(RidgeEdgeTreeConfiguration::edgeChestnut),
			Codec.intRange(4, 64).fieldOf("edge_radius").forGetter(RidgeEdgeTreeConfiguration::edgeRadius)
		).apply(instance, RidgeEdgeTreeConfiguration::new)
	);
}
