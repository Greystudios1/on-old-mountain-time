package net.beforetheblight.gametest;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;

import com.mojang.serialization.Lifecycle;
import dev.worldgen.lithostitched.api.registry.LithostitchedRegistries;
import dev.worldgen.lithostitched.impl.worldgen.biomeinjector.internal.BiomeInjectorManager;
import dev.worldgen.lithostitched.impl.worldgen.biomeinjector.internal.InjectorBiomeSource;
import net.beforetheblight.BeforeTheBlight;
import net.beforetheblight.registry.ModTags;
import net.beforetheblight.worldgen.biome.CoveBiomeTags;
import net.beforetheblight.worldgen.biome.GrassyBaldBiomeTags;
import net.beforetheblight.worldgen.biome.ModBiomes;
import net.beforetheblight.worldgen.biome.RawInjectorBiomeSourceAccess;
import net.beforetheblight.worldgen.biome.RidgeMinimumFootprintFilter;
import net.beforetheblight.worldgen.feature.ModConfiguredFeatures;
import net.beforetheblight.worldgen.feature.RidgeTreeSelectorFeature;
import net.beforetheblight.worldgen.placement.StaggeredChestnutPlacement;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

public final class WorldgenGameTests {
	private static final int SCAN_LIMIT = 16_384;
	private static final int SCAN_STEP = 128;
	private static final int CONNECTIVITY_LIMIT = 2_048;
	private static final int CONNECTIVITY_STEP = 32;
	private static final int CONNECTIVITY_CONFIRMATION_STEP = 4;
	private static final int MIN_CONNECTED_SAMPLES = 16;
	private static final int MIN_RIDGE_HEIGHT_ABOVE_SEA = 48;
	private static final int MIN_CONNECTED_RIDGE_RELIEF = 24;
	private static final int MIN_LANDSCAPE_RELIEF = 32;
	private static final int MIN_RIDGE_ABOVE_RIVER = 32;
	private static final int MAX_NEAREST_RIVER_DISTANCE = 320;
	private static final int RIVER_REFINEMENT_STEP = 8;
	private static final int LANDSCAPE_AUDIT_RADIUS = 1_024;
	private static final int LANDSCAPE_AUDIT_STEP = 32;
	private static final int EDGE_AUDIT_RADIUS = 128;
	private static final int EDGE_AUDIT_STEP = 4;
	private static final int AUDIT_HEIGHT_WORKERS = Math.min(
		4,
		Math.max(1, Runtime.getRuntime().availableProcessors())
	);
	private static final int[][] CARDINAL_STEPS = {
		{CONNECTIVITY_STEP, 0},
		{-CONNECTIVITY_STEP, 0},
		{0, CONNECTIVITY_STEP},
		{0, -CONNECTIVITY_STEP}
	};

	@GameTest(maxTicks = 20)
	public void staggeredChestnutGridOwnsChunkSeams(GameTestHelper helper) {
		for (long seed : new long[] {0L, 20260722L}) {
			boolean expectedRotated = seed == 0L;
			String orientation = expectedRotated ? "rotated" : "unrotated";
			List<BlockPos> positions = new ArrayList<>();
			Set<Long> uniqueColumns = new HashSet<>();
			for (int chunkX = -8; chunkX <= 8; chunkX++) {
				for (int chunkZ = -8; chunkZ <= 8; chunkZ++) {
					List<BlockPos> chunkPositions = StaggeredChestnutPlacement.positionsForChunk(
						seed,
						chunkX,
						chunkZ,
						0
					);
					helper.assertValueEqual(
						chunkPositions,
						StaggeredChestnutPlacement.positionsForChunk(seed, chunkX, chunkZ, 0),
						"staggered " + orientation + " grid determinism for seed " + seed
							+ " in chunk " + chunkX + "," + chunkZ
					);
					for (BlockPos position : chunkPositions) {
						helper.assertValueEqual(
							Math.floorDiv(position.getX(), 16),
							chunkX,
							"staggered " + orientation + " grid X owner at " + position
						);
						helper.assertValueEqual(
							Math.floorDiv(position.getZ(), 16),
							chunkZ,
							"staggered " + orientation + " grid Z owner at " + position
						);
						helper.assertTrue(
							uniqueColumns.add(pack(position.getX(), position.getZ())),
							"staggered " + orientation
								+ " grid duplicated a chunk-seam column at " + position
						);
						positions.add(position);
					}
				}
			}

			double sitesPerChunk = positions.size() / (17.0D * 17.0D);
			helper.assertTrue(
				sitesPerChunk >= 1.85D && sitesPerChunk <= 2.05D,
				"staggered " + orientation + " grid density " + sitesPerChunk
					+ " lies outside 1.85..2.05 sites per chunk"
			);
			boolean hasHorizontalPitch = false;
			boolean hasVerticalPitch = false;
			for (BlockPos position : positions) {
				hasHorizontalPitch |= uniqueColumns.contains(pack(
					position.getX() + StaggeredChestnutPlacement.ALONG_ROW_PITCH,
					position.getZ()
				));
				hasVerticalPitch |= uniqueColumns.contains(pack(
					position.getX(),
					position.getZ() + StaggeredChestnutPlacement.ALONG_ROW_PITCH
				));
				if (position.getX() < -112 || position.getX() > 127
					|| position.getZ() < -112 || position.getZ() > 127) {
					continue;
				}
				long nearestSquared = Long.MAX_VALUE;
				for (BlockPos other : positions) {
					if (other.equals(position)) {
						continue;
					}
					long dx = other.getX() - position.getX();
					long dz = other.getZ() - position.getZ();
					nearestSquared = Math.min(nearestSquared, dx * dx + dz * dz);
				}
				helper.assertTrue(
					nearestSquared == 144L || nearestSquared == 157L,
					"staggered " + orientation + " grid nearest squared distance at " + position
						+ " was " + nearestSquared + "; expected 144 or 157"
				);
			}
			helper.assertValueEqual(
				hasHorizontalPitch,
				!expectedRotated,
				"staggered grid horizontal pitch identifies the " + orientation + " path for seed " + seed
			);
			helper.assertValueEqual(
				hasVerticalPitch,
				expectedRotated,
				"staggered grid vertical pitch identifies the " + orientation + " path for seed " + seed
			);
		}
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void ridgeMinimumFootprintClassifierIsDeterministic(GameTestHelper helper) {
		int minimum = RidgeMinimumFootprintFilter.MIN_COMPONENT_QUARTS;
		helper.assertValueEqual(minimum, 256, "Ridge minimum component size in biome quarts");

		assertSyntheticComponent(helper, lineComponent(0, 0, 0, 1), false, "single quart");
		Set<RidgeMinimumFootprintFilter.QuartCoordinate> diagonalPair = Set.of(
			new RidgeMinimumFootprintFilter.QuartCoordinate(0, 0, 0),
			new RidgeMinimumFootprintFilter.QuartCoordinate(1, 0, 1)
		);
		assertSyntheticComponent(helper, diagonalPair, false, "diagonal pair");
		assertSyntheticComponent(
			helper,
			lineComponent(0, 3, 0, minimum - 1),
			false,
			"255-quart component"
		);
		assertSyntheticComponent(
			helper,
			lineComponent(0, 3, 0, minimum),
			true,
			"256-quart component"
		);
		Set<RidgeMinimumFootprintFilter.QuartCoordinate> qualifying =
			lineComponent(0, 3, 0, minimum + 1);
		assertSyntheticComponent(helper, qualifying, true, "257-quart component");
		assertSyntheticComponent(
			helper,
			lineComponent(-700, -7, -900, minimum),
			true,
			"negative-coordinate component"
		);

		RidgeMinimumFootprintFilter.Classifier tinyCache =
			new RidgeMinimumFootprintFilter.Classifier(minimum, 4);
		int[] mixedOrder = {minimum, 0, 128, 1, minimum - 1, 64, 192};
		for (int x : mixedOrder) {
			helper.assertTrue(
				classifySynthetic(tinyCache, qualifying, x, 3, 0),
				"qualifying component changed under query order or cache eviction at X=" + x
			);
			helper.assertTrue(
				tinyCache.rawCacheSize() <= 4 && tinyCache.statusCacheSize() <= 4,
				"Ridge footprint caches exceeded their configured bound"
			);
		}

		Set<RidgeMinimumFootprintFilter.QuartCoordinate> undersized =
			lineComponent(-400, 11, 50, minimum - 1);
		for (int offset : new int[] {minimum - 2, 0, 127, 1, 200}) {
			helper.assertTrue(
				!classifySynthetic(tinyCache, undersized, -400 + offset, 11, 50),
				"undersized component changed under query order or cache eviction"
			);
		}
		helper.succeed();
	}

	@GameTest(maxTicks = 200)
	public void chestnutOakRidgeInjected(GameTestHelper helper) {
		long stageStartedNanos = System.nanoTime();
		RegistryAccess access = helper.getLevel().registryAccess();
		Holder.Reference<Biome> ridge = access.lookupOrThrow(Registries.BIOME)
			.getOrThrow(ModBiomes.CHESTNUT_OAK_RIDGE);

		helper.assertTrue(
			access.lookupOrThrow(Registries.BIOME)
				.getTagOrEmpty(ModTags.CHESTNUT_OAK_RIDGE_TARGETS)
				.iterator()
				.hasNext(),
			"Chestnut-Oak Ridge target tag did not decode or is empty"
		);
		helper.assertTrue(
			access.lookupOrThrow(LithostitchedRegistries.REGION).get(
				ResourceKey.create(LithostitchedRegistries.REGION, BeforeTheBlight.id("appalachian"))
			).isPresent(),
			"Lithostitched Appalachian region did not decode"
		);
		helper.assertTrue(
			access.lookupOrThrow(LithostitchedRegistries.BIOME_INJECTOR).get(
				ResourceKey.create(LithostitchedRegistries.BIOME_INJECTOR, BeforeTheBlight.id("chestnut_oak_ridge"))
			).isPresent(),
			"Lithostitched Chestnut-Oak Ridge injector did not decode"
		);

		WorldPreset normalPreset = access.lookupOrThrow(Registries.WORLD_PRESET)
			.getValueOrThrow(WorldPresets.NORMAL);
		Registry<LevelStem> emptyStems = new MappedRegistry<LevelStem>(
			Registries.LEVEL_STEM,
			Lifecycle.stable()
		).freeze();
		Registry<LevelStem> stems = normalPreset.createWorldDimensions().bake(emptyStems).dimensions();
		long seed = Long.getLong("before_the_blight.gametest.seed", 0L);
		BiomeInjectorManager.applyBiomeInjectors(access, stems, seed);

		ChunkGenerator overworldGenerator = stems
			.getValueOrThrow(LevelStem.OVERWORLD)
			.generator();
		helper.assertTrue(
			overworldGenerator instanceof NoiseBasedChunkGenerator,
			"NORMAL Overworld does not use the expected noise-based chunk generator"
		);
		NoiseBasedChunkGenerator generator = (NoiseBasedChunkGenerator) overworldGenerator;
		BiomeSource biomeSource = generator.getBiomeSource();
		helper.assertTrue(
			biomeSource instanceof InjectorBiomeSource,
			"NORMAL Overworld biome source was not wrapped by Lithostitched"
		);
		InjectorBiomeSource injectedBiomeSource = (InjectorBiomeSource) biomeSource;
		BiomeSource baselineBiomeSource = injectedBiomeSource.directDelegate();
		helper.assertTrue(
			injectedBiomeSource instanceof RawInjectorBiomeSourceAccess,
			"Ridge minimum-footprint raw biome-source access was not mixed in"
		);
		helper.assertTrue(
			biomeSource.possibleBiomes().contains(ridge),
			"Injected NORMAL Overworld biome source does not expose Chestnut-Oak Ridge"
		);
		generator.validate();
		printWorldgenStage(seed, "context_ready", stageStartedNanos);

		RandomState randomState = RandomState.create(
			generator.generatorSettings().value(),
			access.lookupOrThrow(Registries.NOISE),
			seed
		);
		LevelHeightAccessor heightAccessor = LevelHeightAccessor.create(
			generator.getMinY(),
			generator.getGenDepth()
		);
		int maxYExclusive = generator.getMinY() + generator.getGenDepth();
		RidgeProbe ridgeProbe = findRidge(
			generator,
			baselineBiomeSource,
			biomeSource,
			randomState,
			heightAccessor,
			seed
		);
		helper.assertTrue(
			ridgeProbe != null,
			"Chestnut-Oak Ridge did not produce a bounded composite witness with at least "
				+ "sixteen cardinally connected 32-block-grid surface-biome samples, a crest "
				+ "48 blocks above sea level, 24 blocks of connected relief, and a preserved "
				+ "river at least 32 blocks below that crest within 320 blocks in the "
				+ "NORMAL Overworld for seed " + seed
		);
		printWorldgenStage(seed, "ridge_witness", stageStartedNanos);

		helper.assertTrue(
			ridgeProbe.surfaceHeight() >= generator.getMinY()
				&& ridgeProbe.surfaceHeight() < maxYExclusive,
			"NORMAL terrain base height is outside generator bounds: " + ridgeProbe.surfaceHeight()
		);
		Holder<Biome> baselineAtProbe = sampleSurfaceBiome(
			generator,
			baselineBiomeSource,
			randomState,
			heightAccessor,
			ridgeProbe.x(),
			ridgeProbe.z()
		);
		helper.assertTrue(
			baselineAtProbe.is(ModTags.CHESTNUT_OAK_RIDGE_TARGETS),
			"Ridge replaced a baseline biome outside the low-erosion upland target mask"
		);

		LandscapeAudit landscape = auditLandscape(
			generator,
			baselineBiomeSource,
			biomeSource,
			randomState,
			heightAccessor,
			ridgeProbe.component().maximumX(),
			ridgeProbe.component().maximumZ(),
			ridgeProbe.supplementalRiverAudit()
		);
		printWorldgenStage(seed, "landscape_audit", stageStartedNanos);
		helper.assertTrue(
			landscape.nonTargetChanges() == 0,
			"Lithostitched replacement changed " + landscape.nonTargetChanges()
				+ " sampled surface biomes outside the matching target tag"
		);
		helper.assertTrue(
			landscape.ridgeTargetViolations() == 0,
			"Ridge appeared over " + landscape.ridgeTargetViolations()
				+ " sampled baseline biomes outside the upland target mask"
		);
		helper.assertTrue(
			landscape.riverChanges() == 0,
			"Ridge replacement changed " + landscape.riverChanges()
				+ " sampled vanilla river/frozen-river biomes"
		);
		helper.assertTrue(
			ridgeProbe.component().maximumHeight()
				>= generator.getSeaLevel() + MIN_RIDGE_HEIGHT_ABOVE_SEA
				&& landscape.maxRidgeHeight()
					>= generator.getSeaLevel() + MIN_RIDGE_HEIGHT_ABOVE_SEA,
			"Sampled Ridge component and landscape never both rose at least 48 blocks above sea level; "
				+ "sea=" + generator.getSeaLevel() + ", component_max="
				+ ridgeProbe.component().maximumHeight() + ", audit_max="
				+ landscape.maxRidgeHeight()
		);
		helper.assertTrue(
			ridgeProbe.component().relief() >= MIN_CONNECTED_RIDGE_RELIEF
				&& landscape.ridgeRelief() >= MIN_LANDSCAPE_RELIEF,
			"Sampled Ridge component must provide at least 24 blocks of relief and its "
				+ "crest-centred landscape at least 32; "
				+ "component=" + ridgeProbe.component().minimumHeight() + ".."
				+ ridgeProbe.component().maximumHeight() + ", audit="
				+ landscape.minRidgeHeight() + ".." + landscape.maxRidgeHeight()
		);
		helper.assertTrue(
			landscape.riverSamples() > 0
				&& landscape.nearestRiverDistance() <= MAX_NEAREST_RIVER_DISTANCE,
			"No preserved river/frozen-river sample was found within "
				+ MAX_NEAREST_RIVER_DISTANCE + " blocks of the Ridge crest; "
				+ "samples=" + landscape.riverSamples()
				+ ", nearest=" + landscape.nearestRiverDistance()
		);
		helper.assertTrue(
			ridgeProbe.component().maximumHeight() - landscape.nearestRiverHeight()
				>= MIN_RIDGE_ABOVE_RIVER,
			"The nearest preserved river sample was not at least 32 blocks below the nearby Ridge crest"
		);
		EdgeAudit edgeAudit = auditEdgeBand(
			generator,
			biomeSource,
			randomState,
			heightAccessor,
			ridgeProbe.component().maximumX(),
			ridgeProbe.component().maximumZ()
		);
		printWorldgenStage(seed, "edge_audit", stageStartedNanos);
		helper.assertTrue(
			edgeAudit.ridgeSamples() >= 25,
			"Dense edge audit found too few Ridge samples to characterize the tree transition"
		);
		helper.assertTrue(
			edgeAudit.edgeSamples() > 0,
			"Dense edge audit found no boundary Ridge positions for 1x1 chestnuts"
		);
		helper.assertTrue(
			edgeAudit.interiorSamples() > 0,
			"Dense edge audit found no interior Ridge positions for 2x2 and 3x3 chestnuts"
		);

		System.out.printf(
			"BTB_WORLDGEN_PROBE seed=%d ridge=%d,%d connected_samples=%d "
				+ "connectivity_step=%d surface_biome=true height=%d crest=%d,%d "
				+ "component_height=%d..%d "
				+ "component_relief=%d qualifying_components_examined=%d "
				+ "river_rejected_components=%d selected_component_ordinal=%d "
				+ "audit_ridge_samples=%d audit_height=%d..%d "
				+ "audit_relief=%d dense_ridge_samples=%d "
				+ "dense_edge_samples=%d dense_interior_samples=%d edge_permille=%d "
				+ "audit_river_samples=%d coarse_river_samples=%d "
				+ "coarse_nearest_river=%d river_refined=%s "
				+ "river_refinement_step=%d river_refinement_candidates=%d "
				+ "refined_river_samples=%d river_witness_source=%s "
				+ "river_height=%d..%d nearest_river=%d "
				+ "nearest_river_height=%d "
				+ "non_target_changes=%d target_violations=%d "
				+ "sea_level=%d bounds=%d..%d%n",
			seed,
			ridgeProbe.x(),
			ridgeProbe.z(),
			ridgeProbe.component().samples(),
			CONNECTIVITY_STEP,
			ridgeProbe.surfaceHeight(),
			ridgeProbe.component().maximumX(),
			ridgeProbe.component().maximumZ(),
			ridgeProbe.component().minimumHeight(),
			ridgeProbe.component().maximumHeight(),
			ridgeProbe.component().relief(),
			ridgeProbe.qualifyingComponentsExamined(),
			ridgeProbe.riverRejectedComponents(),
			ridgeProbe.selectedComponentOrdinal(),
			landscape.ridgeSamples(),
			landscape.minRidgeHeight(),
			landscape.maxRidgeHeight(),
			landscape.ridgeRelief(),
			edgeAudit.ridgeSamples(),
			edgeAudit.edgeSamples(),
			edgeAudit.interiorSamples(),
			edgeAudit.edgePermille(),
			landscape.riverSamples(),
			landscape.coarseRiverSamples(),
			landscape.coarseNearestRiverDistance(),
			landscape.riverRefined(),
			RIVER_REFINEMENT_STEP,
			landscape.riverRefinementCandidates(),
			landscape.refinedRiverSamples(),
			landscape.riverWitnessSource(),
			landscape.minRiverHeight(),
			landscape.maxRiverHeight(),
			landscape.nearestRiverDistance(),
			landscape.nearestRiverHeight(),
			landscape.nonTargetChanges(),
			landscape.ridgeTargetViolations(),
			generator.getSeaLevel(),
			generator.getMinY(),
			maxYExclusive - 1
		);
		helper.succeed();
	}

	private static void printWorldgenStage(long seed, String stage, long startedNanos) {
		long elapsedMillis = (System.nanoTime() - startedNanos) / 1_000_000L;
		System.out.printf(
			"BTB_WORLDGEN_STAGE seed=%d stage=%s elapsed_ms=%d%n",
			seed,
			stage,
			elapsedMillis
		);
	}

	private static RidgeProbe findRidge(
		NoiseBasedChunkGenerator generator,
		BiomeSource baselineBiomeSource,
		BiomeSource injectedBiomeSource,
		RandomState randomState,
		LevelHeightAccessor heightAccessor,
		long seed
	) {
		List<int[]> scanPositions = new ArrayList<>();
		scanPositions.add(new int[] {0, 0});
		for (int radius = SCAN_STEP; radius <= SCAN_LIMIT; radius += SCAN_STEP) {
			for (int x = -radius; x <= radius; x += SCAN_STEP) {
				scanPositions.add(new int[] {x, -radius});
				scanPositions.add(new int[] {x, radius});
			}

			for (int z = -radius + SCAN_STEP; z < radius; z += SCAN_STEP) {
				scanPositions.add(new int[] {-radius, z});
				scanPositions.add(new int[] {radius, z});
			}
		}

		Map<Long, Integer> surfaceHeightCache = new HashMap<>();
		Map<Long, Boolean> surfaceRidgeCache = new HashMap<>();
		Set<Long> rejectedCrests = new HashSet<>();
		int qualifyingComponentsExamined = 0;
		int riverRejectedComponents = 0;
		for (int[] position : scanPositions) {
			RidgeCandidate candidate = trySurfaceRidge(
				generator,
				injectedBiomeSource,
				randomState,
				heightAccessor,
				surfaceHeightCache,
				surfaceRidgeCache,
				position[0],
				position[1]
			);
			if (candidate == null) {
				continue;
			}
			long crestKey = pack(
				candidate.component().maximumX(),
				candidate.component().maximumZ()
			);
			if (rejectedCrests.contains(crestKey)) {
				continue;
			}

			qualifyingComponentsExamined++;
			RiverPreflight riverPreflight = findPreservedCreekWitness(
				generator,
				baselineBiomeSource,
				injectedBiomeSource,
				randomState,
				heightAccessor,
				candidate.component().maximumX(),
				candidate.component().maximumZ(),
				candidate.component().maximumHeight()
			);
			if (!riverPreflight.accepted()) {
				riverRejectedComponents++;
				rejectedCrests.add(crestKey);
				System.out.printf(
					"BTB_RIDGE_COMPONENT_REJECTED seed=%d origin=%d,%d crest=%d,%d "
						+ "reason=%s nearest_river=%d%n",
					seed,
					candidate.x(),
					candidate.z(),
					candidate.component().maximumX(),
					candidate.component().maximumZ(),
					riverPreflight.rejectionReason(),
					riverPreflight.nearestRiverDistance()
				);
				continue;
			}

			return new RidgeProbe(
				candidate.x(),
				candidate.z(),
				candidate.surfaceHeight(),
				candidate.component(),
				riverPreflight.supplementalRiverAudit(),
				qualifyingComponentsExamined,
				riverRejectedComponents,
				qualifyingComponentsExamined
			);
		}
		return null;
	}

	private static RidgeCandidate trySurfaceRidge(
		NoiseBasedChunkGenerator generator,
		BiomeSource biomeSource,
		RandomState randomState,
		LevelHeightAccessor heightAccessor,
		Map<Long, Integer> surfaceHeightCache,
		Map<Long, Boolean> surfaceRidgeCache,
		int x,
		int z
	) {
		if (!isCachedSurfaceRidge(
			generator,
			biomeSource,
			randomState,
			heightAccessor,
			surfaceHeightCache,
			surfaceRidgeCache,
			x,
			z
		)) {
			return null;
		}
		int surfaceHeight = getCachedSurfaceHeight(
			generator,
			randomState,
			heightAccessor,
			surfaceHeightCache,
			x,
			z
		);

		ComponentStats component = countConnectedSurfaceRidgeSamples(
			generator,
			biomeSource,
			randomState,
			heightAccessor,
			surfaceHeightCache,
			surfaceRidgeCache,
			x,
			z
		);
		if (component.samples() < MIN_CONNECTED_SAMPLES
			|| component.maximumHeight()
				< generator.getSeaLevel() + MIN_RIDGE_HEIGHT_ABOVE_SEA
			|| component.relief() < MIN_CONNECTED_RIDGE_RELIEF) {
			return null;
		}

		return new RidgeCandidate(x, z, surfaceHeight, component);
	}

	private static boolean isCachedSurfaceRidge(
		NoiseBasedChunkGenerator generator,
		BiomeSource biomeSource,
		RandomState randomState,
		LevelHeightAccessor heightAccessor,
		Map<Long, Integer> surfaceHeightCache,
		Map<Long, Boolean> surfaceRidgeCache,
		int x,
		int z
	) {
		long key = pack(x, z);
		Boolean cached = surfaceRidgeCache.get(key);
		if (cached != null) {
			return cached;
		}
		if (!mayContainRawRidge(generator, biomeSource, randomState, x, z)) {
			surfaceRidgeCache.put(key, false);
			return false;
		}
		int surfaceHeight = getCachedSurfaceHeight(
			generator,
			randomState,
			heightAccessor,
			surfaceHeightCache,
			x,
			z
		);
		boolean ridge = isRidge(biomeSource, randomState, x, surfaceHeight, z);
		surfaceRidgeCache.put(key, ridge);
		return ridge;
	}

	private static RiverPreflight findPreservedCreekWitness(
		NoiseBasedChunkGenerator generator,
		BiomeSource baselineBiomeSource,
		BiomeSource injectedBiomeSource,
		RandomState randomState,
		LevelHeightAccessor heightAccessor,
		int crestX,
		int crestZ,
		int crestHeight
	) {
		RiverAudit coarse = auditRiverLattice(
			generator,
			baselineBiomeSource,
			injectedBiomeSource,
			randomState,
			heightAccessor,
			crestX,
			crestZ,
			LANDSCAPE_AUDIT_STEP,
			false
		);
		if (coarse.riverSamples() > 0) {
			String rejectionReason = creekRejectionReason(coarse, crestHeight);
			return new RiverPreflight(
				rejectionReason.equals("accepted"),
				null,
				riverDistance(coarse),
				rejectionReason
			);
		}

		RiverAudit supplemental = auditRiverLattice(
			generator,
			baselineBiomeSource,
			injectedBiomeSource,
			randomState,
			heightAccessor,
			crestX,
			crestZ,
			RIVER_REFINEMENT_STEP,
			true
		);
		String rejectionReason = creekRejectionReason(supplemental, crestHeight);
		return new RiverPreflight(
			rejectionReason.equals("accepted"),
			supplemental,
			riverDistance(supplemental),
			rejectionReason
		);
	}

	private static String creekRejectionReason(RiverAudit audit, int crestHeight) {
		if (audit.riverSamples() == 0) {
			return "no_preserved_river_within_320";
		}
		if (audit.riverChanges() != 0) {
			return "river_not_preserved";
		}
		if (
			audit.nearestRiverHeight() == Integer.MAX_VALUE
				|| crestHeight - audit.nearestRiverHeight() < MIN_RIDGE_ABOVE_RIVER
		) {
			return "river_drop_below_32";
		}
		return "accepted";
	}

	private static int riverDistance(RiverAudit audit) {
		return audit.nearestRiverDistanceSquared() == Long.MAX_VALUE
			? -1
			: (int) Math.ceil(Math.sqrt(audit.nearestRiverDistanceSquared()));
	}

	private static boolean mayContainRawRidge(
		NoiseBasedChunkGenerator generator,
		BiomeSource biomeSource,
		RandomState randomState,
		int x,
		int z
	) {
		// A filtered Ridge can only come from raw Ridge at the same quart. Scan
		// every quart Y that the existing generator-bounds assertion can accept.
		if (!(biomeSource instanceof RawInjectorBiomeSourceAccess rawSource)) {
			return true;
		}
		int quartX = QuartPos.fromBlock(x);
		int quartZ = QuartPos.fromBlock(z);
		int minimumQuartY = QuartPos.fromBlock(generator.getMinY());
		int maximumQuartY = QuartPos.fromBlock(
			generator.getMinY() + generator.getGenDepth() - 1
		);
		for (int quartY = minimumQuartY; quartY <= maximumQuartY; quartY++) {
			if (rawSource.beforeTheBlight$getRawNoiseBiome(
				quartX,
				quartY,
				quartZ,
				randomState.sampler()
			).is(ModBiomes.CHESTNUT_OAK_RIDGE)) {
				return true;
			}
		}
		return false;
	}

	private static ComponentStats countConnectedSurfaceRidgeSamples(
		NoiseBasedChunkGenerator generator,
		BiomeSource biomeSource,
		RandomState randomState,
		LevelHeightAccessor heightAccessor,
		Map<Long, Integer> surfaceHeightCache,
		Map<Long, Boolean> surfaceRidgeCache,
		int originX,
		int originZ
	) {
		ArrayDeque<int[]> pending = new ArrayDeque<>();
		Set<Long> visited = new HashSet<>();
		pending.add(new int[] {originX, originZ});
		visited.add(pack(originX, originZ));
		int connectedSamples = 0;
		int minimumHeight = Integer.MAX_VALUE;
		int maximumHeight = Integer.MIN_VALUE;
		int maximumX = originX;
		int maximumZ = originZ;

		while (!pending.isEmpty()) {
			int[] position = pending.removeFirst();
			connectedSamples++;
			int positionHeight = getCachedSurfaceHeight(
				generator,
				randomState,
				heightAccessor,
				surfaceHeightCache,
				position[0],
				position[1]
			);
			minimumHeight = Math.min(minimumHeight, positionHeight);
			if (positionHeight > maximumHeight) {
				maximumHeight = positionHeight;
				maximumX = position[0];
				maximumZ = position[1];
			}

			for (int[] step : CARDINAL_STEPS) {
				int x = position[0] + step[0];
				int z = position[1] + step[1];
				if (
					Math.abs(x - originX) > CONNECTIVITY_LIMIT
						|| Math.abs(z - originZ) > CONNECTIVITY_LIMIT
						|| visited.contains(pack(x, z))
				) {
					continue;
				}

				if (isContinuousSurfaceRidgeSegment(
					generator,
					biomeSource,
					randomState,
					heightAccessor,
					surfaceHeightCache,
					surfaceRidgeCache,
					position[0],
					position[1],
					x,
					z
				)) {
					visited.add(pack(x, z));
					pending.addLast(new int[] {x, z});
				}
			}
		}

		return new ComponentStats(
			connectedSamples,
			minimumHeight,
			maximumHeight,
			maximumX,
			maximumZ
		);
	}

	private static boolean isContinuousSurfaceRidgeSegment(
		NoiseBasedChunkGenerator generator,
		BiomeSource biomeSource,
		RandomState randomState,
		LevelHeightAccessor heightAccessor,
		Map<Long, Integer> surfaceHeightCache,
		Map<Long, Boolean> surfaceRidgeCache,
		int fromX,
		int fromZ,
		int toX,
		int toZ
	) {
		int directionX = Integer.signum(toX - fromX);
		int directionZ = Integer.signum(toZ - fromZ);
		for (
			int offset = CONNECTIVITY_CONFIRMATION_STEP;
			offset <= CONNECTIVITY_STEP;
			offset += CONNECTIVITY_CONFIRMATION_STEP
		) {
			int x = fromX + directionX * offset;
			int z = fromZ + directionZ * offset;
			if (!isCachedSurfaceRidge(
				generator,
				biomeSource,
				randomState,
				heightAccessor,
				surfaceHeightCache,
				surfaceRidgeCache,
				x,
				z
			)) {
				return false;
			}
		}
		return true;
	}

	private static LandscapeAudit auditLandscape(
		NoiseBasedChunkGenerator generator,
		BiomeSource baselineBiomeSource,
		BiomeSource injectedBiomeSource,
		RandomState randomState,
		LevelHeightAccessor heightAccessor,
		int originX,
		int originZ,
		RiverAudit selectedSupplementalRiverAudit
	) {
		int ridgeSamples = 0;
		int riverSamples = 0;
		int riverChanges = 0;
		int nonTargetChanges = 0;
		int ridgeTargetViolations = 0;
		int minimumRidgeHeight = Integer.MAX_VALUE;
		int maximumRidgeHeight = Integer.MIN_VALUE;
		int minimumRiverHeight = Integer.MAX_VALUE;
		int maximumRiverHeight = Integer.MIN_VALUE;
		long nearestRiverDistanceSquared = Long.MAX_VALUE;
		int nearestRiverHeight = Integer.MAX_VALUE;
		int gridWidth = 2 * LANDSCAPE_AUDIT_RADIUS / LANDSCAPE_AUDIT_STEP + 1;
		int baseX = originX - LANDSCAPE_AUDIT_RADIUS;
		int baseZ = originZ - LANDSCAPE_AUDIT_RADIUS;
		int[][] surfaceHeights = sampleSurfaceHeightGrid(
			generator,
			randomState,
			heightAccessor,
			baseX,
			baseZ,
			gridWidth,
			LANDSCAPE_AUDIT_STEP
		);

		for (int gridX = 0; gridX < gridWidth; gridX++) {
			int x = baseX + gridX * LANDSCAPE_AUDIT_STEP;
			for (int gridZ = 0; gridZ < gridWidth; gridZ++) {
				int z = baseZ + gridZ * LANDSCAPE_AUDIT_STEP;
				int surfaceHeight = surfaceHeights[gridX][gridZ];
				Holder<Biome> baseline = sampleBiome(
					baselineBiomeSource,
					randomState,
					x,
					surfaceHeight,
					z
				);
				Holder<Biome> injected = sampleBiome(
					injectedBiomeSource,
					randomState,
					x,
					surfaceHeight,
					z
				);

				if (
					!injected.equals(baseline)
						&& !isExpectedAppalachianReplacement(baseline, injected)
				) {
					nonTargetChanges++;
				}
				if (baseline.is(BiomeTags.IS_RIVER)) {
					riverSamples++;
					minimumRiverHeight = Math.min(minimumRiverHeight, surfaceHeight);
					maximumRiverHeight = Math.max(maximumRiverHeight, surfaceHeight);
					if (!injected.equals(baseline)) {
						riverChanges++;
					}
					long deltaX = (long) x - originX;
					long deltaZ = (long) z - originZ;
					long riverDistanceSquared = deltaX * deltaX + deltaZ * deltaZ;
					if (riverDistanceSquared < nearestRiverDistanceSquared) {
						nearestRiverDistanceSquared = riverDistanceSquared;
						nearestRiverHeight = surfaceHeight;
					}
				}
				if (injected.is(ModBiomes.CHESTNUT_OAK_RIDGE)) {
					ridgeSamples++;
					minimumRidgeHeight = Math.min(minimumRidgeHeight, surfaceHeight);
					maximumRidgeHeight = Math.max(maximumRidgeHeight, surfaceHeight);
					if (!baseline.is(ModTags.CHESTNUT_OAK_RIDGE_TARGETS)) {
						ridgeTargetViolations++;
					}
				}
			}
		}

		int coarseRiverSamples = riverSamples;
		int coarseNearestRiverDistance = nearestRiverDistanceSquared == Long.MAX_VALUE
			? -1
			: (int) Math.ceil(Math.sqrt(nearestRiverDistanceSquared));
		boolean riverRefined = nearestRiverDistanceSquared
			> (long) MAX_NEAREST_RIVER_DISTANCE * MAX_NEAREST_RIVER_DISTANCE;
		int riverRefinementCandidates = 0;
		int refinedRiverSamples = 0;
		String riverWitnessSource = "coarse";
		if (riverRefined) {
			RiverAudit refinement = selectedSupplementalRiverAudit == null
				? auditRiverVicinity(
					generator,
					baselineBiomeSource,
					injectedBiomeSource,
					randomState,
					heightAccessor,
					originX,
					originZ
				)
				: selectedSupplementalRiverAudit;
			riverRefinementCandidates = refinement.candidateSamples();
			refinedRiverSamples = refinement.riverSamples();
			riverSamples += refinement.riverSamples();
			riverChanges += refinement.riverChanges();
			nonTargetChanges += refinement.nonTargetChanges();
			if (refinement.riverSamples() > 0) {
				minimumRiverHeight = Math.min(
					minimumRiverHeight,
					refinement.minimumRiverHeight()
				);
				maximumRiverHeight = Math.max(
					maximumRiverHeight,
					refinement.maximumRiverHeight()
				);
			}
			if (refinement.nearestRiverDistanceSquared() < nearestRiverDistanceSquared) {
				nearestRiverDistanceSquared = refinement.nearestRiverDistanceSquared();
				nearestRiverHeight = refinement.nearestRiverHeight();
				riverWitnessSource = "refined";
			}
		}

		return new LandscapeAudit(
			ridgeSamples,
			riverSamples,
			riverChanges,
			nonTargetChanges,
			ridgeTargetViolations,
			minimumRidgeHeight,
			maximumRidgeHeight,
			minimumRiverHeight,
			maximumRiverHeight,
			nearestRiverDistanceSquared == Long.MAX_VALUE
				? -1
				: (int) Math.ceil(Math.sqrt(nearestRiverDistanceSquared)),
			nearestRiverHeight == Integer.MAX_VALUE ? -1 : nearestRiverHeight,
			coarseRiverSamples,
			coarseNearestRiverDistance,
			riverRefined,
			riverRefinementCandidates,
			refinedRiverSamples,
			riverWitnessSource
		);
	}

	private static RiverAudit auditRiverVicinity(
		NoiseBasedChunkGenerator generator,
		BiomeSource baselineBiomeSource,
		BiomeSource injectedBiomeSource,
		RandomState randomState,
		LevelHeightAccessor heightAccessor,
		int originX,
		int originZ
	) {
		return auditRiverLattice(
			generator,
			baselineBiomeSource,
			injectedBiomeSource,
			randomState,
			heightAccessor,
			originX,
			originZ,
			RIVER_REFINEMENT_STEP,
			true
		);
	}

	private static RiverAudit auditRiverLattice(
		NoiseBasedChunkGenerator generator,
		BiomeSource baselineBiomeSource,
		BiomeSource injectedBiomeSource,
		RandomState randomState,
		LevelHeightAccessor heightAccessor,
		int originX,
		int originZ,
		int step,
		boolean excludeCoarseSamples
	) {
		int radius = MAX_NEAREST_RIVER_DISTANCE;
		long radiusSquared = (long) radius * radius;
		List<int[]> possibleRiverColumns = new ArrayList<>();
		int riverSamples = 0;
		int candidateSamples = 0;
		int riverChanges = 0;
		int nonTargetChanges = 0;
		int minimumRiverHeight = Integer.MAX_VALUE;
		int maximumRiverHeight = Integer.MIN_VALUE;
		long nearestRiverDistanceSquared = Long.MAX_VALUE;
		int nearestRiverHeight = Integer.MAX_VALUE;
		for (int x = originX - radius; x <= originX + radius; x += step) {
			long deltaX = (long) x - originX;
			for (int z = originZ - radius; z <= originZ + radius; z += step) {
				long deltaZ = (long) z - originZ;
				long distanceSquared = deltaX * deltaX + deltaZ * deltaZ;
				if (distanceSquared > radiusSquared) {
					continue;
				}
				if (
					excludeCoarseSamples
						&& deltaX % LANDSCAPE_AUDIT_STEP == 0
						&& deltaZ % LANDSCAPE_AUDIT_STEP == 0
				) {
					continue;
				}
				candidateSamples++;
				if (mayContainBaselineRiver(
					generator,
					baselineBiomeSource,
					randomState,
					x,
					z
				)) {
					possibleRiverColumns.add(new int[] {x, z});
				}
			}
		}

		int[] surfaceHeights = sampleSurfaceHeights(
			generator,
			randomState,
			heightAccessor,
			possibleRiverColumns
		);
		for (int index = 0; index < possibleRiverColumns.size(); index++) {
			int[] position = possibleRiverColumns.get(index);
			int x = position[0];
			int z = position[1];
			int surfaceHeight = surfaceHeights[index];
			long deltaX = (long) x - originX;
			long deltaZ = (long) z - originZ;
			long distanceSquared = deltaX * deltaX + deltaZ * deltaZ;
			Holder<Biome> baseline = sampleBiome(
				baselineBiomeSource,
				randomState,
				x,
				surfaceHeight,
				z
			);
			if (!baseline.is(BiomeTags.IS_RIVER)) {
				continue;
			}
			Holder<Biome> injected = sampleBiome(
				injectedBiomeSource,
				randomState,
				x,
				surfaceHeight,
				z
			);
			riverSamples++;
			minimumRiverHeight = Math.min(minimumRiverHeight, surfaceHeight);
			maximumRiverHeight = Math.max(maximumRiverHeight, surfaceHeight);
			if (!injected.equals(baseline)) {
				riverChanges++;
				if (!isExpectedAppalachianReplacement(baseline, injected)) {
					nonTargetChanges++;
				}
			}
			if (distanceSquared < nearestRiverDistanceSquared) {
				nearestRiverDistanceSquared = distanceSquared;
				nearestRiverHeight = surfaceHeight;
			}
		}
		return new RiverAudit(
			candidateSamples,
			riverSamples,
			riverChanges,
			nonTargetChanges,
			minimumRiverHeight,
			maximumRiverHeight,
			nearestRiverDistanceSquared,
			nearestRiverHeight
		);
	}

	private static boolean mayContainBaselineRiver(
		NoiseBasedChunkGenerator generator,
		BiomeSource baselineBiomeSource,
		RandomState randomState,
		int x,
		int z
	) {
		int quartX = QuartPos.fromBlock(x);
		int quartZ = QuartPos.fromBlock(z);
		int minimumQuartY = QuartPos.fromBlock(generator.getMinY());
		// getBaseHeight returns the first air Y and can therefore equal the
		// generator's exclusive top bound. Include that quart in this negative
		// prefilter; exact surface sampling remains the acceptance evidence.
		int maximumQuartY = QuartPos.fromBlock(
			generator.getMinY() + generator.getGenDepth()
		);
		for (int quartY = minimumQuartY; quartY <= maximumQuartY; quartY++) {
			if (baselineBiomeSource.getNoiseBiome(
				quartX,
				quartY,
				quartZ,
				randomState.sampler()
			).is(BiomeTags.IS_RIVER)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isExpectedAppalachianReplacement(
		Holder<Biome> baseline,
		Holder<Biome> injected
	) {
		return (
			injected.is(ModBiomes.CHESTNUT_OAK_RIDGE)
				&& baseline.is(ModTags.CHESTNUT_OAK_RIDGE_TARGETS)
		) || (
			injected.is(ModBiomes.HEMLOCK_BEECH_COVE)
				&& baseline.is(CoveBiomeTags.HEMLOCK_BEECH_COVE_TARGETS)
		) || (
			injected.is(ModBiomes.GRASSY_BALD)
				&& baseline.is(GrassyBaldBiomeTags.GRASSY_BALD_TARGETS)
		);
	}

	private static EdgeAudit auditEdgeBand(
		NoiseBasedChunkGenerator generator,
		BiomeSource biomeSource,
		RandomState randomState,
		LevelHeightAccessor heightAccessor,
		int originX,
		int originZ
	) {
		int ridgeEdgeRadius = ModConfiguredFeatures.RIDGE_EDGE_RADIUS;
		int paddedRadius = EDGE_AUDIT_RADIUS + ridgeEdgeRadius;
		int baseX = originX - paddedRadius;
		int baseZ = originZ - paddedRadius;
		int gridWidth = 2 * paddedRadius / EDGE_AUDIT_STEP + 1;
		boolean[][] ridgeGrid = new boolean[gridWidth][gridWidth];
		int[][] surfaceHeights = sampleSurfaceHeightGrid(
			generator,
			randomState,
			heightAccessor,
			baseX,
			baseZ,
			gridWidth,
			EDGE_AUDIT_STEP
		);
		for (int gridX = 0; gridX < gridWidth; gridX++) {
			int x = baseX + gridX * EDGE_AUDIT_STEP;
			for (int gridZ = 0; gridZ < gridWidth; gridZ++) {
				int z = baseZ + gridZ * EDGE_AUDIT_STEP;
				ridgeGrid[gridX][gridZ] = isRidge(
					biomeSource,
					randomState,
					x,
					surfaceHeights[gridX][gridZ],
					z
				);
			}
		}

		int ridgeSamples = 0;
		int edgeSamples = 0;
		int paddingSamples = ridgeEdgeRadius / EDGE_AUDIT_STEP;
		for (int gridX = paddingSamples; gridX < gridWidth - paddingSamples; gridX++) {
			for (int gridZ = paddingSamples; gridZ < gridWidth - paddingSamples; gridZ++) {
				if (!ridgeGrid[gridX][gridZ]) {
					continue;
				}
				ridgeSamples++;
				int x = baseX + gridX * EDGE_AUDIT_STEP;
				int z = baseZ + gridZ * EDGE_AUDIT_STEP;
				if (!RidgeTreeSelectorFeature.isInteriorRidge(
					(sampleX, sampleZ) -> ridgeGrid[
						(sampleX - baseX) / EDGE_AUDIT_STEP
					][(sampleZ - baseZ) / EDGE_AUDIT_STEP],
					x,
					z,
					ridgeEdgeRadius
				)) {
					edgeSamples++;
				}
			}
		}
		return new EdgeAudit(ridgeSamples, edgeSamples);
	}

	private static int[][] sampleSurfaceHeightGrid(
		NoiseBasedChunkGenerator generator,
		RandomState randomState,
		LevelHeightAccessor heightAccessor,
		int baseX,
		int baseZ,
		int gridWidth,
		int step
	) {
		int[][] heights = new int[gridWidth][gridWidth];
		ForkJoinPool heightPool = new ForkJoinPool(AUDIT_HEIGHT_WORKERS);
		try {
			heightPool.submit(() ->
				IntStream.range(0, gridWidth).parallel().forEach(gridX -> {
					int x = baseX + gridX * step;
					int[] row = heights[gridX];
					for (int gridZ = 0; gridZ < gridWidth; gridZ++) {
						row[gridZ] = getSurfaceHeight(
							generator,
							randomState,
							heightAccessor,
							x,
							baseZ + gridZ * step
						);
					}
				})
			).join();
		} finally {
			heightPool.shutdownNow();
		}
		return heights;
	}

	private static int[] sampleSurfaceHeights(
		NoiseBasedChunkGenerator generator,
		RandomState randomState,
		LevelHeightAccessor heightAccessor,
		List<int[]> positions
	) {
		int[] heights = new int[positions.size()];
		if (positions.isEmpty()) {
			return heights;
		}
		ForkJoinPool heightPool = new ForkJoinPool(AUDIT_HEIGHT_WORKERS);
		try {
			heightPool.submit(() ->
				IntStream.range(0, positions.size()).parallel().forEach(index -> {
					int[] position = positions.get(index);
					heights[index] = getSurfaceHeight(
						generator,
						randomState,
						heightAccessor,
						position[0],
						position[1]
					);
				})
			).join();
		} finally {
			heightPool.shutdownNow();
		}
		return heights;
	}

	private static Holder<Biome> sampleSurfaceBiome(
		NoiseBasedChunkGenerator generator,
		BiomeSource biomeSource,
		RandomState randomState,
		LevelHeightAccessor heightAccessor,
		int x,
		int z
	) {
		return sampleBiome(
			biomeSource,
			randomState,
			x,
			getSurfaceHeight(generator, randomState, heightAccessor, x, z),
			z
		);
	}

	private static int getSurfaceHeight(
		NoiseBasedChunkGenerator generator,
		RandomState randomState,
		LevelHeightAccessor heightAccessor,
		int x,
		int z
	) {
		return generator.getBaseHeight(
			x,
			z,
			Heightmap.Types.WORLD_SURFACE_WG,
			heightAccessor,
			randomState
		);
	}

	private static int getCachedSurfaceHeight(
		NoiseBasedChunkGenerator generator,
		RandomState randomState,
		LevelHeightAccessor heightAccessor,
		Map<Long, Integer> surfaceHeightCache,
		int x,
		int z
	) {
		long key = pack(x, z);
		Integer cached = surfaceHeightCache.get(key);
		if (cached != null) {
			return cached;
		}
		int height = getSurfaceHeight(generator, randomState, heightAccessor, x, z);
		surfaceHeightCache.put(key, height);
		return height;
	}

	private static boolean isRidge(
		BiomeSource biomeSource,
		RandomState randomState,
		int x,
		int y,
		int z
	) {
		return sampleBiome(biomeSource, randomState, x, y, z).is(ModBiomes.CHESTNUT_OAK_RIDGE);
	}

	private static Holder<Biome> sampleBiome(
		BiomeSource biomeSource,
		RandomState randomState,
		int x,
		int y,
		int z
	) {
		return biomeSource.getNoiseBiome(
			QuartPos.fromBlock(x),
			QuartPos.fromBlock(y),
			QuartPos.fromBlock(z),
			randomState.sampler()
		);
	}

	private static long pack(int x, int z) {
		return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
	}

	private static Set<RidgeMinimumFootprintFilter.QuartCoordinate> lineComponent(
		int startX,
		int y,
		int z,
		int length
	) {
		Set<RidgeMinimumFootprintFilter.QuartCoordinate> positions = new HashSet<>();
		for (int offset = 0; offset < length; offset++) {
			positions.add(new RidgeMinimumFootprintFilter.QuartCoordinate(startX + offset, y, z));
		}
		return positions;
	}

	private static boolean classifySynthetic(
		RidgeMinimumFootprintFilter.Classifier classifier,
		Set<RidgeMinimumFootprintFilter.QuartCoordinate> component,
		int x,
		int y,
		int z
	) {
		return classifier.shouldKeep(
			x,
			y,
			z,
			(sampleX, sampleY, sampleZ) -> component.contains(
				new RidgeMinimumFootprintFilter.QuartCoordinate(sampleX, sampleY, sampleZ)
			)
		);
	}

	private static void assertSyntheticComponent(
		GameTestHelper helper,
		Set<RidgeMinimumFootprintFilter.QuartCoordinate> component,
		boolean expectedKeep,
		String description
	) {
		RidgeMinimumFootprintFilter.Classifier classifier =
			new RidgeMinimumFootprintFilter.Classifier();
		for (RidgeMinimumFootprintFilter.QuartCoordinate position : component) {
			helper.assertValueEqual(
				classifySynthetic(
					classifier,
					component,
					position.x(),
					position.y(),
					position.z()
				),
				expectedKeep,
				description + " at " + position
			);
		}
	}

	private record RidgeCandidate(
		int x,
		int z,
		int surfaceHeight,
		ComponentStats component
	) {
	}

	private record RidgeProbe(
		int x,
		int z,
		int surfaceHeight,
		ComponentStats component,
		RiverAudit supplementalRiverAudit,
		int qualifyingComponentsExamined,
		int riverRejectedComponents,
		int selectedComponentOrdinal
	) {
	}

	private record RiverPreflight(
		boolean accepted,
		RiverAudit supplementalRiverAudit,
		int nearestRiverDistance,
		String rejectionReason
	) {
	}

	private record ComponentStats(
		int samples,
		int minimumHeight,
		int maximumHeight,
		int maximumX,
		int maximumZ
	) {
		private int relief() {
			return maximumHeight - minimumHeight;
		}
	}

	private record LandscapeAudit(
		int ridgeSamples,
		int riverSamples,
		int riverChanges,
		int nonTargetChanges,
		int ridgeTargetViolations,
		int minRidgeHeight,
		int maxRidgeHeight,
		int minRiverHeight,
		int maxRiverHeight,
		int nearestRiverDistance,
		int nearestRiverHeight,
		int coarseRiverSamples,
		int coarseNearestRiverDistance,
		boolean riverRefined,
		int riverRefinementCandidates,
		int refinedRiverSamples,
		String riverWitnessSource
	) {
		private int ridgeRelief() {
			return maxRidgeHeight - minRidgeHeight;
		}
	}

	private record RiverAudit(
		int candidateSamples,
		int riverSamples,
		int riverChanges,
		int nonTargetChanges,
		int minimumRiverHeight,
		int maximumRiverHeight,
		long nearestRiverDistanceSquared,
		int nearestRiverHeight
	) {
	}

	private record EdgeAudit(int ridgeSamples, int edgeSamples) {
		private int interiorSamples() {
			return ridgeSamples - edgeSamples;
		}

		private int edgePermille() {
			return ridgeSamples == 0 ? 0 : Math.round(1_000.0F * edgeSamples / ridgeSamples);
		}
	}
}
