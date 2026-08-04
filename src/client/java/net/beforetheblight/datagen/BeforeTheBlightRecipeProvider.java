package net.beforetheblight.datagen;

import java.util.concurrent.CompletableFuture;

import net.beforetheblight.registry.ModBlocks;
import net.beforetheblight.registry.ModItems;
import net.beforetheblight.registry.ModTags;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;

public final class BeforeTheBlightRecipeProvider extends FabricRecipeProvider {
	public BeforeTheBlightRecipeProvider(
		FabricPackOutput output,
		CompletableFuture<HolderLookup.Provider> registriesFuture
	) {
		super(output, registriesFuture);
	}

	@Override
	protected RecipeProvider createRecipeProvider(
		HolderLookup.Provider registries,
		RecipeOutput output
	) {
		return new RecipeProvider(registries, output) {
			@Override
			public void buildRecipes() {
        // Survival bootstrap for homestead and crop-loot onboarding.
				shapeless(RecipeCategory.MISC, ModItems.CORN_KERNELS)
					.requires(Items.WHEAT_SEEDS)
					.requires(Items.YELLOW_DYE)
					.unlockedBy(getHasName(Items.WHEAT_SEEDS), has(Items.WHEAT_SEEDS))
					.save(output);

				shapeless(RecipeCategory.FOOD, ModItems.CORNMEAL, 3)
					.requires(ModItems.EAR_OF_CORN)
					.unlockedBy("has_ear_of_corn", has(ModItems.EAR_OF_CORN))
					.save(output);

				shapeless(RecipeCategory.FOOD, Items.BREAD)
					.requires(ModItems.CORNMEAL, 3)
					.unlockedBy("has_cornmeal", has(ModItems.CORNMEAL))
					.save(output, "before_the_blight:corn_bread");

				shaped(RecipeCategory.TOOLS, ModItems.BROAD_AXE)
					.define('I', Items.IRON_INGOT)
					.define('S', Items.STICK)
					.pattern("III")
					.pattern("IS ")
					.pattern(" S ")
					.unlockedBy("has_chestnut_log", has(ModTags.CHESTNUT_LOG_ITEMS))
					.save(output);

				shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.SAWING_TRESTLES)
					.define('B', ItemTags.PLANKS)
					.define('S', Items.STICK)
					.pattern("B B")
					.pattern("SSS")
					.unlockedBy("has_hewn_beam", has(ModTags.HEWN_BEAM_ITEMS))
					.save(output);

				shaped(RecipeCategory.TOOLS, ModItems.FRAME_SAW)
					.define('B', ItemTags.PLANKS)
					.define('I', Items.IRON_INGOT)
					.define('S', Items.STRING)
					.pattern("BIB")
					.pattern(" S ")
					.pattern("BIB")
					.unlockedBy("has_hewn_beam", has(ModTags.HEWN_BEAM_ITEMS))
					.save(output);

				shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.SPLITTING_STUMP)
					.define('B', ModTags.ROUGH_BOARD_ITEMS)
					.define('L', ModTags.CHESTNUT_LOG_ITEMS)
					.pattern(" B ")
					.pattern("BLB")
					.pattern(" B ")
					.unlockedBy("has_rough_boards", has(ModTags.ROUGH_BOARD_ITEMS))
					.save(output);

				shaped(RecipeCategory.DECORATIONS, ModBlocks.ROCKING_CHAIR)
					.define('B', ModBlocks.ROUGH_CHESTNUT_BOARDS)
					.define('S', Items.STICK)
					.define('W', Items.STRING)
					.pattern("B B")
					.pattern("BWB")
					.pattern("S S")
					.unlockedBy(
						"has_rough_chestnut_boards",
						has(ModBlocks.ROUGH_CHESTNUT_BOARDS)
					)
					.save(output);

				shaped(RecipeCategory.TOOLS, ModItems.FROE)
					.define('I', Items.IRON_INGOT)
					.define('S', Items.STICK)
					.pattern("II")
					.pattern(" S")
					.unlockedBy("has_rough_boards", has(ModTags.ROUGH_BOARD_ITEMS))
					.save(output);

				shaped(RecipeCategory.TOOLS, ModItems.WOODEN_MAUL)
					.define('L', ModTags.CHESTNUT_LOG_ITEMS)
					.define('S', Items.STICK)
					.pattern("LLL")
					.pattern(" LS")
					.pattern("  S")
					.unlockedBy("has_rough_boards", has(ModTags.ROUGH_BOARD_ITEMS))
					.save(output);

				planksFromLogs(
					ModBlocks.CHESTNUT_PLANKS,
					ModTags.CHESTNUT_LOG_ITEMS,
					4
				);
				woodFromLogs(ModBlocks.CHESTNUT_WOOD, ModBlocks.CHESTNUT_LOG);
				woodFromLogs(ModBlocks.STRIPPED_CHESTNUT_WOOD, ModBlocks.STRIPPED_CHESTNUT_LOG);
				planksFromLogs(
					ModBlocks.HEMLOCK_PLANKS,
					ModTags.HEMLOCK_LOG_ITEMS,
					4
				);
				woodFromLogs(ModBlocks.HEMLOCK_WOOD, ModBlocks.HEMLOCK_LOG);
				woodFromLogs(ModBlocks.STRIPPED_HEMLOCK_WOOD, ModBlocks.STRIPPED_HEMLOCK_LOG);
				planksFromLogs(
					ModBlocks.AMERICAN_BEECH_PLANKS,
					ModTags.AMERICAN_BEECH_LOG_ITEMS,
					4
				);
				woodFromLogs(ModBlocks.AMERICAN_BEECH_WOOD, ModBlocks.AMERICAN_BEECH_LOG);
				woodFromLogs(
					ModBlocks.STRIPPED_AMERICAN_BEECH_WOOD,
					ModBlocks.STRIPPED_AMERICAN_BEECH_LOG
				);
				woodFromLogs(ModBlocks.BLACK_WALNUT_WOOD, ModBlocks.BLACK_WALNUT_LOG);
				woodFromLogs(
					ModBlocks.STRIPPED_BLACK_WALNUT_WOOD,
					ModBlocks.STRIPPED_BLACK_WALNUT_LOG
				);

				Ingredient chestnutPlanks = Ingredient.of(ModBlocks.CHESTNUT_PLANKS);
				stairBuilder(ModBlocks.CHESTNUT_STAIRS, chestnutPlanks)
					.group("wooden_stairs")
					.unlockedBy(getHasName(ModBlocks.CHESTNUT_PLANKS), has(ModBlocks.CHESTNUT_PLANKS))
					.save(output);
				slabBuilder(
					RecipeCategory.BUILDING_BLOCKS,
					ModBlocks.CHESTNUT_SLAB,
					chestnutPlanks
				)
					.group("wooden_slab")
					.unlockedBy(getHasName(ModBlocks.CHESTNUT_PLANKS), has(ModBlocks.CHESTNUT_PLANKS))
					.save(output);
				fenceBuilder(ModBlocks.CHESTNUT_FENCE, chestnutPlanks)
					.group("wooden_fence")
					.unlockedBy(getHasName(ModBlocks.CHESTNUT_PLANKS), has(ModBlocks.CHESTNUT_PLANKS))
					.save(output);
				fenceGateBuilder(ModBlocks.CHESTNUT_FENCE_GATE, chestnutPlanks)
					.group("wooden_fence_gate")
					.unlockedBy(getHasName(ModBlocks.CHESTNUT_PLANKS), has(ModBlocks.CHESTNUT_PLANKS))
					.save(output);
				shaped(RecipeCategory.REDSTONE, ModBlocks.CHESTNUT_PRESSURE_PLATE)
					.define('#', chestnutPlanks)
					.pattern("##")
					.group("wooden_pressure_plate")
					.unlockedBy(getHasName(ModBlocks.CHESTNUT_PLANKS), has(ModBlocks.CHESTNUT_PLANKS))
					.save(output);
				buttonBuilder(ModBlocks.CHESTNUT_BUTTON, chestnutPlanks)
					.group("wooden_button")
					.unlockedBy(getHasName(ModBlocks.CHESTNUT_PLANKS), has(ModBlocks.CHESTNUT_PLANKS))
					.save(output);

				Ingredient hemlockPlanks = Ingredient.of(ModBlocks.HEMLOCK_PLANKS);
				stairBuilder(ModBlocks.HEMLOCK_STAIRS, hemlockPlanks)
					.group("wooden_stairs")
					.unlockedBy(getHasName(ModBlocks.HEMLOCK_PLANKS), has(ModBlocks.HEMLOCK_PLANKS))
					.save(output);
				slabBuilder(
					RecipeCategory.BUILDING_BLOCKS,
					ModBlocks.HEMLOCK_SLAB,
					hemlockPlanks
				)
					.group("wooden_slab")
					.unlockedBy(getHasName(ModBlocks.HEMLOCK_PLANKS), has(ModBlocks.HEMLOCK_PLANKS))
					.save(output);
				fenceBuilder(ModBlocks.HEMLOCK_FENCE, hemlockPlanks)
					.group("wooden_fence")
					.unlockedBy(getHasName(ModBlocks.HEMLOCK_PLANKS), has(ModBlocks.HEMLOCK_PLANKS))
					.save(output);
				fenceGateBuilder(ModBlocks.HEMLOCK_FENCE_GATE, hemlockPlanks)
					.group("wooden_fence_gate")
					.unlockedBy(getHasName(ModBlocks.HEMLOCK_PLANKS), has(ModBlocks.HEMLOCK_PLANKS))
					.save(output);
				shaped(RecipeCategory.REDSTONE, ModBlocks.HEMLOCK_PRESSURE_PLATE)
					.define('#', hemlockPlanks)
					.pattern("##")
					.group("wooden_pressure_plate")
					.unlockedBy(getHasName(ModBlocks.HEMLOCK_PLANKS), has(ModBlocks.HEMLOCK_PLANKS))
					.save(output);
				buttonBuilder(ModBlocks.HEMLOCK_BUTTON, hemlockPlanks)
					.group("wooden_button")
					.unlockedBy(getHasName(ModBlocks.HEMLOCK_PLANKS), has(ModBlocks.HEMLOCK_PLANKS))
					.save(output);

				Ingredient beechPlanks = Ingredient.of(ModBlocks.AMERICAN_BEECH_PLANKS);
				stairBuilder(ModBlocks.AMERICAN_BEECH_STAIRS, beechPlanks)
					.group("wooden_stairs")
					.unlockedBy(
						getHasName(ModBlocks.AMERICAN_BEECH_PLANKS),
						has(ModBlocks.AMERICAN_BEECH_PLANKS)
					)
					.save(output);
				slabBuilder(
					RecipeCategory.BUILDING_BLOCKS,
					ModBlocks.AMERICAN_BEECH_SLAB,
					beechPlanks
				)
					.group("wooden_slab")
					.unlockedBy(
						getHasName(ModBlocks.AMERICAN_BEECH_PLANKS),
						has(ModBlocks.AMERICAN_BEECH_PLANKS)
					)
					.save(output);
				fenceBuilder(ModBlocks.AMERICAN_BEECH_FENCE, beechPlanks)
					.group("wooden_fence")
					.unlockedBy(
						getHasName(ModBlocks.AMERICAN_BEECH_PLANKS),
						has(ModBlocks.AMERICAN_BEECH_PLANKS)
					)
					.save(output);
				fenceGateBuilder(ModBlocks.AMERICAN_BEECH_FENCE_GATE, beechPlanks)
					.group("wooden_fence_gate")
					.unlockedBy(
						getHasName(ModBlocks.AMERICAN_BEECH_PLANKS),
						has(ModBlocks.AMERICAN_BEECH_PLANKS)
					)
					.save(output);
				shaped(RecipeCategory.REDSTONE, ModBlocks.AMERICAN_BEECH_PRESSURE_PLATE)
					.define('#', beechPlanks)
					.pattern("##")
					.group("wooden_pressure_plate")
					.unlockedBy(
						getHasName(ModBlocks.AMERICAN_BEECH_PLANKS),
						has(ModBlocks.AMERICAN_BEECH_PLANKS)
					)
					.save(output);
				buttonBuilder(ModBlocks.AMERICAN_BEECH_BUTTON, beechPlanks)
					.group("wooden_button")
					.unlockedBy(
						getHasName(ModBlocks.AMERICAN_BEECH_PLANKS),
						has(ModBlocks.AMERICAN_BEECH_PLANKS)
					)
					.save(output);

				Ingredient roughChestnutBoards = Ingredient.of(ModBlocks.ROUGH_CHESTNUT_BOARDS);
				stairBuilder(ModBlocks.ROUGH_CHESTNUT_BOARD_STAIRS, roughChestnutBoards)
					.group("wooden_stairs")
					.unlockedBy("has_rough_chestnut_boards", has(ModBlocks.ROUGH_CHESTNUT_BOARDS))
					.save(output);
				shaped(
					RecipeCategory.BUILDING_BLOCKS,
					ModBlocks.ROUGH_CHESTNUT_OPEN_STAIRCASE,
					2
				)
					.define('B', ModBlocks.ROUGH_CHESTNUT_BOARDS)
					.define('S', Items.STICK)
					.pattern("BBB")
					.pattern("S S")
					.unlockedBy(
						"has_rough_chestnut_boards",
						has(ModBlocks.ROUGH_CHESTNUT_BOARDS)
					)
					.save(output);
				slabBuilder(
					RecipeCategory.BUILDING_BLOCKS,
					ModBlocks.ROUGH_CHESTNUT_BOARD_SLAB,
					roughChestnutBoards
				)
					.group("wooden_slab")
					.unlockedBy("has_rough_chestnut_boards", has(ModBlocks.ROUGH_CHESTNUT_BOARDS))
					.save(output);

				Ingredient chestnutShingles = Ingredient.of(ModBlocks.CHESTNUT_SHINGLES);
				stairBuilder(ModBlocks.CHESTNUT_SHINGLE_STAIRS, chestnutShingles)
					.group("wooden_stairs")
					.unlockedBy(getHasName(ModBlocks.CHESTNUT_SHINGLES), has(ModBlocks.CHESTNUT_SHINGLES))
					.save(output);
				slabBuilder(
					RecipeCategory.BUILDING_BLOCKS,
					ModBlocks.CHESTNUT_SHINGLE_SLAB,
					chestnutShingles
				)
					.group("wooden_slab")
					.unlockedBy(getHasName(ModBlocks.CHESTNUT_SHINGLES), has(ModBlocks.CHESTNUT_SHINGLES))
					.save(output);
				shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.CHINKED_CHESTNUT_LOGS, 6)
					.define('L', ModTags.CHESTNUT_LOG_ITEMS)
					.define('C', Items.CLAY_BALL)
					.pattern("LLL")
					.pattern("CCC")
					.pattern("LLL")
					.unlockedBy("has_chestnut_logs", has(ModTags.CHESTNUT_LOG_ITEMS))
					.save(output);

				shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.FIELDSTONE, 4)
					.define('#', Items.COBBLESTONE)
					.pattern("##")
					.pattern("##")
					.unlockedBy(getHasName(Items.COBBLESTONE), has(Items.COBBLESTONE))
					.save(output);
				Ingredient fieldstone = Ingredient.of(ModBlocks.FIELDSTONE);
				stairBuilder(ModBlocks.FIELDSTONE_STAIRS, fieldstone)
					.unlockedBy(getHasName(ModBlocks.FIELDSTONE), has(ModBlocks.FIELDSTONE))
					.save(output);
				slabBuilder(RecipeCategory.BUILDING_BLOCKS, ModBlocks.FIELDSTONE_SLAB, fieldstone)
					.unlockedBy(getHasName(ModBlocks.FIELDSTONE), has(ModBlocks.FIELDSTONE))
					.save(output);
				wallBuilder(RecipeCategory.DECORATIONS, ModBlocks.FIELDSTONE_WALL, fieldstone)
					.unlockedBy(getHasName(ModBlocks.FIELDSTONE), has(ModBlocks.FIELDSTONE))
					.save(output);

				shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.DRESSED_FIELDSTONE, 4)
					.define('#', ModBlocks.FIELDSTONE)
					.pattern("##")
					.pattern("##")
					.unlockedBy(getHasName(ModBlocks.FIELDSTONE), has(ModBlocks.FIELDSTONE))
					.save(output);
				Ingredient dressedFieldstone = Ingredient.of(ModBlocks.DRESSED_FIELDSTONE);
				stairBuilder(ModBlocks.DRESSED_FIELDSTONE_STAIRS, dressedFieldstone)
					.unlockedBy(
						getHasName(ModBlocks.DRESSED_FIELDSTONE),
						has(ModBlocks.DRESSED_FIELDSTONE)
					)
					.save(output);
				slabBuilder(
					RecipeCategory.BUILDING_BLOCKS,
					ModBlocks.DRESSED_FIELDSTONE_SLAB,
					dressedFieldstone
				)
					.unlockedBy(
						getHasName(ModBlocks.DRESSED_FIELDSTONE),
						has(ModBlocks.DRESSED_FIELDSTONE)
					)
					.save(output);
				wallBuilder(
					RecipeCategory.DECORATIONS,
					ModBlocks.DRESSED_FIELDSTONE_WALL,
					dressedFieldstone
				)
					.unlockedBy(
						getHasName(ModBlocks.DRESSED_FIELDSTONE),
						has(ModBlocks.DRESSED_FIELDSTONE)
					)
					.save(output);
				shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.CHISELED_FIELDSTONE)
					.define('#', ModBlocks.DRESSED_FIELDSTONE_SLAB)
					.pattern("#")
					.pattern("#")
					.unlockedBy(
						getHasName(ModBlocks.DRESSED_FIELDSTONE_SLAB),
						has(ModBlocks.DRESSED_FIELDSTONE_SLAB)
					)
					.save(output);
				shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.FIELDSTONE_PIER, 2)
					.define('#', ModBlocks.DRESSED_FIELDSTONE)
					.pattern("#")
					.pattern("#")
					.unlockedBy(
						getHasName(ModBlocks.DRESSED_FIELDSTONE),
						has(ModBlocks.DRESSED_FIELDSTONE)
					)
					.save(output);

				simpleCookingRecipe(
					"smelting",
					SmeltingRecipe::new,
					200,
					ModItems.HANDFUL_OF_CHESTNUTS,
					ModItems.ROASTED_CHESTNUTS,
					0.1F
				);
				simpleCookingRecipe(
					"smoking",
					SmokingRecipe::new,
					100,
					ModItems.HANDFUL_OF_CHESTNUTS,
					ModItems.ROASTED_CHESTNUTS,
					0.1F
				);
				simpleCookingRecipe(
					"campfire_cooking",
					CampfireCookingRecipe::new,
					600,
					ModItems.HANDFUL_OF_CHESTNUTS,
					ModItems.ROASTED_CHESTNUTS,
					0.1F
				);
			}
		};
	}

	@Override
	public String getName() {
		return "Before the Blight Recipes";
	}
}
