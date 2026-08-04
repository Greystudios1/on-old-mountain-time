package net.beforetheblight.worldgen.feature;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import net.beforetheblight.worldgen.feature.configurations.SparseUnderstoryConfiguration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.TreeFeature;

/**
 * Places a small, separated understory cluster while preserving walkable gaps.
 *
 * <p>One invocation can place at most six plants, and every accepted plant
 * respects a configured horizontal separation. The placed feature should still
 * use a low count or rarity; this class is a second, data-independent guard
 * against biome-wide thickets.</p>
 */
public final class SparseUnderstoryFeature extends Feature<SparseUnderstoryConfiguration> {
	private static final int BLOCK_UPDATE_FLAGS = 19;

	public SparseUnderstoryFeature(Codec<SparseUnderstoryConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<SparseUnderstoryConfiguration> context) {
		SparseUnderstoryConfiguration configuration = context.config();
		WorldGenLevel level = context.level();
		RandomSource random = context.random();
		Holder<Biome> originBiome = level.getBiome(context.origin());
		List<BlockPos> placed = new ArrayList<>(configuration.maximumPlacements());

		for (
			int attempt = 0;
			attempt < configuration.attempts()
				&& placed.size() < configuration.maximumPlacements();
			attempt++
		) {
			int dx = random.nextIntBetweenInclusive(-configuration.radius(), configuration.radius());
			int dz = random.nextIntBetweenInclusive(-configuration.radius(), configuration.radius());
			if (dx * dx + dz * dz > configuration.radius() * configuration.radius()) {
				continue;
			}
			int x = context.origin().getX() + dx;
			int z = context.origin().getZ() + dz;
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			BlockPos candidate = new BlockPos(x, y, z);
			if (!level.getBiome(candidate).equals(originBiome)) {
				continue;
			}
			if (!isSeparated(candidate, placed, configuration.minimumSeparation())) {
				continue;
			}
			if (!canReplaceWithoutFlooding(level, candidate)) {
				continue;
			}

			BlockState state = configuration.foliageProvider().getState(level, random, candidate);
			if (!state.canSurvive(level, candidate)) {
				continue;
			}
			if (state.getBlock() instanceof DoublePlantBlock) {
				BlockPos upper = candidate.above();
				if (!canReplaceWithoutFlooding(level, upper)) {
					continue;
				}
				DoublePlantBlock.placeAt(level, state, candidate, BLOCK_UPDATE_FLAGS);
			} else {
				level.setBlock(candidate, state, BLOCK_UPDATE_FLAGS);
			}
			placed.add(candidate);
		}
		return !placed.isEmpty();
	}

	/**
	 * Horizontal spacing helper exposed for a deterministic GameTest contract.
	 */
	public static boolean isSeparated(
		BlockPos candidate,
		List<BlockPos> placed,
		int minimumSeparation
	) {
		int minimumDistanceSquared = minimumSeparation * minimumSeparation;
		for (BlockPos occupied : placed) {
			int dx = candidate.getX() - occupied.getX();
			int dz = candidate.getZ() - occupied.getZ();
			if (dx * dx + dz * dz < minimumDistanceSquared) {
				return false;
			}
		}
		return true;
	}

	private static boolean canReplaceWithoutFlooding(WorldGenLevel level, BlockPos pos) {
		return level.isInsideBuildHeight(pos)
			&& level.getFluidState(pos).isEmpty()
			&& TreeFeature.validTreePos(level, pos);
	}
}
