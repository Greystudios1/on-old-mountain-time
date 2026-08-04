package net.beforetheblight.interaction;

import java.util.Objects;
import java.util.Optional;

import net.beforetheblight.block.AbstractSplittingStumpBlock;
import net.beforetheblight.block.LoadedSplittingStumpBlock;
import net.beforetheblight.block.SplittingStumpBlock;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;

/** Pure property-only transitions for hand splitting on a stump. */
public final class SplittingStateMachine {
	public static final int INITIAL_STRIKE_STAGE = 0;
	public static final int FINAL_STRIKE_STAGE = 2;

	private SplittingStateMachine() {
	}

	public static boolean isEmpty(BlockState state) {
		Objects.requireNonNull(state, "state");
		return state.getBlock() instanceof SplittingStumpBlock;
	}

	public static boolean isLoaded(BlockState state) {
		Objects.requireNonNull(state, "state");
		return state.getBlock() instanceof LoadedSplittingStumpBlock;
	}

	public static Optional<SplittingStumpBlock> registeredEmptyBlock() {
		return BuiltInRegistries.BLOCK.stream()
			.filter(SplittingStumpBlock.class::isInstance)
			.map(SplittingStumpBlock.class::cast)
			.findFirst();
	}

	public static Optional<LoadedSplittingStumpBlock> registeredLoadedBlock() {
		return BuiltInRegistries.BLOCK.stream()
			.filter(LoadedSplittingStumpBlock.class::isInstance)
			.map(LoadedSplittingStumpBlock.class::cast)
			.findFirst();
	}

	public static BlockState load(
		BlockState emptyState,
		LoadedSplittingStumpBlock loadedBlock,
		TimberType woodType
	) {
		Objects.requireNonNull(loadedBlock, "loadedBlock");
		Objects.requireNonNull(woodType, "woodType");
		Direction facing = requireEmpty(emptyState).getValue(AbstractSplittingStumpBlock.FACING);
		return loadedBlock.defaultBlockState()
			.setValue(AbstractSplittingStumpBlock.FACING, facing)
			.setValue(LoadedSplittingStumpBlock.WOOD_TYPE, woodType)
			.setValue(LoadedSplittingStumpBlock.SPLIT_KIND, TimberSplitKind.SHINGLES)
			.setValue(LoadedSplittingStumpBlock.FROE_SET, false)
			.setValue(LoadedSplittingStumpBlock.STRIKE_STAGE, INITIAL_STRIKE_STAGE);
	}

	/**
	 * Sets the froe before striking. At stage zero it may be reoriented; after
	 * the first maul strike the chosen outcome is locked.
	 */
	public static FroeTransition setFroe(BlockState loadedState, TimberSplitKind splitKind) {
		Objects.requireNonNull(splitKind, "splitKind");
		BlockState checkedState = requireLoaded(loadedState);
		boolean wasSet = checkedState.getValue(LoadedSplittingStumpBlock.FROE_SET);
		TimberSplitKind previousKind = checkedState.getValue(LoadedSplittingStumpBlock.SPLIT_KIND);
		int strikeStage = checkedState.getValue(LoadedSplittingStumpBlock.STRIKE_STAGE);
		boolean changed = !wasSet || (strikeStage == INITIAL_STRIKE_STAGE && previousKind != splitKind);
		BlockState nextState = changed
			? checkedState
				.setValue(LoadedSplittingStumpBlock.SPLIT_KIND, splitKind)
				.setValue(LoadedSplittingStumpBlock.FROE_SET, true)
			: checkedState;
		return new FroeTransition(previousKind, splitKind, wasSet, strikeStage, nextState, changed);
	}

	/** Three accepted strikes advance 0 -> 1 -> 2 -> empty. */
	public static StrikeTransition strike(
		BlockState loadedState,
		SplittingStumpBlock emptyBlock
	) {
		Objects.requireNonNull(emptyBlock, "emptyBlock");
		BlockState checkedState = requireLoaded(loadedState);
		if (!checkedState.getValue(LoadedSplittingStumpBlock.FROE_SET)) {
			throw new IllegalStateException("A froe must be set before striking the beam.");
		}

		TimberType woodType = checkedState.getValue(LoadedSplittingStumpBlock.WOOD_TYPE);
		TimberSplitKind splitKind = checkedState.getValue(LoadedSplittingStumpBlock.SPLIT_KIND);
		int previousStage = checkedState.getValue(LoadedSplittingStumpBlock.STRIKE_STAGE);
		if (previousStage < FINAL_STRIKE_STAGE) {
			return new StrikeTransition(
				woodType,
				splitKind,
				previousStage,
				checkedState.setValue(LoadedSplittingStumpBlock.STRIKE_STAGE, previousStage + 1),
				false
			);
		}
		return new StrikeTransition(
			woodType,
			splitKind,
			previousStage,
			emptyStateFor(checkedState, emptyBlock),
			true
		);
	}

	public static UnloadTransition unload(
		BlockState loadedState,
		SplittingStumpBlock emptyBlock
	) {
		Objects.requireNonNull(emptyBlock, "emptyBlock");
		BlockState checkedState = requireLoaded(loadedState);
		return new UnloadTransition(
			checkedState.getValue(LoadedSplittingStumpBlock.WOOD_TYPE),
			checkedState.getValue(LoadedSplittingStumpBlock.SPLIT_KIND),
			checkedState.getValue(LoadedSplittingStumpBlock.FROE_SET),
			checkedState.getValue(LoadedSplittingStumpBlock.STRIKE_STAGE),
			emptyStateFor(checkedState, emptyBlock)
		);
	}

	private static BlockState emptyStateFor(
		BlockState loadedState,
		SplittingStumpBlock emptyBlock
	) {
		return emptyBlock.defaultBlockState().setValue(
			AbstractSplittingStumpBlock.FACING,
			loadedState.getValue(AbstractSplittingStumpBlock.FACING)
		);
	}

	private static BlockState requireEmpty(BlockState state) {
		Objects.requireNonNull(state, "state");
		if (!isEmpty(state)) {
			throw new IllegalArgumentException("Expected empty splitting stump, got " + state);
		}
		return state;
	}

	private static BlockState requireLoaded(BlockState state) {
		Objects.requireNonNull(state, "state");
		if (!isLoaded(state)) {
			throw new IllegalArgumentException("Expected loaded splitting stump, got " + state);
		}
		return state;
	}

	public record FroeTransition(
		TimberSplitKind previousKind,
		TimberSplitKind selectedKind,
		boolean wasSet,
		int strikeStage,
		BlockState nextState,
		boolean changed
	) {
		public FroeTransition {
			Objects.requireNonNull(previousKind, "previousKind");
			Objects.requireNonNull(selectedKind, "selectedKind");
			Objects.requireNonNull(nextState, "nextState");
			if (strikeStage < INITIAL_STRIKE_STAGE || strikeStage > FINAL_STRIKE_STAGE) {
				throw new IllegalArgumentException("Strike stage must be in the range 0..2.");
			}
			if (!isLoaded(nextState)) {
				throw new IllegalArgumentException("Setting a froe must remain on a loaded stump.");
			}
		}
	}

	public record StrikeTransition(
		TimberType woodType,
		TimberSplitKind splitKind,
		int previousStage,
		BlockState nextState,
		boolean completed
	) {
		public StrikeTransition {
			Objects.requireNonNull(woodType, "woodType");
			Objects.requireNonNull(splitKind, "splitKind");
			Objects.requireNonNull(nextState, "nextState");
			if (previousStage < INITIAL_STRIKE_STAGE || previousStage > FINAL_STRIKE_STAGE) {
				throw new IllegalArgumentException("Strike stage must be in the range 0..2.");
			}
			if (completed != isEmpty(nextState)) {
				throw new IllegalArgumentException("Only the third strike may empty the stump.");
			}
		}
	}

	public record UnloadTransition(
		TimberType woodType,
		TimberSplitKind splitKind,
		boolean froeSet,
		int strikeStage,
		BlockState nextState
	) {
		public UnloadTransition {
			Objects.requireNonNull(woodType, "woodType");
			Objects.requireNonNull(splitKind, "splitKind");
			Objects.requireNonNull(nextState, "nextState");
			if (strikeStage < INITIAL_STRIKE_STAGE || strikeStage > FINAL_STRIKE_STAGE) {
				throw new IllegalArgumentException("Strike stage must be in the range 0..2.");
			}
			if (!isEmpty(nextState)) {
				throw new IllegalArgumentException("Unloading must produce an empty stump.");
			}
		}
	}
}
