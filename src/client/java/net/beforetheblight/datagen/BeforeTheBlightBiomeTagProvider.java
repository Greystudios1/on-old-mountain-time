package net.beforetheblight.datagen;

import java.util.concurrent.CompletableFuture;

import net.beforetheblight.registry.ModTags;
import net.beforetheblight.worldgen.biome.CoveBiomeTags;
import net.beforetheblight.worldgen.biome.GrassyBaldBiomeTags;
import net.beforetheblight.worldgen.biome.ModBiomes;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

public final class BeforeTheBlightBiomeTagProvider extends FabricTagsProvider<Biome> {
	public BeforeTheBlightBiomeTagProvider(
		FabricPackOutput output,
		CompletableFuture<HolderLookup.Provider> registriesFuture
	) {
		super(output, Registries.BIOME, registriesFuture);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void addTags(HolderLookup.Provider registries) {
		builder(ModTags.CHESTNUT_OAK_RIDGE_TARGETS)
			.add(
				Biomes.MEADOW,
				Biomes.FOREST,
				Biomes.BIRCH_FOREST,
				Biomes.STONY_PEAKS
			);

		builder(CoveBiomeTags.HEMLOCK_BEECH_COVE_TARGETS)
			.add(
				Biomes.FOREST,
				Biomes.BIRCH_FOREST,
				Biomes.DARK_FOREST,
				Biomes.TAIGA,
				Biomes.OLD_GROWTH_PINE_TAIGA,
				Biomes.OLD_GROWTH_SPRUCE_TAIGA
			);

		builder(GrassyBaldBiomeTags.GRASSY_BALD_TARGETS)
			.add(
				Biomes.MEADOW,
				Biomes.GROVE,
				Biomes.WINDSWEPT_HILLS,
				Biomes.WINDSWEPT_GRAVELLY_HILLS
			);

		builder(BiomeTags.IS_OVERWORLD).add(ModBiomes.CHESTNUT_OAK_RIDGE);
		builder(BiomeTags.IS_FOREST).add(ModBiomes.CHESTNUT_OAK_RIDGE);
		builder(BiomeTags.IS_HILL).add(ModBiomes.CHESTNUT_OAK_RIDGE);
		builder(BiomeTags.IS_MOUNTAIN).add(ModBiomes.CHESTNUT_OAK_RIDGE);
		builder(BiomeTags.STRONGHOLD_BIASED_TO).add(ModBiomes.CHESTNUT_OAK_RIDGE);
		builder(BiomeTags.HAS_TRIAL_CHAMBERS).add(ModBiomes.CHESTNUT_OAK_RIDGE);
		builder(BiomeTags.HAS_VILLAGE_PLAINS).add(ModBiomes.CHESTNUT_OAK_RIDGE);

		builder(BiomeTags.IS_OVERWORLD).add(ModBiomes.HEMLOCK_BEECH_COVE);
		builder(BiomeTags.IS_FOREST).add(ModBiomes.HEMLOCK_BEECH_COVE);
		builder(BiomeTags.STRONGHOLD_BIASED_TO).add(ModBiomes.HEMLOCK_BEECH_COVE);
		builder(BiomeTags.HAS_TRIAL_CHAMBERS).add(ModBiomes.HEMLOCK_BEECH_COVE);

		builder(BiomeTags.IS_OVERWORLD).add(ModBiomes.GRASSY_BALD);
		builder(BiomeTags.IS_HILL).add(ModBiomes.GRASSY_BALD);
		builder(BiomeTags.IS_MOUNTAIN).add(ModBiomes.GRASSY_BALD);
		builder(BiomeTags.STRONGHOLD_BIASED_TO).add(ModBiomes.GRASSY_BALD);
		builder(BiomeTags.HAS_TRIAL_CHAMBERS).add(ModBiomes.GRASSY_BALD);
	}
}
