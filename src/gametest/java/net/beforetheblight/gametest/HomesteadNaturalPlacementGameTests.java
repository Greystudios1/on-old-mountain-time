package net.beforetheblight.gametest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.mojang.datafixers.util.Pair;
import net.beforetheblight.registry.ModBlocks;
import net.beforetheblight.worldgen.biome.RawInjectorBiomeSourceAccess;
import net.beforetheblight.worldgen.structure.AppalachianCornCribPiece;
import net.beforetheblight.worldgen.structure.AppalachianCornCribStructure;
import net.beforetheblight.worldgen.structure.AppalachianHomesteadPiece;
import net.beforetheblight.worldgen.structure.AppalachianHomesteadStructure;
import net.beforetheblight.worldgen.structure.ModStructures;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.QuartPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

/**
 * Natural-placement evidence using the opt-in NORMAL GameTest world. The
 * property-gated GameTest-only mixin enables structures before world creation,
 * so one live FULL-chunk start, its decorated blocks, and radius-zero locator
 * behavior exercise the real server chunk pipeline.
 */
public final class HomesteadNaturalPlacementGameTests {
	private static final String NATURAL_WORLD_PROPERTY =
		"before_the_blight.gametest.natural_structure_world";
	private static final String NATURAL_PROFILE_PROPERTY =
		"before_the_blight.gametest.natural_structure_profile";
	private static final int SCAN_BATCH_SIZE = 256;
	private static final int MAX_REGION_RADIUS = 240;
	private static final long COOPERATIVE_DEADLINE_NANOS =
		TimeUnit.SECONDS.toNanos(180);
	private static final int[] TARGET_BIOME_PREFLIGHT_HEIGHTS = {
		64, 80, 96, 112, 128, 144, 160, 176,
		192, 208, 224, 240, 256, 272, 288, 304
	};

	@GameTest(maxTicks = 3_000)
	public void proveOneNaturalHomesteadWitness(GameTestHelper helper) {
		verifyNaturalStructure(helper, NaturalStructureKind.HOMESTEAD);
	}

	@GameTest(maxTicks = 3_000)
	public void proveOneNaturalCornCribWitness(GameTestHelper helper) {
		verifyNaturalStructure(helper, NaturalStructureKind.CORN_CRIB);
	}

	private static void verifyNaturalStructure(
		GameTestHelper helper,
		NaturalStructureKind kind
	) {
		if (!Boolean.getBoolean(NATURAL_WORLD_PROPERTY)) {
			// The normal GameTest suite remains fast and flat. Dedicated base/Tectonic
			// structure qualification opts into the NORMAL-world mixin explicitly.
			helper.succeed();
			return;
		}
		String profile = activeProfile(helper);
		ServerLevel level = helper.getLevel();
		long seed = level.getSeed();
		helper.assertValueEqual(seed, 0L, "natural structure qualification seed");
		helper.assertTrue(!level.isFlat(), "natural structure qualification world is still flat");
		helper.assertTrue(
			level.structureManager().shouldGenerateStructures(),
			"natural structure qualification world has structure generation disabled"
		);
		GenerationFixture fixture = new GenerationFixture(
			level.getChunkSource().getGenerator(),
			level.getChunkSource().randomState(),
			level,
			level.getChunkSource().getGeneratorState()
		);
		Holder.Reference<Structure> target = level.registryAccess()
			.lookupOrThrow(Registries.STRUCTURE)
			.getOrThrow(kind.structureKey());
		StructureSet structureSet = level.registryAccess()
			.lookupOrThrow(Registries.STRUCTURE_SET)
			.getValueOrThrow(kind.structureSetKey());
		helper.assertTrue(
			structureSet.placement() instanceof RandomSpreadStructurePlacement,
			kind.label() + " structure set no longer uses random-spread placement"
		);
		RandomSpreadStructurePlacement placement =
			(RandomSpreadStructurePlacement) structureSet.placement();
		long startedNanos = System.nanoTime();
		long deadlineNanos = startedNanos + COOPERATIVE_DEADLINE_NANOS;

		ScanCounters counters = new ScanCounters();
		NaturalStartEvidence witness = findDetachedWitness(
			helper,
			level,
			fixture,
			target,
			kind,
			placement,
			counters,
			deadlineNanos
		);
		helper.assertTrue(
			witness != null,
			"Expected one detached " + kind.label()
				+ " preflight witness before the 180-second in-test limit; found none after "
				+ counters.inspectedRegions + " placement regions"
		);
		if (witness == null) {
			return;
		}

		double preflightSeconds = (System.nanoTime() - startedNanos) / 1_000_000_000.0D;
		System.out.printf(
			"BTB_NATURAL_STRUCTURE_PRECHECK structure=%s profile=%s seed=%d "
				+ "batch_size=%d inspected_regions=%d prefiltered_candidates=%d "
				+ "target_biome=%d "
				+ "terrain_rejected=%d buildable_target=%d detached_valid=true "
				+ "chunk=%d,%d relief=%d foundation=%d biome=%s elapsed_seconds=%.3f%n",
			kind.id(),
			profile,
			seed,
			SCAN_BATCH_SIZE,
			counters.inspectedRegions,
			counters.prefilteredCandidates,
			counters.targetBiome,
			counters.terrainRejected,
			counters.buildableTarget,
			witness.probe().chunk().x(),
			witness.probe().chunk().z(),
			witness.probe().profile().relief(),
			witness.probe().profile().foundationY(),
			witness.probe().biome().unwrapKey()
				.map(key -> key.identifier().toString())
				.orElse("unregistered"),
			preflightSeconds
		);

		LoadedChunkSet loadedChunks = null;
		try {
			loadedChunks = loadFullChunks(level, witness.start().getBoundingBox());
		}
		catch (RuntimeException failure) {
			helper.fail(
				kind.label() + " FULL chunk generation failed: "
					+ failure.getClass().getSimpleName() + ": "
					+ String.valueOf(failure.getMessage())
			);
			return;
		}
		assertWithinDeadline(
			helper,
			kind,
			counters.inspectedRegions,
			deadlineNanos
		);
		completeLiveWitness(
			helper,
			level,
			fixture,
			target,
			kind,
			profile,
			witness,
			loadedChunks,
			deadlineNanos
		);
	}

	private static NaturalStartEvidence findDetachedWitness(
		GameTestHelper helper,
		ServerLevel level,
		GenerationFixture fixture,
		Holder.Reference<Structure> target,
		NaturalStructureKind kind,
		RandomSpreadStructurePlacement placement,
		ScanCounters counters,
		long deadlineNanos
	) {
		List<ChunkPos> batch = new ArrayList<>(SCAN_BATCH_SIZE);
		Set<Long> uniqueCandidates = new HashSet<>();
		long seed = level.getSeed();
		for (int radius = 0; radius <= MAX_REGION_RADIUS; radius++) {
			for (int regionX = -radius; regionX <= radius; regionX++) {
				for (int regionZ = -radius; regionZ <= radius; regionZ++) {
					if (Math.max(Math.abs(regionX), Math.abs(regionZ)) != radius) {
						continue;
					}
					ChunkPos candidate = placement.getPotentialStructureChunk(
						seed,
						regionX * placement.spacing(),
						regionZ * placement.spacing()
					);
					helper.assertTrue(
						uniqueCandidates.add(candidate.pack()),
						kind.label() + " random-spread grid repeated candidate " + candidate
					);
					batch.add(candidate);
					if (batch.size() == SCAN_BATCH_SIZE) {
						NaturalStartEvidence witness = inspectBatchForDetachedWitness(
							helper,
							level,
							fixture,
							target,
							kind,
							batch,
							counters,
							deadlineNanos
						);
						batch.clear();
						if (witness != null) {
							return witness;
						}
					}
				}
			}
		}
		if (!batch.isEmpty()) {
			return inspectBatchForDetachedWitness(
				helper,
				level,
				fixture,
				target,
				kind,
				batch,
				counters,
				deadlineNanos
			);
		}
		return null;
	}

	private static NaturalStartEvidence inspectBatchForDetachedWitness(
		GameTestHelper helper,
		ServerLevel level,
		GenerationFixture fixture,
		Holder.Reference<Structure> target,
		NaturalStructureKind kind,
		List<ChunkPos> candidates,
		ScanCounters counters,
		long deadlineNanos
	) {
		helper.assertTrue(
			candidates.size() <= SCAN_BATCH_SIZE,
			kind.label() + " preflight batch exceeded " + SCAN_BATCH_SIZE
		);
		assertWithinDeadline(helper, kind, counters.inspectedRegions, deadlineNanos);
		counters.inspectedRegions += candidates.size();
		List<ChunkPos> prefilteredCandidates = candidates.parallelStream()
			.filter(candidate -> matchesTargetBiomeAtProbeHeight(
				fixture,
				target.value(),
				candidate.getMiddleBlockX(),
				candidate.getMiddleBlockZ()
			))
			.toList();
		counters.prefilteredCandidates += prefilteredCandidates.size();
		printScanProgress(level, kind, counters, deadlineNanos);

		for (int candidateIndex = 0; candidateIndex < prefilteredCandidates.size(); candidateIndex++) {
			ChunkPos candidate = prefilteredCandidates.get(candidateIndex);
			assertWithinDeadline(
				helper,
				kind,
				counters.inspectedRegions,
				deadlineNanos
			);
			PlacementProbe probe = inspectTargetSite(
				fixture,
				target.value(),
				kind,
				candidate
			);
			if ((candidateIndex + 1) % 32 == 0) {
				printScanProgress(level, kind, counters, deadlineNanos);
			}
			if (probe == null) {
				continue;
			}
			counters.targetBiome++;
			if (!probe.profile().isBuildable()) {
				counters.terrainRejected++;
				continue;
			}
			counters.buildableTarget++;
			assertWithinDeadline(
				helper,
				kind,
				counters.inspectedRegions,
				deadlineNanos
			);
			StructureStart start = createDetachedNaturalStart(
				helper,
				level.registryAccess(),
				fixture,
				target,
				probe.chunk()
			);
			if (start == null || !start.isValid()) {
				continue;
			}
			return new NaturalStartEvidence(probe, start);
		}
		return null;
	}

	private static void printScanProgress(
		ServerLevel level,
		NaturalStructureKind kind,
		ScanCounters counters,
		long deadlineNanos
	) {
		double elapsedSeconds =
			(System.nanoTime() - (deadlineNanos - COOPERATIVE_DEADLINE_NANOS))
				/ 1_000_000_000.0D;
		System.out.printf(
			"BTB_NATURAL_STRUCTURE_PROGRESS structure=%s profile=%s seed=%d "
				+ "inspected_regions=%d prefiltered_candidates=%d "
				+ "target_surface_sites=%d terrain_rejected=%d "
				+ "buildable_targets=%d elapsed_seconds=%.3f%n",
			kind.id(),
			System.getProperty(NATURAL_PROFILE_PROPERTY, "unknown"),
			level.getSeed(),
			counters.inspectedRegions,
			counters.prefilteredCandidates,
			counters.targetBiome,
			counters.terrainRejected,
			counters.buildableTarget,
			elapsedSeconds
		);
	}

	private static StructureStart createDetachedNaturalStart(
		GameTestHelper helper,
		RegistryAccess access,
		GenerationFixture fixture,
		Holder.Reference<Structure> target,
		ChunkPos candidate
	) {
		ProtoChunk chunk = new ProtoChunk(
			candidate,
			UpgradeData.EMPTY,
			fixture.heightAccessor(),
			helper.getLevel().palettedContainerFactory(),
			null
		);
		fixture.generator().createStructures(
			access,
			fixture.structureState(),
			helper.getLevel().structureManager(),
			chunk,
			helper.getLevel().getStructureManager(),
			Level.OVERWORLD
		);
		return chunk.getStartForStructure(target.value());
	}

	/**
	 * ServerChunkCache#getChunk uses its main-thread managed blocker to pump the
	 * real generation tasks while this focused GameTest waits. Keeping the load
	 * synchronous prevents the accelerated GameTest clock from consuming its
	 * entire logical-tick budget before a remote async future completes.
	 */
	private static LoadedChunkSet loadFullChunks(
		ServerLevel level,
		BoundingBox bounds
	) {
		int minChunkX = SectionPos.blockToSectionCoord(bounds.minX());
		int maxChunkX = SectionPos.blockToSectionCoord(bounds.maxX());
		int minChunkZ = SectionPos.blockToSectionCoord(bounds.minZ());
		int maxChunkZ = SectionPos.blockToSectionCoord(bounds.maxZ());

		List<LoadedChunk> loaded = new ArrayList<>();
		for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
			for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
				ChunkPos position = new ChunkPos(chunkX, chunkZ);
				ChunkAccess chunk = level.getChunk(
					position.x(),
					position.z(),
					ChunkStatus.FULL,
					true
				);
				loaded.add(new LoadedChunk(position, chunk));
			}
		}
		return new LoadedChunkSet(
			List.copyOf(loaded),
			minChunkX,
			maxChunkX,
			minChunkZ,
			maxChunkZ
		);
	}

	private static void completeLiveWitness(
		GameTestHelper helper,
		ServerLevel level,
		GenerationFixture fixture,
		Holder.Reference<Structure> target,
		NaturalStructureKind kind,
		String profile,
		NaturalStartEvidence detachedWitness,
		LoadedChunkSet loadedChunks,
		long deadlineNanos
	) {
		StructureStart detachedStart = detachedWitness.start();
		ChunkPos witnessChunk = detachedStart.getChunkPos();
		int expectedChunkCount =
			(loadedChunks.maxChunkX() - loadedChunks.minChunkX() + 1)
				* (loadedChunks.maxChunkZ() - loadedChunks.minChunkZ() + 1);
		helper.assertValueEqual(
			loadedChunks.chunks().size(),
			expectedChunkCount,
			"FULL chunks intersecting detached start bounds"
		);
		ChunkAccess startChunk = loadedChunks.get(witnessChunk);
		StructureStart liveStart = level.structureManager().getStartForStructure(
			SectionPos.bottomOf(startChunk),
			target.value(),
			startChunk
		);
		helper.assertTrue(
			liveStart != null && liveStart.isValid(),
			kind.label() + " detached preflight did not become a live FULL-chunk StructureStart"
		);
		helper.assertValueEqual(
			liveStart.getChunkPos(),
			witnessChunk,
			kind.label() + " live FULL-chunk start chunk"
		);
		assertSameBounds(
			helper,
			detachedStart.getBoundingBox(),
			liveStart.getBoundingBox(),
			kind.label() + " detached/live bounds"
		);
		System.out.printf(
			"BTB_NATURAL_STRUCTURE_LIVE_START structure=%s profile=%s seed=%d "
				+ "chunk=%d,%d full_chunks=%d live_chunk_start=true%n",
			kind.id(),
			profile,
			level.getSeed(),
			witnessChunk.x(),
			witnessChunk.z(),
			loadedChunks.chunks().size()
		);
		assertWithinDeadline(helper, kind, 0, deadlineNanos);

		assertDecoratedPhysicalStructure(helper, level, kind, liveStart);
		System.out.printf(
			"BTB_NATURAL_STRUCTURE_DECORATED structure=%s profile=%s seed=%d "
				+ "start_chunk=%d,%d physical_signature=true%n",
			kind.id(),
			profile,
			level.getSeed(),
			witnessChunk.x(),
			witnessChunk.z()
		);
		assertWithinDeadline(helper, kind, 0, deadlineNanos);

		BlockPos searchOrigin = new BlockPos(
			witnessChunk.getMiddleBlockX(),
			0,
			witnessChunk.getMiddleBlockZ()
		);
		Pair<BlockPos, Holder<Structure>> located =
			fixture.generator().findNearestMapStructure(
				level,
				HolderSet.direct(target),
				searchOrigin,
				0,
				false
			);
		helper.assertTrue(located != null, "/locate could not find natural " + kind.label());
		if (located == null) {
			return;
		}
		helper.assertTrue(
			located.getSecond().is(kind.structureKey()),
			"/locate returned the wrong structure for " + kind.label()
		);
		ChunkPos locatedChunk = ChunkPos.containing(located.getFirst());
		helper.assertValueEqual(
			locatedChunk,
			witnessChunk,
			kind.label() + " radius-zero locator chunk"
		);
		ChunkAccess locatedChunkAccess = loadedChunks.get(locatedChunk);
		StructureStart locatedStart = level.structureManager().getStartForStructure(
			SectionPos.bottomOf(locatedChunkAccess),
			target.value(),
			locatedChunkAccess
		);
		helper.assertTrue(
			locatedStart != null && locatedStart.isValid(),
			"/locate witness did not resolve to a valid " + kind.label() + " StructureStart"
		);
		helper.assertValueEqual(
			locatedStart.getChunkPos(),
			liveStart.getChunkPos(),
			kind.label() + " locator/live start chunk"
		);
		System.out.printf(
			"BTB_NATURAL_STRUCTURE_LOCATOR structure=%s profile=%s seed=%d "
				+ "radius=0 search_origin=%d,%d block=%d,%d chunk=%d,%d exact=true%n",
			kind.id(),
			profile,
			level.getSeed(),
			searchOrigin.getX(),
			searchOrigin.getZ(),
			located.getFirst().getX(),
			located.getFirst().getZ(),
			locatedChunk.x(),
			locatedChunk.z()
		);
		assertWithinDeadline(helper, kind, 0, deadlineNanos);
		System.out.printf(
			"BTB_NATURAL_STRUCTURE_PASS structure=%s profile=%s seed=%d chunk=%d,%d%n",
			kind.id(),
			profile,
			level.getSeed(),
			witnessChunk.x(),
			witnessChunk.z()
		);
		helper.succeed();
	}

	private static void assertDecoratedPhysicalStructure(
		GameTestHelper helper,
		ServerLevel level,
		NaturalStructureKind kind,
		StructureStart start
	) {
		helper.assertTrue(
			!start.getPieces().isEmpty(),
			kind.label() + " natural StructureStart contains no pieces"
		);
		StructurePiece firstPiece = start.getPieces().getFirst();
		BlockPos templateOrigin = kind.templateOrigin(helper, firstPiece);
		kind.assertPhysicalSignature(helper, level, templateOrigin);
	}

	private static PlacementProbe inspectTargetSite(
		GenerationFixture fixture,
		Structure structure,
		NaturalStructureKind kind,
		ChunkPos chunk
	) {
		int x = chunk.getMiddleBlockX();
		int z = chunk.getMiddleBlockZ();
		int y = surfaceHeight(fixture, x, z);
		Holder<Biome> biome = fixture.generator().getBiomeSource().getNoiseBiome(
			QuartPos.fromBlock(x),
			QuartPos.fromBlock(y),
			QuartPos.fromBlock(z),
			fixture.randomState().sampler()
		);
		if (!structure.biomes().contains(biome)) {
			return null;
		}
		SiteProfile profile = kind.sampleSite(fixture, chunk);
		if (!profile.isBuildable()) {
			return new PlacementProbe(chunk, profile, biome);
		}
		Holder<Biome> exactBiome = fixture.generator().getBiomeSource().getNoiseBiome(
			QuartPos.fromBlock(x),
			QuartPos.fromBlock(profile.foundationY() + 1),
			QuartPos.fromBlock(z),
			fixture.randomState().sampler()
		);
		return structure.biomes().contains(exactBiome)
			? new PlacementProbe(chunk, profile, exactBiome)
			: null;
	}

	/**
	 * Tectonic surface-height sampling is intentionally deferred until a cheap,
	 * non-exhaustive raw-biome heuristic says the candidate may belong to this
	 * structure. Every survivor is still checked through the ordinary filtered
	 * biome source at its generated surface and foundation before it can become
	 * positive witness evidence.
	 */
	private static boolean matchesTargetBiomeAtProbeHeight(
		GenerationFixture fixture,
		Structure structure,
		int x,
		int z
	) {
		var biomeSource = fixture.generator().getBiomeSource();
		RawInjectorBiomeSourceAccess rawSource =
			biomeSource instanceof RawInjectorBiomeSourceAccess access
				? access
				: null;
		int quartX = QuartPos.fromBlock(x);
		int quartZ = QuartPos.fromBlock(z);
		for (int height : TARGET_BIOME_PREFLIGHT_HEIGHTS) {
			Holder<Biome> biome = rawSource != null
				? rawSource.beforeTheBlight$getRawNoiseBiome(
					quartX,
					QuartPos.fromBlock(height),
					quartZ,
					fixture.randomState().sampler()
				)
				: biomeSource.getNoiseBiome(
					quartX,
					QuartPos.fromBlock(height),
					quartZ,
					fixture.randomState().sampler()
				);
			if (structure.biomes().contains(biome)) {
				return true;
			}
		}
		return false;
	}

	private static int surfaceHeight(GenerationFixture fixture, int x, int z) {
		return fixture.generator().getFirstOccupiedHeight(
			x,
			z,
			Heightmap.Types.WORLD_SURFACE_WG,
			fixture.heightAccessor(),
			fixture.randomState()
		);
	}

	private static int oceanFloorHeight(GenerationFixture fixture, int x, int z) {
		return fixture.generator().getFirstOccupiedHeight(
			x,
			z,
			Heightmap.Types.OCEAN_FLOOR_WG,
			fixture.heightAccessor(),
			fixture.randomState()
		);
	}

	/**
	 * Rejection is monotonic: one wet column or relief already above the limit
	 * cannot become buildable after more columns are sampled. Returning early
	 * avoids expensive Tectonic density queries only for rejected sites. Every
	 * accepted site still samples the complete footprint, and production
	 * createStructures performs its own complete terrain screen afterward.
	 */
	private static SiteProfile sampleSiteWithEarlyRejection(
		GenerationFixture fixture,
		int originX,
		int originZ,
		int width,
		int depth,
		int maximumRelief
	) {
		int minimum = Integer.MAX_VALUE;
		int maximum = Integer.MIN_VALUE;
		int samples = 0;
		int dryColumns = 0;
		int expectedColumns = width * depth;
		for (int offsetX = 0; offsetX < width; offsetX++) {
			for (int offsetZ = 0; offsetZ < depth; offsetZ++) {
				int x = originX + offsetX;
				int z = originZ + offsetZ;
				int height = surfaceHeight(fixture, x, z);
				minimum = Math.min(minimum, height);
				maximum = Math.max(maximum, height);
				samples++;
				if (maximum - minimum > maximumRelief) {
					return new SiteProfile(
						minimum,
						maximum,
						samples,
						dryColumns,
						expectedColumns,
						maximumRelief
					);
				}
				if (height != oceanFloorHeight(fixture, x, z)) {
					return new SiteProfile(
						minimum,
						maximum,
						samples,
						dryColumns,
						expectedColumns,
						maximumRelief
					);
				}
				dryColumns++;
			}
		}
		return new SiteProfile(
			minimum,
			maximum,
			samples,
			dryColumns,
			expectedColumns,
			maximumRelief
		);
	}

	private static void assertWithinDeadline(
		GameTestHelper helper,
		NaturalStructureKind kind,
		int inspectedRegions,
		long deadlineNanos
	) {
		helper.assertTrue(
			System.nanoTime() <= deadlineNanos,
			kind.label() + " natural witness gate exceeded its 180-second cooperative "
				+ "deadline after "
				+ inspectedRegions + " placement regions"
		);
	}

	private static void assertSameBounds(
		GameTestHelper helper,
		BoundingBox expected,
		BoundingBox actual,
		String label
	) {
		helper.assertValueEqual(actual.minX(), expected.minX(), label + " min X");
		helper.assertValueEqual(actual.minY(), expected.minY(), label + " min Y");
		helper.assertValueEqual(actual.minZ(), expected.minZ(), label + " min Z");
		helper.assertValueEqual(actual.maxX(), expected.maxX(), label + " max X");
		helper.assertValueEqual(actual.maxY(), expected.maxY(), label + " max Y");
		helper.assertValueEqual(actual.maxZ(), expected.maxZ(), label + " max Z");
	}

	private static String activeProfile(GameTestHelper helper) {
		String profile = System.getProperty(NATURAL_PROFILE_PROPERTY, "").trim();
		helper.assertTrue(
			profile.equals("base") || profile.equals("tectonic"),
			NATURAL_PROFILE_PROPERTY + " must be exactly base or tectonic"
		);
		boolean tectonicLoaded = FabricLoader.getInstance().isModLoaded("tectonic");
		helper.assertValueEqual(
			tectonicLoaded,
			profile.equals("tectonic"),
			"Tectonic Loader state for natural structure profile " + profile
		);
		return profile;
	}

	private static int findBlockY(
		ServerLevel level,
		int x,
		int z,
		java.util.function.Predicate<BlockState> predicate
	) {
		for (int y = level.getMinY(); y <= level.getMaxY(); y++) {
			if (predicate.test(level.getBlockState(new BlockPos(x, y, z)))) {
				return y;
			}
		}
		return Integer.MIN_VALUE;
	}

	private record LoadedChunk(ChunkPos position, ChunkAccess chunk) {
	}

	private record LoadedChunkSet(
		List<LoadedChunk> chunks,
		int minChunkX,
		int maxChunkX,
		int minChunkZ,
		int maxChunkZ
	) {
		private ChunkAccess get(ChunkPos position) {
			return chunks.stream()
				.filter(candidate -> candidate.position().equals(position))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException(
					"FULL result set omitted required chunk " + position
				))
				.chunk();
		}
	}

	private record GenerationFixture(
		ChunkGenerator generator,
		RandomState randomState,
		LevelHeightAccessor heightAccessor,
		ChunkGeneratorStructureState structureState
	) {
	}

	private record SiteProfile(
		int minimumSurfaceY,
		int maximumSurfaceY,
		int sampledColumns,
		int dryColumns,
		int expectedColumns,
		int maximumRelief
	) {
		private int relief() {
			return maximumSurfaceY - minimumSurfaceY;
		}

		private int foundationY() {
			return maximumSurfaceY - 1;
		}

		private boolean isBuildable() {
			return sampledColumns == expectedColumns
				&& dryColumns == expectedColumns
				&& relief() <= maximumRelief;
		}
	}

	private record PlacementProbe(
		ChunkPos chunk,
		SiteProfile profile,
		Holder<Biome> biome
	) {
	}

	private record NaturalStartEvidence(
		PlacementProbe probe,
		StructureStart start
	) {
	}

	private static final class ScanCounters {
		private int inspectedRegions;
		private int prefilteredCandidates;
		private int targetBiome;
		private int terrainRejected;
		private int buildableTarget;
	}

	private enum NaturalStructureKind {
		HOMESTEAD(
			"appalachian_homestead",
			"Appalachian Homestead",
			ModStructures.APPALACHIAN_HOMESTEAD,
			ModStructures.APPALACHIAN_HOMESTEADS
		) {
			@Override
			SiteProfile sampleSite(GenerationFixture fixture, ChunkPos chunk) {
				int originX =
					chunk.getMiddleBlockX() - AppalachianHomesteadStructure.TEMPLATE_WIDTH / 2;
				int originZ =
					chunk.getMiddleBlockZ() - AppalachianHomesteadStructure.TEMPLATE_DEPTH / 2;
				return sampleSiteWithEarlyRejection(
					fixture,
					originX,
					originZ,
					AppalachianHomesteadStructure.TEMPLATE_WIDTH,
					AppalachianHomesteadStructure.SITE_DEPTH,
					AppalachianHomesteadStructure.MAX_SITE_RELIEF
				);
			}

			@Override
			BlockPos templateOrigin(GameTestHelper helper, StructurePiece piece) {
				helper.assertTrue(
					piece instanceof AppalachianHomesteadPiece,
					"Natural Homestead start did not contain an AppalachianHomesteadPiece"
				);
				return ((AppalachianHomesteadPiece) piece).templatePosition();
			}

			@Override
			void assertPhysicalSignature(
				GameTestHelper helper,
				ServerLevel level,
				BlockPos origin
			) {
				int gateY = findBlockY(
					level,
					origin.getX() + 7,
					origin.getZ() + 12,
					state -> state.is(ModBlocks.CHESTNUT_FENCE_GATE)
						&& state.getValue(BlockStateProperties.HORIZONTAL_FACING)
							== net.minecraft.core.Direction.SOUTH
				);
				helper.assertTrue(
					gateY != Integer.MIN_VALUE,
					"Decorated natural Homestead south gate was not physically placed"
				);
				helper.assertTrue(
					level.getBlockState(
						new BlockPos(origin.getX() + 7, gateY, origin.getZ() + 10)
					).is(Blocks.OAK_DOOR),
					"Decorated natural Homestead south door was not physically placed"
				);
				helper.assertTrue(
					level.getBlockState(
						new BlockPos(origin.getX() + 7, gateY - 1, origin.getZ() + 13)
					).is(Blocks.DIRT_PATH),
					"Decorated natural Homestead entrance path was not physically placed"
				);
			}
		},
		CORN_CRIB(
			"appalachian_corn_crib",
			"Appalachian Corn Crib",
			ModStructures.APPALACHIAN_CORN_CRIB,
			ModStructures.APPALACHIAN_CORN_CRIBS
		) {
			@Override
			SiteProfile sampleSite(GenerationFixture fixture, ChunkPos chunk) {
				int originX =
					chunk.getMiddleBlockX() - AppalachianCornCribStructure.TEMPLATE_WIDTH / 2;
				int originZ =
					chunk.getMiddleBlockZ() - AppalachianCornCribStructure.TEMPLATE_DEPTH / 2;
				return sampleSiteWithEarlyRejection(
					fixture,
					originX,
					originZ,
					AppalachianCornCribStructure.TEMPLATE_WIDTH,
					AppalachianCornCribStructure.SITE_DEPTH,
					AppalachianCornCribStructure.MAX_SITE_RELIEF
				);
			}

			@Override
			BlockPos templateOrigin(GameTestHelper helper, StructurePiece piece) {
				helper.assertTrue(
					piece instanceof AppalachianCornCribPiece,
					"Natural Corn Crib start did not contain an AppalachianCornCribPiece"
				);
				return ((AppalachianCornCribPiece) piece).templatePosition();
			}

			@Override
			void assertPhysicalSignature(
				GameTestHelper helper,
				ServerLevel level,
				BlockPos origin
			) {
				int gateY = findBlockY(
					level,
					origin.getX() + 4,
					origin.getZ() + 8,
					state -> state.is(ModBlocks.CHESTNUT_FENCE_GATE)
						&& state.getValue(BlockStateProperties.HORIZONTAL_FACING)
							== net.minecraft.core.Direction.SOUTH
				);
				helper.assertTrue(
					gateY != Integer.MIN_VALUE,
					"Decorated natural Corn Crib south gate was not physically placed"
				);
				int foundationY = gateY - 3;
				helper.assertTrue(
					level.getBlockState(
						new BlockPos(origin.getX() + 4, foundationY + 1, origin.getZ() + 9)
					).is(ModBlocks.ROUGH_CHESTNUT_BOARD_STAIRS),
					"Decorated natural Corn Crib entry step was not physically placed"
				);
				for (int approachZ : new int[] {10, 11}) {
					helper.assertTrue(
						level.getBlockState(
							new BlockPos(
								origin.getX() + 4,
								foundationY,
								origin.getZ() + approachZ
							)
						).is(Blocks.DIRT_PATH),
						"Decorated natural Corn Crib approach path was not physically placed"
					);
				}
			}
		};

		private final String id;
		private final String label;
		private final ResourceKey<Structure> structureKey;
		private final ResourceKey<StructureSet> structureSetKey;

		NaturalStructureKind(
			String id,
			String label,
			ResourceKey<Structure> structureKey,
			ResourceKey<StructureSet> structureSetKey
		) {
			this.id = id;
			this.label = label;
			this.structureKey = structureKey;
			this.structureSetKey = structureSetKey;
		}

		String id() {
			return id;
		}

		String label() {
			return label;
		}

		ResourceKey<Structure> structureKey() {
			return structureKey;
		}

		ResourceKey<StructureSet> structureSetKey() {
			return structureSetKey;
		}

		abstract SiteProfile sampleSite(GenerationFixture fixture, ChunkPos chunk);

		abstract BlockPos templateOrigin(GameTestHelper helper, StructurePiece piece);

		abstract void assertPhysicalSignature(
			GameTestHelper helper,
			ServerLevel level,
			BlockPos origin
		);
	}
}
