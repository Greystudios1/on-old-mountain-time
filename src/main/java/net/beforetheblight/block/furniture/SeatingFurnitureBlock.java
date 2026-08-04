package net.beforetheblight.block.furniture;

import java.util.Comparator;
import java.util.List;

import net.beforetheblight.registry.ModFurnitureBlocks;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Shared, static sitting interaction for non-rocking chairs and stools.
 *
 * <p>The helper entity is intentionally invisible and non-persistent. Unlike
 * the animated rocking chair, the furniture block remains rendered while it
 * is occupied. The persistent {@link #OCCUPIED} state coordinates one
 * transient helper and is repaired by scheduled checks after unloads or
 * interrupted mounts.</p>
 */
public abstract class SeatingFurnitureBlock extends DirectionalFurnitureBlock {
	public static final BooleanProperty OCCUPIED = BooleanProperty.create("occupied");
	private static final int CLEARANCE_BLOCKS = 2;
	private static final int OCCUPANCY_CHECK_DELAY = 10;

	protected SeatingFurnitureBlock(
		BlockBehaviour.Properties properties,
		VoxelShape northShape
	) {
		super(properties, northShape);
		this.registerDefaultState(this.stateDefinition.any()
			.setValue(FACING, net.minecraft.core.Direction.NORTH)
			.setValue(OCCUPIED, false));
	}

	/** Height of the visible seat surface above the block floor, in blocks. */
	public abstract double seatHeight();

	/**
	 * The rider extends into both blocks above the furniture. Requiring both
	 * spaces to be collision-free prevents mounting into low ceilings.
	 */
	public static boolean hasSeatingClearance(BlockGetter level, BlockPos pos) {
		for (int offset = 1; offset <= CLEARANCE_BLOCKS; offset++) {
			BlockPos clearancePos = pos.above(offset);
			if (!level.getBlockState(clearancePos)
				.getCollisionShape(level, clearancePos)
				.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		return state == null ? null : state.setValue(OCCUPIED, false);
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

		// Preserve normal placement against the chair while allowing ordinary
		// held items to sit without forcing the player to clear a hotbar slot.
		if (stack.getItem() instanceof BlockItem) {
			return InteractionResult.PASS;
		}
		return this.tryToSit(level, pos, player);
	}

	@Override
	protected InteractionResult useWithoutItem(
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		BlockHitResult hit
	) {
		InteractionResult result = this.tryToSit(level, pos, player);
		return result instanceof InteractionResult.Success success
			? success.withoutItem()
			: result;
	}

	private InteractionResult tryToSit(Level level, BlockPos pos, Player player) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (!(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.PASS;
		}

		BlockState currentState = serverLevel.getBlockState(pos);
		if (currentState.getBlock() != this) {
			return InteractionResult.PASS;
		}
		if (!hasSeatingClearance(serverLevel, pos)) {
			return InteractionResult.CONSUME;
		}

		List<StaticFurnitureSeatEntity> seats =
			StaticFurnitureSeatEntity.findAt(serverLevel, pos);
		seats.sort(Comparator.comparingInt(StaticFurnitureSeatEntity::getId));

		StaticFurnitureSeatEntity occupiedSeat = seats.stream()
			.filter(StaticFurnitureSeatEntity::isVehicle)
			.findFirst()
			.orElse(null);
		if (occupiedSeat != null) {
			for (StaticFurnitureSeatEntity seat : seats) {
				if (seat != occupiedSeat) {
					seat.discardAsDuplicate();
				}
			}
			if (!currentState.getValue(OCCUPIED)) {
				BlockState occupiedState = currentState.setValue(OCCUPIED, true);
				if (serverLevel.setBlock(
					pos,
					occupiedState,
					Block.UPDATE_ALL
				)) {
					serverLevel.gameEvent(
						GameEvent.BLOCK_ACTIVATE,
						pos,
						GameEvent.Context.of(player, occupiedState)
					);
				}
			}
			serverLevel.scheduleTick(pos, this, OCCUPANCY_CHECK_DELAY);
			return occupiedSeat.hasPassenger(player)
				? InteractionResult.SUCCESS_SERVER
				: InteractionResult.CONSUME;
		}

		StaticFurnitureSeatEntity seat = seats.isEmpty() ? null : seats.getFirst();
		for (StaticFurnitureSeatEntity candidate : seats) {
			if (candidate != seat) {
				candidate.discardAsDuplicate();
			}
		}

		boolean createdSeat = false;
		if (seat == null) {
			seat = ModFurnitureBlocks.STATIC_SEAT.create(
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

		if (!currentState.getValue(OCCUPIED)
			&& !serverLevel.setBlock(
				pos,
				currentState.setValue(OCCUPIED, true),
				Block.UPDATE_ALL
			)) {
			if (createdSeat) {
				seat.discard();
			}
			return InteractionResult.PASS;
		}
		serverLevel.scheduleTick(pos, this, OCCUPANCY_CHECK_DELAY);

		if (!player.startRiding(seat)) {
			serverLevel.setBlock(
				pos,
				currentState.setValue(OCCUPIED, false),
				Block.UPDATE_ALL
			);
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
			0.45F,
			0.8F
		);
		serverLevel.gameEvent(
			GameEvent.BLOCK_ACTIVATE,
			pos,
			GameEvent.Context.of(player, currentState.setValue(OCCUPIED, true))
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

		List<StaticFurnitureSeatEntity> seats =
			StaticFurnitureSeatEntity.findAt(level, pos);
		seats.sort(Comparator.comparingInt(StaticFurnitureSeatEntity::getId));
		StaticFurnitureSeatEntity occupiedSeat = seats.stream()
			.filter(StaticFurnitureSeatEntity::isVehicle)
			.findFirst()
			.orElse(null);
		if (occupiedSeat == null) {
			seats.forEach(StaticFurnitureSeatEntity::discardAsDuplicate);
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

		for (StaticFurnitureSeatEntity seat : seats) {
			if (seat != occupiedSeat) {
				seat.discardAsDuplicate();
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
		StaticFurnitureSeatEntity.removeAt(level, pos);
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
	}

	@Override
	protected void createBlockStateDefinition(
		StateDefinition.Builder<Block, BlockState> builder
	) {
		super.createBlockStateDefinition(builder);
		builder.add(OCCUPIED);
	}
}
