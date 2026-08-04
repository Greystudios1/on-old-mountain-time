package net.beforetheblight.block;

import java.util.EnumMap;
import java.util.Map;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A placeable half log with a real half-block outline and collision.
 *
 * <p>{@link #FLAT_FACE} names the direction from which the split face is
 * visible. Placement chooses a face perpendicular to the log axis; retaining
 * both properties also lets structure templates rotate the piece correctly.</p>
 */
public final class RegionalHalfLogBlock extends RotatedPillarBlock {
	public static final MapCodec<RegionalHalfLogBlock> CODEC = simpleCodec(RegionalHalfLogBlock::new);
	public static final EnumProperty<Direction> FLAT_FACE =
		EnumProperty.create("flat_face", Direction.class);

	private static final Map<Direction, VoxelShape> SHAPES = createShapes();

	public RegionalHalfLogBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(
			this.stateDefinition
				.any()
				.setValue(AXIS, Direction.Axis.Y)
				.setValue(FLAT_FACE, Direction.NORTH)
		);
	}

	@Override
	public MapCodec<RegionalHalfLogBlock> codec() {
		return CODEC;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		Direction.Axis axis = state.getValue(AXIS);
		for (Direction direction : context.getNearestLookingDirections()) {
			if (direction.getAxis() != axis) {
				return state.setValue(FLAT_FACE, direction);
			}
		}
		return state.setValue(FLAT_FACE, perpendicularFallback(axis));
	}

	@Override
	protected VoxelShape getShape(
		BlockState state,
		BlockGetter level,
		BlockPos pos,
		CollisionContext context
	) {
		Direction face = state.getValue(FLAT_FACE);
		if (face.getAxis() == state.getValue(AXIS)) {
			face = perpendicularFallback(state.getValue(AXIS));
		}
		return SHAPES.get(face);
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return super.rotate(state, rotation)
			.setValue(FLAT_FACE, rotation.rotate(state.getValue(FLAT_FACE)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return super.mirror(state, mirror)
			.setValue(FLAT_FACE, mirror.mirror(state.getValue(FLAT_FACE)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AXIS, FLAT_FACE);
	}

	private static Map<Direction, VoxelShape> createShapes() {
		Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
		/*
		 * The occupied half is opposite the named exposed flat face. For
		 * example, flat_face=up exposes the plane at y=8 and retains the lower
		 * half of the log.
		 */
		shapes.put(Direction.UP, Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0));
		shapes.put(Direction.DOWN, Block.box(0.0, 8.0, 0.0, 16.0, 16.0, 16.0));
		shapes.put(Direction.SOUTH, Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 8.0));
		shapes.put(Direction.NORTH, Block.box(0.0, 0.0, 8.0, 16.0, 16.0, 16.0));
		shapes.put(Direction.EAST, Block.box(0.0, 0.0, 0.0, 8.0, 16.0, 16.0));
		shapes.put(Direction.WEST, Block.box(8.0, 0.0, 0.0, 16.0, 16.0, 16.0));
		return Map.copyOf(shapes);
	}

	private static Direction perpendicularFallback(Direction.Axis axis) {
		return axis == Direction.Axis.Y ? Direction.NORTH : Direction.UP;
	}
}
