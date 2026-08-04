package net.beforetheblight.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.beforetheblight.client.season.SeasonalVisuals;

public final class BeforeTheBlightClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		RockingChairClient.initialize();
		FurnitureClient.initialize();
		if (FabricLoader.getInstance().isModLoaded("sereneseasons")) {
			SeasonalVisuals.initialize();
		}
	}
}
