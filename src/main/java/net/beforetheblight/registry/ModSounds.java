package net.beforetheblight.registry;

import net.beforetheblight.BeforeTheBlight;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/** Named interaction sounds provide stable subtitle hooks while reusing vanilla audio. */
public final class ModSounds {
	public static final SoundEvent SAW_STROKE = register("saw_stroke");
	public static final SoundEvent SAW_COMPLETE = register("saw_complete");
	public static final SoundEvent FROE_SET = register("froe_set");
	public static final SoundEvent MAUL_STRIKE = register("maul_strike");
	public static final SoundEvent SPLIT_COMPLETE = register("split_complete");

	private ModSounds() {
	}

	private static SoundEvent register(String name) {
		Identifier id = BeforeTheBlight.id(name);
		return Registry.register(
			BuiltInRegistries.SOUND_EVENT,
			id,
			SoundEvent.createVariableRangeEvent(id)
		);
	}

	public static void initialize() {
		// Class loading performs the registrations.
	}
}
