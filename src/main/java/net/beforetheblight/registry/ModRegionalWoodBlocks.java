package net.beforetheblight.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import net.beforetheblight.BeforeTheBlight;
import net.beforetheblight.block.RegionalHalfLogBlock;
import net.beforetheblight.block.RegionalPoleBlock;
import net.beforetheblight.block.RegionalThinBoardBlock;
import net.beforetheblight.registry.ModContentCatalog.Category;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

/**
 * Regional construction woods and furniture stock used by the farmstead
 * building palette.
 *
 * <p>The class deliberately owns its same-ID {@link BlockItem}s and exposes
 * stable category lists so creative inventory, datagen, structure tooling, and
 * audits can consume one canonical registration order.</p>
 */
public final class ModRegionalWoodBlocks {
	// Yellow-poplar structural family.
	public static final Block YELLOW_POPLAR_LOG = pillar(
		"yellow_poplar_log", MapColor.WOOD, MapColor.COLOR_BROWN
	);
	public static final Block STRIPPED_YELLOW_POPLAR_LOG = pillar(
		"stripped_yellow_poplar_log", MapColor.SAND, MapColor.WOOD
	);
	public static final RegionalPoleBlock PEELED_YELLOW_POPLAR_POLE = pole(
		"peeled_yellow_poplar_pole", MapColor.SAND
	);
	public static final Block HAND_HEWN_YELLOW_POPLAR_LOG = pillar(
		"hand_hewn_yellow_poplar_log", MapColor.SAND, MapColor.WOOD
	);
	public static final Block SQUARED_YELLOW_POPLAR_TIMBER = pillar(
		"squared_yellow_poplar_timber", MapColor.SAND, MapColor.WOOD
	);
	public static final Block YELLOW_POPLAR_BEAM = pillar(
		"yellow_poplar_beam", MapColor.SAND, MapColor.WOOD
	);
	public static final RegionalPoleBlock YELLOW_POPLAR_POST = pole(
		"yellow_poplar_post", MapColor.WOOD
	);
	public static final RegionalHalfLogBlock YELLOW_POPLAR_HALF_LOG = halfLog(
		"yellow_poplar_half_log", MapColor.WOOD
	);
	public static final Block YELLOW_POPLAR_END_GRAIN = cube(
		"yellow_poplar_end_grain", MapColor.SAND
	);
	public static final RegionalThinBoardBlock HAND_SPLIT_YELLOW_POPLAR_BOARD = board(
		"hand_split_yellow_poplar_board", MapColor.SAND
	);
	public static final RegionalThinBoardBlock SASH_SAWN_YELLOW_POPLAR_BOARD = board(
		"sash_sawn_yellow_poplar_board", MapColor.SAND
	);
	public static final RegionalThinBoardBlock YELLOW_POPLAR_FURNITURE_BOARD = board(
		"yellow_poplar_furniture_board", MapColor.SAND
	);

	// White-oak durable construction and seating stock.
	public static final Block WHITE_OAK_LOG = pillar(
		"white_oak_log", MapColor.WOOD, MapColor.STONE
	);
	public static final Block STRIPPED_WHITE_OAK_LOG = pillar(
		"stripped_white_oak_log", MapColor.SAND, MapColor.WOOD
	);
	public static final RegionalPoleBlock PEELED_WHITE_OAK_POLE = pole(
		"peeled_white_oak_pole", MapColor.SAND
	);
	public static final Block HAND_HEWN_WHITE_OAK_TIMBER = pillar(
		"hand_hewn_white_oak_timber", MapColor.SAND, MapColor.WOOD
	);
	public static final Block WHITE_OAK_BEAM = pillar(
		"white_oak_beam", MapColor.SAND, MapColor.WOOD
	);
	public static final RegionalPoleBlock WHITE_OAK_POST = pole(
		"white_oak_post", MapColor.WOOD
	);
	public static final RegionalThinBoardBlock WHITE_OAK_BOARD = board(
		"white_oak_board", MapColor.WOOD
	);
	public static final RegionalPoleBlock WHITE_OAK_SPLIT_RAIL = pole(
		"white_oak_split_rail", MapColor.WOOD
	);
	public static final RegionalThinBoardBlock WHITE_OAK_FURNITURE_BOARD = board(
		"white_oak_furniture_board", MapColor.WOOD
	);
	public static final RegionalThinBoardBlock WHITE_OAK_SPLINT_SEAT = board(
		"white_oak_splint_seat", MapColor.SAND
	);

	// Black-walnut finished furniture components.
	public static final RegionalThinBoardBlock WALNUT_FURNITURE_BOARD = board(
		"walnut_furniture_board", MapColor.COLOR_BROWN
	);
	public static final RegionalPoleBlock WALNUT_TURNED_POST = pole(
		"walnut_turned_post", MapColor.COLOR_BROWN
	);
	public static final RegionalPoleBlock WALNUT_CARVED_RAIL = pole(
		"walnut_carved_rail", MapColor.COLOR_BROWN
	);
	public static final RegionalThinBoardBlock WALNUT_TABLETOP = board(
		"walnut_tabletop", MapColor.COLOR_BROWN
	);
	public static final RegionalThinBoardBlock WALNUT_CHEST_PANEL = board(
		"walnut_chest_panel", MapColor.COLOR_BROWN
	);
	public static final RegionalThinBoardBlock WALNUT_SHELF = board(
		"walnut_shelf", MapColor.COLOR_BROWN
	);

	// Pine and hemlock secondary boards.
	public static final RegionalThinBoardBlock PINE_ROUGH_BOARD = board(
		"pine_rough_board", MapColor.WOOD
	);
	public static final RegionalThinBoardBlock PINE_SASH_SAWN_BOARD = board(
		"pine_sash_sawn_board", MapColor.WOOD
	);
	public static final RegionalThinBoardBlock PINE_SHELF = board(
		"pine_shelf", MapColor.WOOD
	);
	public static final Block PINE_CRATE = cube("pine_crate", MapColor.WOOD);
	public static final RegionalThinBoardBlock HEMLOCK_ROUGH_BOARD = board(
		"hemlock_rough_board", MapColor.PODZOL
	);
	public static final RegionalThinBoardBlock HEMLOCK_ROOF_BOARD = board(
		"hemlock_roof_board", MapColor.PODZOL
	);
	public static final RegionalThinBoardBlock WEATHERED_SOFTWOOD_BOARD = board(
		"weathered_softwood_board", MapColor.STONE
	);

	public static final List<Block> YELLOW_POPLAR_BLOCKS = List.of(
		YELLOW_POPLAR_LOG,
		STRIPPED_YELLOW_POPLAR_LOG,
		PEELED_YELLOW_POPLAR_POLE,
		HAND_HEWN_YELLOW_POPLAR_LOG,
		SQUARED_YELLOW_POPLAR_TIMBER,
		YELLOW_POPLAR_BEAM,
		YELLOW_POPLAR_POST,
		YELLOW_POPLAR_HALF_LOG,
		YELLOW_POPLAR_END_GRAIN,
		HAND_SPLIT_YELLOW_POPLAR_BOARD,
		SASH_SAWN_YELLOW_POPLAR_BOARD,
		YELLOW_POPLAR_FURNITURE_BOARD
	);
	public static final List<Block> WHITE_OAK_BLOCKS = List.of(
		WHITE_OAK_LOG,
		STRIPPED_WHITE_OAK_LOG,
		PEELED_WHITE_OAK_POLE,
		HAND_HEWN_WHITE_OAK_TIMBER,
		WHITE_OAK_BEAM,
		WHITE_OAK_POST,
		WHITE_OAK_BOARD,
		WHITE_OAK_SPLIT_RAIL,
		WHITE_OAK_FURNITURE_BOARD,
		WHITE_OAK_SPLINT_SEAT
	);
	public static final List<Block> WALNUT_COMPONENTS = List.of(
		WALNUT_FURNITURE_BOARD,
		WALNUT_TURNED_POST,
		WALNUT_CARVED_RAIL,
		WALNUT_TABLETOP,
		WALNUT_CHEST_PANEL,
		WALNUT_SHELF
	);
	public static final List<Block> PINE_BLOCKS = List.of(
		PINE_ROUGH_BOARD,
		PINE_SASH_SAWN_BOARD,
		PINE_SHELF,
		PINE_CRATE
	);
	public static final List<Block> HEMLOCK_AND_SOFTWOOD_BLOCKS = List.of(
		HEMLOCK_ROUGH_BOARD,
		HEMLOCK_ROOF_BOARD,
		WEATHERED_SOFTWOOD_BOARD
	);

	public static final List<Block> LOGS_AND_TIMBERS = List.of(
		YELLOW_POPLAR_LOG,
		STRIPPED_YELLOW_POPLAR_LOG,
		HAND_HEWN_YELLOW_POPLAR_LOG,
		SQUARED_YELLOW_POPLAR_TIMBER,
		YELLOW_POPLAR_BEAM,
		YELLOW_POPLAR_HALF_LOG,
		YELLOW_POPLAR_END_GRAIN,
		WHITE_OAK_LOG,
		STRIPPED_WHITE_OAK_LOG,
		HAND_HEWN_WHITE_OAK_TIMBER,
		WHITE_OAK_BEAM
	);
	public static final List<Block> POLES_AND_POSTS = List.of(
		PEELED_YELLOW_POPLAR_POLE,
		YELLOW_POPLAR_POST,
		PEELED_WHITE_OAK_POLE,
		WHITE_OAK_POST,
		WHITE_OAK_SPLIT_RAIL,
		WALNUT_TURNED_POST,
		WALNUT_CARVED_RAIL
	);
	public static final List<Block> BOARDS_AND_COMPONENTS = List.of(
		HAND_SPLIT_YELLOW_POPLAR_BOARD,
		SASH_SAWN_YELLOW_POPLAR_BOARD,
		YELLOW_POPLAR_FURNITURE_BOARD,
		WHITE_OAK_BOARD,
		WHITE_OAK_FURNITURE_BOARD,
		WHITE_OAK_SPLINT_SEAT,
		WALNUT_FURNITURE_BOARD,
		WALNUT_TABLETOP,
		WALNUT_CHEST_PANEL,
		WALNUT_SHELF,
		PINE_ROUGH_BOARD,
		PINE_SASH_SAWN_BOARD,
		PINE_SHELF,
		PINE_CRATE,
		HEMLOCK_ROUGH_BOARD,
		HEMLOCK_ROOF_BOARD,
		WEATHERED_SOFTWOOD_BOARD
	);

	public static final List<Block> BUILDING_MATERIALS = List.of(
		YELLOW_POPLAR_LOG,
		STRIPPED_YELLOW_POPLAR_LOG,
		PEELED_YELLOW_POPLAR_POLE,
		HAND_HEWN_YELLOW_POPLAR_LOG,
		SQUARED_YELLOW_POPLAR_TIMBER,
		YELLOW_POPLAR_BEAM,
		YELLOW_POPLAR_POST,
		YELLOW_POPLAR_HALF_LOG,
		YELLOW_POPLAR_END_GRAIN,
		HAND_SPLIT_YELLOW_POPLAR_BOARD,
		SASH_SAWN_YELLOW_POPLAR_BOARD,
		WHITE_OAK_LOG,
		STRIPPED_WHITE_OAK_LOG,
		PEELED_WHITE_OAK_POLE,
		HAND_HEWN_WHITE_OAK_TIMBER,
		WHITE_OAK_BEAM,
		WHITE_OAK_POST,
		WHITE_OAK_BOARD,
		WHITE_OAK_SPLIT_RAIL,
		PINE_ROUGH_BOARD,
		PINE_SASH_SAWN_BOARD,
		HEMLOCK_ROUGH_BOARD,
		HEMLOCK_ROOF_BOARD,
		WEATHERED_SOFTWOOD_BOARD
	);
	public static final List<Block> FURNITURE_DECOR = List.of(
		YELLOW_POPLAR_FURNITURE_BOARD,
		WHITE_OAK_FURNITURE_BOARD,
		WHITE_OAK_SPLINT_SEAT,
		WALNUT_FURNITURE_BOARD,
		WALNUT_TURNED_POST,
		WALNUT_CARVED_RAIL,
		WALNUT_TABLETOP,
		WALNUT_CHEST_PANEL,
		WALNUT_SHELF,
		PINE_SHELF,
		PINE_CRATE
	);

	public static final List<Block> ALL_BLOCKS = allBlocks();
	public static final List<Block> REGIONAL_WOOD_BLOCKS = ALL_BLOCKS;
	public static final Map<String, List<Block>> BLOCKS_BY_CATEGORY = Map.of(
		"building_materials", BUILDING_MATERIALS,
		"furniture_decor", FURNITURE_DECOR
	);
	public static final Map<String, String> DISPLAY_NAMES = displayNames();
	public static final Map<String, String> EN_US_TRANSLATIONS = translationEntries();

	private static boolean initialized;

	private ModRegionalWoodBlocks() {
	}

	/**
	 * Installs relationships that require all registrations to exist, then
	 * contributes the family to the sorted creative content catalog.
	 */
	public static synchronized void initialize() {
		if (initialized) {
			return;
		}
		initialized = true;

		StrippableBlockRegistry.register(YELLOW_POPLAR_LOG, STRIPPED_YELLOW_POPLAR_LOG);
		StrippableBlockRegistry.register(WHITE_OAK_LOG, STRIPPED_WHITE_OAK_LOG);

		FlammableBlockRegistry flammable = FlammableBlockRegistry.getDefaultInstance();
		for (Block block : LOGS_AND_TIMBERS) {
			flammable.add(block, 5, 5);
		}
		for (Block block : POLES_AND_POSTS) {
			flammable.add(block, 5, 5);
		}
		for (Block block : BOARDS_AND_COMPONENTS) {
			flammable.add(block, 5, 20);
		}

		ModContentCatalog.register(
			Category.BUILDING_MATERIALS,
			BUILDING_MATERIALS.toArray(Block[]::new)
		);
		ModContentCatalog.register(
			Category.FURNITURE_DECOR,
			FURNITURE_DECOR.toArray(Block[]::new)
		);
	}

	private static Block pillar(String name, MapColor endColor, MapColor sideColor) {
		return register(
			name,
			RotatedPillarBlock::new,
			logProperties(endColor, sideColor)
		);
	}

	private static RegionalPoleBlock pole(String name, MapColor color) {
		return register(name, RegionalPoleBlock::new, shapedWoodProperties(color));
	}

	private static RegionalHalfLogBlock halfLog(String name, MapColor color) {
		return register(name, RegionalHalfLogBlock::new, shapedWoodProperties(color));
	}

	private static RegionalThinBoardBlock board(String name, MapColor color) {
		return register(name, RegionalThinBoardBlock::new, shapedWoodProperties(color));
	}

	private static Block cube(String name, MapColor color) {
		return register(name, Block::new, woodProperties(color));
	}

	private static BlockBehaviour.Properties logProperties(
		MapColor endColor,
		MapColor sideColor
	) {
		return BlockBehaviour.Properties.of()
			.mapColor(
				state -> state.getValue(RotatedPillarBlock.AXIS).isVertical()
					? endColor
					: sideColor
			)
			.instrument(NoteBlockInstrument.BASS)
			.strength(2.0F)
			.sound(SoundType.WOOD)
			.ignitedByLava();
	}

	private static BlockBehaviour.Properties woodProperties(MapColor color) {
		return BlockBehaviour.Properties.of()
			.mapColor(color)
			.instrument(NoteBlockInstrument.BASS)
			.strength(2.0F, 3.0F)
			.sound(SoundType.WOOD)
			.ignitedByLava();
	}

	private static BlockBehaviour.Properties shapedWoodProperties(MapColor color) {
		return woodProperties(color).noOcclusion();
	}

	private static <T extends Block> T register(
		String name,
		Function<BlockBehaviour.Properties, T> blockFactory,
		BlockBehaviour.Properties properties
	) {
		ResourceKey<Block> blockKey =
			ResourceKey.create(Registries.BLOCK, BeforeTheBlight.id(name));
		T block = blockFactory.apply(properties.setId(blockKey));

		ResourceKey<Item> itemKey =
			ResourceKey.create(Registries.ITEM, BeforeTheBlight.id(name));
		BlockItem item = new BlockItem(
			block,
			new Item.Properties()
				.setId(itemKey)
				.useBlockDescriptionPrefix()
		);
		item.registerBlocks(Item.BY_BLOCK, item);
		Registry.register(BuiltInRegistries.ITEM, itemKey, item);
		return Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
	}

	private static List<Block> allBlocks() {
		List<Block> blocks = new ArrayList<>();
		blocks.addAll(YELLOW_POPLAR_BLOCKS);
		blocks.addAll(WHITE_OAK_BLOCKS);
		blocks.addAll(WALNUT_COMPONENTS);
		blocks.addAll(PINE_BLOCKS);
		blocks.addAll(HEMLOCK_AND_SOFTWOOD_BLOCKS);
		return List.copyOf(blocks);
	}

	private static Map<String, String> displayNames() {
		Map<String, String> names = new LinkedHashMap<>();
		names.put("yellow_poplar_log", "Yellow-Poplar Log");
		names.put("stripped_yellow_poplar_log", "Stripped Yellow-Poplar Log");
		names.put("peeled_yellow_poplar_pole", "Peeled Yellow-Poplar Pole");
		names.put("hand_hewn_yellow_poplar_log", "Hand-Hewn Yellow-Poplar Log");
		names.put("squared_yellow_poplar_timber", "Squared Yellow-Poplar Timber");
		names.put("yellow_poplar_beam", "Yellow-Poplar Beam");
		names.put("yellow_poplar_post", "Yellow-Poplar Post");
		names.put("yellow_poplar_half_log", "Yellow-Poplar Half Log");
		names.put("yellow_poplar_end_grain", "Yellow-Poplar End Grain");
		names.put("hand_split_yellow_poplar_board", "Hand-Split Yellow-Poplar Board");
		names.put("sash_sawn_yellow_poplar_board", "Sash-Sawn Yellow-Poplar Board");
		names.put("yellow_poplar_furniture_board", "Yellow-Poplar Furniture Board");
		names.put("white_oak_log", "White Oak Log");
		names.put("stripped_white_oak_log", "Stripped White Oak Log");
		names.put("peeled_white_oak_pole", "Peeled White Oak Pole");
		names.put("hand_hewn_white_oak_timber", "Hand-Hewn White Oak Timber");
		names.put("white_oak_beam", "White Oak Beam");
		names.put("white_oak_post", "White Oak Post");
		names.put("white_oak_board", "White Oak Board");
		names.put("white_oak_split_rail", "White Oak Split Rail");
		names.put("white_oak_furniture_board", "White Oak Furniture Board");
		names.put("white_oak_splint_seat", "White Oak Splint Seat");
		names.put("walnut_furniture_board", "Black Walnut Furniture Board");
		names.put("walnut_turned_post", "Black Walnut Turned Post");
		names.put("walnut_carved_rail", "Black Walnut Carved Rail");
		names.put("walnut_tabletop", "Black Walnut Tabletop");
		names.put("walnut_chest_panel", "Black Walnut Chest Panel");
		names.put("walnut_shelf", "Black Walnut Shelf");
		names.put("pine_rough_board", "Pine Rough Board");
		names.put("pine_sash_sawn_board", "Pine Sash-Sawn Board");
		names.put("pine_shelf", "Pine Shelf");
		names.put("pine_crate", "Pine Crate");
		names.put("hemlock_rough_board", "Hemlock Rough Board");
		names.put("hemlock_roof_board", "Hemlock Roof Board");
		names.put("weathered_softwood_board", "Weathered Softwood Board");
		return Collections.unmodifiableMap(names);
	}

	private static Map<String, String> translationEntries() {
		Map<String, String> translations = new LinkedHashMap<>();
		DISPLAY_NAMES.forEach(
			(id, name) -> translations.put("block.before_the_blight." + id, name)
		);
		return Collections.unmodifiableMap(translations);
	}
}
