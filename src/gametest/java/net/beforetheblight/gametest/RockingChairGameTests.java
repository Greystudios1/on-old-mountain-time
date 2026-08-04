package net.beforetheblight.gametest;

import net.beforetheblight.block.RockingChairBlock;
import net.beforetheblight.entity.RockingChairSeatEntity;
import net.beforetheblight.registry.ModBlocks;
import net.beforetheblight.registry.ModFurniture;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Focused lifecycle tests for the chair and its transient seat helper. */
public final class RockingChairGameTests {
	private static final BlockPos TARGET = new BlockPos(3, 2, 3);

	@GameTest(maxTicks = 40)
	public void facingAndOccupiedStatesRoundTrip(GameTestHelper helper) {
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			for (boolean occupied : new boolean[]{false, true}) {
				BlockState original = ModBlocks.ROCKING_CHAIR.defaultBlockState()
					.setValue(RockingChairBlock.FACING, facing)
					.setValue(RockingChairBlock.OCCUPIED, occupied);
				Tag encoded = BlockState.CODEC
					.encodeStart(NbtOps.INSTANCE, original)
					.getOrThrow(IllegalStateException::new);
				BlockState decoded = BlockState.CODEC
					.parse(NbtOps.INSTANCE, encoded)
					.getOrThrow(IllegalStateException::new);
				helper.assertValueEqual(
					decoded,
					original,
					"chair state codec round trip for " + facing + ", occupied=" + occupied
				);
			}
		}
		helper.succeed();
	}

	@GameTest(maxTicks = 80)
	public void onePlayerSitsSecondPlayerCannotStealAndDismountReleases(GameTestHelper helper) {
		helper.setBlock(TARGET, ModBlocks.ROCKING_CHAIR.defaultBlockState());
		ServerPlayer first = player(helper);
		ServerPlayer second = player(helper);

		first.setItemInHand(
			InteractionHand.MAIN_HAND,
			new ItemStack(Items.COBBLESTONE)
		);
		InteractionResult heldItemResult = useChair(helper, first);
		helper.assertTrue(
			heldItemResult instanceof InteractionResult.Success,
			"held block item did not receive the chair's pass-through interaction"
		);
		helper.assertBlockPresent(Blocks.COBBLESTONE, TARGET.above());
		helper.assertTrue(
			!first.isPassenger()
				&& RockingChairSeatEntity.findAt(
					helper.getLevel(),
					helper.absolutePos(TARGET)
				).isEmpty(),
			"held-item use seated the player or created a chair helper"
		);
		helper.assertBlockProperty(TARGET, RockingChairBlock.OCCUPIED, false);
		helper.setBlock(TARGET.above(), Blocks.AIR);
		first.setItemInHand(
			InteractionHand.MAIN_HAND,
			new ItemStack(Items.STICK, 7)
		);

		InteractionResult firstResult = useChair(helper, first);
		helper.assertTrue(
			firstResult instanceof InteractionResult.Success,
			"ordinary held item prevented the first chair interaction"
		);
		helper.assertTrue(
			first.getVehicle() instanceof RockingChairSeatEntity,
			"first player was not mounted while holding an ordinary item"
		);
		helper.assertTrue(
			first.getMainHandItem().is(Items.STICK)
				&& first.getMainHandItem().getCount() == 7,
			"sitting consumed or replaced the ordinary held item"
		);
		helper.assertValueEqual(
			RockingChairSeatEntity.findAt(helper.getLevel(), helper.absolutePos(TARGET)).size(),
			1,
			"chair helper count after first mount"
		);
		helper.assertBlockProperty(TARGET, RockingChairBlock.OCCUPIED, true);

		RockingChairSeatEntity occupiedSeat =
			(RockingChairSeatEntity)first.getVehicle();
		RockingChairSeatEntity conflictingSeat =
			ModFurniture.ROCKING_CHAIR_SEAT.create(
				helper.getLevel(),
				EntitySpawnReason.TRIGGERED
			);
		helper.assertTrue(
			conflictingSeat != null,
			"could not create conflicting occupied chair helper"
		);
		conflictingSeat.anchorTo(
			helper.absolutePos(TARGET),
			helper.getBlockState(TARGET).getValue(RockingChairBlock.FACING)
		);
		helper.assertTrue(
			helper.getLevel().addFreshEntity(conflictingSeat),
			"could not add conflicting occupied chair helper"
		);
		helper.assertTrue(
			second.startRiding(conflictingSeat),
			"could not simulate a second occupied helper"
		);

		RockingChairSeatEntity emptyDuplicate =
			ModFurniture.ROCKING_CHAIR_SEAT.create(
				helper.getLevel(),
				EntitySpawnReason.TRIGGERED
			);
		helper.assertTrue(
			emptyDuplicate != null,
			"could not create empty duplicate chair helper"
		);
		emptyDuplicate.anchorTo(
			helper.absolutePos(TARGET),
			helper.getBlockState(TARGET).getValue(RockingChairBlock.FACING)
		);
		helper.assertTrue(
			helper.getLevel().addFreshEntity(emptyDuplicate),
			"could not add empty duplicate chair helper"
		);
		helper.assertValueEqual(
			RockingChairSeatEntity.findAt(
				helper.getLevel(),
				helper.absolutePos(TARGET)
			).size(),
			3,
			"chair helper count before occupied and empty duplicate cleanup"
		);

		useChair(helper, second);
		helper.assertTrue(
			!second.isPassenger(),
			"rider on a noncanonical occupied helper was not safely dismounted"
		);
		helper.assertTrue(
			first.getVehicle() == occupiedSeat && occupiedSeat.hasPassenger(first),
			"duplicate reconciliation ejected the canonical chair rider"
		);
		helper.assertTrue(
			conflictingSeat.isRemoved(),
			"noncanonical occupied helper survived reconciliation"
		);
		helper.assertTrue(
			emptyDuplicate.isRemoved(),
			"empty duplicate helper survived reconciliation"
		);
		helper.assertBlockProperty(TARGET, RockingChairBlock.OCCUPIED, true);
		helper.assertValueEqual(
			RockingChairSeatEntity.findAt(helper.getLevel(), helper.absolutePos(TARGET)).size(),
			1,
			"chair helper count after denied second mount"
		);

		first.stopRiding();
		helper.runAfterDelay(2, () -> {
			helper.assertBlockProperty(TARGET, RockingChairBlock.OCCUPIED, false);
			helper.assertTrue(
				RockingChairSeatEntity.findAt(
					helper.getLevel(),
					helper.absolutePos(TARGET)
				).isEmpty(),
				"transient chair helper survived dismount"
			);

			InteractionResult reuseResult = useChair(helper, second);
			helper.assertTrue(
				reuseResult instanceof InteractionResult.Success && second.isPassenger(),
				"chair could not be reused after dismount cleanup"
			);
			second.stopRiding();
			removePlayer(helper, first);
			removePlayer(helper, second);
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 80)
	public void riderAttachmentAndRockingAlignForEveryFacing(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		BlockPos absolutePos = helper.absolutePos(TARGET);

		for (Direction facing : Direction.Plane.HORIZONTAL) {
			helper.setBlock(
				TARGET,
				ModBlocks.ROCKING_CHAIR.defaultBlockState()
					.setValue(RockingChairBlock.FACING, facing)
					.setValue(RockingChairBlock.OCCUPIED, true)
			);
			RockingChairSeatEntity seat = ModFurniture.ROCKING_CHAIR_SEAT.create(
				helper.getLevel(),
				EntitySpawnReason.TRIGGERED
			);
			helper.assertTrue(seat != null, "could not create chair helper for " + facing);
			seat.anchorTo(absolutePos, facing);
			helper.assertTrue(
				helper.getLevel().addFreshEntity(seat),
				"could not add chair helper for " + facing
			);
			helper.assertTrue(
				player.startRiding(seat),
				"player could not mount chair facing " + facing
			);

			/*
			 * Put the animation at approximately its positive extreme, then
			 * ask the ordinary passenger pipeline to position the player.
			 */
			seat.tickCount = Math.round(
				(float)(Math.PI * 0.5D) / RockingChairSeatEntity.ROCKING_SPEED
			);
			float angle = seat.rockingAngle(0.0F);
			Vec3 expectedAttachment = seat.riderAttachmentPoint(angle);
			assertVecClose(
				helper,
				seat.getPassengerRidingPosition(player),
				expectedAttachment,
				"public passenger riding position was stale for " + facing
			);
			seat.positionRider(player);
			Vec3 actualAttachment = player.position()
				.add(player.getVehicleAttachmentPoint(seat));
			assertVecClose(
				helper,
				actualAttachment,
				expectedAttachment,
				"player attachment did not follow the chair for " + facing
			);

			Vec3 restAttachment = seat.riderAttachmentPoint(0.0F);
			Vec3 rockingOffset = expectedAttachment.subtract(restAttachment);
			Direction travelDirection = switch (facing) {
				case NORTH -> Direction.SOUTH;
				case SOUTH -> Direction.NORTH;
				case EAST -> Direction.EAST;
				case WEST -> Direction.WEST;
				default -> Direction.NORTH;
			};
			double phaseTravel =
				rockingOffset.x * travelDirection.getStepX()
					+ rockingOffset.z * travelDirection.getStepZ();
			double sidewaysTravel =
				rockingOffset.x * facing.getClockWise().getStepX()
					+ rockingOffset.z * facing.getClockWise().getStepZ();
			helper.assertTrue(
				phaseTravel > 0.02D,
				"chair facing " + facing + " did not follow its render phase"
			);
			helper.assertTrue(
				Math.abs(sidewaysTravel) < 1.0E-6D,
				"chair facing " + facing + " drifted sideways while rocking"
			);
			helper.assertTrue(
				rockingOffset.y < 0.0D,
				"chair facing " + facing + " did not follow a circular rocker arc"
			);
			helper.assertTrue(
				Math.abs(
					restAttachment.y
						- absolutePos.getY()
						- RockingChairSeatEntity.PASSENGER_ATTACHMENT_HEIGHT
				) < 1.0E-6D,
				"resting rider attachment is not on the woven seat for " + facing
			);

			player.stopRiding();
			seat.discard();
			helper.assertTrue(
				!player.isPassenger(),
				"player remained mounted after facing check for " + facing
			);
		}

		removePlayer(helper, player);
		helper.succeed();
	}

	@GameTest(maxTicks = 60)
	public void breakingChairEjectsRiderAndRemovesHelper(GameTestHelper helper) {
		helper.setBlock(TARGET, ModBlocks.ROCKING_CHAIR.defaultBlockState());
		ServerPlayer player = player(helper);
		BlockPos absolutePos = helper.absolutePos(TARGET);

		helper.setBlock(TARGET.above(), Blocks.STONE);
		useChair(helper, player);
		helper.assertTrue(
			!player.isPassenger(),
			"low ceiling allowed the player to sit"
		);
		helper.assertTrue(
			RockingChairSeatEntity.findAt(helper.getLevel(), absolutePos).isEmpty(),
			"low-ceiling rejection created a chair helper"
		);
		helper.assertBlockProperty(TARGET, RockingChairBlock.OCCUPIED, false);
		helper.setBlock(TARGET.above(), Blocks.AIR);

		useChair(helper, player);
		helper.assertTrue(player.isPassenger(), "player was not seated before clearance test");
		helper.setBlock(TARGET.above(2), Blocks.STONE);
		helper.runAfterDelay(2, () -> {
			helper.assertTrue(
				!player.isPassenger(),
				"chair did not safely release the rider after clearance was blocked"
			);
			helper.assertTrue(
				RockingChairSeatEntity.findAt(helper.getLevel(), absolutePos).isEmpty(),
				"blocked-clearance release left a chair helper"
			);
			helper.assertBlockProperty(TARGET, RockingChairBlock.OCCUPIED, false);

			helper.setBlock(TARGET.above(2), Blocks.AIR);
			useChair(helper, player);
			helper.assertTrue(player.isPassenger(), "player was not seated before chair break");
			helper.getLevel().destroyBlock(absolutePos, true, player);
			helper.assertTrue(!player.isPassenger(), "breaking chair did not eject rider");
			helper.assertTrue(
				RockingChairSeatEntity.findAt(helper.getLevel(), absolutePos).isEmpty(),
				"breaking chair left a helper entity"
			);
			helper.assertTrue(helper.getBlockState(TARGET).isAir(), "chair remained after break");
			removePlayer(helper, player);
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 40)
	public void staleOccupiedStateRepairsWithoutASeat(GameTestHelper helper) {
		helper.setBlock(
			TARGET,
			ModBlocks.ROCKING_CHAIR.defaultBlockState()
				.setValue(RockingChairBlock.OCCUPIED, true)
		);
		helper.runAfterDelay(3, () -> {
			helper.assertBlockProperty(TARGET, RockingChairBlock.OCCUPIED, false);
			helper.assertTrue(
				RockingChairSeatEntity.findAt(
					helper.getLevel(),
					helper.absolutePos(TARGET)
				).isEmpty(),
				"stale-state repair created or retained a helper"
			);
			helper.succeed();
		});
	}

	@SuppressWarnings("removal")
	private static ServerPlayer player(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		BlockPos absolute = helper.absolutePos(TARGET);
		player.snapTo(
			absolute.getX() + 0.5D,
			absolute.getY() + 1.0D,
			absolute.getZ() - 1.0D,
			0.0F,
			0.0F
		);
		return player;
	}

	private static void removePlayer(GameTestHelper helper, ServerPlayer player) {
		helper.getLevel().getServer().getPlayerList().remove(player);
	}

	private static InteractionResult useChair(GameTestHelper helper, ServerPlayer player) {
		BlockPos absolutePos = helper.absolutePos(TARGET);
		BlockHitResult hit = new BlockHitResult(
			Vec3.atCenterOf(absolutePos),
			Direction.UP,
			absolutePos,
			false
		);
		return player.gameMode.useItemOn(
			player,
			helper.getLevel(),
			player.getItemInHand(InteractionHand.MAIN_HAND),
			InteractionHand.MAIN_HAND,
			hit
		);
	}

	private static void assertVecClose(
		GameTestHelper helper,
		Vec3 actual,
		Vec3 expected,
		String message
	) {
		helper.assertTrue(
			actual.distanceToSqr(expected) < 1.0E-10D,
			message + ": expected " + expected + ", got " + actual
		);
	}
}
