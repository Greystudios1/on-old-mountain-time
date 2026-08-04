package net.beforetheblight.worldgen.feature;

import net.beforetheblight.BeforeTheBlight;
import net.beforetheblight.worldgen.feature.configurations.HollowFallenLogConfiguration;
import net.beforetheblight.worldgen.feature.configurations.RidgeEdgeTreeConfiguration;
import net.beforetheblight.worldgen.feature.configurations.RidgeTreeConfiguration;
import net.beforetheblight.worldgen.feature.configurations.SparseUnderstoryConfiguration;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.Feature;

public final class ModFeatures {
	public static final Feature<RidgeTreeConfiguration> RIDGE_TREE_SELECTOR = Registry.register(
		BuiltInRegistries.FEATURE,
		BeforeTheBlight.id("ridge_tree_selector"),
		new RidgeTreeSelectorFeature(RidgeTreeConfiguration.CODEC)
	);
	public static final Feature<RidgeEdgeTreeConfiguration> RIDGE_EDGE_TREE_SELECTOR = Registry.register(
		BuiltInRegistries.FEATURE,
		BeforeTheBlight.id("ridge_edge_tree_selector"),
		new RidgeEdgeTreeFeature(RidgeEdgeTreeConfiguration.CODEC)
	);
	public static final Feature<HollowFallenLogConfiguration> HOLLOW_FALLEN_LOG = Registry.register(
		BuiltInRegistries.FEATURE,
		BeforeTheBlight.id("hollow_fallen_log"),
		new HollowFallenLogFeature(HollowFallenLogConfiguration.CODEC)
	);
	public static final Feature<SparseUnderstoryConfiguration> SPARSE_UNDERSTORY = Registry.register(
		BuiltInRegistries.FEATURE,
		BeforeTheBlight.id("sparse_understory"),
		new SparseUnderstoryFeature(SparseUnderstoryConfiguration.CODEC)
	);

	private ModFeatures() {
	}

	public static void initialize() {
		// Loading this class registers the codec before configured features decode.
	}
}
