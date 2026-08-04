package net.beforetheblight.worldgen.feature.configurations;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Data-owned geometry and decay controls for a rare, hollow old-growth log.
 *
 * <p>The feature keeps an unbroken horizontal core beneath a short open rot
 * pocket, then tapers its contiguous shoulder runs toward the distal end.
 * Structural spine and shoulder blocks cannot decay; only terminal fracture
 * details use the decay roll, so decay can shorten an edge without punching an
 * interior gap through the bole. The stripped provider remains optional on
 * decode for legacy datapacks and falls back to the ordinary trunk provider;
 * fresh encoding always writes it explicitly.</p>
 */
public record HollowFallenLogConfiguration(
	BlockStateProvider trunkProvider,
	BlockStateProvider strippedTrunkProvider,
	IntProvider logLength,
	BlockStateProvider surfaceCoverProvider,
	float surfaceCoverChance,
	float shellDecayChance,
	int maximumUnsupportedSections
) implements FeatureConfiguration {
	public static final Codec<HollowFallenLogConfiguration> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			BlockStateProvider.CODEC
				.fieldOf("trunk_provider")
				.forGetter(HollowFallenLogConfiguration::trunkProvider),
			BlockStateProvider.CODEC
				.optionalFieldOf("stripped_trunk_provider")
				.forGetter(configuration -> Optional.of(configuration.strippedTrunkProvider())),
			IntProviders.codec(5, 12)
				.fieldOf("log_length")
				.forGetter(HollowFallenLogConfiguration::logLength),
			BlockStateProvider.CODEC
				.fieldOf("surface_cover_provider")
				.forGetter(HollowFallenLogConfiguration::surfaceCoverProvider),
			Codec.floatRange(0.0F, 1.0F)
				.fieldOf("surface_cover_chance")
				.forGetter(HollowFallenLogConfiguration::surfaceCoverChance),
			Codec.floatRange(0.0F, 0.35F)
				.fieldOf("shell_decay_chance")
				.forGetter(HollowFallenLogConfiguration::shellDecayChance),
			Codec.intRange(0, 2)
				.fieldOf("maximum_unsupported_sections")
				.forGetter(HollowFallenLogConfiguration::maximumUnsupportedSections)
		).apply(
			instance,
			(
				trunkProvider,
				strippedTrunkProvider,
				logLength,
				surfaceCoverProvider,
				surfaceCoverChance,
				shellDecayChance,
				maximumUnsupportedSections
			) -> new HollowFallenLogConfiguration(
				trunkProvider,
				strippedTrunkProvider.orElse(trunkProvider),
				logLength,
				surfaceCoverProvider,
				surfaceCoverChance,
				shellDecayChance,
				maximumUnsupportedSections
			)
		)
	);
}
