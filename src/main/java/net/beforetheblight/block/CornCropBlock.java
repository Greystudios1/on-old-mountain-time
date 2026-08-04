package net.beforetheblight.block;

import com.mojang.serialization.MapCodec;
import net.beforetheblight.compat.seasons.SeasonalPlantClock;
import net.beforetheblight.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A single-block field-corn crop with the full vanilla eight-state (age 0-7) growth cycle.
 * The registered kernels item is both the seed and the pick-block result.
 */
public final class CornCropBlock extends CropBlock {
	public static final MapCodec<CornCropBlock> CODEC = simpleCodec(CornCropBlock::new);

	public CornCropBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	public MapCodec<CornCropBlock> codec() {
		return CODEC;
	}

	@Override
	protected ItemLike getBaseSeedId() {
		return ModItems.CORN_KERNELS;
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (SeasonalPlantClock.allowsGrowth(
			level,
			pos,
			SeasonalPlantClock.Plant.CORN,
			random
		)) {
			super.randomTick(state, level, pos, random);
		}
	}

	@Override
	public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
		if (!super.isValidBonemealTarget(level, pos, state)) {
			return false;
		}
		return !(level instanceof Level actualLevel)
			|| SeasonalPlantClock.allowsBonemeal(
				actualLevel,
				pos,
				SeasonalPlantClock.Plant.CORN
			);
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
			SeasonalPlantClock.Plant.CORN
		)) {
			super.performBonemeal(level, random, pos, state);
		}
	}
}
