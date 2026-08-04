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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A small, non-powered piece of period door or shutter hardware mounted on a
 * sturdy vertical face.
 *
 * <p>Each profile has its own outline rather than occupying a decorative full
 * cube. The placement and neighbour checks follow a ladder: the hardware faces
 * away from its support and drops when that support is removed.</p>
 */
public final class MountedHardwareBlock extends HorizontalDirectionalBlock {
	public static final MapCodec<MountedHardwareBlock> CODEC =
		RecordCodecBuilder.mapCodec(instance -> instance.group(
			Profile.CODEC.fieldOf("profile").forGetter(MountedHardwareBlock::profile),
			propertiesCodec()
		).apply(instance, MountedHardwareBlock::new));

	private final Profile profile;
	private final Map<Direction, VoxelShape> shapes;

	public MountedHardwareBlock(Profile profile, BlockBehaviour.Properties properties) {
		super(properties);
		this.profile = profile;
		this.shapes = Shapes.rotateHorizontal(profile.northShape());
		this.registerDefaultState(
			this.stateDefinition.any().setValue(FACING, Direction.NORTH)
		);
	}

	@Override
	public MapCodec<MountedHardwareBlock> codec() {
		return CODEC;
	}

	public Profile profile() {
		return this.profile;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		LevelReader level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		for (Direction direction : context.getNearestLookingDirections()) {
			if (direction.getAxis().isHorizontal()) {
				BlockState state = this.defaultBlockState()
					.setValue(FACING, direction.getOpposite());
				if (state.canSurvive(level, pos)) {
					return state;
				}
			}
		}
		return null;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		Direction facing = state.getValue(FACING);
		BlockPos supportPos = pos.relative(facing.getOpposite());
		return level.getBlockState(supportPos)
			.isFaceSturdy(level, supportPos, facing);
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
		if (
			directionToNeighbour.getOpposite() == state.getValue(FACING)
				&& !state.canSurvive(level, pos)
		) {
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
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	public enum Profile implements StringRepresentable {
		DROP_LATCH(
			Shapes.or(
				Block.box(2.0, 7.0, 13.0, 14.0, 9.0, 16.0),
				Block.box(1.0, 5.5, 13.0, 3.5, 10.5, 16.0)
			)
		),
		LATCH_CATCH(
			Shapes.or(
				Block.box(6.0, 5.0, 14.0, 10.0, 11.0, 16.0),
				Block.box(7.0, 6.0, 13.0, 9.0, 10.0, 14.0)
			)
		),
		WOODEN_HINGE(
			Shapes.or(
				Block.box(2.0, 6.0, 13.0, 14.0, 10.0, 16.0),
				Block.box(1.0, 4.5, 13.0, 3.0, 11.5, 16.0)
			)
		),
		STRAP_HINGE(
			Shapes.or(
				Block.box(2.0, 7.0, 14.0, 14.0, 9.0, 16.0),
				Block.box(1.0, 5.0, 13.0, 3.0, 11.0, 16.0)
			)
		),
		PINTLE(
			Shapes.or(
				Block.box(6.5, 4.0, 13.0, 9.5, 12.0, 16.0),
				Block.box(5.0, 7.0, 14.0, 11.0, 10.0, 16.0)
			)
		),
		HASP(
			Shapes.or(
				Block.box(3.0, 7.0, 14.0, 13.0, 9.0, 16.0),
				Block.box(11.0, 5.5, 13.0, 14.0, 10.5, 16.0)
			)
		),
		STAPLE(
			Shapes.or(
				Block.box(5.0, 5.0, 14.0, 7.0, 11.0, 16.0),
				Block.box(9.0, 5.0, 14.0, 11.0, 11.0, 16.0),
				Block.box(6.0, 9.0, 13.0, 10.0, 11.0, 16.0)
			)
		),
		SQUARE_NAILS(
			Shapes.or(
				Block.box(3.0, 5.0, 14.0, 5.0, 7.0, 16.0),
				Block.box(7.0, 9.0, 14.0, 9.0, 11.0, 16.0),
				Block.box(11.0, 4.0, 14.0, 13.0, 6.0, 16.0)
			)
		),
		CUT_NAILS(
			Shapes.or(
				Block.box(2.0, 4.0, 14.0, 4.0, 6.0, 16.0),
				Block.box(6.0, 8.0, 14.0, 8.0, 10.0, 16.0),
				Block.box(10.0, 5.0, 14.0, 12.0, 7.0, 16.0),
				Block.box(12.0, 10.0, 14.0, 14.0, 12.0, 16.0)
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
