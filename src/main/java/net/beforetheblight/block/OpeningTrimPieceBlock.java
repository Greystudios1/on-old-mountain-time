package net.beforetheblight.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import java.util.Map;
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
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A low, horizontally oriented opening detail: sill, threshold, or leveling
 * wedge.
 */
public final class OpeningTrimPieceBlock extends HorizontalDirectionalBlock
	implements SimpleWaterloggedBlock {
	public static final MapCodec<OpeningTrimPieceBlock> CODEC =
		RecordCodecBuilder.mapCodec(instance -> instance.group(
			Profile.CODEC.fieldOf("profile").forGetter(OpeningTrimPieceBlock::profile),
			propertiesCodec()
		).apply(instance, OpeningTrimPieceBlock::new));
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

	private final Profile profile;
	private final Map<Direction, VoxelShape> shapes;

	public OpeningTrimPieceBlock(Profile profile, BlockBehaviour.Properties properties) {
		super(properties);
		this.profile = profile;
		this.shapes = Shapes.rotateHorizontal(profile.northShape());
		this.registerDefaultState(
			this.stateDefinition
				.any()
				.setValue(FACING, Direction.NORTH)
				.setValue(WATERLOGGED, false)
		);
	}

	@Override
	public MapCodec<OpeningTrimPieceBlock> codec() {
		return CODEC;
	}

	public Profile profile() {
		return this.profile;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState()
			.setValue(FACING, context.getHorizontalDirection().getOpposite())
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
		return this.shapes.get(state.getValue(FACING));
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED)
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
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, WATERLOGGED);
	}

	public enum Profile implements StringRepresentable {
		SILL(Block.box(1.0, 0.0, 4.0, 15.0, 3.0, 12.0)),
		THRESHOLD(Block.box(0.0, 0.0, 3.0, 16.0, 2.0, 13.0)),
		WEDGE(
			Shapes.or(
				Block.box(3.0, 0.0, 1.0, 13.0, 1.0, 15.0),
				Block.box(3.0, 1.0, 1.0, 13.0, 2.0, 11.0),
				Block.box(3.0, 2.0, 1.0, 13.0, 3.0, 7.0)
			)
		);

		public static final Codec<Profile> CODEC =
			StringRepresentable.fromEnum(Profile::values);

		private final VoxelShape northShape;

		Profile(VoxelShape northShape) {
			this.northShape = northShape;
		}

		VoxelShape northShape() {
			return this.northShape;
		}

		@Override
		public String getSerializedName() {
			return this.name().toLowerCase(Locale.ROOT);
		}
	}
}
