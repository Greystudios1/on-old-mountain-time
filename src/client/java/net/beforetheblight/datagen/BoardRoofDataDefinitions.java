package net.beforetheblight.datagen;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import net.beforetheblight.registry.ModBoardRoofBlocks;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider.TranslationBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

/**
 * Isolated datagen bridge for board-and-roof content.
 *
 * <p>Recipes, loot tables, and the module-owned public tags are emitted by
 * {@code tools/generate_board_roof_polish_data.py}. The shared language and
 * vanilla-tag providers can consume these definitions without repeating a
 * thirty-six-block registry list.</p>
 */
public final class BoardRoofDataDefinitions {
	public static final Map<Block, String> ENGLISH_NAMES = Map.ofEntries(
		Map.entry(ModBoardRoofBlocks.WHITE_OAK_SHINGLES, "White Oak Split Shingles"),
		Map.entry(ModBoardRoofBlocks.WHITE_OAK_SHINGLE_STAIRS, "White Oak Shingle Stairs"),
		Map.entry(ModBoardRoofBlocks.WHITE_OAK_SHINGLE_SLAB, "White Oak Shingle Slab"),
		Map.entry(
			ModBoardRoofBlocks.GRAY_WEATHERED_WHITE_OAK_SHINGLES,
			"Gray-Weathered White Oak Shingles"
		),
		Map.entry(
			ModBoardRoofBlocks.GRAY_WEATHERED_WHITE_OAK_SHINGLE_STAIRS,
			"Gray-Weathered White Oak Shingle Stairs"
		),
		Map.entry(
			ModBoardRoofBlocks.GRAY_WEATHERED_WHITE_OAK_SHINGLE_SLAB,
			"Gray-Weathered White Oak Shingle Slab"
		),
		Map.entry(ModBoardRoofBlocks.DAMP_WHITE_OAK_SHINGLES, "Damp White Oak Shingles"),
		Map.entry(ModBoardRoofBlocks.DAMP_WHITE_OAK_SHINGLE_STAIRS, "Damp White Oak Shingle Stairs"),
		Map.entry(ModBoardRoofBlocks.DAMP_WHITE_OAK_SHINGLE_SLAB, "Damp White Oak Shingle Slab"),
		Map.entry(ModBoardRoofBlocks.MOSSY_WHITE_OAK_SHINGLES, "Mossy White Oak Shingles"),
		Map.entry(ModBoardRoofBlocks.MOSSY_WHITE_OAK_SHINGLE_STAIRS, "Mossy White Oak Shingle Stairs"),
		Map.entry(ModBoardRoofBlocks.MOSSY_WHITE_OAK_SHINGLE_SLAB, "Mossy White Oak Shingle Slab"),
		Map.entry(
			ModBoardRoofBlocks.SOOT_DARKENED_WHITE_OAK_SHINGLES,
			"Soot-Darkened White Oak Shingles"
		),
		Map.entry(
			ModBoardRoofBlocks.SOOT_DARKENED_WHITE_OAK_SHINGLE_STAIRS,
			"Soot-Darkened White Oak Shingle Stairs"
		),
		Map.entry(
			ModBoardRoofBlocks.SOOT_DARKENED_WHITE_OAK_SHINGLE_SLAB,
			"Soot-Darkened White Oak Shingle Slab"
		),
		Map.entry(
			ModBoardRoofBlocks.RANDOM_WIDTH_CHESTNUT_BOARDWORK,
			"Random-Width Chestnut Boardwork"
		),
		Map.entry(
			ModBoardRoofBlocks.RANDOM_WIDTH_POPLAR_BOARDWORK,
			"Random-Width Poplar Boardwork"
		),
		Map.entry(ModBoardRoofBlocks.BROAD_CHESTNUT_PUNCHEON_FLOOR, "Broad Chestnut Puncheon Floor"),
		Map.entry(ModBoardRoofBlocks.BROAD_CHESTNUT_PUNCHEON_STAIRS, "Broad Chestnut Puncheon Stairs"),
		Map.entry(ModBoardRoofBlocks.BROAD_CHESTNUT_PUNCHEON_SLAB, "Broad Chestnut Puncheon Slab"),
		Map.entry(ModBoardRoofBlocks.BROAD_POPLAR_PUNCHEON_FLOOR, "Broad Poplar Puncheon Floor"),
		Map.entry(ModBoardRoofBlocks.BROAD_POPLAR_PUNCHEON_STAIRS, "Broad Poplar Puncheon Stairs"),
		Map.entry(ModBoardRoofBlocks.BROAD_POPLAR_PUNCHEON_SLAB, "Broad Poplar Puncheon Slab"),
		Map.entry(
			ModBoardRoofBlocks.CHESTNUT_BOARD_AND_BATTEN_SIDING,
			"Chestnut Board-and-Batten Siding"
		),
		Map.entry(
			ModBoardRoofBlocks.POPLAR_BOARD_AND_BATTEN_SIDING,
			"Poplar Board-and-Batten Siding"
		),
		Map.entry(ModBoardRoofBlocks.WHITE_OAK_SHINGLE_RIDGE_CAP, "White Oak Shingle Ridge Cap"),
		Map.entry(ModBoardRoofBlocks.WHITE_OAK_SHINGLE_RIDGE_END, "White Oak Shingle Ridge End"),
		Map.entry(ModBoardRoofBlocks.WHITE_OAK_SHINGLE_RAKE_LEFT, "White Oak Shingle Left Rake"),
		Map.entry(ModBoardRoofBlocks.WHITE_OAK_SHINGLE_RAKE_RIGHT, "White Oak Shingle Right Rake"),
		Map.entry(ModBoardRoofBlocks.WHITE_OAK_SHINGLE_LOWER_EAVE, "White Oak Shingle Lower Eave"),
		Map.entry(
			ModBoardRoofBlocks.WHITE_OAK_SHINGLE_CHIMNEY_TRANSITION,
			"White Oak Shingle Chimney Transition"
		),
		Map.entry(ModBoardRoofBlocks.EXPOSED_ROOF_BOARD, "Exposed Roof Board"),
		Map.entry(ModBoardRoofBlocks.EXPOSED_RAFTER_TAIL, "Exposed Rafter Tail"),
		Map.entry(ModBoardRoofBlocks.LOFT_HATCH, "Rough Loft Hatch"),
		Map.entry(ModBoardRoofBlocks.ROUGH_BOARD_STACK, "Stack of Rough Boards"),
		Map.entry(ModBoardRoofBlocks.LOOSE_BOARD, "Loose Rough Board")
	);

	public static final List<Block> SLAB_LOOT = ModBoardRoofBlocks.SLAB_BLOCKS;

	public static final List<Block> SIMPLE_LOOT = ModBoardRoofBlocks.ALL_BLOCKS.stream()
		.filter(block -> !SLAB_LOOT.contains(block))
		.toList();

	public static final Map<Block, String> ACQUISITION_GUIDES = Map.ofEntries(
		Map.entry(
			ModBoardRoofBlocks.WHITE_OAK_SHINGLES,
			"Craft one White Oak Board into four hand-split roof shingles."
		),
		Map.entry(
			ModBoardRoofBlocks.WHITE_OAK_SHINGLE_STAIRS,
			"Craft six White Oak Split Shingles in a stair pattern to receive four roof slopes."
		),
		Map.entry(
			ModBoardRoofBlocks.WHITE_OAK_SHINGLE_SLAB,
			"Craft three White Oak Split Shingles in a row to receive six half courses."
		),
		Map.entry(
			ModBoardRoofBlocks.GRAY_WEATHERED_WHITE_OAK_SHINGLES,
			"Combine White Oak Split Shingles with gray dye for a sun-weathered building variant."
		),
		Map.entry(
			ModBoardRoofBlocks.GRAY_WEATHERED_WHITE_OAK_SHINGLE_STAIRS,
			"Craft the gray-weathered shingle blocks in a stair pattern."
		),
		Map.entry(
			ModBoardRoofBlocks.GRAY_WEATHERED_WHITE_OAK_SHINGLE_SLAB,
			"Craft three gray-weathered shingle blocks in a row."
		),
		Map.entry(
			ModBoardRoofBlocks.DAMP_WHITE_OAK_SHINGLES,
			"Combine White Oak Split Shingles with a water bucket for damp shaded roofing."
		),
		Map.entry(
			ModBoardRoofBlocks.DAMP_WHITE_OAK_SHINGLE_STAIRS,
			"Craft the damp shingle blocks in a stair pattern."
		),
		Map.entry(
			ModBoardRoofBlocks.DAMP_WHITE_OAK_SHINGLE_SLAB,
			"Craft three damp shingle blocks in a row."
		),
		Map.entry(
			ModBoardRoofBlocks.MOSSY_WHITE_OAK_SHINGLES,
			"Combine White Oak Split Shingles with a moss block for shaded, moss-grown roofing."
		),
		Map.entry(
			ModBoardRoofBlocks.MOSSY_WHITE_OAK_SHINGLE_STAIRS,
			"Craft the mossy shingle blocks in a stair pattern."
		),
		Map.entry(
			ModBoardRoofBlocks.MOSSY_WHITE_OAK_SHINGLE_SLAB,
			"Craft three mossy shingle blocks in a row."
		),
		Map.entry(
			ModBoardRoofBlocks.SOOT_DARKENED_WHITE_OAK_SHINGLES,
			"Combine White Oak Split Shingles with charcoal for chimney-adjacent soot darkening."
		),
		Map.entry(
			ModBoardRoofBlocks.SOOT_DARKENED_WHITE_OAK_SHINGLE_STAIRS,
			"Craft the soot-darkened shingle blocks in a stair pattern."
		),
		Map.entry(
			ModBoardRoofBlocks.SOOT_DARKENED_WHITE_OAK_SHINGLE_SLAB,
			"Craft three soot-darkened shingle blocks in a row."
		),
		Map.entry(
			ModBoardRoofBlocks.RANDOM_WIDTH_CHESTNUT_BOARDWORK,
			"Craft four Rough Chestnut Boards in a square; place on any axis to align the boards."
		),
		Map.entry(
			ModBoardRoofBlocks.RANDOM_WIDTH_POPLAR_BOARDWORK,
			"Craft four Hand-Split Yellow-Poplar Boards in a square; place on any axis."
		),
		Map.entry(
			ModBoardRoofBlocks.BROAD_CHESTNUT_PUNCHEON_FLOOR,
			"Craft two Rough Chestnut Boards in a row to receive two broad puncheons."
		),
		Map.entry(
			ModBoardRoofBlocks.BROAD_CHESTNUT_PUNCHEON_STAIRS,
			"Craft broad chestnut puncheons in a stair pattern."
		),
		Map.entry(
			ModBoardRoofBlocks.BROAD_CHESTNUT_PUNCHEON_SLAB,
			"Craft three broad chestnut puncheons in a row."
		),
		Map.entry(
			ModBoardRoofBlocks.BROAD_POPLAR_PUNCHEON_FLOOR,
			"Craft two Hand-Split Yellow-Poplar Boards in a row to receive two broad puncheons."
		),
		Map.entry(
			ModBoardRoofBlocks.BROAD_POPLAR_PUNCHEON_STAIRS,
			"Craft broad poplar puncheons in a stair pattern."
		),
		Map.entry(
			ModBoardRoofBlocks.BROAD_POPLAR_PUNCHEON_SLAB,
			"Craft three broad poplar puncheons in a row."
		),
		Map.entry(
			ModBoardRoofBlocks.CHESTNUT_BOARD_AND_BATTEN_SIDING,
			"Craft Rough Chestnut Boards around sticks in two B-S-B rows; placement chooses the exterior."
		),
		Map.entry(
			ModBoardRoofBlocks.POPLAR_BOARD_AND_BATTEN_SIDING,
			"Craft Hand-Split Yellow-Poplar Boards around sticks in two B-S-B rows."
		),
		Map.entry(
			ModBoardRoofBlocks.WHITE_OAK_SHINGLE_RIDGE_CAP,
			"Craft three White Oak Split Shingles in a row to receive six ridge caps."
		),
		Map.entry(
			ModBoardRoofBlocks.WHITE_OAK_SHINGLE_RIDGE_END,
			"Craft two White Oak Split Shingles diagonally for a finished ridge end."
		),
		Map.entry(
			ModBoardRoofBlocks.WHITE_OAK_SHINGLE_RAKE_LEFT,
			"Craft three White Oak Split Shingles in a left-handed L pattern."
		),
		Map.entry(
			ModBoardRoofBlocks.WHITE_OAK_SHINGLE_RAKE_RIGHT,
			"Craft three White Oak Split Shingles in a right-handed L pattern."
		),
		Map.entry(
			ModBoardRoofBlocks.WHITE_OAK_SHINGLE_LOWER_EAVE,
			"Craft five White Oak Split Shingles in a broad U for a projecting lower eave."
		),
		Map.entry(
			ModBoardRoofBlocks.WHITE_OAK_SHINGLE_CHIMNEY_TRANSITION,
			"Craft five White Oak Split Shingles around clay for a fitted chimney transition."
		),
		Map.entry(
			ModBoardRoofBlocks.EXPOSED_ROOF_BOARD,
			"Craft three Rough Chestnut Boards over one stick; place on any axis beneath shingles."
		),
		Map.entry(
			ModBoardRoofBlocks.EXPOSED_RAFTER_TAIL,
			"Craft two Rough Chestnut Boards vertically; place on any axis beneath an open eave."
		),
		Map.entry(
			ModBoardRoofBlocks.LOFT_HATCH,
			"Craft five Rough Chestnut Boards in a U-shaped hatch pattern to receive two."
		),
		Map.entry(
			ModBoardRoofBlocks.ROUGH_BOARD_STACK,
			"Craft six Rough Chestnut Boards in two full rows for a reusable building-detail stack."
		),
		Map.entry(
			ModBoardRoofBlocks.LOOSE_BOARD,
			"Craft one Rough Chestnut Board into a low loose-board detail."
		)
	);

	private BoardRoofDataDefinitions() {
	}

	public static void addTranslations(TranslationBuilder builder) {
		ENGLISH_NAMES.forEach(builder::add);
		ACQUISITION_GUIDES.forEach((block, text) ->
			builder.add(
				"rei.before_the_blight.guide."
					+ BuiltInRegistries.BLOCK.getKey(block).getPath(),
				text
			)
		);
		builder.add(
			"tag.item.before_the_blight.puncheon_floors",
			"Appalachian Puncheon Floors"
		);
		builder.add(
			"tag.item.before_the_blight.split_shingles",
			"Hand-Split Wooden Shingles"
		);
		builder.add(
			"tag.item.before_the_blight.appalachian_boardwork",
			"Appalachian Boardwork"
		);
		builder.add(
			"tag.item.before_the_blight.roof_trim",
			"Wooden Roof Trim"
		);
	}

	/**
	 * Lets the existing loot provider invoke its protected helper methods.
	 */
	public static void addLoot(
		Consumer<Block> selfDrop,
		Consumer<Block> slabDrop
	) {
		SIMPLE_LOOT.forEach(selfDrop);
		SLAB_LOOT.forEach(slabDrop);
	}
}
