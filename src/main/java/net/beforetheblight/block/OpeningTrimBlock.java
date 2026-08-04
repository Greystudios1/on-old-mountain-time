package net.beforetheblight.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import java.util.Map;
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
 * Reduced-section jamb and lintel members whose length follows the block axis.
 */
public final class OpeningTrimBlock extends RotatedPillarBlock {
	public static final MapCodec<OpeningTrimBlock> CODEC =
		RecordCodecBuilder.mapCodec(instance -> instance.group(
			Profile.CODEC.fieldOf("profile").forGetter(OpeningTrimBlock::profile),
			propertiesCodec()
		).apply(instance, OpeningTrimBlock::new));

	private static final Map<Direction.Axis, VoxelShape> JAMB_SHAPES = Map.of(
		Direction.Axis.X, Block.box(0.0, 5.0, 5.0, 16.0, 11.0, 11.0),
		Direction.Axis.Y, Block.box(5.0, 0.0, 5.0, 11.0, 16.0, 11.0),
		Direction.Axis.Z, Block.box(5.0, 5.0, 0.0, 11.0, 11.0, 16.0)
	);
	private static final Map<Direction.Axis, VoxelShape> LINTEL_SHAPES = Map.of(
		Direction.Axis.X, Block.box(0.0, 4.0, 4.0, 16.0, 12.0, 12.0),
		Direction.Axis.Y, Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0),
		Direction.Axis.Z, Block.box(4.0, 4.0, 0.0, 12.0, 12.0, 16.0)
	);

	private final Profile profile;

	public OpeningTrimBlock(Profile profile, BlockBehaviour.Properties properties) {
		super(properties);
		this.profile = profile;
	}

	@Override
	public MapCodec<OpeningTrimBlock> codec() {
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
		Map<Direction.Axis, VoxelShape> shapes =
			this.profile == Profile.JAMB ? JAMB_SHAPES : LINTEL_SHAPES;
		return shapes.get(state.getValue(AXIS));
	}

	public enum Profile implements StringRepresentable {
		JAMB,
		LINTEL;

		public static final Codec<Profile> CODEC =
			StringRepresentable.fromEnum(Profile::values);

		@Override
		public String getSerializedName() {
			return this.name().toLowerCase(Locale.ROOT);
		}
	}
}
