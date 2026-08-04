package net.beforetheblight.block.furniture;

import java.util.Map;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Two-part trundle bed with synchronized closed/pulled-out presentation. */
public final class OpenTwoPartFurnitureBlock
	extends AbstractTwoPartFurnitureBlock {
	public static final MapCodec<OpenTwoPartFurnitureBlock> CODEC =
		simpleCodec(OpenTwoPartFurnitureBlock::new);
	public static final BooleanProperty OPEN = BooleanProperty.create("open");

	private static final Map<Direction, VoxelShape> LEFT_CLOSED_SHAPES =
		Shapes.rotateHorizontal(trundleShape(FurniturePart.LEFT, false));
	private static final Map<Direction, VoxelShape> RIGHT_CLOSED_SHAPES =
		Shapes.rotateHorizontal(trundleShape(FurniturePart.RIGHT, false));
	private static final Map<Direction, VoxelShape> LEFT_OPEN_SHAPES =
		Shapes.rotateHorizontal(trundleShape(FurniturePart.LEFT, true));
	private static final Map<Direction, VoxelShape> RIGHT_OPEN_SHAPES =
		Shapes.rotateHorizontal(trundleShape(FurniturePart.RIGHT, true));

	public OpenTwoPartFurnitureBlock(BlockBehaviour.Properties properties) {
		super(
			properties,
			trundleShape(FurniturePart.LEFT, false),
			trundleShape(FurniturePart.RIGHT, false),
			false
		);
		this.registerDefaultState(this.stateDefinition.any()
			.setValue(FACING, Direction.NORTH)
			.setValue(PART, FurniturePart.LEFT)
			.setValue(OPEN, false));
	}

	@Override
	public MapCodec<OpenTwoPartFurnitureBlock> codec() {
		return CODEC;
	}

	@Override
	protected BlockState synchronizeFromPartner(
		BlockState state,
		BlockState partner
	) {
		return state.setValue(OPEN, partner.getValue(OPEN));
	}

	@Override
	protected InteractionResult useWithoutItem(
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		BlockHitResult hit
	) {
		BlockPos partnerPos = this.partnerPos(pos, state);
		BlockState partner = level.getBlockState(partnerPos);
		if (!this.isMatchingPartner(state, partner)) {
			return InteractionResult.CONSUME;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		BlockState current = level.getBlockState(pos);
		BlockPos currentPartnerPos = this.partnerPos(pos, current);
		BlockState currentPartner = level.getBlockState(currentPartnerPos);
		if (!current.is(this) || !this.isMatchingPartner(current, currentPartner)) {
			return InteractionResult.CONSUME;
		}
		boolean open = !current.getValue(OPEN);
		if (open && !this.hasClearPulloutLane(
			level,
			pos,
			current,
			currentPartner,
			player
		)) {
			return InteractionResult.CONSUME;
		}
		BlockState changed = current.setValue(OPEN, open);
		BlockState changedPartner = currentPartner.setValue(OPEN, open);
		if (!level.setBlock(pos, changed, Block.UPDATE_ALL)) {
			return InteractionResult.FAIL;
		}
		if (!level.setBlock(currentPartnerPos, changedPartner, Block.UPDATE_ALL)
			&& !level.getBlockState(currentPartnerPos).equals(changedPartner)) {
			level.setBlock(pos, current, Block.UPDATE_ALL);
			level.setBlock(currentPartnerPos, currentPartner, Block.UPDATE_ALL);
			return InteractionResult.FAIL;
		}
		level.playSound(
			null,
			pos,
			SoundEvents.WOOD_PLACE,
			SoundSource.BLOCKS,
			0.4F,
			open ? 1.05F : 0.9F
		);
		var event = open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE;
		level.gameEvent(event, pos, GameEvent.Context.of(player, changed));
		return InteractionResult.SUCCESS_SERVER;
	}

	@Override
	protected VoxelShape getShape(
		BlockState state,
		BlockGetter level,
		BlockPos pos,
		CollisionContext context
	) {
		boolean left = state.getValue(PART) == FurniturePart.LEFT;
		Map<Direction, VoxelShape> shapes = state.getValue(OPEN)
			? (left ? LEFT_OPEN_SHAPES : RIGHT_OPEN_SHAPES)
			: (left ? LEFT_CLOSED_SHAPES : RIGHT_CLOSED_SHAPES);
		return shapes.get(state.getValue(FACING));
	}

	private boolean hasClearPulloutLane(
		Level level,
		BlockPos pos,
		BlockState state,
		BlockState partner,
		Player player
	) {
		BlockPos partnerPos = this.partnerPos(pos, state);
		return this.hasClearPulloutLane(level, pos, state, player)
			&& this.hasClearPulloutLane(level, partnerPos, partner, player);
	}

	private boolean hasClearPulloutLane(
		Level level,
		BlockPos pos,
		BlockState state,
		Player player
	) {
		BlockPos frontPos = pos.relative(state.getValue(FACING));
		if (!level.getWorldBorder().isWithinBounds(frontPos)
			|| !level.getBlockState(frontPos)
				.getCollisionShape(level, frontPos, CollisionContext.empty())
				.isEmpty()) {
			return false;
		}

		BlockState opened = state.setValue(OPEN, true);
		VoxelShape worldShape = opened
			.getCollisionShape(level, pos, CollisionContext.of(player))
			.move(pos.getX(), pos.getY(), pos.getZ());
		return level.isUnobstructed(player, worldShape);
	}

	private static VoxelShape trundleShape(FurniturePart part, boolean open) {
		double legMinX = part == FurniturePart.LEFT ? 1.5 : 12.5;
		double legMaxX = part == FurniturePart.LEFT ? 3.5 : 14.5;
		VoxelShape fixedBed = Shapes.or(
			Block.box(0, 4, 1, 16, 7, 15),
			Block.box(legMinX, 0, 2, legMaxX, 6.25, 4),
			Block.box(legMinX, 0, 12, legMaxX, 6.25, 14),
			part == FurniturePart.LEFT
				? Block.box(0, 3, 1.25, 1.5, 11, 14.75)
				: Block.box(14.5, 3, 1.25, 16, 11, 14.75)
		);
		VoxelShape lowerBed = open
			? Block.box(0, 1, -7, 16, 4, 3)
			: Block.box(0, 1, 3, 16, 4, 13);
		return Shapes.or(fixedBed, lowerBed);
	}

	@Override
	protected void createBlockStateDefinition(
		StateDefinition.Builder<Block, BlockState> builder
	) {
		super.createBlockStateDefinition(builder);
		builder.add(OPEN);
	}
}
