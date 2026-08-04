package net.beforetheblight.gametest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import net.beforetheblight.block.AbstractSplittingStumpBlock;
import net.beforetheblight.block.LoadedSplittingStumpBlock;
import net.beforetheblight.interaction.SplittingStateMachine;
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
import net.minecraft.gametest.framework.GameTestHelper;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class SplittingGameTests {
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
	public void chestnutBeamLoadsAndUnsupportedWoodsAreRejected(GameTestHelper helper) {
		TimberProcess chestnut = chestnutProcess();
		Direction facing = Direction.EAST;
		clearItemEntities(helper, TARGET);
		helper.setBlock(TARGET, empty(facing));
		FakePlayer chestnutPlayer = survivalPlayer(
			helper,
			new ItemStack(chestnut.finalBlock(), 2),
			"btb-split-load"
		);

		InteractionResult loadResult = useHeldItem(helper, chestnutPlayer, TARGET, Direction.UP);
		assertSuccess(helper, loadResult, "chestnut load");
		helper.assertBlockState(
			TARGET,
			loaded(
				facing,
				TimberType.CHESTNUT,
				TimberSplitKind.SHINGLES,
				false,
				SplittingStateMachine.INITIAL_STRIKE_STAGE
			)
		);
		helper.assertValueEqual(
			chestnutPlayer.getInventory().countItem(chestnut.finalBlock().asItem()),
			1,
			"chestnut remaining beam count"
		);
		assertNoItemEntities(helper, TARGET, "chestnut load");

		int playerIndex = 0;
		for (TimberType unsupportedType : List.of(TimberType.OAK, TimberType.SPRUCE)) {
			TimberProcess unsupported = TimberProcessingRegistry.byType(unsupportedType)
				.orElseThrow(() -> new IllegalStateException("Missing process for " + unsupportedType));
			String label = unsupportedType + " rejected load";
			helper.setBlock(TARGET, empty(Direction.NORTH));
			// Block vanilla BlockItem fallback so this assertion isolates the
			// splitting-stump response and inventory contract.
			helper.setBlock(ABOVE_TARGET, Blocks.STONE);
			FakePlayer player = survivalPlayer(
				helper,
				new ItemStack(unsupported.finalBlock(), 2),
				"btb-split-reject-" + playerIndex++
			);
			InteractionResult result = useHeldItem(helper, player, TARGET, Direction.UP);
			assertNotSuccess(helper, result, label);
			helper.assertBlockState(TARGET, empty(Direction.NORTH));
			helper.assertBlockState(ABOVE_TARGET, Blocks.STONE.defaultBlockState());
			helper.assertValueEqual(
				player.getInventory().countItem(unsupported.finalBlock().asItem()),
				2,
				label + " held beam count"
			);
			assertNoItemEntities(helper, TARGET, label);
		}
		helper.setBlock(ABOVE_TARGET, Blocks.AIR);
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void froeSelectsOutcomesAndRepeatOrLockedUseCostsNothing(GameTestHelper helper) {
		int playerIndex = 0;
		for (Direction verticalFace : List.of(Direction.UP, Direction.DOWN)) {
			helper.setBlock(
				TARGET,
				loaded(
					Direction.NORTH,
					TimberType.CHESTNUT,
					TimberSplitKind.RAILS,
					false,
					0
				)
			);
			FakePlayer player = survivalPlayer(
				helper,
				ModItems.FROE.getDefaultInstance(),
				"btb-froe-vertical-" + playerIndex++
			);
			InteractionResult result = useHeldItem(helper, player, TARGET, verticalFace);
			assertSuccess(helper, result, verticalFace + " froe selection");
			helper.assertBlockState(
				TARGET,
				loaded(
					Direction.NORTH,
					TimberType.CHESTNUT,
					TimberSplitKind.SHINGLES,
					true,
					0
				)
			);
			helper.assertValueEqual(
				player.getMainHandItem().getDamageValue(),
				1,
				verticalFace + " froe damage"
			);
		}

		Direction facing = Direction.WEST;
		helper.setBlock(
			TARGET,
			loaded(facing, TimberType.CHESTNUT, TimberSplitKind.SHINGLES, false, 0)
		);
		FakePlayer froePlayer = survivalPlayer(
			helper,
			ModItems.FROE.getDefaultInstance(),
			"btb-froe-side"
		);
		InteractionResult sideResult = useHeldItem(helper, froePlayer, TARGET, Direction.EAST);
		assertSuccess(helper, sideResult, "side froe selection");
		helper.assertBlockState(
			TARGET,
			loaded(facing, TimberType.CHESTNUT, TimberSplitKind.RAILS, true, 0)
		);
		helper.assertValueEqual(froePlayer.getMainHandItem().getDamageValue(), 1, "side froe damage");

		InteractionResult repeated = useHeldItem(helper, froePlayer, TARGET, Direction.EAST);
		assertSuccess(helper, repeated, "repeated side froe");
		helper.assertBlockState(
			TARGET,
			loaded(facing, TimberType.CHESTNUT, TimberSplitKind.RAILS, true, 0)
		);
		helper.assertValueEqual(
			froePlayer.getMainHandItem().getDamageValue(),
			1,
			"repeated froe damage"
		);

		FakePlayer maulPlayer = survivalPlayer(
			helper,
			ModItems.WOODEN_MAUL.getDefaultInstance(),
			"btb-froe-lock-maul"
		);
		assertSuccess(
			helper,
			useHeldItem(helper, maulPlayer, TARGET, Direction.UP),
			"locking maul strike"
		);
		helper.assertBlockState(
			TARGET,
			loaded(facing, TimberType.CHESTNUT, TimberSplitKind.RAILS, true, 1)
		);

		InteractionResult locked = useHeldItem(helper, froePlayer, TARGET, Direction.UP);
		assertSuccess(helper, locked, "locked froe reorientation");
		helper.assertBlockState(
			TARGET,
			loaded(facing, TimberType.CHESTNUT, TimberSplitKind.RAILS, true, 1)
		);
		helper.assertValueEqual(
			froePlayer.getMainHandItem().getDamageValue(),
			1,
			"locked froe damage"
		);
		assertNoItemEntities(helper, TARGET, "froe selection and lock");
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void threeMaulStrikesAdvanceWithCooldownAndDurability(GameTestHelper helper) {
		Direction facing = Direction.SOUTH;
		clearItemEntities(helper, TARGET);
		helper.setBlock(
			TARGET,
			loaded(facing, TimberType.CHESTNUT, TimberSplitKind.SHINGLES, true, 0)
		);
		FakePlayer player = survivalPlayer(
			helper,
			ModItems.WOODEN_MAUL.getDefaultInstance(),
			"btb-maul-stages"
		);
		ItemStack maul = player.getMainHandItem();

		assertSuccess(helper, useHeldItem(helper, player, TARGET, Direction.UP), "first maul strike");
		helper.assertBlockState(
			TARGET,
			loaded(facing, TimberType.CHESTNUT, TimberSplitKind.SHINGLES, true, 1)
		);
		helper.assertValueEqual(maul.getDamageValue(), 1, "first maul strike damage");
		helper.assertTrue(player.getCooldowns().isOnCooldown(maul), "first strike cooldown");

		InteractionResult blocked = useHeldItem(helper, player, TARGET, Direction.UP);
		assertNotSuccess(helper, blocked, "immediate cooldown strike");
		helper.assertBlockState(
			TARGET,
			loaded(facing, TimberType.CHESTNUT, TimberSplitKind.SHINGLES, true, 1)
		);
		helper.assertValueEqual(maul.getDamageValue(), 1, "blocked maul strike damage");

		for (int tick = 1; tick < EXPECTED_COOLDOWN_TICKS; tick++) {
			player.getCooldowns().tick();
			helper.assertTrue(
				player.getCooldowns().isOnCooldown(maul),
				"maul cooldown ended early at tick " + tick
			);
		}
		player.getCooldowns().tick();
		helper.assertFalse(
			player.getCooldowns().isOnCooldown(maul),
			"maul cooldown remained after tick " + EXPECTED_COOLDOWN_TICKS
		);

		assertSuccess(helper, useHeldItem(helper, player, TARGET, Direction.UP), "second maul strike");
		helper.assertBlockState(
			TARGET,
			loaded(facing, TimberType.CHESTNUT, TimberSplitKind.SHINGLES, true, 2)
		);
		helper.assertValueEqual(maul.getDamageValue(), 2, "second maul strike damage");
		clearCooldown(player, maul);

		assertSuccess(helper, useHeldItem(helper, player, TARGET, Direction.UP), "third maul strike");
		helper.assertBlockState(TARGET, empty(facing));
		helper.assertValueEqual(maul.getDamageValue(), 3, "third maul strike damage");
		helper.assertTrue(player.getCooldowns().isOnCooldown(maul), "third strike cooldown");
		assertWorldDropsExactly(
			helper,
			TARGET,
			Map.of(ModBlocks.CHESTNUT_SHINGLES.asItem(), 4),
			"three-strike completion"
		);
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void terminalStrikeOutputsExactMappedKindAndRestoresEmptyStump(GameTestHelper helper) {
		TimberProcess process = chestnutProcess();
		int scenario = 0;
		for (TimberSplitKind splitKind : TimberSplitKind.values()) {
			Direction facing = HORIZONTAL_FACINGS.get(scenario);
			var output = process.splitOutputs().get(splitKind);
			helper.assertTrue(output != null, "missing split output for " + splitKind);
			if (splitKind == TimberSplitKind.SHINGLES) {
				helper.assertValueEqual(output.item().asItem(), ModBlocks.CHESTNUT_SHINGLES.asItem(), "shingle item");
				helper.assertValueEqual(output.count(), 4, "shingle output count");
			} else {
				helper.assertValueEqual(output.item().asItem(), ModBlocks.SPLIT_CHESTNUT_RAILS.asItem(), "rail item");
				helper.assertValueEqual(output.count(), 2, "rail output count");
			}

			clearItemEntities(helper, TARGET);
			helper.setBlock(
				TARGET,
				loaded(
					facing,
					TimberType.CHESTNUT,
					splitKind,
					true,
					SplittingStateMachine.FINAL_STRIKE_STAGE
				)
			);
			FakePlayer player = survivalPlayer(
				helper,
				ModItems.WOODEN_MAUL.getDefaultInstance(),
				"btb-split-terminal-" + scenario++
			);
			assertSuccess(
				helper,
				useHeldItem(helper, player, TARGET, Direction.UP),
				splitKind + " terminal strike"
			);
			helper.assertBlockState(TARGET, empty(facing));
			assertWorldDropsExactly(
				helper,
				TARGET,
				Map.of(output.item().asItem(), output.count()),
				splitKind + " exact terminal output"
			);
		}
		assertFullStackSplittingTransactions(helper, process);
		assertDurabilityLimitedSplittingTransaction(helper, process);
		assertCreativeFullStackSplittingTransaction(helper, process);
		helper.succeed();
	}

	private static void assertFullStackSplittingTransactions(
		GameTestHelper helper,
		TimberProcess process
	) {
		int scenario = 0;
		int additionalInputCount = 63;
		int expectedBatchCount = 64;
		int expectedMaulDamage = 3 * expectedBatchCount;
		Item inputItem = process.finalBlock().asItem();

		for (TimberSplitKind splitKind : TimberSplitKind.values()) {
			Direction facing = HORIZONTAL_FACINGS.get(scenario);
			var output = process.splitOutputs().get(splitKind);
			String label = "full-stack " + splitKind;
			clearItemEntities(helper, TARGET);
			helper.setBlock(
				TARGET,
				loaded(
					facing,
					TimberType.CHESTNUT,
					splitKind,
					true,
					SplittingStateMachine.INITIAL_STRIKE_STAGE
				)
			);
			FakePlayer player = survivalPlayer(
				helper,
				ModItems.WOODEN_MAUL.getDefaultInstance(),
				"btb-split-full-stack-" + scenario++
			);
			ItemStack maul = player.getMainHandItem();
			prepareFullBatchInventory(player, inputItem);
			helper.assertValueEqual(
				player.getInventory().countItem(inputItem),
				additionalInputCount,
				label + " starting input count"
			);

			for (int strike = 1; strike <= 3; strike++) {
				if (strike > 1) {
					clearCooldown(player, maul);
				}
				assertSuccess(
					helper,
					useHeldItem(helper, player, TARGET, Direction.UP),
					label + " maul strike " + strike
				);
				if (strike < 3) {
					helper.assertBlockState(
						TARGET,
						loaded(
							facing,
							TimberType.CHESTNUT,
							splitKind,
							true,
							strike
						)
					);
					helper.assertValueEqual(
						maul.getDamageValue(),
						strike,
						label + " maul damage after strike " + strike
					);
					helper.assertValueEqual(
						player.getInventory().countItem(inputItem),
						additionalInputCount,
						label + " consumed inputs before terminal strike " + strike
					);
					assertNoItemEntities(helper, TARGET, label + " strike " + strike);
				}
			}

			helper.assertBlockState(TARGET, empty(facing));
			helper.assertValueEqual(
				player.getInventory().countItem(inputItem),
				0,
				label + " remaining input count"
			);
			helper.assertValueEqual(
				maul.getDamageValue(),
				expectedMaulDamage,
				label + " proportional maul damage"
			);
			helper.assertTrue(
				player.getMainHandItem().is(ModItems.WOODEN_MAUL),
				label + " unexpectedly broke the maul"
			);
			helper.assertValueEqual(
				player.getInventory().countItem(Items.COBBLESTONE),
				FULL_INVENTORY_FILLER_COUNT,
				label + " changed unrelated full-inventory filler"
			);
			int expectedOutputCount = output.count() * expectedBatchCount;
			assertWorldDropsExactly(
				helper,
				TARGET,
				Map.of(output.item().asItem(), expectedOutputCount),
				label + " output"
			);
			helper.assertTrue(
				itemEntitiesNear(helper, TARGET).stream()
					.allMatch(entity -> entity.getItem().getCount() <= entity.getItem().getMaxStackSize()),
				label + " produced an oversized item stack"
			);
		}
		clearItemEntities(helper, TARGET);
	}

	private static void assertDurabilityLimitedSplittingTransaction(
		GameTestHelper helper,
		TimberProcess process
	) {
		TimberProcess otherProcess = TimberProcessingRegistry.byType(TimberType.OAK)
			.orElseThrow(() -> new IllegalStateException("Oak timber process is missing."));
		Direction facing = Direction.SOUTH;
		TimberSplitKind splitKind = TimberSplitKind.RAILS;
		var output = process.splitOutputs().get(splitKind);
		Item inputItem = process.finalBlock().asItem();
		ItemStack maul = ModItems.WOODEN_MAUL.getDefaultInstance();
		maul.setDamageValue(251);
		FakePlayer player = survivalPlayer(
			helper,
			maul,
			"btb-split-durability-limited"
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
			loaded(
				facing,
				TimberType.CHESTNUT,
				splitKind,
				true,
				SplittingStateMachine.FINAL_STRIKE_STAGE
			)
		);

		assertSuccess(
			helper,
			useHeldItem(helper, player, TARGET, Direction.UP),
			"durability-limited terminal maul strike"
		);
		helper.assertBlockState(TARGET, empty(facing));
		helper.assertValueEqual(
			player.getMainHandItem().getDamageValue(),
			255,
			"durability-limited maul damage"
		);
		helper.assertValueEqual(
			player.getInventory().countItem(inputItem),
			2,
			"durability-limited matching split beams"
		);
		helper.assertValueEqual(
			player.getInventory().countItem(otherProcess.finalBlock().asItem()),
			4,
			"durability-limited other-wood split beams"
		);
		helper.assertValueEqual(
			player.getInventory().countItem(Items.COBBLESTONE),
			5,
			"durability-limited split unrelated items"
		);
		assertWorldDropsExactly(
			helper,
			TARGET,
			Map.of(output.item().asItem(), output.count() * 2),
			"durability-limited split output"
		);

		clearItemEntities(helper, TARGET);
		clearCooldown(player, maul);
		helper.setBlock(
			TARGET,
			loaded(
				facing,
				TimberType.CHESTNUT,
				splitKind,
				true,
				SplittingStateMachine.FINAL_STRIKE_STAGE
			)
		);
		assertSuccess(
			helper,
			useHeldItem(helper, player, TARGET, Direction.UP),
			"one-durability terminal maul strike"
		);
		helper.assertBlockState(TARGET, empty(facing));
		helper.assertTrue(
			player.getMainHandItem().isEmpty(),
			"one-durability terminal maul strike did not break the tool"
		);
		helper.assertValueEqual(
			player.getInventory().countItem(inputItem),
			2,
			"one-durability maul strike consumed an unpaid extra beam"
		);
		helper.assertValueEqual(
			player.getInventory().countItem(otherProcess.finalBlock().asItem()),
			4,
			"one-durability maul strike changed other-wood beams"
		);
		helper.assertValueEqual(
			player.getInventory().countItem(Items.COBBLESTONE),
			5,
			"one-durability maul strike changed unrelated items"
		);
		assertWorldDropsExactly(
			helper,
			TARGET,
			Map.of(output.item().asItem(), output.count()),
			"one-durability split anchor output"
		);
		clearItemEntities(helper, TARGET);
	}

	private static void assertCreativeFullStackSplittingTransaction(
		GameTestHelper helper,
		TimberProcess process
	) {
		Direction facing = Direction.WEST;
		TimberSplitKind splitKind = TimberSplitKind.SHINGLES;
		var output = process.splitOutputs().get(splitKind);
		Item inputItem = process.finalBlock().asItem();
		ItemStack maul = ModItems.WOODEN_MAUL.getDefaultInstance();
		maul.setDamageValue(17);
		FakePlayer player = creativePlayer(
			helper,
			maul,
			"btb-split-creative-full-stack"
		);
		prepareFullBatchInventory(player, inputItem);
		clearItemEntities(helper, TARGET);
		helper.setBlock(
			TARGET,
			loaded(
				facing,
				TimberType.CHESTNUT,
				splitKind,
				true,
				SplittingStateMachine.FINAL_STRIKE_STAGE
			)
		);

		assertSuccess(
			helper,
			useHeldItem(helper, player, TARGET, Direction.UP),
			"creative full-stack terminal maul strike"
		);
		helper.assertBlockState(TARGET, empty(facing));
		helper.assertValueEqual(
			player.getInventory().countItem(inputItem),
			63,
			"creative full-stack splitting consumed matching beams"
		);
		helper.assertValueEqual(
			player.getInventory().countItem(Items.COBBLESTONE),
			FULL_INVENTORY_FILLER_COUNT,
			"creative full-stack splitting changed unrelated filler"
		);
		helper.assertValueEqual(
			player.getMainHandItem().getDamageValue(),
			17,
			"creative full-stack splitting damaged the maul"
		);
		assertWorldDropsExactly(
			helper,
			TARGET,
			Map.of(output.item().asItem(), output.count() * 64),
			"creative full-stack splitting output"
		);
		helper.assertTrue(
			itemEntitiesNear(helper, TARGET).stream()
				.allMatch(entity -> entity.getItem().getCount() <= entity.getItem().getMaxStackSize()),
			"creative full-stack splitting produced an oversized item stack"
		);
		clearItemEntities(helper, TARGET);
	}

	@GameTest(maxTicks = 40)
	public void emptyHandRecoversChestnutBeamAtEveryState(GameTestHelper helper) {
		int playerIndex = 0;
		for (Direction facing : HORIZONTAL_FACINGS) {
			for (TimberSplitKind splitKind : TimberSplitKind.values()) {
				for (boolean froeSet : List.of(false, true)) {
					for (int stage : LoadedSplittingStumpBlock.STRIKE_STAGE.getPossibleValues()) {
						String label = facing + " " + splitKind + " froe=" + froeSet + " stage=" + stage;
						clearItemEntities(helper, TARGET);
						helper.setBlock(
							TARGET,
							loaded(facing, TimberType.CHESTNUT, splitKind, froeSet, stage)
						);
						FakePlayer player = survivalPlayer(
							helper,
							ItemStack.EMPTY,
							"btb-split-recover-" + playerIndex++
						);
						assertSuccess(
							helper,
							useHeldItem(helper, player, TARGET, Direction.UP),
							label
						);
						helper.assertBlockState(TARGET, empty(facing));
						helper.assertValueEqual(
							player.getInventory().countItem(ModBlocks.HEWN_CHESTNUT_BEAM.asItem()),
							1,
							label + " recovered beam count"
						);
						helper.assertValueEqual(
							player.getInventory().countItem(ModBlocks.CHESTNUT_SHINGLES.asItem()),
							0,
							label + " unexpected shingles"
						);
						helper.assertValueEqual(
							player.getInventory().countItem(ModBlocks.SPLIT_CHESTNUT_RAILS.asItem()),
							0,
							label + " unexpected rails"
						);
						assertNoItemEntities(helper, TARGET, label);
					}
				}
			}
		}
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void loadedBreakReturnsStumpAndBeamAtEveryState(GameTestHelper helper) {
		FakePlayer breaker = survivalPlayer(helper, ItemStack.EMPTY, "btb-split-break");
		Map<Item, Integer> expected = Map.of(
			ModBlocks.SPLITTING_STUMP.asItem(),
			1,
			ModBlocks.HEWN_CHESTNUT_BEAM.asItem(),
			1
		);
		for (Direction facing : HORIZONTAL_FACINGS) {
			for (TimberSplitKind splitKind : TimberSplitKind.values()) {
				for (boolean froeSet : List.of(false, true)) {
					for (int stage : LoadedSplittingStumpBlock.STRIKE_STAGE.getPossibleValues()) {
						String label = facing + " " + splitKind + " froe=" + froeSet + " stage=" + stage;
						clearItemEntities(helper, TARGET);
						BlockState state = loaded(
							facing,
							TimberType.CHESTNUT,
							splitKind,
							froeSet,
							stage
						);
						helper.setBlock(TARGET, state);
						List<ItemStack> definedDrops = Block.getDrops(
							state,
							helper.getLevel(),
							helper.absolutePos(TARGET),
							null,
							breaker,
							ItemStack.EMPTY
						);
						helper.assertValueEqual(stackCounts(definedDrops), expected, label + " declared loot");
						helper.assertTrue(
							helper.getLevel().destroyBlock(helper.absolutePos(TARGET), true, breaker),
							label + " live break"
						);
						helper.assertBlockState(TARGET, Blocks.AIR.defaultBlockState());
						assertWorldDropsExactly(helper, TARGET, expected, label + " live drops");
					}
				}
			}
		}
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void allSplittingStatesRoundTripThroughNbt(GameTestHelper helper) {
		helper.assertValueEqual(
			LoadedSplittingStumpBlock.STRIKE_STAGE.getPossibleValues(),
			List.of(0, 1, 2),
			"strike_stage values"
		);
		helper.assertValueEqual(
			LoadedSplittingStumpBlock.WOOD_TYPE.getPossibleValues(),
			List.of(TimberType.CHESTNUT, TimberType.OAK, TimberType.SPRUCE),
			"wood_type values"
		);
		helper.assertValueEqual(
			LoadedSplittingStumpBlock.SPLIT_KIND.getPossibleValues(),
			List.of(TimberSplitKind.SHINGLES, TimberSplitKind.RAILS),
			"split_kind values"
		);

		int stateCount = 0;
		for (Direction facing : HORIZONTAL_FACINGS) {
			assertNbtRoundTrip(helper, empty(facing), "empty facing " + facing);
			stateCount++;
			for (TimberType woodType : TimberType.values()) {
				for (TimberSplitKind splitKind : TimberSplitKind.values()) {
					for (boolean froeSet : List.of(false, true)) {
						for (int stage : LoadedSplittingStumpBlock.STRIKE_STAGE.getPossibleValues()) {
							assertNbtRoundTrip(
								helper,
								loaded(facing, woodType, splitKind, froeSet, stage),
								facing + " " + woodType + " " + splitKind
									+ " froe=" + froeSet + " stage=" + stage
							);
							stateCount++;
						}
					}
				}
			}
		}
		helper.assertValueEqual(stateCount, 148, "persisted splitting-state count");
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void twoPlayersSerializeTerminalStrikeWithoutDuplication(GameTestHelper helper) {
		TimberProcess process = chestnutProcess();
		int scenario = 0;
		int additionalInputsPerPlayer = 2;
		for (TimberSplitKind splitKind : TimberSplitKind.values()) {
			for (boolean playerAWins : List.of(true, false)) {
				Direction facing = HORIZONTAL_FACINGS.get(scenario);
				String label = splitKind + (playerAWins ? " A then B" : " B then A");
				clearItemEntities(helper, TARGET);
				helper.setBlock(
					TARGET,
					loaded(
						facing,
						TimberType.CHESTNUT,
						splitKind,
						true,
						SplittingStateMachine.FINAL_STRIKE_STAGE
					)
				);
				FakePlayer playerA = survivalPlayer(
					helper,
					ModItems.WOODEN_MAUL.getDefaultInstance(),
					"btb-split-term-a-" + scenario
				);
				FakePlayer playerB = survivalPlayer(
					helper,
					ModItems.WOODEN_MAUL.getDefaultInstance(),
					"btb-split-term-b-" + scenario
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
				BlockHitResult hit = hitResult(helper, TARGET, Direction.UP);
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

				assertSuccess(helper, useHeldItem(helper, winner, hit), label + " winner");
				helper.assertBlockState(TARGET, empty(facing));
				var output = process.splitOutputs().get(splitKind);
				Map<Item, Integer> expected = Map.of(
					output.item().asItem(),
					output.count() * (1 + additionalInputsPerPlayer)
				);
				assertWorldDropsExactly(helper, TARGET, expected, label + " winner output");
				helper.assertValueEqual(
					winner.getInventory().countItem(inputItem),
					0,
					label + " winner remaining input count"
				);

				assertNotSuccess(helper, useHeldItem(helper, loser, hit), label + " loser");
				helper.assertBlockState(TARGET, empty(facing));
				assertWorldDropsExactly(helper, TARGET, expected, label + " final output");
				helper.assertValueEqual(
					winner.getMainHandItem().getDamageValue(),
					1 + additionalInputsPerPlayer * 3,
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
					label + " winner cooldown"
				);
				helper.assertFalse(
					loser.getCooldowns().isOnCooldown(loser.getMainHandItem()),
					label + " loser cooldown"
				);
				scenario++;
			}
		}
		helper.succeed();
	}

	private static TimberProcess chestnutProcess() {
		return TimberProcessingRegistry.byType(TimberType.CHESTNUT)
			.orElseThrow(() -> new IllegalStateException("Chestnut timber process is missing."));
	}

	private static BlockState empty(Direction facing) {
		return ModBlocks.SPLITTING_STUMP.defaultBlockState()
			.setValue(AbstractSplittingStumpBlock.FACING, facing);
	}

	private static BlockState loaded(
		Direction facing,
		TimberType woodType,
		TimberSplitKind splitKind,
		boolean froeSet,
		int stage
	) {
		return ModBlocks.LOADED_SPLITTING_STUMP.defaultBlockState()
			.setValue(AbstractSplittingStumpBlock.FACING, facing)
			.setValue(LoadedSplittingStumpBlock.WOOD_TYPE, woodType)
			.setValue(LoadedSplittingStumpBlock.SPLIT_KIND, splitKind)
			.setValue(LoadedSplittingStumpBlock.FROE_SET, froeSet)
			.setValue(LoadedSplittingStumpBlock.STRIKE_STAGE, stage);
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
		BlockPos relativePos,
		Direction face
	) {
		return useHeldItem(helper, player, hitResult(helper, relativePos, face));
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

	private static BlockHitResult hitResult(
		GameTestHelper helper,
		BlockPos relativePos,
		Direction face
	) {
		BlockPos absolutePos = helper.absolutePos(relativePos);
		return new BlockHitResult(Vec3.atCenterOf(absolutePos), face, absolutePos, false);
	}

	private static void clearCooldown(FakePlayer player, ItemStack stack) {
		player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(stack));
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

	private static void assertNotSuccess(
		GameTestHelper helper,
		InteractionResult result,
		String label
	) {
		helper.assertFalse(
			result instanceof InteractionResult.Success,
			label + " unexpectedly returned Success: " + result
		);
	}
}
