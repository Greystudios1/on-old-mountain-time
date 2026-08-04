package net.beforetheblight.block;

import java.util.Map;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Axis-aware structural timber with an outline matched to its visible
 * historic framing profile.
 *
 * <p>The separate factories preserve data-driven codecs for Minecraft while
 * avoiding a block-state property that would create unobtainable profile
 * combinations. Registry IDs express the member's function; this class only
 * supplies the honest collision profile.</p>
 */
public final class ChestnutStructuralMemberBlock extends RotatedPillarBlock {
	public static final MapCodec<ChestnutStructuralMemberBlock> BEAM_CODEC =
		simpleCodec(ChestnutStructuralMemberBlock::beam);
	public static final MapCodec<ChestnutStructuralMemberBlock> JOIST_CODEC =
		simpleCodec(ChestnutStructuralMemberBlock::joist);
	public static final MapCodec<ChestnutStructuralMemberBlock> SLEEPER_CODEC =
		simpleCodec(ChestnutStructuralMemberBlock::sleeper);
	public static final MapCodec<ChestnutStructuralMemberBlock> BLOCKING_CODEC =
		simpleCodec(ChestnutStructuralMemberBlock::blocking);

	private static final Map<Direction.Axis, VoxelShape> BEAM_SHAPES =
		axisShapes(12.0, 12.0);
	private static final Map<Direction.Axis, VoxelShape> JOIST_SHAPES =
		axisShapes(8.0, 16.0);
	private static final Map<Direction.Axis, VoxelShape> SLEEPER_SHAPES =
		axisShapes(12.0, 8.0);
	private static final Map<Direction.Axis, VoxelShape> BLOCKING_SHAPES =
		axisShapes(8.0, 8.0);

	private final MapCodec<ChestnutStructuralMemberBlock> memberCodec;
	private final Map<Direction.Axis, VoxelShape> shapes;

	private ChestnutStructuralMemberBlock(
		BlockBehaviour.Properties properties,
		MapCodec<ChestnutStructuralMemberBlock> memberCodec,
		Map<Direction.Axis, VoxelShape> shapes
	) {
		super(properties);
		this.memberCodec = memberCodec;
		this.shapes = shapes;
	}

	public static ChestnutStructuralMemberBlock beam(
		BlockBehaviour.Properties properties
	) {
		return new ChestnutStructuralMemberBlock(
			properties,
			BEAM_CODEC,
			BEAM_SHAPES
		);
	}

	public static ChestnutStructuralMemberBlock joist(
		BlockBehaviour.Properties properties
	) {
		return new ChestnutStructuralMemberBlock(
			properties,
			JOIST_CODEC,
			JOIST_SHAPES
		);
	}

	public static ChestnutStructuralMemberBlock sleeper(
		BlockBehaviour.Properties properties
	) {
		return new ChestnutStructuralMemberBlock(
			properties,
			SLEEPER_CODEC,
			SLEEPER_SHAPES
		);
	}

	public static ChestnutStructuralMemberBlock blocking(
		BlockBehaviour.Properties properties
	) {
		return new ChestnutStructuralMemberBlock(
			properties,
			BLOCKING_CODEC,
			BLOCKING_SHAPES
		);
	}

	@Override
	public MapCodec<ChestnutStructuralMemberBlock> codec() {
		return this.memberCodec;
	}

	@Override
	protected VoxelShape getShape(
		BlockState state,
		BlockGetter level,
		BlockPos pos,
		CollisionContext context
	) {
		return this.shapes.get(state.getValue(AXIS));
	}

	private static Map<Direction.Axis, VoxelShape> axisShapes(
		double crossSection,
		double verticalThickness
	) {
		double sideInset = (16.0 - crossSection) / 2.0;
		double verticalInset = (16.0 - verticalThickness) / 2.0;
		return Map.of(
			Direction.Axis.X,
			Block.box(
				0.0,
				verticalInset,
				sideInset,
				16.0,
				16.0 - verticalInset,
				16.0 - sideInset
			),
			Direction.Axis.Y,
			Block.box(
				sideInset,
				0.0,
				sideInset,
				16.0 - sideInset,
				16.0,
				16.0 - sideInset
			),
			Direction.Axis.Z,
			Block.box(
				sideInset,
				verticalInset,
				0.0,
				16.0 - sideInset,
				16.0 - verticalInset,
				16.0
			)
		);
	}
}
