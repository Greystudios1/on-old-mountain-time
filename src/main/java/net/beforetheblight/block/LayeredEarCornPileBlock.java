package net.beforetheblight.block;

import java.util.EnumMap;
import java.util.Map;

import com.mojang.serialization.MapCodec;
import net.beforetheblight.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Stable stored ear corn rather than a ticking drying bundle.
 *
 * <p>Five named fullness states each represent four additional dried ears.
 * Edge and corner silhouettes are directional but share the same exact
 * conservation rule. Empty-hand removal takes one four-ear layer at a time.</p>
 */
public final class LayeredEarCornPileBlock extends HorizontalDirectionalBlock {
	public static final MapCodec<LayeredEarCornPileBlock> CODEC =
		simpleCodec(LayeredEarCornPileBlock::new);
	public static final EnumProperty<Fullness> FULLNESS =
		EnumProperty.create("fullness", Fullness.class);
	public static final EnumProperty<PileShape> PILE_SHAPE =
		EnumProperty.create("pile_shape", PileShape.class);
	public static final int EARS_PER_LEVEL = 4;

	private static final Map<Fullness, Map<PileShape, Map<Direction, VoxelShape>>> SHAPES =
		buildShapes();

	public LayeredEarCornPileBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(
			stateDefinition.any()
				.setValue(FACING, Direction.NORTH)
				.setValue(FULLNESS, Fullness.QUARTER)
				.setValue(PILE_SHAPE, PileShape.CENTER)
		);
	}

	@Override
	public MapCodec<LayeredEarCornPileBlock> codec() {
		return CODEC;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = defaultBlockState().setValue(
			FACING,
			context.getHorizontalDirection()
		);
		return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return Block.canSupportCenter(level, pos.below(), Direction.UP);
	}

	@Override
	protected BlockState updateShape(
		BlockState state,
		LevelReader level,
		ScheduledTickAccess ticks,
		BlockPos pos,
		Direction direction,
		BlockPos neighborPos,
		BlockState neighborState,
		RandomSource random
	) {
		if (direction == Direction.DOWN && !state.canSurvive(level, pos)) {
			return Blocks.AIR.defaultBlockState();
		}
		return super.updateShape(
			state,
			level,
			ticks,
			pos,
			direction,
			neighborPos,
			neighborState,
			random
		);
	}

	@Override
	protected VoxelShape getShape(
		BlockState state,
		BlockGetter level,
		BlockPos pos,
		CollisionContext context
	) {
		return SHAPES
			.get(state.getValue(FULLNESS))
			.get(state.getValue(PILE_SHAPE))
			.get(state.getValue(FACING));
	}

	@Override
	protected InteractionResult useItemOn(
		ItemStack stack,
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		InteractionHand hand,
		BlockHitResult hit
	) {
		if (stack.isEmpty()) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!stack.is(ModItems.DRIED_EAR_OF_CORN)) {
			return InteractionResult.PASS;
		}
		Fullness next = state.getValue(FULLNESS).next();
		if (next == null || (!player.hasInfiniteMaterials() && stack.getCount() < EARS_PER_LEVEL)) {
			return InteractionResult.CONSUME;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		ServerLevel serverLevel = (ServerLevel) level;
		BlockState current = serverLevel.getBlockState(pos);
		if (!current.is(this)) {
			return InteractionResult.PASS;
		}
		Fullness currentNext = current.getValue(FULLNESS).next();
		if (currentNext == null) {
			return InteractionResult.CONSUME;
		}
		BlockState filled = current.setValue(FULLNESS, currentNext);
		if (!serverLevel.setBlock(pos, filled, Block.UPDATE_ALL)) {
			return InteractionResult.FAIL;
		}
		stack.consume(EARS_PER_LEVEL, player);
		serverLevel.playSound(
			null,
			pos,
			SoundEvents.WOOD_PLACE,
			SoundSource.BLOCKS,
			0.65F,
			0.8F
		);
		serverLevel.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, filled));
		return InteractionResult.SUCCESS_SERVER;
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

		ServerLevel serverLevel = (ServerLevel) level;
		BlockState current = serverLevel.getBlockState(pos);
		if (!current.is(this)) {
			return InteractionResult.PASS;
		}
		Fullness previous = current.getValue(FULLNESS).previous();
		boolean changed = previous == null
			? serverLevel.removeBlock(pos, false)
			: serverLevel.setBlock(
				pos,
				current.setValue(FULLNESS, previous),
				Block.UPDATE_ALL
			);
		if (!changed) {
			return InteractionResult.FAIL;
		}
		Block.popResource(
			serverLevel,
			pos,
			new ItemStack(ModItems.DRIED_EAR_OF_CORN, EARS_PER_LEVEL)
		);
		serverLevel.playSound(
			null,
			pos,
			SoundEvents.CROP_BREAK,
			SoundSource.BLOCKS,
			0.65F,
			0.8F
		);
		var event = previous == null ? GameEvent.BLOCK_DESTROY : GameEvent.BLOCK_CHANGE;
		BlockState eventState = previous == null
			? current
			: current.setValue(FULLNESS, previous);
		serverLevel.gameEvent(event, pos, GameEvent.Context.of(player, eventState));
		return InteractionResult.SUCCESS_SERVER;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, FULLNESS, PILE_SHAPE);
	}

	private static Map<Fullness, Map<PileShape, Map<Direction, VoxelShape>>> buildShapes() {
		Map<Fullness, Map<PileShape, Map<Direction, VoxelShape>>> result =
			new EnumMap<>(Fullness.class);
		for (Fullness fullness : Fullness.values()) {
			double height = fullness.height();
			Map<PileShape, Map<Direction, VoxelShape>> byShape =
				new EnumMap<>(PileShape.class);
			byShape.put(
				PileShape.CENTER,
				Shapes.rotateHorizontal(Block.box(2.0, 0.0, 2.0, 14.0, height, 14.0))
			);
			byShape.put(
				PileShape.EDGE,
				Shapes.rotateHorizontal(Block.box(2.0, 0.0, 0.0, 14.0, height, 14.0))
			);
			VoxelShape insideCorner = Shapes.or(
				Block.box(0.0, 0.0, 0.0, 16.0, height, 8.0),
				Block.box(8.0, 0.0, 8.0, 16.0, height, 16.0)
			);
			byShape.put(PileShape.INSIDE_CORNER, Shapes.rotateHorizontal(insideCorner));
			byShape.put(
				PileShape.OUTSIDE_CORNER,
				Shapes.rotateHorizontal(Block.box(8.0, 0.0, 0.0, 16.0, height, 8.0))
			);
			result.put(fullness, byShape);
		}
		return result;
	}

	public enum Fullness implements StringRepresentable {
		QUARTER("quarter", 4.0, 1),
		HALF("half", 8.0, 2),
		THREE_QUARTER("three_quarter", 12.0, 3),
		FULL("full", 15.0, 4),
		OVERFLOW("overflow", 16.0, 5);

		private static final Fullness[] VALUES = values();
		private final String serializedName;
		private final double height;
		private final int levels;

		Fullness(String serializedName, double height, int levels) {
			this.serializedName = serializedName;
			this.height = height;
			this.levels = levels;
		}

		public double height() {
			return height;
		}

		public int earCount() {
			return levels * EARS_PER_LEVEL;
		}

		public Fullness next() {
			return ordinal() + 1 < VALUES.length ? VALUES[ordinal() + 1] : null;
		}

		public Fullness previous() {
			return ordinal() == 0 ? null : VALUES[ordinal() - 1];
		}

		@Override
		public String getSerializedName() {
			return serializedName;
		}
	}

	public enum PileShape implements StringRepresentable {
		CENTER("center"),
		EDGE("edge"),
		INSIDE_CORNER("inside_corner"),
		OUTSIDE_CORNER("outside_corner");

		private final String serializedName;

		PileShape(String serializedName) {
			this.serializedName = serializedName;
		}

		@Override
		public String getSerializedName() {
			return serializedName;
		}
	}
}
