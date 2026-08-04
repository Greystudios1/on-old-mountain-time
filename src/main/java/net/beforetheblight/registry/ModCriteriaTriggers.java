package net.beforetheblight.registry;

import net.beforetheblight.BeforeTheBlight;
import net.beforetheblight.advancement.LoadChestnutTrestlesTrigger;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/** Registers Before the Blight's server-authoritative advancement signals. */
public final class ModCriteriaTriggers {
	public static final LoadChestnutTrestlesTrigger LOAD_CHESTNUT_TRESTLES = Registry.register(
		BuiltInRegistries.TRIGGER_TYPES,
		BeforeTheBlight.id("load_chestnut_trestles"),
		new LoadChestnutTrestlesTrigger()
	);

	private ModCriteriaTriggers() {
	}

	public static void initialize() {
		// Calling this method forces registration before advancements are decoded.
	}
}
