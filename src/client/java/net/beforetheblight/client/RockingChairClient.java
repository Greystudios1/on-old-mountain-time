package net.beforetheblight.client;

import net.beforetheblight.client.render.RockingChairSeatRenderer;
import net.beforetheblight.registry.ModFurniture;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/** Client-only registration for the animated chair helper. */
public final class RockingChairClient {
	private RockingChairClient() {
	}

	public static void initialize() {
		EntityRendererRegistry.register(
			ModFurniture.ROCKING_CHAIR_SEAT,
			RockingChairSeatRenderer::new
		);
	}
}
