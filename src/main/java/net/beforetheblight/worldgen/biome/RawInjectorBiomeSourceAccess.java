package net.beforetheblight.worldgen.biome;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;

/** Test and diagnostic access to Lithostitched output before the footprint filter. */
public interface RawInjectorBiomeSourceAccess {
	Holder<Biome> beforeTheBlight$getRawNoiseBiome(
		int quartX,
		int quartY,
		int quartZ,
		Climate.Sampler sampler
	);
}
