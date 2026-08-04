package net.beforetheblight.gametest;

import net.beforetheblight.block.BoardShutterBlock;
import net.beforetheblight.block.OpeningTrimBlock;
import net.beforetheblight.block.OperableSashWindowBlock;
import net.beforetheblight.block.PairedBoardShutterBlock;
import net.beforetheblight.block.ShutteredSashWindowBlock;
import net.beforetheblight.block.SmallSashWindowBlock;
import net.beforetheblight.registry.ModDoorWindowBlocks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class DoorWindowGameTests {
	private static final BlockPos TEST_POS = new BlockPos(2, 2, 2);

	@GameTest(maxTicks = 20)
	public void openingItemsUseTheCorrectRuntimePlacementTypes(GameTestHelper helper) {
		helper.assertTrue(
			ModDoorWindowBlocks.ROUGH_CHESTNUT_BOARD_DOOR.asItem()
				instanceof DoubleHighBlockItem,
			"rough board door must use a DoubleHighBlockItem"
		);
		helper.assertTrue(
			ModDoorWindowBlocks.PEGGED_SPRINGHOUSE_DOOR.asItem()
				instanceof DoubleHighBlockItem,
			"springhouse door must use a DoubleHighBlockItem"
		);
		helper.assertFalse(
			ModDoorWindowBlocks.CRIB_BOARD_HATCH.asItem()
				instanceof DoubleHighBlockItem,
			"one-block crib hatch must not use a DoubleHighBlockItem"
		);
		helper.assertFalse(
			ModDoorWindowBlocks.CELLAR_HATCH.asItem()
				instanceof DoubleHighBlockItem,
			"one-block cellar hatch must not use a DoubleHighBlockItem"
		);
		for (var item : ModDoorWindowBlocks.ALL_ITEMS) {
			helper.assertFalse(item == Items.AIR, "opening block cached an AIR item");
		}
		helper.assertValueEqual(
			ModDoorWindowBlocks.ALL_ITEMS.size(),
			25,
			"obtainable opening item count"
		);
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void sashAndShutterCollisionFollowFacingHingeAndOpenState(GameTestHelper helper) {
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			BlockState sash = ModDoorWindowBlocks.SMALL_SASH_WINDOW
				.defaultBlockState()
				.setValue(SmallSashWindowBlock.FACING, facing);
			helper.setBlock(TEST_POS, sash);
			AABB sashBounds = sash
				.getCollisionShape(helper.getLevel(), helper.absolutePos(TEST_POS))
				.bounds();
			helper.assertValueEqual(
				sashBounds,
				facing.getAxis() == Direction.Axis.Z
					? new AABB(0.0, 0.0, 7.0 / 16.0, 1.0, 1.0, 9.0 / 16.0)
					: new AABB(7.0 / 16.0, 0.0, 0.0, 9.0 / 16.0, 1.0, 1.0),
				facing.getName() + " sash collision"
			);

			for (DoorHingeSide hinge : DoorHingeSide.values()) {
				BlockState closed = ModDoorWindowBlocks.BOARD_SHUTTER
					.defaultBlockState()
					.setValue(BoardShutterBlock.FACING, facing)
					.setValue(BoardShutterBlock.HINGE, hinge)
					.setValue(BoardShutterBlock.OPEN, false);
				BlockState opened = closed.setValue(BoardShutterBlock.OPEN, true);
				AABB closedBounds = closed
					.getCollisionShape(helper.getLevel(), helper.absolutePos(TEST_POS))
					.bounds();
				AABB openBounds = opened
					.getCollisionShape(helper.getLevel(), helper.absolutePos(TEST_POS))
					.bounds();
				helper.assertFalse(
					closedBounds.equals(openBounds),
					facing.getName() + " " + hinge.getSerializedName() + " shutter did not fold"
				);
				helper.assertValueEqual(
					openBounds,
					expectedFoldedBounds(facing, hinge),
					facing.getName() + " " + hinge.getSerializedName() + " folded collision"
				);
			}
		}

		BlockState waterlogged = ModDoorWindowBlocks.BOARD_SHUTTER
			.defaultBlockState()
			.setValue(BoardShutterBlock.WATERLOGGED, true);
		helper.assertTrue(
			waterlogged.getFluidState().is(Fluids.WATER),
			"waterlogged shutter must retain source water"
		);
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void manualShutterAndDoorTransformsPreserveTheirHandedness(GameTestHelper helper) {
		BlockState shutter = ModDoorWindowBlocks.BOARD_SHUTTER
			.defaultBlockState()
			.setValue(BoardShutterBlock.FACING, Direction.NORTH)
			.setValue(BoardShutterBlock.HINGE, DoorHingeSide.LEFT)
			.setValue(BoardShutterBlock.OPEN, false);
		helper.setBlock(TEST_POS, shutter);
		helper.useBlock(TEST_POS);
		helper.assertTrue(
			helper.getBlockState(TEST_POS).getValue(BoardShutterBlock.OPEN),
			"manual use did not open the shutter"
		);
		helper.useBlock(TEST_POS);
		helper.assertFalse(
			helper.getBlockState(TEST_POS).getValue(BoardShutterBlock.OPEN),
			"second manual use did not close the shutter"
		);

		BlockState mirrored = shutter.mirror(Mirror.FRONT_BACK);
		helper.assertValueEqual(
			mirrored.getValue(BoardShutterBlock.HINGE),
			DoorHingeSide.RIGHT,
			"mirroring must swap shutter handedness"
		);
		BlockState rotated = shutter.rotate(Rotation.CLOCKWISE_90);
		helper.assertValueEqual(
			rotated.getValue(BoardShutterBlock.FACING),
			Direction.EAST,
			"rotation must rotate shutter facing"
		);

		for (Direction facing : Direction.Plane.HORIZONTAL) {
			for (DoorHingeSide hinge : DoorHingeSide.values()) {
				BlockState closedDoor = ModDoorWindowBlocks.ROUGH_CHESTNUT_BOARD_DOOR
					.defaultBlockState()
					.setValue(DoorBlock.FACING, facing)
					.setValue(DoorBlock.HINGE, hinge)
					.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER)
					.setValue(DoorBlock.OPEN, false);
				BlockState openDoor = closedDoor.setValue(DoorBlock.OPEN, true);
				helper.assertFalse(
					closedDoor
						.getCollisionShape(helper.getLevel(), helper.absolutePos(TEST_POS))
						.bounds()
						.equals(
							openDoor
								.getCollisionShape(
									helper.getLevel(),
									helper.absolutePos(TEST_POS)
								)
								.bounds()
						),
					facing.getName() + " " + hinge.getSerializedName() + " door collision did not open"
				);
			}
		}
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void polishedOpeningsToggleAndKeepPurposeBuiltCollision(GameTestHelper helper) {
		BlockState operableClosed = ModDoorWindowBlocks.OPERABLE_SASH_WINDOW
			.defaultBlockState()
			.setValue(OperableSashWindowBlock.FACING, Direction.NORTH)
			.setValue(OperableSashWindowBlock.OPEN, false);
		BlockState operableOpen = operableClosed.setValue(
			OperableSashWindowBlock.OPEN,
			true
		);
		helper.assertValueEqual(
			operableClosed
				.getCollisionShape(helper.getLevel(), helper.absolutePos(TEST_POS))
				.toAabbs()
				.size(),
			1,
			"closed sash collision pieces"
		);
		VoxelShape openSashShape = operableOpen.getCollisionShape(
			helper.getLevel(),
			helper.absolutePos(TEST_POS)
		);
		helper.assertFalse(
			Shapes.joinIsNotEmpty(
				openSashShape,
				Shapes.create(
					new AABB(
						6.0 / 16.0,
						2.0 / 16.0,
						7.0 / 16.0,
						10.0 / 16.0,
						6.0 / 16.0,
						9.0 / 16.0
					)
				),
				BooleanOp.AND
			),
			"open sash must clear the lower central opening"
		);
		helper.assertTrue(
			Shapes.joinIsNotEmpty(
				openSashShape,
				Shapes.create(
					new AABB(
						6.0 / 16.0,
						9.0 / 16.0,
						7.0 / 16.0,
						10.0 / 16.0,
						12.0 / 16.0,
						9.0 / 16.0
					)
				),
				BooleanOp.AND
			),
			"open sash must retain its raised upper lights"
		);

		BlockState broken = ModDoorWindowBlocks.BROKEN_PANE_SASH_WINDOW
			.defaultBlockState();
		helper.assertValueEqual(
			broken
				.getCollisionShape(helper.getLevel(), helper.absolutePos(TEST_POS))
				.toAabbs()
				.size(),
			4,
			"broken sash must have no invisible central glass collision"
		);

		BlockState pairedClosed = ModDoorWindowBlocks.PAIRED_BOARD_SHUTTERS
			.defaultBlockState()
			.setValue(PairedBoardShutterBlock.FACING, Direction.NORTH)
			.setValue(PairedBoardShutterBlock.OPEN, false);
		BlockState pairedOpen = pairedClosed.setValue(
			PairedBoardShutterBlock.OPEN,
			true
		);
		helper.assertValueEqual(
			pairedOpen
				.getCollisionShape(helper.getLevel(), helper.absolutePos(TEST_POS))
				.toAabbs()
				.size(),
			2,
			"paired shutters must fold to both jambs"
		);
		helper.assertFalse(
			pairedClosed
				.getCollisionShape(helper.getLevel(), helper.absolutePos(TEST_POS))
				.bounds()
				.equals(
					pairedOpen
						.getCollisionShape(helper.getLevel(), helper.absolutePos(TEST_POS))
						.bounds()
				),
			"paired shutter collision did not open"
		);

		BlockState shutteredOpen = ModDoorWindowBlocks.SHUTTERED_SASH_WINDOW
			.defaultBlockState()
			.setValue(ShutteredSashWindowBlock.FACING, Direction.NORTH)
			.setValue(ShutteredSashWindowBlock.OPEN, true);
		helper.assertValueEqual(
			shutteredOpen
				.getCollisionShape(helper.getLevel(), helper.absolutePos(TEST_POS))
				.toAabbs()
				.size(),
			4,
			"shuttered window must retain sash and both folded leaves"
		);

		for (
			var block : new net.minecraft.world.level.block.Block[] {
				ModDoorWindowBlocks.OPERABLE_SASH_WINDOW,
				ModDoorWindowBlocks.PAIRED_BOARD_SHUTTERS,
				ModDoorWindowBlocks.SHUTTERED_SASH_WINDOW,
			}
		) {
			helper.setBlock(TEST_POS, block.defaultBlockState());
			helper.useBlock(TEST_POS);
			helper.assertTrue(
				helper.getBlockState(TEST_POS).getValue(
					net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN
				),
				"manual use did not open " + block
			);
		}

		BlockState jambX = ModDoorWindowBlocks.ROUGH_CHESTNUT_DOOR_JAMB
			.defaultBlockState()
			.setValue(OpeningTrimBlock.AXIS, Direction.Axis.X);
		BlockState jambY = jambX.setValue(OpeningTrimBlock.AXIS, Direction.Axis.Y);
		helper.assertFalse(
			jambX
				.getCollisionShape(helper.getLevel(), helper.absolutePos(TEST_POS))
				.bounds()
				.equals(
					jambY
						.getCollisionShape(helper.getLevel(), helper.absolutePos(TEST_POS))
						.bounds()
				),
			"jamb collision did not follow its axis"
		);
		helper.succeed();
	}

	private static AABB expectedFoldedBounds(
		Direction facing,
		DoorHingeSide hinge
	) {
		Direction foldedSide = hinge == DoorHingeSide.LEFT
			? facing.getCounterClockWise()
			: facing.getClockWise();
		return switch (foldedSide) {
			case WEST -> new AABB(0.0, 0.0, 0.0, 2.0 / 16.0, 1.0, 1.0);
			case EAST -> new AABB(14.0 / 16.0, 0.0, 0.0, 1.0, 1.0, 1.0);
			case NORTH -> new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 2.0 / 16.0);
			case SOUTH -> new AABB(0.0, 0.0, 14.0 / 16.0, 1.0, 1.0, 1.0);
			default -> throw new IllegalStateException("A shutter cannot fold vertically");
		};
	}
}
