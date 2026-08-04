package net.beforetheblight.datagen;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.beforetheblight.registry.ModBoardRoofBlocks;
import net.beforetheblight.registry.ModBlocks;
import net.beforetheblight.registry.ModCornCribBlocks;
import net.beforetheblight.registry.ModDomesticBlocks;
import net.beforetheblight.registry.ModDoorWindowBlocks;
import net.beforetheblight.registry.ModExteriorBlocks;
import net.beforetheblight.registry.ModFurnitureBlocks;
import net.beforetheblight.registry.ModRegionalWoodBlocks;
import net.beforetheblight.registry.ModSpringhouseBlocks;
import net.beforetheblight.registry.ModStoneHearthBlocks;
import net.beforetheblight.registry.ModTags;
import net.beforetheblight.registry.ModTimberBlocks;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;

public final class BeforeTheBlightBlockTagProvider extends FabricTagsProvider.BlockTagsProvider {
	private static final TagKey<Block> SERENE_SEASONS_SPRING_CROPS = sereneSeasonsCropTag("spring_crops");
	private static final TagKey<Block> SERENE_SEASONS_SUMMER_CROPS = sereneSeasonsCropTag("summer_crops");
	private static final TagKey<Block> SERENE_SEASONS_AUTUMN_CROPS = sereneSeasonsCropTag("autumn_crops");
	private static final TagKey<Block> SERENE_SEASONS_UNBREAKABLE_INFERTILE_CROPS =
		sereneSeasonsCropTag("unbreakable_infertile_crops");

	public BeforeTheBlightBlockTagProvider(
		FabricPackOutput output,
		CompletableFuture<HolderLookup.Provider> registriesFuture
	) {
		super(output, registriesFuture);
	}

	@Override
	protected void addTags(HolderLookup.Provider registries) {
		valueLookupBuilder(ModTags.CHESTNUT_LOGS)
			.add(
				ModBlocks.CHESTNUT_LOG,
				ModBlocks.CHESTNUT_WOOD,
				ModBlocks.STRIPPED_CHESTNUT_LOG,
				ModBlocks.STRIPPED_CHESTNUT_WOOD
			);
		valueLookupBuilder(ModTags.HEMLOCK_LOGS)
			.add(
				ModBlocks.HEMLOCK_LOG,
				ModBlocks.HEMLOCK_WOOD,
				ModBlocks.STRIPPED_HEMLOCK_LOG,
				ModBlocks.STRIPPED_HEMLOCK_WOOD
			);
		valueLookupBuilder(ModTags.AMERICAN_BEECH_LOGS)
			.add(
				ModBlocks.AMERICAN_BEECH_LOG,
				ModBlocks.AMERICAN_BEECH_WOOD,
				ModBlocks.STRIPPED_AMERICAN_BEECH_LOG,
				ModBlocks.STRIPPED_AMERICAN_BEECH_WOOD
			);
		valueLookupBuilder(ModTags.BLACK_WALNUT_LOGS)
			.add(
				ModBlocks.BLACK_WALNUT_LOG,
				ModBlocks.BLACK_WALNUT_WOOD,
				ModBlocks.STRIPPED_BLACK_WALNUT_LOG,
				ModBlocks.STRIPPED_BLACK_WALNUT_WOOD
			);
		valueLookupBuilder(BlockTags.LOGS_THAT_BURN)
			.addTag(ModTags.CHESTNUT_LOGS)
			.addTag(ModTags.HEMLOCK_LOGS)
			.addTag(ModTags.AMERICAN_BEECH_LOGS)
			.addTag(ModTags.BLACK_WALNUT_LOGS);
		valueLookupBuilder(BlockTags.LOGS)
			.addTag(ModTags.CHESTNUT_LOGS)
			.addTag(ModTags.HEMLOCK_LOGS)
			.addTag(ModTags.AMERICAN_BEECH_LOGS)
			.addTag(ModTags.BLACK_WALNUT_LOGS);
		valueLookupBuilder(BlockTags.OVERWORLD_NATURAL_LOGS)
			.add(
				ModBlocks.CHESTNUT_LOG,
				ModBlocks.HEMLOCK_LOG,
				ModBlocks.AMERICAN_BEECH_LOG,
				ModBlocks.BLACK_WALNUT_LOG
			);
		valueLookupBuilder(ModTags.CHESTNUT_WOODEN_BLOCKS)
			.add(
				ModBlocks.CHESTNUT_LOG,
				ModBlocks.HEWN_CHESTNUT_BEAM,
				ModBlocks.HEWN_CHESTNUT_WALL,
				ModBlocks.HEWN_CHESTNUT_POST,
				ModBlocks.CHESTNUT_WOOD,
				ModBlocks.STRIPPED_CHESTNUT_LOG,
				ModBlocks.STRIPPED_CHESTNUT_WOOD,
				ModBlocks.CHESTNUT_PLANKS,
				ModBlocks.ROUGH_CHESTNUT_BOARDS,
				ModBlocks.ROUGH_CHESTNUT_BOARD_STAIRS,
				ModBlocks.ROUGH_CHESTNUT_OPEN_STAIRCASE,
				ModBlocks.ROUGH_CHESTNUT_BOARD_SLAB,
				ModBlocks.ROCKING_CHAIR,
				ModBlocks.CHESTNUT_SHINGLES,
				ModBlocks.CHESTNUT_SHINGLE_STAIRS,
				ModBlocks.CHESTNUT_SHINGLE_SLAB,
				ModBlocks.SPLIT_CHESTNUT_RAILS,
				ModBlocks.CHINKED_CHESTNUT_LOGS,
				ModBlocks.CHESTNUT_STAIRS,
				ModBlocks.CHESTNUT_SLAB,
				ModBlocks.CHESTNUT_FENCE,
				ModBlocks.CHESTNUT_FENCE_GATE,
				ModBlocks.CHESTNUT_PRESSURE_PLATE,
				ModBlocks.CHESTNUT_BUTTON
			);
		valueLookupBuilder(ModTags.HEWING_LOGS)
			.add(ModBlocks.CHESTNUT_LOG, Blocks.OAK_LOG, Blocks.SPRUCE_LOG);
		valueLookupBuilder(ModTags.HEWN_BEAMS)
			.add(
				ModBlocks.HEWN_CHESTNUT_BEAM,
				ModBlocks.HEWN_OAK_BEAM,
				ModBlocks.HEWN_SPRUCE_BEAM
			);
		valueLookupBuilder(ModTags.SAWABLE_BEAMS)
			.add(
				ModBlocks.HEWN_CHESTNUT_BEAM,
				ModBlocks.HEWN_OAK_BEAM,
				ModBlocks.HEWN_SPRUCE_BEAM
			);
		valueLookupBuilder(ModTags.ROUGH_BOARDS)
			.add(
				ModBlocks.ROUGH_CHESTNUT_BOARDS,
				ModBlocks.ROUGH_OAK_BOARDS,
				ModBlocks.ROUGH_SPRUCE_BOARDS
			);
		valueLookupBuilder(BlockTags.MINEABLE_WITH_AXE)
			.add(
				ModBlocks.CHESTNUT_LOG,
				ModBlocks.CHESTNUT_WOOD,
				ModBlocks.STRIPPED_CHESTNUT_LOG,
				ModBlocks.STRIPPED_CHESTNUT_WOOD,
				ModBlocks.CHESTNUT_HEWING_LOG,
				ModBlocks.HEWN_CHESTNUT_BEAM,
				ModBlocks.HEWN_CHESTNUT_WALL,
				ModBlocks.HEWN_CHESTNUT_POST,
				ModBlocks.CHESTNUT_PLANKS,
				ModBlocks.ROUGH_CHESTNUT_BOARDS,
				ModBlocks.ROUGH_CHESTNUT_BOARD_STAIRS,
				ModBlocks.ROUGH_CHESTNUT_OPEN_STAIRCASE,
				ModBlocks.ROUGH_CHESTNUT_BOARD_SLAB,
				ModBlocks.ROCKING_CHAIR,
				ModBlocks.CHESTNUT_SHINGLES,
				ModBlocks.CHESTNUT_SHINGLE_STAIRS,
				ModBlocks.CHESTNUT_SHINGLE_SLAB,
				ModBlocks.SPLIT_CHESTNUT_RAILS,
				ModBlocks.CHINKED_CHESTNUT_LOGS,
				ModBlocks.OAK_HEWING_LOG,
				ModBlocks.HEWN_OAK_BEAM,
				ModBlocks.ROUGH_OAK_BOARDS,
				ModBlocks.SPRUCE_HEWING_LOG,
				ModBlocks.HEWN_SPRUCE_BEAM,
				ModBlocks.ROUGH_SPRUCE_BOARDS,
				ModBlocks.SAWING_TRESTLES,
				ModBlocks.LOADED_SAWING_TRESTLES,
				ModBlocks.SPLITTING_STUMP,
				ModBlocks.LOADED_SPLITTING_STUMP,
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

		valueLookupBuilder(BlockTags.PLANKS).add(
			ModBlocks.CHESTNUT_PLANKS,
			ModBlocks.CHESTNUT_SHINGLES,
			ModBlocks.HEMLOCK_PLANKS,
			ModBlocks.AMERICAN_BEECH_PLANKS
		);
		valueLookupBuilder(BlockTags.PLANKS).add(
			ModBlocks.ROUGH_CHESTNUT_BOARDS,
			ModBlocks.ROUGH_OAK_BOARDS,
			ModBlocks.ROUGH_SPRUCE_BOARDS
		);
		valueLookupBuilder(BlockTags.LEAVES).add(
			ModBlocks.CHESTNUT_LEAVES,
			ModBlocks.HEMLOCK_FOLIAGE,
			ModBlocks.AMERICAN_BEECH_LEAVES,
			ModBlocks.BLACK_WALNUT_LEAVES
		);
		valueLookupBuilder(BlockTags.MINEABLE_WITH_HOE).add(
			ModBlocks.CHESTNUT_LEAVES,
			ModBlocks.HEMLOCK_FOLIAGE,
			ModBlocks.AMERICAN_BEECH_LEAVES,
			ModBlocks.BLACK_WALNUT_LEAVES,
			ModBlocks.MOUNTAIN_LAUREL,
			ModBlocks.LOWBUSH_BLUEBERRY,
			ModBlocks.FOREST_DUFF
		);
		valueLookupBuilder(BlockTags.SAPLINGS).add(
			ModBlocks.CHESTNUT_SAPLING,
			ModBlocks.HEMLOCK_SAPLING,
			ModBlocks.AMERICAN_BEECH_SAPLING,
			ModBlocks.BLACK_WALNUT_SAPLING
		);
		valueLookupBuilder(BlockTags.CROPS).add(ModBlocks.CORN);
		valueLookupBuilder(SERENE_SEASONS_SPRING_CROPS).setReplace(false).add(
			ModBlocks.CORN,
			ModBlocks.CHESTNUT_SAPLING,
			ModBlocks.HEMLOCK_SAPLING,
			ModBlocks.AMERICAN_BEECH_SAPLING,
			ModBlocks.BLACK_WALNUT_SAPLING
		);
		valueLookupBuilder(SERENE_SEASONS_SUMMER_CROPS).setReplace(false).add(
			ModBlocks.CORN,
			ModBlocks.CHESTNUT_SAPLING,
			ModBlocks.HEMLOCK_SAPLING,
			ModBlocks.AMERICAN_BEECH_SAPLING,
			ModBlocks.BLACK_WALNUT_SAPLING
		);
		valueLookupBuilder(SERENE_SEASONS_AUTUMN_CROPS).setReplace(false).add(
			ModBlocks.CORN
		);
		valueLookupBuilder(SERENE_SEASONS_UNBREAKABLE_INFERTILE_CROPS).setReplace(false).add(
			ModBlocks.CHESTNUT_SAPLING,
			ModBlocks.HEMLOCK_SAPLING,
			ModBlocks.AMERICAN_BEECH_SAPLING,
			ModBlocks.BLACK_WALNUT_SAPLING
		);
		valueLookupBuilder(BlockTags.WOODEN_STAIRS).add(
			ModBlocks.CHESTNUT_STAIRS,
			ModBlocks.ROUGH_CHESTNUT_BOARD_STAIRS,
			ModBlocks.ROUGH_CHESTNUT_OPEN_STAIRCASE,
			ModBlocks.CHESTNUT_SHINGLE_STAIRS,
			ModBlocks.HEMLOCK_STAIRS,
			ModBlocks.AMERICAN_BEECH_STAIRS
		);
		valueLookupBuilder(BlockTags.WOODEN_SLABS).add(
			ModBlocks.CHESTNUT_SLAB,
			ModBlocks.ROUGH_CHESTNUT_BOARD_SLAB,
			ModBlocks.CHESTNUT_SHINGLE_SLAB,
			ModBlocks.HEMLOCK_SLAB,
			ModBlocks.AMERICAN_BEECH_SLAB
		);
		valueLookupBuilder(BlockTags.WOODEN_FENCES).add(
			ModBlocks.CHESTNUT_FENCE,
			ModBlocks.SPLIT_CHESTNUT_RAILS,
			ModBlocks.HEMLOCK_FENCE,
			ModBlocks.AMERICAN_BEECH_FENCE
		);
		valueLookupBuilder(BlockTags.FENCES).add(ModBlocks.SPLIT_CHESTNUT_RAILS);
		valueLookupBuilder(BlockTags.MINEABLE_WITH_PICKAXE).add(
			ModBlocks.FIELDSTONE,
			ModBlocks.FIELDSTONE_STAIRS,
			ModBlocks.FIELDSTONE_SLAB,
			ModBlocks.FIELDSTONE_WALL,
			ModBlocks.DRESSED_FIELDSTONE,
			ModBlocks.DRESSED_FIELDSTONE_STAIRS,
			ModBlocks.DRESSED_FIELDSTONE_SLAB,
			ModBlocks.DRESSED_FIELDSTONE_WALL,
			ModBlocks.CHISELED_FIELDSTONE,
			ModBlocks.FIELDSTONE_PIER
		);
		valueLookupBuilder(BlockTags.STAIRS).add(
			ModBlocks.FIELDSTONE_STAIRS,
			ModBlocks.DRESSED_FIELDSTONE_STAIRS,
			ModBlocks.ROUGH_CHESTNUT_OPEN_STAIRCASE
		);
		valueLookupBuilder(BlockTags.SLABS).add(
			ModBlocks.FIELDSTONE_SLAB,
			ModBlocks.DRESSED_FIELDSTONE_SLAB
		);
		valueLookupBuilder(BlockTags.WALLS).add(
			ModBlocks.FIELDSTONE_WALL,
			ModBlocks.DRESSED_FIELDSTONE_WALL,
			ModBlocks.HEWN_CHESTNUT_WALL
		);
		valueLookupBuilder(BlockTags.FENCE_GATES).add(
			ModBlocks.CHESTNUT_FENCE_GATE,
			ModBlocks.HEMLOCK_FENCE_GATE,
			ModBlocks.AMERICAN_BEECH_FENCE_GATE
		);
		valueLookupBuilder(BlockTags.WOODEN_PRESSURE_PLATES).add(
			ModBlocks.CHESTNUT_PRESSURE_PLATE,
			ModBlocks.HEMLOCK_PRESSURE_PLATE,
			ModBlocks.AMERICAN_BEECH_PRESSURE_PLATE
		);
		valueLookupBuilder(BlockTags.WOODEN_BUTTONS).add(
			ModBlocks.CHESTNUT_BUTTON,
			ModBlocks.HEMLOCK_BUTTON,
			ModBlocks.AMERICAN_BEECH_BUTTON
		);

		addPolishTags();
	}

	private void addPolishTags() {
		// Tool semantics consume each isolated registrar's canonical lists.
		addBlocks(BlockTags.MINEABLE_WITH_AXE, ModRegionalWoodBlocks.ALL_BLOCKS);
		valueLookupBuilder(BlockTags.LOGS).add(
			ModRegionalWoodBlocks.YELLOW_POPLAR_LOG,
			ModRegionalWoodBlocks.STRIPPED_YELLOW_POPLAR_LOG,
			ModRegionalWoodBlocks.WHITE_OAK_LOG,
			ModRegionalWoodBlocks.STRIPPED_WHITE_OAK_LOG
		);
		valueLookupBuilder(BlockTags.LOGS_THAT_BURN).add(
			ModRegionalWoodBlocks.YELLOW_POPLAR_LOG,
			ModRegionalWoodBlocks.STRIPPED_YELLOW_POPLAR_LOG,
			ModRegionalWoodBlocks.WHITE_OAK_LOG,
			ModRegionalWoodBlocks.STRIPPED_WHITE_OAK_LOG
		);
		addBlocks(BlockTags.MINEABLE_WITH_AXE, ModBoardRoofBlocks.ALL_BLOCKS);
		addBlocks(BlockTags.MINEABLE_WITH_AXE, ModTimberBlocks.AXE_MINEABLE);
		addBlocks(BlockTags.MINEABLE_WITH_SHOVEL, ModTimberBlocks.SHOVEL_MINEABLE);
		addBlocks(
			BlockTags.MINEABLE_WITH_AXE,
			ModDoorWindowBlocks.AXE_MINEABLE_BLOCKS
		);
		addBlocks(
			BlockTags.MINEABLE_WITH_PICKAXE,
			ModDoorWindowBlocks.PICKAXE_MINEABLE_BLOCKS
		);
		addBlocks(
			BlockTags.MINEABLE_WITH_AXE,
			ModFurnitureBlocks.ALL_ITEMS.stream().map(Block::byItem).toList()
		);
		valueLookupBuilder(BlockTags.CLIMBABLE).add(
			ModFurnitureBlocks.ROUGH_BOARD_LADDER
		);
		valueLookupBuilder(BlockTags.MINEABLE_WITH_AXE).add(
			ModDomesticBlocks.WOODEN_BOWL,
			ModDomesticBlocks.WOODEN_TRENCHER,
			ModDomesticBlocks.WOODEN_CUP,
			ModDomesticBlocks.WOODEN_SPOON,
			ModDomesticBlocks.WOODEN_LADLE,
			ModDomesticBlocks.WOVEN_BASKET,
			ModDomesticBlocks.WOODEN_BUCKET,
			ModDomesticBlocks.WOODEN_PAIL,
			ModDomesticBlocks.WOODEN_WASH_TUB,
			ModDomesticBlocks.WASHBOARD,
			ModDomesticBlocks.FIDDLE
		);
		valueLookupBuilder(BlockTags.MINEABLE_WITH_HOE).add(
			ModDomesticBlocks.DRIED_APPLE_RINGS,
			ModDomesticBlocks.HANGING_DRIED_APPLES,
			ModDomesticBlocks.DRIED_HERB_BUNCH,
			ModDomesticBlocks.HANGING_ONIONS,
			ModDomesticBlocks.HANGING_GARLIC
		);
		addBlocks(
			BlockTags.MINEABLE_WITH_AXE,
			ModCornCribBlocks.AXE_MINEABLE_BLOCKS
		);
		addBlocks(
			BlockTags.MINEABLE_WITH_HOE,
			ModCornCribBlocks.HOE_MINEABLE_BLOCKS
		);
		addBlocks(BlockTags.MINEABLE_WITH_PICKAXE, ModStoneHearthBlocks.PICKAXE_MINEABLE);
		addBlocks(BlockTags.MINEABLE_WITH_SHOVEL, ModStoneHearthBlocks.SHOVEL_MINEABLE);
		addBlocks(BlockTags.MINEABLE_WITH_AXE, ModStoneHearthBlocks.AXE_MINEABLE);
		addBlocks(BlockTags.MINEABLE_WITH_AXE, ModExteriorBlocks.FLAMMABLE_WOOD_BLOCKS);

		valueLookupBuilder(BlockTags.MINEABLE_WITH_PICKAXE).add(
			ModDomesticBlocks.CAST_IRON_POT,
			ModDomesticBlocks.IRON_KETTLE,
			ModDomesticBlocks.SPIDER_SKILLET,
			ModDomesticBlocks.DUTCH_OVEN,
			ModDomesticBlocks.STONEWARE_CROCK,
			ModDomesticBlocks.STONEWARE_JUG,
			ModDomesticBlocks.CERAMIC_PITCHER,
			ModDomesticBlocks.TIN_CUP,
			ModDomesticBlocks.TIN_PLATE,
			ModDomesticBlocks.BETTY_LAMP,
			ModSpringhouseBlocks.STONEWARE_MILK_CROCK,
			ModSpringhouseBlocks.SHALLOW_MILK_PAN,
			ModSpringhouseBlocks.BUTTER_CROCK,
			ModSpringhouseBlocks.DAMP_FIELDSTONE,
			ModSpringhouseBlocks.MOSSY_FIELDSTONE,
			ModExteriorBlocks.STREAM_BANK_STONES
		);
		valueLookupBuilder(BlockTags.MINEABLE_WITH_SHOVEL).add(
			ModSpringhouseBlocks.DAMP_EARTH,
			ModExteriorBlocks.PACKED_DIRT_PATH,
			ModExteriorBlocks.PATH_EDGE,
			ModExteriorBlocks.WAGON_RUT,
			ModExteriorBlocks.MUDDY_WAGON_RUT,
			ModExteriorBlocks.MOSS_PATCH,
			ModExteriorBlocks.LEAF_LITTER
		);
		valueLookupBuilder(BlockTags.MINEABLE_WITH_AXE).add(
			ModSpringhouseBlocks.HOLLOWED_CHESTNUT_TROUGH,
			ModSpringhouseBlocks.HOLLOW_LIMB_SPOUT,
			ModSpringhouseBlocks.DAIRY_WORK_TABLE,
			ModSpringhouseBlocks.CROCK_STAND,
			ModSpringhouseBlocks.WOODEN_LOUVER,
			ModSpringhouseBlocks.WOODEN_MILK_PAIL,
			ModSpringhouseBlocks.DASHER_CHURN
		);

		// Vanilla family tags control connections and recipe compatibility.
		addBlocks(BlockTags.WOODEN_STAIRS, ModBoardRoofBlocks.STAIR_BLOCKS);
		addBlocks(BlockTags.STAIRS, ModBoardRoofBlocks.STAIR_BLOCKS);
		addBlocks(BlockTags.WOODEN_SLABS, ModBoardRoofBlocks.SLAB_BLOCKS);
		addBlocks(BlockTags.SLABS, ModBoardRoofBlocks.SLAB_BLOCKS);
		addBlocks(BlockTags.TRAPDOORS, ModBoardRoofBlocks.TRAPDOOR_BLOCKS);

		addBlocks(BlockTags.DOORS, ModDoorWindowBlocks.DOOR_BLOCKS);
		addBlocks(BlockTags.TRAPDOORS, ModDoorWindowBlocks.HATCH_BLOCKS);
		valueLookupBuilder(BlockTags.BEDS).add(ModFurnitureBlocks.ROPE_BED);

		valueLookupBuilder(BlockTags.SLABS).add(
			ModStoneHearthBlocks.FIELDSTONE_HEARTH,
			ModStoneHearthBlocks.FIELDSTONE_CHIMNEY_CAP
		);
		valueLookupBuilder(BlockTags.STAIRS).add(
			ModStoneHearthBlocks.FIELDSTONE_CHIMNEY_SHOULDER
		);
		valueLookupBuilder(BlockTags.WALLS).add(
			ModStoneHearthBlocks.FIELDSTONE_FIREBOX_WALL,
			ModStoneHearthBlocks.FIELDSTONE_RETAINING_WALL
		);

		valueLookupBuilder(BlockTags.WOODEN_FENCES).add(
			ModExteriorBlocks.SPLIT_RAIL_FENCE,
			ModExteriorBlocks.WEATHERED_SPLIT_RAIL_FENCE,
			ModExteriorBlocks.BROKEN_SPLIT_RAIL_FENCE,
			ModExteriorBlocks.PEELED_POLE_FENCE
		);
		valueLookupBuilder(BlockTags.FENCES).add(
			ModExteriorBlocks.SPLIT_RAIL_FENCE,
			ModExteriorBlocks.WEATHERED_SPLIT_RAIL_FENCE,
			ModExteriorBlocks.BROKEN_SPLIT_RAIL_FENCE,
			ModExteriorBlocks.PEELED_POLE_FENCE
		);
		valueLookupBuilder(BlockTags.FENCE_GATES).add(
			ModExteriorBlocks.SPLIT_RAIL_GATE,
			ModExteriorBlocks.PEELED_POLE_GATE
		);
	}

	private void addBlocks(TagKey<Block> tag, List<Block> blocks) {
		valueLookupBuilder(tag).addAll(blocks);
	}

	private static TagKey<Block> sereneSeasonsCropTag(String path) {
		return TagKey.create(
			Registries.BLOCK,
			Identifier.fromNamespaceAndPath("sereneseasons", path)
		);
	}
}
