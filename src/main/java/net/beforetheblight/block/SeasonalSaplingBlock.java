package net.beforetheblight.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.beforetheblight.compat.seasons.SeasonalPlantClock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A normal sapling whose tree transition observes the optional plant clock.
 */
public final class SeasonalSaplingBlock extends SaplingBlock {
	public static final MapCodec<SeasonalSaplingBlock> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			TreeGrower.CODEC.fieldOf("tree").forGetter(block -> block.treeGrower),
			SeasonalPlantClock.Plant.CODEC.fieldOf("seasonal_plant").forGetter(block -> block.plant),
			propertiesCodec()
		).apply(instance, SeasonalSaplingBlock::new)
	);

	private final SeasonalPlantClock.Plant plant;

	public SeasonalSaplingBlock(
		TreeGrower treeGrower,
		SeasonalPlantClock.Plant plant,
		BlockBehaviour.Properties properties
	) {
		super(treeGrower, properties);
		this.plant = plant;
	}

	@Override
	public MapCodec<SeasonalSaplingBlock> codec() {
		return CODEC;
	}

	@Override
	public void advanceTree(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
		if (SeasonalPlantClock.allowsGrowth(level, pos, plant, random)) {
			super.advanceTree(level, pos, state, random);
		}
	}

	@Override
	public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
		if (!super.isValidBonemealTarget(level, pos, state)) {
			return false;
		}
		return !(level instanceof Level actualLevel)
			|| SeasonalPlantClock.allowsBonemeal(actualLevel, pos, this.plant);
	}

	@Override
	public void performBonemeal(
		ServerLevel level,
		RandomSource random,
		BlockPos pos,
		BlockState state
	) {
		if (SeasonalPlantClock.allowsBonemeal(level, pos, this.plant)) {
			super.advanceTree(level, pos, state, random);
		}
	}
}
