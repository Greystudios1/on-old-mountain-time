package net.beforetheblight.client;

import net.beforetheblight.registry.ModFurnitureBlocks;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.NoopRenderer;

/** Client-only registration hook for static, sit-able furniture. */
public final class FurnitureClient {
	private FurnitureClient() {
	}

	public static void initialize() {
		EntityRendererRegistry.register(
			ModFurnitureBlocks.STATIC_SEAT,
			NoopRenderer::new
		);
	}
}
