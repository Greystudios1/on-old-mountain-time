package net.beforetheblight.block;

import java.util.Map;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A wall-plane diagonal brace for a hewn chestnut post-and-beam frame.
 *
 * <p>Aiming at the left or right half of the placement face selects the two
 * mirrored slopes. The stepped collision follows the visible diagonal closely
 * enough for building while avoiding an invisible full-block barrier.</p>
 */
public final class HewnChestnutKneeBraceBlock
	extends HorizontalDirectionalBlock
	implements SimpleWaterloggedBlock {
	public static final MapCodec<HewnChestnutKneeBraceBlock> CODEC =
		simpleCodec(HewnChestnutKneeBraceBlock::new);
	public static final BooleanProperty MIRRORED =
		BooleanProperty.create("mirrored");
	public static final BooleanProperty WATERLOGGED =
		BlockStateProperties.WATERLOGGED;

	private static final VoxelShape NORMAL_NORTH_SHAPE = Shapes.or(
		Block.box(1.0, 0.0, 0.0, 6.0, 5.0, 4.0),
		Block.box(4.0, 3.0, 0.0, 9.0, 9.0, 4.0),
		Block.box(7.0, 7.0, 0.0, 12.0, 13.0, 4.0),
		Block.box(10.0, 11.0, 0.0, 15.0, 16.0, 4.0)
	);
	private static final VoxelShape MIRRORED_NORTH_SHAPE = Shapes.or(
		Block.box(10.0, 0.0, 0.0, 15.0, 5.0, 4.0),
		Block.box(7.0, 3.0, 0.0, 12.0, 9.0, 4.0),
		Block.box(4.0, 7.0, 0.0, 9.0, 13.0, 4.0),
		Block.box(1.0, 11.0, 0.0, 6.0, 16.0, 4.0)
	);
	private static final Map<Direction, VoxelShape> NORMAL_SHAPES =
		Shapes.rotateHorizontal(NORMAL_NORTH_SHAPE);
	private static final Map<Direction, VoxelShape> MIRRORED_SHAPES =
		Shapes.rotateHorizontal(MIRRORED_NORTH_SHAPE);

	public HewnChestnutKneeBraceBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(
			this.stateDefinition
				.any()
				.setValue(FACING, Direction.NORTH)
				.setValue(MIRRORED, false)
				.setValue(WATERLOGGED, false)
		);
	}

	@Override
	public MapCodec<HewnChestnutKneeBraceBlock> codec() {
		return CODEC;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction facing = context.getHorizontalDirection().getOpposite();
		Direction localRight = facing.getClockWise();
		BlockPos pos = context.getClickedPos();
		Vec3 click = context.getClickLocation();
		double localX = click.x - pos.getX() - 0.5;
		double localZ = click.z - pos.getZ() - 0.5;
		double sideOffset =
			localX * localRight.getStepX() + localZ * localRight.getStepZ();

		return this.defaultBlockState()
			.setValue(FACING, facing)
			.setValue(MIRRORED, sideOffset < 0.0)
			.setValue(
				WATERLOGGED,
				context.getLevel().getFluidState(pos).is(Fluids.WATER)
			);
	}

	@Override
	protected VoxelShape getShape(
		BlockState state,
		BlockGetter level,
		BlockPos pos,
		CollisionContext context
	) {
		Map<Direction, VoxelShape> shapes = state.getValue(MIRRORED)
			? MIRRORED_SHAPES
			: NORMAL_SHAPES;
		return shapes.get(state.getValue(FACING));
	}

	@Override
	protected BlockState updateShape(
		BlockState state,
		LevelReader level,
		ScheduledTickAccess ticks,
		BlockPos pos,
		Direction directionToNeighbour,
		BlockPos neighbourPos,
		BlockState neighbourState,
		RandomSource random
	) {
		if (state.getValue(WATERLOGGED)) {
			ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}
		return super.updateShape(
			state,
			level,
			ticks,
			pos,
			directionToNeighbour,
			neighbourPos,
			neighbourState,
			random
		);
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		BlockState mirrored = super.mirror(state, mirror);
		return mirror == Mirror.NONE
			? mirrored
			: mirrored.setValue(MIRRORED, !state.getValue(MIRRORED));
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED)
			? Fluids.WATER.getSource(false)
			: super.getFluidState(state);
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}

	@Override
	protected void createBlockStateDefinition(
		StateDefinition.Builder<Block, BlockState> builder
	) {
		builder.add(FACING, MIRRORED, WATERLOGGED);
	}
}
