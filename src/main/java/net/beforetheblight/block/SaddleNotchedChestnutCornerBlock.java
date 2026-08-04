package net.beforetheblight.block;

import java.util.Map;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A dedicated interlocking corner for alternating chestnut-log wall courses.
 *
 * <p>The lower visual course is authored as an X-over-Z joint when facing
 * north; the upper course reverses the overlap. Horizontal rotation then
 * carries that local joint orientation to the other three facings. The small
 * vertical separation in the collision shape follows the model's chinking
 * gap instead of presenting an invisible full cube.</p>
 *
 * <p>Newly placed corners are deliberately unchinked. Applying one clay ball
 * fills the joint and sets {@link #CHINKED}; there is intentionally no
 * destructive click action that can remove finished chinking by accident.</p>
 */
public final class SaddleNotchedChestnutCornerBlock extends HorizontalDirectionalBlock {
	public static final MapCodec<SaddleNotchedChestnutCornerBlock> CODEC =
		simpleCodec(SaddleNotchedChestnutCornerBlock::new);
	public static final EnumProperty<Course> COURSE =
		EnumProperty.create("course", Course.class);
	public static final BooleanProperty CHINKED =
		BooleanProperty.create("chinked");

	private static final VoxelShape LOWER_NORTH_SHAPE = Shapes.or(
		// Z log below, X log above: local X-over-Z.
		Block.box(2.0, 0.0, 0.0, 14.0, 7.75, 16.0),
		Block.box(0.0, 8.25, 2.0, 16.0, 16.0, 14.0)
	);
	private static final VoxelShape UPPER_NORTH_SHAPE = Shapes.or(
		// X log below, Z log above: local Z-over-X.
		Block.box(0.0, 0.0, 2.0, 16.0, 7.75, 14.0),
		Block.box(2.0, 8.25, 0.0, 14.0, 16.0, 16.0)
	);
	private static final Map<Direction, VoxelShape> LOWER_SHAPES =
		Shapes.rotateHorizontal(LOWER_NORTH_SHAPE);
	private static final Map<Direction, VoxelShape> UPPER_SHAPES =
		Shapes.rotateHorizontal(UPPER_NORTH_SHAPE);

	public SaddleNotchedChestnutCornerBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(
			this.stateDefinition
				.any()
				.setValue(FACING, Direction.NORTH)
				.setValue(COURSE, Course.LOWER)
				.setValue(CHINKED, false)
		);
	}

	@Override
	public MapCodec<SaddleNotchedChestnutCornerBlock> codec() {
		return CODEC;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockPos pos = context.getClickedPos();
		BlockState below = context.getLevel().getBlockState(pos.below());
		Course course = below.is(this) && below.hasProperty(COURSE)
			? below.getValue(COURSE).opposite()
			: Course.LOWER;
		return this.defaultBlockState()
			.setValue(FACING, context.getHorizontalDirection().getOpposite())
			.setValue(COURSE, course)
			.setValue(CHINKED, false);
	}

	@Override
	protected VoxelShape getShape(
		BlockState state,
		BlockGetter level,
		BlockPos pos,
		CollisionContext context
	) {
		Map<Direction, VoxelShape> shapes = state.getValue(COURSE) == Course.LOWER
			? LOWER_SHAPES
			: UPPER_SHAPES;
		return shapes.get(state.getValue(FACING));
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
		if (!stack.is(Items.CLAY_BALL) || state.getValue(CHINKED)) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		BlockState current = level.getBlockState(pos);
		if (!current.is(this) || current.getValue(CHINKED)) {
			return InteractionResult.PASS;
		}
		BlockState chinked = current.setValue(CHINKED, true);
		if (!level.setBlock(pos, chinked, Block.UPDATE_ALL)) {
			return InteractionResult.FAIL;
		}
		stack.consume(1, player);
		level.playSound(
			null,
			pos,
			SoundEvents.MUD_PLACE,
			SoundSource.BLOCKS,
			0.8F,
			0.9F
		);
		level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, chinked));
		return InteractionResult.SUCCESS_SERVER;
	}

	@Override
	protected void createBlockStateDefinition(
		StateDefinition.Builder<Block, BlockState> builder
	) {
		builder.add(FACING, COURSE, CHINKED);
	}

	public enum Course implements StringRepresentable {
		LOWER("lower"),
		UPPER("upper");

		private final String serializedName;

		Course(String serializedName) {
			this.serializedName = serializedName;
		}

		public Course opposite() {
			return this == LOWER ? UPPER : LOWER;
		}

		@Override
		public String getSerializedName() {
			return this.serializedName;
		}
	}
}
