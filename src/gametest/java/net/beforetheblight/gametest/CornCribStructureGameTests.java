package net.beforetheblight.gametest;

import java.util.List;
import java.util.function.IntBinaryOperator;

import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.JsonOps;
import net.beforetheblight.registry.ModBlocks;
import net.beforetheblight.worldgen.biome.ModBiomes;
import net.beforetheblight.worldgen.structure.AppalachianCornCribPiece;
import net.beforetheblight.worldgen.structure.AppalachianCornCribStructure;
import net.beforetheblight.worldgen.structure.ModStructureTypes;
import net.beforetheblight.worldgen.structure.ModStructures;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.commands.PlaceCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

public final class CornCribStructureGameTests {
	private static final int EXPECTED_SITE_SAMPLES =
		AppalachianCornCribStructure.TEMPLATE_WIDTH * AppalachianCornCribStructure.SITE_DEPTH;

	@GameTest(maxTicks = 20)
	public void cornCribRegistriesTemplateAndPieceCodecDecode(GameTestHelper helper) {
		Holder.Reference<Structure> structure = helper.getLevel().registryAccess()
			.lookupOrThrow(Registries.STRUCTURE)
			.getOrThrow(ModStructures.APPALACHIAN_CORN_CRIB);
		helper.assertTrue(
			structure.value() instanceof AppalachianCornCribStructure,
			"Appalachian Corn Crib did not decode through its custom StructureType"
		);
		helper.assertValueEqual(
			structure.value().type(),
			ModStructureTypes.APPALACHIAN_CORN_CRIB,
			"Appalachian Corn Crib StructureType"
		);
		helper.assertValueEqual(
			structure.value().biomes().size(),
			1,
			"Appalachian Corn Crib exact biome target count"
		);
		helper.assertTrue(
			structure.value().biomes().stream()
				.anyMatch(biome -> biome.is(ModBiomes.CHESTNUT_OAK_RIDGE)),
			"Appalachian Corn Crib is not enabled in Chestnut-Oak Ridge"
		);

		StructureSet structureSet = helper.getLevel().registryAccess()
			.lookupOrThrow(Registries.STRUCTURE_SET)
			.getValueOrThrow(ModStructures.APPALACHIAN_CORN_CRIBS);
		helper.assertValueEqual(structureSet.structures().size(), 1, "one-piece Corn Crib structure set");
		helper.assertTrue(
			structureSet.structures().getFirst().structure().is(ModStructures.APPALACHIAN_CORN_CRIB),
			"Corn Crib structure set references the wrong structure"
		);
		helper.assertTrue(
			structureSet.placement() instanceof RandomSpreadStructurePlacement,
			"Corn Crib does not use random-spread placement"
		);
		RandomSpreadStructurePlacement placement =
			(RandomSpreadStructurePlacement) structureSet.placement();
		helper.assertValueEqual(placement.spacing(), 16, "Corn Crib random-spread spacing");
		helper.assertValueEqual(placement.separation(), 6, "Corn Crib random-spread separation");
		JsonObject encodedPlacement = StructurePlacement.CODEC
			.encodeStart(
				RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess()),
				placement
			)
			.getOrThrow()
			.getAsJsonObject();
		JsonObject exclusionZone = encodedPlacement.getAsJsonObject("exclusion_zone");
		helper.assertTrue(exclusionZone != null, "Corn Crib placement has no exclusion zone");
		helper.assertValueEqual(
			exclusionZone.get("other_set").getAsString(),
			"before_the_blight:appalachian_homesteads",
			"Corn Crib exclusion target"
		);
		helper.assertValueEqual(
			exclusionZone.get("chunk_count").getAsInt(),
			1,
			"Corn Crib exclusion radius"
		);

		StructureTemplate template = helper.getLevel().getStructureManager()
			.getOrCreate(AppalachianCornCribStructure.TEMPLATE_ID);
		Vec3i size = template.getSize();
		helper.assertValueEqual(size.getX(), 9, "Corn Crib template width");
		helper.assertValueEqual(size.getY(), 10, "Corn Crib template height");
		helper.assertValueEqual(size.getZ(), 11, "Corn Crib template depth");

		BlockPos originalPosition = helper.absolutePos(new BlockPos(2, 2, 2));
		AppalachianCornCribPiece original = new AppalachianCornCribPiece(
			helper.getLevel().getStructureManager(),
			originalPosition
		);
		StructurePieceSerializationContext serialization =
			StructurePieceSerializationContext.fromLevel(helper.getLevel());
		CompoundTag saved = original.createTag(serialization);
		StructurePiece loaded = ModStructureTypes.APPALACHIAN_CORN_CRIB_PIECE.load(
			serialization,
			saved
		);
		helper.assertTrue(
			loaded instanceof AppalachianCornCribPiece,
			"Corn Crib piece codec did not restore the custom template piece"
		);
		AppalachianCornCribPiece loadedPiece = (AppalachianCornCribPiece) loaded;
		helper.assertValueEqual(
			loadedPiece.templatePosition(),
			originalPosition,
			"Corn Crib piece template position round-trip"
		);
		helper.assertValueEqual(
			loadedPiece.getRotation(),
			Rotation.NONE,
			"Corn Crib fixed south-facing orientation round-trip"
		);
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void cornCribTerrainScreenIncludesSouthApproach(GameTestHelper helper) {
		List<SiteCase> cases = List.of(
			new SiteCase("flat", (x, z) -> 70, true, 0),
			new SiteCase("gentle east rise", (x, z) -> 70 + x * 2 / 8, true, 2),
			new SiteCase("gentle south rise", (x, z) -> 70 + z * 2 / 11, true, 2),
			new SiteCase("three-block building rise", (x, z) -> x == 8 ? 73 : 70, false, 3),
			new SiteCase("three-block approach rise", (x, z) -> z == 11 ? 73 : 70, false, 3),
			new SiteCase("three-block approach pit", (x, z) -> z == 11 ? 67 : 70, false, 3)
		);
		for (SiteCase siteCase : cases) {
			AppalachianCornCribStructure.SiteProfile profile =
				AppalachianCornCribStructure.sampleSite(siteCase.heights(), 0, 0);
			helper.assertValueEqual(
				profile.sampledColumns(),
				EXPECTED_SITE_SAMPLES,
				siteCase.name() + " sampled the complete crib and south approach"
			);
			helper.assertValueEqual(
				profile.dryColumns(),
				EXPECTED_SITE_SAMPLES,
				siteCase.name() + " synthetic dry-column count"
			);
			helper.assertValueEqual(
				profile.relief(),
				siteCase.expectedRelief(),
				siteCase.name() + " relief"
			);
			helper.assertValueEqual(
				profile.isBuildable(),
				siteCase.expectedBuildable(),
				siteCase.name() + " slope decision"
			);
			helper.assertValueEqual(
				profile.foundationY(),
				profile.maximumSurfaceY() - 1,
				siteCase.name() + " foundation follows the highest sampled surface"
			);
		}
		AppalachianCornCribStructure.SiteProfile wetProfile =
			AppalachianCornCribStructure.sampleSite(
				(x, z) -> 70,
				(x, z) -> x == 4 && z == 11 ? 68 : 70,
				0,
				0
			);
		helper.assertValueEqual(
			wetProfile.dryColumns(),
			EXPECTED_SITE_SAMPLES - 1,
			"one submerged Corn Crib approach column is detected"
		);
		helper.assertValueEqual(
			wetProfile.isBuildable(),
			false,
			"a flat Corn Crib footprint containing water is rejected"
		);
		helper.succeed();
	}

	@GameTest(maxTicks = 200)
	public void placeStructureCommandBuildsSupportedUsableCornCrib(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos requestedPosition = helper.absolutePos(new BlockPos(48, 0, 48));
		ChunkPos sourceChunk = ChunkPos.containing(requestedPosition);
		for (int offsetX = -1; offsetX <= 1; offsetX++) {
			for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
				level.getChunk(sourceChunk.x() + offsetX, sourceChunk.z() + offsetZ);
			}
		}

		Holder.Reference<Structure> structure = level.registryAccess()
			.lookupOrThrow(Registries.STRUCTURE)
			.getOrThrow(ModStructures.APPALACHIAN_CORN_CRIB);
		CommandSourceStack source = level.getServer()
			.createCommandSourceStack()
			.withLevel(level)
			.withPosition(Vec3.atCenterOf(requestedPosition));
		try {
			helper.assertValueEqual(
				PlaceCommand.placeStructure(source, structure, requestedPosition),
				1,
				"/place structure Appalachian Corn Crib result"
			);
		} catch (CommandSyntaxException error) {
			helper.fail("/place structure rejected Appalachian Corn Crib: " + error.getMessage());
			return;
		}

		int originX = sourceChunk.getMiddleBlockX() - AppalachianCornCribStructure.TEMPLATE_WIDTH / 2;
		int originZ = sourceChunk.getMiddleBlockZ() - AppalachianCornCribStructure.TEMPLATE_DEPTH / 2;
		int gateY = findBlockY(
			level,
			originX + 4,
			originZ + 8,
			state -> state.is(ModBlocks.CHESTNUT_FENCE_GATE)
				&& state.getValue(BlockStateProperties.HORIZONTAL_FACING) == net.minecraft.core.Direction.SOUTH
		);
		helper.assertTrue(gateY != Integer.MIN_VALUE, "placed Corn Crib south gate was not found");
		int foundationY = gateY - 3;
		BlockState step = level.getBlockState(new BlockPos(originX + 4, foundationY + 1, originZ + 9));
		helper.assertTrue(
			step.is(ModBlocks.ROUGH_CHESTNUT_BOARD_STAIRS),
			"placed Corn Crib south entry step is missing"
		);

		for (int approachZ : new int[] {10, 11}) {
			BlockPos path = new BlockPos(originX + 4, foundationY, originZ + approachZ);
			helper.assertTrue(
				level.getBlockState(path).is(Blocks.DIRT_PATH),
				"placed Corn Crib south approach path is missing at local Z=" + approachZ
			);
			helper.assertTrue(
				level.getBlockState(path.above()).isAir()
					&& level.getBlockState(path.above(2)).isAir(),
				"placed Corn Crib south approach is obstructed at local Z=" + approachZ
			);
		}

		int[][] pierColumns = {{2, 2}, {6, 2}, {2, 5}, {6, 5}, {2, 8}, {6, 8}};
		for (int[] pier : pierColumns) {
			BlockPos base = new BlockPos(originX + pier[0], foundationY, originZ + pier[1]);
			helper.assertTrue(
				level.getBlockState(base).is(ModBlocks.FIELDSTONE)
					&& level.getBlockState(base.above()).is(ModBlocks.FIELDSTONE),
				"placed Corn Crib fieldstone pier is incomplete at " + base
			);
			BlockState support = level.getBlockState(base.below());
			helper.assertTrue(
				!support.isAir() && support.getFluidState().isEmpty(),
				"placed Corn Crib pier does not meet dry ground at " + base
			);
		}
		helper.assertTrue(
			level.getBlockState(new BlockPos(originX + 4, foundationY + 1, originZ + 5)).isAir(),
			"placed Corn Crib lost its ventilated underfloor"
		);
		helper.succeed();
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

	private record SiteCase(
		String name,
		IntBinaryOperator heights,
		boolean expectedBuildable,
		int expectedRelief
	) {
	}
}
