package net.beforetheblight.compat.seasons;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Required-mod-free regression coverage for the optional provider lifecycle.
 */
public final class SeasonProviderHealthGameTests {
	private static final SeasonProvider BASELINE_PROVIDER =
		(level, pos) -> Optional.empty();

	@GameTest(maxTicks = 20)
	public void exactVersionGateRejectsMissingOlderAndNewerPairs(GameTestHelper helper) {
		Map<String, String> exact = Map.of(
			SeasonalPlantClock.SERENE_SEASONS_MOD_ID,
			SeasonalPlantClock.SUPPORTED_SERENE_SEASONS_VERSION,
			SeasonalPlantClock.GLITCHCORE_MOD_ID,
			SeasonalPlantClock.SUPPORTED_GLITCHCORE_VERSION
		);
		helper.assertTrue(
			SeasonalPlantClock.supportsExactVersions(
				modId -> Optional.ofNullable(exact.get(modId))
			),
			"The exact-qualified Serene Seasons and GlitchCore pair was rejected"
		);

		for (Map<String, String> unsupported : List.of(
			Map.of(
				SeasonalPlantClock.SERENE_SEASONS_MOD_ID,
				"26.1.2.0.5",
				SeasonalPlantClock.GLITCHCORE_MOD_ID,
				SeasonalPlantClock.SUPPORTED_GLITCHCORE_VERSION
			),
			Map.of(
				SeasonalPlantClock.SERENE_SEASONS_MOD_ID,
				SeasonalPlantClock.SUPPORTED_SERENE_SEASONS_VERSION,
				SeasonalPlantClock.GLITCHCORE_MOD_ID,
				"26.1.2.0.1"
			),
			Map.of(
				SeasonalPlantClock.SERENE_SEASONS_MOD_ID,
				SeasonalPlantClock.SUPPORTED_SERENE_SEASONS_VERSION
			)
		)) {
			helper.assertTrue(
				!SeasonalPlantClock.supportsExactVersions(
					modId -> Optional.ofNullable(unsupported.get(modId))
				),
				"An unsupported optional-mod version pair passed the exact gate: "
					+ unsupported
			);
		}

		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void normalSnapshotPromotesDetectedProviderExactlyOnce(GameTestHelper helper) {
		AtomicInteger calls = new AtomicInteger();
		AtomicInteger operationalCallbacks = new AtomicInteger();
		AtomicInteger failureCallbacks = new AtomicInteger();
		SeasonProviderHealth health = new SeasonProviderHealth(
			BASELINE_PROVIDER,
			operationalCallbacks::incrementAndGet,
			failure -> failureCallbacks.incrementAndGet()
		);

		helper.assertTrue(
			health.detect((level, pos) -> {
				calls.incrementAndGet();
				return Optional.empty();
			}),
			"A baseline health state did not accept a detected provider"
		);
		helper.assertTrue(!health.isOperational(), "Detection was reported as health");
		helper.assertValueEqual(
			health.status(),
			SeasonProviderHealth.Status.DETECTED,
			"provider status before first snapshot"
		);

		health.snapshot(null, null);
		health.snapshot(null, null);
		helper.assertTrue(health.isOperational(), "Normal snapshot did not mark health");
		helper.assertValueEqual(calls.get(), 2, "healthy provider snapshot call count");
		helper.assertValueEqual(
			operationalCallbacks.get(),
			1,
			"operational callback count"
		);
		helper.assertValueEqual(failureCallbacks.get(), 0, "failure callback count");
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void runtimeFailurePermanentlyDemotesAndDoesNotRetry(GameTestHelper helper) {
		AtomicInteger snapshotCalls = new AtomicInteger();
		AtomicInteger configCalls = new AtomicInteger();
		AtomicInteger operationalCallbacks = new AtomicInteger();
		AtomicInteger failureCallbacks = new AtomicInteger();
		SeasonProvider provider = new SeasonProvider() {
			@Override
			public Optional<SeasonalPlantClock.Snapshot> snapshot(
				net.minecraft.world.level.Level level,
				net.minecraft.core.BlockPos pos
			) {
				snapshotCalls.incrementAndGet();
				return Optional.empty();
			}

			@Override
			public boolean seasonalCropGrowthEnabled() {
				configCalls.incrementAndGet();
				throw new IllegalStateException("synthetic runtime failure");
			}
		};
		SeasonProviderHealth health = new SeasonProviderHealth(
			BASELINE_PROVIDER,
			operationalCallbacks::incrementAndGet,
			failure -> failureCallbacks.incrementAndGet()
		);
		health.detect(provider);
		health.snapshot(null, null);

		helper.assertTrue(health.isOperational(), "Precondition provider was not healthy");
		helper.assertTrue(
			!health.seasonalCropGrowthEnabled(),
			"Runtime failure did not return the baseline config value"
		);
		helper.assertTrue(!health.isOperational(), "Runtime failure left health enabled");
		helper.assertValueEqual(
			health.status(),
			SeasonProviderHealth.Status.FAILED,
			"provider status after runtime failure"
		);

		health.seasonalCropGrowthEnabled();
		health.snapshot(null, null);
		helper.assertValueEqual(snapshotCalls.get(), 1, "snapshot calls after demotion");
		helper.assertValueEqual(configCalls.get(), 1, "config calls after demotion");
		helper.assertValueEqual(operationalCallbacks.get(), 1, "health callback count");
		helper.assertValueEqual(failureCallbacks.get(), 1, "failure callback count");
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void linkageFailureOnFirstSnapshotLogsOnceAndDoesNotRetry(GameTestHelper helper) {
		AtomicInteger calls = new AtomicInteger();
		AtomicInteger operationalCallbacks = new AtomicInteger();
		AtomicInteger failureCallbacks = new AtomicInteger();
		SeasonProviderHealth health = new SeasonProviderHealth(
			BASELINE_PROVIDER,
			operationalCallbacks::incrementAndGet,
			failure -> failureCallbacks.incrementAndGet()
		);
		health.detect((level, pos) -> {
			calls.incrementAndGet();
			throw new NoClassDefFoundError("synthetic optional API break");
		});

		health.snapshot(null, null);
		health.snapshot(null, null);
		helper.assertTrue(!health.isOperational(), "Linkage failure left health enabled");
		helper.assertValueEqual(calls.get(), 1, "linkage-failed provider retry count");
		helper.assertValueEqual(operationalCallbacks.get(), 0, "health callback count");
		helper.assertValueEqual(failureCallbacks.get(), 1, "failure callback count");
		helper.succeed();
	}
}
