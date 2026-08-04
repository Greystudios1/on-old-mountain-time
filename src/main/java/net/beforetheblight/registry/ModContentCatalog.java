package net.beforetheblight.registry;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import net.beforetheblight.BeforeTheBlight;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/**
 * Canonical, categorized list of every obtainable Before the Blight item.
 *
 * <p>Registry-family classes may append their obtainable items from their
 * {@code initialize()} methods. Registration order is retained within each
 * category, an item may belong to exactly one category, and the catalog is
 * sealed when the creative tabs initialize. Internal state-only blocks must
 * never be added here.</p>
 */
public final class ModContentCatalog {
	public enum Category {
		BUILDING_MATERIALS,
		FURNITURE_DECOR,
		NATURE_FARMING,
		TOOLS_WORKSTATIONS
	}

	private static final Map<Category, List<ItemLike>> ITEMS_BY_CATEGORY =
		new EnumMap<>(Category.class);
	private static final Set<Item> INDEXED_ITEMS = new HashSet<>();
	private static boolean sealed;

	static {
		for (Category category : Category.values()) {
			ITEMS_BY_CATEGORY.put(category, new ArrayList<>());
		}
		registerBaseContent();
	}

	private ModContentCatalog() {
	}

	/**
	 * Append obtainable items to one category.
	 *
	 * <p>Call this only after the corresponding blocks/items have registered
	 * and before {@link #seal()}. Keeping this hook public means future content
	 * families do not need to edit the creative-tab implementation.</p>
	 */
	public static synchronized void register(Category category, ItemLike... items) {
		Objects.requireNonNull(category, "category");
		if (sealed) {
			throw new IllegalStateException("The Before the Blight content catalog is already sealed.");
		}

		List<ItemLike> categoryItems = ITEMS_BY_CATEGORY.get(category);
		for (ItemLike itemLike : items) {
			Objects.requireNonNull(itemLike, "item");
			Item item = itemLike.asItem();
			if (item == Items.AIR) {
				throw new IllegalArgumentException(
					"Internal block without an item cannot enter the content catalog."
				);
			}
			Identifier id = BuiltInRegistries.ITEM.getKey(item);
			if (!BeforeTheBlight.MOD_ID.equals(id.getNamespace())) {
				throw new IllegalArgumentException("Foreign content-catalog item: " + id);
			}
			if (!INDEXED_ITEMS.add(item)) {
				throw new IllegalArgumentException(
					"Duplicate content-catalog item: " + id
				);
			}
			categoryItems.add(itemLike);
		}
	}

	public static synchronized List<ItemLike> items(Category category) {
		requireSealed();
		return List.copyOf(ITEMS_BY_CATEGORY.get(Objects.requireNonNull(category, "category")));
	}

	public static synchronized List<ItemLike> allItems() {
		requireSealed();
		List<ItemLike> all = new ArrayList<>(INDEXED_ITEMS.size());
		for (Category category : Category.values()) {
			all.addAll(ITEMS_BY_CATEGORY.get(category));
		}
		return List.copyOf(all);
	}

	static synchronized void seal() {
		Set<Item> registeredItems = new HashSet<>();
		for (Item item : BuiltInRegistries.ITEM) {
			Identifier id = BuiltInRegistries.ITEM.getKey(item);
			if (BeforeTheBlight.MOD_ID.equals(id.getNamespace())) {
				registeredItems.add(item);
			}
		}
		if (!registeredItems.equals(INDEXED_ITEMS)) {
			Set<Item> missing = new HashSet<>(registeredItems);
			missing.removeAll(INDEXED_ITEMS);
			Set<Item> unexpected = new HashSet<>(INDEXED_ITEMS);
			unexpected.removeAll(registeredItems);
			throw new IllegalStateException(
				"Before the Blight content catalog does not match the item registry. Missing: "
					+ ids(missing)
					+ "; unexpected: "
					+ ids(unexpected)
			);
		}
		sealed = true;
	}

	private static void requireSealed() {
		if (!sealed) {
			throw new IllegalStateException(
				"The Before the Blight content catalog must be sealed by creative-tab initialization before it is read."
			);
		}
	}

	private static List<String> ids(Set<Item> items) {
		return items.stream()
			.map(BuiltInRegistries.ITEM::getKey)
			.map(Identifier::toString)
			.sorted()
			.toList();
	}

	private static void registerBaseContent() {
		register(
			Category.BUILDING_MATERIALS,
			ModBlocks.CHESTNUT_LOG,
			ModBlocks.HEWN_CHESTNUT_BEAM,
			ModBlocks.HEWN_CHESTNUT_WALL,
			ModBlocks.HEWN_CHESTNUT_POST,
			ModBlocks.HEWN_OAK_BEAM,
			ModBlocks.HEWN_SPRUCE_BEAM,
			ModBlocks.CHESTNUT_WOOD,
			ModBlocks.STRIPPED_CHESTNUT_LOG,
			ModBlocks.STRIPPED_CHESTNUT_WOOD,
			ModBlocks.CHESTNUT_PLANKS,
			ModBlocks.ROUGH_CHESTNUT_BOARDS,
			ModBlocks.ROUGH_CHESTNUT_BOARD_STAIRS,
			ModBlocks.ROUGH_CHESTNUT_OPEN_STAIRCASE,
			ModBlocks.ROUGH_CHESTNUT_BOARD_SLAB,
			ModBlocks.CHESTNUT_SHINGLES,
			ModBlocks.CHESTNUT_SHINGLE_STAIRS,
			ModBlocks.CHESTNUT_SHINGLE_SLAB,
			ModBlocks.SPLIT_CHESTNUT_RAILS,
			ModBlocks.CHINKED_CHESTNUT_LOGS,
			ModBlocks.FIELDSTONE,
			ModBlocks.FIELDSTONE_STAIRS,
			ModBlocks.FIELDSTONE_SLAB,
			ModBlocks.FIELDSTONE_WALL,
			ModBlocks.DRESSED_FIELDSTONE,
			ModBlocks.DRESSED_FIELDSTONE_STAIRS,
			ModBlocks.DRESSED_FIELDSTONE_SLAB,
			ModBlocks.DRESSED_FIELDSTONE_WALL,
			ModBlocks.CHISELED_FIELDSTONE,
			ModBlocks.FIELDSTONE_PIER,
			ModBlocks.ROUGH_OAK_BOARDS,
			ModBlocks.ROUGH_SPRUCE_BOARDS,
			ModBlocks.CHESTNUT_STAIRS,
			ModBlocks.CHESTNUT_SLAB,
			ModBlocks.CHESTNUT_FENCE,
			ModBlocks.CHESTNUT_FENCE_GATE,
			ModBlocks.CHESTNUT_PRESSURE_PLATE,
			ModBlocks.CHESTNUT_BUTTON,
			ModBlocks.HEMLOCK_LOG,
			ModBlocks.HEMLOCK_WOOD,
			ModBlocks.STRIPPED_HEMLOCK_LOG,
			ModBlocks.STRIPPED_HEMLOCK_WOOD,
			ModBlocks.HEMLOCK_PLANKS,
			ModBlocks.HEMLOCK_STAIRS,
			ModBlocks.HEMLOCK_SLAB,
			ModBlocks.HEMLOCK_FENCE,
			ModBlocks.HEMLOCK_FENCE_GATE,
			ModBlocks.HEMLOCK_PRESSURE_PLATE,
			ModBlocks.HEMLOCK_BUTTON,
			ModBlocks.AMERICAN_BEECH_LOG,
			ModBlocks.AMERICAN_BEECH_WOOD,
			ModBlocks.STRIPPED_AMERICAN_BEECH_LOG,
			ModBlocks.STRIPPED_AMERICAN_BEECH_WOOD,
			ModBlocks.AMERICAN_BEECH_PLANKS,
			ModBlocks.AMERICAN_BEECH_STAIRS,
			ModBlocks.AMERICAN_BEECH_SLAB,
			ModBlocks.AMERICAN_BEECH_FENCE,
			ModBlocks.AMERICAN_BEECH_FENCE_GATE,
			ModBlocks.AMERICAN_BEECH_PRESSURE_PLATE,
			ModBlocks.AMERICAN_BEECH_BUTTON,
			ModBlocks.BLACK_WALNUT_LOG,
			ModBlocks.BLACK_WALNUT_WOOD,
			ModBlocks.STRIPPED_BLACK_WALNUT_LOG,
			ModBlocks.STRIPPED_BLACK_WALNUT_WOOD
		);
		register(
			Category.FURNITURE_DECOR,
			ModBlocks.ROCKING_CHAIR
		);
		register(
			Category.NATURE_FARMING,
			ModBlocks.CHESTNUT_LEAVES,
			ModBlocks.CHESTNUT_SAPLING,
			ModBlocks.HEMLOCK_FOLIAGE,
			ModBlocks.HEMLOCK_SAPLING,
			ModBlocks.AMERICAN_BEECH_LEAVES,
			ModBlocks.AMERICAN_BEECH_SAPLING,
			ModBlocks.BLACK_WALNUT_LEAVES,
			ModBlocks.BLACK_WALNUT_SAPLING,
			ModBlocks.MOUNTAIN_LAUREL,
			ModBlocks.LOWBUSH_BLUEBERRY,
			ModBlocks.FOREST_DUFF,
			ModItems.HANDFUL_OF_CHESTNUTS,
			ModItems.ROASTED_CHESTNUTS,
			ModItems.CORN_KERNELS,
			ModItems.EAR_OF_CORN,
			ModItems.CORNMEAL,
			ModBlocks.DRYING_CORN_BUNDLE,
			ModItems.DRIED_EAR_OF_CORN
		);
		register(
			Category.TOOLS_WORKSTATIONS,
			ModItems.BROAD_AXE,
			ModItems.FRAME_SAW,
			ModItems.FROE,
			ModItems.WOODEN_MAUL,
			ModBlocks.SAWING_TRESTLES,
			ModBlocks.SPLITTING_STUMP
		);
	}
}
