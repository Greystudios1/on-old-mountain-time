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

public final class AppalachianHomesteadPiece extends TemplateStructurePiece {
	public static final int MAX_FOUNDATION_EXTENSION = 4;
	private static final BlockPos ENTRANCE_PATH = new BlockPos(7, 1, 13);

	public AppalachianHomesteadPiece(
		StructureTemplateManager structureTemplateManager,
		BlockPos position
	) {
		super(
			ModStructureTypes.APPALACHIAN_HOMESTEAD_PIECE,
			0,
			structureTemplateManager,
			AppalachianHomesteadStructure.TEMPLATE_ID,
			AppalachianHomesteadStructure.TEMPLATE_ID.toString(),
			placementSettings(),
			position
		);
	}

	public AppalachianHomesteadPiece(
		StructureTemplateManager structureTemplateManager,
		CompoundTag tag
	) {
		super(
			ModStructureTypes.APPALACHIAN_HOMESTEAD_PIECE,
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
		// The deterministic cabin template intentionally contains no data markers.
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
		extendFieldstoneFoundation(level, chunkBB);
		keepEntranceClear(level, chunkBB);
	}

	private void extendFieldstoneFoundation(WorldGenLevel level, BoundingBox chunkBB) {
		for (int localX = 0; localX < AppalachianHomesteadStructure.TEMPLATE_WIDTH; localX++) {
			for (int localZ = 0; localZ < AppalachianHomesteadStructure.TEMPLATE_DEPTH; localZ++) {
				BlockPos base = worldPosition(new BlockPos(localX, 0, localZ));
				if (!chunkBB.isInside(base)) {
					continue;
				}
				BlockState foundation = level.getBlockState(base);
				if (!foundation.is(ModBlocks.FIELDSTONE)
					&& !foundation.is(Blocks.COBBLESTONE)
					&& !foundation.is(Blocks.STONE_BRICKS)) {
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

	private void keepEntranceClear(WorldGenLevel level, BoundingBox chunkBB) {
		BlockPos path = worldPosition(ENTRANCE_PATH);
		if (!chunkBB.isInside(path)) {
			return;
		}
		fillDown(
			level,
			chunkBB,
			path.below(),
			Blocks.DIRT.defaultBlockState(),
			MAX_FOUNDATION_EXTENSION
		);
		level.setBlock(path, Blocks.DIRT_PATH.defaultBlockState(), 2);
		for (int offsetY = 1; offsetY <= 2; offsetY++) {
			BlockPos clearance = path.above(offsetY);
			if (chunkBB.isInside(clearance)) {
				level.setBlock(clearance, Blocks.AIR.defaultBlockState(), 2);
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
