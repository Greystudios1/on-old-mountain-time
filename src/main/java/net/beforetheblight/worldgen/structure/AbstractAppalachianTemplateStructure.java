package net.beforetheblight.worldgen.structure;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/** Shared fixed-template placement and terrain screening for Appalachian outbuildings. */
abstract class AbstractAppalachianTemplateStructure extends Structure {
	private final Identifier templateId;
	private final int templateWidth;
	private final int templateHeight;
	private final int templateDepth;
	private final int siteDepth;
	private final int maximumSiteRelief;

	protected AbstractAppalachianTemplateStructure(
		Structure.StructureSettings settings,
		Identifier templateId,
		int templateWidth,
		int templateHeight,
		int templateDepth,
		int siteDepth,
		int maximumSiteRelief
	) {
		super(settings);
		this.templateId = templateId;
		this.templateWidth = templateWidth;
		this.templateHeight = templateHeight;
		this.templateDepth = templateDepth;
		this.siteDepth = siteDepth;
		this.maximumSiteRelief = maximumSiteRelief;
	}

	@Override
	protected final Optional<Structure.GenerationStub> findGenerationPoint(
		Structure.GenerationContext context
	) {
		StructureTemplate template = context.structureTemplateManager()
			.get(templateId)
			.orElse(null);
		if (template == null) {
			return Optional.empty();
		}
		Vec3i templateSize = template.getSize();
		if (templateSize.getX() != templateWidth
			|| templateSize.getY() != templateHeight
			|| templateSize.getZ() != templateDepth) {
			return Optional.empty();
		}

		ChunkPos chunkPos = context.chunkPos();
		int centerX = chunkPos.getMiddleBlockX();
		int centerZ = chunkPos.getMiddleBlockZ();
		int centerSurfaceY = context.chunkGenerator().getFirstOccupiedHeight(
			centerX,
			centerZ,
			Heightmap.Types.WORLD_SURFACE_WG,
			context.heightAccessor(),
			context.randomState()
		);
		if (!surfaceBiomeMatches(context, centerX, centerZ, centerSurfaceY)) {
			return Optional.empty();
		}

		int originX = chunkPos.getMiddleBlockX() - templateWidth / 2;
		int originZ = chunkPos.getMiddleBlockZ() - templateDepth / 2;
		TemplateSiteProfile profile = sampleTemplateSite(
			(x, z) -> x == centerX && z == centerZ
				? centerSurfaceY
				: context.chunkGenerator().getFirstOccupiedHeight(
					x,
					z,
					Heightmap.Types.WORLD_SURFACE_WG,
					context.heightAccessor(),
					context.randomState()
				),
			(x, z) -> context.chunkGenerator().getFirstOccupiedHeight(
				x,
				z,
				Heightmap.Types.OCEAN_FLOOR_WG,
				context.heightAccessor(),
				context.randomState()
			),
			originX,
			originZ,
			templateWidth,
			siteDepth,
			maximumSiteRelief
		);
		if (!profile.isBuildable()) {
			return Optional.empty();
		}

		int foundationY = profile.foundationY();
		if (foundationY < context.heightAccessor().getMinY()
			|| foundationY + templateHeight - 1 > context.heightAccessor().getMaxY()) {
			return Optional.empty();
		}

		BlockPos templateOrigin = new BlockPos(originX, foundationY, originZ);
		BlockPos biomeProbe = new BlockPos(
			chunkPos.getMiddleBlockX(),
			foundationY + 1,
			chunkPos.getMiddleBlockZ()
		);
		return Optional.of(
			new Structure.GenerationStub(
				biomeProbe,
				builder -> builder.addPiece(
					createPiece(context.structureTemplateManager(), templateOrigin)
				)
			)
		);
	}

	private boolean surfaceBiomeMatches(
		Structure.GenerationContext context,
		int blockX,
		int blockZ,
		int surfaceY
	) {
		/*
		 * The final vanilla biome probe is at maximumSurfaceY. On any
		 * buildable site it can be from zero through maximumSiteRelief blocks
		 * above this center-column surface, so inspect that complete bounded
		 * range before paying for the full footprint.
		 */
		for (int offsetY = 0; offsetY <= maximumSiteRelief; offsetY++) {
			var biome = context.biomeSource().getNoiseBiome(
				QuartPos.fromBlock(blockX),
				QuartPos.fromBlock(surfaceY + offsetY),
				QuartPos.fromBlock(blockZ),
				context.randomState().sampler()
			);
			if (context.validBiome().test(biome)) {
				return true;
			}
		}
		return false;
	}

	protected abstract StructurePiece createPiece(
		StructureTemplateManager structureTemplateManager,
		BlockPos templateOrigin
	);

	protected static TemplateSiteProfile sampleTemplateSite(
		java.util.function.IntBinaryOperator surfaceHeight,
		java.util.function.IntBinaryOperator oceanFloorHeight,
		int originX,
		int originZ,
		int width,
		int depth,
		int maximumRelief
	) {
		java.util.Objects.requireNonNull(surfaceHeight, "surfaceHeight");
		java.util.Objects.requireNonNull(oceanFloorHeight, "oceanFloorHeight");
		int minimum = Integer.MAX_VALUE;
		int maximum = Integer.MIN_VALUE;
		int samples = 0;
		int dryColumns = 0;
		for (int offsetX = 0; offsetX < width; offsetX++) {
			for (int offsetZ = 0; offsetZ < depth; offsetZ++) {
				int x = originX + offsetX;
				int z = originZ + offsetZ;
				int height = surfaceHeight.applyAsInt(x, z);
				minimum = Math.min(minimum, height);
				maximum = Math.max(maximum, height);
				if (height == oceanFloorHeight.applyAsInt(x, z)) {
					dryColumns++;
				}
				samples++;
			}
		}
		return new TemplateSiteProfile(
			minimum,
			maximum,
			samples,
			dryColumns,
			width * depth,
			maximumRelief
		);
	}

	protected record TemplateSiteProfile(
		int minimumSurfaceY,
		int maximumSurfaceY,
		int sampledColumns,
		int dryColumns,
		int expectedColumns,
		int maximumRelief
	) {
		int relief() {
			return maximumSurfaceY - minimumSurfaceY;
		}

		int foundationY() {
			return maximumSurfaceY - 1;
		}

		boolean isBuildable() {
			return sampledColumns == expectedColumns
				&& dryColumns == expectedColumns
				&& relief() <= maximumRelief;
		}
	}
}
