package net.beforetheblight.registry;

import java.util.List;
import java.util.function.Function;

import net.beforetheblight.BeforeTheBlight;
import net.beforetheblight.block.BushelBasketBlock;
import net.beforetheblight.block.CornBinBlock;
import net.beforetheblight.block.HandCornShellerBlock;
import net.beforetheblight.block.LayeredEarCornPileBlock;
import net.beforetheblight.block.PuncheonFloorEdgeBlock;
import net.beforetheblight.block.ScatteredEarCornBlock;
import net.beforetheblight.block.SeedCornBundleBlock;
import net.beforetheblight.block.WideSetCribWallBlock;
import net.beforetheblight.block.WoodenCornScoopBlock;
import net.beforetheblight.registry.ModContentCatalog.Category;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Isolated registry family for the corn-crib architecture and stored-corn
 * showcase slice.
 *
 * <p>The existing {@code feed_sack}, {@code wall_tool_rack}, and
 * {@code crib_board_hatch} IDs are deliberately reused from their owning
 * modules and are not registered or catalogued a second time here.</p>
 */
public final class ModCornCribBlocks {
	public static final WideSetCribWallBlock WIDE_SET_CHESTNUT_CRIB_WALL = register(
		"wide_set_chestnut_crib_wall",
		WideSetCribWallBlock::new,
		woodProperties()
	);
	public static final PuncheonFloorEdgeBlock PUNCHEON_FLOOR_EDGE_WITH_JOISTS = register(
		"puncheon_floor_edge_with_joists",
		PuncheonFloorEdgeBlock::new,
		woodProperties()
	);
	public static final LayeredEarCornPileBlock YELLOW_EAR_CORN_PILE = register(
		"yellow_ear_corn_pile",
		LayeredEarCornPileBlock::new,
		cornProperties()
	);
	public static final LayeredEarCornPileBlock MIXED_EAR_CORN_PILE = register(
		"mixed_ear_corn_pile",
		LayeredEarCornPileBlock::new,
		cornProperties()
	);
	public static final ScatteredEarCornBlock YELLOW_SCATTERED_EAR_CORN = register(
		"yellow_scattered_ear_corn",
		ScatteredEarCornBlock::new,
		cornProperties().noCollision()
	);
	public static final ScatteredEarCornBlock MIXED_SCATTERED_EAR_CORN = register(
		"mixed_scattered_ear_corn",
		ScatteredEarCornBlock::new,
		cornProperties().noCollision()
	);
	public static final SeedCornBundleBlock SEED_CORN_BUNDLE = register(
		"seed_corn_bundle",
		SeedCornBundleBlock::new,
		cornProperties().noCollision()
	);
	public static final BushelBasketBlock BUSHEL_BASKET = register(
		"bushel_basket",
		BushelBasketBlock::new,
		woodProperties()
	);
	public static final CornBinBlock CORN_BIN = register(
		"corn_bin",
		CornBinBlock::new,
		woodProperties()
	);
	public static final HandCornShellerBlock HAND_CORN_SHELLER = register(
		"hand_corn_sheller",
		HandCornShellerBlock::new,
		woodProperties()
	);
	public static final WoodenCornScoopBlock WOODEN_CORN_SCOOP = register(
		"wooden_corn_scoop",
		WoodenCornScoopBlock::new,
		woodProperties().noCollision()
	);

	/*
	 * register(...) creates and maps every BlockItem before any of these asItem
	 * calls. This is required by the 26.1.2 Item.BY_BLOCK runtime contract.
	 */
	public static final List<Item> BUILDING_ITEMS = List.of(
		WIDE_SET_CHESTNUT_CRIB_WALL.asItem(),
		PUNCHEON_FLOOR_EDGE_WITH_JOISTS.asItem()
	);
	public static final List<Item> NATURE_FARMING_ITEMS = List.of(
		YELLOW_EAR_CORN_PILE.asItem(),
		MIXED_EAR_CORN_PILE.asItem(),
		YELLOW_SCATTERED_EAR_CORN.asItem(),
		MIXED_SCATTERED_EAR_CORN.asItem(),
		SEED_CORN_BUNDLE.asItem(),
		BUSHEL_BASKET.asItem(),
		CORN_BIN.asItem()
	);
	public static final List<Item> TOOLS_WORKSTATIONS_ITEMS = List.of(
		HAND_CORN_SHELLER.asItem(),
		WOODEN_CORN_SCOOP.asItem()
	);
	public static final List<Block> ALL_BLOCKS = List.of(
		WIDE_SET_CHESTNUT_CRIB_WALL,
		PUNCHEON_FLOOR_EDGE_WITH_JOISTS,
		YELLOW_EAR_CORN_PILE,
		MIXED_EAR_CORN_PILE,
		YELLOW_SCATTERED_EAR_CORN,
		MIXED_SCATTERED_EAR_CORN,
		SEED_CORN_BUNDLE,
		BUSHEL_BASKET,
		CORN_BIN,
		HAND_CORN_SHELLER,
		WOODEN_CORN_SCOOP
	);
	public static final List<Item> ALL_ITEMS = List.of(
		WIDE_SET_CHESTNUT_CRIB_WALL.asItem(),
		PUNCHEON_FLOOR_EDGE_WITH_JOISTS.asItem(),
		YELLOW_EAR_CORN_PILE.asItem(),
		MIXED_EAR_CORN_PILE.asItem(),
		YELLOW_SCATTERED_EAR_CORN.asItem(),
		MIXED_SCATTERED_EAR_CORN.asItem(),
		SEED_CORN_BUNDLE.asItem(),
		BUSHEL_BASKET.asItem(),
		CORN_BIN.asItem(),
		HAND_CORN_SHELLER.asItem(),
		WOODEN_CORN_SCOOP.asItem()
	);
	public static final List<Block> AXE_MINEABLE_BLOCKS = List.of(
		WIDE_SET_CHESTNUT_CRIB_WALL,
		PUNCHEON_FLOOR_EDGE_WITH_JOISTS,
		BUSHEL_BASKET,
		CORN_BIN,
		HAND_CORN_SHELLER,
		WOODEN_CORN_SCOOP
	);
	public static final List<Block> HOE_MINEABLE_BLOCKS = List.of(
		YELLOW_EAR_CORN_PILE,
		MIXED_EAR_CORN_PILE,
		YELLOW_SCATTERED_EAR_CORN,
		MIXED_SCATTERED_EAR_CORN,
		SEED_CORN_BUNDLE
	);

	private static boolean initialized;

	private ModCornCribBlocks() {
	}

	private static BlockBehaviour.Properties woodProperties() {
		return BlockBehaviour.Properties.of()
			.mapColor(MapColor.WOOD)
			.strength(1.5F, 2.5F)
			.sound(SoundType.WOOD)
			.noOcclusion()
			.ignitedByLava();
	}

	private static BlockBehaviour.Properties cornProperties() {
		return BlockBehaviour.Properties.ofFullCopy(Blocks.HAY_BLOCK)
			.mapColor(MapColor.COLOR_YELLOW)
			.strength(0.25F)
			.sound(SoundType.CROP)
			.noOcclusion()
			.pushReaction(PushReaction.DESTROY)
			.ignitedByLava();
	}

	private static <T extends Block> T register(
		String name,
		Function<BlockBehaviour.Properties, T> factory,
		BlockBehaviour.Properties properties
	) {
		ResourceKey<Block> blockKey = ResourceKey.create(
			Registries.BLOCK,
			BeforeTheBlight.id(name)
		);
		T block = factory.apply(properties.setId(blockKey));
		Registry.register(BuiltInRegistries.BLOCK, blockKey, block);

		ResourceKey<Item> itemKey = ResourceKey.create(
			Registries.ITEM,
			BeforeTheBlight.id(name)
		);
		BlockItem blockItem = new BlockItem(
			block,
			new Item.Properties()
				.setId(itemKey)
				.useBlockDescriptionPrefix()
				.requiredFeatures(block.requiredFeatures())
		);
		blockItem.registerBlocks(Item.BY_BLOCK, blockItem);
		Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);
		return block;
	}

	/**
	 * Integration hook: call once after base registration and before the
	 * creative catalog is sealed.
	 */
	public static synchronized void initialize() {
		if (initialized) {
			return;
		}

		FlammableBlockRegistry flammable = FlammableBlockRegistry.getDefaultInstance();
		for (Block block : AXE_MINEABLE_BLOCKS) {
			flammable.add(block, 5, 20);
		}
		for (Block block : HOE_MINEABLE_BLOCKS) {
			flammable.add(block, 60, 100);
		}
		ModContentCatalog.register(
			Category.BUILDING_MATERIALS,
			BUILDING_ITEMS.toArray(Item[]::new)
		);
		ModContentCatalog.register(
			Category.NATURE_FARMING,
			NATURE_FARMING_ITEMS.toArray(Item[]::new)
		);
		ModContentCatalog.register(
			Category.TOOLS_WORKSTATIONS,
			TOOLS_WORKSTATIONS_ITEMS.toArray(Item[]::new)
		);
		initialized = true;
	}
}
