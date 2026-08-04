package net.beforetheblight.worldgen.biome;

import net.beforetheblight.BeforeTheBlight;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

/** Biome tags owned by the minimal Grassy Bald worldgen slice. */
public final class GrassyBaldBiomeTags {
	public static final TagKey<Biome> GRASSY_BALD_TARGETS = TagKey.create(
		Registries.BIOME,
		BeforeTheBlight.id("grassy_bald_targets")
	);

	private GrassyBaldBiomeTags() {
	}
}
