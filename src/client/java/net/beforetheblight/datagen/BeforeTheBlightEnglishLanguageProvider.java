package net.beforetheblight.datagen;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.beforetheblight.BeforeTheBlight;
import net.beforetheblight.registry.CornCribContentTranslations;
import net.beforetheblight.registry.DoorWindowTranslations;
import net.beforetheblight.registry.ModBlocks;
import net.beforetheblight.registry.ModContentCatalog;
import net.beforetheblight.registry.ModDomesticItems;
import net.beforetheblight.registry.ModDoorWindowBlocks;
import net.beforetheblight.registry.ModExteriorItems;
import net.beforetheblight.registry.ModFurnitureBlocks;
import net.beforetheblight.registry.ModItems;
import net.beforetheblight.registry.ModRegionalWoodBlocks;
import net.beforetheblight.registry.ModSpringhouseBlocks;
import net.beforetheblight.registry.ModStoneHearthBlocks;
import net.beforetheblight.registry.ModTimberBlocks;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;

public final class BeforeTheBlightEnglishLanguageProvider extends FabricLanguageProvider {
	private static final List<String> ISOLATED_LANGUAGE_RESOURCES = List.of(
		"assets/before_the_blight_domestic/lang/en_us.json",
		"assets/before_the_blight_exterior/lang/en_us.json",
		"assets/before_the_blight_springhouse/lang/en_us.json"
	);

	public BeforeTheBlightEnglishLanguageProvider(
		FabricPackOutput output,
		CompletableFuture<HolderLookup.Provider> registriesFuture
	) {
		super(output, "en_us", registriesFuture);
	}

	@Override
	public void generateTranslations(
		HolderLookup.Provider registries,
		TranslationBuilder output
	) {
		Map<String, String> translations = new LinkedHashMap<>();
		TranslationBuilder builder = collectingBuilder(translations);

		builder.add(ModBlocks.CHESTNUT_LOG, "Chestnut Log");
		builder.add(ModBlocks.CHESTNUT_HEWING_LOG, "Partly Hewn Chestnut Log");
		builder.add(ModBlocks.HEWN_CHESTNUT_BEAM, "Hewn Chestnut Beam");
		builder.add(ModBlocks.HEWN_CHESTNUT_WALL, "Hewn Chestnut Wall");
		builder.add(ModBlocks.HEWN_CHESTNUT_POST, "Hewn Chestnut Post");
		builder.add(ModBlocks.OAK_HEWING_LOG, "Partly Hewn Oak Log");
		builder.add(ModBlocks.HEWN_OAK_BEAM, "Hewn Oak Beam");
		builder.add(ModBlocks.SPRUCE_HEWING_LOG, "Partly Hewn Spruce Log");
		builder.add(ModBlocks.HEWN_SPRUCE_BEAM, "Hewn Spruce Beam");
		builder.add(ModBlocks.CHESTNUT_WOOD, "Chestnut Wood");
		builder.add(ModBlocks.STRIPPED_CHESTNUT_LOG, "Stripped Chestnut Log");
		builder.add(ModBlocks.STRIPPED_CHESTNUT_WOOD, "Stripped Chestnut Wood");
		builder.add(ModBlocks.CHESTNUT_PLANKS, "Chestnut Planks");
		builder.add(ModBlocks.ROUGH_CHESTNUT_BOARDS, "Rough Chestnut Boards");
		builder.add(ModBlocks.ROUGH_CHESTNUT_BOARD_STAIRS, "Rough Chestnut Board Stairs");
		builder.add(
			ModBlocks.ROUGH_CHESTNUT_OPEN_STAIRCASE,
			"Rough Chestnut Open Staircase"
		);
		builder.add(ModBlocks.ROUGH_CHESTNUT_BOARD_SLAB, "Rough Chestnut Board Slab");
		builder.add(ModBlocks.ROCKING_CHAIR, "Ladder-Back Rocking Chair");
		builder.add(ModBlocks.CHESTNUT_SHINGLES, "Chestnut Shingles");
		builder.add(ModBlocks.CHESTNUT_SHINGLE_STAIRS, "Chestnut Shingle Stairs");
		builder.add(ModBlocks.CHESTNUT_SHINGLE_SLAB, "Chestnut Shingle Slab");
		builder.add(ModBlocks.SPLIT_CHESTNUT_RAILS, "Split Chestnut Rails");
		builder.add(ModBlocks.CHINKED_CHESTNUT_LOGS, "Chinked Chestnut Logs");
		builder.add(ModBlocks.FIELDSTONE, "Fieldstone");
		builder.add(ModBlocks.FIELDSTONE_STAIRS, "Fieldstone Stairs");
		builder.add(ModBlocks.FIELDSTONE_SLAB, "Fieldstone Slab");
		builder.add(ModBlocks.FIELDSTONE_WALL, "Fieldstone Wall");
		builder.add(ModBlocks.DRESSED_FIELDSTONE, "Dressed Fieldstone");
		builder.add(ModBlocks.DRESSED_FIELDSTONE_STAIRS, "Dressed Fieldstone Stairs");
		builder.add(ModBlocks.DRESSED_FIELDSTONE_SLAB, "Dressed Fieldstone Slab");
		builder.add(ModBlocks.DRESSED_FIELDSTONE_WALL, "Dressed Fieldstone Wall");
		builder.add(ModBlocks.CHISELED_FIELDSTONE, "Chiseled Fieldstone");
		builder.add(ModBlocks.FIELDSTONE_PIER, "Fieldstone Pier");
		builder.add(ModBlocks.ROUGH_OAK_BOARDS, "Rough Oak Boards");
		builder.add(ModBlocks.ROUGH_SPRUCE_BOARDS, "Rough Spruce Boards");
		builder.add(ModBlocks.SAWING_TRESTLES, "Sawing Trestles");
		builder.add(ModBlocks.LOADED_SAWING_TRESTLES, "Loaded Sawing Trestles");
		builder.add(ModBlocks.SPLITTING_STUMP, "Splitting Stump");
		builder.add(ModBlocks.LOADED_SPLITTING_STUMP, "Loaded Splitting Stump");
		builder.add(ModBlocks.CHESTNUT_LEAVES, "Chestnut Leaves");
		builder.add(ModBlocks.HEMLOCK_FOLIAGE, "Eastern Hemlock Foliage");
		builder.add(ModBlocks.CHESTNUT_SAPLING, "Chestnut Sapling");
		builder.add(ModBlocks.MOUNTAIN_LAUREL, "Mountain Laurel");
		builder.add(ModBlocks.LOWBUSH_BLUEBERRY, "Lowbush Blueberry");
		builder.add(ModBlocks.FOREST_DUFF, "Forest Duff");
		builder.add(ModBlocks.CHESTNUT_PILE, "Chestnut Pile");
		builder.add(ModBlocks.CORN, "Corn Crop");
		builder.add(ModBlocks.DRYING_CORN_BUNDLE, "Corn Drying Rack");
		builder.add(ModBlocks.CHESTNUT_STAIRS, "Chestnut Stairs");
		builder.add(ModBlocks.CHESTNUT_SLAB, "Chestnut Slab");
		builder.add(ModBlocks.CHESTNUT_FENCE, "Chestnut Fence");
		builder.add(ModBlocks.CHESTNUT_FENCE_GATE, "Chestnut Fence Gate");
		builder.add(ModBlocks.CHESTNUT_PRESSURE_PLATE, "Chestnut Pressure Plate");
		builder.add(ModBlocks.CHESTNUT_BUTTON, "Chestnut Button");
		builder.add(ModBlocks.HEMLOCK_LOG, "Eastern Hemlock Log");
		builder.add(ModBlocks.HEMLOCK_WOOD, "Eastern Hemlock Wood");
		builder.add(ModBlocks.STRIPPED_HEMLOCK_LOG, "Stripped Eastern Hemlock Log");
		builder.add(ModBlocks.STRIPPED_HEMLOCK_WOOD, "Stripped Eastern Hemlock Wood");
		builder.add(ModBlocks.HEMLOCK_PLANKS, "Eastern Hemlock Planks");
		builder.add(ModBlocks.HEMLOCK_STAIRS, "Eastern Hemlock Stairs");
		builder.add(ModBlocks.HEMLOCK_SLAB, "Eastern Hemlock Slab");
		builder.add(ModBlocks.HEMLOCK_FENCE, "Eastern Hemlock Fence");
		builder.add(ModBlocks.HEMLOCK_FENCE_GATE, "Eastern Hemlock Fence Gate");
		builder.add(ModBlocks.HEMLOCK_PRESSURE_PLATE, "Eastern Hemlock Pressure Plate");
		builder.add(ModBlocks.HEMLOCK_BUTTON, "Eastern Hemlock Button");
		builder.add(ModBlocks.HEMLOCK_SAPLING, "Eastern Hemlock Sapling");
		builder.add(ModBlocks.AMERICAN_BEECH_LOG, "American Beech Log");
		builder.add(ModBlocks.AMERICAN_BEECH_WOOD, "American Beech Wood");
		builder.add(ModBlocks.STRIPPED_AMERICAN_BEECH_LOG, "Stripped American Beech Log");
		builder.add(ModBlocks.STRIPPED_AMERICAN_BEECH_WOOD, "Stripped American Beech Wood");
		builder.add(ModBlocks.AMERICAN_BEECH_PLANKS, "American Beech Planks");
		builder.add(ModBlocks.AMERICAN_BEECH_STAIRS, "American Beech Stairs");
		builder.add(ModBlocks.AMERICAN_BEECH_SLAB, "American Beech Slab");
		builder.add(ModBlocks.AMERICAN_BEECH_FENCE, "American Beech Fence");
		builder.add(ModBlocks.AMERICAN_BEECH_FENCE_GATE, "American Beech Fence Gate");
		builder.add(ModBlocks.AMERICAN_BEECH_PRESSURE_PLATE, "American Beech Pressure Plate");
		builder.add(ModBlocks.AMERICAN_BEECH_BUTTON, "American Beech Button");
		builder.add(ModBlocks.AMERICAN_BEECH_LEAVES, "American Beech Leaves");
		builder.add(ModBlocks.AMERICAN_BEECH_SAPLING, "American Beech Sapling");
		builder.add(ModBlocks.BLACK_WALNUT_LOG, "Black Walnut Log");
		builder.add(ModBlocks.BLACK_WALNUT_WOOD, "Black Walnut Wood");
		builder.add(ModBlocks.STRIPPED_BLACK_WALNUT_LOG, "Stripped Black Walnut Log");
		builder.add(ModBlocks.STRIPPED_BLACK_WALNUT_WOOD, "Stripped Black Walnut Wood");
		builder.add(ModBlocks.BLACK_WALNUT_LEAVES, "Black Walnut Leaves");
		builder.add(ModBlocks.BLACK_WALNUT_SAPLING, "Black Walnut Sapling");
		builder.add(ModItems.BROAD_AXE, "Broad Axe");
		builder.add(ModItems.FRAME_SAW, "Frame Saw");
		builder.add(ModItems.FROE, "Froe");
		builder.add(ModItems.WOODEN_MAUL, "Wooden Maul");
		builder.add(ModItems.HANDFUL_OF_CHESTNUTS, "Handful of Chestnuts");
		builder.add(ModItems.ROASTED_CHESTNUTS, "Roasted Chestnuts");
		builder.add(ModItems.CORN_KERNELS, "Corn Kernels");
		builder.add(ModItems.EAR_OF_CORN, "Ear of Corn");
		builder.add(ModItems.CORNMEAL, "Cornmeal");
		builder.add(ModItems.DRIED_EAR_OF_CORN, "Dried Ear of Corn");
		builder.add("biome.before_the_blight.chestnut_oak_ridge", "Chestnut-Oak Ridge");
		builder.add("biome.before_the_blight.hemlock_beech_cove", "Hemlock Cove");
		builder.add("biome.before_the_blight.grassy_bald", "Grassy Bald");
		builder.add("itemGroup.before_the_blight.building_materials", "Before the Blight: Building Materials");
		builder.add("itemGroup.before_the_blight.furniture_decor", "Before the Blight: Furniture & Decor");
		builder.add("itemGroup.before_the_blight.nature_farming", "Before the Blight: Nature & Farming");
		builder.add("itemGroup.before_the_blight.tools_workstations", "Before the Blight: Tools & Workstations");
		builder.add("itemGroup.before_the_blight.before_the_blight", "Before the Blight: All");
		builder.add("tag.item.before_the_blight.chestnut_logs", "Chestnut Logs");
		builder.add("tag.item.before_the_blight.chestnut_wooden_blocks", "Chestnut Wooden Blocks");
		builder.add("tag.item.before_the_blight.hemlock_logs", "Eastern Hemlock Logs");
		builder.add("tag.item.before_the_blight.american_beech_logs", "American Beech Logs");
		builder.add("tag.item.before_the_blight.black_walnut_logs", "Black Walnut Logs");
		builder.add("tag.item.before_the_blight.hewing_logs", "Hewing Logs");
		builder.add("tag.item.before_the_blight.hewn_beams", "Hewn Beams");
		builder.add("tag.item.before_the_blight.hewn_details", "Hewn Chestnut Details");
		builder.add("tag.item.before_the_blight.sawable_beams", "Sawable Beams");
		builder.add("tag.item.before_the_blight.rough_boards", "Rough Boards");
		builder.add(
			"item.before_the_blight.broad_axe.tooltip",
			"Hew four times; the final strike batches up to 64 matching logs"
		);
		builder.add(
			"item.before_the_blight.frame_saw.tooltip.load",
			"Load a hewn beam onto sawing trestles"
		);
		builder.add(
			"item.before_the_blight.frame_saw.tooltip.cut",
			"Saw four times; the final stroke batches up to 64 matching beams"
		);
		builder.add(
			"item.before_the_blight.froe.tooltip",
			"Set on a loaded stump: top for shingles, side for rails"
		);
		builder.add(
			"item.before_the_blight.wooden_maul.tooltip",
			"Strike three times; the final blow batches up to 64 matching beams"
		);
		builder.add("rei.before_the_blight.category.hewing", "Hand Hewing");
		builder.add("rei.before_the_blight.category.sawing", "Frame Sawing");
		builder.add("rei.before_the_blight.category.splitting", "Froe Splitting");
		builder.add("rei.before_the_blight.category.drying", "Air Drying");
		builder.add("rei.before_the_blight.note.hewing", "4 strikes; batches up to 64");
		builder.add("rei.before_the_blight.note.sawing", "4 strokes; batches up to 64");
		builder.add(
			"rei.before_the_blight.note.splitting.shingles",
			"Top or bottom face; 3 maul blows"
		);
		builder.add(
			"rei.before_the_blight.note.splitting.rails",
			"Side face; 3 maul blows"
		);
		builder.add(
			"rei.before_the_blight.note.drying",
			"Load 1-64 ears; 3 successful drying advances"
		);

		addGuide(
			builder,
			"chestnut_log",
			"What: Raw American chestnut timber. Obtain: Fell a natural or sapling-grown chestnut tree. Use: Craft chestnut wood, planks, or chinked logs, or place it and strike four times with a broad axe to make a hewn beam."
		);
		addGuide(
			builder,
			"hewn_chestnut_beam",
			"What: A squared chestnut building timber. Obtain: Place a chestnut log and strike it four times with a broad axe; the final strike can process up to 64 total matching logs, including the placed log, with proportional tool damage. Use: Build directly, frame-saw it into four rough boards, or split it on a stump."
		);
		addGuide(
			builder,
			"hewn_chestnut_wall",
			"What: A narrow connected wall of squared chestnut timbers. Obtain: Craft three hewn chestnut beams in a row to receive six. Use: Porch rails, low partitions, crib walls, and detailed timber-frame infill; it connects like a vanilla wall."
		);
		addGuide(
			builder,
			"hewn_chestnut_post",
			"What: A half-block-wide squared chestnut framing member. Obtain: Craft one hewn chestnut beam to receive four. Use: Rotate it on any axis for porch posts, braces, loft frames, rails, and fine structural detailing."
		);
		addGuide(
			builder,
			"hewn_oak_beam",
			"What: A squared oak building timber. Obtain: Place an oak log and strike it four times with a broad axe; the final strike can process up to 64 total matching logs, including the placed log, with proportional tool damage. Use: Build directly or frame-saw it into four rough oak boards."
		);
		addGuide(
			builder,
			"hewn_spruce_beam",
			"What: A squared spruce building timber. Obtain: Place a spruce log and strike it four times with a broad axe; the final strike can process up to 64 total matching logs, including the placed log, with proportional tool damage. Use: Build directly or frame-saw it into four rough spruce boards."
		);
		addGuide(
			builder,
			"chestnut_wood",
			"What: Chestnut bark on all six faces. Obtain: Craft four chestnut logs in a 2 by 2 square to receive three. Use: Building, stripping, and making chestnut planks."
		);
		addGuide(
			builder,
			"stripped_chestnut_log",
			"What: Smooth chestnut timber with exposed end grain. Obtain: Use an ordinary axe, not the Broad Axe, on a placed chestnut log. Use: Building, making stripped chestnut wood, or crafting chestnut planks."
		);
		addGuide(
			builder,
			"stripped_chestnut_wood",
			"What: Smooth chestnut timber on all six faces. Obtain: Use an ordinary axe, not the Broad Axe, on placed chestnut wood, or craft four stripped chestnut logs in a 2 by 2 square to receive three. Use: Building or making chestnut planks."
		);
		addGuide(
			builder,
			"chestnut_planks",
			"What: Finished chestnut lumber. Obtain: Craft one chestnut log or wood block into four planks. Use: General building, chestnut shapes, fences, gates, redstone fittings, and hand-tool workstations."
		);
		addGuide(
			builder,
			"rough_chestnut_boards",
			"What: Wide hand-sawn chestnut boards. Obtain: Load a hewn chestnut beam onto sawing trestles and use a frame saw for four strokes to receive four; the final stroke can batch matching beams. Use: Regional building, rough stairs and slabs, or a splitting stump."
		);
		addGuide(
			builder,
			"rough_chestnut_board_stairs",
			"What: Stairs made from hand-sawn chestnut boards. Obtain: Craft six rough chestnut boards in a stair pattern to receive four. Use: Rustic stairs, roofs, and trim."
		);
		addGuide(
			builder,
			"rough_chestnut_board_slab",
			"What: Half-height hand-sawn chestnut boards. Obtain: Craft three rough chestnut boards in a row to receive six. Use: Floors, shelves, roofs, and trim."
		);
		addGuide(
			builder,
			"rocking_chair",
			"What: A plain ladder-back chestnut rocking chair with a woven seat. Obtain: Craft four rough chestnut boards, two sticks, and one string in the chair pattern. Use: Interact with an empty hand or a held non-block item to sit. A held block item remains available for normal placement, and sneak-use bypasses seating. Dismount normally; the chair and rider rock together while occupied."
		);
		addGuide(
			builder,
			"chestnut_shingles",
			"What: Riven chestnut roofing. Obtain: Load a hewn chestnut beam on a splitting stump, set a froe on the top or bottom face, and strike three times with a wooden maul to receive four. Use: Durable regional roofs and siding."
		);
		addGuide(
			builder,
			"chestnut_shingle_stairs",
			"What: Sloped riven-shingle roofing. Obtain: Craft six chestnut shingle blocks in a stair pattern to receive four. Use: Roof pitches and shaped trim."
		);
		addGuide(
			builder,
			"chestnut_shingle_slab",
			"What: Half-height riven-shingle roofing. Obtain: Craft three chestnut shingle blocks in a row to receive six. Use: Roof edges, shallow pitches, and trim."
		);
		addGuide(
			builder,
			"split_chestnut_rails",
			"What: Hand-riven chestnut rails. Obtain: Load a hewn chestnut beam on a splitting stump, set a froe on a side face, and strike three times with a wooden maul to receive two. Use: Fence-like boundaries and rustic construction."
		);
		addGuide(
			builder,
			"chinked_chestnut_logs",
			"What: Chestnut log walling sealed with clay chinking. Obtain: Craft six chestnut logs around three clay balls to receive six. Use: Weather-tight cabin walls."
		);
		addGuide(
			builder,
			"fieldstone",
			"What: Irregular local stone masonry. Obtain: Craft four cobblestone in a 2 by 2 square to receive four. Use: Foundations, hearths, chimneys, and regional stonework."
		);
		addGuide(
			builder,
			"fieldstone_stairs",
			"What: Shaped fieldstone steps. Obtain: Craft six fieldstone in a stair pattern to receive four. Use: Steps, sloped masonry, and roof or chimney details."
		);
		addGuide(
			builder,
			"fieldstone_slab",
			"What: Half-height fieldstone masonry. Obtain: Craft three fieldstone in a row to receive six. Use: Floors, caps, foundations, and shallow steps."
		);
		addGuide(
			builder,
			"fieldstone_wall",
			"What: Narrow fieldstone walling. Obtain: Craft six fieldstone in two rows to receive six. Use: Boundaries, foundations, retaining walls, and chimney details."
		);
		addGuide(
			builder,
			"dressed_fieldstone",
			"What: Local fieldstone squared into regular masonry faces. Obtain: Craft four fieldstone in a 2 by 2 square to receive four. Use: Formal foundations, hearth surrounds, quoins, and other carefully laid stonework."
		);
		addGuide(
			builder,
			"dressed_fieldstone_stairs",
			"What: Steps cut from dressed fieldstone. Obtain: Craft six dressed fieldstone in a stair pattern to receive four. Use: Masonry stairs, chimney shoulders, and shaped foundation details."
		);
		addGuide(
			builder,
			"dressed_fieldstone_slab",
			"What: A half-height course of dressed fieldstone. Obtain: Craft three dressed fieldstone in a row to receive six. Use: Caps, hearths, shelves, and shallow masonry courses."
		);
		addGuide(
			builder,
			"dressed_fieldstone_wall",
			"What: Narrow walling made from squared local stone. Obtain: Craft six dressed fieldstone in two rows to receive six. Use: Retaining walls, boundaries, chimney details, and foundation edges."
		);
		addGuide(
			builder,
			"chiseled_fieldstone",
			"What: A restrained hand-tooled fieldstone detail. Obtain: Stack two dressed fieldstone slabs in the crafting grid. Use: Sparingly as a lintel, hearth, date-stone, or focal masonry block."
		);
		addGuide(
			builder,
			"fieldstone_pier",
			"What: A squared masonry support with a distinct cap and bed face. Obtain: Stack two dressed fieldstone blocks to receive two. Use: Rotate it for foundation piers, chimney supports, quoins, and heavy stone detailing."
		);
		addGuide(
			builder,
			"rough_oak_boards",
			"What: Wide hand-sawn oak boards. Obtain: Load a hewn oak beam onto sawing trestles and use a frame saw for four strokes to receive four; the final stroke can batch matching beams. Use: Rustic oak construction and the rough-board ingredient tag."
		);
		addGuide(
			builder,
			"rough_spruce_boards",
			"What: Wide hand-sawn spruce boards. Obtain: Load a hewn spruce beam onto sawing trestles and use a frame saw for four strokes to receive four; the final stroke can batch matching beams. Use: Rustic spruce construction and the rough-board ingredient tag."
		);
		addGuide(
			builder,
			"sawing_trestles",
			"What: A support frame for hand sawing. Obtain: Craft two planks above three sticks. Use: Place it, load a hewn beam, then use a frame saw for four strokes to make rough boards."
		);
		addGuide(
			builder,
			"splitting_stump",
			"What: A work surface for riving chestnut timber. Obtain: Craft four rough boards around one chestnut log. Use: Load a hewn chestnut beam, set a froe by face, then strike it three times with a wooden maul."
		);
		addGuide(
			builder,
			"chestnut_stairs",
			"What: Finished chestnut plank stairs. Obtain: Craft six chestnut planks in a stair pattern to receive four. Use: Finished stairs, roofs, and trim."
		);
		addGuide(
			builder,
			"chestnut_slab",
			"What: Finished half-height chestnut planks. Obtain: Craft three chestnut planks in a row to receive six. Use: Floors, shelves, roofs, and trim."
		);
		addGuide(
			builder,
			"chestnut_fence",
			"What: A finished chestnut fence. Obtain: Craft four chestnut planks around two sticks to receive three. Use: Connected boundaries for animals, paths, and builds."
		);
		addGuide(
			builder,
			"chestnut_fence_gate",
			"What: An opening chestnut fence gate. Obtain: Craft two chestnut planks between four sticks. Use: A player-operated opening in fences."
		);
		addGuide(
			builder,
			"chestnut_pressure_plate",
			"What: A wooden redstone pressure plate. Obtain: Craft two chestnut planks side by side. Use: Trigger redstone when entities stand on it."
		);
		addGuide(
			builder,
			"chestnut_button",
			"What: A wooden redstone button. Obtain: Craft one chestnut plank. Use: Provide a short player-operated redstone pulse."
		);
		addGuide(
			builder,
			"chestnut_leaves",
			"What: Broad chestnut canopy foliage. Obtain: Break chestnut leaves with shears or a Silk Touch tool. Use: Natural decoration and composting; ordinary broken leaves can instead drop saplings, sticks, or rare handfuls of chestnuts."
		);
		addGuide(
			builder,
			"hemlock_foliage",
			"What: Evergreen Eastern hemlock foliage. Obtain: Cut it with shears or Silk Touch in Hemlock Cove; ordinary broken foliage can drop a hemlock sapling or sticks. Use: Shaded-canopy decoration and composting."
		);
		addGuide(
			builder,
			"chestnut_sapling",
			"What: A growable American chestnut sapling. Obtain: Break chestnut leaves without shears or Silk Touch; Fortune improves the chance. Use: Plant one for a field tree, a complete 2 by 2 for a forest tree, or a complete 3 by 3 for old growth."
		);
		addCoreWoodGuides(builder, "hemlock", "Eastern hemlock", "Hemlock Cove");
		addGuide(
			builder,
			"hemlock_sapling",
			"What: A growable Eastern hemlock sapling. Obtain: Break hemlock foliage without shears or Silk Touch; Fortune improves the chance. Use: Plant one with open vertical space to grow either a tall or spreading hemlock."
		);
		addCoreWoodGuides(
			builder,
			"american_beech",
			"American beech",
			"Hemlock Cove"
		);
		addGuide(
			builder,
			"american_beech_leaves",
			"What: Broad American beech foliage. Obtain: Cut it with shears or Silk Touch in Hemlock Cove; ordinary broken leaves can drop an American beech sapling or sticks. Use: Deciduous canopy decoration and composting."
		);
		addGuide(
			builder,
			"american_beech_sapling",
			"What: A growable American beech sapling. Obtain: Break American beech leaves without shears or Silk Touch; Fortune improves the chance. Use: Plant one with open space to grow a smooth-barked broadleaf tree."
		);
		addGuide(
			builder,
			"black_walnut_log",
			"What: Raw Black walnut timber. Obtain: Fell a rare natural or sapling-grown Black walnut in Hemlock Cove. Use: Build with it, strip it with an axe, craft all-bark wood, or saw it into walnut furniture board."
		);
		addGuide(
			builder,
			"black_walnut_wood",
			"What: Black walnut bark on all six faces. Obtain: Craft four Black walnut logs in a 2 by 2 square to receive three. Use: Bark-faced building or stripping with an axe."
		);
		addGuide(
			builder,
			"stripped_black_walnut_log",
			"What: A stripped Black walnut log with exposed end grain. Obtain: Use an axe on a placed Black walnut log. Use: Smooth hardwood framing and decoration."
		);
		addGuide(
			builder,
			"stripped_black_walnut_wood",
			"What: Stripped Black walnut wood on every face. Obtain: Strip matching wood with an axe, or craft four stripped logs in a 2 by 2 square to receive three. Use: Smooth all-bark-free construction."
		);
		addGuide(
			builder,
			"black_walnut_leaves",
			"What: Broad Black walnut foliage. Obtain: Cut it with shears or Silk Touch in Hemlock Cove; ordinary broken leaves can drop a Black walnut sapling or sticks. Use: Deciduous canopy decoration and composting."
		);
		addGuide(
			builder,
			"black_walnut_sapling",
			"What: A growable Black walnut sapling. Obtain: Break Black walnut leaves without shears or Silk Touch; Fortune improves the chance. Use: Plant one with open space to grow a broad-crowned hardwood tree."
		);
		addGuide(
			builder,
			"mountain_laurel",
			"What: A dense evergreen Appalachian shrub. Obtain: Gather naturally generated mountain laurel in Chestnut-Oak Ridge, or take it from the creative tab. Use: Patchy understory, slope edges, and garden-like natural decoration."
		);
		addGuide(
			builder,
			"lowbush_blueberry",
			"What: A low, leaf-only blueberry shrub. Obtain: Gather naturally generated lowbush blueberry in Chestnut-Oak Ridge, or take it from the creative tab. Use: Sparse acidic-soil understory decoration; this alpha does not add harvestable berries."
		);
		addGuide(
			builder,
			"forest_duff",
			"What: A thin layer of fallen leaves, needles, and small woody debris. Obtain: Gather naturally generated forest duff in Ridge or Cove forest, or take it from the creative tab. Use: Break up bare ground around trees, deadfall, paths, and structures."
		);
		addGuide(
			builder,
			"broad_axe",
			"What: The dedicated timber-hewing axe. Obtain: Craft four iron ingots and two sticks in the shown broad-axe pattern. Use: Strike a placed chestnut, oak, or spruce log four times; the last strike can process up to 64 total matching logs, including the placed log, with proportional durability cost."
		);
		addGuide(
			builder,
			"frame_saw",
			"What: A two-handed saw for beams. Obtain: Craft four planks, two iron ingots, and one string in the shown frame pattern. Use: Load a hewn beam on sawing trestles and saw four times; the final stroke can batch matching beams."
		);
		addGuide(
			builder,
			"froe",
			"What: An iron blade used to control timber splits. Obtain: Craft two iron ingots and one stick. Use: Set it on a loaded splitting stump: top or bottom for shingles, side for rails, then use a wooden maul."
		);
		addGuide(
			builder,
			"wooden_maul",
			"What: A heavy wooden striking tool. Obtain: Craft four chestnut logs and two sticks in the shown diagonal-handle pattern. Use: Strike a froe set in a loaded stump three times; the final blow can batch matching beams."
		);
		addGuide(
			builder,
			"handful_of_chestnuts",
			"What: A gathered handful of edible chestnuts in their shells. Obtain: Gather a naturally generated chestnut pile in Chestnut-Oak Ridge, or receive a rare drop when chestnut leaves are broken without shears or Silk Touch; Fortune improves the leaf-drop chance. Use: Roast it in a furnace, smoker, or campfire, or place it as a thin chestnut-pile layer."
		);
		addGuide(
			builder,
			"roasted_chestnuts",
			"What: Cooked chestnuts ready to eat. Obtain: Cook a handful of chestnuts in a furnace, smoker, or campfire. Use: Eat for four hunger and moderate saturation."
		);
		addGuide(
			builder,
			"corn_kernels",
			"What: Seed kernels for the regional corn crop. Obtain: Craft one wheat seed with one yellow dye for the bootstrap kernel, then harvest mature corn for two to four kernels plus an ear. Use: Plant on farmland to grow more corn, or compost."
		);
		addGuide(
			builder,
			"ear_of_corn",
			"What: A fresh mature ear of field corn. Obtain: Harvest a fully grown corn crop. Use: Eat it, grind one into three cornmeal, or place up to a stack on a corn drying rack."
		);
		addGuide(
			builder,
			"cornmeal",
			"What: Coarsely ground field corn. Obtain: Craft one fresh ear into three cornmeal or one dried ear into four. Use: Craft three cornmeal into one loaf of bread."
		);
		addGuide(
			builder,
			"drying_corn_bundle",
			"What: A reusable freestanding wooden rack that holds up to 64 ears for air drying. Obtain: Craft it from sticks, rough chestnut boards, and string. Use: Place the empty rack, then use fresh ears on it to transfer as many as will fit. Added fresh ears reset the shared drying age. Empty-hand unloads the exact stored count as fresh ears before maturity or dried ears after three advances, leaving the rack in place; breaking returns the rack and all stored ears."
		);
		addGuide(
			builder,
			"dried_ear_of_corn",
			"What: An air-dried ear suited to milling. Obtain: Load fresh ears onto a corn drying rack, let them reach the fourth visible state, then harvest or break the rack. Use: Craft one dried ear into four cornmeal, or compost it."
		);

		builder.add("subtitles.before_the_blight.saw_stroke", "Frame saw scrapes");
		builder.add("subtitles.before_the_blight.saw_complete", "Boards split free");
		builder.add("subtitles.before_the_blight.froe_set", "Froe is set");
		builder.add("subtitles.before_the_blight.maul_strike", "Maul strikes wood");
		builder.add("subtitles.before_the_blight.split_complete", "Timber splits");

		addRegisteredFamilyTranslations(builder, translations);
		addRegistryFallbacks(builder, translations, isolatedTranslations());
		translations.forEach(output::add);
	}

	private static TranslationBuilder collectingBuilder(Map<String, String> translations) {
		return (key, value) -> {
			String previous = translations.putIfAbsent(key, value);
			if (previous != null) {
				throw new IllegalStateException(
					"Duplicate English translation key " + key
						+ " (existing=\"" + previous + "\", incoming=\"" + value + "\")"
				);
			}
		};
	}

	private static void addRegisteredFamilyTranslations(
		TranslationBuilder builder,
		Map<String, String> translations
	) {
		BoardRoofDataDefinitions.addTranslations(builder);
		ModRegionalWoodBlocks.EN_US_TRANSLATIONS.forEach(builder::add);
		ModFurnitureBlocks.ENGLISH_NAMES.forEach(
			(id, name) -> builder.add("block.before_the_blight." + id, name)
		);
		ModFurnitureBlocks.CONTAINER_ENGLISH_NAMES.forEach(builder::add);
		ModStoneHearthBlocks.LANGUAGE_ENTRIES.forEach(
			(id, name) -> builder.add("block.before_the_blight." + id, name)
		);
		ModTimberBlocks.LANGUAGE_ENTRIES.forEach(
			(id, name) -> builder.add("block.before_the_blight." + id, name)
		);
		DoorWindowTranslations.addTranslations(builder::add);
		CornCribContentTranslations.addTranslations(builder::add);
		addItemTagTranslations(builder);

		/*
		 * The board/roof definition table predates the strict actionable-guide
		 * contract. Preserve its specific acquisition copy while completing
		 * the required What/Obtain/Use structure.
		 */
		translations.replaceAll((key, value) -> {
			if (!key.startsWith("rei.before_the_blight.guide.")) {
				return value;
			}
			String normalized = value;
			if (!normalized.contains("What:")) {
				normalized = "What: A regional building component. " + normalized;
			}
			if (!normalized.contains("Obtain:")) {
				normalized = normalized.replaceFirst(
					"(?i)^What: A regional building component\\.\\s*",
					"What: A regional building component. Obtain: "
				);
			}
			if (!normalized.contains("Use:")) {
				normalized += " Use: Place it as a historically grounded Appalachian building component.";
			}
			return normalized;
		});
	}

	private static void addItemTagTranslations(TranslationBuilder builder) {
		List<String> tagPaths = List.of(
			"appalachian_structural_logs",
			"beam_mounted_props",
			"chinking",
			"chinking_materials",
			"construction_tools",
			"corn_crib_architecture",
			"corn_crib_contents",
			"corn_crib_wall_mounted_props",
			"damaged_blocks",
			"earth_floors",
			"earth_layers",
			"exterior_props",
			"farmstead_fences",
			"fieldstone",
			"fieldstone_foundation_components",
			"fireplace_components",
			"floor_clutter",
			"furniture",
			"furniture/cabin",
			"furniture_board_materials",
			"half_logs",
			"hand_split_boards",
			"hearth_components",
			"hewn_logs",
			"masonry_details",
			"paths",
			"regional_wood_blocks",
			"regional_wood_components",
			"rough_softwood_boards",
			"round_poles",
			"seating",
			"split_rails",
			"storage_furniture",
			"structural_posts",
			"timber_joints",
			"timber_members",
			"wall_mounted_props/cabin",
			"wall_mounted_props/exterior",
			"weathered_blocks",
			"workstations"
		);
		for (String path : tagPaths) {
			String label = switch (path) {
				case "furniture/cabin" -> "Cabin Furniture";
				case "wall_mounted_props/cabin" -> "Cabin Wall-Mounted Props";
				case "wall_mounted_props/exterior" -> "Exterior Wall-Mounted Props";
				default -> humanize(path);
			};
			builder.add(
				"tag.item.before_the_blight." + path.replace('/', '.'),
				label
			);
		}
	}

	private static Map<String, String> isolatedTranslations() {
		Map<String, String> translations = new LinkedHashMap<>();
		ClassLoader loader = BeforeTheBlightEnglishLanguageProvider.class.getClassLoader();
		for (String path : ISOLATED_LANGUAGE_RESOURCES) {
			try (InputStream stream = loader.getResourceAsStream(path)) {
				if (stream == null) {
					throw new IllegalStateException("Missing language resource " + path);
				}
				JsonElement root = JsonParser.parseReader(
					new InputStreamReader(stream, StandardCharsets.UTF_8)
				);
				if (!root.isJsonObject()) {
					throw new IllegalStateException("Language resource is not an object: " + path);
				}
				JsonObject object = root.getAsJsonObject();
				for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
					if (!entry.getValue().isJsonPrimitive()
						|| !entry.getValue().getAsJsonPrimitive().isString()) {
						throw new IllegalStateException(
							"Language value is not text: " + path + "#" + entry.getKey()
						);
					}
					String previous = translations.putIfAbsent(
						entry.getKey(),
						entry.getValue().getAsString()
					);
					if (previous != null) {
						throw new IllegalStateException(
							"Duplicate isolated language key " + entry.getKey()
						);
					}
				}
			} catch (IOException exception) {
				throw new UncheckedIOException(
					"Unable to read language resource " + path,
					exception
				);
			}
		}
		return Map.copyOf(translations);
	}

	private static void addRegistryFallbacks(
		TranslationBuilder builder,
		Map<String, String> translations,
		Map<String, String> isolated
	) {
		for (ItemLike itemLike : ModContentCatalog.allItems()) {
			Item item = itemLike.asItem();
			Identifier id = BuiltInRegistries.ITEM.getKey(item);
			if (!BeforeTheBlight.MOD_ID.equals(id.getNamespace())) {
				throw new IllegalStateException("Foreign item in content catalog: " + id);
			}

			String path = id.getPath();
			String displayKey = item.getDescriptionId();
			String displayName = translations.getOrDefault(
				displayKey,
				isolated.getOrDefault(displayKey, humanize(path))
			);
			if (!translations.containsKey(displayKey) && !isolated.containsKey(displayKey)) {
				builder.add(displayKey, displayName);
			}

			String guideKey = "rei.before_the_blight.guide." + path;
			if (!translations.containsKey(guideKey) && !isolated.containsKey(guideKey)) {
				builder.add(guideKey, fallbackGuide(item, path, displayName));
			}
		}
	}

	private static String fallbackGuide(Item item, String path, String displayName) {
		return switch (path) {
			case "yellow_poplar_log" ->
				"What: Regional yellow-poplar timber. Obtain: Use the bootstrap shapeless recipe to convert one birch log. Use: Build yellow-poplar structural and board components; a coherent natural tree source is deferred.";
			case "white_oak_log" ->
				"What: Regional white-oak timber. Obtain: Use the bootstrap shapeless recipe to convert one oak log. Use: Build white-oak framing, shingles, rails, and furniture stock; a coherent natural tree source is deferred.";
			case "walnut_furniture_board" ->
				"What: Black-walnut furniture stock. Obtain: Saw it from a naturally grown or player-grown Black walnut log. Use: Craft dark furniture panels, rails, shelves, posts, and tabletops.";
			case "pine_rough_board" ->
				"What: Rough pine board stock. Obtain: Use the bootstrap shapeless recipe to convert one spruce plank. Use: Craft secondary shelving, sash stock, and crates.";
			case "rope_bed" ->
				"What: A two-block rope bed. Obtain: Craft it from the materials shown in Roughly Enough Items. Use: Place it with two clear blocks and use it as a functioning bed; breaking either half cleans up the pair and drops one item.";
			case "trundle_bed" ->
				"What: A low trundle-bed furnishing. Obtain: Craft it from the materials shown in Roughly Enough Items. Use: Empty-hand interact to pull out or retract the lower bed when its front lane is clear. This visual furnishing is decorative and is not sleep-capable.";
			case "wooden_cradle" ->
				"What: A period wooden cradle. Obtain: Craft it from the materials shown in Roughly Enough Items. Use: Place it as a cabin furnishing; this alpha version is decorative and is not sleep-capable.";
			case "fieldstone_firebox" ->
				"What: A modular fieldstone firebox with cold, ash, ember, and active states. Obtain: Craft it from fieldstone and the materials shown in Roughly Enough Items. Use: Empty-hand interact to review its persistent hearth states, matching sounds, particles, and state-dependent light. This is a visual building control with no fuel inventory or cooking process.";
			case "hollowed_chestnut_trough" ->
				"What: A connection-aware hollowed chestnut-log trough that holds real bucketable vanilla water. Obtain: Craft the single placeable trough from chestnut logs using the recipe shown in Roughly Enough Items. Use: Collinear troughs automatically select standalone, inlet, middle, and outlet shapes; a water bucket fills each segment and an empty bucket recovers the water. The silty content is sediment, while clear is a save-compatible legacy alias for real water.";
			case "hollow_limb_spout" ->
				"What: A hollow branch spout that holds real bucketable vanilla water. Obtain: Craft it from chestnut logs using the recipe shown in Roughly Enough Items. Use: Place it over a trough, fill it with a water bucket, and recover that water with an empty bucket. The flowing=true state is a save-compatible legacy alias for real water, not painted water.";
			case "wooden_milk_pail" ->
				"What: A placeable wooden dairy pail with iron hoops and bail. Obtain: Craft it from rough chestnut boards and iron nuggets using the recipe shown in Roughly Enough Items. Use: Decorate a springhouse or farmstead; it is a decorative prop and does not store milk or other fluids.";
			case "chestnut_chinking_strip" ->
				"What: A dedicated chinking and log-wall termination piece. Obtain: Craft it from the materials shown in Roughly Enough Items. Use: Sneak-use to cycle core, fresh, aged, cracked, missing, end, and corner appearances.";
			case "chestnut_repair_corner" ->
				"What: A repairable chestnut log corner joint. Obtain: Craft it from the materials shown in Roughly Enough Items. Use: Sneak-use to cycle square, V-notch, and half-dovetail forms; vertical placement alternates its course.";
			case "saddle_notched_chestnut_corner" ->
				"What: An alternating saddle-notched chestnut corner. Obtain: Craft it from the materials shown in Roughly Enough Items. Use: Apply one clay ball to an unchinked placed corner to seal the joint; the finished chinking cannot be removed by an accidental click.";
			case "rough_chestnut_open_staircase" ->
				"What: A straight open-riser service stair with four rough-board treads and two plain chestnut stringers. Obtain: Craft three rough chestnut boards over two sticks to receive two. Use: Build raised porch, crib, barn, and work-shed access; use a separate landing when the run changes direction.";
			case "board_shutter" ->
				"What: A single operable board shutter. Obtain: Craft it from the materials shown in Roughly Enough Items. Use: Empty-hand interact to swing it open or closed; its outline and collision follow the state.";
			case "paired_board_shutters" ->
				"What: A paired operable board-shutter assembly. Obtain: Craft it from the materials shown in Roughly Enough Items. Use: Empty-hand interact to open or close both leaves together.";
			case "operable_sash_window" ->
				"What: An operable glazed sash window. Obtain: Craft it from the materials shown in Roughly Enough Items. Use: Empty-hand interact to raise or lower the sash; its outline and collision follow the state.";
			case "shuttered_sash_window" ->
				"What: A glazed sash window with synchronized shutters. Obtain: Craft it from the materials shown in Roughly Enough Items. Use: Empty-hand interact to open or close the shuttered assembly.";
			case "drop_leaf_table" ->
				"What: A one-block drop-leaf table. Obtain: Craft it from the materials shown in Roughly Enough Items. Use: Empty-hand interact to raise or lower its leaves; the persisted outline and collision follow the state.";
			case "tallow_candle", "betty_lamp" ->
				"What: " + displayName + ". Obtain: Craft it from the materials shown in Roughly Enough Items. Use: Empty-hand interact to light or extinguish it; the persisted lit state controls its light and particles.";
			case "ladder_back_chair" ->
				"What: A ladder-back chair with a woven seat. Obtain: Craft it from the materials shown in Roughly Enough Items. Use: Interact to sit; breaking the chair removes its temporary seat, and dismounting searches for a safe nearby position.";
			case "rough_three_legged_stool" ->
				"What: A compact three-legged stool. Obtain: Craft it from the materials shown in Roughly Enough Items. Use: Interact to sit; breaking the stool removes its temporary seat, and dismounting searches for a safe nearby position.";
			case "backless_bench", "slab_bench", "wall_bench", "high_back_settle" ->
				"What: " + displayName + ". Obtain: Craft it from the materials shown in Roughly Enough Items. Use: Place it as a period cabin furnishing; unlike the rocking chair, this alpha model is not sit-able.";
			case "shaving_horse" ->
				"What: A woodworking shaving horse. Obtain: Craft it from the materials shown in Roughly Enough Items. Use: Place it as a workshop furnishing; this alpha model is decorative and has no processing recipe.";
			case "floor_loom" ->
				"What: A four-post Appalachian treadle loom with a synchronized working pose. Obtain: Craft it from the materials shown in Roughly Enough Items. Use: Interact to move its beater, heddles, and treadles together; this remains a decorative alpha visualization and produces no items.";
			default -> genericGuide(item, path, displayName);
		};
	}

	private static String genericGuide(Item item, String path, String displayName) {
		String use;
		if (item instanceof BlockItem blockItem) {
			Block block = blockItem.getBlock();
			if (block instanceof DoorBlock || block instanceof TrapDoorBlock) {
				use = "Place it as an operable historical opening; its collision follows the open state.";
			} else if (block instanceof FenceBlock || block instanceof FenceGateBlock) {
				use = "Place it in connected fence runs and inspect its ends, corners, and opening state.";
			} else if (ModFurnitureBlocks.FUNCTIONAL_STORAGE.contains(block)) {
				use = "Place it as a persistent 27-slot storage container.";
			} else if (ModFurnitureBlocks.DECORATIVE_TOGGLE_EQUIPMENT.contains(block)) {
				use = "Place and interact to review its visual state; this alpha model does not produce processed items.";
			} else {
				use = "Place it as a historically grounded building, furnishing, farmstead, or landscape component.";
			}
		} else {
			use = "Use it in the recipes and interactions shown by Roughly Enough Items.";
		}
		return "What: " + displayName + ". Obtain: Craft it from the materials shown in Roughly Enough Items; placed examples return according to their loot rules. Use: "
			+ use;
	}

	private static String humanize(String path) {
		StringBuilder result = new StringBuilder(path.length());
		boolean capitalize = true;
		for (int index = 0; index < path.length(); index++) {
			char character = path.charAt(index);
			if (character == '_' || character == '/') {
				result.append(' ');
				capitalize = true;
			} else if (capitalize) {
				result.append(Character.toUpperCase(character));
				capitalize = false;
			} else {
				result.append(character);
			}
		}
		return result.toString();
	}

	private static void addGuide(
		TranslationBuilder builder,
		String itemId,
		String text
	) {
		builder.add("rei.before_the_blight.guide." + itemId, text);
	}

	private static void addCoreWoodGuides(
		TranslationBuilder builder,
		String prefix,
		String species,
		String biome
	) {
		String title = Character.toUpperCase(species.charAt(0)) + species.substring(1);
		addGuide(
			builder,
			prefix + "_log",
			"What: Raw " + species + " timber. Obtain: Fell a natural or sapling-grown tree in "
				+ biome + ". Use: Build with it, strip it with an axe, or craft it into wood or four planks; specialty hand-hewing for this species is deferred."
		);
		addGuide(
			builder,
			prefix + "_wood",
			"What: " + title + " bark on all six faces. Obtain: Craft four matching logs in a 2 by 2 square to receive three. Use: Building, stripping, or crafting into planks."
		);
		addGuide(
			builder,
			"stripped_" + prefix + "_log",
			"What: A stripped " + species + " log with exposed end grain. Obtain: Use an ordinary axe on a placed matching log. Use: Smooth timber framing, decoration, or planks."
		);
		addGuide(
			builder,
			"stripped_" + prefix + "_wood",
			"What: Stripped " + species + " wood on every face. Obtain: Strip matching wood with an axe, or craft four stripped logs in a 2 by 2 square to receive three. Use: Smooth all-bark-free construction or planks."
		);
		addGuide(
			builder,
			prefix + "_planks",
			"What: Finished " + species + " lumber. Obtain: Craft one matching log or wood block into four. Use: General building and the matching stairs, slabs, fences, gates, pressure plates, and buttons."
		);
		addGuide(
			builder,
			prefix + "_stairs",
			"What: Stairs made from " + species + " planks. Obtain: Craft six matching planks in a stair pattern to receive four. Use: Stairs, roof pitches, and shaped wood trim."
		);
		addGuide(
			builder,
			prefix + "_slab",
			"What: Half-height " + species + " planks. Obtain: Craft three matching planks in a row to receive six. Use: Floors, shelves, roof edges, and trim."
		);
		addGuide(
			builder,
			prefix + "_fence",
			"What: A finished " + species + " fence. Obtain: Craft four matching planks around two sticks to receive three. Use: Connected boundaries, pens, paths, and detailed builds."
		);
		addGuide(
			builder,
			prefix + "_fence_gate",
			"What: An opening " + species + " fence gate. Obtain: Craft two matching planks between four sticks. Use: A player-operated opening in a fence line."
		);
		addGuide(
			builder,
			prefix + "_pressure_plate",
			"What: A wooden redstone pressure plate made from " + species + ". Obtain: Craft two matching planks side by side. Use: Trigger redstone when an entity stands on it."
		);
		addGuide(
			builder,
			prefix + "_button",
			"What: A wooden redstone button made from " + species + ". Obtain: Craft one matching plank. Use: Provide a short player-operated redstone pulse."
		);
	}
}
