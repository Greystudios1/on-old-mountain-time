package net.beforetheblight.block.furniture;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A static, sit-able Appalachian ladder-back chair with a 7/16 seat. */
public final class LadderBackChairBlock extends SeatingFurnitureBlock {
	public static final MapCodec<LadderBackChairBlock> CODEC =
		simpleCodec(LadderBackChairBlock::new);

	private static final VoxelShape NORTH_SHAPE = Shapes.or(
		// Four joined legs.
		Block.box(2, 0, 3, 4, 7, 5),
		Block.box(12, 0, 3, 14, 7, 5),
		Block.box(2, 0, 11, 4, 8, 13),
		Block.box(12, 0, 11, 14, 8, 13),
		// Seat surface: visible top and collision both end at 8/16.
		Block.box(2, 7, 3, 14, 8, 13),
		// Back posts and ladder slats.
		Block.box(2, 7, 11, 4, 16, 14),
		Block.box(12, 7, 11, 14, 16, 14),
		Block.box(4, 10, 12, 12, 12, 14),
		Block.box(4, 14, 12, 12, 16, 14)
	);

	public LadderBackChairBlock(BlockBehaviour.Properties properties) {
		super(properties, NORTH_SHAPE);
	}

	@Override
	public MapCodec<LadderBackChairBlock> codec() {
		return CODEC;
	}

	@Override
	public double seatHeight() {
		return 0.515625D;
	}
}
