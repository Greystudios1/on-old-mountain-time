package net.beforetheblight.interaction;

import java.util.Objects;

import net.beforetheblight.block.AbstractSawingTrestlesBlock;
import net.beforetheblight.block.LoadedSawingTrestlesBlock;
import net.beforetheblight.block.SawingTrestlesBlock;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Pure state transitions for the two sawing-trestles blocks.
 *
 * <p>The world, inventory, drops, sounds, and tool durability deliberately do
 * not appear here. Callers can therefore decide and test a transition before
 * applying any server-authoritative side effects.</p>
 */
public final class SawingTrestleStateMachine {
	public static final int INITIAL_CUT_STAGE = 0;
	public static final int FINAL_CUT_STAGE = 3;

	private SawingTrestleStateMachine() {
	}

	/**
	 * Returns whether {@code state} is the empty sawing-trestles block.
	 */
	public static boolean isEmpty(BlockState state) {
		Objects.requireNonNull(state, "state");
		return state.getBlock() instanceof SawingTrestlesBlock;
	}

	/**
	 * Returns whether {@code state} is the loaded sawing-trestles block.
	 */
	public static boolean isLoaded(BlockState state) {
		Objects.requireNonNull(state, "state");
		return state.getBlock() instanceof LoadedSawingTrestlesBlock;
	}

	/**
	 * Resolves the registered empty form without coupling the interaction code
	 * to the registration holder. Codecs may construct transient block objects,
	 * so only instances present in the block registry are considered.
	 */
	public static java.util.Optional<SawingTrestlesBlock> registeredEmptyBlock() {
		return BuiltInRegistries.BLOCK.stream()
			.filter(SawingTrestlesBlock.class::isInstance)
			.map(SawingTrestlesBlock.class::cast)
			.findFirst();
	}

	/**
	 * Resolves the registered loaded form. Keeping the counterpart lookup here
	 * lets the two blocks retain their property-only codecs.
	 */
	public static java.util.Optional<LoadedSawingTrestlesBlock> registeredLoadedBlock() {
		return BuiltInRegistries.BLOCK.stream()
			.filter(LoadedSawingTrestlesBlock.class::isInstance)
			.map(LoadedSawingTrestlesBlock.class::cast)
			.findFirst();
	}

	/**
	 * Loads an empty set of trestles at stage zero while preserving its facing.
	 */
	public static BlockState load(
		BlockState emptyState,
		LoadedSawingTrestlesBlock loadedBlock,
		TimberType woodType
	) {
		Objects.requireNonNull(loadedBlock, "loadedBlock");
		Objects.requireNonNull(woodType, "woodType");
		Direction facing = requireEmpty(emptyState).getValue(AbstractSawingTrestlesBlock.FACING);
		return loadedBlock.defaultBlockState()
			.setValue(AbstractSawingTrestlesBlock.FACING, facing)
			.setValue(LoadedSawingTrestlesBlock.WOOD_TYPE, woodType)
			.setValue(LoadedSawingTrestlesBlock.CUT_STAGE, INITIAL_CUT_STAGE);
	}

	/**
	 * Advances one saw stroke. Stage three completes into empty trestles; all
	 * earlier stages remain loaded. Facing and timber identity are reported in
	 * the returned value so the caller can perform output side effects exactly
	 * once after it successfully installs {@link CutTransition#nextState()}.
	 */
	public static CutTransition advance(
		BlockState loadedState,
		SawingTrestlesBlock emptyBlock
	) {
		Objects.requireNonNull(emptyBlock, "emptyBlock");
		BlockState checkedState = requireLoaded(loadedState);
		TimberType woodType = checkedState.getValue(LoadedSawingTrestlesBlock.WOOD_TYPE);
		int previousStage = checkedState.getValue(LoadedSawingTrestlesBlock.CUT_STAGE);

		if (previousStage < FINAL_CUT_STAGE) {
			return new CutTransition(
				woodType,
				previousStage,
				checkedState.setValue(LoadedSawingTrestlesBlock.CUT_STAGE, previousStage + 1),
				false
			);
		}

		return new CutTransition(
			woodType,
			previousStage,
			emptyStateFor(checkedState, emptyBlock),
			true
		);
	}

	/**
	 * Describes removing an unfinished beam without creating an item or changing
	 * the world. The caller owns recovery, inventory, and drop behavior.
	 */
	public static UnloadTransition unload(
		BlockState loadedState,
		SawingTrestlesBlock emptyBlock
	) {
		Objects.requireNonNull(emptyBlock, "emptyBlock");
		BlockState checkedState = requireLoaded(loadedState);
		return new UnloadTransition(
			checkedState.getValue(LoadedSawingTrestlesBlock.WOOD_TYPE),
			checkedState.getValue(LoadedSawingTrestlesBlock.CUT_STAGE),
			emptyStateFor(checkedState, emptyBlock)
		);
	}

	/**
	 * Produces the empty form of a loaded state while preserving its facing.
	 */
	public static BlockState empty(
		BlockState loadedState,
		SawingTrestlesBlock emptyBlock
	) {
		Objects.requireNonNull(emptyBlock, "emptyBlock");
		return emptyStateFor(requireLoaded(loadedState), emptyBlock);
	}

	private static BlockState emptyStateFor(
		BlockState loadedState,
		SawingTrestlesBlock emptyBlock
	) {
		return emptyBlock.defaultBlockState().setValue(
			AbstractSawingTrestlesBlock.FACING,
			loadedState.getValue(AbstractSawingTrestlesBlock.FACING)
		);
	}

	private static BlockState requireEmpty(BlockState state) {
		Objects.requireNonNull(state, "state");
		if (!isEmpty(state)) {
			throw new IllegalArgumentException("Expected empty sawing trestles, got " + state);
		}
		return state;
	}

	private static BlockState requireLoaded(BlockState state) {
		Objects.requireNonNull(state, "state");
		if (!isLoaded(state)) {
			throw new IllegalArgumentException("Expected loaded sawing trestles, got " + state);
		}
		return state;
	}

	/**
	 * One deterministic saw stroke. {@code completed} is true only for the
	 * stage-three transition to empty trestles.
	 */
	public record CutTransition(
		TimberType woodType,
		int previousStage,
		BlockState nextState,
		boolean completed
	) {
		public CutTransition {
			Objects.requireNonNull(woodType, "woodType");
			Objects.requireNonNull(nextState, "nextState");
			if (previousStage < INITIAL_CUT_STAGE || previousStage > FINAL_CUT_STAGE) {
				throw new IllegalArgumentException("Cut stage must be in the range 0..3.");
			}
			if (completed != isEmpty(nextState)) {
				throw new IllegalArgumentException(
					"Only a completed cut may transition to empty sawing trestles."
				);
			}
		}
	}

	/**
	 * Recovery context for removing an unfinished beam from the workstation.
	 */
	public record UnloadTransition(
		TimberType woodType,
		int cutStage,
		BlockState nextState
	) {
		public UnloadTransition {
			Objects.requireNonNull(woodType, "woodType");
			Objects.requireNonNull(nextState, "nextState");
			if (cutStage < INITIAL_CUT_STAGE || cutStage > FINAL_CUT_STAGE) {
				throw new IllegalArgumentException("Cut stage must be in the range 0..3.");
			}
			if (!isEmpty(nextState)) {
				throw new IllegalArgumentException("Unloading must produce empty sawing trestles.");
			}
		}
	}
}
