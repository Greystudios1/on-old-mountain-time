package net.beforetheblight;

import net.beforetheblight.compat.seasons.SeasonalPlantClock;
import net.beforetheblight.interaction.TimberProcessingRegistry;
import net.beforetheblight.registry.ModBoardRoofBlocks;
import net.beforetheblight.registry.ModBlocks;
import net.beforetheblight.registry.ModCornCribBlocks;
import net.beforetheblight.registry.ModCriteriaTriggers;
import net.beforetheblight.registry.ModCreativeModeTabs;
import net.beforetheblight.registry.ModDomesticBlocks;
import net.beforetheblight.registry.ModDomesticItems;
import net.beforetheblight.registry.ModDoorWindowBlocks;
import net.beforetheblight.registry.ModExteriorItems;
import net.beforetheblight.registry.ModFurnitureBlocks;
import net.beforetheblight.registry.ModItems;
import net.beforetheblight.registry.ModFurniture;
import net.beforetheblight.registry.ModRegionalWoodBlocks;
import net.beforetheblight.registry.ModSounds;
import net.beforetheblight.registry.ModSpringhouseBlocks;
import net.beforetheblight.registry.ModStoneHearthBlocks;
import net.beforetheblight.registry.ModTimberBlocks;
import net.beforetheblight.worldgen.feature.ModFeatures;
import net.beforetheblight.worldgen.feature.trunkplacer.ModTrunkPlacerTypes;
import net.beforetheblight.worldgen.placement.ModPlacementModifierTypes;
import net.beforetheblight.worldgen.structure.ModStructureTypes;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BeforeTheBlight implements ModInitializer {
	public static final String MOD_ID = "before_the_blight";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModCriteriaTriggers.initialize();
		ModStructureTypes.initialize();
		ModPlacementModifierTypes.initialize();
		ModFeatures.initialize();
		ModTrunkPlacerTypes.initialize();
		ModBlocks.initialize();
		ModFurniture.initialize();
		ModRegionalWoodBlocks.initialize();
		ModBoardRoofBlocks.initialize();
		ModTimberBlocks.initialize();
		ModDoorWindowBlocks.initialize();
		ModFurnitureBlocks.initialize();
		ModDomesticBlocks.initialize();
		ModDomesticItems.initialize();
		ModCornCribBlocks.initialize();
		ModSpringhouseBlocks.initialize();
		ModStoneHearthBlocks.initialize();
		ModExteriorItems.initialize();
		TimberProcessingRegistry.bootstrap();
		ModSounds.initialize();
		ModItems.initialize();
		ModCreativeModeTabs.initialize();
		SeasonalPlantClock.initialize();
		LOGGER.info("Initializing Before the Blight.");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
