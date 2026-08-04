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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * One repair-corner item with square, V-notch, and half-dovetail states.
 * Course alternation is native so structure templates do not need separate
 * block IDs for upper and lower joints.
 */
public final class ChestnutRepairCornerBlock
	extends HorizontalDirectionalBlock
	implements SimpleWaterloggedBlock {
	public static final MapCodec<ChestnutRepairCornerBlock> CODEC =
		simpleCodec(ChestnutRepairCornerBlock::new);
	public static final EnumProperty<Style> STYLE =
		EnumProperty.create("style", Style.class);
	public static final EnumProperty<Course> COURSE =
		EnumProperty.create("course", Course.class);
	public static final BooleanProperty WATERLOGGED =
		BlockStateProperties.WATERLOGGED;

	private static final VoxelShape LOWER_NORTH_SHAPE = Shapes.or(
		Block.box(2.0, 0.0, 0.0, 14.0, 7.75, 16.0),
		Block.box(0.0, 8.25, 2.0, 16.0, 16.0, 14.0)
	);
	private static final VoxelShape UPPER_NORTH_SHAPE = Shapes.or(
		Block.box(0.0, 0.0, 2.0, 16.0, 7.75, 14.0),
		Block.box(2.0, 8.25, 0.0, 14.0, 16.0, 16.0)
	);
	private static final Map<Direction, VoxelShape> LOWER_SHAPES =
		Shapes.rotateHorizontal(LOWER_NORTH_SHAPE);
	private static final Map<Direction, VoxelShape> UPPER_SHAPES =
		Shapes.rotateHorizontal(UPPER_NORTH_SHAPE);

	public ChestnutRepairCornerBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(
			this.stateDefinition
				.any()
				.setValue(FACING, Direction.NORTH)
				.setValue(STYLE, Style.SQUARE)
				.setValue(COURSE, Course.LOWER)
				.setValue(WATERLOGGED, false)
		);
	}

	@Override
	protected MapCodec<ChestnutRepairCornerBlock> codec() {
		return CODEC;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockPos pos = context.getClickedPos();
		BlockState below = context.getLevel().getBlockState(pos.below());
		Course course = below.is(this)
			? below.getValue(COURSE).opposite()
			: ((pos.getY() & 1) == 0 ? Course.LOWER : Course.UPPER);
		return this.defaultBlockState()
			.setValue(FACING, context.getHorizontalDirection().getOpposite())
			.setValue(COURSE, course)
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
		Map<Direction, VoxelShape> shapes =
			state.getValue(COURSE) == Course.LOWER
				? LOWER_SHAPES
				: UPPER_SHAPES;
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
	protected FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED)
			? Fluids.WATER.getSource(false)
			: super.getFluidState(state);
	}

	@Override
	protected InteractionResult useWithoutItem(
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		BlockHitResult hit
	) {
		if (!player.isSecondaryUseActive()) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		BlockState current = level.getBlockState(pos);
		if (!current.is(this)) {
			return InteractionResult.PASS;
		}
		BlockState changed = current.cycle(STYLE);
		if (!level.setBlock(pos, changed, Block.UPDATE_ALL)) {
			return InteractionResult.FAIL;
		}
		level.playSound(
			null,
			pos,
			SoundEvents.WOOD_PLACE,
			SoundSource.BLOCKS,
			0.65F,
			0.85F
		);
		level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, changed));
		return InteractionResult.SUCCESS_SERVER;
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}

	@Override
	protected void createBlockStateDefinition(
		StateDefinition.Builder<Block, BlockState> builder
	) {
		builder.add(FACING, STYLE, COURSE, WATERLOGGED);
	}

	public enum Style implements StringRepresentable {
		SQUARE("square"),
		V_NOTCH("v_notch"),
		HALF_DOVETAIL("half_dovetail");

		private final String serializedName;

		Style(String serializedName) {
			this.serializedName = serializedName;
		}

		@Override
		public String getSerializedName() {
			return this.serializedName;
		}
	}

	public enum Course implements StringRepresentable {
		LOWER("lower"),
		UPPER("upper");

		private final String serializedName;

		Course(String serializedName) {
			this.serializedName = serializedName;
		}

		public Course opposite() {
			return this == LOWER ? UPPER : LOWER;
		}

		@Override
		public String getSerializedName() {
			return this.serializedName;
		}
	}
}
