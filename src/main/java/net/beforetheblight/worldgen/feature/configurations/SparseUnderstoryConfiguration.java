package net.beforetheblight.worldgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Bounded understory-patch controls.
 *
 * <p>The hard attempt, placement, radius, and separation limits are part of the
 * codec contract. A data change therefore cannot silently turn these accents
 * into an impassable wall of shrubs.</p>
 */
public record SparseUnderstoryConfiguration(
	BlockStateProvider foliageProvider,
	int attempts,
	int radius,
	int maximumPlacements,
	int minimumSeparation
) implements FeatureConfiguration {
	public static final Codec<SparseUnderstoryConfiguration> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			BlockStateProvider.CODEC
				.fieldOf("foliage_provider")
				.forGetter(SparseUnderstoryConfiguration::foliageProvider),
			Codec.intRange(1, 16)
				.fieldOf("attempts")
				.forGetter(SparseUnderstoryConfiguration::attempts),
			Codec.intRange(1, 6)
				.fieldOf("radius")
				.forGetter(SparseUnderstoryConfiguration::radius),
			Codec.intRange(1, 6)
				.fieldOf("maximum_placements")
				.forGetter(SparseUnderstoryConfiguration::maximumPlacements),
			Codec.intRange(1, 4)
				.fieldOf("minimum_separation")
				.forGetter(SparseUnderstoryConfiguration::minimumSeparation)
		).apply(instance, SparseUnderstoryConfiguration::new)
	);
}
