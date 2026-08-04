package net.beforetheblight.block.springhouse;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * One-block directional dairy furniture and countertop props.
 *
 * <p>The registry supplies a fixed {@link ShapeKind} for each block ID. The
 * per-instance codec captures that immutable kind, so the blocks retain a
 * normal properties-only serialized construction path without pretending
 * visually different props share one collision box.</p>
 */
public final class SpringhousePropBlock extends SpringhouseFacingBlock {
	private final MapCodec<SpringhousePropBlock> codec;

	public SpringhousePropBlock(
		BlockBehaviour.Properties properties,
		ShapeKind shapeKind
	) {
		super(properties, shapeKind.northShape());
		this.codec = simpleCodec(
			decodedProperties -> new SpringhousePropBlock(
				decodedProperties,
				shapeKind
			)
		);
	}

	@Override
	public MapCodec<SpringhousePropBlock> codec() {
		return this.codec;
	}

	public enum ShapeKind {
		TABLE(
			Shapes.or(
				// Thick working top.
				Block.box(1.0, 10.0, 1.0, 15.0, 13.0, 15.0),
				// Four square legs and two visible cross braces.
				Block.box(2.0, 0.0, 2.0, 4.0, 10.25, 4.0),
				Block.box(12.0, 0.0, 2.0, 14.0, 10.25, 4.0),
				Block.box(2.0, 0.0, 12.0, 4.0, 10.25, 14.0),
				Block.box(12.0, 0.0, 12.0, 14.0, 10.25, 14.0),
				Block.box(3.0, 5.0, 1.75, 13.0, 7.0, 4.25),
				Block.box(3.0, 5.0, 11.75, 13.0, 7.0, 14.25)
			)
		),
		STAND(
			Shapes.or(
				Block.box(2.0, 1.0, 2.0, 14.0, 3.0, 14.0),
				Block.box(2.0, 7.0, 2.0, 14.0, 9.0, 14.0),
				Block.box(2.0, 13.0, 2.0, 14.0, 15.0, 14.0),
				Block.box(1.0, 0.0, 1.0, 3.0, 16.0, 3.0),
				Block.box(13.0, 0.0, 1.0, 15.0, 16.0, 3.0),
				Block.box(1.0, 0.0, 13.0, 3.0, 16.0, 15.0),
				Block.box(13.0, 0.0, 13.0, 15.0, 16.0, 15.0)
			)
		),
		PAIL(
			Shapes.or(
				Block.box(4.0, 1.0, 4.0, 12.0, 3.0, 12.0),
				Block.box(3.0, 2.75, 4.0, 5.0, 11.0, 12.0),
				Block.box(11.0, 2.75, 4.0, 13.0, 11.0, 12.0),
				Block.box(4.75, 2.5, 3.0, 11.25, 10.75, 5.0),
				Block.box(4.75, 2.5, 11.0, 11.25, 10.75, 13.0),
				Block.box(2.75, 10.75, 2.75, 5.25, 12.0, 13.25),
				Block.box(10.75, 10.75, 2.75, 13.25, 12.0, 13.25),
				Block.box(4.75, 10.5, 2.5, 11.25, 11.75, 5.5),
				Block.box(4.75, 10.5, 10.5, 11.25, 11.75, 13.5),
				Block.box(3.0, 11.75, 3.0, 5.0, 16.0, 5.0),
				Block.box(11.0, 11.75, 3.0, 13.0, 16.0, 5.0),
				Block.box(4.75, 15.0, 3.25, 11.25, 15.75, 4.75)
			)
		),
		CROCK(
			Shapes.or(
				Block.box(4.0, 1.0, 4.0, 12.0, 3.0, 12.0),
				Block.box(3.0, 2.75, 3.0, 13.0, 11.0, 13.0),
				Block.box(5.0, 10.75, 5.0, 11.0, 14.0, 11.0),
				Block.box(4.0, 13.75, 4.0, 12.0, 15.0, 12.0)
			)
		),
		PAN(
			Shapes.or(
				Block.box(2.0, 1.0, 2.0, 14.0, 3.0, 14.0),
				Block.box(1.0, 2.75, 1.0, 3.0, 6.0, 15.0),
				Block.box(13.0, 2.75, 1.0, 15.0, 6.0, 15.0),
				Block.box(2.75, 2.5, 0.75, 13.25, 5.75, 3.25),
				Block.box(2.75, 2.5, 12.75, 13.25, 5.75, 15.25)
			)
		),
		CHURN(
			Shapes.or(
				Block.box(4.0, 1.0, 4.0, 12.0, 3.0, 12.0),
				Block.box(3.0, 2.75, 3.0, 13.0, 11.0, 13.0),
				Block.box(4.0, 10.75, 4.0, 12.0, 13.0, 12.0),
				Block.box(7.25, 12.0, 7.25, 8.75, 16.0, 8.75)
			)
		),
		BUTTER_CROCK(
			Shapes.or(
				Block.box(4.0, 1.0, 4.0, 12.0, 3.0, 12.0),
				Block.box(3.0, 2.75, 3.0, 13.0, 10.0, 13.0),
				Block.box(4.0, 9.75, 4.0, 12.0, 12.0, 12.0),
				Block.box(6.0, 11.75, 6.0, 10.0, 13.0, 10.0)
			)
		),
		CHEESE_WHEEL(
			Block.box(2.0, 1.0, 2.0, 14.0, 7.0, 14.0)
		);

		private final VoxelShape northShape;

		ShapeKind(VoxelShape northShape) {
			this.northShape = northShape;
		}

		private VoxelShape northShape() {
			return this.northShape;
		}
	}
}
