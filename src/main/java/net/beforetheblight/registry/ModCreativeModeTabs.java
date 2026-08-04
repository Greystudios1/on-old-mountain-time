package net.beforetheblight.registry;

import net.beforetheblight.BeforeTheBlight;
import net.beforetheblight.registry.ModContentCatalog.Category;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public final class ModCreativeModeTabs {
	public static final ResourceKey<CreativeModeTab> BUILDING_MATERIALS_KEY = key("building_materials");
	public static final ResourceKey<CreativeModeTab> FURNITURE_DECOR_KEY = key("furniture_decor");
	public static final ResourceKey<CreativeModeTab> NATURE_FARMING_KEY = key("nature_farming");
	public static final ResourceKey<CreativeModeTab> TOOLS_WORKSTATIONS_KEY = key("tools_workstations");

	/**
	 * Legacy tab key retained as the complete catalog for compatibility.
	 */
	public static final ResourceKey<CreativeModeTab> BEFORE_THE_BLIGHT_KEY = key("before_the_blight");

	public static final CreativeModeTab BUILDING_MATERIALS = register(
		BUILDING_MATERIALS_KEY,
		"itemGroup.before_the_blight.building_materials",
		ModBlocks.HEWN_CHESTNUT_BEAM,
		Category.BUILDING_MATERIALS
	);

	public static final CreativeModeTab FURNITURE_DECOR = register(
		FURNITURE_DECOR_KEY,
		"itemGroup.before_the_blight.furniture_decor",
		ModBlocks.ROCKING_CHAIR,
		Category.FURNITURE_DECOR
	);

	public static final CreativeModeTab NATURE_FARMING = register(
		NATURE_FARMING_KEY,
		"itemGroup.before_the_blight.nature_farming",
		ModBlocks.CHESTNUT_SAPLING,
		Category.NATURE_FARMING
	);

	public static final CreativeModeTab TOOLS_WORKSTATIONS = register(
		TOOLS_WORKSTATIONS_KEY,
		"itemGroup.before_the_blight.tools_workstations",
		ModItems.BROAD_AXE,
		Category.TOOLS_WORKSTATIONS
	);

	public static final CreativeModeTab BEFORE_THE_BLIGHT = Registry.register(
		BuiltInRegistries.CREATIVE_MODE_TAB,
		BEFORE_THE_BLIGHT_KEY,
		FabricCreativeModeTab.builder()
			.title(Component.translatable("itemGroup.before_the_blight.before_the_blight"))
			.icon(() -> new ItemStack(ModBlocks.CHESTNUT_SAPLING))
			.displayItems((parameters, output) ->
				ModContentCatalog.allItems().forEach(output::accept)
			)
			.build()
	);

	private ModCreativeModeTabs() {
	}

	private static ResourceKey<CreativeModeTab> key(String path) {
		return ResourceKey.create(Registries.CREATIVE_MODE_TAB, BeforeTheBlight.id(path));
	}

	private static CreativeModeTab register(
		ResourceKey<CreativeModeTab> key,
		String titleKey,
		ItemLike icon,
		Category category
	) {
		return Registry.register(
			BuiltInRegistries.CREATIVE_MODE_TAB,
			key,
			FabricCreativeModeTab.builder()
				.title(Component.translatable(titleKey))
				.icon(() -> new ItemStack(icon))
				.displayItems((parameters, output) ->
					ModContentCatalog.items(category).forEach(output::accept)
				)
				.build()
		);
	}

	public static void initialize() {
		ModContentCatalog.seal();
	}
}
