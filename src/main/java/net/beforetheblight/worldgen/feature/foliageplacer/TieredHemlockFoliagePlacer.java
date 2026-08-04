package net.beforetheblight.worldgen.feature.foliageplacer;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;

/**
 * Places compact, open sprays around the log attachments of a tiered hemlock.
 *
 * <p>The two middle rows carry cardinal sprays and alternating diagonal pairs;
 * the cap and skirt retain only a one-block cross. This keeps neighboring
 * branch clusters face-connected without merging them into filled horizontal
 * leaf slabs. At the configured radius and offset, every attempted leaf lies
 * at most three face steps from its attachment log.</p>
 */
public final class TieredHemlockFoliagePlacer extends FoliagePlacer {
	public static final MapCodec<TieredHemlockFoliagePlacer> CODEC =
		RecordCodecBuilder.mapCodec(
			instance -> foliagePlacerParts(instance).apply(
				instance,
				TieredHemlockFoliagePlacer::new
			)
		);

	public static final int FOLIAGE_HEIGHT = 3;
	public static final int MAX_SUPPORT_DISTANCE = 3;

	public TieredHemlockFoliagePlacer(IntProvider radius, IntProvider offset) {
		super(radius, offset);
	}

	@Override
	protected FoliagePlacerType<?> type() {
		return ModFoliagePlacerTypes.TIERED_HEMLOCK_FOLIAGE_PLACER;
	}

	@Override
	public int foliageHeight(
		RandomSource random,
		int treeHeight,
		TreeConfiguration configuration
	) {
		return FOLIAGE_HEIGHT;
	}

	@Override
	protected void createFoliage(
		WorldGenLevel level,
		FoliageSetter foliageSetter,
		RandomSource random,
		TreeConfiguration configuration,
		int treeHeight,
		FoliageAttachment foliageAttachment,
		int foliageHeight,
		int leafRadius,
		int offset
	) {
		BlockPos attachment = foliageAttachment.pos();
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		int sprayParity = Math.floorMod(
			attachment.getX() + attachment.getY() + attachment.getZ(),
			2
		);

		for (int layer = 0; layer <= foliageHeight; layer++) {
			boolean middleRow = layer > 0 && layer < foliageHeight;
			int reach = middleRow ? leafRadius : 1;
			for (int dx = -reach; dx <= reach; dx++) {
				for (int dz = -reach; dz <= reach; dz++) {
					if (!placesSprayLeaf(dx, dz, reach, middleRow, sprayParity + layer)) {
						continue;
					}
					cursor.setWithOffset(attachment, dx, offset - layer, dz);
					tryPlaceLeaf(level, foliageSetter, random, configuration, cursor);
				}
			}
		}
	}

	private static boolean placesSprayLeaf(
		int dx,
		int dz,
		int reach,
		boolean middleRow,
		int parity
	) {
		int absX = Math.abs(dx);
		int absZ = Math.abs(dz);
		if (absX + absZ <= 1) {
			return true;
		}
		if (!middleRow) {
			return false;
		}
		if ((dx == 0 || dz == 0) && absX + absZ <= reach) {
			return true;
		}
		if (absX != 1 || absZ != 1) {
			return false;
		}
		return Math.floorMod(parity, 2) == 0
			? dx == dz
			: dx == -dz;
	}

	@Override
	protected boolean shouldSkipLocation(
		RandomSource random,
		int dx,
		int y,
		int dz,
		int currentRadius,
		boolean doubleTrunk
	) {
		// createFoliage places the signed, porous stencil directly.
		return false;
	}
}
