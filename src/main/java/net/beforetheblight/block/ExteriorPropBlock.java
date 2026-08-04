package net.beforetheblight.block;

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
 * Shared placement and collision behavior for small farmstead props.
 *
 * <p>The visible models remain separate assets. The shape key is encoded so
 * block serialization does not depend on an unrecorded constructor choice.</p>
 */
public final class ExteriorPropBlock extends HorizontalDirectionalBlock {
	public static final MapCodec<ExteriorPropBlock> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			ShapeKind.CODEC.fieldOf("shape").forGetter(block -> block.shapeKind),
			propertiesCodec()
		).apply(instance, ExteriorPropBlock::new)
	);

	private final ShapeKind shapeKind;
	private final Map<Direction, VoxelShape> shapes;

	public ExteriorPropBlock(ShapeKind shapeKind, BlockBehaviour.Properties properties) {
		super(properties);
		this.shapeKind = shapeKind;
		this.shapes = Shapes.rotateHorizontal(shapeKind.shape());
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	public MapCodec<ExteriorPropBlock> codec() {
		return CODEC;
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
	protected VoxelShape getCollisionShape(
		BlockState state,
		BlockGetter level,
		BlockPos pos,
		CollisionContext context
	) {
		return this.shapeKind.hasCollision()
			? this.shapes.get(state.getValue(FACING))
			: Shapes.empty();
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	public enum ShapeKind implements StringRepresentable {
		LOW_STACK("low_stack", Block.box(1, 0, 1, 15, 8, 15), true),
		TALL_STACK("tall_stack", Block.box(1, 0, 1, 15, 14, 15), true),
		STUMP("stump", Block.box(2, 0, 2, 14, 10, 14), true),
		TOOL_STUMP(
			"tool_stump",
			Shapes.or(
				Block.box(2, 0, 2, 14, 10, 14),
				Block.box(6, 9, 6, 10, 16, 10)
			),
			true
		),
		SAWHORSE(
			"sawhorse",
			Shapes.or(
				Block.box(1, 7, 5, 15, 10, 11),
				Block.box(2, 0, 4, 5, 8, 7),
				Block.box(11, 0, 4, 14, 8, 7),
				Block.box(2, 0, 9, 5, 8, 12),
				Block.box(11, 0, 9, 14, 8, 12)
			),
			true
		),
		WALL_RACK("wall_rack", Block.box(1, 2, 13, 15, 15, 16), false),
		WHEEL("wheel", Block.box(1, 1, 6, 15, 15, 10), false),
		BARREL("barrel", Block.box(2, 0, 2, 14, 16, 14), true),
		CRATE("crate", Block.box(1, 0, 1, 15, 14, 15), true),
		SACK("sack", Block.box(3, 0, 3, 13, 13, 13), true),
		PATH("path", Block.box(0, 0, 0, 16, 15, 16), true),
		GROUND_STRIP("ground_strip", Block.box(0, 0, 0, 16, 1, 16), false),
		STONES(
			"stones",
			Shapes.or(
				Block.box(0, 0, 2, 6, 3, 9),
				Block.box(5, 0, 0, 12, 4, 7),
				Block.box(10, 0, 6, 16, 3, 14)
			),
			true
		),
		ROOT(
			"root",
			Shapes.or(
				Block.box(0, 0, 7, 16, 2, 10),
				Block.box(8, 0, 2, 11, 2, 14)
			),
			false
		),
		BRANCH(
			"branch",
			Shapes.or(
				Block.box(1, 0, 7, 15, 3, 10),
				Block.box(9, 1, 3, 12, 3, 9)
			),
			false
		),
		BRUSH("brush", Block.box(1, 0, 1, 15, 9, 15), false);

		public static final Codec<ShapeKind> CODEC = StringRepresentable.fromEnum(
			ShapeKind::values
		);

		private final String serializedName;
		private final VoxelShape shape;
		private final boolean collision;

		ShapeKind(String serializedName, VoxelShape shape, boolean collision) {
			this.serializedName = serializedName;
			this.shape = shape;
			this.collision = collision;
		}

		@Override
		public String getSerializedName() {
			return this.serializedName;
		}

		public VoxelShape shape() {
			return this.shape;
		}

		public boolean hasCollision() {
			return this.collision;
		}
	}
}
