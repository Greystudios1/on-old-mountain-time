package net.beforetheblight.worldgen.biome;

import net.beforetheblight.BeforeTheBlight;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

/** Biome tags owned by the Hemlock Cove worldgen slice. */
public final class CoveBiomeTags {
	public static final TagKey<Biome> HEMLOCK_BEECH_COVE_TARGETS = TagKey.create(
		Registries.BIOME,
		BeforeTheBlight.id("hemlock_beech_cove_targets")
	);

	private CoveBiomeTags() {
	}
}
