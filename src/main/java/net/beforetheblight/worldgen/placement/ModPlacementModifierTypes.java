package net.beforetheblight.worldgen.placement;

import net.beforetheblight.BeforeTheBlight;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

public final class ModPlacementModifierTypes {
	public static final PlacementModifierType<StaggeredChestnutPlacement> STAGGERED_CHESTNUT =
		Registry.register(
			BuiltInRegistries.PLACEMENT_MODIFIER_TYPE,
			BeforeTheBlight.id("staggered_chestnut"),
			() -> StaggeredChestnutPlacement.CODEC
		);

	private ModPlacementModifierTypes() {
	}

	public static void initialize() {
		// Loading this class registers the codec before placed features decode.
	}
}
