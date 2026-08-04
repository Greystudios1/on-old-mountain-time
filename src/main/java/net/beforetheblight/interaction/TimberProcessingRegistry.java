package net.beforetheblight.interaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import net.beforetheblight.block.HewingLogBlock;
import net.beforetheblight.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Immutable, server-neutral description of timber-processing transitions.
 * Registration is deliberately closed after bootstrap so client prediction,
 * server mutation, drops, and future processing tools share one contract.
 */
public final class TimberProcessingRegistry {
	private static List<TimberProcess> processes = List.of();
	private static Map<TimberType, TimberProcess> processesByType = Map.of();
	private static Map<Block, TimberProcess> processesBySource = Map.of();
	private static Map<Block, TimberProcess> processesByStagedBlock = Map.of();
	private static Map<Block, TimberProcess> processesByBeam = Map.of();
	private static Map<Item, TimberProcess> processesByBeamItem = Map.of();
	private static boolean bootstrapped;

	private TimberProcessingRegistry() {
	}

	/**
	 * Installs the built-in processing contracts exactly once.
	 */
	public static void bootstrap() {
		if (bootstrapped) {
			throw new IllegalStateException("Timber processing registry has already been bootstrapped.");
		}

		Builder builder = new Builder();
		builder.add(
			TimberType.CHESTNUT,
			ModBlocks.CHESTNUT_LOG,
			ModBlocks.CHESTNUT_HEWING_LOG,
			ModBlocks.HEWN_CHESTNUT_BEAM,
			ModBlocks.CHESTNUT_LOG,
			new ProcessingOutput(ModBlocks.ROUGH_CHESTNUT_BOARDS, 4),
			Map.of(
				TimberSplitKind.SHINGLES,
				new ProcessingOutput(ModBlocks.CHESTNUT_SHINGLES, 4),
				TimberSplitKind.RAILS,
				new ProcessingOutput(ModBlocks.SPLIT_CHESTNUT_RAILS, 2)
			)
		);
		builder.add(
			TimberType.OAK,
			Blocks.OAK_LOG,
			ModBlocks.OAK_HEWING_LOG,
			ModBlocks.HEWN_OAK_BEAM,
			Blocks.OAK_LOG,
			new ProcessingOutput(ModBlocks.ROUGH_OAK_BOARDS, 4),
			Map.of()
		);
		builder.add(
			TimberType.SPRUCE,
			Blocks.SPRUCE_LOG,
			ModBlocks.SPRUCE_HEWING_LOG,
			ModBlocks.HEWN_SPRUCE_BEAM,
			Blocks.SPRUCE_LOG,
			new ProcessingOutput(ModBlocks.ROUGH_SPRUCE_BOARDS, 4),
			Map.of()
		);

		processes = List.copyOf(builder.processes);
		processesByType = Collections.unmodifiableMap(new EnumMap<>(builder.processesByType));
		processesBySource = immutableOrderedCopy(builder.processesBySource);
		processesByStagedBlock = immutableOrderedCopy(builder.processesByStagedBlock);
		processesByBeam = immutableOrderedCopy(builder.processesByBeam);
		processesByBeamItem = immutableOrderedCopy(builder.processesByBeamItem);
		bootstrapped = true;
	}

	/**
	 * Returns every built-in process in deterministic registration order.
	 */
	public static List<TimberProcess> all() {
		ensureBootstrapped();
		return processes;
	}

	/**
	 * Resolves a bounded timber identity to its complete processing contract.
	 */
	public static Optional<TimberProcess> byType(TimberType type) {
		Objects.requireNonNull(type, "type");
		ensureBootstrapped();
		return Optional.ofNullable(processesByType.get(type));
	}

	/**
	 * Resolves a placed hewn beam to its complete processing contract.
	 */
	public static Optional<TimberProcess> byBeam(Block beam) {
		Objects.requireNonNull(beam, "beam");
		ensureBootstrapped();
		return Optional.ofNullable(processesByBeam.get(beam));
	}

	/**
	 * Resolves a held hewn beam item to its complete processing contract.
	 */
	public static Optional<TimberProcess> byBeam(ItemStack beam) {
		Objects.requireNonNull(beam, "beam");
		ensureBootstrapped();
		return Optional.ofNullable(processesByBeamItem.get(beam.getItem()));
	}

	/**
	 * Computes exactly one strike without changing the world.
	 */
	public static Optional<Transition> transition(BlockState currentState) {
		Objects.requireNonNull(currentState, "currentState");
		ensureBootstrapped();

		TimberProcess fromSource = processesBySource.get(currentState.getBlock());
		if (fromSource != null) {
			Direction.Axis axis = currentState.getValue(RotatedPillarBlock.AXIS);
			BlockState nextState = fromSource.stagedBlock()
				.defaultBlockState()
				.setValue(RotatedPillarBlock.AXIS, axis)
				.setValue(HewingLogBlock.HEWING_STAGE, 1);
			return Optional.of(new Transition(fromSource, nextState, 1));
		}

		TimberProcess fromStage = processesByStagedBlock.get(currentState.getBlock());
		if (fromStage == null) {
			return Optional.empty();
		}

		int currentStage = currentState.getValue(HewingLogBlock.HEWING_STAGE);
		Direction.Axis axis = currentState.getValue(RotatedPillarBlock.AXIS);
		if (currentStage < HewingLogBlock.HEWING_STAGE.getPossibleValues().getLast()) {
			return Optional.of(new Transition(
				fromStage,
				currentState.setValue(HewingLogBlock.HEWING_STAGE, currentStage + 1),
				currentStage + 1
			));
		}

		BlockState finalState = fromStage.finalBlock()
			.defaultBlockState()
			.setValue(RotatedPillarBlock.AXIS, axis);
		return Optional.of(new Transition(fromStage, finalState, 4));
	}

	/**
	 * Returns the original log which a partially hewn state must drop.
	 */
	public static Optional<Block> partialReturn(BlockState state) {
		Objects.requireNonNull(state, "state");
		ensureBootstrapped();
		TimberProcess process = processesByStagedBlock.get(state.getBlock());
		return process == null ? Optional.empty() : Optional.of(process.partialReturnBlock());
	}

	public static boolean isBootstrapped() {
		return bootstrapped;
	}

	private static void ensureBootstrapped() {
		if (!bootstrapped) {
			throw new IllegalStateException("Timber processing registry has not been bootstrapped.");
		}
	}

	private static <K, V> Map<K, V> immutableOrderedCopy(Map<K, V> source) {
		return Collections.unmodifiableMap(new LinkedHashMap<>(source));
	}

	public record ProcessingOutput(ItemLike item, int count) {
		public ProcessingOutput {
			Objects.requireNonNull(item, "item");
			if (item.asItem() == Items.AIR) {
				throw new IllegalArgumentException("A timber-processing output must have an item form.");
			}
			if (count < 1) {
				throw new IllegalArgumentException("A timber-processing output count must be positive.");
			}
		}
	}

	public record TimberProcess(
		TimberType type,
		Block sourceBlock,
		HewingLogBlock stagedBlock,
		Block finalBlock,
		Block partialReturnBlock,
		ProcessingOutput roughBoards,
		Map<TimberSplitKind, ProcessingOutput> splitOutputs
	) {
		public TimberProcess {
			Objects.requireNonNull(type, "type");
			Objects.requireNonNull(sourceBlock, "sourceBlock");
			Objects.requireNonNull(stagedBlock, "stagedBlock");
			Objects.requireNonNull(finalBlock, "finalBlock");
			Objects.requireNonNull(partialReturnBlock, "partialReturnBlock");
			Objects.requireNonNull(roughBoards, "roughBoards");
			Objects.requireNonNull(splitOutputs, "splitOutputs");

			EnumMap<TimberSplitKind, ProcessingOutput> copiedSplitOutputs =
				new EnumMap<>(TimberSplitKind.class);
			copiedSplitOutputs.putAll(splitOutputs);
			if (copiedSplitOutputs.values().stream().anyMatch(Objects::isNull)) {
				throw new IllegalArgumentException("A timber split output cannot be null.");
			}
			splitOutputs = Collections.unmodifiableMap(copiedSplitOutputs);

			if (sourceBlock == stagedBlock) {
				throw new IllegalArgumentException("A hewing source and staged block must be different.");
			}
			if (finalBlock == sourceBlock || finalBlock == stagedBlock) {
				throw new IllegalArgumentException(
					"A final hewn block must be distinct from its source and staged blocks."
				);
			}
			if (partialReturnBlock == stagedBlock) {
				throw new IllegalArgumentException("A partial hewing drop cannot expose the staged block.");
			}
			if (!sourceBlock.defaultBlockState().hasProperty(RotatedPillarBlock.AXIS)) {
				throw new IllegalArgumentException("Hewing source must have an axis: " + sourceBlock);
			}
			if (!stagedBlock.defaultBlockState().hasProperty(RotatedPillarBlock.AXIS)
				|| !stagedBlock.defaultBlockState().hasProperty(HewingLogBlock.HEWING_STAGE)) {
				throw new IllegalArgumentException("Staged hewing block must have stage and axis properties: " + stagedBlock);
			}
			if (!finalBlock.defaultBlockState().hasProperty(RotatedPillarBlock.AXIS)) {
				throw new IllegalArgumentException("Final hewn block must have an axis: " + finalBlock);
			}
		}
	}

	public record Transition(TimberProcess process, BlockState nextState, int strike) {
		public Transition {
			Objects.requireNonNull(process, "process");
			Objects.requireNonNull(nextState, "nextState");
			if (strike < 1 || strike > 4) {
				throw new IllegalArgumentException("A hewing strike must be in the range 1..4.");
			}
		}
	}

	private static final class Builder {
		private final List<TimberProcess> processes = new ArrayList<>();
		private final Map<TimberType, TimberProcess> processesByType = new EnumMap<>(TimberType.class);
		private final Map<Block, TimberProcess> processesBySource = new LinkedHashMap<>();
		private final Map<Block, TimberProcess> processesByStagedBlock = new LinkedHashMap<>();
		private final Map<Block, TimberProcess> processesByBeam = new LinkedHashMap<>();
		private final Map<Item, TimberProcess> processesByBeamItem = new LinkedHashMap<>();
		private final Map<Block, String> blockRoles = new LinkedHashMap<>();

		private void add(
			TimberType type,
			Block source,
			HewingLogBlock staged,
			Block result,
			Block partialReturn,
			ProcessingOutput roughBoards,
			Map<TimberSplitKind, ProcessingOutput> splitOutputs
		) {
			Objects.requireNonNull(type, "type");
			claimRole(source, "source for " + type);
			claimRole(staged, "staged block for " + type);
			claimRole(result, "beam for " + type);

			TimberProcess process = new TimberProcess(
				type,
				source,
				staged,
				result,
				partialReturn,
				roughBoards,
				splitOutputs
			);
			if (this.processesByType.putIfAbsent(type, process) != null) {
				throw new IllegalArgumentException("Duplicate timber type: " + type);
			}
			Item beamItem = result.asItem();
			if (beamItem == Items.AIR) {
				throw new IllegalArgumentException("A hewn beam must have an item form: " + result);
			}
			if (this.processesByBeamItem.putIfAbsent(beamItem, process) != null) {
				throw new IllegalArgumentException("Duplicate timber-processing beam item: " + beamItem);
			}

			this.processes.add(process);
			this.processesBySource.put(source, process);
			this.processesByStagedBlock.put(staged, process);
			this.processesByBeam.put(result, process);
		}

		private void claimRole(Block block, String role) {
			Objects.requireNonNull(block, "block");
			String previousRole = this.blockRoles.putIfAbsent(block, role);
			if (previousRole != null) {
				throw new IllegalArgumentException(
					"Duplicate timber-processing block " + block + ": " + previousRole + " and " + role
				);
			}
		}
	}
}
