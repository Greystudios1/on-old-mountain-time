package net.beforetheblight.block;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.MapCodec;
import net.beforetheblight.entity.RockingChairSeatEntity;
import net.beforetheblight.registry.ModFurniture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A plain Appalachian ladder-back rocking chair.
 *
 * <p>The occupied flag only switches the static block model off while the
 * transient seat entity renders the same model with a small rocking transform.
 * A scheduled server tick repairs stale occupied states after a server stop,
 * interrupted mount, or removed helper entity.</p>
 */
public final class RockingChairBlock extends HorizontalDirectionalBlock {
	public static final MapCodec<RockingChairBlock> CODEC = simpleCodec(RockingChairBlock::new);
	public static final BooleanProperty OCCUPIED = BooleanProperty.create("occupied");

	private static final int OCCUPANCY_CHECK_DELAY = 10;
	private static final VoxelShape NORTH_SHAPE = Shapes.or(
		// Rockers.
		Block.box(2, 0, 0, 4, 2, 16),
		Block.box(12, 0, 0, 14, 2, 16),
		// Four legs and the seat.
		Block.box(2, 1, 3, 4, 8, 5),
		Block.box(12, 1, 3, 14, 8, 5),
		Block.box(2, 1, 11, 4, 8, 13),
		Block.box(12, 1, 11, 14, 8, 13),
		Block.box(2, 7, 3, 14, 9, 13),
		// Back posts and two ladder slats.
		Block.box(2, 8, 11, 4, 24, 14),
		Block.box(12, 8, 11, 14, 24, 14),
		Block.box(4, 14, 12, 12, 16, 14),
		Block.box(4, 19, 12, 12, 21, 14)
	);
	private static final Map<Direction, VoxelShape> SHAPES =
		Shapes.rotateHorizontal(NORTH_SHAPE);

	public RockingChairBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any()
			.setValue(FACING, Direction.NORTH)
			.setValue(OCCUPIED, false));
	}

	@Override
	public MapCodec<RockingChairBlock> codec() {
		return CODEC;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState()
			.setValue(FACING, context.getHorizontalDirection().getOpposite())
			.setValue(OCCUPIED, false);
	}

	@Override
	protected VoxelShape getShape(
		BlockState state,
		BlockGetter level,
		BlockPos pos,
		CollisionContext context
	) {
		return SHAPES.get(state.getValue(FACING));
	}

	/**
	 * The seated player extends through both blocks above the chair. Refuse
	 * seating whenever either part of that conservative rider volume has a
	 * collision shape.
	 */
	public static boolean hasSeatingClearance(BlockGetter level, BlockPos pos) {
		return hasNoCollision(level, pos.above())
			&& hasNoCollision(level, pos.above(2));
	}

	private static boolean hasNoCollision(BlockGetter level, BlockPos pos) {
		return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
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

		/*
		 * A block item has a concrete use-on-block action, so let vanilla try
		 * to place it against the chair. Ordinary held items have no useful
		 * chair-side action and should not force the player to empty a hotbar
		 * slot before sitting. Sneak-use remains vanilla's universal held-item
		 * bypass because ServerPlayerGameMode suppresses the block interaction
		 * before this method is called.
		 */
		if (stack.getItem() instanceof BlockItem) {
			return InteractionResult.PASS;
		}

		return this.tryToSit(state, level, pos, player);
	}

	@Override
	protected InteractionResult useWithoutItem(
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		BlockHitResult hit
	) {
		InteractionResult result = this.tryToSit(state, level, pos, player);
		return result instanceof InteractionResult.Success success
			? success.withoutItem()
			: result;
	}

	private InteractionResult tryToSit(
		BlockState state,
		Level level,
		BlockPos pos,
		Player player
	) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (!(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.PASS;
		}

		BlockState currentState = serverLevel.getBlockState(pos);
		if (!currentState.is(this)) {
			return InteractionResult.PASS;
		}
		if (!hasSeatingClearance(serverLevel, pos)) {
			return InteractionResult.CONSUME;
		}

		List<RockingChairSeatEntity> seats = RockingChairSeatEntity.findAt(serverLevel, pos);
		seats.sort(Comparator.comparingInt(RockingChairSeatEntity::getId));
		RockingChairSeatEntity occupiedSeat = seats.stream()
			.filter(RockingChairSeatEntity::isVehicle)
			.findFirst()
			.orElse(null);
		RockingChairSeatEntity reusableSeat = occupiedSeat == null
			? seats.stream()
				.filter(candidate -> !candidate.isVehicle())
				.findFirst()
				.orElse(null)
			: null;

		// Helpers can overlap briefly during interrupted interactions or a
		// logout/rejoin race. Keep the lowest-id occupied helper canonical,
		// then safely dismount and discard every other helper without allowing
		// its removal callback to clear the canonical chair state.
		for (RockingChairSeatEntity candidate : seats) {
			if (candidate != occupiedSeat && candidate != reusableSeat) {
				candidate.discardAsNoncanonical();
			}
		}

		if (occupiedSeat != null) {
			if (!currentState.getValue(OCCUPIED)) {
				BlockState repaired = currentState.setValue(OCCUPIED, true);
				if (serverLevel.setBlock(
					pos,
					repaired,
					Block.UPDATE_ALL
				)) {
					serverLevel.gameEvent(
						GameEvent.BLOCK_ACTIVATE,
						pos,
						GameEvent.Context.of(player, repaired)
					);
				}
			}
			serverLevel.scheduleTick(pos, this, OCCUPANCY_CHECK_DELAY);
			return occupiedSeat.hasPassenger(player)
				? InteractionResult.SUCCESS_SERVER
				: InteractionResult.CONSUME;
		}

		RockingChairSeatEntity seat = reusableSeat;

		boolean createdSeat = false;
		if (seat == null) {
			seat = ModFurniture.ROCKING_CHAIR_SEAT.create(
				serverLevel,
				EntitySpawnReason.TRIGGERED
			);
			if (seat == null) {
				return InteractionResult.PASS;
			}

			seat.anchorTo(pos, currentState.getValue(FACING));
			if (!serverLevel.addFreshEntity(seat)) {
				return InteractionResult.PASS;
			}
			createdSeat = true;
		}

		BlockState occupiedState = currentState.setValue(OCCUPIED, true);
		if (!currentState.getValue(OCCUPIED)
			&& !serverLevel.setBlock(pos, occupiedState, Block.UPDATE_ALL)) {
			if (createdSeat) {
				seat.discard();
			}
			return InteractionResult.PASS;
		}
		serverLevel.scheduleTick(pos, this, OCCUPANCY_CHECK_DELAY);

		if (!player.startRiding(seat)) {
			serverLevel.setBlock(pos, currentState.setValue(OCCUPIED, false), Block.UPDATE_ALL);
			if (createdSeat) {
				seat.discard();
			}
			return InteractionResult.PASS;
		}

		serverLevel.playSound(
			null,
			pos,
			SoundEvents.WOOD_PLACE,
			SoundSource.BLOCKS,
			0.55F,
			0.75F
		);
		serverLevel.gameEvent(
			GameEvent.BLOCK_ACTIVATE,
			pos,
			GameEvent.Context.of(player, occupiedState)
		);
		return InteractionResult.SUCCESS_SERVER;
	}

	@Override
	protected void tick(
		BlockState state,
		ServerLevel level,
		BlockPos pos,
		RandomSource random
	) {
		if (!state.getValue(OCCUPIED)) {
			return;
		}

		List<RockingChairSeatEntity> seats = RockingChairSeatEntity.findAt(level, pos);
		seats.sort(Comparator.comparingInt(RockingChairSeatEntity::getId));
		RockingChairSeatEntity occupiedSeat = seats.stream()
			.filter(RockingChairSeatEntity::isVehicle)
			.findFirst()
			.orElse(null);
		if (occupiedSeat == null) {
			seats.forEach(RockingChairSeatEntity::discardAsNoncanonical);
			BlockState released = state.setValue(OCCUPIED, false);
			if (level.setBlock(pos, released, Block.UPDATE_ALL)) {
				level.gameEvent(
					GameEvent.BLOCK_DEACTIVATE,
					pos,
					GameEvent.Context.of(released)
				);
			}
			return;
		}

		for (RockingChairSeatEntity seat : seats) {
			if (seat != occupiedSeat) {
				seat.discardAsNoncanonical();
			}
		}
		level.scheduleTick(pos, this, OCCUPANCY_CHECK_DELAY);
	}

	@Override
	protected void onPlace(
		BlockState state,
		Level level,
		BlockPos pos,
		BlockState oldState,
		boolean movedByPiston
	) {
		if (!level.isClientSide() && state.getValue(OCCUPIED) && !oldState.is(this)) {
			level.scheduleTick(pos, this, 1);
		}
	}

	@Override
	protected void affectNeighborsAfterRemoval(
		BlockState state,
		ServerLevel level,
		BlockPos pos,
		boolean movedByPiston
	) {
		RockingChairSeatEntity.removeAt(level, pos);
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, OCCUPIED);
	}
}
