package net.beforetheblight.gametest;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.function.Predicate;
import net.beforetheblight.block.DryingCornBundleBlock;
import net.beforetheblight.registry.ModBlocks;
import net.beforetheblight.registry.ModCornCribBlocks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.commands.PlaceCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

public final class AuthoredTemplateGameTests {
	private static final Identifier CABIN = id("appalachian_demo_cabin");
	private static final Identifier CORN_CRIB = id("appalachian_corn_crib");
	private static final Identifier SPRINGHOUSE = id("appalachian_springhouse");

	@GameTest(maxTicks = 100)
	public void placeTemplateCommandBuildsExactCabinPayload(GameTestHelper helper) {
		PlacementResult result = placeTemplate(helper, CABIN, new Vec3i(17, 14, 13));
		helper.assertValueEqual(result.nonAirBlocks(), 747, "Cabin direct-template non-air count");
		helper.assertValueEqual(
			countMatching(result, state -> state.is(ModBlocks.FIELDSTONE)),
			191,
			"Cabin direct-template fieldstone count"
		);
		assertStairFacing(
			helper,
			result,
			new BlockPos(6, 7, 1),
			ModBlocks.CHESTNUT_SHINGLE_STAIRS,
			Direction.SOUTH,
			"Cabin north roof rises toward ridge"
		);
		assertStairFacing(
			helper,
			result,
			new BlockPos(6, 6, 12),
			ModBlocks.CHESTNUT_SHINGLE_STAIRS,
			Direction.NORTH,
			"Cabin south roof rises toward ridge"
		);
		assertStairFacing(
			helper,
			result,
			new BlockPos(9, 1, 6),
			ModBlocks.FIELDSTONE_STAIRS,
			Direction.EAST,
			"Cabin hearth apron rises toward hearth"
		);
		assertStairFacing(
			helper,
			result,
			new BlockPos(5, 2, 5),
			ModBlocks.CHESTNUT_STAIRS,
			Direction.WEST,
			"Cabin west chair back faces away from table"
		);
		assertStairFacing(
			helper,
			result,
			new BlockPos(9, 2, 5),
			ModBlocks.CHESTNUT_STAIRS,
			Direction.EAST,
			"Cabin east chair back faces away from table"
		);
		helper.succeed();
	}

	@GameTest(maxTicks = 100)
	public void placeTemplateCommandBuildsExactCornCribPayload(GameTestHelper helper) {
		PlacementResult result = placeTemplate(helper, CORN_CRIB, new Vec3i(9, 10, 11));
		helper.assertValueEqual(result.nonAirBlocks(), 226, "Corn Crib direct-template non-air count");
		helper.assertValueEqual(
			countMatching(
				result,
				state -> state.is(ModBlocks.DRYING_CORN_BUNDLE)
					&& state.getValue(DryingCornBundleBlock.COUNT) == DryingCornBundleBlock.MAX_EAR_COUNT
					&& state.getValue(DryingCornBundleBlock.AGE) == DryingCornBundleBlock.MAX_AGE
			),
			2,
			"Corn Crib direct-template full mature drying-rack count"
		);
		helper.assertValueEqual(
			countMatching(result, state -> state.is(ModCornCribBlocks.YELLOW_EAR_CORN_PILE)),
			8,
			"Corn Crib direct-template yellow ear-corn pile count"
		);
		assertStairFacing(
			helper,
			result,
			new BlockPos(1, 7, 5),
			ModBlocks.CHESTNUT_SHINGLE_STAIRS,
			Direction.EAST,
			"Corn Crib west roof rises toward ridge"
		);
		assertStairFacing(
			helper,
			result,
			new BlockPos(7, 7, 5),
			ModBlocks.CHESTNUT_SHINGLE_STAIRS,
			Direction.WEST,
			"Corn Crib east roof rises toward ridge"
		);
		assertStairFacing(
			helper,
			result,
			new BlockPos(4, 1, 9),
			ModBlocks.ROUGH_CHESTNUT_BOARD_STAIRS,
			Direction.NORTH,
			"Corn Crib south entry stair rises inward"
		);
		helper.succeed();
	}

	@GameTest(maxTicks = 100)
	public void placeTemplateCommandBuildsExactSpringhousePayload(GameTestHelper helper) {
		PlacementResult result = placeTemplate(helper, SPRINGHOUSE, new Vec3i(9, 10, 9));
		helper.assertValueEqual(result.nonAirBlocks(), 310, "Springhouse direct-template non-air count");
		helper.assertValueEqual(
			countMatching(result, state -> state.is(Blocks.WATER)),
			5,
			"Springhouse direct-template spring-water count"
		);
		assertStairFacing(
			helper,
			result,
			new BlockPos(4, 5, 0),
			ModBlocks.CHESTNUT_SHINGLE_STAIRS,
			Direction.SOUTH,
			"Springhouse north roof rises toward ridge"
		);
		assertStairFacing(
			helper,
			result,
			new BlockPos(4, 5, 8),
			ModBlocks.CHESTNUT_SHINGLE_STAIRS,
			Direction.NORTH,
			"Springhouse south roof rises toward ridge"
		);
		assertStairFacing(
			helper,
			result,
			new BlockPos(4, 0, 8),
			ModBlocks.FIELDSTONE_STAIRS,
			Direction.NORTH,
			"Springhouse south entry stair rises inward"
		);
		helper.succeed();
	}

	private static PlacementResult placeTemplate(
		GameTestHelper helper,
		Identifier templateId,
		Vec3i expectedSize
	) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(40, 80, 40));
		StructureTemplate template = level.getStructureManager().getOrCreate(templateId);
		helper.assertValueEqual(template.getSize(), expectedSize, templateId + " runtime template size");

		ChunkPos firstChunk = ChunkPos.containing(origin);
		ChunkPos lastChunk = ChunkPos.containing(origin.offset(expectedSize));
		for (int chunkX = firstChunk.x(); chunkX <= lastChunk.x(); chunkX++) {
			for (int chunkZ = firstChunk.z(); chunkZ <= lastChunk.z(); chunkZ++) {
				level.getChunk(chunkX, chunkZ);
			}
		}
		clearTemplateVolume(level, origin, expectedSize);

		CommandSourceStack source = level.getServer()
			.createCommandSourceStack()
			.withLevel(level)
			.withPosition(Vec3.atCenterOf(origin));
		try {
			helper.assertValueEqual(
				PlaceCommand.placeTemplate(
					source,
					templateId,
					origin,
					Rotation.NONE,
					Mirror.NONE,
					1.0F,
					0,
					false
				),
				1,
				"/place template result for " + templateId
			);
		} catch (CommandSyntaxException error) {
			helper.fail("/place template rejected " + templateId + ": " + error.getMessage());
			return new PlacementResult(level, origin, expectedSize, -1);
		}

		int nonAirBlocks = countMatching(
			new PlacementResult(level, origin, expectedSize, 0),
			state -> !state.isAir()
		);
		return new PlacementResult(level, origin, expectedSize, nonAirBlocks);
	}

	private static void clearTemplateVolume(ServerLevel level, BlockPos origin, Vec3i size) {
		for (int x = 0; x < size.getX(); x++) {
			for (int y = 0; y < size.getY(); y++) {
				for (int z = 0; z < size.getZ(); z++) {
					level.setBlock(
						origin.offset(x, y, z),
						Blocks.AIR.defaultBlockState(),
						Block.UPDATE_CLIENTS
					);
				}
			}
		}
	}

	private static int countMatching(PlacementResult result, Predicate<BlockState> predicate) {
		int count = 0;
		for (int x = 0; x < result.size().getX(); x++) {
			for (int y = 0; y < result.size().getY(); y++) {
				for (int z = 0; z < result.size().getZ(); z++) {
					if (predicate.test(result.level().getBlockState(result.origin().offset(x, y, z)))) {
						count++;
					}
				}
			}
		}
		return count;
	}

	private static void assertStairFacing(
		GameTestHelper helper,
		PlacementResult result,
		BlockPos localPosition,
		Block expectedBlock,
		Direction expectedFacing,
		String label
	) {
		BlockState state = result.level().getBlockState(result.origin().offset(localPosition));
		helper.assertTrue(state.is(expectedBlock), label + " block");
		helper.assertValueEqual(
			state.getValue(BlockStateProperties.HORIZONTAL_FACING),
			expectedFacing,
			label + " facing"
		);
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath("before_the_blight", path);
	}

	private record PlacementResult(
		ServerLevel level,
		BlockPos origin,
		Vec3i size,
		int nonAirBlocks
	) {
	}
}
