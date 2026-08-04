package net.beforetheblight.gametest;

import java.util.List;
import java.util.function.IntBinaryOperator;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.beforetheblight.registry.ModBlocks;
import net.beforetheblight.worldgen.biome.ModBiomes;
import net.beforetheblight.worldgen.structure.AppalachianHomesteadPiece;
import net.beforetheblight.worldgen.structure.AppalachianHomesteadStructure;
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
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

public final class HomesteadStructureGameTests {
	private static final int EXPECTED_SITE_SAMPLES =
		AppalachianHomesteadStructure.TEMPLATE_WIDTH * AppalachianHomesteadStructure.SITE_DEPTH;

	@GameTest(maxTicks = 20)
	public void homesteadRegistriesTemplateAndPieceCodecDecode(GameTestHelper helper) {
		Holder.Reference<Structure> structure = helper.getLevel().registryAccess()
			.lookupOrThrow(Registries.STRUCTURE)
			.getOrThrow(ModStructures.APPALACHIAN_HOMESTEAD);
		helper.assertTrue(
			structure.value() instanceof AppalachianHomesteadStructure,
			"Appalachian Homestead did not decode through its custom StructureType"
		);
		helper.assertValueEqual(
			structure.value().type(),
			ModStructureTypes.APPALACHIAN_HOMESTEAD,
			"Appalachian Homestead StructureType"
		);
		helper.assertValueEqual(
			structure.value().biomes().size(),
			2,
			"Appalachian Homestead exact biome target count"
		);
		helper.assertTrue(
			structure.value().biomes().stream().anyMatch(biome -> biome.is(ModBiomes.CHESTNUT_OAK_RIDGE)),
			"Appalachian Homestead is not enabled in Chestnut-Oak Ridge"
		);
		helper.assertTrue(
			structure.value().biomes().stream().anyMatch(biome -> biome.is(ModBiomes.HEMLOCK_BEECH_COVE)),
			"Appalachian Homestead is not enabled in Hemlock Cove"
		);

		StructureSet structureSet = helper.getLevel().registryAccess()
			.lookupOrThrow(Registries.STRUCTURE_SET)
			.getValueOrThrow(ModStructures.APPALACHIAN_HOMESTEADS);
		helper.assertValueEqual(structureSet.structures().size(), 1, "one-piece Homestead structure set");
		helper.assertTrue(
			structureSet.structures().getFirst().structure().is(ModStructures.APPALACHIAN_HOMESTEAD),
			"Homestead structure set references the wrong structure"
		);
		helper.assertTrue(
			structureSet.placement() instanceof RandomSpreadStructurePlacement,
			"Homestead does not use random-spread placement"
		);
		RandomSpreadStructurePlacement placement =
			(RandomSpreadStructurePlacement) structureSet.placement();
		helper.assertValueEqual(placement.spacing(), 8, "Homestead random-spread spacing");
		helper.assertValueEqual(placement.separation(), 3, "Homestead random-spread separation");

		StructureTemplate template = helper.getLevel().getStructureManager()
			.getOrCreate(AppalachianHomesteadStructure.TEMPLATE_ID);
		Vec3i size = template.getSize();
		helper.assertValueEqual(size.getX(), 17, "cabin template width");
		helper.assertValueEqual(size.getY(), 14, "cabin template height");
		helper.assertValueEqual(size.getZ(), 13, "cabin template depth");

		BlockPos originalPosition = helper.absolutePos(new BlockPos(2, 2, 2));
		AppalachianHomesteadPiece original = new AppalachianHomesteadPiece(
			helper.getLevel().getStructureManager(),
			originalPosition
		);
		StructurePieceSerializationContext serialization =
			StructurePieceSerializationContext.fromLevel(helper.getLevel());
		CompoundTag saved = original.createTag(serialization);
		StructurePiece loaded = ModStructureTypes.APPALACHIAN_HOMESTEAD_PIECE.load(
			serialization,
			saved
		);
		helper.assertTrue(
			loaded instanceof AppalachianHomesteadPiece,
			"Homestead piece codec did not restore the custom template piece"
		);
		AppalachianHomesteadPiece loadedPiece = (AppalachianHomesteadPiece) loaded;
		helper.assertValueEqual(
			loadedPiece.templatePosition(),
			originalPosition,
			"Homestead piece template position round-trip"
		);
		helper.assertValueEqual(
			loadedPiece.getRotation(),
			Rotation.NONE,
			"Homestead fixed south-facing orientation round-trip"
		);
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void homesteadSyntheticSlopeProfiles(GameTestHelper helper) {
		List<SiteCase> cases = List.of(
			new SiteCase("flat low", (x, z) -> -40, true, 0),
			new SiteCase("flat upland", (x, z) -> 96, true, 0),
			new SiteCase("one-block checker", (x, z) -> 70 + ((x + z) & 1), true, 1),
			new SiteCase("two-block center rise", (x, z) -> inCenter(x, z) ? 74 : 72, true, 2),
			new SiteCase("three-block high corner", (x, z) -> x == 16 && z == 13 ? 83 : 80, true, 3),
			new SiteCase("three-block low corner", (x, z) -> x == 0 && z == 0 ? 77 : 80, true, 3),
			new SiteCase("gentle east rise", (x, z) -> 64 + x * 3 / 16, true, 3),
			new SiteCase("gentle south rise", (x, z) -> 64 + z * 3 / 13, true, 3),
			new SiteCase("gentle diagonal rise", (x, z) -> 64 + (x + z) * 3 / 29, true, 3),
			new SiteCase("three-step terrace", (x, z) -> 70 + (x < 6 ? 0 : x < 12 ? 1 : 3), true, 3),
			new SiteCase("four-block high corner", (x, z) -> x == 16 && z == 13 ? 84 : 80, false, 4),
			new SiteCase("four-block low corner", (x, z) -> x == 0 && z == 0 ? 76 : 80, false, 4),
			new SiteCase("steep east rise", (x, z) -> 64 + x * 4 / 16, false, 4),
			new SiteCase("steep south rise", (x, z) -> 64 + z * 4 / 13, false, 4),
			new SiteCase("steep diagonal rise", (x, z) -> 64 + (x + z) * 4 / 29, false, 4),
			new SiteCase("four-block checker", (x, z) -> 70 + (((x + z) & 1) * 4), false, 4),
			new SiteCase("six-block center pit", (x, z) -> inCenter(x, z) ? 64 : 70, false, 6),
			new SiteCase("ten-block center spike", (x, z) -> inCenter(x, z) ? 80 : 70, false, 10),
			new SiteCase("four-block east step", (x, z) -> x < 8 ? 72 : 76, false, 4),
			new SiteCase("five-block south step", (x, z) -> z < 7 ? 72 : 77, false, 5)
		);
		helper.assertValueEqual(cases.size(), 20, "exact synthetic Homestead site-case count");
		for (SiteCase siteCase : cases) {
			AppalachianHomesteadStructure.SiteProfile profile =
				AppalachianHomesteadStructure.sampleSite(siteCase.heights(), 0, 0);
			helper.assertValueEqual(
				profile.sampledColumns(),
				EXPECTED_SITE_SAMPLES,
				siteCase.name() + " sampled every cabin and entrance column"
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
		AppalachianHomesteadStructure.SiteProfile wetProfile =
			AppalachianHomesteadStructure.sampleSite(
				(x, z) -> 70,
				(x, z) -> x == 0 && z == 0 ? 69 : 70,
				0,
				0
			);
		helper.assertValueEqual(
			wetProfile.dryColumns(),
			EXPECTED_SITE_SAMPLES - 1,
			"one submerged Homestead column is detected"
		);
		helper.assertValueEqual(
			wetProfile.isBuildable(),
			false,
			"a flat Homestead footprint containing water is rejected"
		);
		helper.succeed();
	}

	@GameTest(maxTicks = 200)
	public void placeStructureCommandBuildsUsableHomestead(GameTestHelper helper) {
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
			.getOrThrow(ModStructures.APPALACHIAN_HOMESTEAD);
		CommandSourceStack source = level.getServer()
			.createCommandSourceStack()
			.withLevel(level)
			.withPosition(Vec3.atCenterOf(requestedPosition));
		try {
			helper.assertValueEqual(
				PlaceCommand.placeStructure(source, structure, requestedPosition),
				1,
				"/place structure Appalachian Homestead result"
			);
		} catch (CommandSyntaxException error) {
			helper.fail("/place structure rejected Appalachian Homestead: " + error.getMessage());
			return;
		}

		int originX = sourceChunk.getMiddleBlockX() - AppalachianHomesteadStructure.TEMPLATE_WIDTH / 2;
		int originZ = sourceChunk.getMiddleBlockZ() - AppalachianHomesteadStructure.TEMPLATE_DEPTH / 2;
		int gateY = findBlockY(
			level,
			originX + 7,
			originZ + 12,
			state -> state.is(ModBlocks.CHESTNUT_FENCE_GATE)
				&& state.getValue(BlockStateProperties.HORIZONTAL_FACING) == net.minecraft.core.Direction.SOUTH
		);
		helper.assertTrue(gateY != Integer.MIN_VALUE, "placed Homestead south gate was not found");
		BlockState door = level.getBlockState(new BlockPos(originX + 7, gateY, originZ + 10));
		helper.assertTrue(door.is(Blocks.OAK_DOOR), "placed Homestead south door is missing");
		helper.assertValueEqual(
			door.getValue(BlockStateProperties.HORIZONTAL_FACING),
			net.minecraft.core.Direction.SOUTH,
			"placed Homestead deterministic door orientation"
		);
		helper.assertTrue(
			level.getBlockState(new BlockPos(originX + 7, gateY - 1, originZ + 13)).is(Blocks.DIRT_PATH),
			"placed Homestead entrance path is missing"
		);
		helper.assertTrue(
			level.getBlockState(new BlockPos(originX + 7, gateY, originZ + 13)).isAir()
				&& level.getBlockState(new BlockPos(originX + 7, gateY + 1, originZ + 13)).isAir(),
			"placed Homestead entrance clearance is blocked"
		);
		helper.succeed();
	}

	private static boolean inCenter(int x, int z) {
		return x >= 6 && x <= 10 && z >= 5 && z <= 8;
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
