package net.beforetheblight.gametest;

import java.util.List;

import net.beforetheblight.block.BushelBasketBlock;
import net.beforetheblight.block.ChestnutChinkingStripBlock;
import net.beforetheblight.block.ChestnutRepairCornerBlock;
import net.beforetheblight.block.CornBinBlock;
import net.beforetheblight.block.DryingCornBundleBlock;
import net.beforetheblight.block.HandCornShellerBlock;
import net.beforetheblight.block.LayeredEarCornPileBlock;
import net.beforetheblight.block.SaddleNotchedChestnutCornerBlock;
import net.beforetheblight.block.furniture.AbstractTwoPartFurnitureBlock;
import net.beforetheblight.block.furniture.ActiveFurnitureBlock;
import net.beforetheblight.block.furniture.ActiveTwoPartFurnitureBlock;
import net.beforetheblight.block.furniture.FurniturePart;
import net.beforetheblight.block.furniture.OpenFurnitureBlock;
import net.beforetheblight.block.furniture.OpenTwoPartFurnitureBlock;
import net.beforetheblight.block.furniture.SeatingFurnitureBlock;
import net.beforetheblight.block.domestic.DomesticLightBlock;
import net.beforetheblight.block.stonehearth.FireboxState;
import net.beforetheblight.block.stonehearth.StoneFireboxBlock;
import net.beforetheblight.registry.ModBlocks;
import net.beforetheblight.registry.ModBoardRoofBlocks;
import net.beforetheblight.registry.ModCornCribBlocks;
import net.beforetheblight.registry.ModDomesticBlocks;
import net.beforetheblight.registry.ModDoorWindowBlocks;
import net.beforetheblight.registry.ModExteriorBlocks;
import net.beforetheblight.registry.ModFurnitureBlocks;
import net.beforetheblight.registry.ModItems;
import net.beforetheblight.registry.ModStoneHearthBlocks;
import net.beforetheblight.registry.ModTimberBlocks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Dispatch-level coverage for the mod's interactive block families.
 *
 * <p>Every click in this class travels through
 * {@link net.minecraft.server.level.ServerPlayerGameMode#useItemOn}; direct
 * calls to a block's protected use hooks would miss the empty-hand fallback,
 * held-item precedence, secondary-use suppression, and item-placement rules
 * that players actually encounter.</p>
 */
public final class InteractiveBlockGameTests {
	private static final double DROP_SEARCH_RADIUS = 1.75D;

	@GameTest(maxTicks = 80)
	public void cornTransactionsConserveItemsAcrossDispatchAndStateRoundTrip(
		GameTestHelper helper
	) {
		ServerPlayer player = player(helper, new BlockPos(3, 2, 3));
		try {
			verifyBushelBasket(helper, player, new BlockPos(2, 2, 2));
			verifyCornBin(helper, player, new BlockPos(4, 2, 2));
			verifyHandSheller(helper, player, new BlockPos(6, 2, 2));
			verifyLayeredEarPile(helper, player, new BlockPos(8, 2, 2));
			verifyDryingBundleHarvest(helper, player, new BlockPos(2, 2, 5));
			helper.succeed();
		} finally {
			removePlayer(helper, player);
		}
	}

	@GameTest(maxTicks = 60)
	public void saddleJoineryAndSingleBlockFurnitureUseRealDispatch(
		GameTestHelper helper
	) {
		ServerPlayer player = player(helper, new BlockPos(4, 2, 4));
		try {
			BlockPos saddlePos = new BlockPos(2, 2, 2);
			helper.setBlock(
				saddlePos,
				ModTimberBlocks.SADDLE_NOTCHED_CHESTNUT_CORNER.defaultBlockState()
			);
			ItemStack clay = new ItemStack(Items.CLAY_BALL, 2);
			assertSuccess(helper, use(helper, player, saddlePos, clay), "clay chinking");
			helper.assertBlockProperty(
				saddlePos,
				SaddleNotchedChestnutCornerBlock.CHINKED,
				true
			);
			helper.assertValueEqual(clay.getCount(), 1, "clay remaining after chinking");
			assertRoundTripAt(helper, saddlePos, "chinked saddle corner");
			use(helper, player, saddlePos, clay);
			helper.assertValueEqual(clay.getCount(), 1, "second chinking must not consume clay");

			BlockPos chinkingPos = new BlockPos(4, 2, 2);
			helper.setBlock(
				chinkingPos,
				ModTimberBlocks.CHESTNUT_CHINKING_STRIP.defaultBlockState()
			);
			ChestnutChinkingStripBlock.Condition oldCondition = helper
				.getBlockState(chinkingPos)
				.getValue(ChestnutChinkingStripBlock.CONDITION);
			player.setShiftKeyDown(true);
			assertSuccess(
				helper,
				use(helper, player, chinkingPos, ItemStack.EMPTY),
				"chinking condition cycle"
			);
			player.setShiftKeyDown(false);
			helper.assertFalse(
				helper.getBlockState(chinkingPos)
					.getValue(ChestnutChinkingStripBlock.CONDITION) == oldCondition,
				"secondary empty-hand use did not cycle chinking condition"
			);

			BlockPos repairPos = new BlockPos(6, 2, 2);
			helper.setBlock(
				repairPos,
				ModTimberBlocks.CHESTNUT_REPAIR_CORNER.defaultBlockState()
			);
			ChestnutRepairCornerBlock.Style oldStyle = helper
				.getBlockState(repairPos)
				.getValue(ChestnutRepairCornerBlock.STYLE);
			player.setShiftKeyDown(true);
			assertSuccess(
				helper,
				use(helper, player, repairPos, ItemStack.EMPTY),
				"repair-corner style cycle"
			);
			player.setShiftKeyDown(false);
			helper.assertFalse(
				helper.getBlockState(repairPos)
					.getValue(ChestnutRepairCornerBlock.STYLE) == oldStyle,
				"secondary empty-hand use did not cycle repair-corner style"
			);

			BlockPos dropLeafPos = new BlockPos(2, 2, 5);
			putSupported(helper, dropLeafPos, ModFurnitureBlocks.DROP_LEAF_TABLE.defaultBlockState());
			assertSuccess(
				helper,
				use(helper, player, dropLeafPos, ItemStack.EMPTY),
				"drop-leaf table open"
			);
			helper.assertBlockProperty(dropLeafPos, OpenFurnitureBlock.OPEN, true);
			assertSuccess(
				helper,
				use(helper, player, dropLeafPos, ItemStack.EMPTY),
				"drop-leaf table close"
			);
			helper.assertBlockProperty(dropLeafPos, OpenFurnitureBlock.OPEN, false);

			BlockPos wheelPos = new BlockPos(4, 2, 5);
			putSupported(helper, wheelPos, ModFurnitureBlocks.SPINNING_WHEEL.defaultBlockState());
			assertSuccess(
				helper,
				use(helper, player, wheelPos, ItemStack.EMPTY),
				"spinning-wheel display toggle"
			);
			helper.assertBlockProperty(wheelPos, ActiveFurnitureBlock.ACTIVE, true);

			BlockPos chairPos = new BlockPos(6, 2, 5);
			putSupported(helper, chairPos, ModFurnitureBlocks.LADDER_BACK_CHAIR.defaultBlockState());
			assertSuccess(
				helper,
				use(helper, player, chairPos, ItemStack.EMPTY),
				"ladder-back-chair sitting"
			);
			helper.assertBlockProperty(chairPos, SeatingFurnitureBlock.OCCUPIED, true);
			helper.assertTrue(player.isPassenger(), "chair did not mount the player");
			player.stopRiding();
			helper.succeed();
		} finally {
			player.setShiftKeyDown(false);
			if (player.isPassenger()) {
				player.stopRiding();
			}
			removePlayer(helper, player);
		}
	}

	@GameTest(maxTicks = 80)
	public void twoPartFurnitureOpeningsLightsAndFireboxUseRealDispatch(
		GameTestHelper helper
	) {
		ServerPlayer player = player(helper, new BlockPos(5, 2, 5));
		try {
			BlockPos trundleLeft = new BlockPos(2, 2, 2);
			BlockPos trundleRight = trundleLeft.east();
			putTwoPart(
				helper,
				trundleLeft,
				trundleRight,
				ModFurnitureBlocks.TRUNDLE_BED.defaultBlockState()
					.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
			);
			assertSuccess(
				helper,
				use(helper, player, trundleLeft, ItemStack.EMPTY),
				"trundle pullout"
			);
			helper.assertBlockProperty(trundleLeft, OpenTwoPartFurnitureBlock.OPEN, true);
			helper.assertBlockProperty(trundleRight, OpenTwoPartFurnitureBlock.OPEN, true);
			assertSuccess(
				helper,
				use(helper, player, trundleRight, ItemStack.EMPTY),
				"trundle retraction from partner half"
			);
			helper.assertBlockProperty(trundleLeft, OpenTwoPartFurnitureBlock.OPEN, false);
			helper.assertBlockProperty(trundleRight, OpenTwoPartFurnitureBlock.OPEN, false);

			BlockPos loomLeft = new BlockPos(5, 2, 2);
			BlockPos loomRight = loomLeft.east();
			putTwoPart(
				helper,
				loomLeft,
				loomRight,
				ModFurnitureBlocks.FLOOR_LOOM.defaultBlockState()
					.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
			);
			assertSuccess(
				helper,
				use(helper, player, loomRight, ItemStack.EMPTY),
				"floor-loom display toggle"
			);
			helper.assertBlockProperty(loomLeft, ActiveTwoPartFurnitureBlock.ACTIVE, true);
			helper.assertBlockProperty(loomRight, ActiveTwoPartFurnitureBlock.ACTIVE, true);

			List<Block> openings = List.of(
				ModDoorWindowBlocks.BOARD_SHUTTER,
				ModDoorWindowBlocks.PAIRED_BOARD_SHUTTERS,
				ModDoorWindowBlocks.OPERABLE_SASH_WINDOW,
				ModDoorWindowBlocks.SHUTTERED_SASH_WINDOW
			);
			for (int index = 0; index < openings.size(); index++) {
				BlockPos pos = new BlockPos(2 + index * 2, 2, 5);
				helper.setBlock(pos, openings.get(index).defaultBlockState());
				assertSuccess(
					helper,
					use(helper, player, pos, ItemStack.EMPTY),
					"opening toggle " + openings.get(index)
				);
				helper.assertBlockProperty(pos, BlockStateProperties.OPEN, true);
			}

			List<Block> lights = List.of(
				ModDomesticBlocks.TALLOW_CANDLE,
				ModDomesticBlocks.BETTY_LAMP
			);
			for (int index = 0; index < lights.size(); index++) {
				BlockPos pos = new BlockPos(2 + index * 2, 2, 7);
				putSupported(helper, pos, lights.get(index).defaultBlockState());
				assertSuccess(
					helper,
					use(helper, player, pos, ItemStack.EMPTY),
					"domestic-light ignition " + lights.get(index)
				);
				helper.assertBlockProperty(pos, DomesticLightBlock.LIT, true);
			}

			BlockPos fireboxPos = new BlockPos(6, 2, 7);
			helper.setBlock(fireboxPos, ModStoneHearthBlocks.FIELDSTONE_FIREBOX.defaultBlockState());
			FireboxState cold = helper.getBlockState(fireboxPos)
				.getValue(StoneFireboxBlock.FIRE_STATE);
			assertSuccess(
				helper,
				use(helper, player, fireboxPos, ItemStack.EMPTY),
				"firebox visual-state cycle"
			);
			helper.assertFalse(
				helper.getBlockState(fireboxPos)
					.getValue(StoneFireboxBlock.FIRE_STATE) == cold,
				"firebox state did not advance"
			);
			assertRoundTripAt(helper, fireboxPos, "fieldstone firebox state");
			helper.succeed();
		} finally {
			removePlayer(helper, player);
		}
	}

	@GameTest(maxTicks = 80)
	public void representativeVanillaBackedFamiliesUseRuntimePaths(
		GameTestHelper helper
	) {
		ServerPlayer player = player(helper, new BlockPos(4, 2, 4));
		try {
			BlockPos doorPos = new BlockPos(2, 2, 2);
			putSupported(helper, doorPos, Blocks.AIR.defaultBlockState());
			helper.setBlock(doorPos.above(), Blocks.AIR);
			ItemStack doorItem = new ItemStack(
				ModDoorWindowBlocks.ROUGH_CHESTNUT_BOARD_DOOR.asItem()
			);
			assertSuccess(
				helper,
				useOnFace(helper, player, doorPos.below(), doorItem, Direction.UP),
				"rough chestnut door placement"
			);
			helper.assertBlockPresent(ModDoorWindowBlocks.ROUGH_CHESTNUT_BOARD_DOOR, doorPos);
			assertSuccess(
				helper,
				use(helper, player, doorPos, ItemStack.EMPTY),
				"rough chestnut door opening"
			);
			helper.assertBlockProperty(doorPos, DoorBlock.OPEN, true);

			BlockPos hatchPos = new BlockPos(5, 2, 2);
			helper.setBlock(hatchPos, ModBoardRoofBlocks.LOFT_HATCH.defaultBlockState());
			assertSuccess(
				helper,
				use(helper, player, hatchPos, ItemStack.EMPTY),
				"loft hatch opening"
			);
			helper.assertBlockProperty(hatchPos, TrapDoorBlock.OPEN, true);

			BlockPos gatePos = new BlockPos(7, 2, 2);
			helper.setBlock(gatePos, ModExteriorBlocks.SPLIT_RAIL_GATE.defaultBlockState());
			assertSuccess(
				helper,
				use(helper, player, gatePos, ItemStack.EMPTY),
				"split rail gate opening"
			);
			helper.assertBlockProperty(gatePos, FenceGateBlock.OPEN, true);

			BlockPos buttonPos = new BlockPos(2, 2, 5);
			putSupported(
				helper,
				buttonPos,
				ModBlocks.CHESTNUT_BUTTON.defaultBlockState()
					.setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
			);
			assertSuccess(
				helper,
				use(helper, player, buttonPos, ItemStack.EMPTY),
				"chestnut button press"
			);
			helper.assertBlockProperty(buttonPos, ButtonBlock.POWERED, true);

			BlockPos storagePos = new BlockPos(5, 2, 5);
			putSupported(helper, storagePos, ModFurnitureBlocks.SIX_BOARD_CHEST.defaultBlockState());
			assertSuccess(
				helper,
				use(helper, player, storagePos, ItemStack.EMPTY),
				"six-board chest opening"
			);
			helper.assertTrue(
				player.containerMenu != player.inventoryMenu,
				"storage dispatch did not open a container menu"
			);
			// BarrelBlockEntity ties OPEN to the active viewer count. Verify the
			// opened state while this player still owns the menu, then verify the
			// corresponding close transition after releasing it.
			helper.assertBlockProperty(storagePos, BarrelBlock.OPEN, true);
			player.closeContainer();
			helper.assertBlockProperty(storagePos, BarrelBlock.OPEN, false);

			verifyChestnutPilePlacementAndRecovery(
				helper,
				player,
				new BlockPos(7, 2, 5)
			);

			BlockPos bedFoot = new BlockPos(3, 2, 7);
			BlockPos bedHead = bedFoot.north();
			putBed(helper, bedFoot, bedHead);
			assertSuccess(
				helper,
				use(helper, player, bedFoot, ItemStack.EMPTY),
				"rope-bed use"
			);
			helper.assertBlockPresent(ModFurnitureBlocks.ROPE_BED, bedFoot);
			helper.assertBlockPresent(ModFurnitureBlocks.ROPE_BED, bedHead);
			helper.succeed();
		} finally {
			if (player.isPassenger()) {
				player.stopRiding();
			}
			removePlayer(helper, player);
		}
	}

	@GameTest(maxTicks = 40)
	public void representativePressurePlateRespondsToARealPlayer(
		GameTestHelper helper
	) {
		BlockPos platePos = new BlockPos(3, 2, 3);
		putSupported(
			helper,
			platePos,
			ModBlocks.CHESTNUT_PRESSURE_PLATE.defaultBlockState()
		);
		ServerPlayer player = player(helper, platePos);
		BlockPos absolute = helper.absolutePos(platePos);
		player.snapTo(
			absolute.getX() + 0.5D,
			absolute.getY() + 0.125D,
			absolute.getZ() + 0.5D,
			0.0F,
			0.0F
		);
		// A real client's connection calls ServerPlayer#doTick after processing
		// movement. GameTest's embedded mock connection is not in the server's
		// network tick list, so neither snapTo nor waiting advances that deferred
		// block-effects pass. Record the same short downward landing movement and
		// run the real server-player movement tick so vanilla
		// BasePressurePlateBlock receives entityInside normally.
		player.move(MoverType.SELF, new Vec3(0.0D, -0.25D, 0.0D));
		player.doTick();

		helper.startSequence()
			.thenIdle(3)
			.thenExecute(() -> helper.assertBlockProperty(
				platePos,
				PressurePlateBlock.POWERED,
				true
			))
			.thenExecute(() -> removePlayer(helper, player))
			.thenSucceed();
	}

	private static void verifyBushelBasket(
		GameTestHelper helper,
		ServerPlayer player,
		BlockPos pos
	) {
		clearItemEntities(helper, pos);
		helper.setBlock(pos, ModCornCribBlocks.BUSHEL_BASKET.defaultBlockState());
		ItemStack ears = new ItemStack(ModItems.DRIED_EAR_OF_CORN, 8);
		assertSuccess(helper, use(helper, player, pos, ears), "bushel fill");
		helper.assertValueEqual(ears.getCount(), 4, "bushel fill input remainder");
		helper.assertBlockProperty(pos, BushelBasketBlock.FILLED, true);
		assertRoundTripAt(helper, pos, "filled bushel basket");
		assertSuccess(helper, use(helper, player, pos, ItemStack.EMPTY), "bushel empty");
		helper.assertBlockProperty(pos, BushelBasketBlock.FILLED, false);
		helper.assertValueEqual(
			worldDropCount(helper, pos, ModItems.DRIED_EAR_OF_CORN),
			4,
			"bushel recovered ears"
		);
		use(helper, player, pos, ItemStack.EMPTY);
		helper.assertValueEqual(
			worldDropCount(helper, pos, ModItems.DRIED_EAR_OF_CORN),
			4,
			"empty bushel duplicate recovery"
		);
	}

	private static void verifyCornBin(
		GameTestHelper helper,
		ServerPlayer player,
		BlockPos pos
	) {
		clearItemEntities(helper, pos);
		helper.setBlock(pos, ModCornCribBlocks.CORN_BIN.defaultBlockState());
		ItemStack ears = new ItemStack(ModItems.DRIED_EAR_OF_CORN, 12);
		assertSuccess(helper, use(helper, player, pos, ears), "corn-bin first fill");
		assertSuccess(helper, use(helper, player, pos, ears), "corn-bin second fill");
		helper.assertValueEqual(ears.getCount(), 4, "corn-bin input remainder");
		helper.assertBlockProperty(pos, CornBinBlock.LEVEL, 2);
		assertRoundTripAt(helper, pos, "two-level corn bin");
		assertSuccess(helper, use(helper, player, pos, ItemStack.EMPTY), "corn-bin first removal");
		assertSuccess(helper, use(helper, player, pos, ItemStack.EMPTY), "corn-bin second removal");
		helper.assertBlockProperty(pos, CornBinBlock.LEVEL, 0);
		helper.assertValueEqual(
			worldDropCount(helper, pos, ModItems.DRIED_EAR_OF_CORN),
			8,
			"corn-bin recovered ears"
		);
		use(helper, player, pos, ItemStack.EMPTY);
		helper.assertValueEqual(
			worldDropCount(helper, pos, ModItems.DRIED_EAR_OF_CORN),
			8,
			"empty corn-bin duplicate recovery"
		);
	}

	private static void verifyHandSheller(
		GameTestHelper helper,
		ServerPlayer player,
		BlockPos pos
	) {
		clearItemEntities(helper, pos);
		helper.setBlock(pos, ModCornCribBlocks.HAND_CORN_SHELLER.defaultBlockState());
		ItemStack ears = new ItemStack(ModItems.DRIED_EAR_OF_CORN, 2);
		assertSuccess(helper, use(helper, player, pos, ears), "hand-sheller load");
		helper.assertValueEqual(ears.getCount(), 1, "hand-sheller input remainder");
		helper.assertBlockProperty(pos, HandCornShellerBlock.LOADED, true);
		assertRoundTripAt(helper, pos, "loaded hand sheller");
		assertSuccess(helper, use(helper, player, pos, ItemStack.EMPTY), "hand-sheller operation");
		helper.assertBlockProperty(pos, HandCornShellerBlock.LOADED, false);
		helper.assertValueEqual(
			worldDropCount(helper, pos, ModItems.CORN_KERNELS),
			4,
			"hand-sheller kernels"
		);
		use(helper, player, pos, ItemStack.EMPTY);
		helper.assertValueEqual(
			worldDropCount(helper, pos, ModItems.CORN_KERNELS),
			4,
			"empty hand-sheller duplicate output"
		);
	}

	private static void verifyLayeredEarPile(
		GameTestHelper helper,
		ServerPlayer player,
		BlockPos pos
	) {
		clearItemEntities(helper, pos);
		putSupported(helper, pos, ModCornCribBlocks.YELLOW_EAR_CORN_PILE.defaultBlockState());
		ItemStack ears = new ItemStack(ModItems.DRIED_EAR_OF_CORN, 8);
		assertSuccess(helper, use(helper, player, pos, ears), "ear-pile layer addition");
		helper.assertValueEqual(ears.getCount(), 4, "ear-pile input remainder");
		helper.assertBlockProperty(
			pos,
			LayeredEarCornPileBlock.FULLNESS,
			LayeredEarCornPileBlock.Fullness.HALF
		);
		assertRoundTripAt(helper, pos, "half-full ear pile");
		assertSuccess(helper, use(helper, player, pos, ItemStack.EMPTY), "ear-pile layer removal");
		helper.assertBlockProperty(
			pos,
			LayeredEarCornPileBlock.FULLNESS,
			LayeredEarCornPileBlock.Fullness.QUARTER
		);
		assertSuccess(helper, use(helper, player, pos, ItemStack.EMPTY), "ear-pile final removal");
		helper.assertTrue(helper.getBlockState(pos).isAir(), "last ear-pile layer remained");
		helper.assertValueEqual(
			worldDropCount(helper, pos, ModItems.DRIED_EAR_OF_CORN),
			8,
			"ear-pile recovered ears"
		);
	}

	private static void verifyDryingBundleHarvest(
		GameTestHelper helper,
		ServerPlayer player,
		BlockPos pos
	) {
		clearItemEntities(helper, pos);
		helper.setBlock(
			pos,
			ModBlocks.DRYING_CORN_BUNDLE.defaultBlockState()
				.setValue(DryingCornBundleBlock.AGE, DryingCornBundleBlock.MAX_AGE)
				.setValue(DryingCornBundleBlock.COUNT, DryingCornBundleBlock.LEGACY_EAR_COUNT)
		);
		assertRoundTripAt(helper, pos, "mature corn drying rack");
		assertSuccess(
			helper,
			use(helper, player, pos, ItemStack.EMPTY),
			"mature corn-rack unload"
		);
		helper.assertBlockProperty(pos, DryingCornBundleBlock.COUNT, 0);
		helper.assertBlockProperty(pos, DryingCornBundleBlock.AGE, 0);
		helper.assertValueEqual(
			worldDropCount(helper, pos, ModItems.DRIED_EAR_OF_CORN),
			DryingCornBundleBlock.LEGACY_EAR_COUNT,
			"corn-rack recovered ears"
		);
		use(helper, player, pos, ItemStack.EMPTY);
		helper.assertValueEqual(
			worldDropCount(helper, pos, ModItems.DRIED_EAR_OF_CORN),
			DryingCornBundleBlock.LEGACY_EAR_COUNT,
			"emptied corn-rack duplicate output"
		);
	}

	private static void verifyChestnutPilePlacementAndRecovery(
		GameTestHelper helper,
		ServerPlayer player,
		BlockPos pos
	) {
		clearItemEntities(helper, pos);
		putSupported(
			helper,
			pos,
			ModBlocks.CHESTNUT_PILE.defaultBlockState()
				.setValue(SnowLayerBlock.LAYERS, 1)
		);
		ItemStack handfuls = new ItemStack(ModItems.HANDFUL_OF_CHESTNUTS, 2);
		assertSuccess(
			helper,
			use(helper, player, pos, handfuls),
			"chestnut-pile layer addition"
		);
		helper.assertValueEqual(handfuls.getCount(), 1, "chestnut handful placement remainder");
		helper.assertBlockProperty(pos, SnowLayerBlock.LAYERS, 2);
		helper.assertTrue(
			player.gameMode.destroyBlock(helper.absolutePos(pos)),
			"survival player could not break chestnut pile"
		);
		helper.assertValueEqual(
			worldDropCount(helper, pos, ModItems.HANDFUL_OF_CHESTNUTS),
			2,
			"two-layer chestnut pile recovery"
		);
	}

	private static void putTwoPart(
		GameTestHelper helper,
		BlockPos left,
		BlockPos right,
		BlockState baseState
	) {
		helper.setBlock(left.below(), Blocks.STONE);
		helper.setBlock(right.below(), Blocks.STONE);
		helper.setBlock(
			left,
			baseState.setValue(AbstractTwoPartFurnitureBlock.PART, FurniturePart.LEFT)
		);
		helper.setBlock(
			right,
			baseState.setValue(AbstractTwoPartFurnitureBlock.PART, FurniturePart.RIGHT)
		);
	}

	private static void putBed(
		GameTestHelper helper,
		BlockPos foot,
		BlockPos head
	) {
		helper.setBlock(foot.below(), Blocks.STONE);
		helper.setBlock(head.below(), Blocks.STONE);
		BlockState base = ModFurnitureBlocks.ROPE_BED.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
			.setValue(BedBlock.OCCUPIED, false);
		helper.setBlock(foot, base.setValue(BedBlock.PART, BedPart.FOOT));
		helper.setBlock(head, base.setValue(BedBlock.PART, BedPart.HEAD));
	}

	private static void putSupported(
		GameTestHelper helper,
		BlockPos pos,
		BlockState state
	) {
		helper.setBlock(pos.below(), Blocks.STONE);
		helper.setBlock(pos, state);
	}

	private static void assertRoundTripAt(
		GameTestHelper helper,
		BlockPos pos,
		String label
	) {
		BlockState original = helper.getBlockState(pos);
		Tag encoded = BlockState.CODEC
			.encodeStart(NbtOps.INSTANCE, original)
			.getOrThrow(IllegalStateException::new);
		BlockState decoded = BlockState.CODEC
			.parse(NbtOps.INSTANCE, encoded)
			.getOrThrow(IllegalStateException::new);
		helper.assertValueEqual(decoded, original, label + " NBT round trip");
		helper.setBlock(pos, decoded);
	}

	private static void assertSuccess(
		GameTestHelper helper,
		InteractionResult result,
		String label
	) {
		helper.assertTrue(
			result instanceof InteractionResult.Success,
			label + " did not return success: " + result
		);
	}

	private static InteractionResult use(
		GameTestHelper helper,
		ServerPlayer player,
		BlockPos relativePos,
		ItemStack stack
	) {
		return useOnFace(helper, player, relativePos, stack, Direction.UP);
	}

	private static InteractionResult useOnFace(
		GameTestHelper helper,
		ServerPlayer player,
		BlockPos relativePos,
		ItemStack stack,
		Direction face
	) {
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		BlockPos absolutePos = helper.absolutePos(relativePos);
		Vec3 location = Vec3.atCenterOf(absolutePos).add(
			face.getStepX() * 0.5D,
			face.getStepY() * 0.5D,
			face.getStepZ() * 0.5D
		);
		BlockHitResult hit = new BlockHitResult(
			location,
			face,
			absolutePos,
			false
		);
		return player.gameMode.useItemOn(
			player,
			helper.getLevel(),
			stack,
			InteractionHand.MAIN_HAND,
			hit
		);
	}

	@SuppressWarnings("removal")
	private static ServerPlayer player(GameTestHelper helper, BlockPos relativeTarget) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(relativeTarget);
		player.snapTo(
			absolute.getX() + 0.5D,
			absolute.getY() + 0.1D,
			absolute.getZ() + 1.5D,
			180.0F,
			0.0F
		);
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		return player;
	}

	private static void removePlayer(GameTestHelper helper, ServerPlayer player) {
		helper.getLevel().getServer().getPlayerList().remove(player);
	}

	private static int worldDropCount(
		GameTestHelper helper,
		BlockPos relativePos,
		Item item
	) {
		return itemEntities(helper, relativePos).stream()
			.map(ItemEntity::getItem)
			.filter(stack -> stack.is(item))
			.mapToInt(ItemStack::getCount)
			.sum();
	}

	private static List<ItemEntity> itemEntities(
		GameTestHelper helper,
		BlockPos relativePos
	) {
		BlockPos absolutePos = helper.absolutePos(relativePos);
		return helper.getLevel().getEntities(
			EntityType.ITEM,
			new AABB(absolutePos).inflate(DROP_SEARCH_RADIUS),
			ItemEntity::isAlive
		);
	}

	private static void clearItemEntities(
		GameTestHelper helper,
		BlockPos relativePos
	) {
		itemEntities(helper, relativePos).forEach(ItemEntity::discard);
	}
}
