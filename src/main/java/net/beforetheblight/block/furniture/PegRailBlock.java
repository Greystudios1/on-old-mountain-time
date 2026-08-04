package net.beforetheblight.block.furniture;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A wall-mounted rail with three projecting wooden pegs. */
public final class PegRailBlock extends WallMountedFurnitureBlock {
	public static final MapCodec<PegRailBlock> CODEC = simpleCodec(PegRailBlock::new);

	private static final VoxelShape NORTH_SHAPE = Shapes.or(
		Block.box(1, 7.5, 14, 15, 10.5, 16),
		Block.box(3, 7.75, 10, 4, 8.75, 14.25),
		Block.box(6, 7.75, 10, 7, 8.75, 14.25),
		Block.box(9, 7.75, 10, 10, 8.75, 14.25),
		Block.box(12, 7.75, 10, 13, 8.75, 14.25)
	);

	public PegRailBlock(BlockBehaviour.Properties properties) {
		super(properties, NORTH_SHAPE);
	}

	@Override
	public MapCodec<PegRailBlock> codec() {
		return CODEC;
	}
}
