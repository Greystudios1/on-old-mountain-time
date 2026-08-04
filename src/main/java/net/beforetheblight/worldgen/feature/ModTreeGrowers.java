package net.beforetheblight.worldgen.feature;

import java.util.Optional;

import net.minecraft.world.level.block.grower.TreeGrower;

public final class ModTreeGrowers {
	public static final TreeGrower CHESTNUT = new TreeGrower(
		"before_the_blight:chestnut",
		0.25F,
		Optional.of(ModConfiguredFeatures.CHESTNUT_FOREST),
		Optional.empty(),
		Optional.of(ModConfiguredFeatures.CHESTNUT_ORDINARY),
		Optional.of(ModConfiguredFeatures.CHESTNUT_MATURE),
		Optional.empty(),
		Optional.empty()
	);

	public static final TreeGrower HEMLOCK = new TreeGrower(
		"before_the_blight:hemlock",
		0.25F,
		Optional.of(ModConfiguredFeatures.HEMLOCK_OLD_GROWTH),
		Optional.empty(),
		Optional.of(ModConfiguredFeatures.HEMLOCK_TALL),
		Optional.of(ModConfiguredFeatures.HEMLOCK_SPREADING),
		Optional.empty(),
		Optional.empty()
	);

	public static final TreeGrower AMERICAN_BEECH = new TreeGrower(
		"before_the_blight:american_beech",
		0.1F,
		Optional.empty(),
		Optional.empty(),
		Optional.of(ModConfiguredFeatures.BEECH),
		Optional.empty(),
		Optional.empty(),
		Optional.empty()
	);

	public static final TreeGrower BLACK_WALNUT = new TreeGrower(
		"before_the_blight:black_walnut",
		0.1F,
		Optional.empty(),
		Optional.empty(),
		Optional.of(ModConfiguredFeatures.BLACK_WALNUT),
		Optional.empty(),
		Optional.empty(),
		Optional.empty()
	);

	private ModTreeGrowers() {
	}
}
