package net.beforetheblight.gametest;

import java.util.HashSet;
import java.util.Set;

import com.mojang.serialization.Lifecycle;
import dev.worldgen.lithostitched.api.registry.LithostitchedRegistries;
import dev.worldgen.lithostitched.impl.worldgen.biomeinjector.internal.BiomeInjectorManager;
import dev.worldgen.lithostitched.impl.worldgen.biomeinjector.internal.InjectorBiomeSource;
import net.beforetheblight.BeforeTheBlight;
import net.beforetheblight.worldgen.biome.CoveBiomeTags;
import net.beforetheblight.worldgen.biome.CoveMinimumFootprintFilter;
import net.beforetheblight.worldgen.biome.ModBiomes;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
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

public final class CoveWorldgenGameTests {
	private static final int SCAN_LIMIT = 16_384;
	private static final int SCAN_STEP = 128;

	@GameTest(maxTicks = 20)
	public void coveMinimumFootprintClassifierIsDeterministic(GameTestHelper helper) {
		int minimum = CoveMinimumFootprintFilter.MIN_COMPONENT_QUARTS;
		helper.assertValueEqual(minimum, 144, "Cove minimum component size in biome quarts");

		assertSyntheticComponent(helper, lineComponent(0, 0, 0, 1), false, "single quart");
		Set<CoveMinimumFootprintFilter.QuartCoordinate> diagonalPair = Set.of(
			new CoveMinimumFootprintFilter.QuartCoordinate(0, 0, 0),
			new CoveMinimumFootprintFilter.QuartCoordinate(1, 0, 1)
		);
		assertSyntheticComponent(helper, diagonalPair, false, "diagonal pair");
		assertSyntheticComponent(
			helper,
			lineComponent(0, 3, 0, minimum - 1),
			false,
			"143-quart component"
		);
		Set<CoveMinimumFootprintFilter.QuartCoordinate> qualifying =
			lineComponent(-300, 3, -500, minimum);
		assertSyntheticComponent(helper, qualifying, true, "144-quart component");

		CoveMinimumFootprintFilter.Classifier tinyCache =
			new CoveMinimumFootprintFilter.Classifier(minimum, 4);
		for (int offset : new int[] {0, minimum - 1, 63, 1, 96}) {
			helper.assertTrue(
				classifySynthetic(tinyCache, qualifying, -300 + offset, 3, -500),
				"qualifying Cove component changed under query order or cache eviction"
			);
			helper.assertTrue(
				tinyCache.rawCacheSize() <= 4 && tinyCache.statusCacheSize() <= 4,
				"Cove footprint caches exceeded their configured bound"
			);
		}
		helper.succeed();
	}

	@GameTest(maxTicks = 200)
	public void hemlockBeechCoveInjectedAndLocateable(GameTestHelper helper) {
		RegistryAccess access = helper.getLevel().registryAccess();
		Holder.Reference<Biome> cove = access.lookupOrThrow(Registries.BIOME)
			.getOrThrow(ModBiomes.HEMLOCK_BEECH_COVE);
		helper.assertTrue(
			access.lookupOrThrow(Registries.BIOME)
				.getTagOrEmpty(CoveBiomeTags.HEMLOCK_BEECH_COVE_TARGETS)
				.iterator()
				.hasNext(),
			"Hemlock Cove target tag did not decode or is empty"
		);
		helper.assertTrue(
			access.lookupOrThrow(LithostitchedRegistries.REGION).get(
				ResourceKey.create(LithostitchedRegistries.REGION, BeforeTheBlight.id("appalachian"))
			).isPresent(),
			"Lithostitched Appalachian region did not decode"
		);
		helper.assertTrue(
			access.lookupOrThrow(LithostitchedRegistries.BIOME_INJECTOR).get(
				ResourceKey.create(
					LithostitchedRegistries.BIOME_INJECTOR,
					BeforeTheBlight.id("hemlock_beech_cove")
				)
			).isPresent(),
			"Lithostitched Hemlock Cove injector did not decode"
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

		ChunkGenerator overworldGenerator = stems.getValueOrThrow(LevelStem.OVERWORLD).generator();
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
		BiomeSource baseline = ((InjectorBiomeSource) biomeSource).directDelegate();
		helper.assertTrue(
			biomeSource.possibleBiomes().contains(cove),
			"Injected NORMAL Overworld biome source does not expose Hemlock Cove"
		);
		generator.validate();

		RandomState randomState = RandomState.create(
			generator.generatorSettings().value(),
			access.lookupOrThrow(Registries.NOISE),
			seed
		);
		LevelHeightAccessor heightAccessor = LevelHeightAccessor.create(
			generator.getMinY(),
			generator.getGenDepth()
		);
		CoveProbe probe = findCove(generator, biomeSource, randomState, heightAccessor);
		helper.assertTrue(
			probe != null,
			"Hemlock Cove could not be located on the NORMAL Overworld surface within "
				+ SCAN_LIMIT + " blocks for seed " + seed
		);

		Holder<Biome> baselineAtProbe = sampleSurfaceBiome(
			generator,
			baseline,
			randomState,
			heightAccessor,
			probe.x(),
			probe.z()
		);
		helper.assertTrue(
			baselineAtProbe.is(CoveBiomeTags.HEMLOCK_BEECH_COVE_TARGETS),
			"Cove replaced a baseline biome outside its explicit forest target mask"
		);
		System.out.printf(
			"BTB_COVE_WORLDGEN_PROBE seed=%d cove=%d,%d height=%d baseline=%s%n",
			seed,
			probe.x(),
			probe.z(),
			probe.surfaceHeight(),
			baselineAtProbe.unwrapKey().map(ResourceKey::identifier).orElse(null)
		);
		helper.succeed();
	}

	private static CoveProbe findCove(
		NoiseBasedChunkGenerator generator,
		BiomeSource biomeSource,
		RandomState randomState,
		LevelHeightAccessor heightAccessor
	) {
		CoveProbe origin = trySurfaceCove(generator, biomeSource, randomState, heightAccessor, 0, 0);
		if (origin != null) {
			return origin;
		}
		for (int radius = SCAN_STEP; radius <= SCAN_LIMIT; radius += SCAN_STEP) {
			for (int x = -radius; x <= radius; x += SCAN_STEP) {
				CoveProbe north = trySurfaceCove(generator, biomeSource, randomState, heightAccessor, x, -radius);
				if (north != null) {
					return north;
				}
				CoveProbe south = trySurfaceCove(generator, biomeSource, randomState, heightAccessor, x, radius);
				if (south != null) {
					return south;
				}
			}
			for (int z = -radius + SCAN_STEP; z < radius; z += SCAN_STEP) {
				CoveProbe west = trySurfaceCove(generator, biomeSource, randomState, heightAccessor, -radius, z);
				if (west != null) {
					return west;
				}
				CoveProbe east = trySurfaceCove(generator, biomeSource, randomState, heightAccessor, radius, z);
				if (east != null) {
					return east;
				}
			}
		}
		return null;
	}

	private static CoveProbe trySurfaceCove(
		NoiseBasedChunkGenerator generator,
		BiomeSource biomeSource,
		RandomState randomState,
		LevelHeightAccessor heightAccessor,
		int x,
		int z
	) {
		int surfaceHeight = getSurfaceHeight(generator, randomState, heightAccessor, x, z);
		return sampleBiome(biomeSource, randomState, x, surfaceHeight, z)
			.is(ModBiomes.HEMLOCK_BEECH_COVE)
				? new CoveProbe(x, z, surfaceHeight)
				: null;
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

	private static Set<CoveMinimumFootprintFilter.QuartCoordinate> lineComponent(
		int startX,
		int y,
		int z,
		int length
	) {
		Set<CoveMinimumFootprintFilter.QuartCoordinate> positions = new HashSet<>();
		for (int offset = 0; offset < length; offset++) {
			positions.add(new CoveMinimumFootprintFilter.QuartCoordinate(startX + offset, y, z));
		}
		return positions;
	}

	private static boolean classifySynthetic(
		CoveMinimumFootprintFilter.Classifier classifier,
		Set<CoveMinimumFootprintFilter.QuartCoordinate> component,
		int x,
		int y,
		int z
	) {
		return classifier.shouldKeep(
			x,
			y,
			z,
			(sampleX, sampleY, sampleZ) -> component.contains(
				new CoveMinimumFootprintFilter.QuartCoordinate(sampleX, sampleY, sampleZ)
			)
		);
	}

	private static void assertSyntheticComponent(
		GameTestHelper helper,
		Set<CoveMinimumFootprintFilter.QuartCoordinate> component,
		boolean expectedKeep,
		String description
	) {
		CoveMinimumFootprintFilter.Classifier classifier =
			new CoveMinimumFootprintFilter.Classifier();
		for (CoveMinimumFootprintFilter.QuartCoordinate position : component) {
			helper.assertValueEqual(
				classifySynthetic(classifier, component, position.x(), position.y(), position.z()),
				expectedKeep,
				description + " at " + position
			);
		}
	}

	private record CoveProbe(int x, int z, int surfaceHeight) {
	}
}
