package net.beforetheblight.entity;

import java.util.ArrayList;
import java.util.List;

import net.beforetheblight.block.RockingChairBlock;
import net.beforetheblight.registry.ModFurniture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
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
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A non-pickable, non-persistent vehicle used only while a player occupies a
 * rocking chair.
 *
 * <p>The entity type remains serializable because vanilla refuses to mount a
 * player onto a {@code noSave} entity type. {@link #shouldBeSaved()} prevents
 * empty helpers from entering chunk saves. Vanilla may still include an
 * occupied root vehicle in player data during logout; the removal callback
 * releases the block first, so a reloaded helper immediately revalidates,
 * ejects safely, and discards itself instead of locking the chair.</p>
 */
public final class RockingChairSeatEntity extends Entity {
	private static final double SEARCH_INFLATION = 0.25D;
	/**
	 * The woven seat ends at 8/16 blocks. This tiny allowance keeps the
	 * passenger attachment just above the visible weave instead of burying it
	 * in the model.
	 */
	public static final double PASSENGER_ATTACHMENT_HEIGHT = 0.515625D;
	/** Existing occupied-chair renderer pivot; retained to preserve its motion. */
	public static final double ROCKING_PIVOT_HEIGHT = 0.125D;
	public static final float ROCKING_SPEED = 0.14F;
	public static final float MAX_ROCKING_ANGLE = 3.5F;

	private static final double ROCKING_RADIUS =
		PASSENGER_ATTACHMENT_HEIGHT - ROCKING_PIVOT_HEIGHT;

	private boolean preserveOccupiedStateOnRemoval;

	public RockingChairSeatEntity(EntityType<? extends RockingChairSeatEntity> type, Level level) {
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

	public Direction chairFacing() {
		BlockState state = this.level().getBlockState(this.anchorPos());
		return state.getBlock() instanceof RockingChairBlock
			? state.getValue(RockingChairBlock.FACING)
			: Direction.fromYRot(this.getYRot());
	}

	public float rockingAngle(float partialTick) {
		return Mth.sin((this.tickCount + partialTick) * ROCKING_SPEED) * MAX_ROCKING_ANGLE;
	}

	/**
	 * World-space location of the rider's vehicle attachment (the seated
	 * pelvis), rotated around exactly the same rocker pivot as the visible
	 * chair model.
	 */
	public Vec3 riderAttachmentPoint(float angleDegrees) {
		return this.position().add(this.riderAttachmentOffset(angleDegrees));
	}

	private Vec3 riderAttachmentOffset(float angleDegrees) {
		double angleRadians = angleDegrees * Mth.DEG_TO_RAD;
		double phaseOffset = Math.sin(angleRadians) * ROCKING_RADIUS;
		double verticalOffset =
			ROCKING_PIVOT_HEIGHT + Math.cos(angleRadians) * ROCKING_RADIUS;
		/*
		 * Preserve the occupied chair renderer's established per-facing phase:
		 * north/south use opposite-facing travel while east/west use
		 * same-facing travel. The phase has no gameplay meaning, but using the
		 * exact renderer convention prevents the rider and chair separating.
		 */
		Direction travelDirection = switch (this.chairFacing()) {
			case NORTH -> Direction.SOUTH;
			case SOUTH -> Direction.NORTH;
			case EAST -> Direction.EAST;
			case WEST -> Direction.WEST;
			default -> Direction.NORTH;
		};
		return new Vec3(
			travelDirection.getStepX() * phaseOffset,
			verticalOffset,
			travelDirection.getStepZ() * phaseOffset
		);
	}

	@Override
	protected Vec3 getPassengerAttachmentPoint(
		Entity passenger,
		EntityDimensions dimensions,
		float scale
	) {
		return this.riderAttachmentOffset(this.rockingAngle(0.0F)).scale(scale);
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
		boolean validChair = state.getBlock() instanceof RockingChairBlock
			&& state.getValue(RockingChairBlock.OCCUPIED);
		boolean validClearance =
			RockingChairBlock.hasSeatingClearance(this.level(), anchor);
		boolean validPassenger = this.getPassengers().size() == 1
			&& this.getFirstPassenger() instanceof Player passenger
			&& !passenger.isRemoved();
		if (!validChair || !validClearance || !validPassenger) {
			this.releaseChair();
		}
	}

	@Override
	protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
		if (!this.hasPassenger(passenger)) {
			return;
		}

		/*
		 * Vanilla subtracts passenger.getVehicleAttachmentPoint(this) from
		 * getPassengerRidingPosition(passenger). Keeping the animated seat
		 * contact in getPassengerAttachmentPoint preserves that full API
		 * contract and prevents the player's 0.6-block vehicle attachment
		 * from becoming an equal-sized hover.
		 */
		super.positionRider(passenger, moveFunction);
	}

	@Override
	public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
		BlockPos anchor = this.anchorPos();
		Direction facing = this.chairFacing();
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

	private void releaseChair() {
		if (this.level() instanceof ServerLevel serverLevel) {
			BlockPos anchor = this.anchorPos();
			BlockState state = serverLevel.getBlockState(anchor);
			if (state.getBlock() instanceof RockingChairBlock
				&& state.getValue(RockingChairBlock.OCCUPIED)) {
				BlockState released = state.setValue(
					RockingChairBlock.OCCUPIED,
					false
				);
				if (serverLevel.setBlock(
					anchor,
					released,
					RockingChairBlock.UPDATE_ALL
				)) {
					serverLevel.gameEvent(
						GameEvent.BLOCK_DEACTIVATE,
						anchor,
						GameEvent.Context.of(released)
					);
				}
			}
		}

		this.ejectPassengers();
		this.discard();
	}

	@Override
	public void onRemoval(RemovalReason reason) {
		if (!this.preserveOccupiedStateOnRemoval
			&& reason != RemovalReason.UNLOADED_TO_CHUNK) {
			this.releaseOccupiedStateOnly();
		}
		super.onRemoval(reason);
	}

	/**
	 * Removes a noncanonical helper without changing the chair state owned by
	 * the deterministic canonical helper. Any displaced rider is dismounted
	 * normally before the duplicate entity is discarded.
	 */
	public void discardAsNoncanonical() {
		this.preserveOccupiedStateOnRemoval = true;
		this.ejectPassengers();
		this.discard();
	}

	private void releaseOccupiedStateOnly() {
		if (!(this.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		BlockPos anchor = this.anchorPos();
		BlockState state = serverLevel.getBlockState(anchor);
		if (state.getBlock() instanceof RockingChairBlock
			&& state.getValue(RockingChairBlock.OCCUPIED)) {
			BlockState released = state.setValue(
				RockingChairBlock.OCCUPIED,
				false
			);
			if (serverLevel.setBlock(
				anchor,
				released,
				RockingChairBlock.UPDATE_ALL
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

	public static List<RockingChairSeatEntity> findAt(ServerLevel level, BlockPos pos) {
		return level.getEntities(
			ModFurniture.ROCKING_CHAIR_SEAT,
			new AABB(pos).inflate(SEARCH_INFLATION),
			seat -> seat.anchorPos().equals(pos)
		);
	}

	public static void removeAt(ServerLevel level, BlockPos pos) {
		for (RockingChairSeatEntity seat : findAt(level, pos)) {
			seat.ejectPassengers();
			seat.discard();
		}
	}
}
