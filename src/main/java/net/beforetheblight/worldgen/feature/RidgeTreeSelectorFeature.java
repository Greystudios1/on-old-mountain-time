package net.beforetheblight.worldgen.feature;

import com.mojang.serialization.Codec;
import net.beforetheblight.worldgen.biome.ModBiomes;
import net.beforetheblight.worldgen.feature.configurations.RidgeTreeConfiguration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Places only the large two-by-two or three-by-three Ridge chestnuts whose
 * lattice anchors are safely inside the surface-biome boundary.
 */
public final class RidgeTreeSelectorFeature extends Feature<RidgeTreeConfiguration> {
	private static final int LARGE_TREE_SHAPE_ATTEMPTS = 4;
	private static final int LARGE_FOOTPRINT_RADIUS = 1;
	private static final int[][] EDGE_SAMPLE_DIRECTIONS = {
		{-1, -1},
		{0, -1},
		{1, -1},
		{-1, 0},
		{1, 0},
		{-1, 1},
		{0, 1},
		{1, 1}
	};

	public RidgeTreeSelectorFeature(Codec<RidgeTreeConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<RidgeTreeConfiguration> context) {
		RidgeTreeConfiguration configuration = context.config();
		RandomSource random = context.random();
		RandomState randomState = context.level().getLevel().getChunkSource().randomState();
		PlacedFeature selected;

		if (!isInteriorRidge(
			context.level(),
			context.chunkGenerator(),
			randomState,
			context.origin(),
			configuration.edgeRadius()
		)) {
			return false;
		} else if (random.nextFloat() < configuration.oldGrowthChanceInInterior()) {
			selected = configuration.oldGrowthChestnut().value();
		} else {
			selected = configuration.forestChestnut().value();
		}

		BlockPos fittedOrigin = fitLargeTreeOrigin(
			context,
			randomState,
			LARGE_FOOTPRINT_RADIUS
		);
		for (int attempt = 0; attempt < LARGE_TREE_SHAPE_ATTEMPTS; attempt++) {
			if (selected.place(
				context.level(),
				context.chunkGenerator(),
				random,
				fittedOrigin
			)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Keeps a lattice site's X/Z fixed and raises its origin to the highest
	 * undecorated surface in a three-by-three large-tree footprint. Terrain-
	 * adaptive natural placers extend the remaining base columns downward.
	 */
	private static BlockPos fitLargeTreeOrigin(
		FeaturePlaceContext<RidgeTreeConfiguration> context,
		RandomState randomState,
		int radius
	) {
		BlockPos origin = context.origin();
		// Do not seed this from the incoming live-heightmap Y. An earlier
		// lattice tree's canopy can raise that heightmap during the same feature
		// pass, while generator base height remains order-independent.
		int maximumSurfaceY = Integer.MIN_VALUE;
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				maximumSurfaceY = Math.max(
					maximumSurfaceY,
					context.chunkGenerator().getBaseHeight(
						origin.getX() + dx,
						origin.getZ() + dz,
						Heightmap.Types.OCEAN_FLOOR_WG,
						context.level(),
						randomState
					)
				);
			}
		}
		return new BlockPos(origin.getX(), maximumSurfaceY, origin.getZ());
	}

	/**
	 * Samples every neighboring column at its undecorated generator surface.
	 * Sampling at the candidate Y would misclassify steep slopes, while reading
	 * the live heightmap would let an earlier tree's canopy affect later routing.
	 */
	public static boolean isInteriorRidge(
		WorldGenLevel level,
		ChunkGenerator chunkGenerator,
		RandomState randomState,
		BlockPos origin,
		int radius
	) {
		return isInteriorRidge(
			(x, z) -> isRidgeAtSurface(level, chunkGenerator, randomState, x, z),
			origin.getX(),
			origin.getZ(),
			radius
		);
	}

	public static boolean isInteriorRidge(
		SurfaceRidgeSampler sampler,
		int originX,
		int originZ,
		int radius
	) {
		if (!sampler.isRidge(originX, originZ)) {
			return false;
		}

		for (int[] direction : EDGE_SAMPLE_DIRECTIONS) {
			int x = originX + direction[0] * radius;
			int z = originZ + direction[1] * radius;
			if (!sampler.isRidge(x, z)) {
				return false;
			}
		}
		return true;
	}

	private static boolean isRidgeAtSurface(
		WorldGenLevel level,
		ChunkGenerator chunkGenerator,
		RandomState randomState,
		int x,
		int z
	) {
		int surfaceY = chunkGenerator.getBaseHeight(
			x,
			z,
			Heightmap.Types.WORLD_SURFACE_WG,
			level,
			randomState
		);
		return chunkGenerator.getBiomeSource().getNoiseBiome(
			QuartPos.fromBlock(x),
			QuartPos.fromBlock(surfaceY),
			QuartPos.fromBlock(z),
			randomState.sampler()
		).is(ModBiomes.CHESTNUT_OAK_RIDGE);
	}

	@FunctionalInterface
	public interface SurfaceRidgeSampler {
		boolean isRidge(int x, int z);
	}
}
