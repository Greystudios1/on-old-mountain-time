package net.beforetheblight.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.beforetheblight.compat.seasons.SeasonalPlantClock;
import net.beforetheblight.worldgen.feature.ModConfiguredFeatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;

/**
 * Routes a complete three-by-three chestnut planting to old-growth while
 * leaving one-by-one and two-by-two growth to the normal {@link TreeGrower}.
 */
public final class ChestnutSaplingBlock extends SaplingBlock {
	public static final MapCodec<ChestnutSaplingBlock> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			TreeGrower.CODEC.fieldOf("tree").forGetter(block -> block.treeGrower),
			propertiesCodec()
		).apply(instance, ChestnutSaplingBlock::new)
	);

	private static final int OLD_GROWTH_RADIUS = 1;

	public ChestnutSaplingBlock(TreeGrower treeGrower, BlockBehaviour.Properties properties) {
		super(treeGrower, properties);
	}

	@Override
	public MapCodec<ChestnutSaplingBlock> codec() {
		return CODEC;
	}

	@Override
	public void advanceTree(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
		if (!SeasonalPlantClock.allowsGrowth(
			level,
			pos,
			SeasonalPlantClock.Plant.CHESTNUT,
			random
		)) {
			return;
		}
		advanceTreeUnconditionally(level, pos, state, random);
	}

	private void advanceTreeUnconditionally(
		ServerLevel level,
		BlockPos pos,
		BlockState state,
		RandomSource random
	) {
		if (state.getValue(STAGE) == 0) {
			super.advanceTree(level, pos, state, random);
			return;
		}

		List<BlockPos> completeCenters = findCompleteThreeByThreeCenters(level, pos);
		if (completeCenters.isEmpty()) {
			super.advanceTree(level, pos, state, random);
			return;
		}

		BlockPos center = completeCenters.get(random.nextInt(completeCenters.size()));
		placeOldGrowth(level, center, random);
	}

	@Override
	public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
		if (level instanceof Level actualLevel
			&& !SeasonalPlantClock.allowsBonemeal(
				actualLevel,
				pos,
				SeasonalPlantClock.Plant.CHESTNUT
			)) {
			return false;
		}

		List<BlockPos> completeCenters = findCompleteThreeByThreeCenters(level, pos);
		if (completeCenters.isEmpty()) {
			return super.isValidBonemealTarget(level, pos, state);
		}
		if (!(level instanceof ServerLevel serverLevel)) {
			return false;
		}

		Optional<ConfiguredFeature<?, ?>> feature = getOldGrowthFeature(serverLevel);
		if (feature.isEmpty() || !(feature.get().config() instanceof TreeConfiguration configuration)) {
			return false;
		}

		int minimumHeight = configuration.trunkPlacer.getBaseHeight();
		return completeCenters.stream()
			.anyMatch(center -> serverLevel.isInsideBuildHeight(center.above(minimumHeight)));
	}

	@Override
	public void performBonemeal(
		ServerLevel level,
		RandomSource random,
		BlockPos pos,
		BlockState state
	) {
		if (SeasonalPlantClock.allowsBonemeal(
			level,
			pos,
			SeasonalPlantClock.Plant.CHESTNUT
		)) {
			advanceTreeUnconditionally(level, pos, state, random);
		}
	}

	private void placeOldGrowth(ServerLevel level, BlockPos center, RandomSource random) {
		Optional<ConfiguredFeature<?, ?>> feature = getOldGrowthFeature(level);
		if (feature.isEmpty()) {
			return;
		}

		Optional<SaplingSquare> snapshot = snapshotSquare(level, center);
		if (snapshot.isEmpty()) {
			return;
		}

		SaplingSquare square = snapshot.get();
		boolean placed = false;
		try {
			boolean cleared = true;
			for (SaplingCell cell : square.cells()) {
				cleared &= level.setBlock(cell.pos(), cell.clearedState(), Block.UPDATE_NONE);
			}

			if (cleared) {
				placed = feature.get().place(
					level,
					level.getChunkSource().getGenerator(),
					random,
					square.center()
				);
			}
		} finally {
			if (!placed) {
				restoreSquare(level, square);
			}
		}

		if (placed) {
			syncSquare(level, square);
		}
	}

	private List<BlockPos> findCompleteThreeByThreeCenters(LevelReader level, BlockPos triggeringPos) {
		List<BlockPos> centers = new ArrayList<>(9);
		for (int centerDx = -OLD_GROWTH_RADIUS; centerDx <= OLD_GROWTH_RADIUS; centerDx++) {
			for (int centerDz = -OLD_GROWTH_RADIUS; centerDz <= OLD_GROWTH_RADIUS; centerDz++) {
				BlockPos center = triggeringPos.offset(centerDx, 0, centerDz);
				if (isCompleteThreeByThree(level, center)) {
					centers.add(center);
				}
			}
		}
		return centers;
	}

	private boolean isCompleteThreeByThree(LevelReader level, BlockPos center) {
		for (int dx = -OLD_GROWTH_RADIUS; dx <= OLD_GROWTH_RADIUS; dx++) {
			for (int dz = -OLD_GROWTH_RADIUS; dz <= OLD_GROWTH_RADIUS; dz++) {
				if (!level.getBlockState(center.offset(dx, 0, dz)).is(this)) {
					return false;
				}
			}
		}
		return true;
	}

	private Optional<SaplingSquare> snapshotSquare(ServerLevel level, BlockPos center) {
		List<SaplingCell> cells = new ArrayList<>(9);
		for (int dx = -OLD_GROWTH_RADIUS; dx <= OLD_GROWTH_RADIUS; dx++) {
			for (int dz = -OLD_GROWTH_RADIUS; dz <= OLD_GROWTH_RADIUS; dz++) {
				BlockPos cellPos = center.offset(dx, 0, dz).immutable();
				BlockState originalState = level.getBlockState(cellPos);
				if (!originalState.is(this)) {
					return Optional.empty();
				}

				cells.add(new SaplingCell(
					cellPos,
					originalState,
					level.getFluidState(cellPos).createLegacyBlock()
				));
			}
		}
		return Optional.of(new SaplingSquare(center.immutable(), List.copyOf(cells)));
	}

	private static Optional<ConfiguredFeature<?, ?>> getOldGrowthFeature(ServerLevel level) {
		return level.registryAccess()
			.lookupOrThrow(Registries.CONFIGURED_FEATURE)
			.get(ModConfiguredFeatures.CHESTNUT_OLD_GROWTH)
			.map(holder -> holder.value());
	}

	private static void restoreSquare(ServerLevel level, SaplingSquare square) {
		for (SaplingCell cell : square.cells()) {
			BlockState failedState = level.getBlockState(cell.pos());
			level.setBlock(cell.pos(), cell.originalState(), Block.UPDATE_NONE);
			level.sendBlockUpdated(
				cell.pos(),
				failedState,
				cell.originalState(),
				Block.UPDATE_CLIENTS
			);
		}
	}

	private static void syncSquare(ServerLevel level, SaplingSquare square) {
		for (SaplingCell cell : square.cells()) {
			level.sendBlockUpdated(
				cell.pos(),
				cell.originalState(),
				level.getBlockState(cell.pos()),
				Block.UPDATE_CLIENTS
			);
		}
	}

	private record SaplingCell(BlockPos pos, BlockState originalState, BlockState clearedState) {
	}

	private record SaplingSquare(BlockPos center, List<SaplingCell> cells) {
	}
}
