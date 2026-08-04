package net.beforetheblight.registry;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import net.beforetheblight.BeforeTheBlight;
import net.beforetheblight.block.BoardShutterBlock;
import net.beforetheblight.block.MountedHardwareBlock;
import net.beforetheblight.block.OpenFrameWindowBlock;
import net.beforetheblight.block.OpeningTrimBlock;
import net.beforetheblight.block.OpeningTrimPieceBlock;
import net.beforetheblight.block.OperableSashWindowBlock;
import net.beforetheblight.block.PairedBoardShutterBlock;
import net.beforetheblight.block.ShutteredSashWindowBlock;
import net.beforetheblight.block.SmallSashWindowBlock;
import net.fabricmc.fabric.api.object.builder.v1.block.type.BlockSetTypeBuilder;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.MapColor;

/**
 * Isolated registry slice for historically grounded farmstead openings.
 *
 * <p>The main registry calls {@link #initialize()} during integration. Keeping
 * this slice separate lets the structures and creative-tab ordering adopt the
 * complete set atomically instead of exposing half-registered props.</p>
 */
public final class ModDoorWindowBlocks {
	private static boolean initialized;

	private static final BlockSetType ROUGH_CHESTNUT_BOARD_BLOCK_SET_TYPE =
		BlockSetTypeBuilder.copyOf(BlockSetType.OAK)
			.register(BeforeTheBlight.id("rough_chestnut_board"));

	public static final DoorBlock ROUGH_CHESTNUT_BOARD_DOOR = register(
		"rough_chestnut_board_door",
		properties -> new DoorBlock(ROUGH_CHESTNUT_BOARD_BLOCK_SET_TYPE, properties),
		BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_DOOR),
		DoubleHighBlockItem::new
	);

	public static final DoorBlock PEGGED_SPRINGHOUSE_DOOR = register(
		"pegged_springhouse_door",
		properties -> new DoorBlock(ROUGH_CHESTNUT_BOARD_BLOCK_SET_TYPE, properties),
		BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_DOOR),
		DoubleHighBlockItem::new
	);

	public static final TrapDoorBlock CRIB_BOARD_HATCH = register(
		"crib_board_hatch",
		properties -> new TrapDoorBlock(ROUGH_CHESTNUT_BOARD_BLOCK_SET_TYPE, properties),
		BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_TRAPDOOR),
		BlockItem::new
	);

	public static final SmallSashWindowBlock SMALL_SASH_WINDOW = register(
		"small_sash_window",
		SmallSashWindowBlock::new,
		BlockBehaviour.Properties.of()
			.mapColor(MapColor.WOOD)
			.strength(1.0F, 2.0F)
			.sound(SoundType.WOOD)
			.noOcclusion()
			.ignitedByLava(),
		BlockItem::new
	);

	public static final BoardShutterBlock BOARD_SHUTTER = register(
		"board_shutter",
		BoardShutterBlock::new,
		openingProperties(),
		BlockItem::new
	);

	/*
	 * The shared board/roof slice already owns loft_hatch, and the springhouse
	 * slice owns wooden_louver. They deliberately remain single canonical IDs.
	 */
	public static final TrapDoorBlock CELLAR_HATCH = register(
		"cellar_hatch",
		properties -> new TrapDoorBlock(ROUGH_CHESTNUT_BOARD_BLOCK_SET_TYPE, properties),
		BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_TRAPDOOR),
		BlockItem::new
	);

	public static final SmallSashWindowBlock TWO_OVER_TWO_SASH_WINDOW = register(
		"two_over_two_sash_window",
		SmallSashWindowBlock::new,
		openingProperties(),
		BlockItem::new
	);

	public static final SmallSashWindowBlock FOUR_OVER_FOUR_SASH_WINDOW = register(
		"four_over_four_sash_window",
		SmallSashWindowBlock::new,
		openingProperties(),
		BlockItem::new
	);

	public static final SmallSashWindowBlock SIX_OVER_SIX_SASH_WINDOW = register(
		"six_over_six_sash_window",
		SmallSashWindowBlock::new,
		openingProperties(),
		BlockItem::new
	);

	public static final SmallSashWindowBlock FIXED_PANE_WINDOW = register(
		"fixed_pane_window",
		SmallSashWindowBlock::new,
		openingProperties(),
		BlockItem::new
	);

	public static final OperableSashWindowBlock OPERABLE_SASH_WINDOW = register(
		"operable_sash_window",
		OperableSashWindowBlock::new,
		openingProperties(),
		BlockItem::new
	);

	public static final OpenFrameWindowBlock BROKEN_PANE_SASH_WINDOW = register(
		"broken_pane_sash_window",
		OpenFrameWindowBlock::new,
		openingProperties(),
		BlockItem::new
	);

	public static final ShutteredSashWindowBlock SHUTTERED_SASH_WINDOW = register(
		"shuttered_sash_window",
		ShutteredSashWindowBlock::new,
		openingProperties(),
		BlockItem::new
	);

	public static final PairedBoardShutterBlock PAIRED_BOARD_SHUTTERS = register(
		"paired_board_shutters",
		PairedBoardShutterBlock::new,
		openingProperties(),
		BlockItem::new
	);

	public static final OpeningTrimBlock ROUGH_CHESTNUT_DOOR_JAMB = register(
		"rough_chestnut_door_jamb",
		properties -> new OpeningTrimBlock(OpeningTrimBlock.Profile.JAMB, properties),
		openingProperties(),
		BlockItem::new
	);

	public static final OpeningTrimBlock HEWN_CHESTNUT_LINTEL = register(
		"hewn_chestnut_lintel",
		properties -> new OpeningTrimBlock(OpeningTrimBlock.Profile.LINTEL, properties),
		openingProperties(),
		BlockItem::new
	);

	public static final OpeningTrimPieceBlock WOODEN_WINDOW_SILL = register(
		"wooden_window_sill",
		properties -> new OpeningTrimPieceBlock(
			OpeningTrimPieceBlock.Profile.SILL,
			properties
		),
		openingProperties(),
		BlockItem::new
	);

	public static final OpeningTrimPieceBlock ROUGH_DOOR_THRESHOLD = register(
		"rough_door_threshold",
		properties -> new OpeningTrimPieceBlock(
			OpeningTrimPieceBlock.Profile.THRESHOLD,
			properties
		),
		openingProperties(),
		BlockItem::new
	);

	public static final OpeningTrimPieceBlock WOODEN_LEVELING_WEDGE = register(
		"wooden_leveling_wedge",
		properties -> new OpeningTrimPieceBlock(
			OpeningTrimPieceBlock.Profile.WEDGE,
			properties
		),
		openingProperties(),
		BlockItem::new
	);

	public static final MountedHardwareBlock INSTALLED_WROUGHT_STRAP_HINGE = hardware(
		"installed_wrought_strap_hinge",
		MountedHardwareBlock.Profile.STRAP_HINGE,
		false
	);

	public static final MountedHardwareBlock INSTALLED_WROUGHT_PINTLE = hardware(
		"installed_wrought_pintle",
		MountedHardwareBlock.Profile.PINTLE,
		false
	);

	public static final MountedHardwareBlock INSTALLED_FORGED_HASP = hardware(
		"installed_forged_hasp",
		MountedHardwareBlock.Profile.HASP,
		false
	);

	public static final MountedHardwareBlock INSTALLED_FORGED_STAPLE = hardware(
		"installed_forged_staple",
		MountedHardwareBlock.Profile.STAPLE,
		false
	);

	public static final MountedHardwareBlock INSTALLED_SQUARE_NAILS = hardware(
		"installed_square_nails",
		MountedHardwareBlock.Profile.SQUARE_NAILS,
		false
	);

	public static final MountedHardwareBlock INSTALLED_CUT_NAILS = hardware(
		"installed_cut_nails",
		MountedHardwareBlock.Profile.CUT_NAILS,
		false
	);

	public static final List<Block> OPENING_BLOCKS = List.of(
		ROUGH_CHESTNUT_BOARD_DOOR,
		PEGGED_SPRINGHOUSE_DOOR,
		CRIB_BOARD_HATCH,
		CELLAR_HATCH,
		SMALL_SASH_WINDOW,
		TWO_OVER_TWO_SASH_WINDOW,
		FOUR_OVER_FOUR_SASH_WINDOW,
		SIX_OVER_SIX_SASH_WINDOW,
		FIXED_PANE_WINDOW,
		OPERABLE_SASH_WINDOW,
		BROKEN_PANE_SASH_WINDOW,
		SHUTTERED_SASH_WINDOW,
		BOARD_SHUTTER,
		PAIRED_BOARD_SHUTTERS,
		ROUGH_CHESTNUT_DOOR_JAMB,
		HEWN_CHESTNUT_LINTEL,
		WOODEN_WINDOW_SILL,
		ROUGH_DOOR_THRESHOLD,
		WOODEN_LEVELING_WEDGE
	);

	public static final List<Block> HARDWARE_BLOCKS = List.of(
		INSTALLED_WROUGHT_STRAP_HINGE,
		INSTALLED_WROUGHT_PINTLE,
		INSTALLED_FORGED_HASP,
		INSTALLED_FORGED_STAPLE,
		INSTALLED_SQUARE_NAILS,
		INSTALLED_CUT_NAILS
	);

	public static final List<Block> DOOR_BLOCKS = List.of(
		ROUGH_CHESTNUT_BOARD_DOOR,
		PEGGED_SPRINGHOUSE_DOOR
	);

	public static final List<Block> HATCH_BLOCKS = List.of(
		CRIB_BOARD_HATCH,
		CELLAR_HATCH
	);

	public static final List<Block> IRON_HARDWARE_BLOCKS = List.of(
		INSTALLED_WROUGHT_STRAP_HINGE,
		INSTALLED_WROUGHT_PINTLE,
		INSTALLED_FORGED_HASP,
		INSTALLED_FORGED_STAPLE,
		INSTALLED_SQUARE_NAILS,
		INSTALLED_CUT_NAILS
	);

	public static final List<Block> WOODEN_BLOCKS = List.of(
		ROUGH_CHESTNUT_BOARD_DOOR,
		PEGGED_SPRINGHOUSE_DOOR,
		CRIB_BOARD_HATCH,
		CELLAR_HATCH,
		SMALL_SASH_WINDOW,
		TWO_OVER_TWO_SASH_WINDOW,
		FOUR_OVER_FOUR_SASH_WINDOW,
		SIX_OVER_SIX_SASH_WINDOW,
		FIXED_PANE_WINDOW,
		OPERABLE_SASH_WINDOW,
		BROKEN_PANE_SASH_WINDOW,
		SHUTTERED_SASH_WINDOW,
		BOARD_SHUTTER,
		PAIRED_BOARD_SHUTTERS,
		ROUGH_CHESTNUT_DOOR_JAMB,
		HEWN_CHESTNUT_LINTEL,
		WOODEN_WINDOW_SILL,
		ROUGH_DOOR_THRESHOLD,
		WOODEN_LEVELING_WEDGE
	);
	public static final List<Block> AXE_MINEABLE_BLOCKS = WOODEN_BLOCKS;
	public static final List<Block> PICKAXE_MINEABLE_BLOCKS = IRON_HARDWARE_BLOCKS;

	public static final List<Block> ALL_BLOCKS = java.util.stream.Stream.concat(
		OPENING_BLOCKS.stream(),
		HARDWARE_BLOCKS.stream()
	).toList();

	public static final List<Item> ALL_ITEMS = ALL_BLOCKS.stream()
		.map(Block::asItem)
		.toList();

	public static final List<Item> BUILDING = OPENING_BLOCKS.stream()
		.map(Block::asItem)
		.toList();
	public static final List<Item> BUILDING_ITEMS = BUILDING;
	public static final List<Item> DECOR = HARDWARE_BLOCKS.stream()
		.map(Block::asItem)
		.toList();
	public static final List<Item> DECOR_ITEMS = DECOR;

	private ModDoorWindowBlocks() {
	}

	private static BlockBehaviour.Properties openingProperties() {
		return BlockBehaviour.Properties.of()
			.mapColor(MapColor.WOOD)
			.strength(1.0F, 2.0F)
			.sound(SoundType.WOOD)
			.noOcclusion()
			.ignitedByLava();
	}

	private static MountedHardwareBlock hardware(
		String name,
		MountedHardwareBlock.Profile profile,
		boolean wooden
	) {
		BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
			.mapColor(wooden ? MapColor.WOOD : MapColor.COLOR_BLACK)
			.strength(wooden ? 0.6F : 1.5F, wooden ? 1.0F : 3.0F)
			.sound(wooden ? SoundType.WOOD : SoundType.METAL)
			.noCollision()
			.noOcclusion();
		if (wooden) {
			properties = properties.ignitedByLava();
		}
		return register(
			name,
			blockProperties -> new MountedHardwareBlock(profile, blockProperties),
			properties,
			BlockItem::new
		);
	}

	private static <T extends Block> T register(
		String name,
		Function<BlockBehaviour.Properties, T> blockFactory,
		BlockBehaviour.Properties properties,
		BiFunction<Block, Item.Properties, ? extends BlockItem> itemFactory
	) {
		ResourceKey<Block> blockKey = ResourceKey.create(
			Registries.BLOCK,
			BeforeTheBlight.id(name)
		);
		T block = blockFactory.apply(properties.setId(blockKey));
		Registry.register(BuiltInRegistries.BLOCK, blockKey, block);

		ResourceKey<Item> itemKey = ResourceKey.create(
			Registries.ITEM,
			BeforeTheBlight.id(name)
		);
		Item.Properties itemProperties = new Item.Properties()
			.setId(itemKey)
			.useBlockDescriptionPrefix()
			.requiredFeatures(block.requiredFeatures());
		BlockItem blockItem = itemFactory.apply(block, itemProperties);
		blockItem.registerBlocks(Item.BY_BLOCK, blockItem);
		Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);
		return block;
	}

	public static synchronized void initialize() {
		if (initialized) {
			return;
		}

		FlammableBlockRegistry flammable = FlammableBlockRegistry.getDefaultInstance();
		for (Block block : WOODEN_BLOCKS) {
			flammable.add(block, 5, 20);
		}
		ModContentCatalog.register(
			ModContentCatalog.Category.BUILDING_MATERIALS,
			BUILDING_ITEMS.toArray(Item[]::new)
		);
		ModContentCatalog.register(
			ModContentCatalog.Category.FURNITURE_DECOR,
			DECOR_ITEMS.toArray(Item[]::new)
		);
		initialized = true;
	}
}
