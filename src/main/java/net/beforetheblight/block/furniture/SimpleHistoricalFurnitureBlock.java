package net.beforetheblight.block.furniture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** Configured one-block static furniture and wall fixtures. */
public final class SimpleHistoricalFurnitureBlock
	extends DirectionalFurnitureBlock {
	public enum Style implements StringRepresentable {
		SLAB_BENCH("slab_bench", slabBenchShape(), false),
		WOODEN_CRADLE("wooden_cradle", cradleShape(), false),
		MANTEL_SHELF("mantel_shelf", mantelShape(), true),
		GUN_RACK("gun_rack", gunRackShape(), true),
		BROOM_RACK("broom_rack", broomRackShape(), true),
		CRADLE_SHELF("cradle_shelf", cradleShelfShape(), true);

		public static final Codec<Style> CODEC =
			StringRepresentable.fromEnum(Style::values);

		private final String serializedName;
		private final VoxelShape shape;
		private final boolean wallMounted;

		Style(String serializedName, VoxelShape shape, boolean wallMounted) {
			this.serializedName = serializedName;
			this.shape = shape;
			this.wallMounted = wallMounted;
		}

		@Override
		public String getSerializedName() {
			return this.serializedName;
		}
	}

	public static final MapCodec<SimpleHistoricalFurnitureBlock> CODEC =
		RecordCodecBuilder.mapCodec(instance -> instance.group(
			Style.CODEC.fieldOf("style")
				.forGetter(SimpleHistoricalFurnitureBlock::style),
			propertiesCodec()
		).apply(instance, SimpleHistoricalFurnitureBlock::new));

	private final Style style;

	public SimpleHistoricalFurnitureBlock(
		Style style,
		BlockBehaviour.Properties properties
	) {
		super(properties, style.shape);
		this.style = style;
	}

	public Style style() {
		return this.style;
	}

	@Override
	public MapCodec<SimpleHistoricalFurnitureBlock> codec() {
		return CODEC;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		if (!this.style.wallMounted) {
			BlockState state = super.getStateForPlacement(context);
			return state != null && state.canSurvive(
				context.getLevel(),
				context.getClickedPos()
			) ? state : null;
		}

		for (Direction direction : context.getNearestLookingDirections()) {
			if (!direction.getAxis().isHorizontal()) {
				continue;
			}
			BlockState state = this.defaultBlockState()
				.setValue(FACING, direction.getOpposite());
			if (state.canSurvive(context.getLevel(), context.getClickedPos())) {
				return state;
			}
		}
		return null;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		if (this.style.wallMounted) {
			Direction facing = state.getValue(FACING);
			BlockPos supportPos = pos.relative(facing.getOpposite());
			return level.getBlockState(supportPos).isFaceSturdy(
				level,
				supportPos,
				facing
			);
		}

		BlockPos below = pos.below();
		return level.getBlockState(below).isFaceSturdy(
			level,
			below,
			Direction.UP
		);
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
		Direction supportDirection = this.style.wallMounted
			? state.getValue(FACING).getOpposite()
			: Direction.DOWN;
		return directionToNeighbour == supportDirection
			&& !state.canSurvive(level, pos)
				? Blocks.AIR.defaultBlockState()
				: super.updateShape(
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

	private static VoxelShape slabBenchShape() {
		return Shapes.or(
			Block.box(1, 0, 3, 3, 7, 5),
			Block.box(13, 0, 11, 15, 7, 13),
			Block.box(0.5, 7, 2, 15.5, 9, 14)
		);
	}

	private static VoxelShape cradleShape() {
		return Shapes.or(
			Block.box(1, 1, 2, 15, 5, 14),
			Block.box(0, 0, 1, 3, 2, 15),
			Block.box(13, 0, 1, 16, 2, 15),
			Block.box(1, 5, 2, 3, 11, 14),
			Block.box(13, 5, 2, 15, 11, 14)
		);
	}

	private static VoxelShape mantelShape() {
		return Shapes.or(
			Block.box(0, 8, 10, 16, 10, 16),
			Block.box(1, 4, 13, 4, 8, 16),
			Block.box(12, 4, 13, 15, 8, 16)
		);
	}

	private static VoxelShape gunRackShape() {
		return Shapes.or(
			Block.box(1, 5, 14, 15, 7, 16),
			Block.box(1, 12, 14, 15, 14, 16),
			Block.box(3, 4, 11, 5, 8, 14),
			Block.box(11, 11, 11, 13, 15, 14)
		);
	}

	private static VoxelShape broomRackShape() {
		return Shapes.or(
			Block.box(1, 8, 14, 15, 11, 16),
			Block.box(3, 7, 11, 4.5, 9, 14),
			Block.box(7.25, 7, 11, 8.75, 9, 14),
			Block.box(11.5, 7, 11, 13, 9, 14)
		);
	}

	private static VoxelShape cradleShelfShape() {
		return Shapes.or(
			Block.box(1, 6, 9, 15, 8, 16),
			Block.box(1, 8, 14, 3, 14, 16),
			Block.box(13, 8, 14, 15, 14, 16),
			Block.box(2, 13, 13, 14, 15, 16)
		);
	}
}
