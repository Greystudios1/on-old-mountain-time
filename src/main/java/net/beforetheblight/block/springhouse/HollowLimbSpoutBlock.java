package net.beforetheblight.block.springhouse;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A short hollow branch that directs spring water into a trough.
 *
 * <p>The block is a real vanilla water container: water buckets set its
 * waterlogged state, it exposes a source {@link FluidState}, and another bucket
 * can recover that source. The {@code flowing=true} value remains solely as a
 * save-compatible alias for old showcase worlds and migrates to waterlogging;
 * it no longer selects a painted, uncollectable water cuboid.</p>
 */
public final class HollowLimbSpoutBlock extends SpringhouseFacingBlock
	implements SimpleWaterloggedBlock {
	public static final MapCodec<HollowLimbSpoutBlock> CODEC =
		simpleCodec(HollowLimbSpoutBlock::new);
	public static final BooleanProperty FLOWING =
		BooleanProperty.create("flowing");
	public static final BooleanProperty WATERLOGGED =
		BlockStateProperties.WATERLOGGED;

	private static final VoxelShape NORTH_SHAPE = Shapes.or(
		// Main branch, projecting from the south/back toward north.
		Block.box(4.0, 10.0, 1.0, 12.0, 14.0, 16.0),
		// Irregular hollow underside and open tip.
		Block.box(6.0, 8.0, 0.0, 10.0, 11.0, 15.0)
	);

	public HollowLimbSpoutBlock(BlockBehaviour.Properties properties) {
		super(properties, NORTH_SHAPE);
		this.registerDefaultState(
			this.defaultBlockState()
				.setValue(FLOWING, false)
				.setValue(WATERLOGGED, false)
		);
	}

	@Override
	public MapCodec<HollowLimbSpoutBlock> codec() {
		return CODEC;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return super.getStateForPlacement(context).setValue(
			WATERLOGGED,
			context.getLevel()
				.getFluidState(context.getClickedPos())
				.is(Fluids.WATER)
		);
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
				state.setValue(WATERLOGGED, true).setValue(FLOWING, false),
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
			.setValue(FLOWING, false);
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
		if (state.getValue(FLOWING)) {
			migrated = state
				.setValue(FLOWING, false)
				.setValue(WATERLOGGED, true);
		}
		if (hasWater(migrated)) {
			ticks.scheduleTick(
				pos,
				Fluids.WATER,
				Fluids.WATER.getTickDelay(level)
			);
		}
		return super.updateShape(
			migrated,
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
		builder.add(FLOWING, WATERLOGGED);
	}

	private static boolean hasWater(BlockState state) {
		return state.getValue(WATERLOGGED) || state.getValue(FLOWING);
	}
}
