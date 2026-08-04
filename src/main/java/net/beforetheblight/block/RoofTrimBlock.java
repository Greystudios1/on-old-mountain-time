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
 * Directional, non-full roof finish pieces.
 *
 * <p>Every profile has a dedicated ID/model, while this shared implementation
 * supplies conservative collision that follows rotation. This keeps ridge,
 * rake, eave, and chimney pieces from behaving like invisible full cubes.</p>
 */
public final class RoofTrimBlock extends HorizontalDirectionalBlock {
	public static final MapCodec<RoofTrimBlock> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			Profile.CODEC.fieldOf("profile").forGetter(block -> block.profile),
			propertiesCodec()
		).apply(instance, RoofTrimBlock::new)
	);

	private static final Map<Profile, VoxelShape> NORTH_SHAPES = Map.of(
		Profile.RIDGE_CAP,
		Shapes.or(
			Block.box(0.0, 8.0, 5.0, 16.0, 12.0, 11.0),
			Block.box(0.0, 12.0, 7.0, 16.0, 16.0, 9.0)
		),
		Profile.RIDGE_END,
		Shapes.or(
			Block.box(0.0, 8.0, 5.0, 10.0, 12.0, 11.0),
			Block.box(0.0, 12.0, 7.0, 9.0, 16.0, 9.0)
		),
		Profile.RAKE_LEFT,
		Block.box(0.0, 0.0, 0.0, 4.0, 16.0, 16.0),
		Profile.RAKE_RIGHT,
		Block.box(12.0, 0.0, 0.0, 16.0, 16.0, 16.0),
		Profile.LOWER_EAVE,
		Block.box(0.0, 0.0, 0.0, 16.0, 6.0, 10.0),
		Profile.CHIMNEY_TRANSITION,
		Shapes.or(
			Block.box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0),
			Block.box(0.0, 3.0, 6.0, 16.0, 9.0, 10.0)
		)
	);

	private final Profile profile;
	private final Map<Direction, VoxelShape> shapes;

	public RoofTrimBlock(Profile profile, BlockBehaviour.Properties properties) {
		super(properties);
		this.profile = profile;
		this.shapes = Shapes.rotateHorizontal(NORTH_SHAPES.get(profile));
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	public MapCodec<RoofTrimBlock> codec() {
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
		RIDGE_CAP,
		RIDGE_END,
		RAKE_LEFT,
		RAKE_RIGHT,
		LOWER_EAVE,
		CHIMNEY_TRANSITION;

		public static final Codec<Profile> CODEC = StringRepresentable.fromEnum(Profile::values);

		@Override
		public String getSerializedName() {
			return this.name().toLowerCase(Locale.ROOT);
		}
	}
}
