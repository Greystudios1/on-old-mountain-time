package net.beforetheblight.worldgen.feature;

import com.mojang.serialization.Codec;
import net.beforetheblight.worldgen.feature.configurations.RidgeEdgeTreeConfiguration;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/** Places independently sampled one-by-one chestnuts only along Ridge edges. */
public final class RidgeEdgeTreeFeature extends Feature<RidgeEdgeTreeConfiguration> {
	public RidgeEdgeTreeFeature(Codec<RidgeEdgeTreeConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<RidgeEdgeTreeConfiguration> context) {
		RidgeEdgeTreeConfiguration configuration = context.config();
		RandomState randomState = context.level().getLevel().getChunkSource().randomState();
		if (RidgeTreeSelectorFeature.isInteriorRidge(
			context.level(),
			context.chunkGenerator(),
			randomState,
			context.origin(),
			configuration.edgeRadius()
		)) {
			return false;
		}

		BlockPos origin = context.origin();
		int surfaceY = context.chunkGenerator().getBaseHeight(
			origin.getX(),
			origin.getZ(),
			Heightmap.Types.OCEAN_FLOOR_WG,
			context.level(),
			randomState
		);
		RandomSource random = context.random();
		return configuration.edgeChestnut().value().place(
			context.level(),
			context.chunkGenerator(),
			random,
			new BlockPos(origin.getX(), surfaceY, origin.getZ())
		);
	}
}
