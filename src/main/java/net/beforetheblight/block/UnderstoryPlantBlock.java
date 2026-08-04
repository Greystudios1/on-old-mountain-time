package net.beforetheblight.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A small, non-crop forest understory plant.
 *
 * <p>Mountain laurel and lowbush blueberry deliberately share this simple
 * survival contract. Their authored models provide the visual height and
 * density while this bounded outline remains easy to select without becoming
 * a collision obstacle.</p>
 */
public final class UnderstoryPlantBlock extends VegetationBlock {
	public static final MapCodec<UnderstoryPlantBlock> CODEC =
		simpleCodec(UnderstoryPlantBlock::new);
	private static final VoxelShape SHAPE = box(2.0, 0.0, 2.0, 14.0, 12.0, 14.0);

	public UnderstoryPlantBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	public MapCodec<UnderstoryPlantBlock> codec() {
		return CODEC;
	}

	@Override
	protected VoxelShape getShape(
		BlockState state,
		BlockGetter level,
		BlockPos pos,
		CollisionContext context
	) {
		return SHAPE;
	}
}
