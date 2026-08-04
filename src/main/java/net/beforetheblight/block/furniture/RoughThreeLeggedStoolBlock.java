package net.beforetheblight.block.furniture;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A low, sit-able three-legged stool suitable for dairy work. */
public final class RoughThreeLeggedStoolBlock extends SeatingFurnitureBlock {
	public static final MapCodec<RoughThreeLeggedStoolBlock> CODEC =
		simpleCodec(RoughThreeLeggedStoolBlock::new);

	private static final VoxelShape NORTH_SHAPE = Shapes.or(
		// Two front legs and one centered rear leg make the three-leg form
		// stable and visibly distinct from a cut-down table.
		Block.box(3, 0, 3, 5, 7.25, 5),
		Block.box(11, 0, 3, 13, 7.25, 5),
		Block.box(7, 0, 11, 9, 7.25, 13),
		Block.box(3, 6.75, 3, 13, 8.25, 13)
	);

	public RoughThreeLeggedStoolBlock(BlockBehaviour.Properties properties) {
		super(properties, NORTH_SHAPE);
	}

	@Override
	public MapCodec<RoughThreeLeggedStoolBlock> codec() {
		return CODEC;
	}

	@Override
	public double seatHeight() {
		return 0.53125D;
	}
}
