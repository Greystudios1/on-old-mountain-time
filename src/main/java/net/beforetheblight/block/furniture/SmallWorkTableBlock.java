package net.beforetheblight.block.furniture;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A one-block rough work table with open space between its four legs. */
public final class SmallWorkTableBlock extends DirectionalFurnitureBlock {
	public static final MapCodec<SmallWorkTableBlock> CODEC =
		simpleCodec(SmallWorkTableBlock::new);

	private static final VoxelShape NORTH_SHAPE = Shapes.or(
		// Four legs and shallow aprons; the top surface is near 12/16.
		Block.box(2, 0, 2, 4, 11.25, 4),
		Block.box(12, 0, 2, 14, 11.25, 4),
		Block.box(2, 0, 12, 4, 11.25, 14),
		Block.box(12, 0, 12, 14, 11.25, 14),
		Block.box(2, 9.25, 2, 14, 11.25, 4),
		Block.box(2, 9.25, 12, 14, 11.25, 14),
		Block.box(1, 11.25, 1, 15, 12.5, 15)
	);

	public SmallWorkTableBlock(BlockBehaviour.Properties properties) {
		super(properties, NORTH_SHAPE);
	}

	@Override
	public MapCodec<SmallWorkTableBlock> codec() {
		return CODEC;
	}
}
