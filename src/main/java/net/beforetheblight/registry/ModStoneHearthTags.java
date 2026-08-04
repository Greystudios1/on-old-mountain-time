package net.beforetheblight.registry;

import net.beforetheblight.BeforeTheBlight;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Isolated tag contract for the stone, hearth, earth, and chinking slice.
 */
public final class ModStoneHearthTags {
	public static final TagKey<Block> FIELDSTONE = block("fieldstone");
	public static final TagKey<Item> FIELDSTONE_ITEMS = item("fieldstone");
	public static final TagKey<Block> HEARTH_COMPONENTS = block("hearth_components");
	public static final TagKey<Item> HEARTH_COMPONENT_ITEMS = item("hearth_components");
	public static final TagKey<Block> EARTH_FLOORS = block("earth_floors");
	public static final TagKey<Item> EARTH_FLOOR_ITEMS = item("earth_floors");
	public static final TagKey<Block> FLOOR_CLUTTER = block("floor_clutter");
	public static final TagKey<Item> FLOOR_CLUTTER_ITEMS = item("floor_clutter");
	public static final TagKey<Block> CHINKING_MATERIALS = block("chinking_materials");
	public static final TagKey<Item> CHINKING_MATERIAL_ITEMS = item("chinking_materials");
	public static final TagKey<Block> FOUNDATION_COMPONENTS =
		block("fieldstone_foundation_components");
	public static final TagKey<Item> FOUNDATION_COMPONENT_ITEMS =
		item("fieldstone_foundation_components");
	public static final TagKey<Block> FIREPLACE_COMPONENTS =
		block("fireplace_components");
	public static final TagKey<Item> FIREPLACE_COMPONENT_ITEMS =
		item("fireplace_components");
	public static final TagKey<Block> MASONRY_DETAILS =
		block("masonry_details");
	public static final TagKey<Item> MASONRY_DETAIL_ITEMS =
		item("masonry_details");
	public static final TagKey<Block> EARTH_LAYERS =
		block("earth_layers");
	public static final TagKey<Item> EARTH_LAYER_ITEMS =
		item("earth_layers");
	public static final TagKey<Item> HEARTH_COOKWARE =
		item("hearth_cookware");

	private ModStoneHearthTags() {
	}

	private static TagKey<Block> block(String path) {
		return TagKey.create(Registries.BLOCK, BeforeTheBlight.id(path));
	}

	private static TagKey<Item> item(String path) {
		return TagKey.create(Registries.ITEM, BeforeTheBlight.id(path));
	}
}
