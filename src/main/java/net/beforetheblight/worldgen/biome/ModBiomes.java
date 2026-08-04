package net.beforetheblight.worldgen.biome;

import net.beforetheblight.BeforeTheBlight;
import net.beforetheblight.worldgen.feature.ModPlacedFeatures;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.biome.OverworldBiomes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.attribute.BackgroundMusic;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public final class ModBiomes {
	public static final ResourceKey<Biome> CHESTNUT_OAK_RIDGE = ResourceKey.create(
		Registries.BIOME,
		BeforeTheBlight.id("chestnut_oak_ridge")
	);
	public static final ResourceKey<Biome> HEMLOCK_BEECH_COVE = ResourceKey.create(
		Registries.BIOME,
		BeforeTheBlight.id("hemlock_beech_cove")
	);
	public static final ResourceKey<Biome> GRASSY_BALD = ResourceKey.create(
		Registries.BIOME,
		BeforeTheBlight.id("grassy_bald")
	);

	private ModBiomes() {
	}

	public static void bootstrap(BootstrapContext<Biome> context) {
		HolderGetter<PlacedFeature> placedFeatures = context.lookup(Registries.PLACED_FEATURE);
		HolderGetter<ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);
		context.register(CHESTNUT_OAK_RIDGE, chestnutOakRidge(placedFeatures, carvers));
		context.register(HEMLOCK_BEECH_COVE, hemlockBeechCove(placedFeatures, carvers));
		context.register(GRASSY_BALD, grassyBald(placedFeatures, carvers));
	}

	private static Biome chestnutOakRidge(
		HolderGetter<PlacedFeature> placedFeatures,
		HolderGetter<ConfiguredWorldCarver<?>> carvers
	) {
		BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
		OverworldBiomes.globalOverworldGeneration(generation);
		BiomeDefaultFeatures.addDefaultOres(generation);
		BiomeDefaultFeatures.addDefaultSoftDisks(generation);
		generation.addFeature(
			GenerationStep.Decoration.VEGETAL_DECORATION,
			ModPlacedFeatures.CHESTNUT_OAK_RIDGE_TREES
		);
		generation.addFeature(
			GenerationStep.Decoration.VEGETAL_DECORATION,
			ModPlacedFeatures.CHESTNUT_OAK_RIDGE_EDGE_TREES
		);
		generation.addFeature(
			GenerationStep.Decoration.VEGETAL_DECORATION,
			ModPlacedFeatures.CHESTNUT_OAK_RIDGE_OAKS
		);
		BiomeDefaultFeatures.addFerns(generation);
		generation.addFeature(
			GenerationStep.Decoration.VEGETAL_DECORATION,
			ModPlacedFeatures.CHESTNUT_FALLEN
		);
		generation.addFeature(
			GenerationStep.Decoration.VEGETAL_DECORATION,
			ModPlacedFeatures.CHESTNUT_HOLLOW_FALLEN
		);
		generation.addFeature(
			GenerationStep.Decoration.VEGETAL_DECORATION,
			ModPlacedFeatures.CHESTNUT_PILE_PATCH
		);
		BiomeDefaultFeatures.addBushes(generation);
		BiomeDefaultFeatures.addDefaultFlowers(generation);
		BiomeDefaultFeatures.addForestGrass(generation);
		BiomeDefaultFeatures.addDefaultMushrooms(generation);
		BiomeDefaultFeatures.addLeafLitterPatch(generation);
		BiomeDefaultFeatures.addDefaultExtraVegetation(generation, true);
		generation.addFeature(
			GenerationStep.Decoration.VEGETAL_DECORATION,
			ModPlacedFeatures.RIDGE_GROUND_COVER
		);
		generation.addFeature(
			GenerationStep.Decoration.VEGETAL_DECORATION,
			ModPlacedFeatures.RIDGE_UNDERSTORY_PATCH
		);

		MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
		BiomeDefaultFeatures.farmAnimals(mobs);
		BiomeDefaultFeatures.commonSpawns(mobs);
		mobs.addSpawn(MobCategory.CREATURE, 5, new MobSpawnSettings.SpawnerData(EntityType.WOLF, 4, 4));

		return OverworldBiomes.baseBiome(0.68F, 0.8F)
			.setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SoundEvents.MUSIC_BIOME_FOREST))
			.specialEffects(
				new BiomeSpecialEffects.Builder()
					.waterColor(0x3F76E4)
					.grassColorOverride(0x709650)
					.foliageColorOverride(0x5F853F)
					.dryFoliageColorOverride(0xA36D46)
					.build()
			)
			.mobSpawnSettings(mobs.build())
			.generationSettings(generation.build())
			.build();
	}

	private static Biome hemlockBeechCove(
		HolderGetter<PlacedFeature> placedFeatures,
		HolderGetter<ConfiguredWorldCarver<?>> carvers
	) {
		BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
		OverworldBiomes.globalOverworldGeneration(generation);
		BiomeDefaultFeatures.addDefaultOres(generation);
		BiomeDefaultFeatures.addDefaultSoftDisks(generation);
		generation.addFeature(
			GenerationStep.Decoration.VEGETAL_DECORATION,
			ModPlacedFeatures.HEMLOCK_BEECH_COVE_TREES
		);
		generation.addFeature(
			GenerationStep.Decoration.VEGETAL_DECORATION,
			ModPlacedFeatures.HEMLOCK_OLD_GROWTH_NATURAL
		);
		generation.addFeature(
			GenerationStep.Decoration.VEGETAL_DECORATION,
			ModPlacedFeatures.BLACK_WALNUT_NATURAL
		);
		BiomeDefaultFeatures.addFerns(generation);
		generation.addFeature(
			GenerationStep.Decoration.VEGETAL_DECORATION,
			ModPlacedFeatures.COVE_FALLEN_HEMLOCK
		);
		generation.addFeature(
			GenerationStep.Decoration.VEGETAL_DECORATION,
			ModPlacedFeatures.COVE_HOLLOW_FALLEN_HEMLOCK
		);
		generation.addFeature(
			GenerationStep.Decoration.VEGETAL_DECORATION,
			ModPlacedFeatures.RHODODENDRON
		);
		generation.addFeature(
			GenerationStep.Decoration.VEGETAL_DECORATION,
			ModPlacedFeatures.COVE_GROUND_COVER
		);
		BiomeDefaultFeatures.addBushes(generation);
		BiomeDefaultFeatures.addForestGrass(generation);
		BiomeDefaultFeatures.addDefaultMushrooms(generation);
		BiomeDefaultFeatures.addLeafLitterPatch(generation);
		BiomeDefaultFeatures.addDefaultExtraVegetation(generation, true);
		generation.addFeature(
			GenerationStep.Decoration.VEGETAL_DECORATION,
			ModPlacedFeatures.COVE_UNDERSTORY_PATCH
		);

		MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
		BiomeDefaultFeatures.farmAnimals(mobs);
		BiomeDefaultFeatures.commonSpawns(mobs);
		mobs.addSpawn(MobCategory.CREATURE, 6, new MobSpawnSettings.SpawnerData(EntityType.WOLF, 2, 4));

		return OverworldBiomes.baseBiome(0.55F, 0.95F)
			.setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SoundEvents.MUSIC_BIOME_FOREST))
			.specialEffects(
				new BiomeSpecialEffects.Builder()
					.waterColor(0x355D62)
					.grassColorOverride(0x3F6542)
					.foliageColorOverride(0x2D5637)
					.dryFoliageColorOverride(0x715C3E)
					.build()
			)
			.mobSpawnSettings(mobs.build())
			.generationSettings(generation.build())
			.build();
	}

	/**
	 * A deliberately open Appalachian summit meadow. It reuses the restrained
	 * vanilla meadow vegetation set and intentionally registers no tree feature,
	 * keeping the bald readable as a clearing between the two forest biomes.
	 */
	private static Biome grassyBald(
		HolderGetter<PlacedFeature> placedFeatures,
		HolderGetter<ConfiguredWorldCarver<?>> carvers
	) {
		BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
		OverworldBiomes.globalOverworldGeneration(generation);
		BiomeDefaultFeatures.addDefaultOres(generation);
		BiomeDefaultFeatures.addDefaultSoftDisks(generation);
		BiomeDefaultFeatures.addExtraEmeralds(generation);
		BiomeDefaultFeatures.addPlainGrass(generation);
		BiomeDefaultFeatures.addDefaultFlowers(generation);
		BiomeDefaultFeatures.addForestGrass(generation);
		BiomeDefaultFeatures.addDefaultMushrooms(generation);
		BiomeDefaultFeatures.addDefaultExtraVegetation(generation, false);

		MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
		BiomeDefaultFeatures.farmAnimals(mobs);
		BiomeDefaultFeatures.commonSpawns(mobs);

		return OverworldBiomes.baseBiome(0.58F, 0.72F)
			.setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SoundEvents.MUSIC_BIOME_MEADOW))
			.specialEffects(
				new BiomeSpecialEffects.Builder()
					.waterColor(0x3F76E4)
					.grassColorOverride(0x83A957)
					.foliageColorOverride(0x70934B)
					.dryFoliageColorOverride(0xB28A54)
					.build()
			)
			.mobSpawnSettings(mobs.build())
			.generationSettings(generation.build())
			.build();
	}
}
