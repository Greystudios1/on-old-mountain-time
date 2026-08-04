package net.beforetheblight.block;

import java.util.Locale;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Low-profile construction clutter with rotation-matched collision.
 */
public final class BoardClutterBlock extends HorizontalDirectionalBlock {
	public static final MapCodec<BoardClutterBlock> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			Profile.CODEC.fieldOf("profile").forGetter(block -> block.profile),
			propertiesCodec()
		).apply(instance, BoardClutterBlock::new)
	);

	private static final Map<Profile, VoxelShape> NORTH_SHAPES = Map.of(
		Profile.STACK,
		Shapes.or(
			Block.box(1.0, 0.0, 2.0, 15.0, 2.0, 14.0),
			Block.box(2.0, 2.0, 1.0, 14.0, 4.0, 15.0),
			Block.box(1.0, 4.0, 3.0, 15.0, 6.0, 13.0)
		),
		Profile.LOOSE,
		Block.box(1.0, 0.0, 3.0, 15.0, 1.5, 13.0)
	);

	private final Profile profile;
	private final Map<Direction, VoxelShape> shapes;

	public BoardClutterBlock(Profile profile, BlockBehaviour.Properties properties) {
		super(properties);
		this.profile = profile;
		this.shapes = Shapes.rotateHorizontal(NORTH_SHAPES.get(profile));
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	public MapCodec<BoardClutterBlock> codec() {
		return CODEC;
	}

	public Profile profile() {
		return this.profile;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(
			FACING,
			context.getHorizontalDirection().getOpposite()
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
		STACK,
		LOOSE;

		public static final Codec<Profile> CODEC = StringRepresentable.fromEnum(Profile::values);

		@Override
		public String getSerializedName() {
			return this.name().toLowerCase(Locale.ROOT);
		}
	}
}
