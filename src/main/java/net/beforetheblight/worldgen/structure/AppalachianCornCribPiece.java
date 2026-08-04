package net.beforetheblight.worldgen.structure;

import net.beforetheblight.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public final class AppalachianCornCribPiece extends TemplateStructurePiece {
	public static final int MAX_FOUNDATION_EXTENSION = 4;
	private static final BlockPos ENTRY_STEP = new BlockPos(4, 1, 9);
	private static final BlockPos INNER_APPROACH = new BlockPos(4, 0, 10);
	private static final BlockPos OUTER_APPROACH = new BlockPos(4, 0, 11);

	public AppalachianCornCribPiece(
		StructureTemplateManager structureTemplateManager,
		BlockPos position
	) {
		super(
			ModStructureTypes.APPALACHIAN_CORN_CRIB_PIECE,
			0,
			structureTemplateManager,
			AppalachianCornCribStructure.TEMPLATE_ID,
			AppalachianCornCribStructure.TEMPLATE_ID.toString(),
			placementSettings(),
			position
		);
	}

	public AppalachianCornCribPiece(
		StructureTemplateManager structureTemplateManager,
		CompoundTag tag
	) {
		super(
			ModStructureTypes.APPALACHIAN_CORN_CRIB_PIECE,
			tag,
			structureTemplateManager,
			ignored -> placementSettings()
		);
	}

	private static StructurePlaceSettings placementSettings() {
		return new StructurePlaceSettings()
			.setRotation(Rotation.NONE)
			.setMirror(Mirror.NONE)
			.addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK)
			.setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING);
	}

	@Override
	protected void handleDataMarker(
		String markerId,
		BlockPos position,
		ServerLevelAccessor level,
		RandomSource random,
		BoundingBox chunkBB
	) {
		// The deterministic crib template intentionally contains no data markers.
	}

	@Override
	public void postProcess(
		WorldGenLevel level,
		StructureManager structureManager,
		ChunkGenerator generator,
		RandomSource random,
		BoundingBox chunkBB,
		ChunkPos chunkPos,
		BlockPos referencePos
	) {
		super.postProcess(level, structureManager, generator, random, chunkBB, chunkPos, referencePos);
		extendPierFoundations(level, chunkBB);
		keepSouthApproachClear(level, chunkBB);
	}

	private void extendPierFoundations(WorldGenLevel level, BoundingBox chunkBB) {
		for (int localX = 0; localX < AppalachianCornCribStructure.TEMPLATE_WIDTH; localX++) {
			for (int localZ = 0; localZ < AppalachianCornCribStructure.TEMPLATE_DEPTH; localZ++) {
				BlockPos base = worldPosition(new BlockPos(localX, 0, localZ));
				if (!chunkBB.isInside(base) || !level.getBlockState(base).is(ModBlocks.FIELDSTONE)) {
					continue;
				}
				fillDown(
					level,
					chunkBB,
					base.below(),
					ModBlocks.FIELDSTONE.defaultBlockState(),
					MAX_FOUNDATION_EXTENSION
				);
			}
		}
	}

	private void keepSouthApproachClear(WorldGenLevel level, BoundingBox chunkBB) {
		for (BlockPos localPath : new BlockPos[] {INNER_APPROACH, OUTER_APPROACH}) {
			BlockPos path = worldPosition(localPath);
			if (!chunkBB.isInside(path)) {
				continue;
			}
			fillDown(
				level,
				chunkBB,
				path.below(),
				Blocks.DIRT.defaultBlockState(),
				MAX_FOUNDATION_EXTENSION
			);
			level.setBlock(path, Blocks.DIRT_PATH.defaultBlockState(), 2);
			clearAbove(level, chunkBB, path, 2);
		}

		BlockPos step = worldPosition(ENTRY_STEP);
		clearAbove(level, chunkBB, step, 2);
	}

	private static void clearAbove(
		WorldGenLevel level,
		BoundingBox chunkBB,
		BlockPos surface,
		int clearance
	) {
		for (int offsetY = 1; offsetY <= clearance; offsetY++) {
			BlockPos position = surface.above(offsetY);
			if (chunkBB.isInside(position)) {
				level.setBlock(position, Blocks.AIR.defaultBlockState(), 2);
			}
		}
	}

	private BlockPos worldPosition(BlockPos localPosition) {
		return this.templatePosition.offset(
			StructureTemplate.calculateRelativePosition(this.placeSettings, localPosition)
		);
	}

	private static void fillDown(
		WorldGenLevel level,
		BoundingBox chunkBB,
		BlockPos start,
		BlockState fill,
		int maximumDepth
	) {
		BlockPos.MutableBlockPos cursor = start.mutable();
		for (int depth = 0; depth < maximumDepth && chunkBB.isInside(cursor); depth++) {
			BlockState existing = level.getBlockState(cursor);
			if (!existing.isAir()
				&& existing.getFluidState().isEmpty()
				&& !existing.canBeReplaced()) {
				break;
			}
			level.setBlock(cursor, fill, 2);
			cursor.move(0, -1, 0);
		}
	}
}
