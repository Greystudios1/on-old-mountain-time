package net.beforetheblight.block.springhouse;

import java.util.Map;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * One segment of a hollowed chestnut-log spring trough.
 *
 * <p>The facing direction is the direction of water travel. For the authored
 * north-facing shapes, an inlet has its closed timber end to the south and an
 * outlet has its closed timber end to the north. Water is real vanilla water:
 * the block is waterloggable, exposes a source {@link FluidState}, and supports
 * water-bucket placement and pickup. The {@code content=clear} value remains
 * only as a save-compatible alias for old worlds and migrates to waterlogging;
 * {@code content=silty} represents visible sediment, not a second fake fluid.
 * New placements turn the water-flow axis a quarter turn clockwise from the
 * placer's look direction, so the long trough runs left-to-right in front of
 * the player. Stored {@code facing} values retain their original meaning.</p>
 */
public final class HollowedChestnutTroughBlock extends SpringhouseFacingBlock
	implements SimpleWaterloggedBlock {
	public static final MapCodec<HollowedChestnutTroughBlock> CODEC =
		simpleCodec(HollowedChestnutTroughBlock::new);
	public static final EnumProperty<TroughPart> PART =
		EnumProperty.create("part", TroughPart.class);
	public static final EnumProperty<TroughContent> CONTENT =
		EnumProperty.create("content", TroughContent.class);
	public static final BooleanProperty WATERLOGGED =
		BlockStateProperties.WATERLOGGED;

	private static final VoxelShape NORTH_MIDDLE_SHAPE = Shapes.or(
		// Two-pixel timber floor between the sides of the hollow.
		Block.box(3.0, 2.75, 0.0, 13.0, 5.0, 16.0),
		// The two intact sides of the hollowed log.
		Block.box(0.0, 3.0, 0.0, 3.0, 10.0, 16.0),
		Block.box(13.0, 3.0, 0.0, 16.0, 10.0, 16.0)
	);
	private static final VoxelShape NORTH_INLET_SHAPE = Shapes.or(
		NORTH_MIDDLE_SHAPE,
		// Rear/south end wall.
		Block.box(3.0, 5.0, 14.0, 13.0, 10.0, 16.0)
	);
	private static final VoxelShape NORTH_OUTLET_SHAPE = Shapes.or(
		NORTH_MIDDLE_SHAPE,
		// Front/north end wall.
		Block.box(3.0, 5.0, 0.0, 13.0, 10.0, 2.0)
	);
	private static final VoxelShape NORTH_STANDALONE_SHAPE = Shapes.or(
		NORTH_INLET_SHAPE,
		NORTH_OUTLET_SHAPE
	);
	private static final Map<TroughPart, Map<Direction, VoxelShape>> SHAPES =
		Map.of(
			TroughPart.STANDALONE,
			Shapes.rotateHorizontal(NORTH_STANDALONE_SHAPE),
			TroughPart.INLET, Shapes.rotateHorizontal(NORTH_INLET_SHAPE),
			TroughPart.MIDDLE, Shapes.rotateHorizontal(NORTH_MIDDLE_SHAPE),
			TroughPart.OUTLET, Shapes.rotateHorizontal(NORTH_OUTLET_SHAPE)
		);

	public HollowedChestnutTroughBlock(BlockBehaviour.Properties properties) {
		super(properties, NORTH_MIDDLE_SHAPE);
		this.registerDefaultState(
			this.defaultBlockState()
				.setValue(PART, TroughPart.STANDALONE)
				.setValue(CONTENT, TroughContent.EMPTY)
				.setValue(WATERLOGGED, false)
		);
	}

	@Override
	public MapCodec<HollowedChestnutTroughBlock> codec() {
		return CODEC;
	}

	@Override
	protected VoxelShape getShape(
		BlockState state,
		BlockGetter level,
		BlockPos pos,
		CollisionContext context
	) {
		return SHAPES
			.get(state.getValue(PART))
			.get(state.getValue(FACING));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState placed = super
			.getStateForPlacement(context)
			.setValue(
				FACING,
				placementFacingFor(context.getHorizontalDirection())
			)
			.setValue(
				WATERLOGGED,
				context.getLevel()
					.getFluidState(context.getClickedPos())
					.is(Fluids.WATER)
			);
		return placed.setValue(
			PART,
			resolvePart(context.getLevel(), context.getClickedPos(), placed)
		);
	}

	/**
	 * Maps the placer's look direction to the trough's long/water-flow axis.
	 *
	 * <p>The inherited furniture convention faces a block back toward its
	 * placer. A trough is linear rather than front-facing, so its useful
	 * placement axis is perpendicular to that look direction.</p>
	 */
	public static Direction placementFacingFor(Direction placerFacing) {
		return placerFacing.getClockWise();
	}

	@Override
	public boolean placeLiquid(
		LevelAccessor level,
		BlockPos pos,
		BlockState state,
		FluidState fluidState
	) {
		if (hasWater(state) || !fluidState.is(Fluids.WATER)) {
			return false;
		}
		if (!level.isClientSide()) {
			level.setBlock(
				pos,
				state
					.setValue(WATERLOGGED, true)
					.setValue(
						CONTENT,
						state.getValue(CONTENT) == TroughContent.CLEAR
							? TroughContent.EMPTY
							: state.getValue(CONTENT)
					),
				Block.UPDATE_ALL
			);
			level.scheduleTick(
				pos,
				Fluids.WATER,
				Fluids.WATER.getTickDelay(level)
			);
		}
		return true;
	}

	@Override
	public ItemStack pickupBlock(
		LivingEntity user,
		LevelAccessor level,
		BlockPos pos,
		BlockState state
	) {
		if (!hasWater(state)) {
			return ItemStack.EMPTY;
		}
		BlockState drained = state
			.setValue(WATERLOGGED, false)
			.setValue(
				CONTENT,
				state.getValue(CONTENT) == TroughContent.CLEAR
					? TroughContent.EMPTY
					: state.getValue(CONTENT)
			);
		level.setBlock(pos, drained, Block.UPDATE_ALL);
		if (!drained.canSurvive(level, pos)) {
			level.destroyBlock(pos, true);
		}
		return new ItemStack(Items.WATER_BUCKET);
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return hasWater(state)
			? Fluids.WATER.getSource(false)
			: super.getFluidState(state);
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
		BlockState migrated = state;
		if (state.getValue(CONTENT) == TroughContent.CLEAR) {
			migrated = state
				.setValue(CONTENT, TroughContent.EMPTY)
				.setValue(WATERLOGGED, true);
		}
		BlockState connected = migrated.setValue(
			PART,
			resolvePart(level, pos, migrated)
		);
		if (hasWater(connected)) {
			ticks.scheduleTick(
				pos,
				Fluids.WATER,
				Fluids.WATER.getTickDelay(level)
			);
		}
		return super.updateShape(
			connected,
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
	protected void createBlockStateDefinition(
		StateDefinition.Builder<Block, BlockState> builder
	) {
		super.createBlockStateDefinition(builder);
		builder.add(PART, CONTENT, WATERLOGGED);
	}

	private static boolean hasWater(BlockState state) {
		return state.getValue(WATERLOGGED)
			|| state.getValue(CONTENT) == TroughContent.CLEAR;
	}

	private TroughPart resolvePart(
		BlockGetter level,
		BlockPos pos,
		BlockState state
	) {
		Direction facing = state.getValue(FACING);
		boolean forward = connectsTo(
			level.getBlockState(pos.relative(facing)),
			facing.getAxis()
		);
		boolean backward = connectsTo(
			level.getBlockState(pos.relative(facing.getOpposite())),
			facing.getAxis()
		);
		if (forward && backward) {
			return TroughPart.MIDDLE;
		}
		if (forward) {
			return TroughPart.INLET;
		}
		if (backward) {
			return TroughPart.OUTLET;
		}
		return TroughPart.STANDALONE;
	}

	private boolean connectsTo(BlockState neighbour, Direction.Axis axis) {
		return neighbour.is(this)
			&& neighbour.getValue(FACING).getAxis() == axis;
	}

	public enum TroughPart implements StringRepresentable {
		STANDALONE("standalone"),
		INLET("inlet"),
		MIDDLE("middle"),
		OUTLET("outlet");

		private final String serializedName;

		TroughPart(String serializedName) {
			this.serializedName = serializedName;
		}

		@Override
		public String getSerializedName() {
			return this.serializedName;
		}
	}

	public enum TroughContent implements StringRepresentable {
		EMPTY("empty"),
		CLEAR("clear"),
		SILTY("silty");

		private final String serializedName;

		TroughContent(String serializedName) {
			this.serializedName = serializedName;
		}

		@Override
		public String getSerializedName() {
			return this.serializedName;
		}
	}
}
