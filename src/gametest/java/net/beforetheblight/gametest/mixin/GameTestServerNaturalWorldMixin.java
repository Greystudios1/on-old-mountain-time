package net.beforetheblight.gametest.mixin;

import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Test-source-only switch from vanilla's structureless flat GameTest world. */
@Mixin(GameTestServer.class)
abstract class GameTestServerNaturalWorldMixin {
	@Unique
	private static final String NATURAL_WORLD_PROPERTY =
		"before_the_blight.gametest.natural_structure_world";

	@Shadow
	@Final
	private static WorldOptions WORLD_OPTIONS;

	@Redirect(
		method = "lambda$create$1",
		at = @At(
			value = "FIELD",
			target = "Lnet/minecraft/world/level/levelgen/presets/WorldPresets;FLAT:Lnet/minecraft/resources/ResourceKey;"
		),
		require = 1
	)
	private static ResourceKey<WorldPreset> beforeTheBlight$selectWorldPreset() {
		return naturalWorldEnabled() ? WorldPresets.NORMAL : WorldPresets.FLAT;
	}

	@Redirect(
		method = "lambda$create$1",
		at = @At(
			value = "FIELD",
			target = "Lnet/minecraft/gametest/framework/GameTestServer;WORLD_OPTIONS:Lnet/minecraft/world/level/levelgen/WorldOptions;"
		),
		require = 1
	)
	private static WorldOptions beforeTheBlight$enableStructureGeneration() {
		return naturalWorldEnabled()
			? WORLD_OPTIONS.withStructures(true)
			: WORLD_OPTIONS;
	}

	@Unique
	private static boolean naturalWorldEnabled() {
		return Boolean.getBoolean(NATURAL_WORLD_PROPERTY);
	}
}
