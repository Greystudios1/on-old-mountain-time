package net.beforetheblight.block.domestic;

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
 * A reusable, horizontally oriented block for small domestic objects.
 *
 * <p>The profile is part of the block codec, while facing is ordinary persisted
 * block state. This lets visually unrelated registered props share predictable
 * placement and collision behavior without adding block entities.</p>
 */
public class HorizontalPropBlock extends HorizontalDirectionalBlock {
	public static final MapCodec<HorizontalPropBlock> CODEC =
		RecordCodecBuilder.mapCodec(instance -> instance.group(
			Profile.CODEC.fieldOf("profile").forGetter(HorizontalPropBlock::profile),
			propertiesCodec()
		).apply(instance, HorizontalPropBlock::new));

	private final Profile profile;
	private final Map<Direction, VoxelShape> shapes;

	public HorizontalPropBlock(Profile profile, BlockBehaviour.Properties properties) {
		super(properties);
		this.profile = profile;
		this.shapes = Shapes.rotateHorizontal(profile.northShape());
		this.registerDefaultState(
			this.stateDefinition.any().setValue(FACING, Direction.NORTH)
		);
	}

	@Override
	public MapCodec<? extends HorizontalPropBlock> codec() {
		return CODEC;
	}

	public final Profile profile() {
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
	protected void createBlockStateDefinition(
		StateDefinition.Builder<Block, BlockState> builder
	) {
		builder.add(FACING);
	}

	/**
	 * Stable shape vocabulary shared by floor-standing domestic props.
	 * NORTH-authored asymmetric profiles are rotated with their block state.
	 */
	public enum Profile implements StringRepresentable {
		TINY(Block.box(6.0, 0.0, 6.0, 10.0, 5.0, 10.0)),
		FLAT(Block.box(2.0, 0.0, 2.0, 14.0, 2.0, 14.0)),
		SMALL(Block.box(4.0, 0.0, 4.0, 12.0, 8.0, 12.0)),
		TALL(Block.box(5.0, 0.0, 5.0, 11.0, 16.0, 11.0)),
		MEDIUM(Block.box(2.0, 0.0, 2.0, 14.0, 10.0, 14.0)),
		LARGE(Block.box(1.0, 0.0, 1.0, 15.0, 15.0, 15.0)),
		WIDE_LOW(Block.box(1.0, 0.0, 4.0, 15.0, 6.0, 12.0)),
		LONG_THIN(Block.box(6.0, 0.0, 0.0, 10.0, 4.0, 16.0));

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
