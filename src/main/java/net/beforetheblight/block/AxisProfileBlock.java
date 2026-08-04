package net.beforetheblight.block;

import java.util.Locale;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A non-full board or roof member whose length follows the placement axis.
 */
public final class AxisProfileBlock extends RotatedPillarBlock {
	public static final MapCodec<AxisProfileBlock> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			Profile.CODEC.fieldOf("profile").forGetter(block -> block.profile),
			propertiesCodec()
		).apply(instance, AxisProfileBlock::new)
	);

	private static final Map<Direction.Axis, VoxelShape> ROOF_BOARD_SHAPES = Map.of(
		Direction.Axis.X, Block.box(0.0, 6.0, 2.0, 16.0, 10.0, 14.0),
		Direction.Axis.Y, Block.box(6.0, 0.0, 2.0, 10.0, 16.0, 14.0),
		Direction.Axis.Z, Block.box(2.0, 6.0, 0.0, 14.0, 10.0, 16.0)
	);
	private static final Map<Direction.Axis, VoxelShape> RAFTER_TAIL_SHAPES = Map.of(
		Direction.Axis.X, Block.box(0.0, 5.0, 5.0, 16.0, 11.0, 11.0),
		Direction.Axis.Y, Block.box(5.0, 0.0, 5.0, 11.0, 16.0, 11.0),
		Direction.Axis.Z, Block.box(5.0, 5.0, 0.0, 11.0, 11.0, 16.0)
	);

	private final Profile profile;

	public AxisProfileBlock(Profile profile, BlockBehaviour.Properties properties) {
		super(properties);
		this.profile = profile;
	}

	@Override
	public MapCodec<AxisProfileBlock> codec() {
		return CODEC;
	}

	public Profile profile() {
		return this.profile;
	}

	@Override
	protected VoxelShape getShape(
		BlockState state,
		BlockGetter level,
		BlockPos pos,
		CollisionContext context
	) {
		Map<Direction.Axis, VoxelShape> profiles = this.profile == Profile.ROOF_BOARD
			? ROOF_BOARD_SHAPES
			: RAFTER_TAIL_SHAPES;
		return profiles.get(state.getValue(AXIS));
	}

	public enum Profile implements StringRepresentable {
		ROOF_BOARD,
		RAFTER_TAIL;

		public static final Codec<Profile> CODEC = StringRepresentable.fromEnum(Profile::values);

		@Override
		public String getSerializedName() {
			return this.name().toLowerCase(Locale.ROOT);
		}
	}
}
