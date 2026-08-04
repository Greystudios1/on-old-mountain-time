package net.beforetheblight.gametest;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import net.beforetheblight.BeforeTheBlight;
import net.beforetheblight.block.AbstractSawingTrestlesBlock;
import net.beforetheblight.block.AbstractSplittingStumpBlock;
import net.beforetheblight.block.HewingLogBlock;
import net.beforetheblight.block.LoadedSawingTrestlesBlock;
import net.beforetheblight.block.LoadedSplittingStumpBlock;
import net.beforetheblight.interaction.TimberProcessingRegistry;
import net.beforetheblight.interaction.TimberProcessingRegistry.TimberProcess;
import net.beforetheblight.interaction.TimberSplitKind;
import net.beforetheblight.interaction.TimberType;
import net.beforetheblight.registry.ModBlocks;
import net.beforetheblight.registry.ModItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.impl.client.gametest.world.TestWorldSaveImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Two-process persistence proof for staged hewing, sawing, and splitting states.
 *
 * <p>The write and read phases are intentionally separate client GameTest JVMs.
 * The reader uses Fabric's pinned test-only save wrapper so Minecraft reopens the
 * exact integrated-world folder through its normal world-open flow. This class
 * never uses TestInput, movement, mouse, or camera APIs.</p>
 */
public final class HewingPersistenceClientGameTest implements FabricClientGameTest {
	private static final String PHASE_PROPERTY =
		"before_the_blight.client_gametest.hewing_persistence_phase";
	private static final String RUN_ID_PROPERTY =
		"before_the_blight.client_gametest.hewing_persistence_run_id";
	private static final String COMMIT_PROPERTY =
		"before_the_blight.client_gametest.hewing_persistence_commit";
	private static final Identifier MARKER_ID =
		BeforeTheBlight.id("hewing_persistence_probe");
	private static final BlockPos ORIGIN = new BlockPos(4096, 96, 4096);
	private static final List<Direction> HORIZONTAL_FACINGS = List.of(
		Direction.NORTH,
		Direction.EAST,
		Direction.SOUTH,
		Direction.WEST
	);
	private static final int HEWING_PARTIAL_STATE_COUNT = 27;
	private static final int HEWING_FINAL_STATE_COUNT = 9;
	private static final int HEWING_STATE_COUNT =
		HEWING_PARTIAL_STATE_COUNT + HEWING_FINAL_STATE_COUNT;
	private static final int SAWING_LOADED_STATE_COUNT = 48;
	private static final int SAWING_EMPTY_STATE_COUNT = 4;
	private static final int SAWING_STATE_COUNT =
		SAWING_LOADED_STATE_COUNT + SAWING_EMPTY_STATE_COUNT;
	private static final int SPLITTING_LOADED_STATE_COUNT = 144;
	private static final int SPLITTING_EMPTY_STATE_COUNT = 4;
	private static final int SPLITTING_STATE_COUNT =
		SPLITTING_LOADED_STATE_COUNT + SPLITTING_EMPTY_STATE_COUNT;
	private static final int EXPECTED_STATE_COUNT =
		HEWING_STATE_COUNT + SAWING_STATE_COUNT + SPLITTING_STATE_COUNT;

	@Override
	public void runTest(ClientGameTestContext context) {
		TestConfig config = TestConfig.fromSystemProperties();
		switch (config.phase()) {
			case "write" -> writeAndClose(context, config);
			case "read" -> reopenAndVerify(context, config);
			default -> throw new AssertionError("Unsupported persistence phase: " + config.phase());
		}
	}

	private static void writeAndClose(ClientGameTestContext context, TestConfig config) {
		Path expectedSave = expectedSaveDirectory(context, config.worldName());
		check(
			!Files.exists(expectedSave),
			"Writer requires an absent world directory: " + expectedSave
		);

		Path canonicalSave;
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
			.setUseConsistentSettings(true)
			.adjustSettings(state -> state.setName(config.worldName()))
			.create()) {
			canonicalSave = canonical(singleplayer.getWorldSave().getSaveDirectory());
			check(
				canonicalSave.equals(canonical(expectedSave)),
				"Created world path did not match the requested save path: expected="
					+ expectedSave + " actual=" + canonicalSave
			);
			singleplayer.getServer().runOnServer(server -> {
				writeStates(server.overworld(), config.runId());

				CompoundTag marker = new CompoundTag();
				marker.putString("run_id", config.runId());
				marker.putString("commit", config.expectedCommit());
				marker.putString("world_name", config.worldName());
				marker.putString("world_path", canonicalSave.toString());
				marker.putLong("writer_pid", ProcessHandle.current().pid());
				marker.putLong(
					"writer_jvm_start_ms",
					ManagementFactory.getRuntimeMXBean().getStartTime()
				);
				marker.putLong("state_count", EXPECTED_STATE_COUNT);
				marker.putLong("hewing_state_count", HEWING_STATE_COUNT);
				marker.putLong("sawing_state_count", SAWING_STATE_COUNT);
				marker.putLong("sawing_loaded_state_count", SAWING_LOADED_STATE_COUNT);
				marker.putLong("sawing_empty_state_count", SAWING_EMPTY_STATE_COUNT);
				marker.putLong("splitting_state_count", SPLITTING_STATE_COUNT);
				marker.putLong("splitting_loaded_state_count", SPLITTING_LOADED_STATE_COUNT);
				marker.putLong("splitting_empty_state_count", SPLITTING_EMPTY_STATE_COUNT);
				server.getCommandStorage().set(MARKER_ID, marker);

				check(
					server.saveEverything(false, true, true),
					"Integrated server saveEverything returned false"
				);
			});
		}

		check(Files.isDirectory(canonicalSave), "World disappeared after normal close");
		emitPass(
			config,
			canonicalSave,
			ProcessHandle.current().pid(),
			ManagementFactory.getRuntimeMXBean().getStartTime(),
			false,
			false,
			false,
			false
		);
	}

	private static void reopenAndVerify(ClientGameTestContext context, TestConfig config) {
		Path expectedSave = expectedSaveDirectory(context, config.worldName());
		check(Files.isDirectory(expectedSave), "Reader could not find saved world: " + expectedSave);
		check(Files.isRegularFile(expectedSave.resolve("level.dat")), "Saved world has no level.dat");
		Path canonicalSave = canonical(expectedSave);

		ReadEvidence evidence;
		try (TestSingleplayerContext singleplayer = new TestWorldSaveImpl(context, expectedSave).open()) {
			Path openedSave = canonical(singleplayer.getWorldSave().getSaveDirectory());
			check(
				openedSave.equals(canonicalSave),
				"Minecraft opened a different save: expected=" + canonicalSave + " actual=" + openedSave
			);

			evidence = singleplayer.getServer().computeOnServer(server -> {
				CompoundTag marker = server.getCommandStorage().get(MARKER_ID);
				check(marker != null, "Persisted CommandStorage marker was missing");
				check(
					config.runId().equals(marker.getStringOr("run_id", "")),
					"Persisted run ID did not match"
				);
				check(
					config.expectedCommit().equals(marker.getStringOr("commit", "")),
					"Persisted Git commit did not match"
				);
				check(
					config.worldName().equals(marker.getStringOr("world_name", "")),
					"Persisted world name did not match"
				);
				check(
					marker.getLongOr("state_count", -1L) == EXPECTED_STATE_COUNT,
					"Persisted state count did not match"
				);
				check(
					marker.getLongOr("hewing_state_count", -1L) == HEWING_STATE_COUNT,
					"Persisted hewing-state count did not match"
				);
				check(
					marker.getLongOr("sawing_state_count", -1L) == SAWING_STATE_COUNT,
					"Persisted sawing-state count did not match"
				);
				check(
					marker.getLongOr("sawing_loaded_state_count", -1L) == SAWING_LOADED_STATE_COUNT,
					"Persisted loaded-sawing-state count did not match"
				);
				check(
					marker.getLongOr("sawing_empty_state_count", -1L) == SAWING_EMPTY_STATE_COUNT,
					"Persisted empty-sawing-state count did not match"
				);
				check(
					marker.getLongOr("splitting_state_count", -1L) == SPLITTING_STATE_COUNT,
					"Persisted splitting-state count did not match"
				);
				check(
					marker.getLongOr("splitting_loaded_state_count", -1L) == SPLITTING_LOADED_STATE_COUNT,
					"Persisted loaded-splitting-state count did not match"
				);
				check(
					marker.getLongOr("splitting_empty_state_count", -1L) == SPLITTING_EMPTY_STATE_COUNT,
					"Persisted empty-splitting-state count did not match"
				);

				long writerPid = marker.getLongOr("writer_pid", -1L);
				long writerStart = marker.getLongOr("writer_jvm_start_ms", -1L);
				check(writerPid > 0, "Persisted writer PID was invalid");
				check(writerStart > 0, "Persisted writer JVM start time was invalid");
				check(
					writerPid != ProcessHandle.current().pid(),
					"Reader reused the writer Minecraft JVM PID"
				);
				check(
					writerStart != ManagementFactory.getRuntimeMXBean().getStartTime(),
					"Reader reused the writer Minecraft JVM start time"
				);

				Path storedSave = canonical(Path.of(marker.getStringOr("world_path", "")));
				check(storedSave.equals(canonicalSave), "Persisted canonical world path did not match");
				TimberReadEvidence timberEvidence = verifyThenContinueStates(
					server.overworld(),
					config.runId()
				);

				check(
					server.saveEverything(false, true, true),
					"Reader saveEverything returned false"
				);
				return new ReadEvidence(
					writerPid,
					writerStart,
					timberEvidence.sawingContinuationVerified(),
					timberEvidence.sawingRecoveryVerified(),
					timberEvidence.splittingContinuationVerified(),
					timberEvidence.splittingRecoveryVerified()
				);
			});
		}

		emitPass(
			config,
			canonicalSave,
			evidence.writerPid(),
			evidence.writerJvmStartMs(),
			evidence.sawingContinuationVerified(),
			evidence.sawingRecoveryVerified(),
			evidence.splittingContinuationVerified(),
			evidence.splittingRecoveryVerified()
		);
	}

	private static void writeStates(ServerLevel level, String runId) {
		List<BlockState> expectedStates = persistenceStates();
		check(expectedStates.size() == EXPECTED_STATE_COUNT, "Persistence state contract drifted");
		writeHewingStates(level, runId, hewingPersistenceStates());
		writeSawingStates(level, runId, sawingPersistenceStates());
		writeSplittingStates(level, runId, splittingPersistenceStates());

		for (int index = 0; index < expectedStates.size(); index++) {
			check(
				level.getBlockState(ORIGIN.offset(index, 0, 0)).equals(expectedStates.get(index)),
				"Writer final-state mismatch at slot " + index
			);
		}
	}

	private static void writeHewingStates(
		ServerLevel level,
		String runId,
		List<BlockState> expectedStates
	) {
		check(expectedStates.size() == HEWING_STATE_COUNT, "Hewing persistence matrix drifted");
		FakePlayer player = survivalPlayer(level, "btb-persist-w", runId + ":write");
		player.setItemInHand(InteractionHand.MAIN_HAND, ModItems.BROAD_AXE.getDefaultInstance());
		int expectedDamage = 0;

		for (int index = 0; index < expectedStates.size(); index++) {
			BlockPos target = ORIGIN.offset(index, 0, 0);
			BlockState expected = expectedStates.get(index);
			TimberProcess process = timberProcessForHewingState(expected);
			Direction.Axis axis = expected.getValue(RotatedPillarBlock.AXIS);
			check(
				level.setBlock(
					target,
					process.sourceBlock().defaultBlockState()
						.setValue(RotatedPillarBlock.AXIS, axis),
					Block.UPDATE_ALL_IMMEDIATE
				),
				"Could not place persistence source log at " + target
			);
			player.setPos(target.getX() + 0.5, target.getY() + 1.0, target.getZ() + 0.5);
			int strikes = expected.is(process.stagedBlock())
				? expected.getValue(HewingLogBlock.HEWING_STAGE)
				: 4;
			for (int strike = 1; strike <= strikes; strike++) {
				assertSuccess(useHeldItem(level, player, target), "writer slot " + index + " strike " + strike);
				expectedDamage++;
				check(
					player.getMainHandItem().getDamageValue() == expectedDamage,
					"Writer broad-axe damage drifted at slot " + index
				);
			}
			check(
				level.getBlockState(target).equals(expected),
				"Writer produced the wrong state at slot " + index
			);
		}
	}

	private static void writeSawingStates(
		ServerLevel level,
		String runId,
		List<BlockState> expectedStates
	) {
		check(expectedStates.size() == SAWING_STATE_COUNT, "Sawing persistence matrix drifted");
		FakePlayer player = survivalPlayer(level, "btb-persist-s", runId + ":saw-write");

		for (int sawIndex = 0; sawIndex < expectedStates.size(); sawIndex++) {
			int index = HEWING_STATE_COUNT + sawIndex;
			BlockPos target = ORIGIN.offset(index, 0, 0);
			BlockState expected = expectedStates.get(sawIndex);
			Direction facing = expected.getValue(AbstractSawingTrestlesBlock.FACING);
			BlockState emptyState = emptySawing(facing);
			check(
				level.setBlock(target, emptyState, Block.UPDATE_ALL_IMMEDIATE),
				"Could not place empty sawing trestles at slot " + index
			);

			if (expected.is(ModBlocks.SAWING_TRESTLES)) {
				check(level.getBlockState(target).equals(expected), "Writer empty-trestles mismatch at slot " + index);
				continue;
			}

			TimberType woodType = expected.getValue(LoadedSawingTrestlesBlock.WOOD_TYPE);
			int targetStage = expected.getValue(LoadedSawingTrestlesBlock.CUT_STAGE);
			TimberProcess process = timberProcess(woodType);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(process.finalBlock(), 2));
			player.setPos(target.getX() + 0.5, target.getY() + 1.0, target.getZ() + 0.5);
			assertSuccess(useHeldItem(level, player, target), "writer saw slot " + index + " load");
			check(
				player.getInventory().countItem(process.finalBlock().asItem()) == 1,
				"Writer saw slot " + index + " did not consume exactly one beam"
			);
			check(
				level.getBlockState(target).equals(loadedSawing(facing, woodType, 0)),
				"Writer saw slot " + index + " did not load at stage zero"
			);

			ItemStack frameSaw = ModItems.FRAME_SAW.getDefaultInstance();
			player.setItemInHand(InteractionHand.MAIN_HAND, frameSaw);
			for (int strike = 1; strike <= targetStage; strike++) {
				clearCooldown(player, frameSaw);
				assertSuccess(
					useHeldItem(level, player, target),
					"writer saw slot " + index + " strike " + strike
				);
				check(
					level.getBlockState(target).equals(loadedSawing(facing, woodType, strike)),
					"Writer saw slot " + index + " stage drifted after strike " + strike
				);
				check(
					frameSaw.getDamageValue() == strike,
					"Writer saw slot " + index + " durability drifted after strike " + strike
				);
			}
			check(level.getBlockState(target).equals(expected), "Writer saw state mismatch at slot " + index);
			assertNoItemEntities(level, target, "writer saw slot " + index);
		}
	}

	private static void writeSplittingStates(
		ServerLevel level,
		String runId,
		List<BlockState> expectedStates
	) {
		check(expectedStates.size() == SPLITTING_STATE_COUNT, "Splitting persistence matrix drifted");
		int splittingOffset = HEWING_STATE_COUNT + SAWING_STATE_COUNT;

		for (int splitIndex = 0; splitIndex < expectedStates.size(); splitIndex++) {
			int index = splittingOffset + splitIndex;
			BlockPos target = ORIGIN.offset(index, 0, 0);
			BlockState expected = expectedStates.get(splitIndex);
			Direction facing = expected.getValue(AbstractSplittingStumpBlock.FACING);

			if (expected.is(ModBlocks.SPLITTING_STUMP)) {
				check(
					level.setBlock(target, emptySplitting(facing), Block.UPDATE_ALL_IMMEDIATE),
					"Could not place empty splitting stump at slot " + index
				);
				continue;
			}

			if (!isInteractionReachableSplittingState(expected)) {
				// The declared block-state schema deliberately retains invalid or
				// future-compatible combinations. Persist them directly while the
				// reachable chestnut subset below remains interaction-created.
				check(
					level.setBlock(target, expected, Block.UPDATE_ALL_IMMEDIATE),
					"Could not place declared splitting state at slot " + index
				);
				continue;
			}

			check(
				level.setBlock(target, emptySplitting(facing), Block.UPDATE_ALL_IMMEDIATE),
				"Could not prepare reachable splitting state at slot " + index
			);
			TimberProcess process = timberProcess(TimberType.CHESTNUT);
			FakePlayer player = survivalPlayer(
				level,
				"btb-persist-p",
				runId + ":split-write:" + splitIndex
			);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(process.finalBlock(), 2));
			player.setPos(target.getX() + 0.5, target.getY() + 1.0, target.getZ() + 0.5);
			assertSuccess(useHeldItem(level, player, target), "writer split slot " + index + " load");
			check(
				player.getInventory().countItem(process.finalBlock().asItem()) == 1,
				"Writer split slot " + index + " did not consume exactly one beam"
			);

			if (expected.getValue(LoadedSplittingStumpBlock.FROE_SET)) {
				TimberSplitKind splitKind = expected.getValue(LoadedSplittingStumpBlock.SPLIT_KIND);
				ItemStack froe = ModItems.FROE.getDefaultInstance();
				player.setItemInHand(InteractionHand.MAIN_HAND, froe);
				Direction clickedFace = splitKind == TimberSplitKind.SHINGLES
					? Direction.UP
					: Direction.NORTH;
				assertSuccess(
					useHeldItem(level, player, target, clickedFace),
					"writer split slot " + index + " set froe"
				);
				check(froe.getDamageValue() == 1, "Writer froe durability drifted at slot " + index);

				int targetStage = expected.getValue(LoadedSplittingStumpBlock.STRIKE_STAGE);
				ItemStack maul = ModItems.WOODEN_MAUL.getDefaultInstance();
				player.setItemInHand(InteractionHand.MAIN_HAND, maul);
				for (int strike = 1; strike <= targetStage; strike++) {
					clearCooldown(player, maul);
					assertSuccess(
						useHeldItem(level, player, target),
						"writer split slot " + index + " maul strike " + strike
					);
					check(
						maul.getDamageValue() == strike,
						"Writer maul durability drifted at slot " + index
					);
				}
			}

			check(level.getBlockState(target).equals(expected), "Writer split state mismatch at slot " + index);
			assertNoItemEntities(level, target, "writer split slot " + index);
		}
	}

	private static TimberReadEvidence verifyThenContinueStates(ServerLevel level, String runId) {
		List<BlockState> expectedStates = persistenceStates();
		check(expectedStates.size() == EXPECTED_STATE_COUNT, "Reader persistence matrix drifted");
		for (int index = 0; index < expectedStates.size(); index++) {
			BlockState actual = level.getBlockState(ORIGIN.offset(index, 0, 0));
			check(
				actual.equals(expectedStates.get(index)),
				"Reopened state mismatch at slot " + index
			);
		}

		FakePlayer player = survivalPlayer(level, "btb-persist-r", runId + ":read");
		List<BlockState> hewingStates = hewingPersistenceStates();
		for (int index = 0; index < hewingStates.size(); index++) {
			BlockPos target = ORIGIN.offset(index, 0, 0);
			BlockState persisted = hewingStates.get(index);
			TimberProcess process = timberProcessForHewingState(persisted);
			player.setItemInHand(InteractionHand.MAIN_HAND, ModItems.BROAD_AXE.getDefaultInstance());
			player.setPos(target.getX() + 0.5, target.getY() + 1.0, target.getZ() + 0.5);
			InteractionResult result = useHeldItem(level, player, target);

			if (persisted.is(process.stagedBlock())) {
				int stage = persisted.getValue(HewingLogBlock.HEWING_STAGE);
				Direction.Axis axis = persisted.getValue(RotatedPillarBlock.AXIS);
				BlockState continued = stage < 3
					? staged(process, axis, stage + 1)
					: beam(process, axis);
				assertSuccess(result, "reader slot " + index);
				check(level.getBlockState(target).equals(continued), "Reader continuation mismatch at slot " + index);
				check(player.getMainHandItem().getDamageValue() == 1, "Reader damage mismatch at slot " + index);
			} else {
				check(result instanceof InteractionResult.Pass, "Final beam did not return PASS at slot " + index);
				check(level.getBlockState(target).equals(persisted), "Final beam changed at slot " + index);
				check(player.getMainHandItem().getDamageValue() == 0, "Final beam damaged the axe at slot " + index);
			}
		}

		BlockState continuedSawState = loadedSawing(Direction.NORTH, TimberType.CHESTNUT, 1);
		int continuedSawIndex = expectedStates.indexOf(continuedSawState);
		check(continuedSawIndex >= HEWING_STATE_COUNT, "Saw continuation probe state was missing");
		BlockPos continuedSawTarget = ORIGIN.offset(continuedSawIndex, 0, 0);
		FakePlayer sawPlayer = survivalPlayer(level, "btb-persist-sc", runId + ":saw-continue");
		ItemStack frameSaw = ModItems.FRAME_SAW.getDefaultInstance();
		sawPlayer.setItemInHand(InteractionHand.MAIN_HAND, frameSaw);
		sawPlayer.setPos(
			continuedSawTarget.getX() + 0.5,
			continuedSawTarget.getY() + 1.0,
			continuedSawTarget.getZ() + 0.5
		);
		assertNoItemEntities(level, continuedSawTarget, "reader saw continuation before stroke");
		assertSuccess(
			useHeldItem(level, sawPlayer, continuedSawTarget),
			"reader saw nonterminal continuation"
		);
		check(
			level.getBlockState(continuedSawTarget).equals(
				loadedSawing(Direction.NORTH, TimberType.CHESTNUT, 2)
			),
			"Reader saw continuation did not advance from stage one to stage two"
		);
		check(frameSaw.getDamageValue() == 1, "Reader saw continuation durability was not exactly one");
		check(
			sawPlayer.getCooldowns().isOnCooldown(frameSaw),
			"Reader saw continuation did not start its cooldown"
		);
		assertNoItemEntities(level, continuedSawTarget, "reader saw continuation after stroke");

		BlockState recoveredSawState = loadedSawing(Direction.WEST, TimberType.SPRUCE, 2);
		int recoveredSawIndex = expectedStates.indexOf(recoveredSawState);
		check(recoveredSawIndex >= HEWING_STATE_COUNT, "Saw recovery probe state was missing");
		check(recoveredSawIndex != continuedSawIndex, "Saw probes unexpectedly targeted the same slot");
		BlockPos recoveredSawTarget = ORIGIN.offset(recoveredSawIndex, 0, 0);
		TimberProcess recoveredProcess = timberProcess(TimberType.SPRUCE);
		FakePlayer recoveryPlayer = survivalPlayer(level, "btb-persist-sr", runId + ":saw-recover");
		recoveryPlayer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		recoveryPlayer.setPos(
			recoveredSawTarget.getX() + 0.5,
			recoveredSawTarget.getY() + 1.0,
			recoveredSawTarget.getZ() + 0.5
		);
		assertNoItemEntities(level, recoveredSawTarget, "reader saw recovery before unload");
		assertSuccess(
			useHeldItem(level, recoveryPlayer, recoveredSawTarget),
			"reader saw empty-hand recovery"
		);
		check(
			level.getBlockState(recoveredSawTarget).equals(emptySawing(Direction.WEST)),
			"Reader saw recovery did not restore west-facing empty trestles"
		);
		check(
			recoveryPlayer.getInventory().countItem(recoveredProcess.finalBlock().asItem()) == 1,
			"Reader saw recovery did not return exactly one spruce beam"
		);
		check(
			recoveryPlayer.getInventory().countItem(recoveredProcess.roughBoards().item().asItem()) == 0,
			"Reader saw recovery returned unexpected rough boards"
		);
		check(
			recoveryPlayer.getInventory().countItem(ModBlocks.SAWING_TRESTLES.asItem()) == 0,
			"Reader saw recovery returned unexpected trestles"
		);
		assertNoItemEntities(level, recoveredSawTarget, "reader saw recovery after unload");

		BlockState continuedSplitState = loadedSplitting(
			Direction.SOUTH,
			TimberType.CHESTNUT,
			TimberSplitKind.SHINGLES,
			true,
			1
		);
		int continuedSplitIndex = expectedStates.indexOf(continuedSplitState);
		int splittingOffset = HEWING_STATE_COUNT + SAWING_STATE_COUNT;
		check(continuedSplitIndex >= splittingOffset, "Split continuation probe state was missing");
		BlockPos continuedSplitTarget = ORIGIN.offset(continuedSplitIndex, 0, 0);
		FakePlayer splitPlayer = survivalPlayer(level, "btb-persist-pc", runId + ":split-continue");
		ItemStack maul = ModItems.WOODEN_MAUL.getDefaultInstance();
		splitPlayer.setItemInHand(InteractionHand.MAIN_HAND, maul);
		splitPlayer.setPos(
			continuedSplitTarget.getX() + 0.5,
			continuedSplitTarget.getY() + 1.0,
			continuedSplitTarget.getZ() + 0.5
		);
		assertNoItemEntities(level, continuedSplitTarget, "reader split continuation before strike");
		assertSuccess(
			useHeldItem(level, splitPlayer, continuedSplitTarget),
			"reader split nonterminal continuation"
		);
		check(
			level.getBlockState(continuedSplitTarget).equals(
				loadedSplitting(
					Direction.SOUTH,
					TimberType.CHESTNUT,
					TimberSplitKind.SHINGLES,
					true,
					2
				)
			),
			"Reader split continuation did not advance from stage one to stage two"
		);
		check(maul.getDamageValue() == 1, "Reader split continuation durability was not exactly one");
		check(
			splitPlayer.getCooldowns().isOnCooldown(maul),
			"Reader split continuation did not start its cooldown"
		);
		assertNoItemEntities(level, continuedSplitTarget, "reader split continuation after strike");

		BlockState recoveredSplitState = loadedSplitting(
			Direction.EAST,
			TimberType.CHESTNUT,
			TimberSplitKind.RAILS,
			true,
			2
		);
		int recoveredSplitIndex = expectedStates.indexOf(recoveredSplitState);
		check(recoveredSplitIndex >= splittingOffset, "Split recovery probe state was missing");
		check(recoveredSplitIndex != continuedSplitIndex, "Split probes unexpectedly targeted the same slot");
		BlockPos recoveredSplitTarget = ORIGIN.offset(recoveredSplitIndex, 0, 0);
		TimberProcess recoveredSplitProcess = timberProcess(TimberType.CHESTNUT);
		FakePlayer splitRecoveryPlayer = survivalPlayer(
			level,
			"btb-persist-pr",
			runId + ":split-recover"
		);
		splitRecoveryPlayer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		splitRecoveryPlayer.setPos(
			recoveredSplitTarget.getX() + 0.5,
			recoveredSplitTarget.getY() + 1.0,
			recoveredSplitTarget.getZ() + 0.5
		);
		assertNoItemEntities(level, recoveredSplitTarget, "reader split recovery before unload");
		assertSuccess(
			useHeldItem(level, splitRecoveryPlayer, recoveredSplitTarget),
			"reader split empty-hand recovery"
		);
		check(
			level.getBlockState(recoveredSplitTarget).equals(emptySplitting(Direction.EAST)),
			"Reader split recovery did not restore east-facing empty stump"
		);
		check(
			splitRecoveryPlayer.getInventory().countItem(recoveredSplitProcess.finalBlock().asItem()) == 1,
			"Reader split recovery did not return exactly one chestnut beam"
		);
		check(
			splitRecoveryPlayer.getInventory().countItem(
				recoveredSplitProcess.splitOutputs().get(TimberSplitKind.RAILS).item().asItem()
			) == 0,
			"Reader split recovery returned rails instead of its beam"
		);
		assertNoItemEntities(level, recoveredSplitTarget, "reader split recovery after unload");

		for (int index = 0; index < expectedStates.size(); index++) {
			BlockPos target = ORIGIN.offset(index, 0, 0);
			BlockState expected = expectedStates.get(index);
			if (!level.getBlockState(target).equals(expected)) {
				check(
					level.setBlock(target, expected, Block.UPDATE_ALL_IMMEDIATE),
					"Could not restore retained persistence state at slot " + index
				);
			}
			check(
				level.getBlockState(target).equals(expected),
				"Retained persistence state restore mismatch at slot " + index
			);
		}

		return new TimberReadEvidence(true, true, true, true);
	}

	private static FakePlayer survivalPlayer(ServerLevel level, String name, String identity) {
		FakePlayer player = FakePlayer.get(
			level,
			new GameProfile(
				UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8)),
				name
			)
		);
		player.setGameMode(GameType.SURVIVAL);
		check(player.gameMode() == GameType.SURVIVAL, "Fake player was not in survival mode");
		check(!player.hasInfiniteMaterials(), "Fake player unexpectedly had infinite materials");
		return player;
	}

	private static InteractionResult useHeldItem(
		ServerLevel level,
		FakePlayer player,
		BlockPos target
	) {
		return useHeldItem(level, player, target, Direction.UP);
	}

	private static InteractionResult useHeldItem(
		ServerLevel level,
		FakePlayer player,
		BlockPos target,
		Direction clickedFace
	) {
		return player.gameMode.useItemOn(
			player,
			level,
			player.getMainHandItem(),
			InteractionHand.MAIN_HAND,
			new BlockHitResult(Vec3.atCenterOf(target), clickedFace, target, false)
		);
	}

	private static void assertSuccess(InteractionResult result, String label) {
		check(result instanceof InteractionResult.Success, label + " did not return Success: " + result);
	}

	private static List<BlockState> persistenceStates() {
		List<BlockState> states = new ArrayList<>(EXPECTED_STATE_COUNT);
		states.addAll(hewingPersistenceStates());
		states.addAll(sawingPersistenceStates());
		states.addAll(splittingPersistenceStates());
		check(states.size() == EXPECTED_STATE_COUNT, "Total persistence state enumeration drifted");
		return List.copyOf(states);
	}

	private static List<BlockState> hewingPersistenceStates() {
		List<BlockState> states = new ArrayList<>(HEWING_STATE_COUNT);
		for (TimberType woodType : TimberType.values()) {
			TimberProcess process = timberProcess(woodType);
			for (Direction.Axis axis : Direction.Axis.values()) {
				for (int stage = 1; stage <= 3; stage++) {
					states.add(staged(process, axis, stage));
				}
			}
			for (Direction.Axis axis : Direction.Axis.values()) {
				states.add(beam(process, axis));
			}
		}
		check(states.size() == HEWING_STATE_COUNT, "Hewing state enumeration drifted");
		return List.copyOf(states);
	}

	private static List<BlockState> sawingPersistenceStates() {
		List<BlockState> states = new ArrayList<>(SAWING_STATE_COUNT);
		for (Direction facing : HORIZONTAL_FACINGS) {
			states.add(emptySawing(facing));
		}
		for (Direction facing : HORIZONTAL_FACINGS) {
			for (TimberType woodType : TimberType.values()) {
				for (int stage = 0; stage <= 3; stage++) {
					states.add(loadedSawing(facing, woodType, stage));
				}
			}
		}
		check(states.size() == SAWING_STATE_COUNT, "Sawing state enumeration drifted");
		return List.copyOf(states);
	}

	private static List<BlockState> splittingPersistenceStates() {
		List<BlockState> states = new ArrayList<>(SPLITTING_STATE_COUNT);
		for (Direction facing : HORIZONTAL_FACINGS) {
			states.add(emptySplitting(facing));
		}
		for (Direction facing : HORIZONTAL_FACINGS) {
			for (TimberType woodType : TimberType.values()) {
				for (TimberSplitKind splitKind : TimberSplitKind.values()) {
					for (boolean froeSet : List.of(false, true)) {
						for (int stage : LoadedSplittingStumpBlock.STRIKE_STAGE.getPossibleValues()) {
							states.add(loadedSplitting(facing, woodType, splitKind, froeSet, stage));
						}
					}
				}
			}
		}
		check(states.size() == SPLITTING_STATE_COUNT, "Splitting state enumeration drifted");
		return List.copyOf(states);
	}

	private static BlockState emptySawing(Direction facing) {
		return ModBlocks.SAWING_TRESTLES.defaultBlockState()
			.setValue(AbstractSawingTrestlesBlock.FACING, facing);
	}

	private static BlockState loadedSawing(Direction facing, TimberType woodType, int stage) {
		return ModBlocks.LOADED_SAWING_TRESTLES.defaultBlockState()
			.setValue(AbstractSawingTrestlesBlock.FACING, facing)
			.setValue(LoadedSawingTrestlesBlock.WOOD_TYPE, woodType)
			.setValue(LoadedSawingTrestlesBlock.CUT_STAGE, stage);
	}

	private static BlockState emptySplitting(Direction facing) {
		return ModBlocks.SPLITTING_STUMP.defaultBlockState()
			.setValue(AbstractSplittingStumpBlock.FACING, facing);
	}

	private static BlockState loadedSplitting(
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

	private static boolean isInteractionReachableSplittingState(BlockState state) {
		if (!state.is(ModBlocks.LOADED_SPLITTING_STUMP)
			|| state.getValue(LoadedSplittingStumpBlock.WOOD_TYPE) != TimberType.CHESTNUT) {
			return false;
		}
		boolean froeSet = state.getValue(LoadedSplittingStumpBlock.FROE_SET);
		return froeSet || (
			state.getValue(LoadedSplittingStumpBlock.SPLIT_KIND) == TimberSplitKind.SHINGLES
				&& state.getValue(LoadedSplittingStumpBlock.STRIKE_STAGE) == 0
		);
	}

	private static TimberProcess timberProcess(TimberType woodType) {
		return TimberProcessingRegistry.byType(woodType).orElseThrow(
			() -> new AssertionError("Missing timber process for " + woodType)
		);
	}

	private static TimberProcess timberProcessForHewingState(BlockState state) {
		for (TimberType woodType : TimberType.values()) {
			TimberProcess process = timberProcess(woodType);
			if (state.is(process.stagedBlock()) || state.is(process.finalBlock())) {
				return process;
			}
		}
		throw new AssertionError("No timber process owns hewing state " + state);
	}

	private static void clearCooldown(FakePlayer player, ItemStack stack) {
		player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(stack));
	}

	private static void assertNoItemEntities(ServerLevel level, BlockPos target, String label) {
		List<ItemEntity> entities = level.getEntities(
			EntityType.ITEM,
			new AABB(target).inflate(1.5),
			ItemEntity::isAlive
		);
		check(entities.isEmpty(), label + " emitted unexpected item entities: " + entities.size());
	}

	private static BlockState staged(TimberProcess process, Direction.Axis axis, int stage) {
		return process.stagedBlock().defaultBlockState()
			.setValue(RotatedPillarBlock.AXIS, axis)
			.setValue(HewingLogBlock.HEWING_STAGE, stage);
	}

	private static BlockState beam(TimberProcess process, Direction.Axis axis) {
		return process.finalBlock().defaultBlockState()
			.setValue(RotatedPillarBlock.AXIS, axis);
	}

	private static Path expectedSaveDirectory(ClientGameTestContext context, String worldName) {
		return context.computeOnClient(client -> client.getLevelSource()
			.getBaseDir()
			.resolve(worldName)
			.toAbsolutePath()
			.normalize());
	}

	private static Path canonical(Path path) {
		try {
			return path.toRealPath();
		} catch (IOException exception) {
			throw new AssertionError("Could not resolve real path: " + path, exception);
		}
	}

	private static void emitPass(
		TestConfig config,
		Path saveDirectory,
		long writerPid,
		long writerJvmStartMs,
		boolean readerSawContinuation,
		boolean readerSawRecovery,
		boolean readerSplitContinuation,
		boolean readerSplitRecovery
	) {
		String pathSha256 = sha256(saveDirectory.toString().toLowerCase(Locale.ROOT));
		System.out.println(
			"BTB_HEWING_CLIENT_PERSISTENCE"
				+ " phase=" + config.phase()
				+ " outcome=PASS"
				+ " pid=" + ProcessHandle.current().pid()
				+ " jvm_start_ms=" + ManagementFactory.getRuntimeMXBean().getStartTime()
				+ " writer_pid=" + writerPid
				+ " writer_jvm_start_ms=" + writerJvmStartMs
				+ " run_id=" + config.runId()
				+ " commit=" + config.expectedCommit()
				+ " world_name=" + config.worldName()
				+ " states=" + EXPECTED_STATE_COUNT
				+ " hewing_states=" + HEWING_STATE_COUNT
				+ " sawing_states=" + SAWING_STATE_COUNT
				+ " saw_loaded_states=" + SAWING_LOADED_STATE_COUNT
				+ " saw_empty_states=" + SAWING_EMPTY_STATE_COUNT
				+ " splitting_states=" + SPLITTING_STATE_COUNT
				+ " split_loaded_states=" + SPLITTING_LOADED_STATE_COUNT
				+ " split_empty_states=" + SPLITTING_EMPTY_STATE_COUNT
				+ " reader_saw_continuation=" + readerSawContinuation
				+ " reader_saw_recovery=" + readerSawRecovery
				+ " reader_split_continuation=" + readerSplitContinuation
				+ " reader_split_recovery=" + readerSplitRecovery
				+ " normal_close=true"
				+ " world_path_sha256=" + pathSha256
		);
	}

	private static String sha256(String value) {
		try {
			return HexFormat.of().formatHex(
				MessageDigest.getInstance("SHA-256")
					.digest(value.getBytes(StandardCharsets.UTF_8))
			);
		} catch (NoSuchAlgorithmException exception) {
			throw new AssertionError("SHA-256 was unavailable", exception);
		}
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}

	private record ReadEvidence(
		long writerPid,
		long writerJvmStartMs,
		boolean sawingContinuationVerified,
		boolean sawingRecoveryVerified,
		boolean splittingContinuationVerified,
		boolean splittingRecoveryVerified
	) {
	}

	private record TimberReadEvidence(
		boolean sawingContinuationVerified,
		boolean sawingRecoveryVerified,
		boolean splittingContinuationVerified,
		boolean splittingRecoveryVerified
	) {
	}

	private record TestConfig(
		String phase,
		String runId,
		String expectedCommit,
		String worldName
	) {
		private static TestConfig fromSystemProperties() {
			String phase = System.getProperty(PHASE_PROPERTY, "").trim();
			String runId = System.getProperty(RUN_ID_PROPERTY, "").trim();
			String expectedCommit = System.getProperty(COMMIT_PROPERTY, "")
				.trim()
				.toLowerCase(Locale.ROOT);
			check(phase.equals("write") || phase.equals("read"), "Phase must be write or read");
			check(
				runId.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,47}"),
				"Run ID was missing or unsafe"
			);
			check(expectedCommit.matches("[0-9a-f]{40}"), "Expected Git commit was invalid");
			return new TestConfig(
				phase,
				runId,
				expectedCommit,
				"btb-hewing-persistence-" + runId
			);
		}
	}
}
