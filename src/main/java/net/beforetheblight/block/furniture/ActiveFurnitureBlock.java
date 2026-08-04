package net.beforetheblight.block.furniture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Decorative textile equipment with a synchronized static active/idle model.
 * No production recipes or ticking process are implied.
 */
public final class ActiveFurnitureBlock extends DirectionalFurnitureBlock {
	public enum Style implements StringRepresentable {
		SPINNING_WHEEL("spinning_wheel", spinningWheelShape()),
		TABLE_LOOM("table_loom", tableLoomShape()),
		QUILL_WHEEL("quill_wheel", quillWheelShape()),
		YARN_WINDER("yarn_winder", yarnWinderShape());

		public static final Codec<Style> CODEC =
			StringRepresentable.fromEnum(Style::values);

		private final String serializedName;
		private final VoxelShape shape;

		Style(String serializedName, VoxelShape shape) {
			this.serializedName = serializedName;
			this.shape = shape;
		}

		@Override
		public String getSerializedName() {
			return this.serializedName;
		}
	}

	public static final MapCodec<ActiveFurnitureBlock> CODEC =
		RecordCodecBuilder.mapCodec(instance -> instance.group(
			Style.CODEC.fieldOf("style").forGetter(ActiveFurnitureBlock::style),
			propertiesCodec()
		).apply(instance, ActiveFurnitureBlock::new));
	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

	private final Style style;

	public ActiveFurnitureBlock(
		Style style,
		BlockBehaviour.Properties properties
	) {
		super(properties, style.shape);
		this.style = style;
		this.registerDefaultState(this.stateDefinition.any()
			.setValue(FACING, Direction.NORTH)
			.setValue(ACTIVE, false));
	}

	public Style style() {
		return this.style;
	}

	@Override
	public MapCodec<ActiveFurnitureBlock> codec() {
		return CODEC;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		return state != null && state.canSurvive(
			context.getLevel(),
			context.getClickedPos()
		) ? state.setValue(ACTIVE, false) : null;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
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
		return directionToNeighbour == Direction.DOWN
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

	@Override
	protected InteractionResult useWithoutItem(
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		BlockHitResult hit
	) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		BlockState current = level.getBlockState(pos);
		if (!current.is(this)) {
			return InteractionResult.PASS;
		}
		boolean active = !current.getValue(ACTIVE);
		BlockState changed = current.setValue(ACTIVE, active);
		if (!level.setBlock(pos, changed, Block.UPDATE_ALL)) {
			return InteractionResult.FAIL;
		}
		level.playSound(
			null,
			pos,
			SoundEvents.WOOD_PLACE,
			SoundSource.BLOCKS,
			0.4F,
			active ? 1.15F : 0.85F
		);
		var event = active ? GameEvent.BLOCK_ACTIVATE : GameEvent.BLOCK_DEACTIVATE;
		level.gameEvent(event, pos, GameEvent.Context.of(player, changed));
		return InteractionResult.SUCCESS_SERVER;
	}

	@Override
	protected void createBlockStateDefinition(
		StateDefinition.Builder<Block, BlockState> builder
	) {
		super.createBlockStateDefinition(builder);
		builder.add(ACTIVE);
	}

	private static VoxelShape spinningWheelShape() {
		return Shapes.or(
			Block.box(2, 0, 5, 4, 8, 7),
			Block.box(12, 0, 9, 14, 8, 11),
			Block.box(2, 7, 4, 14, 9, 12),
			Block.box(3, 8, 6, 13, 16, 10),
			Block.box(7, 8, 3, 9, 16, 13)
		);
	}

	private static VoxelShape tableLoomShape() {
		return Shapes.or(
			Block.box(1, 0, 2, 15, 4, 14),
			Block.box(2, 4, 3, 4, 13, 5),
			Block.box(12, 4, 3, 14, 13, 5),
			Block.box(2, 11, 3, 14, 13, 13),
			Block.box(3, 6, 7, 13, 8, 10)
		);
	}

	private static VoxelShape quillWheelShape() {
		return Shapes.or(
			Block.box(2, 0, 3, 4, 8, 5),
			Block.box(12, 0, 11, 14, 8, 13),
			Block.box(2, 7, 3, 14, 9, 13),
			Block.box(4, 8, 5, 12, 16, 11),
			Block.box(7, 9, 2, 9, 14, 14)
		);
	}

	private static VoxelShape yarnWinderShape() {
		return Shapes.or(
			Block.box(6, 0, 6, 10, 8, 10),
			Block.box(2, 7, 7, 14, 9, 9),
			Block.box(7, 7, 2, 9, 9, 14),
			Block.box(3, 9, 3, 5, 15, 5),
			Block.box(11, 9, 11, 13, 15, 13)
		);
	}
}
