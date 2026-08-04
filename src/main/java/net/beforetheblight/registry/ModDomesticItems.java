package net.beforetheblight.registry;

import java.util.ArrayList;
import java.util.List;

import net.beforetheblight.BeforeTheBlight;
import net.beforetheblight.registry.ModContentCatalog.Category;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Obtainable item surface for the isolated domestic-prop registry.
 */
public final class ModDomesticItems {
	// Hearth cookware.
	public static final Item CAST_IRON_POT = registerBlockItem(
		"cast_iron_pot", ModDomesticBlocks.CAST_IRON_POT
	);
	public static final Item IRON_KETTLE = registerBlockItem(
		"iron_kettle", ModDomesticBlocks.IRON_KETTLE
	);
	public static final Item SPIDER_SKILLET = registerBlockItem(
		"spider_skillet", ModDomesticBlocks.SPIDER_SKILLET
	);
	public static final Item DUTCH_OVEN = registerBlockItem(
		"dutch_oven", ModDomesticBlocks.DUTCH_OVEN
	);

	// Dishes and containers.
	public static final Item WOODEN_BOWL = registerBlockItem(
		"wooden_bowl", ModDomesticBlocks.WOODEN_BOWL
	);
	public static final Item WOODEN_TRENCHER = registerBlockItem(
		"wooden_trencher", ModDomesticBlocks.WOODEN_TRENCHER
	);
	public static final Item WOODEN_CUP = registerBlockItem(
		"wooden_cup", ModDomesticBlocks.WOODEN_CUP
	);
	public static final Item WOODEN_SPOON = registerBlockItem(
		"wooden_spoon", ModDomesticBlocks.WOODEN_SPOON
	);
	public static final Item WOODEN_LADLE = registerBlockItem(
		"wooden_ladle", ModDomesticBlocks.WOODEN_LADLE
	);
	public static final Item WOVEN_BASKET = registerBlockItem(
		"woven_basket", ModDomesticBlocks.WOVEN_BASKET
	);
	public static final Item WOODEN_BUCKET = registerBlockItem(
		"wooden_bucket", ModDomesticBlocks.WOODEN_BUCKET
	);
	public static final Item WOODEN_PAIL = registerBlockItem(
		"wooden_pail", ModDomesticBlocks.WOODEN_PAIL
	);
	public static final Item STONEWARE_CROCK = registerBlockItem(
		"stoneware_crock", ModDomesticBlocks.STONEWARE_CROCK
	);
	public static final Item STONEWARE_JUG = registerBlockItem(
		"stoneware_jug", ModDomesticBlocks.STONEWARE_JUG
	);
	public static final Item CERAMIC_PITCHER = registerBlockItem(
		"ceramic_pitcher", ModDomesticBlocks.CERAMIC_PITCHER
	);
	public static final Item TIN_CUP = registerBlockItem(
		"tin_cup", ModDomesticBlocks.TIN_CUP
	);
	public static final Item TIN_PLATE = registerBlockItem(
		"tin_plate", ModDomesticBlocks.TIN_PLATE
	);
	public static final Item PRESERVING_JAR = registerBlockItem(
		"preserving_jar", ModDomesticBlocks.PRESERVING_JAR
	);
	public static final Item GREEN_GLASS_BOTTLE = registerBlockItem(
		"green_glass_bottle", ModDomesticBlocks.GREEN_GLASS_BOTTLE
	);

	// Food props are placeable pantry and table dressing, not edible items.
	public static final Item CORNBREAD_ROUND = registerBlockItem(
		"cornbread_round", ModDomesticBlocks.CORNBREAD_ROUND
	);
	public static final Item HOE_CAKE = registerBlockItem(
		"hoe_cake", ModDomesticBlocks.HOE_CAKE
	);
	public static final Item HOMINY_BOWL = registerBlockItem(
		"hominy_bowl", ModDomesticBlocks.HOMINY_BOWL
	);
	public static final Item DRIED_BEAN_BOWL = registerBlockItem(
		"dried_bean_bowl", ModDomesticBlocks.DRIED_BEAN_BOWL
	);
	public static final Item DRIED_APPLE_RINGS = registerBlockItem(
		"dried_apple_rings", ModDomesticBlocks.DRIED_APPLE_RINGS
	);
	public static final Item HANGING_DRIED_APPLES = registerBlockItem(
		"hanging_dried_apples", ModDomesticBlocks.HANGING_DRIED_APPLES
	);
	public static final Item DRIED_HERB_BUNCH = registerBlockItem(
		"dried_herb_bunch", ModDomesticBlocks.DRIED_HERB_BUNCH
	);
	public static final Item HANGING_ONIONS = registerBlockItem(
		"hanging_onions", ModDomesticBlocks.HANGING_ONIONS
	);
	public static final Item HANGING_GARLIC = registerBlockItem(
		"hanging_garlic", ModDomesticBlocks.HANGING_GARLIC
	);

	// Lighting, textiles, washing, and personal objects.
	public static final Item TALLOW_CANDLE = registerBlockItem(
		"tallow_candle", ModDomesticBlocks.TALLOW_CANDLE
	);
	public static final Item BETTY_LAMP = registerBlockItem(
		"betty_lamp", ModDomesticBlocks.BETTY_LAMP
	);
	public static final Item FOLDED_PATCHWORK_QUILT = registerBlockItem(
		"folded_patchwork_quilt", ModDomesticBlocks.FOLDED_PATCHWORK_QUILT
	);
	public static final Item FOLDED_WOOL_BLANKET = registerBlockItem(
		"folded_wool_blanket", ModDomesticBlocks.FOLDED_WOOL_BLANKET
	);
	public static final Item RAG_RUG = registerBlockItem(
		"rag_rug", ModDomesticBlocks.RAG_RUG
	);
	public static final Item WOVEN_WALL_TEXTILE = registerBlockItem(
		"woven_wall_textile", ModDomesticBlocks.WOVEN_WALL_TEXTILE
	);
	public static final Item WOODEN_WASH_TUB = registerBlockItem(
		"wooden_wash_tub", ModDomesticBlocks.WOODEN_WASH_TUB
	);
	public static final Item WASHBOARD = registerBlockItem(
		"washboard", ModDomesticBlocks.WASHBOARD
	);
	public static final Item SOAP_BLOCK = registerBlockItem(
		"soap_block", ModDomesticBlocks.SOAP_BLOCK
	);
	public static final Item PLAIN_BIBLE = registerBlockItem(
		"plain_bible", ModDomesticBlocks.PLAIN_BIBLE
	);
	public static final Item ALMANAC = registerBlockItem(
		"almanac", ModDomesticBlocks.ALMANAC
	);
	public static final Item LETTER_BUNDLE = registerBlockItem(
		"letter_bundle", ModDomesticBlocks.LETTER_BUNDLE
	);
	public static final Item INK_BOTTLE = registerBlockItem(
		"ink_bottle", ModDomesticBlocks.INK_BOTTLE
	);
	public static final Item WORK_BOOTS = registerBlockItem(
		"work_boots", ModDomesticBlocks.WORK_BOOTS
	);
	public static final Item FIDDLE = registerBlockItem(
		"fiddle", ModDomesticBlocks.FIDDLE
	);

	public static final List<Item> HEARTH_COOKWARE = List.of(
		CAST_IRON_POT, IRON_KETTLE, SPIDER_SKILLET, DUTCH_OVEN
	);
	public static final List<Item> FOOD_AND_CONTAINERS = List.of(
		WOODEN_BOWL,
		WOODEN_TRENCHER,
		WOODEN_CUP,
		WOODEN_SPOON,
		WOODEN_LADLE,
		WOVEN_BASKET,
		WOODEN_BUCKET,
		WOODEN_PAIL,
		STONEWARE_CROCK,
		STONEWARE_JUG,
		CERAMIC_PITCHER,
		TIN_CUP,
		TIN_PLATE,
		PRESERVING_JAR,
		GREEN_GLASS_BOTTLE,
		CORNBREAD_ROUND,
		HOE_CAKE,
		HOMINY_BOWL,
		DRIED_BEAN_BOWL,
		DRIED_APPLE_RINGS,
		HANGING_DRIED_APPLES,
		DRIED_HERB_BUNCH,
		HANGING_ONIONS,
		HANGING_GARLIC
	);
	public static final List<Item> FOOD_PROPS = List.of(
		CORNBREAD_ROUND,
		HOE_CAKE,
		HOMINY_BOWL,
		DRIED_BEAN_BOWL,
		DRIED_APPLE_RINGS,
		HANGING_DRIED_APPLES,
		DRIED_HERB_BUNCH,
		HANGING_ONIONS,
		HANGING_GARLIC
	);
	public static final List<Item> FURNISHINGS_AND_PROPS = List.of(
		TALLOW_CANDLE,
		BETTY_LAMP,
		FOLDED_PATCHWORK_QUILT,
		FOLDED_WOOL_BLANKET,
		RAG_RUG,
		WOVEN_WALL_TEXTILE,
		WOODEN_WASH_TUB,
		WASHBOARD,
		SOAP_BLOCK,
		PLAIN_BIBLE,
		ALMANAC,
		LETTER_BUNDLE,
		INK_BOTTLE,
		WORK_BOOTS,
		FIDDLE
	);
	public static final List<Item> ALL = concatenate(
		HEARTH_COOKWARE,
		FOOD_AND_CONTAINERS,
		FURNISHINGS_AND_PROPS
	);

	private static boolean catalogRegistered;

	private ModDomesticItems() {
	}

	private static Item registerBlockItem(String name, Block block) {
		ResourceKey<Item> key = ResourceKey.create(
			Registries.ITEM,
			BeforeTheBlight.id(name)
		);
		BlockItem item = new BlockItem(
			block,
			new Item.Properties()
				.setId(key)
				.useBlockDescriptionPrefix()
		);
		item.registerBlocks(Item.BY_BLOCK, item);
		return Registry.register(BuiltInRegistries.ITEM, key, item);
	}

	@SafeVarargs
	private static List<Item> concatenate(List<Item>... groups) {
		List<Item> combined = new ArrayList<>();
		for (List<Item> group : groups) {
			combined.addAll(group);
		}
		return List.copyOf(combined);
	}

	/**
	 * Adds each obtainable item to one sorted creative-catalog category.
	 * Must run before {@link ModCreativeModeTabs#initialize()} seals the catalog.
	 */
	public static synchronized void initialize() {
		if (catalogRegistered) {
			return;
		}

		List<Item> furnitureDecor = new ArrayList<>();
		furnitureDecor.addAll(HEARTH_COOKWARE);
		furnitureDecor.addAll(
			FOOD_AND_CONTAINERS.subList(0, FOOD_AND_CONTAINERS.size() - FOOD_PROPS.size())
		);
		furnitureDecor.addAll(FURNISHINGS_AND_PROPS);
		ModContentCatalog.register(
			Category.FURNITURE_DECOR,
			furnitureDecor.toArray(Item[]::new)
		);
		ModContentCatalog.register(
			Category.NATURE_FARMING,
			FOOD_PROPS.toArray(Item[]::new)
		);
		catalogRegistered = true;
	}
}
