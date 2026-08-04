package net.beforetheblight.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import net.beforetheblight.BeforeTheBlight;
import net.beforetheblight.block.AxisProfileBlock;
import net.beforetheblight.block.BoardClutterBlock;
import net.beforetheblight.block.FacingBoardworkBlock;
import net.beforetheblight.block.RoofTrimBlock;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.ItemLike;

/**
 * Isolated registry slice for historically grounded boardwork and roofing.
 *
 * <p>The public category lists are the only integration surface needed by
 * creative tabs, REI visibility, loot/tag providers, and the asset zoo. The
 * existing chestnut shingle IDs remain untouched for save compatibility; this
 * module adds white-oak roofing as the preferred new-build material.</p>
 */
public final class ModBoardRoofBlocks {
	private static boolean initialized;

	public static final Block WHITE_OAK_SHINGLES = register(
		"white_oak_shingles",
		Block::new,
		woodProperties(),
		true
	);
	public static final Block WHITE_OAK_SHINGLE_STAIRS = register(
		"white_oak_shingle_stairs",
		properties -> new StairBlock(WHITE_OAK_SHINGLES.defaultBlockState(), properties),
		woodProperties(),
		true
	);
	public static final Block WHITE_OAK_SHINGLE_SLAB = register(
		"white_oak_shingle_slab",
		SlabBlock::new,
		woodProperties(),
		true
	);

	public static final Block GRAY_WEATHERED_WHITE_OAK_SHINGLES = register(
		"gray_weathered_white_oak_shingles",
		Block::new,
		woodProperties(),
		true
	);
	public static final Block GRAY_WEATHERED_WHITE_OAK_SHINGLE_STAIRS = register(
		"gray_weathered_white_oak_shingle_stairs",
		properties -> new StairBlock(
			GRAY_WEATHERED_WHITE_OAK_SHINGLES.defaultBlockState(),
			properties
		),
		woodProperties(),
		true
	);
	public static final Block GRAY_WEATHERED_WHITE_OAK_SHINGLE_SLAB = register(
		"gray_weathered_white_oak_shingle_slab",
		SlabBlock::new,
		woodProperties(),
		true
	);

	public static final Block DAMP_WHITE_OAK_SHINGLES = register(
		"damp_white_oak_shingles",
		Block::new,
		woodProperties(),
		true
	);
	public static final Block DAMP_WHITE_OAK_SHINGLE_STAIRS = register(
		"damp_white_oak_shingle_stairs",
		properties -> new StairBlock(DAMP_WHITE_OAK_SHINGLES.defaultBlockState(), properties),
		woodProperties(),
		true
	);
	public static final Block DAMP_WHITE_OAK_SHINGLE_SLAB = register(
		"damp_white_oak_shingle_slab",
		SlabBlock::new,
		woodProperties(),
		true
	);

	public static final Block MOSSY_WHITE_OAK_SHINGLES = register(
		"mossy_white_oak_shingles",
		Block::new,
		woodProperties(),
		true
	);
	public static final Block MOSSY_WHITE_OAK_SHINGLE_STAIRS = register(
		"mossy_white_oak_shingle_stairs",
		properties -> new StairBlock(MOSSY_WHITE_OAK_SHINGLES.defaultBlockState(), properties),
		woodProperties(),
		true
	);
	public static final Block MOSSY_WHITE_OAK_SHINGLE_SLAB = register(
		"mossy_white_oak_shingle_slab",
		SlabBlock::new,
		woodProperties(),
		true
	);

	public static final Block SOOT_DARKENED_WHITE_OAK_SHINGLES = register(
		"soot_darkened_white_oak_shingles",
		Block::new,
		woodProperties(),
		true
	);
	public static final Block SOOT_DARKENED_WHITE_OAK_SHINGLE_STAIRS = register(
		"soot_darkened_white_oak_shingle_stairs",
		properties -> new StairBlock(
			SOOT_DARKENED_WHITE_OAK_SHINGLES.defaultBlockState(),
			properties
		),
		woodProperties(),
		true
	);
	public static final Block SOOT_DARKENED_WHITE_OAK_SHINGLE_SLAB = register(
		"soot_darkened_white_oak_shingle_slab",
		SlabBlock::new,
		woodProperties(),
		true
	);

	public static final Block RANDOM_WIDTH_CHESTNUT_BOARDWORK = register(
		"random_width_chestnut_boardwork",
		RotatedPillarBlock::new,
		woodProperties(),
		true
	);
	public static final Block RANDOM_WIDTH_POPLAR_BOARDWORK = register(
		"random_width_poplar_boardwork",
		RotatedPillarBlock::new,
		woodProperties(),
		true
	);

	public static final Block BROAD_CHESTNUT_PUNCHEON_FLOOR = register(
		"broad_chestnut_puncheon_floor",
		RotatedPillarBlock::new,
		woodProperties(),
		true
	);
	public static final Block BROAD_CHESTNUT_PUNCHEON_STAIRS = register(
		"broad_chestnut_puncheon_stairs",
		properties -> new StairBlock(BROAD_CHESTNUT_PUNCHEON_FLOOR.defaultBlockState(), properties),
		woodProperties(),
		true
	);
	public static final Block BROAD_CHESTNUT_PUNCHEON_SLAB = register(
		"broad_chestnut_puncheon_slab",
		SlabBlock::new,
		woodProperties(),
		true
	);
	public static final Block BROAD_POPLAR_PUNCHEON_FLOOR = register(
		"broad_poplar_puncheon_floor",
		RotatedPillarBlock::new,
		woodProperties(),
		true
	);
	public static final Block BROAD_POPLAR_PUNCHEON_STAIRS = register(
		"broad_poplar_puncheon_stairs",
		properties -> new StairBlock(BROAD_POPLAR_PUNCHEON_FLOOR.defaultBlockState(), properties),
		woodProperties(),
		true
	);
	public static final Block BROAD_POPLAR_PUNCHEON_SLAB = register(
		"broad_poplar_puncheon_slab",
		SlabBlock::new,
		woodProperties(),
		true
	);

	public static final Block CHESTNUT_BOARD_AND_BATTEN_SIDING = register(
		"chestnut_board_and_batten_siding",
		FacingBoardworkBlock::new,
		woodProperties(),
		true
	);
	public static final Block POPLAR_BOARD_AND_BATTEN_SIDING = register(
		"poplar_board_and_batten_siding",
		FacingBoardworkBlock::new,
		woodProperties(),
		true
	);

	public static final Block WHITE_OAK_SHINGLE_RIDGE_CAP = roofTrim(
		"white_oak_shingle_ridge_cap",
		RoofTrimBlock.Profile.RIDGE_CAP
	);
	public static final Block WHITE_OAK_SHINGLE_RIDGE_END = roofTrim(
		"white_oak_shingle_ridge_end",
		RoofTrimBlock.Profile.RIDGE_END
	);
	public static final Block WHITE_OAK_SHINGLE_RAKE_LEFT = roofTrim(
		"white_oak_shingle_rake_left",
		RoofTrimBlock.Profile.RAKE_LEFT
	);
	public static final Block WHITE_OAK_SHINGLE_RAKE_RIGHT = roofTrim(
		"white_oak_shingle_rake_right",
		RoofTrimBlock.Profile.RAKE_RIGHT
	);
	public static final Block WHITE_OAK_SHINGLE_LOWER_EAVE = roofTrim(
		"white_oak_shingle_lower_eave",
		RoofTrimBlock.Profile.LOWER_EAVE
	);
	public static final Block WHITE_OAK_SHINGLE_CHIMNEY_TRANSITION = roofTrim(
		"white_oak_shingle_chimney_transition",
		RoofTrimBlock.Profile.CHIMNEY_TRANSITION
	);

	public static final Block EXPOSED_ROOF_BOARD = register(
		"exposed_roof_board",
		properties -> new AxisProfileBlock(AxisProfileBlock.Profile.ROOF_BOARD, properties),
		woodProperties().noOcclusion(),
		true
	);
	public static final Block EXPOSED_RAFTER_TAIL = register(
		"exposed_rafter_tail",
		properties -> new AxisProfileBlock(AxisProfileBlock.Profile.RAFTER_TAIL, properties),
		woodProperties().noOcclusion(),
		true
	);
	public static final Block LOFT_HATCH = register(
		"loft_hatch",
		properties -> new TrapDoorBlock(BlockSetType.OAK, properties),
		BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_TRAPDOOR),
		true
	);
	public static final Block ROUGH_BOARD_STACK = register(
		"rough_board_stack",
		properties -> new BoardClutterBlock(BoardClutterBlock.Profile.STACK, properties),
		woodProperties().noOcclusion(),
		true
	);
	public static final Block LOOSE_BOARD = register(
		"loose_board",
		properties -> new BoardClutterBlock(BoardClutterBlock.Profile.LOOSE, properties),
		woodProperties().noOcclusion(),
		true
	);

	public static final List<Block> ROOFING_BLOCKS = List.of(
		WHITE_OAK_SHINGLES,
		WHITE_OAK_SHINGLE_STAIRS,
		WHITE_OAK_SHINGLE_SLAB,
		GRAY_WEATHERED_WHITE_OAK_SHINGLES,
		GRAY_WEATHERED_WHITE_OAK_SHINGLE_STAIRS,
		GRAY_WEATHERED_WHITE_OAK_SHINGLE_SLAB,
		DAMP_WHITE_OAK_SHINGLES,
		DAMP_WHITE_OAK_SHINGLE_STAIRS,
		DAMP_WHITE_OAK_SHINGLE_SLAB,
		MOSSY_WHITE_OAK_SHINGLES,
		MOSSY_WHITE_OAK_SHINGLE_STAIRS,
		MOSSY_WHITE_OAK_SHINGLE_SLAB,
		SOOT_DARKENED_WHITE_OAK_SHINGLES,
		SOOT_DARKENED_WHITE_OAK_SHINGLE_STAIRS,
		SOOT_DARKENED_WHITE_OAK_SHINGLE_SLAB,
		WHITE_OAK_SHINGLE_RIDGE_CAP,
		WHITE_OAK_SHINGLE_RIDGE_END,
		WHITE_OAK_SHINGLE_RAKE_LEFT,
		WHITE_OAK_SHINGLE_RAKE_RIGHT,
		WHITE_OAK_SHINGLE_LOWER_EAVE,
		WHITE_OAK_SHINGLE_CHIMNEY_TRANSITION
	);

	public static final List<Block> SHINGLE_MATERIAL_BLOCKS = List.of(
		WHITE_OAK_SHINGLES,
		GRAY_WEATHERED_WHITE_OAK_SHINGLES,
		DAMP_WHITE_OAK_SHINGLES,
		MOSSY_WHITE_OAK_SHINGLES,
		SOOT_DARKENED_WHITE_OAK_SHINGLES
	);

	public static final List<Block> ROOF_TRIM_BLOCKS = List.of(
		WHITE_OAK_SHINGLE_RIDGE_CAP,
		WHITE_OAK_SHINGLE_RIDGE_END,
		WHITE_OAK_SHINGLE_RAKE_LEFT,
		WHITE_OAK_SHINGLE_RAKE_RIGHT,
		WHITE_OAK_SHINGLE_LOWER_EAVE,
		WHITE_OAK_SHINGLE_CHIMNEY_TRANSITION
	);

	public static final List<Block> BOARDWORK_BLOCKS = List.of(
		RANDOM_WIDTH_CHESTNUT_BOARDWORK,
		RANDOM_WIDTH_POPLAR_BOARDWORK,
		BROAD_CHESTNUT_PUNCHEON_FLOOR,
		BROAD_CHESTNUT_PUNCHEON_STAIRS,
		BROAD_CHESTNUT_PUNCHEON_SLAB,
		BROAD_POPLAR_PUNCHEON_FLOOR,
		BROAD_POPLAR_PUNCHEON_STAIRS,
		BROAD_POPLAR_PUNCHEON_SLAB,
		CHESTNUT_BOARD_AND_BATTEN_SIDING,
		POPLAR_BOARD_AND_BATTEN_SIDING
	);

	public static final List<Block> DETAIL_BLOCKS = List.of(
		EXPOSED_ROOF_BOARD,
		EXPOSED_RAFTER_TAIL,
		LOFT_HATCH,
		ROUGH_BOARD_STACK,
		LOOSE_BOARD
	);

	public static final List<Block> STAIR_BLOCKS = List.of(
		WHITE_OAK_SHINGLE_STAIRS,
		GRAY_WEATHERED_WHITE_OAK_SHINGLE_STAIRS,
		DAMP_WHITE_OAK_SHINGLE_STAIRS,
		MOSSY_WHITE_OAK_SHINGLE_STAIRS,
		SOOT_DARKENED_WHITE_OAK_SHINGLE_STAIRS,
		BROAD_CHESTNUT_PUNCHEON_STAIRS,
		BROAD_POPLAR_PUNCHEON_STAIRS
	);

	public static final List<Block> SLAB_BLOCKS = List.of(
		WHITE_OAK_SHINGLE_SLAB,
		GRAY_WEATHERED_WHITE_OAK_SHINGLE_SLAB,
		DAMP_WHITE_OAK_SHINGLE_SLAB,
		MOSSY_WHITE_OAK_SHINGLE_SLAB,
		SOOT_DARKENED_WHITE_OAK_SHINGLE_SLAB,
		BROAD_CHESTNUT_PUNCHEON_SLAB,
		BROAD_POPLAR_PUNCHEON_SLAB
	);

	public static final List<Block> TRAPDOOR_BLOCKS = List.of(LOFT_HATCH);

	public static final List<Block> BUILDING_BLOCKS = joined(
		ROOFING_BLOCKS,
		BOARDWORK_BLOCKS,
		DETAIL_BLOCKS
	);
	public static final List<Block> ALL_BLOCKS = BUILDING_BLOCKS;
	public static final List<ItemLike> ROOFING_ITEMS = asItems(ROOFING_BLOCKS);
	public static final List<ItemLike> BOARDWORK_ITEMS = asItems(BOARDWORK_BLOCKS);
	public static final List<ItemLike> DETAIL_ITEMS = asItems(DETAIL_BLOCKS);
	public static final List<ItemLike> ALL_ITEMS = asItems(ALL_BLOCKS);

	private ModBoardRoofBlocks() {
	}

	private static Block roofTrim(String name, RoofTrimBlock.Profile profile) {
		return register(
			name,
			properties -> new RoofTrimBlock(profile, properties),
			woodProperties().noOcclusion(),
			true
		);
	}

	private static BlockBehaviour.Properties woodProperties() {
		return BlockBehaviour.Properties.of()
			.mapColor(MapColor.WOOD)
			.instrument(NoteBlockInstrument.BASS)
			.strength(2.0F, 3.0F)
			.sound(SoundType.WOOD)
			.ignitedByLava();
	}

	private static <T extends Block> T register(
		String name,
		Function<BlockBehaviour.Properties, T> blockFactory,
		BlockBehaviour.Properties properties,
		boolean registerBlockItem
	) {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, BeforeTheBlight.id(name));
		T block = blockFactory.apply(properties.setId(blockKey));

		if (registerBlockItem) {
			ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, BeforeTheBlight.id(name));
			BlockItem blockItem = new BlockItem(
				block,
				new Item.Properties()
					.setId(itemKey)
					.useBlockDescriptionPrefix()
			);
			Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);
			blockItem.registerBlocks(Item.BY_BLOCK, blockItem);
		}

		return Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
	}

	@SafeVarargs
	private static List<Block> joined(List<Block>... groups) {
		List<Block> blocks = new ArrayList<>();
		for (List<Block> group : groups) {
			blocks.addAll(group);
		}
		return List.copyOf(blocks);
	}

	private static List<ItemLike> asItems(List<Block> blocks) {
		return blocks.stream().map(Block::asItem).map(item -> (ItemLike)item).toList();
	}

	/**
	 * Forces registration and installs ordinary wood flammability.
	 */
	public static synchronized void initialize() {
		if (initialized) {
			return;
		}
		initialized = true;

		FlammableBlockRegistry flammable = FlammableBlockRegistry.getDefaultInstance();
		for (Block block : ALL_BLOCKS) {
			flammable.add(block, 5, 20);
		}
		ModContentCatalog.register(
			ModContentCatalog.Category.BUILDING_MATERIALS,
			ALL_ITEMS.toArray(ItemLike[]::new)
		);
	}
}
