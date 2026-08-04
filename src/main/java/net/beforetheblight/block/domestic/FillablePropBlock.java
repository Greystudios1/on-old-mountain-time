package net.beforetheblight.block.domestic;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * A horizontal prop with four persistent, model-selectable fill levels.
 *
 * <p>The state is deliberately data-only. Specific registered containers can
 * use loot, recipes, or authored structure states without requiring a block
 * entity or a ticking inventory.</p>
 */
public final class FillablePropBlock extends HorizontalPropBlock {
	public static final MapCodec<FillablePropBlock> CODEC =
		RecordCodecBuilder.mapCodec(instance -> instance.group(
			Profile.CODEC.fieldOf("profile").forGetter(FillablePropBlock::profile),
			propertiesCodec()
		).apply(instance, FillablePropBlock::new));
	public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, 3);

	public FillablePropBlock(Profile profile, BlockBehaviour.Properties properties) {
		super(profile, properties);
		this.registerDefaultState(
			this.stateDefinition.any()
				.setValue(FACING, Direction.NORTH)
				.setValue(FILL, 0)
		);
	}

	@Override
	public MapCodec<FillablePropBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(
		StateDefinition.Builder<Block, BlockState> builder
	) {
		super.createBlockStateDefinition(builder);
		builder.add(FILL);
	}
}
