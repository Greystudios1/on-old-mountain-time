package net.beforetheblight.gametest;

import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import net.beforetheblight.block.HewingLogBlock;
import net.beforetheblight.interaction.TimberProcessingRegistry;
import net.beforetheblight.interaction.TimberProcessingRegistry.TimberProcess;
import net.beforetheblight.interaction.TimberSplitKind;
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
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.gametest.framework.GameTestHelper;

public final class HewingGameTests {
	private static final BlockPos TARGET = new BlockPos(3, 2, 3);
	private static final double DROP_SEARCH_RADIUS = 1.5;
	private static final int MAIN_INVENTORY_SIZE = 36;
	private static final int FULL_INVENTORY_FILLER_COUNT = 34 * 64;
	private static final List<HewingCase> HEWING_CASES = List.of(
		new HewingCase(
			"chestnut",
			TimberType.CHESTNUT,
			ModBlocks.CHESTNUT_LOG,
			ModBlocks.STRIPPED_CHESTNUT_LOG,
			ModBlocks.CHESTNUT_HEWING_LOG,
			ModBlocks.HEWN_CHESTNUT_BEAM,
			ModBlocks.ROUGH_CHESTNUT_BOARDS
		),
		new HewingCase(
			"oak",
			TimberType.OAK,
			Blocks.OAK_LOG,
			Blocks.STRIPPED_OAK_LOG,
			ModBlocks.OAK_HEWING_LOG,
			ModBlocks.HEWN_OAK_BEAM,
			ModBlocks.ROUGH_OAK_BOARDS
		),
		new HewingCase(
			"spruce",
			TimberType.SPRUCE,
			Blocks.SPRUCE_LOG,
			Blocks.STRIPPED_SPRUCE_LOG,
			ModBlocks.SPRUCE_HEWING_LOG,
			ModBlocks.HEWN_SPRUCE_BEAM,
			ModBlocks.ROUGH_SPRUCE_BOARDS
		)
	);

	@GameTest(maxTicks = 20)
	public void everyTimberMappingCarriesTheCompleteProcessingPath(GameTestHelper helper) {
		helper.assertValueEqual(
			TimberProcessingRegistry.all().size(),
			HEWING_CASES.size(),
			"registered timber-process count"
		);
		for (HewingCase hewingCase : HEWING_CASES) {
			String label = hewingCase.label();
			TimberProcess process = TimberProcessingRegistry.byType(hewingCase.type())
				.orElseThrow(() -> new IllegalStateException(label + " timber process is missing."));
			helper.assertValueEqual(process.sourceBlock(), hewingCase.source(), label + " source log");
			helper.assertValueEqual(process.stagedBlock(), hewingCase.staged(), label + " staged log");
			helper.assertValueEqual(process.finalBlock(), hewingCase.beam(), label + " hewn beam");
			helper.assertValueEqual(process.partialReturnBlock(), hewingCase.source(), label + " partial return");
			helper.assertValueEqual(process.roughBoards().item(), hewingCase.roughBoards(), label + " rough boards");
			helper.assertValueEqual(process.roughBoards().count(), 4, label + " rough-board output count");
			if (hewingCase.type() == TimberType.CHESTNUT) {
				helper.assertValueEqual(process.splitOutputs().size(), 2, label + " split-output count");
				helper.assertValueEqual(
					process.splitOutputs().get(TimberSplitKind.SHINGLES).item(),
					ModBlocks.CHESTNUT_SHINGLES,
					label + " shingle output"
				);
				helper.assertValueEqual(
					process.splitOutputs().get(TimberSplitKind.SHINGLES).count(),
					4,
					label + " shingle count"
				);
				helper.assertValueEqual(
					process.splitOutputs().get(TimberSplitKind.RAILS).item(),
					ModBlocks.SPLIT_CHESTNUT_RAILS,
					label + " rail output"
				);
				helper.assertValueEqual(
					process.splitOutputs().get(TimberSplitKind.RAILS).count(),
					2,
					label + " rail count"
				);
			} else {
				helper.assertTrue(process.splitOutputs().isEmpty(), label + " split outputs must remain unsupported");
			}
			helper.assertValueEqual(
				TimberProcessingRegistry.byBeam(hewingCase.beam()).orElseThrow(),
				process,
				label + " beam block lookup"
			);
			helper.assertValueEqual(
				TimberProcessingRegistry.byBeam(hewingCase.beam().asItem().getDefaultInstance()).orElseThrow(),
				process,
				label + " beam item lookup"
			);
		}

		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void fourStrikesPreserveEveryAxisAndDamageOnce(GameTestHelper helper) {
		int caseIndex = 0;
		for (HewingCase hewingCase : HEWING_CASES) {
			int axisIndex = 0;
			for (Direction.Axis axis : Direction.Axis.values()) {
				BlockPos target = new BlockPos(2 + axisIndex * 2, 2, 2 + caseIndex * 2);
				FakePlayer player = survivalPlayer(
					helper,
					ModItems.BROAD_AXE.getDefaultInstance(),
					"btb-axis-" + caseIndex + axisIndex
				);
				ItemStack broadAxe = player.getMainHandItem();
				helper.assertTrue(broadAxe.isDamageableItem(), "Broad axe is not damageable");
				helper.setBlock(
					target,
					hewingCase.source().defaultBlockState()
						.setValue(RotatedPillarBlock.AXIS, axis)
				);

				for (int strike = 1; strike <= 4; strike++) {
					String label = hewingCase.label() + " axis " + axis + " strike " + strike;
					InteractionResult result = useHeldItem(helper, player, target);
					assertSuccess(helper, result, label);
					BlockState expected = strike <= 3
						? staged(hewingCase, axis, strike)
						: beam(hewingCase, axis);
					helper.assertBlockState(target, expected);
					helper.assertValueEqual(broadAxe.getDamageValue(), strike, label + " damage");
					assertNoItemEntities(helper, target, label);
				}

				String finalLabel = hewingCase.label() + " axis " + axis + " final beam use";
				ItemStack beforeFifthUse = broadAxe.copy();
				InteractionResult fifthResult = useHeldItem(helper, player, target);
				assertPass(helper, fifthResult, finalLabel);
				helper.assertBlockState(target, beam(hewingCase, axis));
				helper.assertTrue(
					ItemStack.matches(beforeFifthUse, player.getMainHandItem()),
					finalLabel + " changed the broad axe"
				);
				assertNoItemEntities(helper, target, finalLabel);
				axisIndex++;
			}
			caseIndex++;
		}

		HewingCase bulkCase = HEWING_CASES.getFirst();
		BlockPos bulkTarget = new BlockPos(8, 2, 8);
		FakePlayer bulkPlayer = survivalPlayer(
			helper,
			ModItems.BROAD_AXE.getDefaultInstance(),
			"btb-full-stack-hewing"
		);
		ItemStack bulkAxe = bulkPlayer.getMainHandItem();
		helper.assertValueEqual(
			bulkAxe.getMaxDamage(),
			256,
			"full-stack broad axe durability"
		);
		prepareFullBatchInventory(bulkPlayer, bulkCase.source().asItem());
		helper.setBlock(
			bulkTarget,
			bulkCase.source().defaultBlockState()
				.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y)
		);

		for (int strike = 1; strike <= 3; strike++) {
			String label = "full-stack hewing strike " + strike;
			assertSuccess(helper, useHeldItem(helper, bulkPlayer, bulkTarget), label);
			helper.assertBlockState(
				bulkTarget,
				staged(bulkCase, Direction.Axis.Y, strike)
			);
			helper.assertValueEqual(
				bulkAxe.getDamageValue(),
				strike,
				label + " damage"
			);
			helper.assertValueEqual(
				bulkPlayer.getInventory().countItem(bulkCase.source().asItem()),
				63,
				label + " unconsumed additional logs"
			);
			assertNoItemEntities(helper, bulkTarget, label);
		}

		assertSuccess(
			helper,
			useHeldItem(helper, bulkPlayer, bulkTarget),
			"full-stack terminal hewing strike"
		);
		helper.assertBlockState(
			bulkTarget,
			beam(bulkCase, Direction.Axis.Y)
		);
		helper.assertValueEqual(
			bulkPlayer.getInventory().countItem(bulkCase.source().asItem()),
			0,
			"full-stack consumed additional logs"
		);
		helper.assertValueEqual(
			bulkPlayer.getInventory().countItem(Items.COBBLESTONE),
			FULL_INVENTORY_FILLER_COUNT,
			"full-stack hewing changed unrelated full-inventory filler"
		);
		helper.assertTrue(
			bulkPlayer.getMainHandItem().isEmpty(),
			"256-durability broad axe did not break after 64 complete logs"
		);
		List<ItemEntity> bulkDrops = itemEntitiesNear(helper, bulkTarget);
		helper.assertTrue(
			bulkDrops.stream().allMatch(
				entity -> entity.getItem().is(bulkCase.beam().asItem())
			),
			"full-stack hewing spawned a non-beam item"
		);
		helper.assertValueEqual(
			bulkDrops.stream().mapToInt(entity -> entity.getItem().getCount()).sum(),
			63,
			"full-stack additional beam drops"
		);
		bulkDrops.forEach(ItemEntity::discard);

		HewingCase otherWoodCase = HEWING_CASES.get(1);
		BlockPos constrainedTarget = new BlockPos(8, 2, 4);
		ItemStack constrainedAxe = ModItems.BROAD_AXE.getDefaultInstance();
		constrainedAxe.setDamageValue(250);
		FakePlayer constrainedPlayer = survivalPlayer(
			helper,
			constrainedAxe,
			"btb-durability-limited-hewing"
		);
		constrainedPlayer.getInventory().setItem(
			1,
			new ItemStack(bulkCase.source(), 3)
		);
		constrainedPlayer.getInventory().setItem(
			2,
			new ItemStack(otherWoodCase.source(), 4)
		);
		constrainedPlayer.getInventory().setItem(
			3,
			new ItemStack(Items.COBBLESTONE, 5)
		);
		helper.setBlock(
			constrainedTarget,
			staged(bulkCase, Direction.Axis.X, 3)
		);

		assertSuccess(
			helper,
			useHeldItem(helper, constrainedPlayer, constrainedTarget),
			"durability-limited terminal hewing strike"
		);
		helper.assertBlockState(
			constrainedTarget,
			beam(bulkCase, Direction.Axis.X)
		);
		helper.assertValueEqual(
			constrainedPlayer.getMainHandItem().getDamageValue(),
			255,
			"durability-limited broad axe damage"
		);
		helper.assertValueEqual(
			constrainedPlayer.getInventory().countItem(bulkCase.source().asItem()),
			2,
			"durability-limited matching logs"
		);
		helper.assertValueEqual(
			constrainedPlayer.getInventory().countItem(otherWoodCase.source().asItem()),
			4,
			"durability-limited other-wood logs"
		);
		helper.assertValueEqual(
			constrainedPlayer.getInventory().countItem(Items.COBBLESTONE),
			5,
			"durability-limited unrelated items"
		);
		List<ItemEntity> constrainedDrops = itemEntitiesNear(helper, constrainedTarget);
		helper.assertTrue(
			constrainedDrops.stream().allMatch(
				entity -> entity.getItem().is(bulkCase.beam().asItem())
			),
			"durability-limited hewing spawned a non-beam item"
		);
		helper.assertValueEqual(
			constrainedDrops.stream().mapToInt(entity -> entity.getItem().getCount()).sum(),
			1,
			"durability-limited extra beam drops"
		);
		constrainedDrops.forEach(ItemEntity::discard);

		BlockPos creativeTarget = new BlockPos(8, 2, 6);
		ItemStack creativeAxe = ModItems.BROAD_AXE.getDefaultInstance();
		creativeAxe.setDamageValue(17);
		FakePlayer creative = creativePlayer(
			helper,
			creativeAxe,
			"btb-creative-full-stack-hewing"
		);
		prepareFullBatchInventory(creative, bulkCase.source().asItem());
		helper.setBlock(
			creativeTarget,
			staged(bulkCase, Direction.Axis.Z, 3)
		);

		assertSuccess(
			helper,
			useHeldItem(helper, creative, creativeTarget),
			"creative full-stack terminal hewing strike"
		);
		helper.assertBlockState(
			creativeTarget,
			beam(bulkCase, Direction.Axis.Z)
		);
		helper.assertValueEqual(
			creative.getInventory().countItem(bulkCase.source().asItem()),
			63,
			"creative full-stack hewing consumed matching logs"
		);
		helper.assertValueEqual(
			creative.getInventory().countItem(Items.COBBLESTONE),
			FULL_INVENTORY_FILLER_COUNT,
			"creative full-stack hewing changed unrelated filler"
		);
		helper.assertValueEqual(
			creative.getMainHandItem().getDamageValue(),
			17,
			"creative full-stack hewing damaged the broad axe"
		);
		List<ItemEntity> creativeDrops = itemEntitiesNear(helper, creativeTarget);
		helper.assertTrue(
			creativeDrops.stream().allMatch(
				entity -> entity.getItem().is(bulkCase.beam().asItem())
			),
			"creative full-stack hewing spawned a non-beam item"
		);
		helper.assertValueEqual(
			creativeDrops.stream().mapToInt(entity -> entity.getItem().getCount()).sum(),
			63,
			"creative full-stack additional beam drops"
		);
		creativeDrops.forEach(ItemEntity::discard);

		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void invalidTargetsReturnPassWithoutSideEffects(GameTestHelper helper) {
		List<InvalidTarget> targets = List.of(
			new InvalidTarget("stone", Blocks.STONE.defaultBlockState()),
			new InvalidTarget(
				"unregistered birch log",
				Blocks.BIRCH_LOG.defaultBlockState()
					.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X)
			),
			new InvalidTarget("chestnut planks", ModBlocks.CHESTNUT_PLANKS.defaultBlockState()),
			new InvalidTarget("final beam", beam(HEWING_CASES.getFirst(), Direction.Axis.Z))
		);

		for (int index = 0; index < targets.size(); index++) {
			InvalidTarget target = targets.get(index);
			FakePlayer player = survivalPlayer(
				helper,
				ModItems.BROAD_AXE.getDefaultInstance(),
				"btb-invalid-" + index
			);
			helper.setBlock(TARGET, target.state());
			BlockState beforeState = helper.getBlockState(TARGET);
			ItemStack beforeStack = player.getMainHandItem().copy();

			InteractionResult result = useHeldItem(helper, player, TARGET);
			assertPass(helper, result, target.label());
			helper.assertBlockState(TARGET, beforeState);
			helper.assertTrue(
				ItemStack.matches(beforeStack, player.getMainHandItem()),
				"Invalid target changed the held stack: " + target.label()
			);
			assertNoItemEntities(helper, TARGET, target.label());
		}

		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void ordinaryIronAxesStillStrip(GameTestHelper helper) {
		int caseIndex = 0;
		for (HewingCase hewingCase : HEWING_CASES) {
			int axisIndex = 0;
			for (Direction.Axis axis : Direction.Axis.values()) {
				BlockPos target = new BlockPos(2 + axisIndex * 2, 2, 2 + caseIndex * 2);
				FakePlayer player = survivalPlayer(
					helper,
					Items.IRON_AXE.getDefaultInstance(),
					"btb-strip-" + caseIndex + axisIndex
				);
				helper.setBlock(
					target,
					hewingCase.source().defaultBlockState()
						.setValue(RotatedPillarBlock.AXIS, axis)
				);

				InteractionResult result = useHeldItem(helper, player, target);
				String label = "ordinary " + hewingCase.label() + " stripping on axis " + axis;
				assertSuccess(helper, result, label);
				helper.assertBlockState(
					target,
					hewingCase.stripped().defaultBlockState()
						.setValue(RotatedPillarBlock.AXIS, axis)
				);
				helper.assertValueEqual(
					player.getMainHandItem().getDamageValue(),
					1,
					label + " damage"
				);
				assertNoItemEntities(helper, target, label);
				axisIndex++;
			}
			caseIndex++;
		}

		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void partialAndFinalBlocksDropExactlyOneItem(GameTestHelper helper) {
		FakePlayer breaker = survivalPlayer(helper, ItemStack.EMPTY, "btb-drop-check");

		for (HewingCase hewingCase : HEWING_CASES) {
			for (Direction.Axis axis : Direction.Axis.values()) {
				for (int stage = 1; stage <= 3; stage++) {
					assertExactBreakDrop(
						helper,
						breaker,
						staged(hewingCase, axis, stage),
						hewingCase.source().asItem(),
						hewingCase.label() + " partial axis " + axis + " stage " + stage
					);
				}

				assertExactBreakDrop(
					helper,
					breaker,
					beam(hewingCase, axis),
					hewingCase.beam().asItem(),
					hewingCase.label() + " final beam axis " + axis
				);
			}
		}

		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void stagedStatesRoundTripThroughNbt(GameTestHelper helper) {
		helper.assertValueEqual(
			HewingLogBlock.HEWING_STAGE.getPossibleValues(),
			List.of(1, 2, 3),
			"hewing_stage values"
		);

		for (HewingCase hewingCase : HEWING_CASES) {
			for (Direction.Axis axis : Direction.Axis.values()) {
				for (int stage = 1; stage <= 3; stage++) {
					assertNbtRoundTrip(
						helper,
						staged(hewingCase, axis, stage),
						hewingCase.label() + " partial axis " + axis + " stage " + stage
					);
				}
				assertNbtRoundTrip(
					helper,
					beam(hewingCase, axis),
					hewingCase.label() + " final beam axis " + axis
				);
			}
		}

		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void twoPlayersSerializeWithoutDuplicateFinalTransition(GameTestHelper helper) {
		int caseIndex = 0;
		for (HewingCase hewingCase : HEWING_CASES) {
			int z = 2 + caseIndex * 4;
			String label = hewingCase.label();
			BlockPos sourceTarget = new BlockPos(2, 2, z);
			FakePlayer sourceFirst = survivalPlayer(
				helper,
				ModItems.BROAD_AXE.getDefaultInstance(),
				"btb-s" + caseIndex + "a"
			);
			FakePlayer sourceSecond = survivalPlayer(
				helper,
				ModItems.BROAD_AXE.getDefaultInstance(),
				"btb-s" + caseIndex + "b"
			);
			helper.setBlock(
				sourceTarget,
				hewingCase.source().defaultBlockState()
					.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X)
			);
			BlockHitResult sourceHit = hitResult(helper, sourceTarget);

			InteractionResult sourceFirstResult = useHeldItem(helper, sourceFirst, sourceHit);
			assertSuccess(helper, sourceFirstResult, label + " first serialized source strike");
			helper.assertBlockState(sourceTarget, staged(hewingCase, Direction.Axis.X, 1));
			helper.assertValueEqual(
				sourceFirst.getMainHandItem().getDamageValue(),
				1,
				label + " first source player damage"
			);
			helper.assertValueEqual(
				sourceSecond.getMainHandItem().getDamageValue(),
				0,
				label + " second source player damage before use"
			);

			InteractionResult sourceSecondResult = useHeldItem(helper, sourceSecond, sourceHit);
			assertSuccess(helper, sourceSecondResult, label + " second serialized source strike");
			helper.assertBlockState(sourceTarget, staged(hewingCase, Direction.Axis.X, 2));
			helper.assertValueEqual(
				sourceFirst.getMainHandItem().getDamageValue(),
				1,
				label + " first source player final damage"
			);
			helper.assertValueEqual(
				sourceSecond.getMainHandItem().getDamageValue(),
				1,
				label + " second source player final damage"
			);
			assertNoItemEntities(helper, sourceTarget, label + " serialized source strikes");

			assertTerminalCollision(
				helper,
				hewingCase,
				new BlockPos(4, 2, z),
				Direction.Axis.Y,
				true,
				caseIndex,
				label + " terminal A then B"
			);
			assertTerminalCollision(
				helper,
				hewingCase,
				new BlockPos(6, 2, z),
				Direction.Axis.Z,
				false,
				caseIndex,
				label + " terminal B then A"
			);
			caseIndex++;
		}

		helper.succeed();
	}

	private static void assertTerminalCollision(
		GameTestHelper helper,
		HewingCase hewingCase,
		BlockPos target,
		Direction.Axis axis,
		boolean firstPlayerWins,
		int caseIndex,
		String label
	) {
		FakePlayer playerA = survivalPlayer(
			helper,
			ModItems.BROAD_AXE.getDefaultInstance(),
			"btb-t" + caseIndex + (firstPlayerWins ? "a1" : "a2")
		);
		FakePlayer playerB = survivalPlayer(
			helper,
			ModItems.BROAD_AXE.getDefaultInstance(),
			"btb-t" + caseIndex + (firstPlayerWins ? "b1" : "b2")
		);
		playerA.getInventory().setItem(
			1,
			new ItemStack(hewingCase.source(), 3)
		);
		playerB.getInventory().setItem(
			1,
			new ItemStack(hewingCase.source(), 3)
		);
		helper.setBlock(target, staged(hewingCase, axis, 3));
		BlockHitResult hit = hitResult(helper, target);
		FakePlayer winner = firstPlayerWins ? playerA : playerB;
		FakePlayer loser = firstPlayerWins ? playerB : playerA;

		InteractionResult winnerResult = useHeldItem(helper, winner, hit);
		InteractionResult loserResult = useHeldItem(helper, loser, hit);

		assertSuccess(helper, winnerResult, label + " winner");
		assertPass(helper, loserResult, label + " loser");
		helper.assertBlockState(target, beam(hewingCase, axis));
		helper.assertValueEqual(winner.getMainHandItem().getDamageValue(), 13, label + " winner damage");
		helper.assertValueEqual(loser.getMainHandItem().getDamageValue(), 0, label + " loser damage");
		helper.assertValueEqual(
			winner.getInventory().countItem(hewingCase.source().asItem()),
			0,
			label + " winner remaining extra logs"
		);
		helper.assertValueEqual(
			loser.getInventory().countItem(hewingCase.source().asItem()),
			3,
			label + " loser remaining extra logs"
		);
		List<ItemEntity> drops = itemEntitiesNear(helper, target);
		helper.assertTrue(
			drops.stream().allMatch(
				entity -> entity.getItem().is(hewingCase.beam().asItem())
			),
			label + " spawned a non-beam item"
		);
		helper.assertValueEqual(
			drops.stream().mapToInt(entity -> entity.getItem().getCount()).sum(),
			3,
			label + " winner extra beam drops"
		);
		drops.forEach(ItemEntity::discard);
	}

	private static void assertExactBreakDrop(
		GameTestHelper helper,
		FakePlayer breaker,
		BlockState state,
		Item expectedItem,
		String label
	) {
		helper.despawnItem(TARGET, 2.0);
		helper.setBlock(TARGET, state);
		BlockPos absolutePos = helper.absolutePos(TARGET);
		List<ItemStack> definedDrops = Block.getDrops(
			state,
			helper.getLevel(),
			absolutePos,
			null,
			breaker,
			ItemStack.EMPTY
		);
		helper.assertValueEqual(definedDrops.size(), 1, label + " loot stack count");
		helper.assertTrue(
			definedDrops.getFirst().is(expectedItem),
			label + " loot returned the wrong item"
		);
		helper.assertValueEqual(definedDrops.getFirst().getCount(), 1, label + " loot item count");

		helper.assertTrue(
			helper.getLevel().destroyBlock(absolutePos, true, breaker),
			label + " block was not destroyed"
		);
		List<ItemEntity> spawned = itemEntitiesNear(helper, TARGET);
		helper.assertTrue(!spawned.isEmpty(), label + " spawned no item entity");
		helper.assertTrue(
			spawned.stream().allMatch(entity -> entity.getItem().is(expectedItem)),
			label + " spawned an unexpected item"
		);
		helper.assertValueEqual(
			spawned.stream().mapToInt(entity -> entity.getItem().getCount()).sum(),
			1,
			label + " spawned item count"
		);
		spawned.forEach(ItemEntity::discard);
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

	private static BlockState staged(HewingCase hewingCase, Direction.Axis axis, int stage) {
		return hewingCase.staged().defaultBlockState()
			.setValue(RotatedPillarBlock.AXIS, axis)
			.setValue(HewingLogBlock.HEWING_STAGE, stage);
	}

	private static BlockState beam(HewingCase hewingCase, Direction.Axis axis) {
		return hewingCase.beam().defaultBlockState()
			.setValue(RotatedPillarBlock.AXIS, axis);
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

	private record InvalidTarget(String label, BlockState state) {
	}

	private record HewingCase(
		String label,
		TimberType type,
		Block source,
		Block stripped,
		HewingLogBlock staged,
		Block beam,
		Block roughBoards
	) {
	}
}
