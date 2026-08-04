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

/** Thin, wall-mounted chinking with structure-friendly condition states. */
public final class ChestnutChinkingStripBlock
	extends HorizontalDirectionalBlock
	implements SimpleWaterloggedBlock {
	public static final MapCodec<ChestnutChinkingStripBlock> CODEC =
		simpleCodec(ChestnutChinkingStripBlock::new);
	public static final EnumProperty<Condition> CONDITION =
		EnumProperty.create("condition", Condition.class);
	public static final BooleanProperty WATERLOGGED =
		BlockStateProperties.WATERLOGGED;

	private static final Map<Direction, VoxelShape> FULL_SHAPES =
		Shapes.rotateHorizontal(Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 2.0));
	private static final Map<Direction, VoxelShape> MISSING_SHAPES =
		Shapes.rotateHorizontal(
			Shapes.or(
				Block.box(0.0, 1.0, 0.0, 5.0, 4.0, 1.0),
				Block.box(11.0, 10.0, 0.0, 16.0, 14.0, 1.0)
			)
		);

	public ChestnutChinkingStripBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(
			this.stateDefinition
				.any()
				.setValue(FACING, Direction.NORTH)
				.setValue(CONDITION, Condition.FRESH)
				.setValue(WATERLOGGED, false)
		);
	}

	@Override
	protected MapCodec<ChestnutChinkingStripBlock> codec() {
		return CODEC;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction clicked = context.getClickedFace();
		Direction facing = clicked.getAxis().isHorizontal()
			? clicked
			: context.getHorizontalDirection().getOpposite();
		return this.defaultBlockState()
			.setValue(FACING, facing)
			.setValue(
				WATERLOGGED,
				context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER)
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
			state.getValue(CONDITION) == Condition.MISSING
				? MISSING_SHAPES
				: FULL_SHAPES;
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
		BlockState changed = current.cycle(CONDITION);
		if (!level.setBlock(pos, changed, Block.UPDATE_ALL)) {
			return InteractionResult.FAIL;
		}
		level.playSound(
			null,
			pos,
			SoundEvents.MUD_PLACE,
			SoundSource.BLOCKS,
			0.65F,
			0.9F
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
		builder.add(FACING, CONDITION, WATERLOGGED);
	}

	public enum Condition implements StringRepresentable {
		CORE("core"),
		FRESH("fresh"),
		AGED("aged"),
		CRACKED("cracked"),
		MISSING("missing"),
		END("end"),
		CORNER("corner");

		private final String serializedName;

		Condition(String serializedName) {
			this.serializedName = serializedName;
		}

		@Override
		public String getSerializedName() {
			return this.serializedName;
		}
	}
}
