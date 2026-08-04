package net.beforetheblight.datagen;

import java.util.concurrent.CompletableFuture;

import net.beforetheblight.registry.ModTags;
import net.beforetheblight.registry.ModItems;
import net.beforetheblight.registry.ModBlocks;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class BeforeTheBlightItemTagProvider extends FabricTagsProvider.ItemTagsProvider {
	private static final TagKey<Item> SERENE_SEASONS_SPRING_CROPS = sereneSeasonsCropTag("spring_crops");
	private static final TagKey<Item> SERENE_SEASONS_SUMMER_CROPS = sereneSeasonsCropTag("summer_crops");
	private static final TagKey<Item> SERENE_SEASONS_AUTUMN_CROPS = sereneSeasonsCropTag("autumn_crops");

	public BeforeTheBlightItemTagProvider(
		FabricPackOutput output,
		CompletableFuture<HolderLookup.Provider> registriesFuture,
		BeforeTheBlightBlockTagProvider blockTags
	) {
		super(output, registriesFuture, blockTags);
	}

	@Override
	protected void addTags(HolderLookup.Provider registries) {
		copy(
			ModTags.CHESTNUT_LOGS,
			ModTags.CHESTNUT_LOG_ITEMS
		);
		copy(ModTags.HEMLOCK_LOGS, ModTags.HEMLOCK_LOG_ITEMS);
		copy(ModTags.AMERICAN_BEECH_LOGS, ModTags.AMERICAN_BEECH_LOG_ITEMS);
		copy(ModTags.BLACK_WALNUT_LOGS, ModTags.BLACK_WALNUT_LOG_ITEMS);
		copy(
			ModTags.CHESTNUT_WOODEN_BLOCKS,
			ModTags.CHESTNUT_WOODEN_ITEMS
		);
		copy(ModTags.HEWING_LOGS, ModTags.HEWING_LOG_ITEMS);
		copy(ModTags.HEWN_BEAMS, ModTags.HEWN_BEAM_ITEMS);
		copy(ModTags.SAWABLE_BEAMS, ModTags.SAWABLE_BEAM_ITEMS);
		copy(ModTags.ROUGH_BOARDS, ModTags.ROUGH_BOARD_ITEMS);
		copy(BlockTags.LOGS_THAT_BURN, ItemTags.LOGS_THAT_BURN);
		copy(BlockTags.LOGS, ItemTags.LOGS);
		copy(BlockTags.PLANKS, ItemTags.PLANKS);
		copy(BlockTags.LEAVES, ItemTags.LEAVES);
		copy(BlockTags.SAPLINGS, ItemTags.SAPLINGS);
		copy(BlockTags.WOODEN_STAIRS, ItemTags.WOODEN_STAIRS);
		copy(BlockTags.WOODEN_SLABS, ItemTags.WOODEN_SLABS);
		copy(BlockTags.WOODEN_FENCES, ItemTags.WOODEN_FENCES);
		copy(BlockTags.FENCES, ItemTags.FENCES);
		copy(BlockTags.STAIRS, ItemTags.STAIRS);
		copy(BlockTags.SLABS, ItemTags.SLABS);
		copy(BlockTags.WALLS, ItemTags.WALLS);
		copy(BlockTags.FENCE_GATES, ItemTags.FENCE_GATES);
		copy(BlockTags.WOODEN_PRESSURE_PLATES, ItemTags.WOODEN_PRESSURE_PLATES);
		copy(BlockTags.WOODEN_BUTTONS, ItemTags.WOODEN_BUTTONS);
		copy(BlockTags.DOORS, ItemTags.DOORS);
		copy(BlockTags.TRAPDOORS, ItemTags.TRAPDOORS);
		copy(BlockTags.BEDS, ItemTags.BEDS);
		valueLookupBuilder(SERENE_SEASONS_SPRING_CROPS).setReplace(false).add(
			ModItems.CORN_KERNELS,
			ModBlocks.CHESTNUT_SAPLING.asItem(),
			ModBlocks.HEMLOCK_SAPLING.asItem(),
			ModBlocks.AMERICAN_BEECH_SAPLING.asItem(),
			ModBlocks.BLACK_WALNUT_SAPLING.asItem()
		);
		valueLookupBuilder(SERENE_SEASONS_SUMMER_CROPS).setReplace(false).add(
			ModItems.CORN_KERNELS,
			ModBlocks.CHESTNUT_SAPLING.asItem(),
			ModBlocks.HEMLOCK_SAPLING.asItem(),
			ModBlocks.AMERICAN_BEECH_SAPLING.asItem(),
			ModBlocks.BLACK_WALNUT_SAPLING.asItem()
		);
		valueLookupBuilder(SERENE_SEASONS_AUTUMN_CROPS).setReplace(false).add(
			ModItems.CORN_KERNELS
		);
		valueLookupBuilder(ItemTags.VILLAGER_PLANTABLE_SEEDS).add(ModItems.CORN_KERNELS);
	}

	private static TagKey<Item> sereneSeasonsCropTag(String path) {
		return TagKey.create(
			Registries.ITEM,
			Identifier.fromNamespaceAndPath("sereneseasons", path)
		);
	}
}
