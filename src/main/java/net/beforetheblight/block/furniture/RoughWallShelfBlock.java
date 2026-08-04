package net.beforetheblight.block.furniture;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A rough chestnut wall shelf with two visible triangular-support volumes. */
public final class RoughWallShelfBlock extends WallMountedFurnitureBlock {
	public static final MapCodec<RoughWallShelfBlock> CODEC =
		simpleCodec(RoughWallShelfBlock::new);

	private static final VoxelShape NORTH_SHAPE = Shapes.or(
		// Shelf board; the south edge is mounted against the wall.
		Block.box(1, 9, 8, 15, 10.5, 16),
		// Two support brackets match the visible model volumes.
		Block.box(2, 4, 12, 4, 9.25, 15.5),
		Block.box(12, 4, 12, 14, 9.25, 15.5)
	);

	public RoughWallShelfBlock(BlockBehaviour.Properties properties) {
		super(properties, NORTH_SHAPE);
	}

	@Override
	public MapCodec<RoughWallShelfBlock> codec() {
		return CODEC;
	}
}
