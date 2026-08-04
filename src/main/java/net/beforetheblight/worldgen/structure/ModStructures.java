package net.beforetheblight.worldgen.structure;

import net.beforetheblight.BeforeTheBlight;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;

public final class ModStructures {
	public static final ResourceKey<Structure> APPALACHIAN_HOMESTEAD = ResourceKey.create(
		Registries.STRUCTURE,
		BeforeTheBlight.id("appalachian_homestead")
	);
	public static final ResourceKey<StructureSet> APPALACHIAN_HOMESTEADS = ResourceKey.create(
		Registries.STRUCTURE_SET,
		BeforeTheBlight.id("appalachian_homesteads")
	);
	public static final ResourceKey<Structure> APPALACHIAN_CORN_CRIB = ResourceKey.create(
		Registries.STRUCTURE,
		BeforeTheBlight.id("appalachian_corn_crib")
	);
	public static final ResourceKey<StructureSet> APPALACHIAN_CORN_CRIBS = ResourceKey.create(
		Registries.STRUCTURE_SET,
		BeforeTheBlight.id("appalachian_corn_cribs")
	);

	private ModStructures() {
	}
}
