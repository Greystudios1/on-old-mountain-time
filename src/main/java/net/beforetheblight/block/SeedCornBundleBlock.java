package net.beforetheblight.block;

import java.util.Map;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A tied seed-corn bundle that attaches to a wall or hangs beneath a beam. */
public final class SeedCornBundleBlock extends HorizontalDirectionalBlock {
	public static final MapCodec<SeedCornBundleBlock> CODEC =
		simpleCodec(SeedCornBundleBlock::new);
	public static final EnumProperty<Attachment> ATTACHMENT =
		EnumProperty.create("attachment", Attachment.class);

	private static final VoxelShape WALL_NORTH_SHAPE = Shapes.or(
		Block.box(6.0, 13.0, 14.0, 10.0, 16.0, 16.0),
		Block.box(3.0, 2.0, 11.0, 13.0, 14.0, 16.0)
	);
	private static final Map<Direction, VoxelShape> WALL_SHAPES =
		Shapes.rotateHorizontal(WALL_NORTH_SHAPE);
	private static final VoxelShape CEILING_SHAPE = Shapes.or(
		Block.box(7.0, 13.0, 7.0, 9.0, 16.0, 9.0),
		Block.box(3.0, 1.0, 3.0, 13.0, 14.0, 13.0)
	);

	public SeedCornBundleBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(
			stateDefinition.any()
				.setValue(FACING, Direction.NORTH)
				.setValue(ATTACHMENT, Attachment.WALL)
		);
	}

	@Override
	public MapCodec<SeedCornBundleBlock> codec() {
		return CODEC;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction clickedFace = context.getClickedFace();
		if (clickedFace == Direction.UP) {
			return null;
		}

		BlockState state = defaultBlockState();
		if (clickedFace == Direction.DOWN) {
			state = state
				.setValue(ATTACHMENT, Attachment.CEILING)
				.setValue(FACING, context.getHorizontalDirection());
		} else {
			state = state
				.setValue(ATTACHMENT, Attachment.WALL)
				.setValue(FACING, clickedFace);
		}
		return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		if (state.getValue(ATTACHMENT) == Attachment.CEILING) {
			return level.getBlockState(pos.above()).isFaceSturdy(
				level,
				pos.above(),
				Direction.DOWN
			);
		}

		Direction facing = state.getValue(FACING);
		BlockPos supportPos = pos.relative(facing.getOpposite());
		return level.getBlockState(supportPos).isFaceSturdy(level, supportPos, facing);
	}

	@Override
	protected BlockState updateShape(
		BlockState state,
		LevelReader level,
		ScheduledTickAccess ticks,
		BlockPos pos,
		Direction direction,
		BlockPos neighborPos,
		BlockState neighborState,
		RandomSource random
	) {
		Direction supportDirection = state.getValue(ATTACHMENT) == Attachment.CEILING
			? Direction.UP
			: state.getValue(FACING).getOpposite();
		if (direction == supportDirection && !state.canSurvive(level, pos)) {
			return Blocks.AIR.defaultBlockState();
		}
		return super.updateShape(
			state,
			level,
			ticks,
			pos,
			direction,
			neighborPos,
			neighborState,
			random
		);
	}

	@Override
	protected VoxelShape getShape(
		BlockState state,
		BlockGetter level,
		BlockPos pos,
		CollisionContext context
	) {
		return state.getValue(ATTACHMENT) == Attachment.CEILING
			? CEILING_SHAPE
			: WALL_SHAPES.get(state.getValue(FACING));
	}

	@Override
	protected VoxelShape getCollisionShape(
		BlockState state,
		BlockGetter level,
		BlockPos pos,
		CollisionContext context
	) {
		return Shapes.empty();
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, ATTACHMENT);
	}

	public enum Attachment implements StringRepresentable {
		WALL("wall"),
		CEILING("ceiling");

		private final String serializedName;

		Attachment(String serializedName) {
			this.serializedName = serializedName;
		}

		@Override
		public String getSerializedName() {
			return serializedName;
		}
	}
}
