package net.beforetheblight.block;

import java.util.Map;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A centered, eight-by-eight pole that can follow any block axis.
 *
 * <p>This is intentionally not a full cube: its outline and collision match
 * the visible pole/post so regional timber details do not behave like
 * invisible sixteen-by-sixteen blocks.</p>
 */
public final class RegionalPoleBlock extends RotatedPillarBlock {
	public static final MapCodec<RegionalPoleBlock> CODEC = simpleCodec(RegionalPoleBlock::new);

	private static final Map<Direction.Axis, VoxelShape> SHAPES = Map.of(
		Direction.Axis.X, Block.box(0.0, 4.0, 4.0, 16.0, 12.0, 12.0),
		Direction.Axis.Y, Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0),
		Direction.Axis.Z, Block.box(4.0, 4.0, 0.0, 12.0, 12.0, 16.0)
	);

	public RegionalPoleBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	public MapCodec<RegionalPoleBlock> codec() {
		return CODEC;
	}

	@Override
	protected VoxelShape getShape(
		BlockState state,
		BlockGetter level,
		BlockPos pos,
		CollisionContext context
	) {
		return SHAPES.get(state.getValue(AXIS));
	}
}
