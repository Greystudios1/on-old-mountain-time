package net.beforetheblight.registry;

import java.util.function.Function;

import net.beforetheblight.BeforeTheBlight;
import net.beforetheblight.block.ChestnutSaplingBlock;
import net.beforetheblight.block.CornCropBlock;
import net.beforetheblight.block.DryingCornBundleBlock;
import net.beforetheblight.block.ForestDuffBlock;
import net.beforetheblight.block.HewnChestnutPostBlock;
import net.beforetheblight.block.HewingLogBlock;
import net.beforetheblight.block.LoadedSawingTrestlesBlock;
import net.beforetheblight.block.LoadedSplittingStumpBlock;
import net.beforetheblight.block.OpenChestnutStairBlock;
import net.beforetheblight.block.RockingChairBlock;
import net.beforetheblight.block.SawingTrestlesBlock;
import net.beforetheblight.block.SeasonalChestnutLeavesBlock;
import net.beforetheblight.block.SeasonalSaplingBlock;
import net.beforetheblight.block.SplittingStumpBlock;
import net.beforetheblight.block.UnderstoryPlantBlock;
import net.beforetheblight.compat.seasons.SeasonalPlantClock;
import net.beforetheblight.worldgen.feature.ModTreeGrowers;
import net.fabricmc.fabric.api.object.builder.v1.block.type.BlockSetTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.block.type.WoodTypeBuilder;
import net.fabricmc.fabric.api.registry.CompostableRegistry;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TintedParticleLeavesBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public final class ModBlocks {
	private static final BlockSetType CHESTNUT_BLOCK_SET_TYPE = BlockSetTypeBuilder.copyOf(BlockSetType.OAK)
		.register(BeforeTheBlight.id("chestnut"));
	private static final WoodType CHESTNUT_WOOD_TYPE = WoodTypeBuilder.copyOf(WoodType.OAK)
		.register(BeforeTheBlight.id("chestnut"), CHESTNUT_BLOCK_SET_TYPE);
	private static final BlockSetType HEMLOCK_BLOCK_SET_TYPE = BlockSetTypeBuilder.copyOf(BlockSetType.SPRUCE)
		.register(BeforeTheBlight.id("hemlock"));
	private static final WoodType HEMLOCK_WOOD_TYPE = WoodTypeBuilder.copyOf(WoodType.SPRUCE)
		.register(BeforeTheBlight.id("hemlock"), HEMLOCK_BLOCK_SET_TYPE);
	private static final BlockSetType AMERICAN_BEECH_BLOCK_SET_TYPE = BlockSetTypeBuilder.copyOf(BlockSetType.OAK)
		.register(BeforeTheBlight.id("american_beech"));
	private static final WoodType AMERICAN_BEECH_WOOD_TYPE = WoodTypeBuilder.copyOf(WoodType.OAK)
		.register(BeforeTheBlight.id("american_beech"), AMERICAN_BEECH_BLOCK_SET_TYPE);

	public static final Block CHESTNUT_LOG = register(
		"chestnut_log",
		RotatedPillarBlock::new,
		logProperties(MapColor.WOOD, MapColor.COLOR_BROWN),
		true
	);

	public static final Block CHESTNUT_WOOD = register(
		"chestnut_wood",
		RotatedPillarBlock::new,
		woodProperties(MapColor.COLOR_BROWN),
		true
	);

	public static final Block STRIPPED_CHESTNUT_LOG = register(
		"stripped_chestnut_log",
		RotatedPillarBlock::new,
		logProperties(MapColor.WOOD, MapColor.WOOD),
		true
	);

	public static final Block STRIPPED_CHESTNUT_WOOD = register(
		"stripped_chestnut_wood",
		RotatedPillarBlock::new,
		woodProperties(MapColor.WOOD),
		true
	);

	public static final HewingLogBlock CHESTNUT_HEWING_LOG = register(
		"chestnut_hewing_log",
		HewingLogBlock::new,
		logProperties(MapColor.WOOD, MapColor.WOOD),
		false
	);

	public static final Block HEWN_CHESTNUT_BEAM = register(
		"hewn_chestnut_beam",
		RotatedPillarBlock::new,
		logProperties(MapColor.WOOD, MapColor.WOOD),
		true
	);

	public static final Block HEWN_CHESTNUT_WALL = register(
		"hewn_chestnut_wall",
		WallBlock::new,
		woodProperties(MapColor.WOOD).forceSolidOn(),
		true
	);

	public static final HewnChestnutPostBlock HEWN_CHESTNUT_POST = register(
		"hewn_chestnut_post",
		HewnChestnutPostBlock::new,
		woodProperties(MapColor.WOOD).forceSolidOn().noOcclusion(),
		true
	);

	public static final Block CHESTNUT_PLANKS = register(
		"chestnut_planks",
		Block::new,
		woodProperties(MapColor.WOOD),
		true
	);

	public static final Block ROUGH_CHESTNUT_BOARDS = register(
		"rough_chestnut_boards",
		Block::new,
		woodProperties(MapColor.WOOD),
		true
	);

	public static final Block ROUGH_CHESTNUT_BOARD_STAIRS = register(
		"rough_chestnut_board_stairs",
		properties -> new StairBlock(ROUGH_CHESTNUT_BOARDS.defaultBlockState(), properties),
		woodProperties(MapColor.WOOD),
		true
	);

	public static final OpenChestnutStairBlock ROUGH_CHESTNUT_OPEN_STAIRCASE =
		register(
			"rough_chestnut_open_staircase",
			OpenChestnutStairBlock::new,
			woodProperties(MapColor.WOOD).noOcclusion().forceSolidOff(),
			true
		);

	public static final Block ROUGH_CHESTNUT_BOARD_SLAB = register(
		"rough_chestnut_board_slab",
		SlabBlock::new,
		woodProperties(MapColor.WOOD),
		true
	);

	public static final Block CHESTNUT_SHINGLES = register(
		"chestnut_shingles",
		Block::new,
		woodProperties(MapColor.WOOD),
		true
	);

	public static final Block CHESTNUT_SHINGLE_STAIRS = register(
		"chestnut_shingle_stairs",
		properties -> new StairBlock(CHESTNUT_SHINGLES.defaultBlockState(), properties),
		woodProperties(MapColor.WOOD),
		true
	);

	public static final Block CHESTNUT_SHINGLE_SLAB = register(
		"chestnut_shingle_slab",
		SlabBlock::new,
		woodProperties(MapColor.WOOD),
		true
	);

	public static final Block SPLIT_CHESTNUT_RAILS = register(
		"split_chestnut_rails",
		FenceBlock::new,
		woodProperties(MapColor.WOOD).forceSolidOn(),
		true
	);

	public static final Block CHINKED_CHESTNUT_LOGS = register(
		"chinked_chestnut_logs",
		RotatedPillarBlock::new,
		logProperties(MapColor.WOOD, MapColor.COLOR_BROWN),
		true
	);

	public static final Block FIELDSTONE = register(
		"fieldstone",
		Block::new,
		BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE),
		true
	);

	public static final Block FIELDSTONE_STAIRS = register(
		"fieldstone_stairs",
		properties -> new StairBlock(FIELDSTONE.defaultBlockState(), properties),
		BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE_STAIRS),
		true
	);

	public static final Block FIELDSTONE_SLAB = register(
		"fieldstone_slab",
		SlabBlock::new,
		BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE_SLAB),
		true
	);

	public static final Block FIELDSTONE_WALL = register(
		"fieldstone_wall",
		WallBlock::new,
		BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE_WALL),
		true
	);

	public static final Block DRESSED_FIELDSTONE = register(
		"dressed_fieldstone",
		Block::new,
		BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS),
		true
	);

	public static final Block DRESSED_FIELDSTONE_STAIRS = register(
		"dressed_fieldstone_stairs",
		properties -> new StairBlock(DRESSED_FIELDSTONE.defaultBlockState(), properties),
		BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICK_STAIRS),
		true
	);

	public static final Block DRESSED_FIELDSTONE_SLAB = register(
		"dressed_fieldstone_slab",
		SlabBlock::new,
		BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICK_SLAB),
		true
	);

	public static final Block DRESSED_FIELDSTONE_WALL = register(
		"dressed_fieldstone_wall",
		WallBlock::new,
		BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICK_WALL),
		true
	);

	public static final Block CHISELED_FIELDSTONE = register(
		"chiseled_fieldstone",
		Block::new,
		BlockBehaviour.Properties.ofFullCopy(Blocks.CHISELED_STONE_BRICKS),
		true
	);

	public static final Block FIELDSTONE_PIER = register(
		"fieldstone_pier",
		RotatedPillarBlock::new,
		BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS),
		true
	);

	public static final HewingLogBlock OAK_HEWING_LOG = register(
		"oak_hewing_log",
		HewingLogBlock::new,
		BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG),
		false
	);

	public static final Block HEWN_OAK_BEAM = register(
		"hewn_oak_beam",
		RotatedPillarBlock::new,
		BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_LOG),
		true
	);

	public static final Block ROUGH_OAK_BOARDS = register(
		"rough_oak_boards",
		Block::new,
		BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS),
		true
	);

	public static final HewingLogBlock SPRUCE_HEWING_LOG = register(
		"spruce_hewing_log",
		HewingLogBlock::new,
		BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_LOG),
		false
	);

	public static final Block HEWN_SPRUCE_BEAM = register(
		"hewn_spruce_beam",
		RotatedPillarBlock::new,
		BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_SPRUCE_LOG),
		true
	);

	public static final Block ROUGH_SPRUCE_BOARDS = register(
		"rough_spruce_boards",
		Block::new,
		BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_PLANKS),
		true
	);

	public static final SawingTrestlesBlock SAWING_TRESTLES = register(
		"sawing_trestles",
		SawingTrestlesBlock::new,
		woodProperties(MapColor.WOOD).noOcclusion(),
		true
	);

	public static final LoadedSawingTrestlesBlock LOADED_SAWING_TRESTLES = register(
		"loaded_sawing_trestles",
		LoadedSawingTrestlesBlock::new,
		woodProperties(MapColor.WOOD).noOcclusion(),
		false
	);

	public static final SplittingStumpBlock SPLITTING_STUMP = register(
		"splitting_stump",
		SplittingStumpBlock::new,
		woodProperties(MapColor.WOOD).noOcclusion(),
		true
	);

	public static final LoadedSplittingStumpBlock LOADED_SPLITTING_STUMP = register(
		"loaded_splitting_stump",
		LoadedSplittingStumpBlock::new,
		woodProperties(MapColor.WOOD).noOcclusion(),
		false
	);

	public static final Block CHESTNUT_LEAVES = register(
		"chestnut_leaves",
		SeasonalChestnutLeavesBlock::new,
		leavesProperties(),
		true
	);

	public static final Block HEMLOCK_FOLIAGE = register(
		"hemlock_foliage",
		properties -> new TintedParticleLeavesBlock(0.01F, properties),
		leavesProperties(),
		true
	);

	public static final Block CHESTNUT_SAPLING = register(
		"chestnut_sapling",
		properties -> new ChestnutSaplingBlock(ModTreeGrowers.CHESTNUT, properties),
		saplingProperties(),
		true
	);

	public static final Block MOUNTAIN_LAUREL = register(
		"mountain_laurel",
		UnderstoryPlantBlock::new,
		understoryProperties(),
		true
	);

	public static final Block LOWBUSH_BLUEBERRY = register(
		"lowbush_blueberry",
		UnderstoryPlantBlock::new,
		understoryProperties(),
		true
	);

	public static final Block FOREST_DUFF = register(
		"forest_duff",
		ForestDuffBlock::new,
		understoryProperties(),
		true
	);

	public static final RockingChairBlock ROCKING_CHAIR = register(
		"rocking_chair",
		RockingChairBlock::new,
		woodProperties(MapColor.WOOD).noOcclusion(),
		true
	);

	public static final Block CHESTNUT_PILE = register(
		"chestnut_pile",
		SnowLayerBlock::new,
		BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_BROWN)
			.replaceable()
			.noOcclusion()
			.forceSolidOff()
			.strength(0.1F)
			.sound(SoundType.WOOD)
			.ignitedByLava()
			.isViewBlocking((state, level, pos) ->
				state.getValue(SnowLayerBlock.LAYERS) >= SnowLayerBlock.MAX_HEIGHT)
			.pushReaction(PushReaction.DESTROY),
		false
	);

	public static final CornCropBlock CORN = register(
		"corn",
		CornCropBlock::new,
		BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT),
		false
	);

	public static final DryingCornBundleBlock DRYING_CORN_BUNDLE = register(
		"drying_corn_bundle",
		DryingCornBundleBlock::new,
		BlockBehaviour.Properties.of()
			.mapColor(MapColor.WOOD)
			.noOcclusion()
			.randomTicks()
			.strength(1.5F)
			.sound(SoundType.WOOD)
			.ignitedByLava()
			.pushReaction(PushReaction.DESTROY)
			.noLootTable(),
		true
	);

	public static final Block CHESTNUT_STAIRS = register(
		"chestnut_stairs",
		properties -> new StairBlock(CHESTNUT_PLANKS.defaultBlockState(), properties),
		woodProperties(MapColor.WOOD),
		true
	);

	public static final Block CHESTNUT_SLAB = register(
		"chestnut_slab",
		SlabBlock::new,
		woodProperties(MapColor.WOOD),
		true
	);

	public static final Block CHESTNUT_FENCE = register(
		"chestnut_fence",
		FenceBlock::new,
		woodProperties(MapColor.WOOD).forceSolidOn(),
		true
	);

	public static final Block CHESTNUT_FENCE_GATE = register(
		"chestnut_fence_gate",
		properties -> new FenceGateBlock(CHESTNUT_WOOD_TYPE, properties),
		woodProperties(MapColor.WOOD).forceSolidOn(),
		true
	);

	public static final Block CHESTNUT_PRESSURE_PLATE = register(
		"chestnut_pressure_plate",
		properties -> new PressurePlateBlock(CHESTNUT_BLOCK_SET_TYPE, properties),
		BlockBehaviour.Properties.of()
			.mapColor(MapColor.WOOD)
			.forceSolidOn()
			.instrument(NoteBlockInstrument.BASS)
			.noCollision()
			.strength(0.5F)
			.ignitedByLava()
			.pushReaction(PushReaction.DESTROY),
		true
	);

	public static final Block CHESTNUT_BUTTON = register(
		"chestnut_button",
		properties -> new ButtonBlock(CHESTNUT_BLOCK_SET_TYPE, 30, properties),
		BlockBehaviour.Properties.of()
			.noCollision()
			.strength(0.5F)
			.pushReaction(PushReaction.DESTROY),
		true
	);

	public static final Block HEMLOCK_LOG = register(
		"hemlock_log",
		RotatedPillarBlock::new,
		logProperties(MapColor.WOOD, MapColor.COLOR_BROWN),
		true
	);

	public static final Block HEMLOCK_WOOD = register(
		"hemlock_wood",
		RotatedPillarBlock::new,
		woodProperties(MapColor.COLOR_BROWN),
		true
	);

	public static final Block STRIPPED_HEMLOCK_LOG = register(
		"stripped_hemlock_log",
		RotatedPillarBlock::new,
		logProperties(MapColor.WOOD, MapColor.WOOD),
		true
	);

	public static final Block STRIPPED_HEMLOCK_WOOD = register(
		"stripped_hemlock_wood",
		RotatedPillarBlock::new,
		woodProperties(MapColor.WOOD),
		true
	);

	public static final Block HEMLOCK_PLANKS = register(
		"hemlock_planks",
		Block::new,
		woodProperties(MapColor.WOOD),
		true
	);

	public static final Block HEMLOCK_STAIRS = register(
		"hemlock_stairs",
		properties -> new StairBlock(HEMLOCK_PLANKS.defaultBlockState(), properties),
		woodProperties(MapColor.WOOD),
		true
	);

	public static final Block HEMLOCK_SLAB = register(
		"hemlock_slab",
		SlabBlock::new,
		woodProperties(MapColor.WOOD),
		true
	);

	public static final Block HEMLOCK_FENCE = register(
		"hemlock_fence",
		FenceBlock::new,
		woodProperties(MapColor.WOOD).forceSolidOn(),
		true
	);

	public static final Block HEMLOCK_FENCE_GATE = register(
		"hemlock_fence_gate",
		properties -> new FenceGateBlock(HEMLOCK_WOOD_TYPE, properties),
		woodProperties(MapColor.WOOD).forceSolidOn(),
		true
	);

	public static final Block HEMLOCK_PRESSURE_PLATE = register(
		"hemlock_pressure_plate",
		properties -> new PressurePlateBlock(HEMLOCK_BLOCK_SET_TYPE, properties),
		BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_PRESSURE_PLATE),
		true
	);

	public static final Block HEMLOCK_BUTTON = register(
		"hemlock_button",
		properties -> new ButtonBlock(HEMLOCK_BLOCK_SET_TYPE, 30, properties),
		BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_BUTTON),
		true
	);

	public static final Block HEMLOCK_SAPLING = register(
		"hemlock_sapling",
		properties -> new SeasonalSaplingBlock(
			ModTreeGrowers.HEMLOCK,
			SeasonalPlantClock.Plant.HEMLOCK,
			properties
		),
		saplingProperties(),
		true
	);

	public static final Block AMERICAN_BEECH_LOG = register(
		"american_beech_log",
		RotatedPillarBlock::new,
		logProperties(MapColor.WOOD, MapColor.STONE),
		true
	);

	public static final Block AMERICAN_BEECH_WOOD = register(
		"american_beech_wood",
		RotatedPillarBlock::new,
		woodProperties(MapColor.STONE),
		true
	);

	public static final Block STRIPPED_AMERICAN_BEECH_LOG = register(
		"stripped_american_beech_log",
		RotatedPillarBlock::new,
		logProperties(MapColor.WOOD, MapColor.WOOD),
		true
	);

	public static final Block STRIPPED_AMERICAN_BEECH_WOOD = register(
		"stripped_american_beech_wood",
		RotatedPillarBlock::new,
		woodProperties(MapColor.WOOD),
		true
	);

	public static final Block AMERICAN_BEECH_PLANKS = register(
		"american_beech_planks",
		Block::new,
		woodProperties(MapColor.WOOD),
		true
	);

	public static final Block AMERICAN_BEECH_STAIRS = register(
		"american_beech_stairs",
		properties -> new StairBlock(AMERICAN_BEECH_PLANKS.defaultBlockState(), properties),
		woodProperties(MapColor.WOOD),
		true
	);

	public static final Block AMERICAN_BEECH_SLAB = register(
		"american_beech_slab",
		SlabBlock::new,
		woodProperties(MapColor.WOOD),
		true
	);

	public static final Block AMERICAN_BEECH_FENCE = register(
		"american_beech_fence",
		FenceBlock::new,
		woodProperties(MapColor.WOOD).forceSolidOn(),
		true
	);

	public static final Block AMERICAN_BEECH_FENCE_GATE = register(
		"american_beech_fence_gate",
		properties -> new FenceGateBlock(AMERICAN_BEECH_WOOD_TYPE, properties),
		woodProperties(MapColor.WOOD).forceSolidOn(),
		true
	);

	public static final Block AMERICAN_BEECH_PRESSURE_PLATE = register(
		"american_beech_pressure_plate",
		properties -> new PressurePlateBlock(AMERICAN_BEECH_BLOCK_SET_TYPE, properties),
		BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PRESSURE_PLATE),
		true
	);

	public static final Block AMERICAN_BEECH_BUTTON = register(
		"american_beech_button",
		properties -> new ButtonBlock(AMERICAN_BEECH_BLOCK_SET_TYPE, 30, properties),
		BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_BUTTON),
		true
	);

	public static final Block AMERICAN_BEECH_LEAVES = register(
		"american_beech_leaves",
		properties -> new TintedParticleLeavesBlock(0.01F, properties),
		leavesProperties(),
		true
	);

	public static final Block AMERICAN_BEECH_SAPLING = register(
		"american_beech_sapling",
		properties -> new SeasonalSaplingBlock(
			ModTreeGrowers.AMERICAN_BEECH,
			SeasonalPlantClock.Plant.AMERICAN_BEECH,
			properties
		),
		saplingProperties(),
		true
	);

	public static final Block BLACK_WALNUT_LOG = register(
		"black_walnut_log",
		RotatedPillarBlock::new,
		logProperties(MapColor.WOOD, MapColor.COLOR_BROWN),
		true
	);

	public static final Block BLACK_WALNUT_WOOD = register(
		"black_walnut_wood",
		RotatedPillarBlock::new,
		woodProperties(MapColor.COLOR_BROWN),
		true
	);

	public static final Block STRIPPED_BLACK_WALNUT_LOG = register(
		"stripped_black_walnut_log",
		RotatedPillarBlock::new,
		logProperties(MapColor.WOOD, MapColor.WOOD),
		true
	);

	public static final Block STRIPPED_BLACK_WALNUT_WOOD = register(
		"stripped_black_walnut_wood",
		RotatedPillarBlock::new,
		woodProperties(MapColor.WOOD),
		true
	);

	public static final Block BLACK_WALNUT_LEAVES = register(
		"black_walnut_leaves",
		properties -> new TintedParticleLeavesBlock(0.01F, properties),
		leavesProperties(),
		true
	);

	public static final Block BLACK_WALNUT_SAPLING = register(
		"black_walnut_sapling",
		properties -> new SeasonalSaplingBlock(
			ModTreeGrowers.BLACK_WALNUT,
			SeasonalPlantClock.Plant.BLACK_WALNUT,
			properties
		),
		saplingProperties(),
		true
	);

	private ModBlocks() {
	}

	private static BlockBehaviour.Properties logProperties(MapColor topColor, MapColor sideColor) {
		return BlockBehaviour.Properties.of()
			.mapColor(state -> state.getValue(RotatedPillarBlock.AXIS).isVertical() ? topColor : sideColor)
			.instrument(NoteBlockInstrument.BASS)
			.strength(2.0F)
			.sound(SoundType.WOOD)
			.ignitedByLava();
	}

	private static BlockBehaviour.Properties woodProperties(MapColor mapColor) {
		return BlockBehaviour.Properties.of()
			.mapColor(mapColor)
			.instrument(NoteBlockInstrument.BASS)
			.strength(2.0F, 3.0F)
			.sound(SoundType.WOOD)
			.ignitedByLava();
	}

	private static BlockBehaviour.Properties leavesProperties() {
		return BlockBehaviour.Properties.of()
			.mapColor(MapColor.PLANT)
			.strength(0.2F)
			.randomTicks()
			.sound(SoundType.GRASS)
			.noOcclusion()
			.isValidSpawn(Blocks::ocelotOrParrot)
			.isSuffocating(Blocks::never)
			.isViewBlocking(Blocks::never)
			.ignitedByLava()
			.pushReaction(PushReaction.DESTROY)
			.isRedstoneConductor(Blocks::never);
	}

	private static BlockBehaviour.Properties saplingProperties() {
		return BlockBehaviour.Properties.of()
			.mapColor(MapColor.PLANT)
			.randomTicks()
			.noCollision()
			.instabreak()
			.sound(SoundType.GRASS)
			.pushReaction(PushReaction.DESTROY);
	}

	private static BlockBehaviour.Properties understoryProperties() {
		return BlockBehaviour.Properties.of()
			.mapColor(MapColor.PLANT)
			.replaceable()
			.noCollision()
			.noOcclusion()
			.instabreak()
			.sound(SoundType.GRASS)
			.pushReaction(PushReaction.DESTROY);
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

	public static void initialize() {
		StrippableBlockRegistry.register(CHESTNUT_LOG, STRIPPED_CHESTNUT_LOG);
		StrippableBlockRegistry.register(CHESTNUT_WOOD, STRIPPED_CHESTNUT_WOOD);
		StrippableBlockRegistry.register(HEMLOCK_LOG, STRIPPED_HEMLOCK_LOG);
		StrippableBlockRegistry.register(HEMLOCK_WOOD, STRIPPED_HEMLOCK_WOOD);
		StrippableBlockRegistry.register(AMERICAN_BEECH_LOG, STRIPPED_AMERICAN_BEECH_LOG);
		StrippableBlockRegistry.register(AMERICAN_BEECH_WOOD, STRIPPED_AMERICAN_BEECH_WOOD);
		StrippableBlockRegistry.register(BLACK_WALNUT_LOG, STRIPPED_BLACK_WALNUT_LOG);
		StrippableBlockRegistry.register(BLACK_WALNUT_WOOD, STRIPPED_BLACK_WALNUT_WOOD);

		FlammableBlockRegistry flammableBlocks = FlammableBlockRegistry.getDefaultInstance();
		flammableBlocks.add(CHESTNUT_LOG, 5, 5);
		flammableBlocks.add(CHESTNUT_WOOD, 5, 5);
		flammableBlocks.add(STRIPPED_CHESTNUT_LOG, 5, 5);
		flammableBlocks.add(STRIPPED_CHESTNUT_WOOD, 5, 5);
		flammableBlocks.add(CHESTNUT_HEWING_LOG, 5, 5);
		flammableBlocks.add(HEWN_CHESTNUT_BEAM, 5, 5);
		flammableBlocks.add(HEWN_CHESTNUT_WALL, 5, 5);
		flammableBlocks.add(HEWN_CHESTNUT_POST, 5, 5);
		flammableBlocks.add(CHESTNUT_PLANKS, 5, 20);
		flammableBlocks.add(ROUGH_CHESTNUT_BOARDS, 5, 20);
		flammableBlocks.add(ROUGH_CHESTNUT_BOARD_STAIRS, 5, 20);
		flammableBlocks.add(ROUGH_CHESTNUT_OPEN_STAIRCASE, 5, 20);
		flammableBlocks.add(ROUGH_CHESTNUT_BOARD_SLAB, 5, 20);
		flammableBlocks.add(CHESTNUT_SHINGLES, 5, 20);
		flammableBlocks.add(CHESTNUT_SHINGLE_STAIRS, 5, 20);
		flammableBlocks.add(CHESTNUT_SHINGLE_SLAB, 5, 20);
		flammableBlocks.add(SPLIT_CHESTNUT_RAILS, 5, 20);
		flammableBlocks.add(CHINKED_CHESTNUT_LOGS, 5, 5);
		flammableBlocks.add(OAK_HEWING_LOG, 5, 5);
		flammableBlocks.add(HEWN_OAK_BEAM, 5, 5);
		flammableBlocks.add(ROUGH_OAK_BOARDS, 5, 20);
		flammableBlocks.add(SPRUCE_HEWING_LOG, 5, 5);
		flammableBlocks.add(HEWN_SPRUCE_BEAM, 5, 5);
		flammableBlocks.add(ROUGH_SPRUCE_BOARDS, 5, 20);
		flammableBlocks.add(SAWING_TRESTLES, 5, 20);
		flammableBlocks.add(LOADED_SAWING_TRESTLES, 5, 20);
		flammableBlocks.add(SPLITTING_STUMP, 5, 5);
		flammableBlocks.add(LOADED_SPLITTING_STUMP, 5, 5);
		flammableBlocks.add(DRYING_CORN_BUNDLE, 30, 60);
		flammableBlocks.add(CHESTNUT_LEAVES, 30, 60);
		flammableBlocks.add(HEMLOCK_FOLIAGE, 30, 60);
		flammableBlocks.add(CHESTNUT_STAIRS, 5, 20);
		flammableBlocks.add(CHESTNUT_SLAB, 5, 20);
		flammableBlocks.add(CHESTNUT_FENCE, 5, 20);
		flammableBlocks.add(CHESTNUT_FENCE_GATE, 5, 20);
		flammableBlocks.add(CHESTNUT_PILE, 30, 60);
		flammableBlocks.add(MOUNTAIN_LAUREL, 30, 60);
		flammableBlocks.add(LOWBUSH_BLUEBERRY, 30, 60);
		flammableBlocks.add(FOREST_DUFF, 30, 60);
		flammableBlocks.add(ROCKING_CHAIR, 5, 20);
		flammableBlocks.add(HEMLOCK_LOG, 5, 5);
		flammableBlocks.add(HEMLOCK_WOOD, 5, 5);
		flammableBlocks.add(STRIPPED_HEMLOCK_LOG, 5, 5);
		flammableBlocks.add(STRIPPED_HEMLOCK_WOOD, 5, 5);
		flammableBlocks.add(HEMLOCK_PLANKS, 5, 20);
		flammableBlocks.add(HEMLOCK_STAIRS, 5, 20);
		flammableBlocks.add(HEMLOCK_SLAB, 5, 20);
		flammableBlocks.add(HEMLOCK_FENCE, 5, 20);
		flammableBlocks.add(HEMLOCK_FENCE_GATE, 5, 20);
		flammableBlocks.add(HEMLOCK_SAPLING, 60, 100);
		flammableBlocks.add(AMERICAN_BEECH_LOG, 5, 5);
		flammableBlocks.add(AMERICAN_BEECH_WOOD, 5, 5);
		flammableBlocks.add(STRIPPED_AMERICAN_BEECH_LOG, 5, 5);
		flammableBlocks.add(STRIPPED_AMERICAN_BEECH_WOOD, 5, 5);
		flammableBlocks.add(AMERICAN_BEECH_PLANKS, 5, 20);
		flammableBlocks.add(AMERICAN_BEECH_STAIRS, 5, 20);
		flammableBlocks.add(AMERICAN_BEECH_SLAB, 5, 20);
		flammableBlocks.add(AMERICAN_BEECH_FENCE, 5, 20);
		flammableBlocks.add(AMERICAN_BEECH_FENCE_GATE, 5, 20);
		flammableBlocks.add(AMERICAN_BEECH_LEAVES, 30, 60);
		flammableBlocks.add(AMERICAN_BEECH_SAPLING, 60, 100);
		flammableBlocks.add(BLACK_WALNUT_LOG, 5, 5);
		flammableBlocks.add(BLACK_WALNUT_WOOD, 5, 5);
		flammableBlocks.add(STRIPPED_BLACK_WALNUT_LOG, 5, 5);
		flammableBlocks.add(STRIPPED_BLACK_WALNUT_WOOD, 5, 5);
		flammableBlocks.add(BLACK_WALNUT_LEAVES, 30, 60);
		flammableBlocks.add(BLACK_WALNUT_SAPLING, 60, 100);

		CompostableRegistry.INSTANCE.add(CHESTNUT_LEAVES, 0.3F);
		CompostableRegistry.INSTANCE.add(HEMLOCK_FOLIAGE, 0.3F);
		CompostableRegistry.INSTANCE.add(CHESTNUT_SAPLING, 0.3F);
		CompostableRegistry.INSTANCE.add(MOUNTAIN_LAUREL, 0.3F);
		CompostableRegistry.INSTANCE.add(LOWBUSH_BLUEBERRY, 0.3F);
		CompostableRegistry.INSTANCE.add(FOREST_DUFF, 0.3F);
		CompostableRegistry.INSTANCE.add(HEMLOCK_SAPLING, 0.3F);
		CompostableRegistry.INSTANCE.add(AMERICAN_BEECH_LEAVES, 0.3F);
		CompostableRegistry.INSTANCE.add(AMERICAN_BEECH_SAPLING, 0.3F);
		CompostableRegistry.INSTANCE.add(BLACK_WALNUT_LEAVES, 0.3F);
		CompostableRegistry.INSTANCE.add(BLACK_WALNUT_SAPLING, 0.3F);
	}
}
