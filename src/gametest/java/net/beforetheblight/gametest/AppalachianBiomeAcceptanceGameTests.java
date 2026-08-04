package net.beforetheblight.gametest;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.mojang.serialization.Lifecycle;
import dev.worldgen.lithostitched.api.registry.LithostitchedRegistries;
import dev.worldgen.lithostitched.impl.worldgen.biomeinjector.internal.BiomeInjectorManager;
import dev.worldgen.lithostitched.impl.worldgen.biomeinjector.internal.InjectorBiomeSource;
import net.beforetheblight.BeforeTheBlight;
import net.beforetheblight.worldgen.biome.CoveBiomeTags;
import net.beforetheblight.worldgen.biome.CoveMinimumFootprintFilter;
import net.beforetheblight.worldgen.biome.GrassyBaldBiomeTags;
import net.beforetheblight.worldgen.biome.ModBiomes;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Runtime acceptance probes used by {@code tools/Run-AppalachianBiomeSeedMatrix.ps1}.
 * Each Gradle invocation supplies one seed and creates one isolated fresh GameTest world.
 * These tests inspect the real NORMAL preset after Lithostitched injection; they do not
 * substitute a synthetic biome source.
 */
public final class AppalachianBiomeAcceptanceGameTests {
	private static final int LOCATE_LIMIT_BLOCKS = 16_384;
	private static final int LOCATE_STEP_BLOCKS = 128;
	private static final int LOCAL_RADIUS_BLOCKS = 512;
	private static final int LOCAL_STEP_BLOCKS = 64;
	private static final int TRANSITION_LIMIT_BLOCKS = 4_096;
	private static final int TRANSITION_STEP_BLOCKS = 128;
	private static final int MIN_BALD_SURFACE_QUARTS = 16;
	private static final int[] LOCATE_PROBE_HEIGHTS = {
		64, 80, 96, 112, 128, 144, 160, 176, 192, 208, 224, 240, 256, 272, 288, 304
	};
	private static final int[][] CARDINAL_QUARTS = {
		{1, 0},
		{-1, 0},
		{0, 1},
		{0, -1}
	};
	private static WorldContext cachedWorld;

	@GameTest(maxTicks = 200)
	public void hemlockBeechCoveFreshSeedAcceptance(GameTestHelper helper) {
		WorldContext world = createWorldContext(helper);
		assertInjector(helper, world, "hemlock_beech_cove");
		helper.assertTrue(
			world.biomeSource().possibleBiomes().stream().anyMatch(holder -> holder.is(ModBiomes.HEMLOCK_BEECH_COVE)),
			"Injected NORMAL Overworld does not expose Hemlock Cove"
		);

		SurfaceSample probe = findSurfaceBiome(world, ModBiomes.HEMLOCK_BEECH_COVE);
		helper.assertTrue(
			probe != null,
			"Hemlock Cove was not located within " + LOCATE_LIMIT_BLOCKS
				+ " blocks for seed " + world.seed()
		);
		Holder<Biome> baseline = sampleSurfaceBiome(world, world.baselineBiomeSource(), probe.x(), probe.z());
		helper.assertTrue(
			baseline.is(CoveBiomeTags.HEMLOCK_BEECH_COVE_TARGETS),
			"Cove replaced a baseline biome outside its target tag"
		);

		ComponentAudit footprint = auditSurfaceComponent(
			world,
			probe,
			ModBiomes.HEMLOCK_BEECH_COVE,
			CoveMinimumFootprintFilter.MIN_COMPONENT_QUARTS
		);
		helper.assertTrue(
			footprint.samples() >= CoveMinimumFootprintFilter.MIN_COMPONENT_QUARTS,
			"Cove surface component contained only " + footprint.samples()
				+ " connected quart samples; expected at least "
				+ CoveMinimumFootprintFilter.MIN_COMPONENT_QUARTS
		);
		LocalDensity density = auditLocalDensity(world, probe, ModBiomes.HEMLOCK_BEECH_COVE);
		TransitionProbe transition = findNearestSampledTransition(
			world,
			probe,
			holder -> !holder.is(ModBiomes.HEMLOCK_BEECH_COVE)
		);
		helper.assertTrue(
			transition != null,
			"No sampled exit from Hemlock Cove was found within "
				+ TRANSITION_LIMIT_BLOCKS + " blocks"
		);

		printProbe("COVE", world, probe, baseline, footprint, density, transition);
		helper.succeed();
	}

	@GameTest(maxTicks = 200)
	public void grassyBaldFreshSeedAcceptance(GameTestHelper helper) {
		WorldContext world = createWorldContext(helper);
		assertInjector(helper, world, "grassy_bald");
		helper.assertTrue(
			world.biomeSource().possibleBiomes().stream().anyMatch(holder -> holder.is(ModBiomes.GRASSY_BALD)),
			"Injected NORMAL Overworld does not expose Grassy Bald"
		);

		SurfaceSample probe = findSurfaceBiome(world, ModBiomes.GRASSY_BALD);
		helper.assertTrue(
			probe != null,
			"Grassy Bald was not located within " + LOCATE_LIMIT_BLOCKS
				+ " blocks for seed " + world.seed()
		);
		Holder<Biome> baseline = sampleSurfaceBiome(world, world.baselineBiomeSource(), probe.x(), probe.z());
		helper.assertTrue(
			baseline.is(GrassyBaldBiomeTags.GRASSY_BALD_TARGETS),
			"Grassy Bald replaced a baseline biome outside its target tag"
		);

		ComponentAudit footprint = auditSurfaceComponent(
			world,
			probe,
			ModBiomes.GRASSY_BALD,
			MIN_BALD_SURFACE_QUARTS
		);
		helper.assertTrue(
			footprint.samples() >= MIN_BALD_SURFACE_QUARTS,
			"Grassy Bald surface component contained only " + footprint.samples()
				+ " connected quart samples; expected at least " + MIN_BALD_SURFACE_QUARTS
		);
		LocalDensity density = auditLocalDensity(world, probe, ModBiomes.GRASSY_BALD);
		TransitionProbe transition = findNearestSampledTransition(
			world,
			probe,
			holder -> !holder.is(ModBiomes.GRASSY_BALD)
		);
		helper.assertTrue(
			transition != null,
			"No sampled exit from Grassy Bald was found within "
				+ TRANSITION_LIMIT_BLOCKS + " blocks"
		);

		printProbe("BALD", world, probe, baseline, footprint, density, transition);
		helper.succeed();
	}

	private static synchronized WorldContext createWorldContext(GameTestHelper helper) {
		RegistryAccess access = helper.getLevel().registryAccess();
		long seed = Long.getLong("before_the_blight.gametest.seed", 0L);
		if (cachedWorld != null && cachedWorld.access() == access && cachedWorld.seed() == seed) {
			return cachedWorld;
		}
		WorldPreset normalPreset = access.lookupOrThrow(Registries.WORLD_PRESET)
			.getValueOrThrow(WorldPresets.NORMAL);
		Registry<LevelStem> emptyStems = new MappedRegistry<LevelStem>(
			Registries.LEVEL_STEM,
			Lifecycle.stable()
		).freeze();
		Registry<LevelStem> stems = normalPreset.createWorldDimensions().bake(emptyStems).dimensions();
		// The GameTest server has already applied global worldgen modifiers and
		// surface rules during startup. Only wrap this newly baked NORMAL preset
		// with the seed-bound biome injectors; replaying the combined startup hook
		// would duplicate add_features modifiers from compatibility mods.
		BiomeInjectorManager.applyBiomeInjectors(access, stems, seed);

		ChunkGenerator overworldGenerator = stems.getValueOrThrow(LevelStem.OVERWORLD).generator();
		helper.assertTrue(
			overworldGenerator instanceof NoiseBasedChunkGenerator,
			"NORMAL Overworld does not use a noise-based chunk generator"
		);
		NoiseBasedChunkGenerator generator = (NoiseBasedChunkGenerator) overworldGenerator;
		BiomeSource biomeSource = generator.getBiomeSource();
		helper.assertTrue(
			biomeSource instanceof InjectorBiomeSource,
			"NORMAL Overworld biome source was not wrapped by Lithostitched"
		);
		assertNoRepeatedFeatures(helper, access, generator, biomeSource);
		generator.validate();
		RandomState randomState = RandomState.create(
			generator.generatorSettings().value(),
			access.lookupOrThrow(Registries.NOISE),
			seed
		);
		cachedWorld = new WorldContext(
			access,
			generator,
			biomeSource,
			((InjectorBiomeSource) biomeSource).directDelegate(),
			randomState,
			LevelHeightAccessor.create(generator.getMinY(), generator.getGenDepth()),
			seed,
			new HashMap<>()
		);
		return cachedWorld;
	}

	@SuppressWarnings("deprecation")
	private static void assertNoRepeatedFeatures(
		GameTestHelper helper,
		RegistryAccess access,
		ChunkGenerator generator,
		BiomeSource biomeSource
	) {
		Registry<PlacedFeature> registry = access.lookupOrThrow(Registries.PLACED_FEATURE);
		for (Holder<Biome> biome : biomeSource.possibleBiomes()) {
			BiomeGenerationSettings settings = generator.getBiomeGenerationSettings(biome);
			for (int step = 0; step < settings.features().size(); step++) {
				Set<PlacedFeature> seen = new HashSet<>();
				HolderSet<PlacedFeature> features = settings.features().get(step);
				for (Holder<PlacedFeature> feature : features) {
					if (!seen.add(feature.value())) {
						helper.fail(
							"BTB_FEATURE_DUPLICATE biome=" + biome.unwrapKey()
								.map(Object::toString).orElse("unkeyed")
								+ " step=" + step
								+ " feature=" + registry.getKey(feature.value())
						);
					}
				}
			}
		}
	}

	private static void assertInjector(GameTestHelper helper, WorldContext world, String path) {
		helper.assertTrue(
			world.access().lookupOrThrow(LithostitchedRegistries.REGION).get(
				ResourceKey.create(
					LithostitchedRegistries.REGION,
					BeforeTheBlight.id("appalachian")
				)
			).isPresent(),
			"Lithostitched Appalachian region did not decode"
		);
		helper.assertTrue(
			world.access().lookupOrThrow(LithostitchedRegistries.BIOME_INJECTOR).get(
				ResourceKey.create(
					LithostitchedRegistries.BIOME_INJECTOR,
					BeforeTheBlight.id(path)
				)
			).isPresent(),
			"Lithostitched injector did not decode: " + path
		);
	}

	private static SurfaceSample findSurfaceBiome(
		WorldContext world,
		ResourceKey<Biome> target
	) {
		SurfaceSample origin = sampleSurface(world, 0, 0);
		if (origin.biome().is(target)) {
			return origin;
		}
		for (int radius = LOCATE_STEP_BLOCKS;
			radius <= LOCATE_LIMIT_BLOCKS;
			radius += LOCATE_STEP_BLOCKS) {
			for (int x = -radius; x <= radius; x += LOCATE_STEP_BLOCKS) {
				SurfaceSample north = findSurfaceCandidate(world, x, -radius, target);
				if (north != null) {
					return north;
				}
				SurfaceSample south = findSurfaceCandidate(world, x, radius, target);
				if (south != null) {
					return south;
				}
			}
			for (int z = -radius + LOCATE_STEP_BLOCKS;
				z < radius;
				z += LOCATE_STEP_BLOCKS) {
				SurfaceSample west = findSurfaceCandidate(world, -radius, z, target);
				if (west != null) {
					return west;
				}
				SurfaceSample east = findSurfaceCandidate(world, radius, z, target);
				if (east != null) {
					return east;
				}
			}
		}
		return null;
	}

	private static SurfaceSample findSurfaceCandidate(
		WorldContext world,
		int x,
		int z,
		ResourceKey<Biome> target
	) {
		int quartX = QuartPos.fromBlock(x);
		int quartZ = QuartPos.fromBlock(z);
		for (int height : LOCATE_PROBE_HEIGHTS) {
			Holder<Biome> probe = world.biomeSource().getNoiseBiome(
				quartX,
				QuartPos.fromBlock(height),
				quartZ,
				world.randomState().sampler()
			);
			if (probe.is(target)) {
				SurfaceSample surface = sampleSurface(world, x, z);
				return surface.biome().is(target) ? surface : null;
			}
		}
		return null;
	}

	private static ComponentAudit auditSurfaceComponent(
		WorldContext world,
		SurfaceSample origin,
		ResourceKey<Biome> target,
		int sampleLimit
	) {
		int originQuartX = QuartPos.fromBlock(origin.x());
		int originQuartZ = QuartPos.fromBlock(origin.z());
		ArrayDeque<QuartCoordinate> pending = new ArrayDeque<>();
		Set<Long> visited = new HashSet<>();
		Map<Long, SurfaceSample> samples = new HashMap<>();
		pending.add(new QuartCoordinate(originQuartX, originQuartZ));

		int count = 0;
		int minQuartX = originQuartX;
		int maxQuartX = originQuartX;
		int minQuartZ = originQuartZ;
		int maxQuartZ = originQuartZ;
		int minimumHeight = Integer.MAX_VALUE;
		int maximumHeight = Integer.MIN_VALUE;
		int boundaryEdges = 0;

		while (!pending.isEmpty() && count < sampleLimit) {
			QuartCoordinate coordinate = pending.removeFirst();
			long packed = pack(coordinate.x(), coordinate.z());
			if (!visited.add(packed)) {
				continue;
			}
			SurfaceSample sample = sampleQuartSurface(world, coordinate.x(), coordinate.z(), samples);
			if (!sample.biome().is(target)) {
				continue;
			}
			count++;
			minQuartX = Math.min(minQuartX, coordinate.x());
			maxQuartX = Math.max(maxQuartX, coordinate.x());
			minQuartZ = Math.min(minQuartZ, coordinate.z());
			maxQuartZ = Math.max(maxQuartZ, coordinate.z());
			minimumHeight = Math.min(minimumHeight, sample.surfaceHeight());
			maximumHeight = Math.max(maximumHeight, sample.surfaceHeight());
			for (int[] direction : CARDINAL_QUARTS) {
				QuartCoordinate next = new QuartCoordinate(
					coordinate.x() + direction[0],
					coordinate.z() + direction[1]
				);
				SurfaceSample neighbor = sampleQuartSurface(world, next.x(), next.z(), samples);
				if (neighbor.biome().is(target)) {
					if (!visited.contains(pack(next.x(), next.z()))) {
						pending.addLast(next);
					}
				}
				else {
					boundaryEdges++;
				}
			}
		}
		long boundingQuartArea = (long) (maxQuartX - minQuartX + 1)
			* (maxQuartZ - minQuartZ + 1);
		int densityPermille = boundingQuartArea == 0L
			? 0
			: (int) Math.round(1_000.0D * count / boundingQuartArea);
		return new ComponentAudit(
			count,
			!pending.isEmpty(),
			(minQuartX * 4),
			((maxQuartX + 1) * 4 - 1),
			(minQuartZ * 4),
			((maxQuartZ + 1) * 4 - 1),
			minimumHeight,
			maximumHeight,
			boundaryEdges,
			densityPermille
		);
	}

	private static LocalDensity auditLocalDensity(
		WorldContext world,
		SurfaceSample origin,
		ResourceKey<Biome> target
	) {
		int total = 0;
		int targetSamples = 0;
		for (int dx = -LOCAL_RADIUS_BLOCKS; dx <= LOCAL_RADIUS_BLOCKS; dx += LOCAL_STEP_BLOCKS) {
			for (int dz = -LOCAL_RADIUS_BLOCKS; dz <= LOCAL_RADIUS_BLOCKS; dz += LOCAL_STEP_BLOCKS) {
				total++;
				if (sampleSurface(world, origin.x() + dx, origin.z() + dz).biome().is(target)) {
					targetSamples++;
				}
			}
		}
		return new LocalDensity(
			total,
			targetSamples,
			(int) Math.round(1_000.0D * targetSamples / total)
		);
	}

	private static TransitionProbe findNearestSampledTransition(
		WorldContext world,
		SurfaceSample origin,
		Predicate<Holder<Biome>> target
	) {
		TransitionProbe nearest = null;
		long nearestSquared = Long.MAX_VALUE;
		for (int radius = TRANSITION_STEP_BLOCKS;
			radius <= TRANSITION_LIMIT_BLOCKS;
			radius += TRANSITION_STEP_BLOCKS) {
			for (int offset = -radius; offset <= radius; offset += TRANSITION_STEP_BLOCKS) {
				nearest = nearerTransition(
					world,
					origin,
					target,
					offset,
					-radius,
					nearest,
					nearestSquared
				);
				if (nearest != null) {
					nearestSquared = (long) nearest.distanceX(origin.x()) * nearest.distanceX(origin.x())
						+ (long) nearest.distanceZ(origin.z()) * nearest.distanceZ(origin.z());
				}
				nearest = nearerTransition(
					world,
					origin,
					target,
					offset,
					radius,
					nearest,
					nearestSquared
				);
				if (nearest != null) {
					nearestSquared = (long) nearest.distanceX(origin.x()) * nearest.distanceX(origin.x())
						+ (long) nearest.distanceZ(origin.z()) * nearest.distanceZ(origin.z());
				}
			}
			for (int offset = -radius + TRANSITION_STEP_BLOCKS;
				offset < radius;
				offset += TRANSITION_STEP_BLOCKS) {
				nearest = nearerTransition(
					world,
					origin,
					target,
					-radius,
					offset,
					nearest,
					nearestSquared
				);
				if (nearest != null) {
					nearestSquared = (long) nearest.distanceX(origin.x()) * nearest.distanceX(origin.x())
						+ (long) nearest.distanceZ(origin.z()) * nearest.distanceZ(origin.z());
				}
				nearest = nearerTransition(
					world,
					origin,
					target,
					radius,
					offset,
					nearest,
					nearestSquared
				);
				if (nearest != null) {
					nearestSquared = (long) nearest.distanceX(origin.x()) * nearest.distanceX(origin.x())
						+ (long) nearest.distanceZ(origin.z()) * nearest.distanceZ(origin.z());
				}
			}
			if (nearest != null && (long) (radius + TRANSITION_STEP_BLOCKS)
				* (radius + TRANSITION_STEP_BLOCKS) > nearestSquared) {
				break;
			}
		}
		return nearest;
	}

	private static TransitionProbe nearerTransition(
		WorldContext world,
		SurfaceSample origin,
		Predicate<Holder<Biome>> target,
		int dx,
		int dz,
		TransitionProbe current,
		long currentSquared
	) {
		long squared = (long) dx * dx + (long) dz * dz;
		long limitSquared = (long) TRANSITION_LIMIT_BLOCKS * TRANSITION_LIMIT_BLOCKS;
		if (squared > limitSquared || squared >= currentSquared) {
			return current;
		}
		SurfaceSample sample = findTransitionCandidate(
			world,
			origin.x() + dx,
			origin.z() + dz,
			origin.surfaceHeight(),
			target
		);
		if (sample == null) {
			return current;
		}
		return new TransitionProbe(
			sample.x(),
			sample.z(),
			sample.surfaceHeight(),
			(int) Math.round(Math.sqrt(squared)),
			biomeIdentifier(sample.biome())
		);
	}

	private static SurfaceSample findTransitionCandidate(
		WorldContext world,
		int x,
		int z,
		int originHeight,
		Predicate<Holder<Biome>> target
	) {
		int quartX = QuartPos.fromBlock(x);
		int quartZ = QuartPos.fromBlock(z);
		for (int offset : new int[] {0, 32, -32, 64, -64}) {
			Holder<Biome> probe = world.biomeSource().getNoiseBiome(
				quartX,
				QuartPos.fromBlock(originHeight + offset),
				quartZ,
				world.randomState().sampler()
			);
			if (target.test(probe)) {
				SurfaceSample surface = sampleSurface(world, x, z);
				return target.test(surface.biome()) ? surface : null;
			}
		}
		return null;
	}

	private static SurfaceSample sampleQuartSurface(
		WorldContext world,
		int quartX,
		int quartZ,
		Map<Long, SurfaceSample> cache
	) {
		long packed = pack(quartX, quartZ);
		SurfaceSample cached = cache.get(packed);
		if (cached != null) {
			return cached;
		}
		SurfaceSample sample = sampleSurface(world, quartX * 4 + 2, quartZ * 4 + 2);
		cache.put(packed, sample);
		return sample;
	}

	private static SurfaceSample sampleSurface(WorldContext world, int x, int z) {
		long key = pack(x, z);
		SurfaceSample cached = world.surfaceCache().get(key);
		if (cached != null) {
			return cached;
		}
		int surfaceHeight = world.generator().getBaseHeight(
			x,
			z,
			Heightmap.Types.WORLD_SURFACE_WG,
			world.heightAccessor(),
			world.randomState()
		);
		Holder<Biome> biome = world.biomeSource().getNoiseBiome(
			QuartPos.fromBlock(x),
			QuartPos.fromBlock(surfaceHeight),
			QuartPos.fromBlock(z),
			world.randomState().sampler()
		);
		SurfaceSample sample = new SurfaceSample(x, z, surfaceHeight, biome);
		world.surfaceCache().put(key, sample);
		return sample;
	}

	private static Holder<Biome> sampleSurfaceBiome(
		WorldContext world,
		BiomeSource biomeSource,
		int x,
		int z
	) {
		int surfaceHeight = world.generator().getBaseHeight(
			x,
			z,
			Heightmap.Types.WORLD_SURFACE_WG,
			world.heightAccessor(),
			world.randomState()
		);
		return biomeSource.getNoiseBiome(
			QuartPos.fromBlock(x),
			QuartPos.fromBlock(surfaceHeight),
			QuartPos.fromBlock(z),
			world.randomState().sampler()
		);
	}

	private static void printProbe(
		String type,
		WorldContext world,
		SurfaceSample probe,
		Holder<Biome> baseline,
		ComponentAudit footprint,
		LocalDensity density,
		TransitionProbe transition
	) {
		System.out.printf(
			"BTB_%s_ACCEPTANCE seed=%d location=%d,%d height=%d baseline=%s "
				+ "locate_step=%d locate_limit=%d footprint_samples=%d footprint_step=4 "
				+ "footprint_capped=%s footprint_bounds=%d..%d,%d..%d "
				+ "footprint_height=%d..%d footprint_relief=%d boundary_edges=%d "
				+ "footprint_density_permille=%d local_samples=%d local_target_samples=%d "
				+ "local_density_permille=%d transition=%s transition_location=%d,%d "
				+ "transition_height=%d transition_distance=%d transition_step=%d "
				+ "transition_limit=%d%n",
			type,
			world.seed(),
			probe.x(),
			probe.z(),
			probe.surfaceHeight(),
			biomeIdentifier(baseline),
			LOCATE_STEP_BLOCKS,
			LOCATE_LIMIT_BLOCKS,
			footprint.samples(),
			footprint.capped(),
			footprint.minimumX(),
			footprint.maximumX(),
			footprint.minimumZ(),
			footprint.maximumZ(),
			footprint.minimumHeight(),
			footprint.maximumHeight(),
			footprint.maximumHeight() - footprint.minimumHeight(),
			footprint.boundaryEdges(),
			footprint.densityPermille(),
			density.totalSamples(),
			density.targetSamples(),
			density.densityPermille(),
			transition.biome(),
			transition.x(),
			transition.z(),
			transition.surfaceHeight(),
			transition.distance(),
			TRANSITION_STEP_BLOCKS,
			TRANSITION_LIMIT_BLOCKS
		);
	}

	private static String biomeIdentifier(Holder<Biome> biome) {
		return biome.unwrapKey().map(ResourceKey::identifier).orElseThrow().toString();
	}

	private static long pack(int x, int z) {
		return ((long) x << 32) ^ (z & 0xFFFF_FFFFL);
	}

	private record WorldContext(
		RegistryAccess access,
		NoiseBasedChunkGenerator generator,
		BiomeSource biomeSource,
		BiomeSource baselineBiomeSource,
		RandomState randomState,
		LevelHeightAccessor heightAccessor,
		long seed,
		Map<Long, SurfaceSample> surfaceCache
	) {
	}

	private record SurfaceSample(int x, int z, int surfaceHeight, Holder<Biome> biome) {
	}

	private record QuartCoordinate(int x, int z) {
	}

	private record ComponentAudit(
		int samples,
		boolean capped,
		int minimumX,
		int maximumX,
		int minimumZ,
		int maximumZ,
		int minimumHeight,
		int maximumHeight,
		int boundaryEdges,
		int densityPermille
	) {
	}

	private record LocalDensity(int totalSamples, int targetSamples, int densityPermille) {
	}

	private record TransitionProbe(
		int x,
		int z,
		int surfaceHeight,
		int distance,
		String biome
	) {
		int distanceX(int originX) {
			return x - originX;
		}

		int distanceZ(int originZ) {
			return z - originZ;
		}
	}
}
