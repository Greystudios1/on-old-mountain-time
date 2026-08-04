package net.beforetheblight.block;

import java.util.List;

import com.mojang.serialization.MapCodec;
import net.beforetheblight.compat.seasons.SeasonalPlantClock;
import net.beforetheblight.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.TintedParticleLeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * Keeps the normal leaf loot table while constraining only mast drops to the
 * effective early/mid-autumn window when the optional temperate clock is active.
 */
public final class SeasonalChestnutLeavesBlock extends TintedParticleLeavesBlock {
	public static final MapCodec<SeasonalChestnutLeavesBlock> CODEC =
		simpleCodec(SeasonalChestnutLeavesBlock::new);

	public SeasonalChestnutLeavesBlock(BlockBehaviour.Properties properties) {
		super(0.01F, properties);
	}

	@Override
	public MapCodec<SeasonalChestnutLeavesBlock> codec() {
		return CODEC;
	}

	@Override
	protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
		List<ItemStack> drops = super.getDrops(state, params);
		Vec3 origin = params.getOptionalParameter(LootContextParams.ORIGIN);
		if (origin == null) {
			return drops;
		}

		BlockPos pos = BlockPos.containing(origin);
		if (SeasonalPlantClock.chestnutMastAvailable(params.getLevel(), pos)) {
			return drops;
		}

		return drops.stream()
			.filter(stack -> !stack.is(ModItems.HANDFUL_OF_CHESTNUTS))
			.toList();
	}
}
