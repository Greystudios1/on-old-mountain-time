package net.beforetheblight.worldgen.feature;

import java.util.List;
import java.util.OptionalInt;

import net.beforetheblight.BeforeTheBlight;
import net.beforetheblight.registry.ModBlocks;
import net.beforetheblight.worldgen.feature.configurations.HollowFallenLogConfiguration;
import net.beforetheblight.worldgen.feature.configurations.RidgeEdgeTreeConfiguration;
import net.beforetheblight.worldgen.feature.configurations.RidgeTreeConfiguration;
import net.beforetheblight.worldgen.feature.configurations.SparseUnderstoryConfiguration;
import net.beforetheblight.worldgen.feature.foliageplacer.TieredHemlockFoliagePlacer;
import net.beforetheblight.worldgen.feature.trunkplacer.ForestChestnutTrunkPlacer;
import net.beforetheblight.worldgen.feature.trunkplacer.HollowChestnutTrunkPlacer;
import net.beforetheblight.worldgen.feature.trunkplacer.OldGrowthHemlockTrunkPlacer;
import net.beforetheblight.worldgen.feature.trunkplacer.TieredHemlockTrunkPlacer;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.TreePlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FallenTreeConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.ThreeLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FancyFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.FancyTrunkPlacer;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public final class ModConfiguredFeatures {
	private static final float OLD_GROWTH_CHANCE = 0.05F;
	private static final float MATURE_CHANCE_AFTER_OLD_GROWTH = 0.25F / (1.0F - OLD_GROWTH_CHANCE);
	private static final float RIDGE_FANCY_SHARE_WITHIN_OAK_INFILL = 2.0F / 7.0F;
	private static final float RIDGE_OLD_GROWTH_CHANCE_IN_INTERIOR = 1.0F / 9.0F;
	private static final float RIDGE_EDGE_MATURE_CHANCE = 0.30F;
	private static final float COVE_TALL_HEMLOCK_CHANCE = 0.60F;
	private static final float COVE_SPREADING_HEMLOCK_CHANCE_AFTER_TALL = 0.50F;
	public static final int RIDGE_EDGE_RADIUS = 4;

	public static final ResourceKey<ConfiguredFeature<?, ?>> CHESTNUT_ORDINARY = key("chestnut_ordinary");
	public static final ResourceKey<ConfiguredFeature<?, ?>> CHESTNUT_MATURE = key("chestnut_mature");
	public static final ResourceKey<ConfiguredFeature<?, ?>> CHESTNUT_FOREST = key("chestnut_forest");
	public static final ResourceKey<ConfiguredFeature<?, ?>> CHESTNUT_OLD_GROWTH = key("chestnut_old_growth");
	public static final ResourceKey<ConfiguredFeature<?, ?>> CHESTNUT_FOREST_NATURAL = key("chestnut_forest_natural");
	public static final ResourceKey<ConfiguredFeature<?, ?>> CHESTNUT_OLD_GROWTH_NATURAL = key("chestnut_old_growth_natural");
	public static final ResourceKey<ConfiguredFeature<?, ?>> CHESTNUT_TREES = key("chestnut_trees");
	public static final ResourceKey<ConfiguredFeature<?, ?>> CHESTNUT_EDGE_TREES = key("chestnut_edge_trees");
	public static final ResourceKey<ConfiguredFeature<?, ?>> CHESTNUT_FALLEN = key("chestnut_fallen");
	public static final ResourceKey<ConfiguredFeature<?, ?>> CHESTNUT_HOLLOW_FALLEN = key("chestnut_hollow_fallen");
	public static final ResourceKey<ConfiguredFeature<?, ?>> CHESTNUT_PILE_PATCH = key("chestnut_pile_patch");
	public static final ResourceKey<ConfiguredFeature<?, ?>> RIDGE_GROUND_COVER = key("ridge_ground_cover");
	public static final ResourceKey<ConfiguredFeature<?, ?>> RIDGE_UNDERSTORY_PATCH = key("ridge_understory_patch");
	public static final ResourceKey<ConfiguredFeature<?, ?>> CHESTNUT_OAK_RIDGE_TREES = key("chestnut_oak_ridge_trees");
	public static final ResourceKey<ConfiguredFeature<?, ?>> CHESTNUT_OAK_RIDGE_EDGE_TREES = key("chestnut_oak_ridge_edge_trees");
	public static final ResourceKey<ConfiguredFeature<?, ?>> CHESTNUT_OAK_RIDGE_OAKS = key("chestnut_oak_ridge_oaks");
	public static final ResourceKey<ConfiguredFeature<?, ?>> HEMLOCK_TALL = key("hemlock_tall");
	public static final ResourceKey<ConfiguredFeature<?, ?>> HEMLOCK_SPREADING = key("hemlock_spreading");
	public static final ResourceKey<ConfiguredFeature<?, ?>> HEMLOCK_OLD_GROWTH = key("hemlock_old_growth");
	public static final ResourceKey<ConfiguredFeature<?, ?>> BEECH = key("beech");
	public static final ResourceKey<ConfiguredFeature<?, ?>> BLACK_WALNUT = key("black_walnut");
	public static final ResourceKey<ConfiguredFeature<?, ?>> HEMLOCK_BEECH_COVE_TREES = key("hemlock_beech_cove_trees");
	public static final ResourceKey<ConfiguredFeature<?, ?>> RHODODENDRON = key("rhododendron");
	public static final ResourceKey<ConfiguredFeature<?, ?>> COVE_GROUND_COVER = key("cove_ground_cover");
	public static final ResourceKey<ConfiguredFeature<?, ?>> COVE_UNDERSTORY_PATCH = key("cove_understory_patch");
	public static final ResourceKey<ConfiguredFeature<?, ?>> COVE_FALLEN_HEMLOCK = key("cove_fallen_hemlock");
	public static final ResourceKey<ConfiguredFeature<?, ?>> COVE_HOLLOW_FALLEN_HEMLOCK = key(
		"cove_hollow_fallen_hemlock"
	);

	private ModConfiguredFeatures() {
	}

	public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
		FeatureUtils.register(context, CHESTNUT_ORDINARY, Feature.TREE, ordinaryChestnut());
		FeatureUtils.register(context, CHESTNUT_MATURE, Feature.TREE, matureChestnut());
		FeatureUtils.register(context, CHESTNUT_FOREST, Feature.TREE, forestChestnut());
		FeatureUtils.register(context, CHESTNUT_OLD_GROWTH, Feature.TREE, oldGrowthChestnut());
		FeatureUtils.register(context, CHESTNUT_FOREST_NATURAL, Feature.TREE, naturalForestChestnut());
		FeatureUtils.register(context, CHESTNUT_OLD_GROWTH_NATURAL, Feature.TREE, naturalOldGrowthChestnut());
		FeatureUtils.register(context, HEMLOCK_TALL, Feature.TREE, tallHemlock());
		FeatureUtils.register(context, HEMLOCK_SPREADING, Feature.TREE, spreadingHemlock());
		FeatureUtils.register(context, HEMLOCK_OLD_GROWTH, Feature.TREE, oldGrowthHemlock());
		FeatureUtils.register(context, BEECH, Feature.TREE, beech());
		FeatureUtils.register(context, BLACK_WALNUT, Feature.TREE, blackWalnut());
		FeatureUtils.register(
			context,
			RHODODENDRON,
			Feature.SIMPLE_BLOCK,
			new SimpleBlockConfiguration(
				new WeightedStateProvider(
					WeightedList.<BlockState>builder()
						.add(Blocks.AZALEA.defaultBlockState(), 4)
						.add(Blocks.FLOWERING_AZALEA.defaultBlockState(), 1)
				)
			)
		);
		FeatureUtils.register(
			context,
			COVE_GROUND_COVER,
			Feature.SIMPLE_BLOCK,
			new SimpleBlockConfiguration(
				new WeightedStateProvider(
					WeightedList.<BlockState>builder()
						.add(Blocks.MOSS_CARPET.defaultBlockState(), 8)
						.add(Blocks.FERN.defaultBlockState(), 3)
						.add(Blocks.BROWN_MUSHROOM.defaultBlockState(), 1)
				)
			)
		);
		FeatureUtils.register(
			context,
			COVE_UNDERSTORY_PATCH,
			ModFeatures.SPARSE_UNDERSTORY,
			new SparseUnderstoryConfiguration(
				new WeightedStateProvider(
					WeightedList.<BlockState>builder()
						.add(ModBlocks.MOUNTAIN_LAUREL.defaultBlockState(), 1)
						.add(ModBlocks.FOREST_DUFF.defaultBlockState(), 5)
				),
				10,
				4,
				4,
				2
			)
		);
		FeatureUtils.register(
			context,
			COVE_FALLEN_HEMLOCK,
			Feature.FALLEN_TREE,
			new FallenTreeConfiguration.FallenTreeConfigurationBuilder(
				BlockStateProvider.simple(ModBlocks.HEMLOCK_LOG),
				UniformInt.of(6, 10)
			).build()
		);
		FeatureUtils.register(
			context,
			COVE_HOLLOW_FALLEN_HEMLOCK,
			ModFeatures.HOLLOW_FALLEN_LOG,
			new HollowFallenLogConfiguration(
				BlockStateProvider.simple(ModBlocks.HEMLOCK_LOG),
				BlockStateProvider.simple(ModBlocks.STRIPPED_HEMLOCK_LOG),
				UniformInt.of(7, 11),
				new WeightedStateProvider(
					WeightedList.<BlockState>builder()
						.add(Blocks.MOSS_CARPET.defaultBlockState(), 4)
						.add(Blocks.LEAF_LITTER.defaultBlockState(), 1)
				),
				0.50F,
				0.12F,
				2
			)
		);
		FeatureUtils.register(
			context,
			CHESTNUT_FALLEN,
			Feature.FALLEN_TREE,
			new FallenTreeConfiguration.FallenTreeConfigurationBuilder(
				BlockStateProvider.simple(ModBlocks.CHESTNUT_LOG),
				UniformInt.of(4, 7)
			).build()
		);
		FeatureUtils.register(
			context,
			CHESTNUT_HOLLOW_FALLEN,
			ModFeatures.HOLLOW_FALLEN_LOG,
			new HollowFallenLogConfiguration(
				BlockStateProvider.simple(ModBlocks.CHESTNUT_LOG),
				BlockStateProvider.simple(ModBlocks.STRIPPED_CHESTNUT_LOG),
				UniformInt.of(6, 10),
				new WeightedStateProvider(
					WeightedList.<BlockState>builder()
						.add(Blocks.LEAF_LITTER.defaultBlockState(), 3)
						.add(Blocks.MOSS_CARPET.defaultBlockState(), 1)
				),
				0.32F,
				0.08F,
				2
			)
		);
		FeatureUtils.register(
			context,
			CHESTNUT_PILE_PATCH,
			Feature.SIMPLE_BLOCK,
			new SimpleBlockConfiguration(BlockStateProvider.simple(ModBlocks.CHESTNUT_PILE))
		);
		FeatureUtils.register(
			context,
			RIDGE_GROUND_COVER,
			Feature.SIMPLE_BLOCK,
			new SimpleBlockConfiguration(
				new WeightedStateProvider(
					WeightedList.<BlockState>builder()
						.add(Blocks.SHORT_GRASS.defaultBlockState(), 5)
						.add(Blocks.FERN.defaultBlockState(), 3)
						.add(Blocks.LEAF_LITTER.defaultBlockState(), 2)
				)
			)
		);
		FeatureUtils.register(
			context,
			RIDGE_UNDERSTORY_PATCH,
			ModFeatures.SPARSE_UNDERSTORY,
			new SparseUnderstoryConfiguration(
				new WeightedStateProvider(
					WeightedList.<BlockState>builder()
						.add(ModBlocks.MOUNTAIN_LAUREL.defaultBlockState(), 3)
						.add(ModBlocks.LOWBUSH_BLUEBERRY.defaultBlockState(), 5)
						.add(ModBlocks.FOREST_DUFF.defaultBlockState(), 4)
				),
				12,
				5,
				5,
				2
			)
		);

		HolderGetter<PlacedFeature> placedFeatures = context.lookup(Registries.PLACED_FEATURE);
		Holder<PlacedFeature> ordinary = placedFeatures.getOrThrow(ModPlacedFeatures.CHESTNUT_ORDINARY_CHECKED);
		Holder<PlacedFeature> mature = placedFeatures.getOrThrow(ModPlacedFeatures.CHESTNUT_MATURE_CHECKED);
		Holder<PlacedFeature> strictOldGrowth = placedFeatures.getOrThrow(
			ModPlacedFeatures.CHESTNUT_OLD_GROWTH_CHECKED
		);
		Holder<PlacedFeature> naturalOldGrowth = placedFeatures.getOrThrow(
			ModPlacedFeatures.CHESTNUT_OLD_GROWTH_NATURAL
		);
		Holder<PlacedFeature> naturalForest = placedFeatures.getOrThrow(
			ModPlacedFeatures.CHESTNUT_FOREST_NATURAL
		);
		Holder<PlacedFeature> edgeChestnuts = placedFeatures.getOrThrow(ModPlacedFeatures.CHESTNUT_EDGE_TREES_CHECKED);
		Holder<PlacedFeature> oak = placedFeatures.getOrThrow(TreePlacements.OAK_CHECKED);
		Holder<PlacedFeature> fancyOak = placedFeatures.getOrThrow(TreePlacements.FANCY_OAK_CHECKED);
		Holder<PlacedFeature> tallHemlock = placedFeatures.getOrThrow(ModPlacedFeatures.HEMLOCK_TALL_CHECKED);
		Holder<PlacedFeature> spreadingHemlock = placedFeatures.getOrThrow(
			ModPlacedFeatures.HEMLOCK_SPREADING_CHECKED
		);
		Holder<PlacedFeature> beech = placedFeatures.getOrThrow(ModPlacedFeatures.BEECH_CHECKED);

		// RandomSelectorFeature tests entries in order. The second probability is
		// conditional on the old-growth roll failing, yielding 70% / 25% / 5%.
		FeatureUtils.register(
			context,
			CHESTNUT_TREES,
			Feature.RANDOM_SELECTOR,
			new RandomFeatureConfiguration(
				List.of(
					new WeightedPlacedFeature(strictOldGrowth, OLD_GROWTH_CHANCE),
					new WeightedPlacedFeature(mature, MATURE_CHANCE_AFTER_OLD_GROWTH)
				),
				ordinary
			)
		);

		FeatureUtils.register(
			context,
			CHESTNUT_EDGE_TREES,
			Feature.RANDOM_SELECTOR,
			new RandomFeatureConfiguration(
				List.of(new WeightedPlacedFeature(mature, RIDGE_EDGE_MATURE_CHANCE)),
				ordinary
			)
		);

		// The lattice is large-chestnut-only: boundary anchors do not place a
		// tree, while interior anchors choose 8/9 two-by-two forest and 1/9
		// three-by-three old growth. Small edge chestnuts and oak infill are
		// independent streams, so neither inherits or consumes a lattice site.
		FeatureUtils.register(
			context,
			CHESTNUT_OAK_RIDGE_TREES,
			ModFeatures.RIDGE_TREE_SELECTOR,
			new RidgeTreeConfiguration(
				naturalForest,
				naturalOldGrowth,
				RIDGE_EDGE_RADIUS,
				RIDGE_OLD_GROWTH_CHANCE_IN_INTERIOR
			)
		);

		FeatureUtils.register(
			context,
			CHESTNUT_OAK_RIDGE_EDGE_TREES,
			ModFeatures.RIDGE_EDGE_TREE_SELECTOR,
			new RidgeEdgeTreeConfiguration(edgeChestnuts, RIDGE_EDGE_RADIUS)
		);

		FeatureUtils.register(
			context,
			CHESTNUT_OAK_RIDGE_OAKS,
			Feature.RANDOM_SELECTOR,
			new RandomFeatureConfiguration(
				List.of(new WeightedPlacedFeature(fancyOak, RIDGE_FANCY_SHARE_WITHIN_OAK_INFILL)),
				oak
			)
		);

		// RandomSelectorFeature evaluates probabilities in sequence. After the
		// 60% tall-hemlock branch fails, a 50% conditional roll yields an
		// absolute 20% spreading hemlock and leaves 20% American beech as the default.
		FeatureUtils.register(
			context,
			HEMLOCK_BEECH_COVE_TREES,
			Feature.RANDOM_SELECTOR,
			new RandomFeatureConfiguration(
				List.of(
					new WeightedPlacedFeature(tallHemlock, COVE_TALL_HEMLOCK_CHANCE),
					new WeightedPlacedFeature(
						spreadingHemlock,
						COVE_SPREADING_HEMLOCK_CHANCE_AFTER_TALL
					)
				),
				beech
			)
		);
	}

	private static TreeConfiguration ordinaryChestnut() {
		return new TreeConfiguration.TreeConfigurationBuilder(
				BlockStateProvider.simple(ModBlocks.CHESTNUT_LOG),
				new FancyTrunkPlacer(16, 2, 1),
				BlockStateProvider.simple(ModBlocks.CHESTNUT_LEAVES),
				new FancyFoliagePlacer(ConstantInt.of(2), ConstantInt.of(4), 4),
				new TwoLayersFeatureSize(4, 0, 10, OptionalInt.empty())
			)
			.ignoreVines()
			.build();
	}

	private static TreeConfiguration matureChestnut() {
		return new TreeConfiguration.TreeConfigurationBuilder(
				BlockStateProvider.simple(ModBlocks.CHESTNUT_LOG),
				new FancyTrunkPlacer(18, 3, 1),
				BlockStateProvider.simple(ModBlocks.CHESTNUT_LEAVES),
				new FancyFoliagePlacer(ConstantInt.of(2), ConstantInt.of(4), 4),
				new TwoLayersFeatureSize(4, 0, 11, OptionalInt.empty())
			)
			.ignoreVines()
			.build();
	}

	private static TreeConfiguration forestChestnut() {
		return new TreeConfiguration.TreeConfigurationBuilder(
				BlockStateProvider.simple(ModBlocks.CHESTNUT_LOG),
				new ForestChestnutTrunkPlacer(21, 4, 3),
				BlockStateProvider.simple(ModBlocks.CHESTNUT_LEAVES),
				new FancyFoliagePlacer(ConstantInt.of(2), ConstantInt.of(2), 4),
				new ThreeLayersFeatureSize(14, 80, 1, 8, 8, OptionalInt.empty())
			)
			.ignoreVines()
			.build();
	}

	private static TreeConfiguration oldGrowthChestnut() {
		return new TreeConfiguration.TreeConfigurationBuilder(
				BlockStateProvider.simple(ModBlocks.CHESTNUT_LOG),
				new HollowChestnutTrunkPlacer(28, 4, 3),
				BlockStateProvider.simple(ModBlocks.CHESTNUT_LEAVES),
				new FancyFoliagePlacer(ConstantInt.of(3), ConstantInt.of(2), 4),
				new ThreeLayersFeatureSize(14, 80, 1, 9, 9, OptionalInt.empty())
			)
			.ignoreVines()
			.build();
	}

	private static TreeConfiguration naturalForestChestnut() {
		return new TreeConfiguration.TreeConfigurationBuilder(
				BlockStateProvider.simple(ModBlocks.CHESTNUT_LOG),
				new ForestChestnutTrunkPlacer(21, 4, 3, true),
				BlockStateProvider.simple(ModBlocks.CHESTNUT_LEAVES),
				new FancyFoliagePlacer(ConstantInt.of(2), ConstantInt.of(2), 4),
				// Natural Ridge placers preflight their complete log geometry. Keeping
				// the sapling form's radius-eight clearance here would reject the next
				// 12-block lattice neighbor after the first tree grew.
				new ThreeLayersFeatureSize(14, 80, 0, 0, 0, OptionalInt.empty())
			)
			.ignoreVines()
			.build();
	}

	private static TreeConfiguration naturalOldGrowthChestnut() {
		return new TreeConfiguration.TreeConfigurationBuilder(
				BlockStateProvider.simple(ModBlocks.CHESTNUT_LOG),
				new HollowChestnutTrunkPlacer(28, 4, 3, true),
				BlockStateProvider.simple(ModBlocks.CHESTNUT_LEAVES),
				new FancyFoliagePlacer(ConstantInt.of(3), ConstantInt.of(2), 4),
				new ThreeLayersFeatureSize(14, 80, 0, 0, 0, OptionalInt.empty())
			)
			.ignoreVines()
			.build();
	}

	private static TreeConfiguration tallHemlock() {
		return new TreeConfiguration.TreeConfigurationBuilder(
				BlockStateProvider.simple(ModBlocks.HEMLOCK_LOG),
				new TieredHemlockTrunkPlacer(18, 5, 4),
				BlockStateProvider.simple(ModBlocks.HEMLOCK_FOLIAGE),
				new TieredHemlockFoliagePlacer(ConstantInt.of(2), ConstantInt.of(2)),
				new TwoLayersFeatureSize(2, 0, 3)
			)
			.ignoreVines()
			.build();
	}

	private static TreeConfiguration spreadingHemlock() {
		return new TreeConfiguration.TreeConfigurationBuilder(
				BlockStateProvider.simple(ModBlocks.HEMLOCK_LOG),
				new FancyTrunkPlacer(14, 4, 2),
				BlockStateProvider.simple(ModBlocks.HEMLOCK_FOLIAGE),
				new FancyFoliagePlacer(ConstantInt.of(3), ConstantInt.of(3), 5),
				new TwoLayersFeatureSize(3, 0, 5, OptionalInt.empty())
			)
			.ignoreVines()
			.build();
	}

	private static TreeConfiguration oldGrowthHemlock() {
		return new TreeConfiguration.TreeConfigurationBuilder(
				BlockStateProvider.simple(ModBlocks.HEMLOCK_LOG),
				new OldGrowthHemlockTrunkPlacer(32, 10, 10),
				BlockStateProvider.simple(ModBlocks.HEMLOCK_FOLIAGE),
				new FancyFoliagePlacer(UniformInt.of(2, 3), ConstantInt.of(0), 3),
				new ThreeLayersFeatureSize(2, 80, 1, 0, 0, OptionalInt.empty())
			)
			.ignoreVines()
			.build();
	}

	private static TreeConfiguration beech() {
		return new TreeConfiguration.TreeConfigurationBuilder(
				BlockStateProvider.simple(ModBlocks.AMERICAN_BEECH_LOG),
				new FancyTrunkPlacer(16, 5, 3),
				BlockStateProvider.simple(ModBlocks.AMERICAN_BEECH_LEAVES),
				new FancyFoliagePlacer(ConstantInt.of(3), ConstantInt.of(3), 5),
				new TwoLayersFeatureSize(4, 0, 10, OptionalInt.empty())
			)
			.ignoreVines()
			.build();
	}

	private static TreeConfiguration blackWalnut() {
		return new TreeConfiguration.TreeConfigurationBuilder(
				BlockStateProvider.simple(ModBlocks.BLACK_WALNUT_LOG),
				// Black walnut keeps a clear forest-grown bole, but its crown must
				// break into lateral leaders instead of ending in one flat blob.
				new FancyTrunkPlacer(12, 5, 4),
				BlockStateProvider.simple(ModBlocks.BLACK_WALNUT_LEAVES),
				// Fancy foliage adds one block to its three interior row radii.
				// Radius two with four rows keeps every generated leaf within the
				// vanilla six-face-step support limit while retaining a deep crown.
				new FancyFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), 4),
				new TwoLayersFeatureSize(3, 0, 6, OptionalInt.empty())
			)
			.ignoreVines()
			.build();
	}

	private static ResourceKey<ConfiguredFeature<?, ?>> key(String path) {
		return ResourceKey.create(Registries.CONFIGURED_FEATURE, BeforeTheBlight.id(path));
	}
}
