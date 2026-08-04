package net.beforetheblight.worldgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Data-owned tree choices for the Chestnut-Oak Ridge canopy selector.
 *
 * <p>This selector owns only interior two-by-two and three-by-three chestnuts.
 * Boundary one-by-one chestnuts and oak infill have independent configured and
 * placed features, so neither can consume or inherit a large-tree lattice
 * site.</p>
 */
public record RidgeTreeConfiguration(
	Holder<PlacedFeature> forestChestnut,
	Holder<PlacedFeature> oldGrowthChestnut,
	int edgeRadius,
	float oldGrowthChanceInInterior
) implements FeatureConfiguration {
	public static final Codec<RidgeTreeConfiguration> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			PlacedFeature.CODEC.fieldOf("forest_chestnut").forGetter(RidgeTreeConfiguration::forestChestnut),
			PlacedFeature.CODEC.fieldOf("old_growth_chestnut").forGetter(RidgeTreeConfiguration::oldGrowthChestnut),
			Codec.intRange(4, 64).fieldOf("edge_radius").forGetter(RidgeTreeConfiguration::edgeRadius),
			Codec.floatRange(0.0F, 1.0F)
				.fieldOf("old_growth_chance_in_interior")
				.forGetter(RidgeTreeConfiguration::oldGrowthChanceInInterior)
		).apply(instance, RidgeTreeConfiguration::new)
	);
}
