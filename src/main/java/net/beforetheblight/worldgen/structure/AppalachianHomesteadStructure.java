package net.beforetheblight.worldgen.structure;

import java.util.function.IntBinaryOperator;

import com.mojang.serialization.MapCodec;
import net.beforetheblight.BeforeTheBlight;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/** A fixed-orientation, terrain-screened home for the demo cabin template. */
public final class AppalachianHomesteadStructure extends AbstractAppalachianTemplateStructure {
	public static final MapCodec<AppalachianHomesteadStructure> CODEC = simpleCodec(
		AppalachianHomesteadStructure::new
	);
	public static final Identifier TEMPLATE_ID = BeforeTheBlight.id("appalachian_demo_cabin");
	public static final int TEMPLATE_WIDTH = 17;
	public static final int TEMPLATE_HEIGHT = 14;
	public static final int TEMPLATE_DEPTH = 13;
	public static final int SITE_DEPTH = TEMPLATE_DEPTH + 1;
	public static final int MAX_SITE_RELIEF = 3;

	public AppalachianHomesteadStructure(Structure.StructureSettings settings) {
		super(
			settings,
			TEMPLATE_ID,
			TEMPLATE_WIDTH,
			TEMPLATE_HEIGHT,
			TEMPLATE_DEPTH,
			SITE_DEPTH,
			MAX_SITE_RELIEF
		);
	}

	@Override
	protected StructurePiece createPiece(
		StructureTemplateManager structureTemplateManager,
		BlockPos templateOrigin
	) {
		return new AppalachianHomesteadPiece(structureTemplateManager, templateOrigin);
	}

	public static SiteProfile sampleSite(
		IntBinaryOperator surfaceHeight,
		int originX,
		int originZ
	) {
		return sampleSite(surfaceHeight, surfaceHeight, originX, originZ);
	}

	public static SiteProfile sampleSite(
		IntBinaryOperator surfaceHeight,
		IntBinaryOperator oceanFloorHeight,
		int originX,
		int originZ
	) {
		TemplateSiteProfile profile = sampleTemplateSite(
			surfaceHeight,
			oceanFloorHeight,
			originX,
			originZ,
			TEMPLATE_WIDTH,
			SITE_DEPTH,
			MAX_SITE_RELIEF
		);
		return new SiteProfile(
			profile.minimumSurfaceY(),
			profile.maximumSurfaceY(),
			profile.sampledColumns(),
			profile.dryColumns()
		);
	}

	public static BlockPos templateOriginForChunk(ChunkPos chunkPos, int foundationY) {
		return new BlockPos(
			chunkPos.getMiddleBlockX() - TEMPLATE_WIDTH / 2,
			foundationY,
			chunkPos.getMiddleBlockZ() - TEMPLATE_DEPTH / 2
		);
	}

	@Override
	public StructureType<?> type() {
		return ModStructureTypes.APPALACHIAN_HOMESTEAD;
	}

	public record SiteProfile(
		int minimumSurfaceY,
		int maximumSurfaceY,
		int sampledColumns,
		int dryColumns
	) {
		public int relief() {
			return maximumSurfaceY - minimumSurfaceY;
		}

		public int foundationY() {
			return maximumSurfaceY - 1;
		}

		public boolean isBuildable() {
			return sampledColumns == TEMPLATE_WIDTH * SITE_DEPTH
				&& dryColumns == TEMPLATE_WIDTH * SITE_DEPTH
				&& relief() <= MAX_SITE_RELIEF;
		}
	}
}
