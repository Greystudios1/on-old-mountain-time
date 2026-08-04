package net.beforetheblight.registry;

import net.beforetheblight.BeforeTheBlight;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Public material tags for structure pools, recipes, and compatibility packs.
 */
public final class ModBoardRoofTags {
	public static final TagKey<Block> PUNCHEON_FLOORS = block("puncheon_floors");
	public static final TagKey<Item> PUNCHEON_FLOOR_ITEMS = item("puncheon_floors");
	public static final TagKey<Block> SPLIT_SHINGLES = block("split_shingles");
	public static final TagKey<Item> SPLIT_SHINGLE_ITEMS = item("split_shingles");
	public static final TagKey<Block> APPALACHIAN_BOARDWORK = block("appalachian_boardwork");
	public static final TagKey<Item> APPALACHIAN_BOARDWORK_ITEMS = item("appalachian_boardwork");
	public static final TagKey<Block> ROOF_TRIM = block("roof_trim");
	public static final TagKey<Item> ROOF_TRIM_ITEMS = item("roof_trim");

	private ModBoardRoofTags() {
	}

	private static TagKey<Block> block(String path) {
		return TagKey.create(Registries.BLOCK, BeforeTheBlight.id(path));
	}

	private static TagKey<Item> item(String path) {
		return TagKey.create(Registries.ITEM, BeforeTheBlight.id(path));
	}
}
