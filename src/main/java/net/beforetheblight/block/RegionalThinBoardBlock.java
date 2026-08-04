package net.beforetheblight.block;

import java.util.Map;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A two-pixel-thick construction or furniture board.
 *
 * <p>The board sits against the support opposite its outward-facing direction.
 * This supplies honest floor, ceiling, wall, shelf, panel, and tabletop
 * geometry from one restrained six-direction blockstate.</p>
 */
public final class RegionalThinBoardBlock extends Block {
	public static final MapCodec<RegionalThinBoardBlock> CODEC = simpleCodec(RegionalThinBoardBlock::new);
	public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

	private static final Map<Direction, VoxelShape> SHAPES = Map.of(
		Direction.UP, Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0),
		Direction.DOWN, Block.box(0.0, 14.0, 0.0, 16.0, 16.0, 16.0),
		Direction.NORTH, Block.box(0.0, 0.0, 14.0, 16.0, 16.0, 16.0),
		Direction.SOUTH, Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 2.0),
		Direction.WEST, Block.box(14.0, 0.0, 0.0, 16.0, 16.0, 16.0),
		Direction.EAST, Block.box(0.0, 0.0, 0.0, 2.0, 16.0, 16.0)
	);

	public RegionalThinBoardBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(
			this.stateDefinition.any().setValue(FACING, Direction.UP)
		);
	}

	@Override
	protected MapCodec<RegionalThinBoardBlock> codec() {
		return CODEC;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(FACING, context.getClickedFace());
	}

	@Override
	protected VoxelShape getShape(
		BlockState state,
		BlockGetter level,
		BlockPos pos,
		CollisionContext context
	) {
		return SHAPES.get(state.getValue(FACING));
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}
}
