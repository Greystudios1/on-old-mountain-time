package net.beforetheblight.block.furniture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Configured two-block implementation shared by wide static furniture. */
public final class TwoPartFurnitureBlock extends AbstractTwoPartFurnitureBlock {
	public enum Style implements StringRepresentable {
		BACKLESS_BENCH(
			"backless_bench",
			benchShape(false, true),
			benchShape(false, false),
			false
		),
		WALL_BENCH(
			"wall_bench",
			wallBenchShape(true),
			wallBenchShape(false),
			true
		),
		HIGH_BACK_SETTLE(
			"high_back_settle",
			benchShape(true, true),
			benchShape(true, false),
			false
		),
		TRESTLE_TABLE(
			"trestle_table",
			trestleTableShape(true),
			trestleTableShape(false),
			false
		),
		FARMHOUSE_TABLE(
			"farmhouse_table",
			farmhouseTableShape(true),
			farmhouseTableShape(false),
			false
		),
		ROUGH_WORKBENCH(
			"rough_workbench",
			workbenchShape(true),
			workbenchShape(false),
			false
		),
		SHAVING_HORSE(
			"shaving_horse",
			shavingHorseShape(true),
			shavingHorseShape(false),
			false
		);

		public static final Codec<Style> CODEC =
			StringRepresentable.fromEnum(Style::values);

		private final String serializedName;
		private final VoxelShape leftShape;
		private final VoxelShape rightShape;
		private final boolean wallMounted;

		Style(
			String serializedName,
			VoxelShape leftShape,
			VoxelShape rightShape,
			boolean wallMounted
		) {
			this.serializedName = serializedName;
			this.leftShape = leftShape;
			this.rightShape = rightShape;
			this.wallMounted = wallMounted;
		}

		@Override
		public String getSerializedName() {
			return this.serializedName;
		}
	}

	public static final MapCodec<TwoPartFurnitureBlock> CODEC =
		RecordCodecBuilder.mapCodec(instance -> instance.group(
			Style.CODEC.fieldOf("style").forGetter(TwoPartFurnitureBlock::style),
			propertiesCodec()
		).apply(instance, TwoPartFurnitureBlock::new));

	private final Style style;

	public TwoPartFurnitureBlock(Style style, BlockBehaviour.Properties properties) {
		super(
			properties,
			style.leftShape,
			style.rightShape,
			style.wallMounted
		);
		this.style = style;
	}

	public Style style() {
		return this.style;
	}

	@Override
	public MapCodec<TwoPartFurnitureBlock> codec() {
		return CODEC;
	}

	private static VoxelShape benchShape(boolean highBack, boolean leftPart) {
		double legMinX = leftPart ? 2 : 12;
		double legMaxX = leftPart ? 4 : 14;
		VoxelShape base = Shapes.or(
			Block.box(legMinX, 0, 3, legMaxX, 7.5, 5),
			Block.box(legMinX, 0, 11, legMaxX, 7.5, 13),
			Block.box(0, 7, 2, 16, 9, 14)
		);
		double postMinX = leftPart ? 1.25 : 13;
		double postMaxX = leftPart ? 3 : 14.75;
		return highBack
			? Shapes.or(
				base,
				Block.box(postMinX, 8, 12.5, postMaxX, 16, 15.25),
				Block.box(0, 10, 13, 16, 12, 15.25),
				Block.box(0, 14, 13, 16, 16, 15.25)
			)
			: base;
	}

	private static VoxelShape wallBenchShape(boolean leftPart) {
		double legMinX = leftPart ? 2 : 12;
		double legMaxX = leftPart ? 4 : 14;
		double postMinX = leftPart ? 1.25 : 13;
		double postMaxX = leftPart ? 3 : 14.75;
		return Shapes.or(
			Block.box(0, 7, 2, 16, 9, 14),
			Block.box(legMinX, 0, 3, legMaxX, 7.5, 5),
			Block.box(legMinX, 0, 11, legMaxX, 7.5, 13),
			Block.box(postMinX, 8, 12.5, postMaxX, 16, 15.25),
			Block.box(0, 10, 13, 16, 12, 15.25),
			Block.box(0, 14, 13, 16, 16, 15.25)
		);
	}

	private static VoxelShape trestleTableShape(boolean outerEnd) {
		VoxelShape shape = Shapes.or(
			Block.box(0, 11, 1, 16, 13, 15),
			Block.box(1, 9, 6.5, 15, 11, 9.5)
		);
		return outerEnd
			? Shapes.or(
				shape,
				Block.box(1, 0, 3, 3.5, 11, 13),
				Block.box(12.5, 0, 3, 15, 11, 13)
			)
			: shape;
	}

	private static VoxelShape farmhouseTableShape(boolean outerEnd) {
		VoxelShape top = Block.box(0, 11.25, 1, 16, 12.5, 15);
		return outerEnd
			? Shapes.or(
				top,
				Block.box(1, 0, 2, 3, 11.25, 4),
				Block.box(13, 0, 2, 15, 11.25, 4),
				Block.box(1, 0, 12, 3, 11.25, 14),
				Block.box(13, 0, 12, 15, 11.25, 14)
			)
			: top;
	}

	private static VoxelShape workbenchShape(boolean outerEnd) {
		VoxelShape top = Shapes.or(
			Block.box(0, 11, 1, 16, 14, 15),
			Block.box(0, 8.5, 2, 16, 11, 4),
			Block.box(0, 8.5, 12, 16, 11, 14)
		);
		return outerEnd
			? Shapes.or(
				top,
				Block.box(1, 0, 2, 4, 11, 5),
				Block.box(12, 0, 11, 15, 11, 14)
			)
			: top;
	}

	private static VoxelShape shavingHorseShape(boolean clampEnd) {
		VoxelShape base = Shapes.or(
			Block.box(0, 6, 5, 16, 8.5, 11),
			Block.box(1, 0, 5, 3, 6, 7),
			Block.box(13, 0, 9, 15, 6, 11)
		);
		return clampEnd
			? Shapes.or(
				base,
				Block.box(3, 8, 6, 13, 10, 10),
				Block.box(7, 8, 6.5, 9, 16, 9.5)
			)
			: base;
	}
}
