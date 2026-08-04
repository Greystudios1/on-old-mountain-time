package net.beforetheblight.compat.seasons;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Optional-provider lifecycle that never links the optional API itself.
 *
 * <p>A detected provider is not operational until one snapshot call returns
 * normally. A linkage or runtime failure permanently selects baseline behavior
 * for the remainder of the process, so a broken adapter cannot throw on every
 * crop tick or tint query.</p>
 */
final class SeasonProviderHealth {
	enum Status {
		BASELINE,
		DETECTED,
		OPERATIONAL,
		FAILED
	}

	private record State(Status status, SeasonProvider provider) {
		private State {
			Objects.requireNonNull(status, "status");
			Objects.requireNonNull(provider, "provider");
		}
	}

	@FunctionalInterface
	private interface ProviderOperation<T> {
		T apply(SeasonProvider provider);
	}

	private final SeasonProvider baselineProvider;
	private final Runnable onOperational;
	private final Consumer<Throwable> onFailure;
	private final AtomicReference<State> state;

	SeasonProviderHealth(
		SeasonProvider baselineProvider,
		Runnable onOperational,
		Consumer<Throwable> onFailure
	) {
		this.baselineProvider = Objects.requireNonNull(
			baselineProvider,
			"baselineProvider"
		);
		this.onOperational = Objects.requireNonNull(onOperational, "onOperational");
		this.onFailure = Objects.requireNonNull(onFailure, "onFailure");
		this.state = new AtomicReference<>(
			new State(Status.BASELINE, baselineProvider)
		);
	}

	boolean detect(SeasonProvider detectedProvider) {
		Objects.requireNonNull(detectedProvider, "detectedProvider");
		State baseline = this.state.get();
		return baseline.status() == Status.BASELINE
			&& this.state.compareAndSet(
				baseline,
				new State(Status.DETECTED, detectedProvider)
			);
	}

	void failDetection(Throwable failure) {
		fail(this.state.get(), failure);
	}

	boolean isOperational() {
		return this.state.get().status() == Status.OPERATIONAL;
	}

	Status status() {
		return this.state.get().status();
	}

	Optional<SeasonalPlantClock.Snapshot> snapshot(Level level, BlockPos pos) {
		State observed = this.state.get();
		if (observed.status() == Status.BASELINE || observed.status() == Status.FAILED) {
			return Optional.empty();
		}

		Optional<SeasonalPlantClock.Snapshot> result;
		try {
			result = Objects.requireNonNull(
				observed.provider().snapshot(level, pos),
				"provider snapshot"
			);
		} catch (LinkageError | RuntimeException exception) {
			fail(observed, exception);
			return Optional.empty();
		}

		if (observed.status() == Status.DETECTED) {
			State operational = new State(Status.OPERATIONAL, observed.provider());
			if (this.state.compareAndSet(observed, operational)) {
				this.onOperational.run();
			}
		}

		State current = this.state.get();
		if (current.status() == Status.FAILED
			|| current.provider() != observed.provider()) {
			return Optional.empty();
		}
		return result;
	}

	boolean seasonalCropGrowthEnabled() {
		return callOperational(
			this.baselineProvider.seasonalCropGrowthEnabled(),
			SeasonProvider::seasonalCropGrowthEnabled
		);
	}

	boolean isCropFertile(
		Level level,
		BlockPos pos,
		SeasonalPlantClock.Plant plant
	) {
		return callOperational(
			this.baselineProvider.isCropFertile(level, pos, plant),
			provider -> provider.isCropFertile(level, pos, plant)
		);
	}

	boolean isUndergroundFertilityExempt(Level level, BlockPos pos) {
		return callOperational(
			this.baselineProvider.isUndergroundFertilityExempt(level, pos),
			provider -> provider.isUndergroundFertilityExempt(level, pos)
		);
	}

	boolean seasonalFoliageColorEnabled() {
		return callOperational(
			this.baselineProvider.seasonalFoliageColorEnabled(),
			SeasonProvider::seasonalFoliageColorEnabled
		);
	}

	private <T> T callOperational(T fallback, ProviderOperation<T> operation) {
		State observed = this.state.get();
		if (observed.status() != Status.OPERATIONAL) {
			return fallback;
		}

		T result;
		try {
			result = operation.apply(observed.provider());
		} catch (LinkageError | RuntimeException exception) {
			fail(observed, exception);
			return fallback;
		}

		State current = this.state.get();
		return current.status() == Status.OPERATIONAL
			&& current.provider() == observed.provider()
				? result
				: fallback;
	}

	private void fail(State observed, Throwable failure) {
		Objects.requireNonNull(failure, "failure");
		while (observed.status() != Status.FAILED) {
			State failed = new State(Status.FAILED, this.baselineProvider);
			if (this.state.compareAndSet(observed, failed)) {
				this.onFailure.accept(failure);
				return;
			}
			observed = this.state.get();
		}
	}
}
