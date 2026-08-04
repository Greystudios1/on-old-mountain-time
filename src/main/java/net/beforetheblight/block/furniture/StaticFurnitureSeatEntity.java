package net.beforetheblight.block.furniture;

import java.util.ArrayList;
import java.util.List;

import net.beforetheblight.registry.ModFurnitureBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Invisible vehicle used while a player occupies static furniture.
 *
 * <p>The entity is registered as serializable because vanilla will not mount a
 * player on a {@code noSave} entity type. {@link #shouldBeSaved()} still keeps
 * the transient helper out of chunk data. The furniture block owns the
 * persistent occupied flag and scheduled recovery repairs it when this helper
 * disappears during an interrupted mount or unload.</p>
 */
public final class StaticFurnitureSeatEntity extends Entity {
	private static final double SEARCH_INFLATION = 0.25D;
	private boolean preserveOccupiedStateOnRemoval;

	public StaticFurnitureSeatEntity(
		EntityType<? extends StaticFurnitureSeatEntity> type,
		Level level
	) {
		super(type, level);
		this.noPhysics = true;
		this.setNoGravity(true);
	}

	public void anchorTo(BlockPos pos, Direction facing) {
		this.snapTo(
			pos.getX() + 0.5D,
			pos.getY(),
			pos.getZ() + 0.5D,
			facing.toYRot(),
			0.0F
		);
		this.setDeltaMovement(Vec3.ZERO);
	}

	public BlockPos anchorPos() {
		return BlockPos.containing(this.getX(), this.getY(), this.getZ());
	}

	private Direction furnitureFacing() {
		BlockState state = this.level().getBlockState(this.anchorPos());
		return state.getBlock() instanceof SeatingFurnitureBlock
			? state.getValue(SeatingFurnitureBlock.FACING)
			: Direction.fromYRot(this.getYRot());
	}

	private double furnitureSeatHeight() {
		BlockState state = this.level().getBlockState(this.anchorPos());
		return state.getBlock() instanceof SeatingFurnitureBlock furniture
			? furniture.seatHeight()
			: 0.4375D;
	}

	@Override
	protected Vec3 getPassengerAttachmentPoint(
		Entity passenger,
		EntityDimensions dimensions,
		float scale
	) {
		return new Vec3(0.0D, this.furnitureSeatHeight() * scale, 0.0D);
	}

	@Override
	public void tick() {
		super.tick();
		this.setDeltaMovement(Vec3.ZERO);
		if (this.level().isClientSide()) {
			return;
		}

		BlockPos anchor = this.anchorPos();
		BlockState state = this.level().getBlockState(anchor);
		boolean validFurniture = state.getBlock() instanceof SeatingFurnitureBlock;
		boolean validOccupiedState = validFurniture
			&& state.getValue(SeatingFurnitureBlock.OCCUPIED);
		boolean validClearance =
			SeatingFurnitureBlock.hasSeatingClearance(this.level(), anchor);
		boolean validPassenger = this.getPassengers().size() == 1
			&& this.getFirstPassenger() instanceof Player passenger
			&& !passenger.isRemoved();
		if (!validFurniture || !validOccupiedState || !validClearance || !validPassenger) {
			this.releaseOccupiedState();
			this.ejectPassengers();
			this.discard();
		}
	}

	@Override
	protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
		if (this.hasPassenger(passenger)) {
			super.positionRider(passenger, moveFunction);
		}
	}

	@Override
	public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
		BlockPos anchor = this.anchorPos();
		Direction facing = this.furnitureFacing();
		List<Direction> directions = new ArrayList<>(4);
		directions.add(facing);
		directions.add(facing.getClockWise());
		directions.add(facing.getCounterClockWise());
		directions.add(facing.getOpposite());

		for (Direction direction : directions) {
			BlockPos beside = anchor.relative(direction);
			Vec3 location = this.findSafeDismount(passenger, beside);
			if (location != null) {
				return location;
			}

			location = this.findSafeDismount(passenger, beside.below());
			if (location != null) {
				return location;
			}
		}
		return Vec3.atBottomCenterOf(anchor.above(2));
	}

	private Vec3 findSafeDismount(LivingEntity passenger, BlockPos pos) {
		double floorHeight = this.level().getBlockFloorHeight(pos);
		if (!DismountHelper.isBlockFloorValid(floorHeight)) {
			return null;
		}

		Vec3 target = Vec3.upFromBottomCenterOf(pos, floorHeight);
		for (Pose pose : passenger.getDismountPoses()) {
			if (DismountHelper.canDismountTo(this.level(), target, passenger, pose)) {
				passenger.setPose(pose);
				return target;
			}
		}
		return null;
	}

	public void discardAsDuplicate() {
		this.preserveOccupiedStateOnRemoval = true;
		this.ejectPassengers();
		this.discard();
	}

	@Override
	public void onRemoval(RemovalReason reason) {
		if (!this.preserveOccupiedStateOnRemoval
			&& reason != RemovalReason.UNLOADED_TO_CHUNK) {
			this.releaseOccupiedState();
		}
		super.onRemoval(reason);
	}

	private void releaseOccupiedState() {
		if (!(this.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		BlockPos anchor = this.anchorPos();
		BlockState state = serverLevel.getBlockState(anchor);
		if (state.getBlock() instanceof SeatingFurnitureBlock
			&& state.getValue(SeatingFurnitureBlock.OCCUPIED)) {
			BlockState released = state.setValue(
				SeatingFurnitureBlock.OCCUPIED,
				false
			);
			if (serverLevel.setBlock(
				anchor,
				released,
				SeatingFurnitureBlock.UPDATE_ALL
			)) {
				serverLevel.gameEvent(
					GameEvent.BLOCK_DEACTIVATE,
					anchor,
					GameEvent.Context.of(released)
				);
			}
		}
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public PushReaction getPistonPushReaction() {
		return PushReaction.IGNORE;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder entityData) {
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}

	public static List<StaticFurnitureSeatEntity> findAt(
		ServerLevel level,
		BlockPos pos
	) {
		return level.getEntities(
			ModFurnitureBlocks.STATIC_SEAT,
			new AABB(pos).inflate(SEARCH_INFLATION),
			seat -> seat.anchorPos().equals(pos)
		);
	}

	public static void removeAt(ServerLevel level, BlockPos pos) {
		for (StaticFurnitureSeatEntity seat : findAt(level, pos)) {
			seat.ejectPassengers();
			seat.discard();
		}
	}
}
