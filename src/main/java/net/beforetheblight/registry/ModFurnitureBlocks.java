package net.beforetheblight.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import net.beforetheblight.BeforeTheBlight;
import net.beforetheblight.block.furniture.ActiveFurnitureBlock;
import net.beforetheblight.block.furniture.ActiveTwoPartFurnitureBlock;
import net.beforetheblight.block.furniture.AbstractTwoPartFurnitureBlock;
import net.beforetheblight.block.furniture.HistoricalStorageBlock;
import net.beforetheblight.block.furniture.LadderBackChairBlock;
import net.beforetheblight.block.furniture.OpenFurnitureBlock;
import net.beforetheblight.block.furniture.OpenTwoPartFurnitureBlock;
import net.beforetheblight.block.furniture.PegRailBlock;
import net.beforetheblight.block.furniture.RoughThreeLeggedStoolBlock;
import net.beforetheblight.block.furniture.RoughWallShelfBlock;
import net.beforetheblight.block.furniture.SimpleHistoricalFurnitureBlock;
import net.beforetheblight.block.furniture.SixBoardChestBlock;
import net.beforetheblight.block.furniture.SmallWorkTableBlock;
import net.beforetheblight.block.furniture.StaticFurnitureSeatEntity;
import net.beforetheblight.block.furniture.TwoPartFurnitureBlock;
import net.beforetheblight.block.furniture.TwoPartFurnitureItem;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityType;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

/**
 * Isolated registry surface for the historical furniture polish slice.
 *
 * <p>The category lists are item lists so creative-tab integration can consume
 * them directly without reaching into the block registry. Fixtures live in
 * {@link #BUILDING}; movable room furniture lives in {@link #FURNITURE}; and
 * {@link #ALL_ITEMS} preserves that deterministic display order.</p>
 */
public final class ModFurnitureBlocks {
	private static boolean initialized;

	public static final EntityType<StaticFurnitureSeatEntity> STATIC_SEAT =
		registerSeatEntity();
	/** Compatibility alias for integration code written against the long name. */
	public static final EntityType<StaticFurnitureSeatEntity> STATIC_FURNITURE_SEAT =
		STATIC_SEAT;

	public static final LadderBackChairBlock LADDER_BACK_CHAIR = register(
		"ladder_back_chair",
		LadderBackChairBlock::new,
		furnitureProperties(1.5F)
	);

	public static final RoughThreeLeggedStoolBlock ROUGH_THREE_LEGGED_STOOL = register(
		"rough_three_legged_stool",
		RoughThreeLeggedStoolBlock::new,
		furnitureProperties(1.2F)
	);

	public static final SmallWorkTableBlock SMALL_WORK_TABLE = register(
		"small_work_table",
		SmallWorkTableBlock::new,
		furnitureProperties(2.0F)
	);

	public static final RoughWallShelfBlock ROUGH_WALL_SHELF = register(
		"rough_wall_shelf",
		RoughWallShelfBlock::new,
		furnitureProperties(1.2F)
	);

	public static final PegRailBlock PEG_RAIL = register(
		"peg_rail",
		PegRailBlock::new,
		furnitureProperties(0.8F)
	);

	public static final SixBoardChestBlock SIX_BOARD_CHEST = register(
		"six_board_chest",
		SixBoardChestBlock::new,
		storageProperties(2.0F)
	);

	public static final TwoPartFurnitureBlock BACKLESS_BENCH = register(
		"backless_bench",
		properties -> new TwoPartFurnitureBlock(
			TwoPartFurnitureBlock.Style.BACKLESS_BENCH,
			properties
		),
		multiblockFurnitureProperties(1.5F)
	);

	public static final SimpleHistoricalFurnitureBlock SLAB_BENCH = register(
		"slab_bench",
		properties -> new SimpleHistoricalFurnitureBlock(
			SimpleHistoricalFurnitureBlock.Style.SLAB_BENCH,
			properties
		),
		furnitureProperties(1.5F)
	);

	public static final TwoPartFurnitureBlock WALL_BENCH = register(
		"wall_bench",
		properties -> new TwoPartFurnitureBlock(
			TwoPartFurnitureBlock.Style.WALL_BENCH,
			properties
		),
		multiblockFurnitureProperties(1.5F)
	);

	public static final TwoPartFurnitureBlock HIGH_BACK_SETTLE = register(
		"high_back_settle",
		properties -> new TwoPartFurnitureBlock(
			TwoPartFurnitureBlock.Style.HIGH_BACK_SETTLE,
			properties
		),
		multiblockFurnitureProperties(2.0F)
	);

	public static final TwoPartFurnitureBlock TRESTLE_TABLE = register(
		"trestle_table",
		properties -> new TwoPartFurnitureBlock(
			TwoPartFurnitureBlock.Style.TRESTLE_TABLE,
			properties
		),
		multiblockFurnitureProperties(2.0F)
	);

	public static final TwoPartFurnitureBlock FARMHOUSE_TABLE = register(
		"farmhouse_table",
		properties -> new TwoPartFurnitureBlock(
			TwoPartFurnitureBlock.Style.FARMHOUSE_TABLE,
			properties
		),
		multiblockFurnitureProperties(2.0F)
	);

	public static final OpenFurnitureBlock DROP_LEAF_TABLE = register(
		"drop_leaf_table",
		OpenFurnitureBlock::new,
		furnitureProperties(1.8F)
	);

	public static final TwoPartFurnitureBlock ROUGH_WORKBENCH = register(
		"rough_workbench",
		properties -> new TwoPartFurnitureBlock(
			TwoPartFurnitureBlock.Style.ROUGH_WORKBENCH,
			properties
		),
		multiblockFurnitureProperties(2.5F)
	);

	public static final BedBlock ROPE_BED = registerBed("rope_bed");

	public static final OpenTwoPartFurnitureBlock TRUNDLE_BED = register(
		"trundle_bed",
		OpenTwoPartFurnitureBlock::new,
		multiblockFurnitureProperties(1.5F)
	);

	public static final SimpleHistoricalFurnitureBlock WOODEN_CRADLE = register(
		"wooden_cradle",
		properties -> new SimpleHistoricalFurnitureBlock(
			SimpleHistoricalFurnitureBlock.Style.WOODEN_CRADLE,
			properties
		),
		furnitureProperties(1.2F)
	);

	public static final HistoricalStorageBlock BLANKET_CHEST = register(
		"blanket_chest",
		properties -> new HistoricalStorageBlock(
			HistoricalStorageBlock.Style.BLANKET_CHEST,
			properties
		),
		storageProperties(2.0F)
	);

	public static final HistoricalStorageBlock WALL_CUPBOARD = register(
		"wall_cupboard",
		properties -> new HistoricalStorageBlock(
			HistoricalStorageBlock.Style.WALL_CUPBOARD,
			properties
		),
		storageProperties(2.0F)
	);

	public static final HistoricalStorageBlock CORNER_CUPBOARD = register(
		"corner_cupboard",
		properties -> new HistoricalStorageBlock(
			HistoricalStorageBlock.Style.CORNER_CUPBOARD,
			properties
		),
		storageProperties(2.0F)
	);

	public static final HistoricalStorageBlock PIE_SAFE = register(
		"pie_safe",
		properties -> new HistoricalStorageBlock(
			HistoricalStorageBlock.Style.PIE_SAFE,
			properties
		),
		storageProperties(2.0F)
	);

	public static final HistoricalStorageBlock DRAWER_CHEST = register(
		"drawer_chest",
		properties -> new HistoricalStorageBlock(
			HistoricalStorageBlock.Style.DRAWER_CHEST,
			properties
		),
		storageProperties(2.0F)
	);

	public static final ActiveFurnitureBlock SPINNING_WHEEL = register(
		"spinning_wheel",
		properties -> new ActiveFurnitureBlock(
			ActiveFurnitureBlock.Style.SPINNING_WHEEL,
			properties
		),
		furnitureProperties(1.5F)
	);

	public static final ActiveTwoPartFurnitureBlock FLOOR_LOOM = register(
		"floor_loom",
		ActiveTwoPartFurnitureBlock::new,
		multiblockFurnitureProperties(2.0F)
	);

	public static final ActiveFurnitureBlock TABLE_LOOM = register(
		"table_loom",
		properties -> new ActiveFurnitureBlock(
			ActiveFurnitureBlock.Style.TABLE_LOOM,
			properties
		),
		furnitureProperties(1.5F)
	);

	public static final ActiveFurnitureBlock QUILL_WHEEL = register(
		"quill_wheel",
		properties -> new ActiveFurnitureBlock(
			ActiveFurnitureBlock.Style.QUILL_WHEEL,
			properties
		),
		furnitureProperties(1.5F)
	);

	public static final ActiveFurnitureBlock YARN_WINDER = register(
		"yarn_winder",
		properties -> new ActiveFurnitureBlock(
			ActiveFurnitureBlock.Style.YARN_WINDER,
			properties
		),
		furnitureProperties(1.2F)
	);

	public static final SimpleHistoricalFurnitureBlock MANTEL_SHELF = register(
		"mantel_shelf",
		properties -> new SimpleHistoricalFurnitureBlock(
			SimpleHistoricalFurnitureBlock.Style.MANTEL_SHELF,
			properties
		),
		furnitureProperties(1.2F)
	);

	public static final SimpleHistoricalFurnitureBlock GUN_RACK = register(
		"gun_rack",
		properties -> new SimpleHistoricalFurnitureBlock(
			SimpleHistoricalFurnitureBlock.Style.GUN_RACK,
			properties
		),
		furnitureProperties(1.2F)
	);

	public static final SimpleHistoricalFurnitureBlock BROOM_RACK = register(
		"broom_rack",
		properties -> new SimpleHistoricalFurnitureBlock(
			SimpleHistoricalFurnitureBlock.Style.BROOM_RACK,
			properties
		),
		furnitureProperties(1.0F)
	);

	public static final LadderBlock ROUGH_BOARD_LADDER = register(
		"rough_board_ladder",
		LadderBlock::new,
		BlockBehaviour.Properties.ofFullCopy(Blocks.LADDER)
	);

	public static final SimpleHistoricalFurnitureBlock CRADLE_SHELF = register(
		"cradle_shelf",
		properties -> new SimpleHistoricalFurnitureBlock(
			SimpleHistoricalFurnitureBlock.Style.CRADLE_SHELF,
			properties
		),
		furnitureProperties(1.2F)
	);

	public static final TwoPartFurnitureBlock SHAVING_HORSE = register(
		"shaving_horse",
		properties -> new TwoPartFurnitureBlock(
			TwoPartFurnitureBlock.Style.SHAVING_HORSE,
			properties
		),
		multiblockFurnitureProperties(2.0F)
	);

	/** Wall-mounted building fixtures, in intended creative-menu order. */
	public static final List<Item> BUILDING = List.of(
		ROUGH_WALL_SHELF.asItem(),
		PEG_RAIL.asItem(),
		WALL_BENCH.asItem(),
		MANTEL_SHELF.asItem(),
		GUN_RACK.asItem(),
		BROOM_RACK.asItem(),
		ROUGH_BOARD_LADDER.asItem(),
		CRADLE_SHELF.asItem()
	);

	/** Free-standing furniture, in intended creative-menu order. */
	public static final List<Item> FURNITURE = List.of(
		LADDER_BACK_CHAIR.asItem(),
		ROUGH_THREE_LEGGED_STOOL.asItem(),
		SMALL_WORK_TABLE.asItem(),
		SIX_BOARD_CHEST.asItem(),
		BACKLESS_BENCH.asItem(),
		SLAB_BENCH.asItem(),
		HIGH_BACK_SETTLE.asItem(),
		TRESTLE_TABLE.asItem(),
		FARMHOUSE_TABLE.asItem(),
		DROP_LEAF_TABLE.asItem(),
		ROUGH_WORKBENCH.asItem(),
		ROPE_BED.asItem(),
		TRUNDLE_BED.asItem(),
		WOODEN_CRADLE.asItem(),
		BLANKET_CHEST.asItem(),
		WALL_CUPBOARD.asItem(),
		CORNER_CUPBOARD.asItem(),
		PIE_SAFE.asItem(),
		DRAWER_CHEST.asItem(),
		SPINNING_WHEEL.asItem(),
		FLOOR_LOOM.asItem(),
		TABLE_LOOM.asItem(),
		QUILL_WHEEL.asItem(),
		YARN_WINDER.asItem(),
		SHAVING_HORSE.asItem()
	);

	/** Every obtainable item owned by this module, with no duplicates. */
	public static final List<Item> ALL_ITEMS = concatenate(BUILDING, FURNITURE);

	/** Functional barrel-backed storage blocks owned by the furniture module. */
	public static final List<Block> FUNCTIONAL_STORAGE = List.of(
		SIX_BOARD_CHEST,
		BLANKET_CHEST,
		WALL_CUPBOARD,
		CORNER_CUPBOARD,
		PIE_SAFE,
		DRAWER_CHEST
	);

	/** Static-toggle textile props; none currently transforms recipes or items. */
	public static final List<Block> DECORATIVE_TOGGLE_EQUIPMENT = List.of(
		SPINNING_WHEEL,
		FLOOR_LOOM,
		TABLE_LOOM,
		QUILL_WHEEL,
		YARN_WINDER
	);

	/** Translation handoff map for resource/datagen integrations. */
	public static final Map<String, String> ENGLISH_NAMES = Map.ofEntries(
		Map.entry("ladder_back_chair", "Ladder-Back Chair"),
		Map.entry("rough_three_legged_stool", "Rough Three-Legged Stool"),
		Map.entry("small_work_table", "Small Work Table"),
		Map.entry("rough_wall_shelf", "Rough Wall Shelf"),
		Map.entry("peg_rail", "Peg Rail"),
		Map.entry("six_board_chest", "Six-Board Chest"),
		Map.entry("backless_bench", "Backless Bench"),
		Map.entry("slab_bench", "Slab Bench"),
		Map.entry("wall_bench", "Wall Bench"),
		Map.entry("high_back_settle", "High-Back Settle"),
		Map.entry("trestle_table", "Trestle Table"),
		Map.entry("farmhouse_table", "Farmhouse Table"),
		Map.entry("drop_leaf_table", "Drop-Leaf Table"),
		Map.entry("rough_workbench", "Rough Workbench"),
		Map.entry("rope_bed", "Rope Bed"),
		Map.entry("trundle_bed", "Trundle Bed"),
		Map.entry("wooden_cradle", "Wooden Cradle"),
		Map.entry("blanket_chest", "Blanket Chest"),
		Map.entry("wall_cupboard", "Wall Cupboard"),
		Map.entry("corner_cupboard", "Corner Cupboard"),
		Map.entry("pie_safe", "Pie Safe"),
		Map.entry("drawer_chest", "Drawer Chest"),
		Map.entry("spinning_wheel", "Spinning Wheel"),
		Map.entry("floor_loom", "Floor Loom"),
		Map.entry("table_loom", "Table Loom"),
		Map.entry("quill_wheel", "Quill Wheel"),
		Map.entry("yarn_winder", "Yarn Winder"),
		Map.entry("mantel_shelf", "Mantel Shelf"),
		Map.entry("gun_rack", "Gun Rack"),
		Map.entry("broom_rack", "Broom Rack"),
		Map.entry("rough_board_ladder", "Rough Board Ladder"),
		Map.entry("cradle_shelf", "Cradle Shelf"),
		Map.entry("shaving_horse", "Shaving Horse")
	);

	public static final Map<String, String> CONTAINER_ENGLISH_NAMES = Map.ofEntries(
		Map.entry(
			"container.before_the_blight.six_board_chest",
			"Six-Board Chest"
		),
		Map.entry(
			"container.before_the_blight.blanket_chest",
			"Blanket Chest"
		),
		Map.entry(
			"container.before_the_blight.wall_cupboard",
			"Wall Cupboard"
		),
		Map.entry(
			"container.before_the_blight.corner_cupboard",
			"Corner Cupboard"
		),
		Map.entry("container.before_the_blight.pie_safe", "Pie Safe"),
		Map.entry(
			"container.before_the_blight.drawer_chest",
			"Drawer Chest"
		)
	);

	/**
	 * Explicit integration contract: this is a functional, persistent 27-slot
	 * container backed by vanilla's barrel block entity.
	 */
	public static final boolean SIX_BOARD_CHEST_HAS_INVENTORY = true;
	public static final boolean ROPE_BED_SUPPORTS_SLEEP = true;
	public static final boolean BENCHES_SUPPORT_SITTING = false;
	public static final boolean TRUNDLE_BED_SUPPORTS_SLEEP = false;
	public static final boolean WOODEN_CRADLE_SUPPORTS_SLEEP = false;
	public static final boolean TEXTILE_EQUIPMENT_PRODUCES_ITEMS = false;
	public static final boolean SHAVING_HORSE_SUPPORTS_PROCESSING = false;

	private ModFurnitureBlocks() {
	}

	private static BlockBehaviour.Properties furnitureProperties(float hardness) {
		return BlockBehaviour.Properties.of()
			.mapColor(MapColor.WOOD)
			.instrument(NoteBlockInstrument.BASS)
			.strength(hardness, 3.0F)
			.sound(SoundType.WOOD)
			.noOcclusion()
			.ignitedByLava();
	}

	private static BlockBehaviour.Properties multiblockFurnitureProperties(
		float hardness
	) {
		return furnitureProperties(hardness)
			.pushReaction(PushReaction.BLOCK);
	}

	private static BlockBehaviour.Properties storageProperties(float hardness) {
		return furnitureProperties(hardness)
			.pushReaction(PushReaction.BLOCK);
	}

	private static EntityType<StaticFurnitureSeatEntity> registerSeatEntity() {
		ResourceKey<EntityType<?>> key = ResourceKey.create(
			Registries.ENTITY_TYPE,
			BeforeTheBlight.id("static_furniture_seat")
		);
		EntityType<StaticFurnitureSeatEntity> type = EntityType.Builder
			.<StaticFurnitureSeatEntity>of(
				StaticFurnitureSeatEntity::new,
				MobCategory.MISC
			)
			.sized(0.01F, 0.01F)
			.clientTrackingRange(8)
			.updateInterval(1)
			.noSummon()
			.noLootTable()
			.build(key);
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, type);
	}

	private static <T extends Block> T register(
		String name,
		Function<BlockBehaviour.Properties, T> blockFactory,
		BlockBehaviour.Properties properties
	) {
		ResourceKey<Block> blockKey = ResourceKey.create(
			Registries.BLOCK,
			BeforeTheBlight.id(name)
		);
		T block = blockFactory.apply(properties.setId(blockKey));

		ResourceKey<Item> itemKey = ResourceKey.create(
			Registries.ITEM,
			BeforeTheBlight.id(name)
		);
		Item.Properties itemProperties = new Item.Properties()
			.setId(itemKey)
			.useBlockDescriptionPrefix();
		BlockItem blockItem = block instanceof AbstractTwoPartFurnitureBlock
			? new TwoPartFurnitureItem(block, itemProperties)
			: new BlockItem(block, itemProperties);
		blockItem.registerBlocks(Item.BY_BLOCK, blockItem);
		Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);
		return Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
	}

	private static BedBlock registerBed(String name) {
		ResourceKey<Block> blockKey = ResourceKey.create(
			Registries.BLOCK,
			BeforeTheBlight.id(name)
		);
		BedBlock block = new BedBlock(
			DyeColor.WHITE,
			BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_BED)
				.setId(blockKey)
		);

		ResourceKey<Item> itemKey = ResourceKey.create(
			Registries.ITEM,
			BeforeTheBlight.id(name)
		);
		BedItem bedItem = new BedItem(
			block,
			new Item.Properties()
				.setId(itemKey)
				.stacksTo(1)
				.useBlockDescriptionPrefix()
		);
		bedItem.registerBlocks(Item.BY_BLOCK, bedItem);
		Registry.register(BuiltInRegistries.ITEM, itemKey, bedItem);
		return Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
	}

	private static List<Item> concatenate(List<Item> first, List<Item> second) {
		List<Item> combined = new ArrayList<>(first.size() + second.size());
		combined.addAll(first);
		combined.addAll(second);
		return List.copyOf(combined);
	}

	public static synchronized void initialize() {
		if (initialized) {
			return;
		}

		FabricBlockEntityType barrelType =
			(FabricBlockEntityType)(Object)BlockEntityType.BARREL;
		for (Block storage : FUNCTIONAL_STORAGE) {
			barrelType.addValidBlock(storage);
		}
		((FabricBlockEntityType)(Object)BlockEntityType.BED)
			.addValidBlock(ROPE_BED);

		ModContentCatalog.register(
			ModContentCatalog.Category.BUILDING_MATERIALS,
			BUILDING.toArray(Item[]::new)
		);
		ModContentCatalog.register(
			ModContentCatalog.Category.FURNITURE_DECOR,
			FURNITURE.toArray(Item[]::new)
		);

		FlammableBlockRegistry flammable = FlammableBlockRegistry.getDefaultInstance();
		for (Item item : ALL_ITEMS) {
			Block block = Block.byItem(item);
			if (block != Blocks.AIR) {
				flammable.add(block, 5, 20);
			}
		}
		initialized = true;
	}
}
