package net.beforetheblight.gametest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import net.beforetheblight.block.AbstractSawingTrestlesBlock;
import net.beforetheblight.block.LoadedSawingTrestlesBlock;
import net.beforetheblight.interaction.SawingTrestleStateMachine;
import net.beforetheblight.interaction.TimberProcessingRegistry;
import net.beforetheblight.interaction.TimberProcessingRegistry.TimberProcess;
import net.beforetheblight.interaction.TimberType;
import net.beforetheblight.registry.ModBlocks;
import net.beforetheblight.registry.ModItems;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.gametest.framework.GameTestHelper;

public final class SawingGameTests {
	private static final BlockPos TARGET = new BlockPos(3, 2, 3);
	private static final BlockPos ABOVE_TARGET = TARGET.above();
	private static final double DROP_SEARCH_RADIUS = 1.5;
	private static final int EXPECTED_COOLDOWN_TICKS = 8;
	private static final int MAIN_INVENTORY_SIZE = 36;
	private static final int FULL_INVENTORY_FILLER_COUNT = 34 * 64;
	private static final List<Direction> HORIZONTAL_FACINGS = List.of(
		Direction.NORTH,
		Direction.EAST,
		Direction.SOUTH,
		Direction.WEST
	);

	@GameTest(maxTicks = 40)
	public void allRegisteredBeamsLoadAtStageZeroAndConsumeExactlyOne(GameTestHelper helper) {
		int playerIndex = 0;
		for (TimberProcess process : TimberProcessingRegistry.all()) {
			for (Direction facing : HORIZONTAL_FACINGS) {
				String label = process.type() + " loading facing " + facing;
				clearItemEntities(helper, TARGET);
				helper.setBlock(TARGET, empty(facing));
				FakePlayer player = survivalPlayer(
					helper,
					new ItemStack(process.finalBlock(), 2),
					"btb-load-" + playerIndex++
				);

				InteractionResult result = useHeldItem(helper, player, TARGET);
				assertSuccess(helper, result, label);
				helper.assertBlockState(
					TARGET,
					loaded(facing, process.type(), SawingTrestleStateMachine.INITIAL_CUT_STAGE)
				);
				helper.assertTrue(
					player.getMainHandItem().is(process.finalBlock().asItem()),
					label + " changed the remaining held item"
				);
				helper.assertValueEqual(
					player.getMainHandItem().getCount(),
					1,
					label + " held beam count"
				);
				helper.assertValueEqual(
					player.getInventory().countItem(process.finalBlock().asItem()),
					1,
					label + " total beam inventory count"
				);
				assertNoItemEntities(helper, TARGET, label);
			}
		}

		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void sawStrokesAdvanceAllRegisteredWoodsWithoutEarlyOutput(GameTestHelper helper) {
		int playerIndex = 0;
		for (TimberProcess process : TimberProcessingRegistry.all()) {
			Direction facing = HORIZONTAL_FACINGS.get(process.type().ordinal());
			String label = process.type() + " staged sawing";
			clearItemEntities(helper, TARGET);
			helper.setBlock(
				TARGET,
				loaded(facing, process.type(), SawingTrestleStateMachine.INITIAL_CUT_STAGE)
			);
			FakePlayer player = survivalPlayer(
				helper,
				ModItems.FRAME_SAW.getDefaultInstance(),
				"btb-stage-" + playerIndex++
			);
			ItemStack frameSaw = player.getMainHandItem();
			helper.assertTrue(frameSaw.isDamageableItem(), label + " frame saw is not damageable");

			for (int expectedStage = 1; expectedStage <= 3; expectedStage++) {
				if (expectedStage > 1) {
					clearCooldown(player, frameSaw);
				}
				InteractionResult result = useHeldItem(helper, player, TARGET);
				assertSuccess(helper, result, label + " stage " + expectedStage);
				helper.assertBlockState(
					TARGET,
					loaded(facing, process.type(), expectedStage)
				);
				helper.assertValueEqual(
					frameSaw.getDamageValue(),
					expectedStage,
					label + " damage after stage " + expectedStage
				);
				helper.assertTrue(
					player.getCooldowns().isOnCooldown(frameSaw),
					label + " did not start cooldown after stage " + expectedStage
				);
				assertNoItemEntities(helper, TARGET, label + " stage " + expectedStage);
			}
		}

		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void finalStrokeOutputsMappedBoardsAndRestoresEmptyTrestles(GameTestHelper helper) {
		int playerIndex = 0;
		for (TimberProcess process : TimberProcessingRegistry.all()) {
			for (Direction facing : HORIZONTAL_FACINGS) {
				String label = process.type() + " completion facing " + facing;
				clearItemEntities(helper, TARGET);
				helper.setBlock(
					TARGET,
					loaded(facing, process.type(), SawingTrestleStateMachine.FINAL_CUT_STAGE)
				);
				FakePlayer player = survivalPlayer(
					helper,
					ModItems.FRAME_SAW.getDefaultInstance(),
					"btb-done-" + playerIndex++
				);
				ItemStack frameSaw = player.getMainHandItem();

				InteractionResult result = useHeldItem(helper, player, TARGET);
				assertSuccess(helper, result, label);
				helper.assertBlockState(TARGET, empty(facing));
				helper.assertValueEqual(frameSaw.getDamageValue(), 1, label + " saw damage");
				helper.assertTrue(
					player.getCooldowns().isOnCooldown(frameSaw),
					label + " did not start cooldown"
				);

				Item outputItem = process.roughBoards().item().asItem();
				int outputCount = process.roughBoards().count();
				helper.assertItemEntityCountIs(
					outputItem,
					TARGET,
					DROP_SEARCH_RADIUS,
					outputCount
				);
				assertWorldDropsExactly(
					helper,
					TARGET,
					Map.of(outputItem, outputCount),
					label
				);
				clearItemEntities(helper, TARGET);
			}
		}

		assertFullStackSawingTransaction(helper);
		assertDurabilityLimitedSawingTransaction(helper);
		assertCreativeFullStackSawingTransaction(helper);
		assertSawingOutputSurvivesDisabledBlockDrops(helper);
		helper.succeed();
	}

	private static void assertFullStackSawingTransaction(GameTestHelper helper) {
		TimberProcess process = TimberProcessingRegistry.byType(TimberType.CHESTNUT)
			.orElseThrow(() -> new IllegalStateException("Chestnut timber process is missing."));
		Direction facing = Direction.NORTH;
		Item inputItem = process.finalBlock().asItem();
		Item outputItem = process.roughBoards().item().asItem();
		int additionalInputCount = 63;
		int expectedBatchCount = 64;
		int expectedOutputCount = process.roughBoards().count() * expectedBatchCount;

		clearItemEntities(helper, TARGET);
		helper.setBlock(
			TARGET,
			loaded(facing, process.type(), SawingTrestleStateMachine.INITIAL_CUT_STAGE)
		);
		FakePlayer player = survivalPlayer(
			helper,
			ModItems.FRAME_SAW.getDefaultInstance(),
			"btb-saw-full-stack"
		);
		ItemStack frameSaw = player.getMainHandItem();
		prepareFullBatchInventory(player, inputItem);
		helper.assertValueEqual(
			player.getInventory().countItem(inputItem),
			additionalInputCount,
			"full-stack sawing starting input count"
		);
		helper.assertValueEqual(
			frameSaw.getMaxDamage(),
			256,
			"full-stack frame-saw durability"
		);

		for (int stroke = 1; stroke <= 4; stroke++) {
			if (stroke > 1) {
				clearCooldown(player, frameSaw);
			}
			assertSuccess(
				helper,
				useHeldItem(helper, player, TARGET),
				"full-stack saw stroke " + stroke
			);
			if (stroke < 4) {
				helper.assertBlockState(TARGET, loaded(facing, process.type(), stroke));
				helper.assertValueEqual(
					frameSaw.getDamageValue(),
					stroke,
					"full-stack saw damage after stroke " + stroke
				);
				helper.assertValueEqual(
					player.getInventory().countItem(inputItem),
					additionalInputCount,
					"full-stack saw consumed inputs before terminal stroke " + stroke
				);
				assertNoItemEntities(helper, TARGET, "full-stack saw stroke " + stroke);
			}
		}

		helper.assertBlockState(TARGET, empty(facing));
		helper.assertValueEqual(
			player.getInventory().countItem(inputItem),
			0,
			"full-stack sawing remaining input count"
		);
		helper.assertTrue(
			player.getMainHandItem().isEmpty(),
			"full-stack sawing did not break the fully spent frame saw"
		);
		helper.assertValueEqual(
			player.getInventory().countItem(Items.COBBLESTONE),
			FULL_INVENTORY_FILLER_COUNT,
			"full-stack sawing changed unrelated full-inventory filler"
		);
		assertWorldDropsExactly(
			helper,
			TARGET,
			Map.of(outputItem, expectedOutputCount),
			"full-stack sawing output"
		);
		helper.assertTrue(
			itemEntitiesNear(helper, TARGET).stream()
				.allMatch(entity -> entity.getItem().getCount() <= entity.getItem().getMaxStackSize()),
			"full-stack sawing produced an oversized item stack"
		);
		clearItemEntities(helper, TARGET);
	}

	private static void assertDurabilityLimitedSawingTransaction(
		GameTestHelper helper
	) {
		TimberProcess process = TimberProcessingRegistry.byType(TimberType.CHESTNUT)
			.orElseThrow(() -> new IllegalStateException("Chestnut timber process is missing."));
		TimberProcess otherProcess = TimberProcessingRegistry.byType(TimberType.OAK)
			.orElseThrow(() -> new IllegalStateException("Oak timber process is missing."));
		Direction facing = Direction.EAST;
		Item inputItem = process.finalBlock().asItem();
		Item outputItem = process.roughBoards().item().asItem();
		ItemStack frameSaw = ModItems.FRAME_SAW.getDefaultInstance();
		frameSaw.setDamageValue(250);
		FakePlayer player = survivalPlayer(
			helper,
			frameSaw,
			"btb-saw-durability-limited"
		);
		player.getInventory().setItem(1, new ItemStack(inputItem, 3));
		player.getInventory().setItem(
			2,
			new ItemStack(otherProcess.finalBlock(), 4)
		);
		player.getInventory().setItem(3, new ItemStack(Items.COBBLESTONE, 5));
		clearItemEntities(helper, TARGET);
		helper.setBlock(
			TARGET,
			loaded(facing, process.type(), SawingTrestleStateMachine.FINAL_CUT_STAGE)
		);

		assertSuccess(
			helper,
			useHeldItem(helper, player, TARGET),
			"durability-limited terminal saw stroke"
		);
		helper.assertBlockState(TARGET, empty(facing));
		helper.assertValueEqual(
			player.getMainHandItem().getDamageValue(),
			255,
			"durability-limited frame-saw damage"
		);
		helper.assertValueEqual(
			player.getInventory().countItem(inputItem),
			2,
			"durability-limited matching beams"
		);
		helper.assertValueEqual(
			player.getInventory().countItem(otherProcess.finalBlock().asItem()),
			4,
			"durability-limited other-wood beams"
		);
		helper.assertValueEqual(
			player.getInventory().countItem(Items.COBBLESTONE),
			5,
			"durability-limited unrelated items"
		);
		assertWorldDropsExactly(
			helper,
			TARGET,
			Map.of(outputItem, process.roughBoards().count() * 2),
			"durability-limited sawing output"
		);
		clearItemEntities(helper, TARGET);
	}

	private static void assertCreativeFullStackSawingTransaction(
		GameTestHelper helper
	) {
		TimberProcess process = TimberProcessingRegistry.byType(TimberType.CHESTNUT)
			.orElseThrow(() -> new IllegalStateException("Chestnut timber process is missing."));
		Direction facing = Direction.WEST;
		Item inputItem = process.finalBlock().asItem();
		Item outputItem = process.roughBoards().item().asItem();
		ItemStack frameSaw = ModItems.FRAME_SAW.getDefaultInstance();
		frameSaw.setDamageValue(17);
		FakePlayer player = creativePlayer(
			helper,
			frameSaw,
			"btb-saw-creative-full-stack"
		);
		prepareFullBatchInventory(player, inputItem);
		clearItemEntities(helper, TARGET);
		helper.setBlock(
			TARGET,
			loaded(facing, process.type(), SawingTrestleStateMachine.FINAL_CUT_STAGE)
		);

		assertSuccess(
			helper,
			useHeldItem(helper, player, TARGET),
			"creative full-stack terminal saw stroke"
		);
		helper.assertBlockState(TARGET, empty(facing));
		helper.assertValueEqual(
			player.getInventory().countItem(inputItem),
			63,
			"creative full-stack sawing consumed matching beams"
		);
		helper.assertValueEqual(
			player.getInventory().countItem(Items.COBBLESTONE),
			FULL_INVENTORY_FILLER_COUNT,
			"creative full-stack sawing changed unrelated filler"
		);
		helper.assertValueEqual(
			player.getMainHandItem().getDamageValue(),
			17,
			"creative full-stack sawing damaged the frame saw"
		);
		assertWorldDropsExactly(
			helper,
			TARGET,
			Map.of(outputItem, process.roughBoards().count() * 64),
			"creative full-stack sawing output"
		);
		helper.assertTrue(
			itemEntitiesNear(helper, TARGET).stream()
				.allMatch(entity -> entity.getItem().getCount() <= entity.getItem().getMaxStackSize()),
			"creative full-stack sawing produced an oversized item stack"
		);
		clearItemEntities(helper, TARGET);
	}

	private static void assertSawingOutputSurvivesDisabledBlockDrops(
		GameTestHelper helper
	) {
		TimberProcess process = TimberProcessingRegistry.byType(TimberType.CHESTNUT)
			.orElseThrow(() -> new IllegalStateException("Chestnut timber process is missing."));
		Direction facing = Direction.SOUTH;
		Item inputItem = process.finalBlock().asItem();
		Item outputItem = process.roughBoards().item().asItem();
		int additionalInputCount = 2;
		int expectedOutputCount = process.roughBoards().count() * (1 + additionalInputCount);
		boolean originalBlockDrops = helper.getLevel().getGameRules().get(
			GameRules.BLOCK_DROPS
		);

		clearItemEntities(helper, TARGET);
		helper.setBlock(
			TARGET,
			loaded(facing, process.type(), SawingTrestleStateMachine.FINAL_CUT_STAGE)
		);
		FakePlayer player = survivalPlayer(
			helper,
			ModItems.FRAME_SAW.getDefaultInstance(),
			"btb-saw-disabled-block-drops"
		);
		player.getInventory().setItem(1, new ItemStack(inputItem, additionalInputCount));

		try {
			helper.getLevel().getGameRules().set(
				GameRules.BLOCK_DROPS,
				false,
				helper.getLevel().getServer()
			);
			helper.assertFalse(
				helper.getLevel().getGameRules().get(GameRules.BLOCK_DROPS),
				"doBlockDrops did not disable for the sawing-output probe"
			);
			assertSuccess(
				helper,
				useHeldItem(helper, player, TARGET),
				"disabled-doBlockDrops terminal saw batch"
			);
			helper.assertBlockState(TARGET, empty(facing));
			helper.assertValueEqual(
				player.getInventory().countItem(inputItem),
				0,
				"disabled-doBlockDrops terminal saw retained paid inputs"
			);
			assertWorldDropsExactly(
				helper,
				TARGET,
				Map.of(outputItem, expectedOutputCount),
				"disabled-doBlockDrops terminal saw output"
			);
		} finally {
			helper.getLevel().getGameRules().set(
				GameRules.BLOCK_DROPS,
				originalBlockDrops,
				helper.getLevel().getServer()
			);
		}

		helper.assertValueEqual(
			helper.getLevel().getGameRules().get(GameRules.BLOCK_DROPS),
			originalBlockDrops,
			"doBlockDrops restoration after sawing-output probe"
		);
		clearItemEntities(helper, TARGET);
	}

	@GameTest(maxTicks = 40)
	public void invalidItemsAreSideEffectFree(GameTestHelper helper) {
		assertPassWithoutSideEffects(
			helper,
			empty(Direction.NORTH),
			Items.STICK.getDefaultInstance(),
			"stick on empty trestles",
			0
		);
		assertPassWithoutSideEffects(
			helper,
			empty(Direction.EAST),
			ModItems.BROAD_AXE.getDefaultInstance(),
			"broad axe on empty trestles",
			0
		);
		assertPassWithoutSideEffects(
			helper,
			empty(Direction.SOUTH),
			ModItems.FRAME_SAW.getDefaultInstance(),
			"frame saw on empty trestles",
			0
		);
		assertPassWithoutSideEffects(
			helper,
			empty(Direction.WEST),
			ItemStack.EMPTY,
			"empty hand on empty trestles",
			0
		);

		int playerIndex = 0;
		for (TimberProcess process : TimberProcessingRegistry.all()) {
			BlockState state = loaded(Direction.NORTH, process.type(), 1);
			assertPassWithoutSideEffects(
				helper,
				state,
				Items.STICK.getDefaultInstance(),
				process.type() + " loaded trestles with stick",
				playerIndex++
			);
			assertPassWithoutSideEffects(
				helper,
				state,
				ModItems.BROAD_AXE.getDefaultInstance(),
				process.type() + " loaded trestles with broad axe",
				playerIndex++
			);

			clearItemEntities(helper, TARGET);
			helper.setBlock(TARGET, state);
			helper.setBlock(ABOVE_TARGET, Blocks.AIR);
			FakePlayer beamPlayer = survivalPlayer(
				helper,
				process.finalBlock().asItem().getDefaultInstance(),
				"btb-reject-" + playerIndex++
			);
			ItemStack beforeBeam = beamPlayer.getMainHandItem().copy();
			InteractionResult beamResult = useHeldItem(helper, beamPlayer, TARGET);
			assertSuccess(helper, beamResult, process.type() + " rejected beam handling");
			helper.assertBlockState(TARGET, state);
			helper.assertBlockState(ABOVE_TARGET, Blocks.AIR.defaultBlockState());
			helper.assertTrue(
				ItemStack.matches(beforeBeam, beamPlayer.getMainHandItem()),
				process.type() + " rejected beam changed the held stack"
			);
			assertNoItemEntities(helper, TARGET, process.type() + " rejected beam");
		}

		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void emptyHandRecoversEveryRegisteredWoodAtEveryStage(GameTestHelper helper) {
		int playerIndex = 0;
		for (TimberProcess process : TimberProcessingRegistry.all()) {
			for (int stage = 0; stage <= SawingTrestleStateMachine.FINAL_CUT_STAGE; stage++) {
				Direction facing = HORIZONTAL_FACINGS.get(
					(process.type().ordinal() + stage) % HORIZONTAL_FACINGS.size()
				);
				String label = process.type() + " recovery stage " + stage;
				clearItemEntities(helper, TARGET);
				helper.setBlock(TARGET, loaded(facing, process.type(), stage));
				FakePlayer player = survivalPlayer(
					helper,
					ItemStack.EMPTY,
					"btb-recov-" + playerIndex++
				);

				InteractionResult result = useHeldItem(helper, player, TARGET);
				assertSuccess(helper, result, label);
				helper.assertBlockState(TARGET, empty(facing));
				helper.assertValueEqual(
					player.getInventory().countItem(process.finalBlock().asItem()),
					1,
					label + " recovered beam inventory count"
				);
				helper.assertValueEqual(
					player.getInventory().countItem(process.roughBoards().item().asItem()),
					0,
					label + " unexpected rough-board inventory count"
				);
				helper.assertValueEqual(
					player.getInventory().countItem(ModBlocks.SAWING_TRESTLES.asItem()),
					0,
					label + " unexpected trestles inventory count"
				);
				assertNoItemEntities(helper, TARGET, label);
			}
		}

		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void loadedBreakReturnsTrestlesAndMappedBeamAtEveryStage(GameTestHelper helper) {
		FakePlayer breaker = survivalPlayer(helper, ItemStack.EMPTY, "btb-saw-break");
		for (TimberProcess process : TimberProcessingRegistry.all()) {
			for (int stage = 0; stage <= SawingTrestleStateMachine.FINAL_CUT_STAGE; stage++) {
				Direction facing = HORIZONTAL_FACINGS.get(
					(process.type().ordinal() + stage) % HORIZONTAL_FACINGS.size()
				);
				String label = process.type() + " loaded break stage " + stage;
				clearItemEntities(helper, TARGET);
				BlockState state = loaded(facing, process.type(), stage);
				helper.setBlock(TARGET, state);
				Map<Item, Integer> expected = Map.of(
					ModBlocks.SAWING_TRESTLES.asItem(),
					1,
					process.finalBlock().asItem(),
					1
				);

				List<ItemStack> definedDrops = Block.getDrops(
					state,
					helper.getLevel(),
					helper.absolutePos(TARGET),
					null,
					breaker,
					ItemStack.EMPTY
				);
				helper.assertValueEqual(
					stackCounts(definedDrops),
					expected,
					label + " declared loot"
				);

				helper.assertTrue(
					helper.getLevel().destroyBlock(helper.absolutePos(TARGET), true, breaker),
					label + " block was not destroyed"
				);
				helper.assertBlockState(TARGET, Blocks.AIR.defaultBlockState());
				assertWorldDropsExactly(helper, TARGET, expected, label + " live break");
				clearItemEntities(helper, TARGET);
			}
		}

		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void allSawingStatesRoundTripThroughNbt(GameTestHelper helper) {
		helper.assertValueEqual(
			LoadedSawingTrestlesBlock.CUT_STAGE.getPossibleValues(),
			List.of(0, 1, 2, 3),
			"cut_stage values"
		);
		helper.assertValueEqual(
			LoadedSawingTrestlesBlock.WOOD_TYPE.getPossibleValues(),
			List.of(TimberType.CHESTNUT, TimberType.OAK, TimberType.SPRUCE),
			"wood_type values"
		);

		int stateCount = 0;
		for (Direction facing : HORIZONTAL_FACINGS) {
			assertNbtRoundTrip(helper, empty(facing), "empty facing " + facing);
			stateCount++;
			for (TimberType woodType : TimberType.values()) {
				for (int stage : LoadedSawingTrestlesBlock.CUT_STAGE.getPossibleValues()) {
					assertNbtRoundTrip(
						helper,
						loaded(facing, woodType, stage),
						woodType + " loaded facing " + facing + " stage " + stage
					);
					stateCount++;
				}
			}
		}
		helper.assertValueEqual(stateCount, 52, "persisted sawing-state count");

		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void sawDurabilityAndEightTickCooldownGateOneStroke(GameTestHelper helper) {
		TimberProcess process = TimberProcessingRegistry.byType(TimberType.CHESTNUT)
			.orElseThrow(() -> new IllegalStateException("Chestnut timber process is missing."));
		Direction facing = Direction.WEST;
		clearItemEntities(helper, TARGET);
		helper.setBlock(TARGET, loaded(facing, process.type(), 0));
		FakePlayer player = survivalPlayer(
			helper,
			ModItems.FRAME_SAW.getDefaultInstance(),
			"btb-saw-cool"
		);
		ItemStack frameSaw = player.getMainHandItem();

		InteractionResult first = useHeldItem(helper, player, TARGET);
		assertSuccess(helper, first, "first cooldown stroke");
		helper.assertBlockState(TARGET, loaded(facing, process.type(), 1));
		helper.assertValueEqual(frameSaw.getDamageValue(), 1, "first cooldown stroke damage");
		helper.assertTrue(
			player.getCooldowns().isOnCooldown(frameSaw),
			"first stroke did not start cooldown"
		);

		InteractionResult blocked = useHeldItem(helper, player, TARGET);
		assertPass(helper, blocked, "immediate cooldown stroke");
		helper.assertBlockState(TARGET, loaded(facing, process.type(), 1));
		helper.assertValueEqual(frameSaw.getDamageValue(), 1, "blocked cooldown stroke damage");
		assertNoItemEntities(helper, TARGET, "blocked cooldown stroke");

		for (int tick = 1; tick < EXPECTED_COOLDOWN_TICKS; tick++) {
			player.getCooldowns().tick();
			helper.assertTrue(
				player.getCooldowns().isOnCooldown(frameSaw),
				"cooldown ended early at tick " + tick
			);
		}
		player.getCooldowns().tick();
		helper.assertFalse(
			player.getCooldowns().isOnCooldown(frameSaw),
			"cooldown remained after tick " + EXPECTED_COOLDOWN_TICKS
		);

		InteractionResult afterCooldown = useHeldItem(helper, player, TARGET);
		assertSuccess(helper, afterCooldown, "post-cooldown stroke");
		helper.assertBlockState(TARGET, loaded(facing, process.type(), 2));
		helper.assertValueEqual(frameSaw.getDamageValue(), 2, "post-cooldown stroke damage");
		assertNoItemEntities(helper, TARGET, "post-cooldown stroke");

		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void twoPlayersSerializeTerminalStrokeWithoutDuplicateBoards(GameTestHelper helper) {
		int scenarioIndex = 0;
		int additionalInputsPerPlayer = 2;
		for (TimberProcess process : TimberProcessingRegistry.all()) {
			for (boolean playerAWins : List.of(true, false)) {
				Direction facing = HORIZONTAL_FACINGS.get(scenarioIndex % HORIZONTAL_FACINGS.size());
				String label = process.type() + (playerAWins ? " terminal A then B" : " terminal B then A");
				clearItemEntities(helper, TARGET);
				helper.setBlock(
					TARGET,
					loaded(facing, process.type(), SawingTrestleStateMachine.FINAL_CUT_STAGE)
				);
				FakePlayer playerA = survivalPlayer(
					helper,
					ModItems.FRAME_SAW.getDefaultInstance(),
					"btb-term-a" + scenarioIndex
				);
				FakePlayer playerB = survivalPlayer(
					helper,
					ModItems.FRAME_SAW.getDefaultInstance(),
					"btb-term-b" + scenarioIndex
				);
				Item inputItem = process.finalBlock().asItem();
				playerA.getInventory().setItem(
					1,
					new ItemStack(inputItem, additionalInputsPerPlayer)
				);
				playerB.getInventory().setItem(
					1,
					new ItemStack(inputItem, additionalInputsPerPlayer)
				);
				FakePlayer winner = playerAWins ? playerA : playerB;
				FakePlayer loser = playerAWins ? playerB : playerA;
				ItemStack loserToolBefore = loser.getMainHandItem().copy();
				BlockHitResult hit = hitResult(helper, TARGET);
				helper.assertValueEqual(
					winner.getInventory().countItem(inputItem),
					additionalInputsPerPlayer,
					label + " winner starting input count"
				);
				helper.assertValueEqual(
					loser.getInventory().countItem(inputItem),
					additionalInputsPerPlayer,
					label + " loser starting input count"
				);

				InteractionResult winnerResult = useHeldItem(helper, winner, hit);
				assertSuccess(helper, winnerResult, label + " winner");
				helper.assertBlockState(TARGET, empty(facing));
				Map<Item, Integer> expectedOutput = Map.of(
					process.roughBoards().item().asItem(),
					process.roughBoards().count() * (1 + additionalInputsPerPlayer)
				);
				assertWorldDropsExactly(helper, TARGET, expectedOutput, label + " winner output");
				helper.assertValueEqual(
					winner.getInventory().countItem(inputItem),
					0,
					label + " winner remaining input count"
				);

				InteractionResult loserResult = useHeldItem(helper, loser, hit);
				assertPass(helper, loserResult, label + " loser");
				helper.assertBlockState(TARGET, empty(facing));
				assertWorldDropsExactly(helper, TARGET, expectedOutput, label + " final output");
				helper.assertValueEqual(
					winner.getMainHandItem().getDamageValue(),
					1 + additionalInputsPerPlayer * 4,
					label + " winner damage"
				);
				helper.assertTrue(
					ItemStack.matches(loserToolBefore, loser.getMainHandItem()),
					label + " loser tool changed"
				);
				helper.assertValueEqual(
					loser.getInventory().countItem(inputItem),
					additionalInputsPerPlayer,
					label + " loser input count changed"
				);
				helper.assertTrue(
					winner.getCooldowns().isOnCooldown(winner.getMainHandItem()),
					label + " winner cooldown missing"
				);
				helper.assertFalse(
					loser.getCooldowns().isOnCooldown(loser.getMainHandItem()),
					label + " loser cooldown unexpectedly started"
				);
				clearItemEntities(helper, TARGET);
				scenarioIndex++;
			}
		}

		helper.succeed();
	}

	private static BlockState empty(Direction facing) {
		return ModBlocks.SAWING_TRESTLES.defaultBlockState()
			.setValue(AbstractSawingTrestlesBlock.FACING, facing);
	}

	private static BlockState loaded(Direction facing, TimberType woodType, int stage) {
		return ModBlocks.LOADED_SAWING_TRESTLES.defaultBlockState()
			.setValue(AbstractSawingTrestlesBlock.FACING, facing)
			.setValue(LoadedSawingTrestlesBlock.WOOD_TYPE, woodType)
			.setValue(LoadedSawingTrestlesBlock.CUT_STAGE, stage);
	}

	private static FakePlayer survivalPlayer(
		GameTestHelper helper,
		ItemStack heldStack,
		String name
	) {
		FakePlayer player = FakePlayer.get(
			helper.getLevel(),
			new GameProfile(UUID.randomUUID(), name)
		);
		player.setGameMode(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, heldStack);
		helper.assertValueEqual(player.gameMode(), GameType.SURVIVAL, name + " game mode");
		helper.assertFalse(player.hasInfiniteMaterials(), name + " has infinite materials");
		return player;
	}

	private static FakePlayer creativePlayer(
		GameTestHelper helper,
		ItemStack heldStack,
		String name
	) {
		FakePlayer player = FakePlayer.get(
			helper.getLevel(),
			new GameProfile(UUID.randomUUID(), name)
		);
		player.setGameMode(GameType.CREATIVE);
		player.setItemInHand(InteractionHand.MAIN_HAND, heldStack);
		helper.assertValueEqual(player.gameMode(), GameType.CREATIVE, name + " game mode");
		helper.assertTrue(player.hasInfiniteMaterials(), name + " lacks infinite materials");
		return player;
	}

	private static void prepareFullBatchInventory(FakePlayer player, Item input) {
		player.getInventory().setItem(1, new ItemStack(input, 31));
		player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(input, 32));
		for (int slot = 2; slot < MAIN_INVENTORY_SIZE; slot++) {
			player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
		}
	}

	private static InteractionResult useHeldItem(
		GameTestHelper helper,
		FakePlayer player,
		BlockPos relativePos
	) {
		return useHeldItem(helper, player, hitResult(helper, relativePos));
	}

	private static InteractionResult useHeldItem(
		GameTestHelper helper,
		FakePlayer player,
		BlockHitResult hit
	) {
		return player.gameMode.useItemOn(
			player,
			helper.getLevel(),
			player.getMainHandItem(),
			InteractionHand.MAIN_HAND,
			hit
		);
	}

	private static BlockHitResult hitResult(GameTestHelper helper, BlockPos relativePos) {
		BlockPos absolutePos = helper.absolutePos(relativePos);
		return new BlockHitResult(
			Vec3.atCenterOf(absolutePos),
			Direction.UP,
			absolutePos,
			false
		);
	}

	private static void clearCooldown(FakePlayer player, ItemStack stack) {
		player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(stack));
	}

	private static void assertPassWithoutSideEffects(
		GameTestHelper helper,
		BlockState state,
		ItemStack heldStack,
		String label,
		int playerIndex
	) {
		clearItemEntities(helper, TARGET);
		helper.setBlock(TARGET, state);
		FakePlayer player = survivalPlayer(
			helper,
			heldStack,
			"btb-invalid-" + playerIndex
		);
		ItemStack beforeStack = player.getMainHandItem().copy();

		InteractionResult result = useHeldItem(helper, player, TARGET);
		assertPass(helper, result, label);
		helper.assertBlockState(TARGET, state);
		helper.assertTrue(
			ItemStack.matches(beforeStack, player.getMainHandItem()),
			label + " changed the held stack"
		);
		helper.assertFalse(
			player.getCooldowns().isOnCooldown(player.getMainHandItem()),
			label + " started a cooldown"
		);
		assertNoItemEntities(helper, TARGET, label);
	}

	private static void assertNbtRoundTrip(
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
		helper.assertValueEqual(decoded, original, label + " NBT round trip");
	}

	private static Map<Item, Integer> stackCounts(Iterable<ItemStack> stacks) {
		Map<Item, Integer> counts = new LinkedHashMap<>();
		for (ItemStack stack : stacks) {
			if (!stack.isEmpty()) {
				counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
			}
		}
		return counts;
	}

	private static void assertWorldDropsExactly(
		GameTestHelper helper,
		BlockPos relativePos,
		Map<Item, Integer> expected,
		String label
	) {
		Map<Item, Integer> actual = new LinkedHashMap<>();
		for (ItemEntity entity : itemEntitiesNear(helper, relativePos)) {
			ItemStack stack = entity.getItem();
			actual.merge(stack.getItem(), stack.getCount(), Integer::sum);
		}
		helper.assertValueEqual(actual, expected, label + " world-drop multiset");
	}

	private static void assertNoItemEntities(
		GameTestHelper helper,
		BlockPos relativePos,
		String label
	) {
		helper.assertValueEqual(
			itemEntitiesNear(helper, relativePos).size(),
			0,
			label + " item entity count"
		);
	}

	private static List<ItemEntity> itemEntitiesNear(
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

	private static void clearItemEntities(GameTestHelper helper, BlockPos relativePos) {
		itemEntitiesNear(helper, relativePos).forEach(ItemEntity::discard);
	}

	private static void assertSuccess(
		GameTestHelper helper,
		InteractionResult result,
		String label
	) {
		helper.assertTrue(
			result instanceof InteractionResult.Success,
			label + " did not return Success: " + result
		);
	}

	private static void assertPass(
		GameTestHelper helper,
		InteractionResult result,
		String label
	) {
		helper.assertTrue(
			result instanceof InteractionResult.Pass,
			label + " did not return PASS: " + result
		);
	}

}
