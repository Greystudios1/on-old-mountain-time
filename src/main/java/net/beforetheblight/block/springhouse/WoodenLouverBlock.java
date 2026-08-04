package net.beforetheblight.block.springhouse;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A fixed wooden ventilation louver for cool, shaded springhouse walls.
 */
public final class WoodenLouverBlock extends SpringhouseFacingBlock {
	public static final MapCodec<WoodenLouverBlock> CODEC =
		simpleCodec(WoodenLouverBlock::new);

	private static final VoxelShape NORTH_SHAPE = Shapes.or(
		Block.box(1.0, 2.0, 14.0, 3.0, 14.0, 16.0),
		Block.box(13.0, 2.0, 14.0, 15.0, 14.0, 16.0),
		Block.box(2.75, 1.75, 13.75, 13.25, 4.25, 15.75),
		Block.box(2.75, 11.75, 13.75, 13.25, 14.25, 15.75),
		Block.box(2.5, 5.0, 13.0, 13.5, 6.0, 15.5),
		Block.box(2.5, 7.0, 13.0, 13.5, 8.0, 15.5),
		Block.box(2.5, 9.0, 13.0, 13.5, 10.0, 15.5),
		Block.box(2.5, 11.0, 13.0, 13.5, 12.25, 15.5)
	);

	public WoodenLouverBlock(BlockBehaviour.Properties properties) {
		super(properties, NORTH_SHAPE);
	}

	@Override
	public MapCodec<WoodenLouverBlock> codec() {
		return CODEC;
	}
}
