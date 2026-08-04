package net.beforetheblight.registry;

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

/** Obtainable item forms and categorized creative ordering for exterior assets. */
public final class ModExteriorItems {
	public static final Item SPLIT_RAIL_FENCE = registerBlockItem(
		"split_rail_fence",
		ModExteriorBlocks.SPLIT_RAIL_FENCE
	);
	public static final Item SPLIT_RAIL_GATE = registerBlockItem(
		"split_rail_gate",
		ModExteriorBlocks.SPLIT_RAIL_GATE
	);
	public static final Item WEATHERED_SPLIT_RAIL_FENCE = registerBlockItem(
		"weathered_split_rail_fence",
		ModExteriorBlocks.WEATHERED_SPLIT_RAIL_FENCE
	);
	public static final Item BROKEN_SPLIT_RAIL_FENCE = registerBlockItem(
		"broken_split_rail_fence",
		ModExteriorBlocks.BROKEN_SPLIT_RAIL_FENCE
	);
	public static final Item PEELED_POLE_FENCE = registerBlockItem(
		"peeled_pole_fence",
		ModExteriorBlocks.PEELED_POLE_FENCE
	);
	public static final Item PEELED_POLE_GATE = registerBlockItem(
		"peeled_pole_gate",
		ModExteriorBlocks.PEELED_POLE_GATE
	);

	public static final Item STACKED_FIREWOOD = registerBlockItem(
		"stacked_firewood",
		ModExteriorBlocks.STACKED_FIREWOOD
	);
	public static final Item LOG_STACK = registerBlockItem(
		"log_stack",
		ModExteriorBlocks.LOG_STACK
	);
	public static final Item SHINGLE_STACK = registerBlockItem(
		"shingle_stack",
		ModExteriorBlocks.SHINGLE_STACK
	);
	public static final Item CHOPPING_BLOCK = registerBlockItem(
		"chopping_block",
		ModExteriorBlocks.CHOPPING_BLOCK
	);
	public static final Item AXE_IN_CHOPPING_BLOCK = registerBlockItem(
		"axe_in_chopping_block",
		ModExteriorBlocks.AXE_IN_CHOPPING_BLOCK
	);
	public static final Item SAWHORSE = registerBlockItem(
		"sawhorse",
		ModExteriorBlocks.SAWHORSE
	);
	public static final Item WALL_TOOL_RACK = registerBlockItem(
		"wall_tool_rack",
		ModExteriorBlocks.WALL_TOOL_RACK
	);
	public static final Item WAGON_WHEEL = registerBlockItem(
		"wagon_wheel",
		ModExteriorBlocks.WAGON_WHEEL
	);
	public static final Item WOODEN_BARREL = registerBlockItem(
		"wooden_barrel",
		ModExteriorBlocks.WOODEN_BARREL
	);
	public static final Item PRODUCE_CRATE = registerBlockItem(
		"produce_crate",
		ModExteriorBlocks.PRODUCE_CRATE
	);
	public static final Item FEED_SACK = registerBlockItem(
		"feed_sack",
		ModExteriorBlocks.FEED_SACK
	);

	public static final Item PACKED_DIRT_PATH = registerBlockItem(
		"packed_dirt_path",
		ModExteriorBlocks.PACKED_DIRT_PATH
	);
	public static final Item PATH_EDGE = registerBlockItem(
		"path_edge",
		ModExteriorBlocks.PATH_EDGE
	);
	public static final Item WAGON_RUT = registerBlockItem(
		"wagon_rut",
		ModExteriorBlocks.WAGON_RUT
	);
	public static final Item MUDDY_WAGON_RUT = registerBlockItem(
		"muddy_wagon_rut",
		ModExteriorBlocks.MUDDY_WAGON_RUT
	);
	public static final Item STREAM_BANK_STONES = registerBlockItem(
		"stream_bank_stones",
		ModExteriorBlocks.STREAM_BANK_STONES
	);
	public static final Item EXPOSED_ROOT = registerBlockItem(
		"exposed_root",
		ModExteriorBlocks.EXPOSED_ROOT
	);
	public static final Item MOSS_PATCH = registerBlockItem(
		"moss_patch",
		ModExteriorBlocks.MOSS_PATCH
	);
	public static final Item LEAF_LITTER = registerBlockItem(
		"leaf_litter",
		ModExteriorBlocks.LEAF_LITTER
	);
	public static final Item FALLEN_BRANCH = registerBlockItem(
		"fallen_branch",
		ModExteriorBlocks.FALLEN_BRANCH
	);
	public static final Item BRUSH_PILE = registerBlockItem(
		"brush_pile",
		ModExteriorBlocks.BRUSH_PILE
	);

	public static final Item ROTTED_LOG_STACK = registerBlockItem(
		"rotted_log_stack",
		ModExteriorBlocks.ROTTED_LOG_STACK
	);
	public static final Item BROKEN_WAGON_WHEEL = registerBlockItem(
		"broken_wagon_wheel",
		ModExteriorBlocks.BROKEN_WAGON_WHEEL
	);
	public static final Item BROKEN_CRATE = registerBlockItem(
		"broken_crate",
		ModExteriorBlocks.BROKEN_CRATE
	);

	public static final List<Item> BUILDING_ITEMS = List.of(
		SPLIT_RAIL_FENCE,
		SPLIT_RAIL_GATE,
		WEATHERED_SPLIT_RAIL_FENCE,
		BROKEN_SPLIT_RAIL_FENCE,
		PEELED_POLE_FENCE,
		PEELED_POLE_GATE
	);
	public static final List<Item> WORKSHOP_ITEMS = List.of(
		CHOPPING_BLOCK,
		AXE_IN_CHOPPING_BLOCK,
		SAWHORSE,
		WALL_TOOL_RACK
	);
	public static final List<Item> NATURE_FARMING_ITEMS = List.of(
		FEED_SACK
	);
	public static final List<Item> DECOR_ITEMS = List.of(
		STACKED_FIREWOOD,
		LOG_STACK,
		SHINGLE_STACK,
		WAGON_WHEEL,
		WOODEN_BARREL,
		PRODUCE_CRATE,
		PACKED_DIRT_PATH,
		PATH_EDGE,
		WAGON_RUT,
		MUDDY_WAGON_RUT,
		STREAM_BANK_STONES,
		EXPOSED_ROOT,
		MOSS_PATCH,
		LEAF_LITTER,
		FALLEN_BRANCH,
		BRUSH_PILE,
		ROTTED_LOG_STACK,
		BROKEN_WAGON_WHEEL,
		BROKEN_CRATE
	);
	public static final List<Item> ALL_ITEMS = List.of(
		SPLIT_RAIL_FENCE,
		SPLIT_RAIL_GATE,
		WEATHERED_SPLIT_RAIL_FENCE,
		BROKEN_SPLIT_RAIL_FENCE,
		PEELED_POLE_FENCE,
		PEELED_POLE_GATE,
		STACKED_FIREWOOD,
		LOG_STACK,
		SHINGLE_STACK,
		CHOPPING_BLOCK,
		AXE_IN_CHOPPING_BLOCK,
		SAWHORSE,
		WALL_TOOL_RACK,
		WAGON_WHEEL,
		WOODEN_BARREL,
		PRODUCE_CRATE,
		FEED_SACK,
		PACKED_DIRT_PATH,
		PATH_EDGE,
		WAGON_RUT,
		MUDDY_WAGON_RUT,
		STREAM_BANK_STONES,
		EXPOSED_ROOT,
		MOSS_PATCH,
		LEAF_LITTER,
		FALLEN_BRANCH,
		BRUSH_PILE,
		ROTTED_LOG_STACK,
		BROKEN_WAGON_WHEEL,
		BROKEN_CRATE
	);

	private static boolean catalogRegistered;

	private ModExteriorItems() {
	}

	private static Item registerBlockItem(String name, Block block) {
		ResourceKey<Item> itemKey = ResourceKey.create(
			Registries.ITEM,
			BeforeTheBlight.id(name)
		);
		BlockItem blockItem = new BlockItem(
			block,
			new Item.Properties()
				.setId(itemKey)
				.useBlockDescriptionPrefix()
		);
		blockItem.registerBlocks(Item.BY_BLOCK, blockItem);
		return Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);
	}

	public static synchronized void initialize() {
		ModExteriorBlocks.initialize();
		if (catalogRegistered) {
			return;
		}
		ModContentCatalog.register(
			Category.BUILDING_MATERIALS,
			BUILDING_ITEMS.toArray(Item[]::new)
		);
		ModContentCatalog.register(
			Category.TOOLS_WORKSTATIONS,
			WORKSHOP_ITEMS.toArray(Item[]::new)
		);
		ModContentCatalog.register(
			Category.NATURE_FARMING,
			NATURE_FARMING_ITEMS.toArray(Item[]::new)
		);
		ModContentCatalog.register(
			Category.FURNITURE_DECOR,
			DECOR_ITEMS.toArray(Item[]::new)
		);
		catalogRegistered = true;
	}
}
