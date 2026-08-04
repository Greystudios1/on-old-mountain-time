package net.beforetheblight.datagen;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import net.beforetheblight.block.HewingLogBlock;
import net.beforetheblight.block.CornCropBlock;
import net.beforetheblight.registry.ModBlocks;
import net.beforetheblight.registry.ModItems;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class BeforeTheBlightModelProvider extends FabricModelProvider {
	public BeforeTheBlightModelProvider(FabricPackOutput output) {
		super(output);
	}

	@Override
	public void generateBlockStateModels(BlockModelGenerators generator) {
		generator.woodProvider(ModBlocks.CHESTNUT_LOG)
			.log(ModBlocks.CHESTNUT_LOG)
			.wood(ModBlocks.CHESTNUT_WOOD);
		generator.woodProvider(ModBlocks.STRIPPED_CHESTNUT_LOG)
			.log(ModBlocks.STRIPPED_CHESTNUT_LOG)
			.wood(ModBlocks.STRIPPED_CHESTNUT_WOOD);
		generator.woodProvider(ModBlocks.HEMLOCK_LOG)
			.log(ModBlocks.HEMLOCK_LOG)
			.wood(ModBlocks.HEMLOCK_WOOD);
		generator.woodProvider(ModBlocks.STRIPPED_HEMLOCK_LOG)
			.log(ModBlocks.STRIPPED_HEMLOCK_LOG)
			.wood(ModBlocks.STRIPPED_HEMLOCK_WOOD);
		generator.woodProvider(ModBlocks.AMERICAN_BEECH_LOG)
			.log(ModBlocks.AMERICAN_BEECH_LOG)
			.wood(ModBlocks.AMERICAN_BEECH_WOOD);
		generator.woodProvider(ModBlocks.STRIPPED_AMERICAN_BEECH_LOG)
			.log(ModBlocks.STRIPPED_AMERICAN_BEECH_LOG)
			.wood(ModBlocks.STRIPPED_AMERICAN_BEECH_WOOD);
		generator.woodProvider(ModBlocks.BLACK_WALNUT_LOG)
			.log(ModBlocks.BLACK_WALNUT_LOG)
			.wood(ModBlocks.BLACK_WALNUT_WOOD);
		generator.woodProvider(ModBlocks.STRIPPED_BLACK_WALNUT_LOG)
			.log(ModBlocks.STRIPPED_BLACK_WALNUT_LOG)
			.wood(ModBlocks.STRIPPED_BLACK_WALNUT_WOOD);
		generator.woodProvider(ModBlocks.HEWN_CHESTNUT_BEAM)
			.log(ModBlocks.HEWN_CHESTNUT_BEAM);
		generator.woodProvider(ModBlocks.HEWN_OAK_BEAM)
			.log(ModBlocks.HEWN_OAK_BEAM);
		generator.woodProvider(ModBlocks.HEWN_SPRUCE_BEAM)
			.log(ModBlocks.HEWN_SPRUCE_BEAM);

		createHewingStageModels(
			generator,
			ModBlocks.CHESTNUT_LOG,
			ModBlocks.CHESTNUT_HEWING_LOG,
			ModBlocks.HEWN_CHESTNUT_BEAM
		);
		createHewingStageModels(
			generator,
			Blocks.OAK_LOG,
			ModBlocks.OAK_HEWING_LOG,
			ModBlocks.HEWN_OAK_BEAM
		);
		createHewingStageModels(
			generator,
			Blocks.SPRUCE_LOG,
			ModBlocks.SPRUCE_HEWING_LOG,
			ModBlocks.HEWN_SPRUCE_BEAM
		);

		generator.family(ModBlocks.CHESTNUT_PLANKS)
			.stairs(ModBlocks.CHESTNUT_STAIRS)
			.slab(ModBlocks.CHESTNUT_SLAB)
			.fence(ModBlocks.CHESTNUT_FENCE)
			.fenceGate(ModBlocks.CHESTNUT_FENCE_GATE)
			.pressurePlate(ModBlocks.CHESTNUT_PRESSURE_PLATE)
			.button(ModBlocks.CHESTNUT_BUTTON);
		generator.family(ModBlocks.HEMLOCK_PLANKS)
			.stairs(ModBlocks.HEMLOCK_STAIRS)
			.slab(ModBlocks.HEMLOCK_SLAB)
			.fence(ModBlocks.HEMLOCK_FENCE)
			.fenceGate(ModBlocks.HEMLOCK_FENCE_GATE)
			.pressurePlate(ModBlocks.HEMLOCK_PRESSURE_PLATE)
			.button(ModBlocks.HEMLOCK_BUTTON);
		generator.family(ModBlocks.AMERICAN_BEECH_PLANKS)
			.stairs(ModBlocks.AMERICAN_BEECH_STAIRS)
			.slab(ModBlocks.AMERICAN_BEECH_SLAB)
			.fence(ModBlocks.AMERICAN_BEECH_FENCE)
			.fenceGate(ModBlocks.AMERICAN_BEECH_FENCE_GATE)
			.pressurePlate(ModBlocks.AMERICAN_BEECH_PRESSURE_PLATE)
			.button(ModBlocks.AMERICAN_BEECH_BUTTON);
		generator.family(ModBlocks.ROUGH_CHESTNUT_BOARDS)
			.stairs(ModBlocks.ROUGH_CHESTNUT_BOARD_STAIRS)
			.slab(ModBlocks.ROUGH_CHESTNUT_BOARD_SLAB);
		generator.family(ModBlocks.CHESTNUT_SHINGLES)
			.stairs(ModBlocks.CHESTNUT_SHINGLE_STAIRS)
			.slab(ModBlocks.CHESTNUT_SHINGLE_SLAB);
		generator.new BlockFamilyProvider(TextureMapping.cube(ModBlocks.SPLIT_CHESTNUT_RAILS))
			.fence(ModBlocks.SPLIT_CHESTNUT_RAILS);

		TextureMapping chinkedLogTextures = new TextureMapping()
			.put(
				TextureSlot.SIDE,
				TextureMapping.getBlockTexture(ModBlocks.CHINKED_CHESTNUT_LOGS)
			)
			.put(
				TextureSlot.END,
				TextureMapping.getBlockTexture(ModBlocks.HEWN_CHESTNUT_BEAM, "_top")
			);
		Identifier chinkedLogsModel = ModelTemplates.CUBE_COLUMN.create(
			ModBlocks.CHINKED_CHESTNUT_LOGS,
			chinkedLogTextures,
			generator.modelOutput
		);
		generator.blockStateOutput.accept(
			MultiVariantGenerator.dispatch(
				ModBlocks.CHINKED_CHESTNUT_LOGS,
				BlockModelGenerators.plainVariant(chinkedLogsModel)
			).with(
				PropertyDispatch.modify(BlockStateProperties.AXIS)
					.select(Direction.Axis.Y, BlockModelGenerators.NOP)
					.select(
						Direction.Axis.Z,
						BlockModelGenerators.X_ROT_90.then(
							BlockModelGenerators.UV_LOCK
						)
					)
					.select(
						Direction.Axis.X,
						BlockModelGenerators.X_ROT_90
							.then(BlockModelGenerators.Y_ROT_90)
							.then(BlockModelGenerators.UV_LOCK)
					)
			)
		);
		generator.registerSimpleItemModel(ModBlocks.CHINKED_CHESTNUT_LOGS, chinkedLogsModel);

		generator.family(ModBlocks.FIELDSTONE)
			.stairs(ModBlocks.FIELDSTONE_STAIRS)
			.slab(ModBlocks.FIELDSTONE_SLAB)
			.wall(ModBlocks.FIELDSTONE_WALL);
		generator.family(ModBlocks.DRESSED_FIELDSTONE)
			.stairs(ModBlocks.DRESSED_FIELDSTONE_STAIRS)
			.slab(ModBlocks.DRESSED_FIELDSTONE_SLAB)
			.wall(ModBlocks.DRESSED_FIELDSTONE_WALL);
		generator.createTrivialCube(ModBlocks.CHISELED_FIELDSTONE);
		generator.woodProvider(ModBlocks.FIELDSTONE_PIER)
			.log(ModBlocks.FIELDSTONE_PIER);
		generator.createTrivialCube(ModBlocks.ROUGH_OAK_BOARDS);
		generator.createTrivialCube(ModBlocks.ROUGH_SPRUCE_BOARDS);

		// The authored albedo remains the baseline. The vanilla leaves parent only
		// exposes tint layer 0; the client returns opaque white unless the optional
		// seasonal bridge has a temperate Overworld phase to apply.
		ModelTemplate seasonallyTintedLeaves = new ModelTemplate(
			Optional.of(Identifier.withDefaultNamespace("block/leaves")),
			Optional.empty(),
			TextureSlot.ALL
		);
		Identifier leavesModel = seasonallyTintedLeaves.create(
			ModBlocks.CHESTNUT_LEAVES,
			TextureMapping.cube(ModBlocks.CHESTNUT_LEAVES),
			generator.modelOutput
		);
		generator.blockStateOutput.accept(
			BlockModelGenerators.createSimpleBlock(
				ModBlocks.CHESTNUT_LEAVES,
				BlockModelGenerators.plainVariant(leavesModel)
			)
		);
		generator.registerSimpleItemModel(ModBlocks.CHESTNUT_LEAVES, leavesModel);
		generator.createCrossBlockWithDefaultItem(
			ModBlocks.CHESTNUT_SAPLING,
			BlockModelGenerators.PlantType.NOT_TINTED
		);
		Identifier beechLeavesModel = seasonallyTintedLeaves.create(
			ModBlocks.AMERICAN_BEECH_LEAVES,
			TextureMapping.cube(ModBlocks.AMERICAN_BEECH_LEAVES),
			generator.modelOutput
		);
		generator.blockStateOutput.accept(
			BlockModelGenerators.createSimpleBlock(
				ModBlocks.AMERICAN_BEECH_LEAVES,
				BlockModelGenerators.plainVariant(beechLeavesModel)
			)
		);
		generator.registerSimpleItemModel(ModBlocks.AMERICAN_BEECH_LEAVES, beechLeavesModel);
		Identifier blackWalnutLeavesModel = seasonallyTintedLeaves.create(
			ModBlocks.BLACK_WALNUT_LEAVES,
			TextureMapping.cube(ModBlocks.BLACK_WALNUT_LEAVES),
			generator.modelOutput
		);
		generator.blockStateOutput.accept(
			BlockModelGenerators.createSimpleBlock(
				ModBlocks.BLACK_WALNUT_LEAVES,
				BlockModelGenerators.plainVariant(blackWalnutLeavesModel)
			)
		);
		generator.registerSimpleItemModel(ModBlocks.BLACK_WALNUT_LEAVES, blackWalnutLeavesModel);
		generator.createCrossBlockWithDefaultItem(
			ModBlocks.HEMLOCK_SAPLING,
			BlockModelGenerators.PlantType.NOT_TINTED
		);
		generator.createCrossBlockWithDefaultItem(
			ModBlocks.AMERICAN_BEECH_SAPLING,
			BlockModelGenerators.PlantType.NOT_TINTED
		);
		generator.createCrossBlockWithDefaultItem(
			ModBlocks.BLACK_WALNUT_SAPLING,
			BlockModelGenerators.PlantType.NOT_TINTED
		);
		createCornCropModels(generator);

		TextureMapping pileTextures = TextureMapping.cube(ModBlocks.CHESTNUT_PILE);
		Map<Integer, Identifier> pileModels = new HashMap<>();
		for (int layers = 1; layers < 8; layers++) {
			int height = layers * 2;
			ModelTemplate layerTemplate = new ModelTemplate(
				Optional.of(Identifier.withDefaultNamespace("block/snow_height" + height)),
				Optional.empty(),
				TextureSlot.TEXTURE,
				TextureSlot.PARTICLE
			);
			pileModels.put(
				layers,
				layerTemplate.createWithSuffix(
					ModBlocks.CHESTNUT_PILE,
					"_height" + height,
					pileTextures,
					generator.modelOutput
				)
			);
		}
		pileModels.put(
			8,
			ModelTemplates.CUBE_ALL.create(
				ModBlocks.CHESTNUT_PILE,
				pileTextures,
				generator.modelOutput
			)
		);
		generator.blockStateOutput.accept(
			MultiVariantGenerator.dispatch(ModBlocks.CHESTNUT_PILE)
				.with(
					PropertyDispatch.<Integer>initial(SnowLayerBlock.LAYERS)
						.generate(layers -> BlockModelGenerators.plainVariant(pileModels.get(layers)))
				)
		);
	}

	@Override
	public void generateItemModels(ItemModelGenerators generator) {
		generator.generateFlatItem(ModItems.BROAD_AXE, ModelTemplates.FLAT_HANDHELD_ITEM);
		generator.generateFlatItem(ModItems.HANDFUL_OF_CHESTNUTS, ModelTemplates.FLAT_ITEM);
		generator.generateFlatItem(ModItems.ROASTED_CHESTNUTS, ModelTemplates.FLAT_ITEM);
		generator.generateFlatItem(ModItems.EAR_OF_CORN, ModelTemplates.FLAT_ITEM);
		generator.generateFlatItem(ModItems.CORNMEAL, ModelTemplates.FLAT_ITEM);
	}

	@Override
	public String getName() {
		return "Before the Blight Models";
	}

	private static void createHewingStageModels(
		BlockModelGenerators generator,
		Block sourceLog,
		HewingLogBlock stagedLog,
		Block hewnBeam
	) {
		Map<Integer, Identifier> stageModels = new HashMap<>();
		for (int stage : HewingLogBlock.HEWING_STAGE.getPossibleValues()) {
			stageModels.put(
				stage,
				createHewingStageModel(generator, sourceLog, stagedLog, hewnBeam, stage)
			);
		}
		generator.blockStateOutput.accept(
			MultiVariantGenerator.dispatch(stagedLog)
				.with(
					PropertyDispatch.<Integer>initial(HewingLogBlock.HEWING_STAGE)
						.generate(stage -> BlockModelGenerators.plainVariant(stageModels.get(stage)))
				)
				.with(BlockModelGenerators.createRotatedPillar())
		);
	}

	private static void createCornCropModels(BlockModelGenerators generator) {
		generator.registerSimpleFlatItemModel(ModItems.CORN_KERNELS);
		Map<Integer, Identifier> stageModels = new HashMap<>();
		for (int age : CornCropBlock.AGE.getPossibleValues()) {
			String suffix = "_stage_" + age;
			stageModels.put(
				age,
				ModelTemplates.CROP.createWithSuffix(
					ModBlocks.CORN,
					suffix,
					TextureMapping.crop(TextureMapping.getBlockTexture(ModBlocks.CORN, suffix)),
					generator.modelOutput
				)
			);
		}
		generator.blockStateOutput.accept(
			MultiVariantGenerator.dispatch(ModBlocks.CORN)
				.with(
					PropertyDispatch.<Integer>initial(CornCropBlock.AGE)
						.generate(age -> BlockModelGenerators.plainVariant(stageModels.get(age)))
				)
		);
	}

	private static Identifier createHewingStageModel(
		BlockModelGenerators generator,
		Block sourceLog,
		HewingLogBlock stagedLog,
		Block hewnBeam,
		int stage
	) {
		Material bark = TextureMapping.getBlockTexture(sourceLog);
		Material hewn = TextureMapping.getBlockTexture(hewnBeam);
		Material end = TextureMapping.getBlockTexture(sourceLog, "_top");
		TextureMapping textures = new TextureMapping()
			.put(TextureSlot.PARTICLE, bark)
			.put(TextureSlot.NORTH, hewn)
			.put(TextureSlot.EAST, stage >= 2 ? hewn : bark)
			.put(TextureSlot.SOUTH, stage >= 3 ? hewn : bark)
			.put(TextureSlot.WEST, bark)
			.put(TextureSlot.UP, end)
			.put(TextureSlot.DOWN, end);

		return ModelTemplates.CUBE.createWithSuffix(
			stagedLog,
			"_stage" + stage,
			textures,
			generator.modelOutput
		);
	}
}
