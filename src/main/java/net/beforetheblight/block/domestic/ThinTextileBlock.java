package net.beforetheblight.block.domestic;

import java.util.Locale;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Thin folded, floor-laid, or wall-hung domestic textiles.
 */
public final class ThinTextileBlock extends HorizontalDirectionalBlock {
	public static final MapCodec<ThinTextileBlock> CODEC =
		RecordCodecBuilder.mapCodec(instance -> instance.group(
			Profile.CODEC.fieldOf("profile").forGetter(ThinTextileBlock::profile),
			propertiesCodec()
		).apply(instance, ThinTextileBlock::new));

	private final Profile profile;
	private final Map<Direction, VoxelShape> shapes;

	public ThinTextileBlock(Profile profile, BlockBehaviour.Properties properties) {
		super(properties);
		this.profile = profile;
		this.shapes = Shapes.rotateHorizontal(profile.northShape());
		this.registerDefaultState(
			this.stateDefinition.any().setValue(FACING, Direction.NORTH)
		);
	}

	@Override
	public MapCodec<ThinTextileBlock> codec() {
		return CODEC;
	}

	public Profile profile() {
		return this.profile;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction facing;
		if (this.profile == Profile.WALL) {
			Direction clickedFace = context.getClickedFace();
			if (!clickedFace.getAxis().isHorizontal()) {
				return null;
			}
			facing = clickedFace;
		} else {
			facing = context.getHorizontalDirection().getOpposite();
		}

		BlockState state = this.defaultBlockState().setValue(FACING, facing);
		return state.canSurvive(context.getLevel(), context.getClickedPos())
			? state
			: null;
	}

	@Override
	protected boolean canSurvive(
		BlockState state,
		LevelReader level,
		BlockPos pos
	) {
		if (this.profile == Profile.WALL) {
			Direction facing = state.getValue(FACING);
			return Block.canSupportCenter(
				level,
				pos.relative(facing.getOpposite()),
				facing
			);
		}
		return Block.canSupportCenter(level, pos.below(), Direction.UP);
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
		Direction supportDirection = this.profile == Profile.WALL
			? state.getValue(FACING).getOpposite()
			: Direction.DOWN;
		if (directionToNeighbour == supportDirection
			&& !state.canSurvive(level, pos)) {
			return Blocks.AIR.defaultBlockState();
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
	protected VoxelShape getShape(
		BlockState state,
		BlockGetter level,
		BlockPos pos,
		CollisionContext context
	) {
		return this.shapes.get(state.getValue(FACING));
	}

	@Override
	protected void createBlockStateDefinition(
		StateDefinition.Builder<Block, BlockState> builder
	) {
		builder.add(FACING);
	}

	public enum Profile implements StringRepresentable {
		FOLDED(Block.box(3.0, 0.0, 4.0, 13.0, 5.0, 12.0)),
		RUG(Block.box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0)),
		WALL(Block.box(1.0, 1.0, 15.0, 15.0, 15.0, 16.0));

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
