package net.beforetheblight.datagen;

import net.beforetheblight.worldgen.biome.ModBiomes;
import net.beforetheblight.worldgen.feature.ModConfiguredFeatures;
import net.beforetheblight.worldgen.feature.ModPlacedFeatures;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;

public final class BeforeTheBlightDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator dataGenerator) {
		FabricDataGenerator.Pack pack = dataGenerator.createPack();

		pack.addProvider(BeforeTheBlightDynamicRegistryProvider::new);
		pack.addProvider(BeforeTheBlightBiomeTagProvider::new);
		pack.addProvider(BeforeTheBlightContentCatalogProvider::new);
		pack.addProvider(BeforeTheBlightModelProvider::new);
		pack.addProvider(BeforeTheBlightBlockLootProvider::new);
		pack.addProvider(BeforeTheBlightRecipeProvider::new);

		BeforeTheBlightBlockTagProvider blockTags = pack.addProvider(BeforeTheBlightBlockTagProvider::new);
		pack.addProvider((output, registriesFuture) ->
			new BeforeTheBlightItemTagProvider(output, registriesFuture, blockTags));

		pack.addProvider(BeforeTheBlightEnglishLanguageProvider::new);
	}

	@Override
	public void buildRegistry(RegistrySetBuilder registryBuilder) {
		registryBuilder.add(Registries.CONFIGURED_FEATURE, ModConfiguredFeatures::bootstrap);
		registryBuilder.add(Registries.PLACED_FEATURE, ModPlacedFeatures::bootstrap);
		registryBuilder.add(Registries.BIOME, ModBiomes::bootstrap);
	}
}
