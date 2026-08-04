package net.beforetheblight.datagen;

import java.util.concurrent.CompletableFuture;

import net.beforetheblight.block.LoadedSawingTrestlesBlock;
import net.beforetheblight.block.LoadedSplittingStumpBlock;
import net.beforetheblight.block.CornCropBlock;
import net.beforetheblight.interaction.TimberProcessingRegistry;
import net.beforetheblight.registry.ModBlocks;
import net.beforetheblight.registry.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider;
import net.minecraft.advancements.criterion.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.BonusLevelTableCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

public final class BeforeTheBlightBlockLootProvider extends FabricBlockLootSubProvider {
	public BeforeTheBlightBlockLootProvider(
		FabricPackOutput output,
		CompletableFuture<HolderLookup.Provider> registriesFuture
	) {
		super(output, registriesFuture);
	}

	@Override
	public void generate() {
		dropSelf(ModBlocks.CHESTNUT_LOG);
		dropSelf(ModBlocks.HEWN_CHESTNUT_BEAM);
		dropSelf(ModBlocks.HEWN_OAK_BEAM);
		dropSelf(ModBlocks.HEWN_SPRUCE_BEAM);
		dropSelf(ModBlocks.CHESTNUT_WOOD);
		dropSelf(ModBlocks.STRIPPED_CHESTNUT_LOG);
		dropSelf(ModBlocks.STRIPPED_CHESTNUT_WOOD);
		dropSelf(ModBlocks.CHESTNUT_PLANKS);
		dropSelf(ModBlocks.ROUGH_CHESTNUT_BOARDS);
		dropSelf(ModBlocks.ROUGH_CHESTNUT_BOARD_STAIRS);
		dropSelf(ModBlocks.ROUGH_CHESTNUT_OPEN_STAIRCASE);
		dropSelf(ModBlocks.ROCKING_CHAIR);
		dropSelf(ModBlocks.CHESTNUT_SHINGLES);
		dropSelf(ModBlocks.CHESTNUT_SHINGLE_STAIRS);
		dropSelf(ModBlocks.SPLIT_CHESTNUT_RAILS);
		dropSelf(ModBlocks.CHINKED_CHESTNUT_LOGS);
		dropSelf(ModBlocks.FIELDSTONE);
		dropSelf(ModBlocks.FIELDSTONE_STAIRS);
		dropSelf(ModBlocks.FIELDSTONE_WALL);
		dropSelf(ModBlocks.DRESSED_FIELDSTONE);
		dropSelf(ModBlocks.DRESSED_FIELDSTONE_STAIRS);
		dropSelf(ModBlocks.DRESSED_FIELDSTONE_WALL);
		dropSelf(ModBlocks.CHISELED_FIELDSTONE);
		dropSelf(ModBlocks.FIELDSTONE_PIER);
		dropSelf(ModBlocks.ROUGH_OAK_BOARDS);
		dropSelf(ModBlocks.ROUGH_SPRUCE_BOARDS);
		dropSelf(ModBlocks.SAWING_TRESTLES);
		dropSelf(ModBlocks.SPLITTING_STUMP);
		dropSelf(ModBlocks.CHESTNUT_SAPLING);
		dropSelf(ModBlocks.CHESTNUT_STAIRS);
		dropSelf(ModBlocks.CHESTNUT_FENCE);
		dropSelf(ModBlocks.CHESTNUT_FENCE_GATE);
		dropSelf(ModBlocks.CHESTNUT_PRESSURE_PLATE);
		dropSelf(ModBlocks.CHESTNUT_BUTTON);
		dropSelf(ModBlocks.MOUNTAIN_LAUREL);
		dropSelf(ModBlocks.LOWBUSH_BLUEBERRY);
		dropSelf(ModBlocks.FOREST_DUFF);
		dropSelf(ModBlocks.HEMLOCK_LOG);
		dropSelf(ModBlocks.HEMLOCK_WOOD);
		dropSelf(ModBlocks.STRIPPED_HEMLOCK_LOG);
		dropSelf(ModBlocks.STRIPPED_HEMLOCK_WOOD);
		dropSelf(ModBlocks.HEMLOCK_PLANKS);
		dropSelf(ModBlocks.HEMLOCK_STAIRS);
		dropSelf(ModBlocks.HEMLOCK_FENCE);
		dropSelf(ModBlocks.HEMLOCK_FENCE_GATE);
		dropSelf(ModBlocks.HEMLOCK_PRESSURE_PLATE);
		dropSelf(ModBlocks.HEMLOCK_BUTTON);
		dropSelf(ModBlocks.HEMLOCK_SAPLING);
		dropSelf(ModBlocks.AMERICAN_BEECH_LOG);
		dropSelf(ModBlocks.AMERICAN_BEECH_WOOD);
		dropSelf(ModBlocks.STRIPPED_AMERICAN_BEECH_LOG);
		dropSelf(ModBlocks.STRIPPED_AMERICAN_BEECH_WOOD);
		dropSelf(ModBlocks.AMERICAN_BEECH_PLANKS);
		dropSelf(ModBlocks.AMERICAN_BEECH_STAIRS);
		dropSelf(ModBlocks.AMERICAN_BEECH_FENCE);
		dropSelf(ModBlocks.AMERICAN_BEECH_FENCE_GATE);
		dropSelf(ModBlocks.AMERICAN_BEECH_PRESSURE_PLATE);
		dropSelf(ModBlocks.AMERICAN_BEECH_BUTTON);
		dropSelf(ModBlocks.AMERICAN_BEECH_SAPLING);
		dropSelf(ModBlocks.BLACK_WALNUT_LOG);
		dropSelf(ModBlocks.BLACK_WALNUT_WOOD);
		dropSelf(ModBlocks.STRIPPED_BLACK_WALNUT_LOG);
		dropSelf(ModBlocks.STRIPPED_BLACK_WALNUT_WOOD);
		dropSelf(ModBlocks.BLACK_WALNUT_SAPLING);

		add(ModBlocks.CHESTNUT_LEAVES, block -> createChestnutLeavesDrops());
		add(
			ModBlocks.HEMLOCK_FOLIAGE,
			block -> createLeavesDrops(
				ModBlocks.HEMLOCK_FOLIAGE,
				ModBlocks.HEMLOCK_SAPLING,
				NORMAL_LEAVES_SAPLING_CHANCES
			)
		);
		add(
			ModBlocks.AMERICAN_BEECH_LEAVES,
			block -> createLeavesDrops(
				ModBlocks.AMERICAN_BEECH_LEAVES,
				ModBlocks.AMERICAN_BEECH_SAPLING,
				NORMAL_LEAVES_SAPLING_CHANCES
			)
		);
		add(
			ModBlocks.BLACK_WALNUT_LEAVES,
			block -> createLeavesDrops(
				ModBlocks.BLACK_WALNUT_LEAVES,
				ModBlocks.BLACK_WALNUT_SAPLING,
				NORMAL_LEAVES_SAPLING_CHANCES
			)
		);
		add(ModBlocks.CHESTNUT_PILE, block -> createChestnutPileDrops());
		add(ModBlocks.CHESTNUT_HEWING_LOG, block -> createHewingLogDrops(ModBlocks.CHESTNUT_HEWING_LOG));
		add(ModBlocks.OAK_HEWING_LOG, block -> createHewingLogDrops(ModBlocks.OAK_HEWING_LOG));
		add(ModBlocks.SPRUCE_HEWING_LOG, block -> createHewingLogDrops(ModBlocks.SPRUCE_HEWING_LOG));
		add(ModBlocks.LOADED_SAWING_TRESTLES, block -> createLoadedSawingTrestlesDrops());
		add(ModBlocks.LOADED_SPLITTING_STUMP, block -> createLoadedSplittingStumpDrops());
		add(ModBlocks.CORN, block -> createCornDrops());
		add(ModBlocks.CHESTNUT_SLAB, this::createSlabItemTable);
		add(ModBlocks.ROUGH_CHESTNUT_BOARD_SLAB, this::createSlabItemTable);
		add(ModBlocks.CHESTNUT_SHINGLE_SLAB, this::createSlabItemTable);
		add(ModBlocks.FIELDSTONE_SLAB, this::createSlabItemTable);
		add(ModBlocks.DRESSED_FIELDSTONE_SLAB, this::createSlabItemTable);
		add(ModBlocks.HEMLOCK_SLAB, this::createSlabItemTable);
		add(ModBlocks.AMERICAN_BEECH_SLAB, this::createSlabItemTable);

		/*
		 * Board/roof loot is intentionally the only isolated polish-family
		 * loot emitted here. The other isolated registrars own authored,
		 * state-aware loot JSON under src/main/resources; mirroring them here
		 * would create duplicate generated/static output paths.
		 */
		BoardRoofDataDefinitions.addLoot(
			this::dropSelf,
			block -> add(block, this::createSlabItemTable)
		);
	}

	private LootTable.Builder createHewingLogDrops(Block stagedBlock) {
		var partialReturn = TimberProcessingRegistry.partialReturn(
			stagedBlock.defaultBlockState()
		).orElseThrow(() -> new IllegalStateException("Hewing process is not registered for " + stagedBlock));
		return LootTable.lootTable().withPool(
			LootPool.lootPool()
				.setRolls(ConstantValue.exactly(1.0F))
				.add(LootItem.lootTableItem(partialReturn))
		);
	}

	private LootTable.Builder createLoadedSawingTrestlesDrops() {
		LootTable.Builder table = LootTable.lootTable().withPool(
			LootPool.lootPool()
				.setRolls(ConstantValue.exactly(1.0F))
				.add(LootItem.lootTableItem(ModBlocks.SAWING_TRESTLES))
		);
		for (var process : TimberProcessingRegistry.all()) {
			table.withPool(
				LootPool.lootPool()
					.setRolls(ConstantValue.exactly(1.0F))
					.add(LootItem.lootTableItem(process.finalBlock()))
					.when(
						LootItemBlockStatePropertyCondition
							.hasBlockStateProperties(ModBlocks.LOADED_SAWING_TRESTLES)
							.setProperties(
								StatePropertiesPredicate.Builder.properties().hasProperty(
									LoadedSawingTrestlesBlock.WOOD_TYPE,
									process.type()
								)
							)
					)
			);
		}
		return table;
	}

	private LootTable.Builder createLoadedSplittingStumpDrops() {
		LootTable.Builder table = LootTable.lootTable().withPool(
			LootPool.lootPool()
				.setRolls(ConstantValue.exactly(1.0F))
				.add(LootItem.lootTableItem(ModBlocks.SPLITTING_STUMP))
		);
		for (var process : TimberProcessingRegistry.all()) {
			if (process.splitOutputs().isEmpty()) {
				continue;
			}
			table.withPool(
				LootPool.lootPool()
					.setRolls(ConstantValue.exactly(1.0F))
					.add(LootItem.lootTableItem(process.finalBlock()))
					.when(
						LootItemBlockStatePropertyCondition
							.hasBlockStateProperties(ModBlocks.LOADED_SPLITTING_STUMP)
							.setProperties(
								StatePropertiesPredicate.Builder.properties().hasProperty(
									LoadedSplittingStumpBlock.WOOD_TYPE,
									process.type()
								)
							)
					)
			);
		}
		return table;
	}

	private LootTable.Builder createChestnutLeavesDrops() {
		HolderLookup.RegistryLookup<Enchantment> enchantments =
			registries.lookupOrThrow(Registries.ENCHANTMENT);
		return createLeavesDrops(
			ModBlocks.CHESTNUT_LEAVES,
			ModBlocks.CHESTNUT_SAPLING,
			NORMAL_LEAVES_SAPLING_CHANCES
		).withPool(
			LootPool.lootPool()
				.setRolls(ConstantValue.exactly(1.0F))
				.when(doesNotHaveShearsOrSilkTouch())
				.add(
					((LootPoolSingletonContainer.Builder<?>) applyExplosionCondition(
						ModBlocks.CHESTNUT_LEAVES,
						LootItem.lootTableItem(ModItems.HANDFUL_OF_CHESTNUTS)
					)).when(
						BonusLevelTableCondition.bonusLevelFlatChance(
							enchantments.getOrThrow(Enchantments.FORTUNE),
							0.005F,
							0.0055555557F,
							0.00625F,
							0.008333334F,
							0.025F
						)
					)
				)
		);
	}

	private LootTable.Builder createCornDrops() {
		return applyExplosionDecay(
			ModBlocks.CORN,
			LootTable.lootTable()
				.withPool(
					LootPool.lootPool().add(
						LootItem.lootTableItem(ModItems.EAR_OF_CORN)
							.when(cornIsMature())
							.otherwise(LootItem.lootTableItem(ModItems.CORN_KERNELS))
					)
				)
				.withPool(
					LootPool.lootPool()
						.when(cornIsMature())
						.add(
							LootItem.lootTableItem(ModItems.CORN_KERNELS)
								.apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F)))
						)
				)
		);
	}

	private static LootItemBlockStatePropertyCondition.Builder cornIsMature() {
		return LootItemBlockStatePropertyCondition
			.hasBlockStateProperties(ModBlocks.CORN)
			.setProperties(
				StatePropertiesPredicate.Builder.properties().hasProperty(
					CornCropBlock.AGE,
					CornCropBlock.MAX_AGE
				)
			);
	}

	private LootTable.Builder createChestnutPileDrops() {
		return LootTable.lootTable().withPool(
			LootPool.lootPool()
				.setRolls(ConstantValue.exactly(1.0F))
				.add(
					(LootPoolEntryContainer.Builder<?>) applyExplosionDecay(
						ModBlocks.CHESTNUT_PILE,
						LootItem.lootTableItem(ModItems.HANDFUL_OF_CHESTNUTS)
							.apply(
								SnowLayerBlock.LAYERS.getPossibleValues(),
								layers -> SetItemCountFunction
									.setCount(ConstantValue.exactly(layers.intValue()))
									.when(
										LootItemBlockStatePropertyCondition
											.hasBlockStateProperties(ModBlocks.CHESTNUT_PILE)
											.setProperties(
												StatePropertiesPredicate.Builder.properties()
													.hasProperty(SnowLayerBlock.LAYERS, layers.intValue())
											)
									)
							)
					)
				)
		);
	}
}
