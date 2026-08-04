package net.beforetheblight.worldgen.placement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraft.util.RandomSource;

/**
 * Emits one seed-phased, globally staggered chestnut lattice. The lattice is
 * independent of chunk borders: neighboring rows are eleven blocks apart and
 * offset six blocks along a twelve-block pitch, so nominal neighboring sites
 * are 12 or sqrt(157) blocks apart.
 */
public final class StaggeredChestnutPlacement extends PlacementModifier {
	public static final StaggeredChestnutPlacement INSTANCE = new StaggeredChestnutPlacement();
	public static final MapCodec<StaggeredChestnutPlacement> CODEC = MapCodec.unit(() -> INSTANCE);

	public static final int ALONG_ROW_PITCH = 12;
	public static final int ROW_PITCH = 11;
	public static final int ALTERNATE_ROW_OFFSET = 6;
	private static final int CHUNK_SIZE = 16;
	private static final long PHASE_SALT = 0x6A09E667F3BCC909L;

	private StaggeredChestnutPlacement() {
	}

	@Override
	public Stream<BlockPos> getPositions(
		PlacementContext context,
		RandomSource random,
		BlockPos origin
	) {
		int chunkX = Math.floorDiv(origin.getX(), CHUNK_SIZE);
		int chunkZ = Math.floorDiv(origin.getZ(), CHUNK_SIZE);
		return positionsForChunk(context.getLevel().getSeed(), chunkX, chunkZ, origin.getY())
			.stream();
	}

	/**
	 * Returns exactly the lattice columns owned by one chunk. This pure helper is
	 * also the regression surface for negative coordinates and chunk seams.
	 */
	public static List<BlockPos> positionsForChunk(long seed, int chunkX, int chunkZ, int y) {
		long mixedSeed = mix64(seed ^ PHASE_SALT);
		int alongPhase = (int) Math.floorMod(mixedSeed, (long) ALONG_ROW_PITCH);
		int rowPhase = (int) Math.floorMod(mix64(mixedSeed), (long) ROW_PITCH);
		boolean rotated = (mix64(mixedSeed ^ 0xBB67AE8584CAA73BL) & 1L) != 0L;

		int minX = chunkX * CHUNK_SIZE;
		int minZ = chunkZ * CHUNK_SIZE;
		int maxX = minX + CHUNK_SIZE - 1;
		int maxZ = minZ + CHUNK_SIZE - 1;
		List<BlockPos> positions = new ArrayList<>(4);
		if (rotated) {
			emitPattern(
				positions,
				minZ,
				maxZ,
				minX,
				maxX,
				alongPhase,
				rowPhase,
				y,
				true
			);
		} else {
			emitPattern(
				positions,
				minX,
				maxX,
				minZ,
				maxZ,
				alongPhase,
				rowPhase,
				y,
				false
			);
		}
		return List.copyOf(positions);
	}

	private static void emitPattern(
		List<BlockPos> positions,
		int minimumAlong,
		int maximumAlong,
		int minimumRowAxis,
		int maximumRowAxis,
		int alongPhase,
		int rowPhase,
		int y,
		boolean rotated
	) {
		long firstRow = ceilDiv((long) minimumRowAxis - rowPhase, ROW_PITCH);
		long lastRow = Math.floorDiv((long) maximumRowAxis - rowPhase, ROW_PITCH);
		for (long row = firstRow; row <= lastRow; row++) {
			long rowCoordinate = rowPhase + row * ROW_PITCH;
			long stagger = Math.floorMod(row, 2L) == 0L ? 0L : ALTERNATE_ROW_OFFSET;
			long rowAlongPhase = alongPhase + stagger;
			long firstColumn = ceilDiv((long) minimumAlong - rowAlongPhase, ALONG_ROW_PITCH);
			long lastColumn = Math.floorDiv((long) maximumAlong - rowAlongPhase, ALONG_ROW_PITCH);
			for (long column = firstColumn; column <= lastColumn; column++) {
				int along = Math.toIntExact(rowAlongPhase + column * ALONG_ROW_PITCH);
				int rowAxis = Math.toIntExact(rowCoordinate);
				positions.add(rotated
					? new BlockPos(rowAxis, y, along)
					: new BlockPos(along, y, rowAxis));
			}
		}
	}

	private static long ceilDiv(long value, int divisor) {
		return -Math.floorDiv(-value, divisor);
	}

	private static long mix64(long value) {
		value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
		value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
		return value ^ (value >>> 31);
	}

	@Override
	public PlacementModifierType<?> type() {
		return ModPlacementModifierTypes.STAGGERED_CHESTNUT;
	}
}
