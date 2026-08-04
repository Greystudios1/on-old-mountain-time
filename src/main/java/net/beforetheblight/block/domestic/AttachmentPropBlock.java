package net.beforetheblight.block.domestic;

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
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A small prop that may be fixed to a floor, wall, or ceiling.
 *
 * <p>Vanilla's face-attached base class supplies placement fallback, support
 * validation, and removal when the supporting face disappears.</p>
 */
public class AttachmentPropBlock extends FaceAttachedHorizontalDirectionalBlock {
	public static final MapCodec<AttachmentPropBlock> CODEC =
		RecordCodecBuilder.mapCodec(instance -> instance.group(
			Profile.CODEC.fieldOf("profile").forGetter(AttachmentPropBlock::profile),
			propertiesCodec()
		).apply(instance, AttachmentPropBlock::new));

	private final Profile profile;
	private final Map<AttachFace, Map<Direction, VoxelShape>> shapes;

	public AttachmentPropBlock(Profile profile, BlockBehaviour.Properties properties) {
		super(properties);
		this.profile = profile;
		this.shapes = Map.of(
			AttachFace.FLOOR,
			Shapes.rotateHorizontal(profile.floorShape()),
			AttachFace.WALL,
			Shapes.rotateHorizontal(profile.wallShape()),
			AttachFace.CEILING,
			Shapes.rotateHorizontal(profile.ceilingShape())
		);
		this.registerDefaultState(
			this.stateDefinition.any()
				.setValue(FACING, Direction.NORTH)
				.setValue(FACE, AttachFace.WALL)
		);
	}

	@Override
	public MapCodec<? extends AttachmentPropBlock> codec() {
		return CODEC;
	}

	public final Profile profile() {
		return this.profile;
	}

	@Override
	protected VoxelShape getShape(
		BlockState state,
		BlockGetter level,
		BlockPos pos,
		CollisionContext context
	) {
		return this.shapes
			.get(state.getValue(FACE))
			.get(state.getValue(FACING));
	}

	@Override
	protected void createBlockStateDefinition(
		StateDefinition.Builder<Block, BlockState> builder
	) {
		builder.add(FACING, FACE);
	}

	public enum Profile implements StringRepresentable {
		SMALL(
			Block.box(4.0, 0.0, 4.0, 12.0, 8.0, 12.0),
			Block.box(4.0, 4.0, 12.0, 12.0, 12.0, 16.0),
			Block.box(4.0, 8.0, 4.0, 12.0, 16.0, 12.0)
		),
		TALL(
			Block.box(5.0, 0.0, 5.0, 11.0, 16.0, 11.0),
			Block.box(5.0, 0.0, 12.0, 11.0, 16.0, 16.0),
			Block.box(5.0, 0.0, 5.0, 11.0, 16.0, 11.0)
		),
		WIDE(
			Block.box(1.0, 0.0, 5.0, 15.0, 7.0, 11.0),
			Block.box(1.0, 4.0, 12.0, 15.0, 12.0, 16.0),
			Block.box(1.0, 9.0, 5.0, 15.0, 16.0, 11.0)
		);

		public static final Codec<Profile> CODEC =
			StringRepresentable.fromEnum(Profile::values);

		private final VoxelShape floorShape;
		private final VoxelShape wallShape;
		private final VoxelShape ceilingShape;

		Profile(
			VoxelShape floorShape,
			VoxelShape wallShape,
			VoxelShape ceilingShape
		) {
			this.floorShape = floorShape;
			this.wallShape = wallShape;
			this.ceilingShape = ceilingShape;
		}

		VoxelShape floorShape() {
			return this.floorShape;
		}

		VoxelShape wallShape() {
			return this.wallShape;
		}

		VoxelShape ceilingShape() {
			return this.ceilingShape;
		}

		@Override
		public String getSerializedName() {
			return this.name().toLowerCase(Locale.ROOT);
		}
	}
}
