package net.beforetheblight.worldgen.feature;

import java.util.List;

import net.beforetheblight.BeforeTheBlight;
import net.beforetheblight.registry.ModBlocks;
import net.beforetheblight.worldgen.placement.StaggeredChestnutPlacement;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.levelgen.placement.SurfaceWaterDepthFilter;

public final class ModPlacedFeatures {
	public static final ResourceKey<PlacedFeature> CHESTNUT_ORDINARY_CHECKED = key("chestnut_ordinary_checked");
	public static final ResourceKey<PlacedFeature> CHESTNUT_MATURE_CHECKED = key("chestnut_mature_checked");
	public static final ResourceKey<PlacedFeature> CHESTNUT_FOREST_CHECKED = key("chestnut_forest_checked");
	public static final ResourceKey<PlacedFeature> CHESTNUT_OLD_GROWTH_CHECKED = key("chestnut_old_growth_checked");
	public static final ResourceKey<PlacedFeature> CHESTNUT_FOREST_NATURAL = key("chestnut_forest_natural");
	public static final ResourceKey<PlacedFeature> CHESTNUT_OLD_GROWTH_NATURAL = key("chestnut_old_growth_natural");
	public static final ResourceKey<PlacedFeature> CHESTNUT_TREES_CHECKED = key("chestnut_trees_checked");
	public static final ResourceKey<PlacedFeature> CHESTNUT_EDGE_TREES_CHECKED = key("chestnut_edge_trees_checked");
	public static final ResourceKey<PlacedFeature> CHESTNUT_TREES = key("chestnut_trees");
	public static final ResourceKey<PlacedFeature> CHESTNUT_OAK_RIDGE_TREES = key("chestnut_oak_ridge_trees");
	public static final ResourceKey<PlacedFeature> CHESTNUT_OAK_RIDGE_EDGE_TREES = key("chestnut_oak_ridge_edge_trees");
	public static final ResourceKey<PlacedFeature> CHESTNUT_OAK_RIDGE_OAKS = key("chestnut_oak_ridge_oaks");
	public static final ResourceKey<PlacedFeature> CHESTNUT_FALLEN = key("chestnut_fallen");
	public static final ResourceKey<PlacedFeature> CHESTNUT_HOLLOW_FALLEN = key("chestnut_hollow_fallen");
	public static final ResourceKey<PlacedFeature> CHESTNUT_PILE_PATCH = key("chestnut_pile_patch");
	public static final ResourceKey<PlacedFeature> RIDGE_GROUND_COVER = key("ridge_ground_cover");
	public static final ResourceKey<PlacedFeature> RIDGE_UNDERSTORY_PATCH = key("ridge_understory_patch");
	public static final ResourceKey<PlacedFeature> HEMLOCK_TALL_CHECKED = key("hemlock_tall_checked");
	public static final ResourceKey<PlacedFeature> HEMLOCK_SPREADING_CHECKED = key("hemlock_spreading_checked");
	public static final ResourceKey<PlacedFeature> HEMLOCK_OLD_GROWTH_NATURAL = key(
		"hemlock_old_growth_natural"
	);
	public static final ResourceKey<PlacedFeature> BEECH_CHECKED = key("beech_checked");
	public static final ResourceKey<PlacedFeature> BLACK_WALNUT_CHECKED = key("black_walnut_checked");
	public static final ResourceKey<PlacedFeature> BLACK_WALNUT_NATURAL = key("black_walnut_natural");
	public static final ResourceKey<PlacedFeature> HEMLOCK_BEECH_COVE_TREES = key("hemlock_beech_cove_trees");
	public static final ResourceKey<PlacedFeature> RHODODENDRON = key("rhododendron");
	public static final ResourceKey<PlacedFeature> COVE_GROUND_COVER = key("cove_ground_cover");
	public static final ResourceKey<PlacedFeature> COVE_UNDERSTORY_PATCH = key("cove_understory_patch");
	public static final ResourceKey<PlacedFeature> COVE_FALLEN_HEMLOCK = key("cove_fallen_hemlock");
	public static final ResourceKey<PlacedFeature> COVE_HOLLOW_FALLEN_HEMLOCK = key(
		"cove_hollow_fallen_hemlock"
	);

	private ModPlacedFeatures() {
	}

	public static void bootstrap(BootstrapContext<PlacedFeature> context) {
		HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);
		Holder<ConfiguredFeature<?, ?>> ordinary = configuredFeatures.getOrThrow(ModConfiguredFeatures.CHESTNUT_ORDINARY);
		Holder<ConfiguredFeature<?, ?>> mature = configuredFeatures.getOrThrow(ModConfiguredFeatures.CHESTNUT_MATURE);
		Holder<ConfiguredFeature<?, ?>> forest = configuredFeatures.getOrThrow(ModConfiguredFeatures.CHESTNUT_FOREST);
		Holder<ConfiguredFeature<?, ?>> oldGrowth = configuredFeatures.getOrThrow(ModConfiguredFeatures.CHESTNUT_OLD_GROWTH);
		Holder<ConfiguredFeature<?, ?>> naturalForest = configuredFeatures.getOrThrow(
			ModConfiguredFeatures.CHESTNUT_FOREST_NATURAL
		);
		Holder<ConfiguredFeature<?, ?>> naturalOldGrowth = configuredFeatures.getOrThrow(
			ModConfiguredFeatures.CHESTNUT_OLD_GROWTH_NATURAL
		);
		Holder<ConfiguredFeature<?, ?>> trees = configuredFeatures.getOrThrow(ModConfiguredFeatures.CHESTNUT_TREES);
		Holder<ConfiguredFeature<?, ?>> edgeTrees = configuredFeatures.getOrThrow(ModConfiguredFeatures.CHESTNUT_EDGE_TREES);
		Holder<ConfiguredFeature<?, ?>> ridgeTrees = configuredFeatures.getOrThrow(ModConfiguredFeatures.CHESTNUT_OAK_RIDGE_TREES);
		Holder<ConfiguredFeature<?, ?>> ridgeEdgeTrees = configuredFeatures.getOrThrow(
			ModConfiguredFeatures.CHESTNUT_OAK_RIDGE_EDGE_TREES
		);
		Holder<ConfiguredFeature<?, ?>> ridgeOaks = configuredFeatures.getOrThrow(ModConfiguredFeatures.CHESTNUT_OAK_RIDGE_OAKS);
		Holder<ConfiguredFeature<?, ?>> fallen = configuredFeatures.getOrThrow(ModConfiguredFeatures.CHESTNUT_FALLEN);
		Holder<ConfiguredFeature<?, ?>> hollowFallen = configuredFeatures.getOrThrow(
			ModConfiguredFeatures.CHESTNUT_HOLLOW_FALLEN
		);
		Holder<ConfiguredFeature<?, ?>> pilePatch = configuredFeatures.getOrThrow(ModConfiguredFeatures.CHESTNUT_PILE_PATCH);
		Holder<ConfiguredFeature<?, ?>> ridgeGroundCover = configuredFeatures.getOrThrow(
			ModConfiguredFeatures.RIDGE_GROUND_COVER
		);
		Holder<ConfiguredFeature<?, ?>> ridgeUnderstory = configuredFeatures.getOrThrow(
			ModConfiguredFeatures.RIDGE_UNDERSTORY_PATCH
		);
		Holder<ConfiguredFeature<?, ?>> tallHemlock = configuredFeatures.getOrThrow(ModConfiguredFeatures.HEMLOCK_TALL);
		Holder<ConfiguredFeature<?, ?>> spreadingHemlock = configuredFeatures.getOrThrow(
			ModConfiguredFeatures.HEMLOCK_SPREADING
		);
		Holder<ConfiguredFeature<?, ?>> oldGrowthHemlock = configuredFeatures.getOrThrow(
			ModConfiguredFeatures.HEMLOCK_OLD_GROWTH
		);
		Holder<ConfiguredFeature<?, ?>> beech = configuredFeatures.getOrThrow(ModConfiguredFeatures.BEECH);
		Holder<ConfiguredFeature<?, ?>> blackWalnut = configuredFeatures.getOrThrow(
			ModConfiguredFeatures.BLACK_WALNUT
		);
		Holder<ConfiguredFeature<?, ?>> coveTrees = configuredFeatures.getOrThrow(
			ModConfiguredFeatures.HEMLOCK_BEECH_COVE_TREES
		);
		Holder<ConfiguredFeature<?, ?>> rhododendron = configuredFeatures.getOrThrow(
			ModConfiguredFeatures.RHODODENDRON
		);
		Holder<ConfiguredFeature<?, ?>> coveGroundCover = configuredFeatures.getOrThrow(
			ModConfiguredFeatures.COVE_GROUND_COVER
		);
		Holder<ConfiguredFeature<?, ?>> coveUnderstory = configuredFeatures.getOrThrow(
			ModConfiguredFeatures.COVE_UNDERSTORY_PATCH
		);
		Holder<ConfiguredFeature<?, ?>> coveFallenHemlock = configuredFeatures.getOrThrow(
			ModConfiguredFeatures.COVE_FALLEN_HEMLOCK
		);
		Holder<ConfiguredFeature<?, ?>> coveHollowFallenHemlock = configuredFeatures.getOrThrow(
			ModConfiguredFeatures.COVE_HOLLOW_FALLEN_HEMLOCK
		);

		PlacementUtils.register(
			context,
			CHESTNUT_ORDINARY_CHECKED,
			ordinary,
			PlacementUtils.filteredByBlockSurvival(ModBlocks.CHESTNUT_SAPLING)
		);
		PlacementUtils.register(
			context,
			CHESTNUT_FOREST_CHECKED,
			forest,
			PlacementUtils.filteredByBlockSurvival(ModBlocks.CHESTNUT_SAPLING)
		);
		PlacementUtils.register(
			context,
			CHESTNUT_MATURE_CHECKED,
			mature,
			PlacementUtils.filteredByBlockSurvival(ModBlocks.CHESTNUT_SAPLING)
		);
		PlacementUtils.register(
			context,
			CHESTNUT_EDGE_TREES_CHECKED,
			edgeTrees,
			PlacementUtils.filteredByBlockSurvival(ModBlocks.CHESTNUT_SAPLING)
		);
		PlacementUtils.register(
			context,
			CHESTNUT_OLD_GROWTH_CHECKED,
			oldGrowth,
			PlacementUtils.filteredByBlockSurvival(ModBlocks.CHESTNUT_SAPLING)
		);
		PlacementUtils.register(context, CHESTNUT_FOREST_NATURAL, naturalForest, List.of());
		PlacementUtils.register(context, CHESTNUT_OLD_GROWTH_NATURAL, naturalOldGrowth, List.of());
		PlacementUtils.register(
			context,
			CHESTNUT_TREES_CHECKED,
			trees,
			PlacementUtils.filteredByBlockSurvival(ModBlocks.CHESTNUT_SAPLING)
		);

		PlacementUtils.register(
			context,
			CHESTNUT_TREES,
			trees,
			VegetationPlacements.treePlacement(
				PlacementUtils.countExtra(7, 0.1F, 1),
				ModBlocks.CHESTNUT_SAPLING
			)
		);
		PlacementUtils.register(
			context,
			CHESTNUT_OAK_RIDGE_TREES,
			ridgeTrees,
			StaggeredChestnutPlacement.INSTANCE,
			SurfaceWaterDepthFilter.forMaxDepth(0),
			PlacementUtils.HEIGHTMAP_OCEAN_FLOOR,
			BiomeFilter.biome()
		);
		PlacementUtils.register(
			context,
			CHESTNUT_OAK_RIDGE_EDGE_TREES,
			ridgeEdgeTrees,
			VegetationPlacements.treePlacement(CountPlacement.of(2))
		);
		PlacementUtils.register(
			context,
			CHESTNUT_OAK_RIDGE_OAKS,
			ridgeOaks,
			VegetationPlacements.treePlacement(
				// One oak attempt plus a 1/20 extra keeps the independent infill
				// near the chestnut lattice's intended 25% oak / 10% fancy share.
				PlacementUtils.countExtra(1, 0.05F, 1)
			)
		);
		PlacementUtils.register(
			context,
			CHESTNUT_FALLEN,
			fallen,
			VegetationPlacements.treePlacement(
				RarityFilter.onAverageOnceEvery(24),
				ModBlocks.CHESTNUT_SAPLING
			)
		);
		PlacementUtils.register(
			context,
			CHESTNUT_HOLLOW_FALLEN,
			hollowFallen,
			VegetationPlacements.treePlacement(
				RarityFilter.onAverageOnceEvery(96),
				ModBlocks.CHESTNUT_SAPLING
			)
		);
		PlacementUtils.register(
			context,
			CHESTNUT_PILE_PATCH,
			pilePatch,
			RarityFilter.onAverageOnceEvery(6),
			InSquarePlacement.spread(),
			PlacementUtils.HEIGHTMAP_NO_LEAVES,
			BiomeFilter.biome()
		);
		PlacementUtils.register(
			context,
			RIDGE_GROUND_COVER,
			ridgeGroundCover,
			CountPlacement.of(32),
			InSquarePlacement.spread(),
			PlacementUtils.HEIGHTMAP_NO_LEAVES,
			BiomeFilter.biome(),
			PlacementUtils.isEmpty()
		);
		PlacementUtils.register(
			context,
			RIDGE_UNDERSTORY_PATCH,
			ridgeUnderstory,
			CountPlacement.of(1),
			InSquarePlacement.spread(),
			PlacementUtils.HEIGHTMAP_NO_LEAVES,
			BiomeFilter.biome()
		);

		PlacementUtils.register(
			context,
			HEMLOCK_TALL_CHECKED,
			tallHemlock,
			PlacementUtils.filteredByBlockSurvival(ModBlocks.HEMLOCK_SAPLING)
		);
		PlacementUtils.register(
			context,
			HEMLOCK_SPREADING_CHECKED,
			spreadingHemlock,
			PlacementUtils.filteredByBlockSurvival(ModBlocks.HEMLOCK_SAPLING)
		);
		PlacementUtils.register(
			context,
			HEMLOCK_OLD_GROWTH_NATURAL,
			oldGrowthHemlock,
			VegetationPlacements.treePlacement(
				RarityFilter.onAverageOnceEvery(48),
				ModBlocks.HEMLOCK_SAPLING
			)
		);
		PlacementUtils.register(
			context,
			BEECH_CHECKED,
			beech,
			PlacementUtils.filteredByBlockSurvival(ModBlocks.AMERICAN_BEECH_SAPLING)
		);
		PlacementUtils.register(
			context,
			BLACK_WALNUT_CHECKED,
			blackWalnut,
			PlacementUtils.filteredByBlockSurvival(ModBlocks.BLACK_WALNUT_SAPLING)
		);
		PlacementUtils.register(
			context,
			BLACK_WALNUT_NATURAL,
			blackWalnut,
			VegetationPlacements.treePlacement(
				RarityFilter.onAverageOnceEvery(12),
				ModBlocks.BLACK_WALNUT_SAPLING
			)
		);
		PlacementUtils.register(
			context,
			HEMLOCK_BEECH_COVE_TREES,
			coveTrees,
			VegetationPlacements.treePlacement(CountPlacement.of(14))
		);
		PlacementUtils.register(
			context,
			RHODODENDRON,
			rhododendron,
			CountPlacement.of(8),
			InSquarePlacement.spread(),
			PlacementUtils.HEIGHTMAP_NO_LEAVES,
			BiomeFilter.biome(),
			PlacementUtils.filteredByBlockSurvival(Blocks.AZALEA)
		);
		PlacementUtils.register(
			context,
			COVE_GROUND_COVER,
			coveGroundCover,
			CountPlacement.of(32),
			InSquarePlacement.spread(),
			PlacementUtils.HEIGHTMAP_NO_LEAVES,
			BiomeFilter.biome(),
			PlacementUtils.isEmpty(),
			PlacementUtils.filteredByBlockSurvival(Blocks.MOSS_CARPET)
		);
		PlacementUtils.register(
			context,
			COVE_UNDERSTORY_PATCH,
			coveUnderstory,
			CountPlacement.of(1),
			InSquarePlacement.spread(),
			PlacementUtils.HEIGHTMAP_NO_LEAVES,
			BiomeFilter.biome()
		);
		PlacementUtils.register(
			context,
			COVE_FALLEN_HEMLOCK,
			coveFallenHemlock,
			VegetationPlacements.treePlacement(
				RarityFilter.onAverageOnceEvery(16),
				ModBlocks.HEMLOCK_SAPLING
			)
		);
		PlacementUtils.register(
			context,
			COVE_HOLLOW_FALLEN_HEMLOCK,
			coveHollowFallenHemlock,
			VegetationPlacements.treePlacement(
				RarityFilter.onAverageOnceEvery(72),
				ModBlocks.HEMLOCK_SAPLING
			)
		);
	}

	private static ResourceKey<PlacedFeature> key(String path) {
		return ResourceKey.create(Registries.PLACED_FEATURE, BeforeTheBlight.id(path));
	}
}
