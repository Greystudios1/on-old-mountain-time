package net.beforetheblight.gametest;

import net.beforetheblight.block.springhouse.HollowedChestnutTroughBlock;
import net.beforetheblight.block.springhouse.HollowedChestnutTroughBlock.TroughContent;
import net.beforetheblight.block.springhouse.HollowedChestnutTroughBlock.TroughPart;
import net.beforetheblight.block.springhouse.HollowLimbSpoutBlock;
import net.beforetheblight.registry.ModSpringhouseBlocks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Runtime contracts for the isolated springhouse building-polish palette. */
public final class SpringhousePolishGameTests {
	private static final BlockPos TARGET = new BlockPos(3, 2, 3);

	@GameTest(maxTicks = 20)
	public void troughPublishesEveryPersistentShapeAndContentState(
		GameTestHelper helper
	) {
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			for (TroughPart part : TroughPart.values()) {
				for (TroughContent content : TroughContent.values()) {
					for (boolean waterlogged : new boolean[] {false, true}) {
						BlockState original = ModSpringhouseBlocks
							.HOLLOWED_CHESTNUT_TROUGH
							.defaultBlockState()
							.setValue(HollowedChestnutTroughBlock.FACING, facing)
							.setValue(HollowedChestnutTroughBlock.PART, part)
							.setValue(HollowedChestnutTroughBlock.CONTENT, content)
							.setValue(
								HollowedChestnutTroughBlock.WATERLOGGED,
								waterlogged
							);
						assertCodecRoundTrip(
							helper,
							original,
							"trough "
								+ facing
								+ "/"
								+ part
								+ "/"
								+ content
								+ "/waterlogged="
								+ waterlogged
						);

						helper.setBlock(TARGET, original);
						VoxelShape shape = original.getShape(
							helper.getLevel(),
							helper.absolutePos(TARGET)
						);
						helper.assertTrue(
							!shape.isEmpty() && shape.bounds().getYsize() < 1.0,
							"trough collision must follow its low timber rim"
						);
						helper.assertValueEqual(
							original.getCollisionShape(
								helper.getLevel(),
								helper.absolutePos(TARGET)
							),
							shape,
							"trough outline and collision must agree"
						);
						helper.assertValueEqual(
							original.getFluidState().is(Fluids.WATER),
							waterlogged || content == TroughContent.CLEAR,
							"trough water must be a vanilla FluidState"
						);
					}
				}
			}
		}
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void troughAndSpoutWaterBucketsAreLossless(GameTestHelper helper) {
		BlockPos absoluteTarget = helper.absolutePos(TARGET);
		HollowedChestnutTroughBlock trough =
			ModSpringhouseBlocks.HOLLOWED_CHESTNUT_TROUGH;
		BlockState siltyTrough = trough
			.defaultBlockState()
			.setValue(HollowedChestnutTroughBlock.CONTENT, TroughContent.SILTY);
		helper.setBlock(TARGET, siltyTrough);
		helper.assertTrue(
			trough.placeLiquid(
				helper.getLevel(),
				absoluteTarget,
				siltyTrough,
				Fluids.WATER.getSource(false)
			),
			"water bucket must fill a dry trough"
		);
		BlockState wetTrough = helper.getLevel().getBlockState(absoluteTarget);
		helper.assertTrue(
			wetTrough.getValue(HollowedChestnutTroughBlock.WATERLOGGED),
			"filled trough must store waterlogged=true"
		);
		helper.assertValueEqual(
			wetTrough.getValue(HollowedChestnutTroughBlock.CONTENT),
			TroughContent.SILTY,
			"real water must not erase a sediment deposit"
		);
		ItemStack troughBucket = trough.pickupBlock(
			null,
			helper.getLevel(),
			absoluteTarget,
			wetTrough
		);
		helper.assertTrue(
			troughBucket.is(Items.WATER_BUCKET),
			"draining a trough must return exactly one water bucket"
		);
		helper.assertTrue(
			!helper.getBlockState(TARGET).getFluidState().is(Fluids.WATER),
			"drained trough must no longer expose water"
		);

		BlockState legacyClear = trough
			.defaultBlockState()
			.setValue(HollowedChestnutTroughBlock.CONTENT, TroughContent.CLEAR);
		helper.setBlock(TARGET, legacyClear);
		helper.assertTrue(
			legacyClear.getFluidState().is(Fluids.WATER),
			"legacy content=clear must resolve to real water"
		);
		ItemStack legacyBucket = trough.pickupBlock(
			null,
			helper.getLevel(),
			absoluteTarget,
			legacyClear
		);
		helper.assertTrue(
			legacyBucket.is(Items.WATER_BUCKET),
			"legacy clear trough water must remain recoverable"
		);
			helper.assertValueEqual(
			helper
				.getBlockState(TARGET)
				.getValue(HollowedChestnutTroughBlock.CONTENT),
			TroughContent.EMPTY,
			"bucket pickup must normalize the legacy clear alias"
		);

		HollowLimbSpoutBlock spout = ModSpringhouseBlocks.HOLLOW_LIMB_SPOUT;
		BlockState drySpout = spout.defaultBlockState();
		helper.setBlock(TARGET, drySpout);
		helper.assertTrue(
			spout.placeLiquid(
				helper.getLevel(),
				absoluteTarget,
				drySpout,
				Fluids.WATER.getSource(false)
			),
			"water bucket must fill a dry spout"
		);
		BlockState wetSpout = helper.getLevel().getBlockState(absoluteTarget);
		helper.assertTrue(
			wetSpout.getValue(HollowLimbSpoutBlock.WATERLOGGED)
				&& wetSpout.getFluidState().is(Fluids.WATER),
			"filled spout must expose real vanilla water"
		);
		ItemStack spoutBucket = spout.pickupBlock(
			null,
			helper.getLevel(),
			absoluteTarget,
			wetSpout
		);
		helper.assertTrue(
			spoutBucket.is(Items.WATER_BUCKET),
			"draining a spout must return exactly one water bucket"
		);

		BlockState legacyFlowing = spout
			.defaultBlockState()
			.setValue(HollowLimbSpoutBlock.FLOWING, true);
		helper.assertTrue(
			legacyFlowing.getFluidState().is(Fluids.WATER),
			"legacy flowing spout must resolve to real water"
		);
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void troughConnectionsFollowCollinearNeighbours(GameTestHelper helper) {
		BlockPos center = new BlockPos(3, 2, 3);
		Direction[] placerFacings = {
			Direction.NORTH,
			Direction.EAST,
			Direction.SOUTH,
			Direction.WEST,
		};
		Direction[] expectedTroughFacings = {
			Direction.EAST,
			Direction.SOUTH,
			Direction.WEST,
			Direction.NORTH,
		};
		for (int index = 0; index < placerFacings.length; index++) {
			helper.assertValueEqual(
				HollowedChestnutTroughBlock.placementFacingFor(
					placerFacings[index]
				),
				expectedTroughFacings[index],
				"trough placement must turn its long axis left-to-right"
			);
		}
		BlockState wetSilty = ModSpringhouseBlocks.HOLLOWED_CHESTNUT_TROUGH
			.defaultBlockState()
			.setValue(HollowedChestnutTroughBlock.CONTENT, TroughContent.SILTY)
			.setValue(HollowedChestnutTroughBlock.WATERLOGGED, true);
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			BlockPos forward = center.relative(facing);
			BlockPos backward = center.relative(facing.getOpposite());
			BlockState centerState = wetSilty.setValue(
				HollowedChestnutTroughBlock.FACING,
				facing
			);
			BlockState sameAxis = ModSpringhouseBlocks.HOLLOWED_CHESTNUT_TROUGH
				.defaultBlockState()
				.setValue(
					HollowedChestnutTroughBlock.FACING,
					facing.getOpposite()
				);

			setWithNeighbourUpdates(helper, center, centerState);
			assertTroughPart(helper, center, TroughPart.STANDALONE);

			setWithNeighbourUpdates(helper, forward, sameAxis);
			assertTroughPart(helper, center, TroughPart.INLET);
			setWithNeighbourUpdates(helper, backward, sameAxis);
			assertTroughPart(helper, center, TroughPart.MIDDLE);
			helper.assertValueEqual(
				helper
					.getBlockState(center)
					.getValue(HollowedChestnutTroughBlock.CONTENT),
				TroughContent.SILTY,
				"connection updates must preserve sediment"
			);
			helper.assertTrue(
				helper
					.getBlockState(center)
					.getValue(HollowedChestnutTroughBlock.WATERLOGGED),
				"connection updates must preserve waterlogging"
			);

			helper.getLevel().destroyBlock(helper.absolutePos(forward), false);
			assertTroughPart(helper, center, TroughPart.OUTLET);
			helper.getLevel().destroyBlock(helper.absolutePos(backward), false);
			assertTroughPart(helper, center, TroughPart.STANDALONE);

			BlockState perpendicularState = sameAxis.setValue(
				HollowedChestnutTroughBlock.FACING,
				facing.getClockWise()
			);
			setWithNeighbourUpdates(helper, forward, perpendicularState);
			assertTroughPart(helper, center, TroughPart.STANDALONE);
			assertTroughPart(helper, forward, TroughPart.STANDALONE);
			helper.getLevel().destroyBlock(helper.absolutePos(forward), false);
			helper.getLevel().destroyBlock(helper.absolutePos(center), false);
		}

		BlockState rotated = ModSpringhouseBlocks.HOLLOWED_CHESTNUT_TROUGH
			.defaultBlockState()
			.setValue(HollowedChestnutTroughBlock.FACING, Direction.NORTH)
			.setValue(HollowedChestnutTroughBlock.PART, TroughPart.INLET)
			.rotate(Rotation.CLOCKWISE_90);
		helper.assertValueEqual(
			rotated.getValue(HollowedChestnutTroughBlock.FACING),
			Direction.EAST,
			"trough rotation must rotate its facing"
		);
		helper.assertValueEqual(
			rotated.getValue(HollowedChestnutTroughBlock.PART),
			TroughPart.INLET,
			"trough rotation must preserve its relative connection part"
		);
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void propsAreDirectionalNonFullBlocksWithRealBlockItems(
		GameTestHelper helper
	) {
		for (Block block : ModSpringhouseBlocks.ALL) {
			helper.assertTrue(
				block.asItem() != Items.AIR,
				"springhouse block is missing an obtainable BlockItem: " + block
			);
			helper.assertValueEqual(
				Item.BY_BLOCK.get(block),
				block.asItem(),
				"Item.BY_BLOCK mapping for " + block
			);
		}

		for (Direction facing : Direction.Plane.HORIZONTAL) {
			for (boolean waterlogged : new boolean[] {false, true}) {
				for (boolean legacyFlowing : new boolean[] {false, true}) {
					BlockState spout = ModSpringhouseBlocks.HOLLOW_LIMB_SPOUT
						.defaultBlockState()
						.setValue(HollowLimbSpoutBlock.FACING, facing)
						.setValue(HollowLimbSpoutBlock.FLOWING, legacyFlowing)
						.setValue(HollowLimbSpoutBlock.WATERLOGGED, waterlogged);
					assertCodecRoundTrip(
						helper,
						spout,
						"spout "
							+ facing
							+ "/flowing="
							+ legacyFlowing
							+ "/waterlogged="
							+ waterlogged
					);
					helper.assertValueEqual(
						spout.getFluidState().is(Fluids.WATER),
						waterlogged || legacyFlowing,
						"spout water must be a vanilla FluidState"
					);
				}
			}

			for (Block block : ModSpringhouseBlocks.FURNITURE) {
				BlockState state = block
					.defaultBlockState()
					.setValue(HorizontalDirectionalBlock.FACING, facing);
				assertCodecRoundTrip(
					helper,
					state,
					"directional springhouse prop " + block + "/" + facing
				);
				VoxelShape shape = state.getShape(
					helper.getLevel(),
					helper.absolutePos(TARGET)
				);
				helper.assertTrue(
					!shape.isEmpty() && shape.bounds().getXsize() < 1.0,
					"prop must not use invisible full-cube geometry: " + block
				);
			}
		}
		helper.succeed();
	}

	private static void assertCodecRoundTrip(
		GameTestHelper helper,
		BlockState original,
		String label
	) {
		Tag encoded = BlockState.CODEC
			.encodeStart(NbtOps.INSTANCE, original)
			.getOrThrow(IllegalStateException::new);
		BlockState decoded = BlockState.CODEC
			.parse(NbtOps.INSTANCE, encoded)
			.getOrThrow(IllegalStateException::new);
		helper.assertValueEqual(decoded, original, label + " codec round trip");
	}

	private static void setWithNeighbourUpdates(
		GameTestHelper helper,
		BlockPos relativePos,
		BlockState state
	) {
		helper.getLevel().setBlock(
			helper.absolutePos(relativePos),
			state,
			Block.UPDATE_ALL
		);
	}

	private static void assertTroughPart(
		GameTestHelper helper,
		BlockPos relativePos,
		TroughPart expected
	) {
		helper.assertValueEqual(
			helper
				.getBlockState(relativePos)
				.getValue(HollowedChestnutTroughBlock.PART),
			expected,
			"automatic trough part at " + relativePos
		);
	}
}
