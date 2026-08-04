package net.beforetheblight.gametest;

import net.beforetheblight.block.HewnChestnutPostBlock;
import net.beforetheblight.block.HewnChestnutPostBlock.PostConnection;
import net.beforetheblight.registry.ModBlocks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class HewnDetailGameTests {
	private static final BlockPos POST_POS = new BlockPos(2, 2, 2);
	private static final BlockPos WALL_POS = new BlockPos(4, 2, 4);

	@GameTest(maxTicks = 20)
	public void postShapeAndWaterStateMatchEveryAxis(GameTestHelper helper) {
		for (Direction.Axis axis : Direction.Axis.values()) {
			BlockState state = ModBlocks.HEWN_CHESTNUT_POST
				.defaultBlockState()
				.setValue(RotatedPillarBlock.AXIS, axis);
			helper.setBlock(POST_POS, state);
			AABB bounds = state
				.getShape(helper.getLevel(), helper.absolutePos(POST_POS))
				.bounds();
			helper.assertValueEqual(bounds, expectedBounds(axis), axis.getName() + " post bounds");
		}

		BlockState waterlogged = ModBlocks.HEWN_CHESTNUT_POST
			.defaultBlockState()
			.setValue(HewnChestnutPostBlock.WATERLOGGED, true);
		helper.assertTrue(
			waterlogged.getFluidState().is(Fluids.WATER),
			"waterlogged post must retain a water fluid state"
		);

		BlockState rotated = ModBlocks.HEWN_CHESTNUT_POST
			.defaultBlockState()
			.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X)
			.setValue(HewnChestnutPostBlock.WATERLOGGED, true)
			.setValue(HewnChestnutPostBlock.NORTH, PostConnection.WALL_TALL)
			.setValue(HewnChestnutPostBlock.EAST, PostConnection.RAIL)
			.rotate(Rotation.CLOCKWISE_90);
		helper.assertValueEqual(
			rotated.getValue(RotatedPillarBlock.AXIS),
			Direction.Axis.Z,
			"clockwise rotation must rotate a horizontal post axis"
		);
		helper.assertValueEqual(
			rotated.getValue(HewnChestnutPostBlock.EAST),
			PostConnection.WALL_TALL,
			"clockwise rotation must move the north tall-wall join east"
		);
		helper.assertValueEqual(
			rotated.getValue(HewnChestnutPostBlock.SOUTH),
			PostConnection.RAIL,
			"clockwise rotation must move the east rail join south"
		);
		helper.assertTrue(
			rotated.getValue(HewnChestnutPostBlock.WATERLOGGED),
			"rotation must retain waterlogging"
		);
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void adjacentHewnWallsConnectThroughTheWallTag(GameTestHelper helper) {
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			assertPostConnection(
				helper,
				direction,
				ModBlocks.HEWN_CHESTNUT_WALL,
				PostConnection.WALL_LOW
			);
			BlockState wall = helper.getBlockState(WALL_POS.relative(direction));
			helper.assertTrue(
				wall.getValue(WallBlock.PROPERTY_BY_DIRECTION.get(direction.getOpposite()))
					!= WallSide.NONE,
				direction.getName() + " wall must extend into the post block"
			);

			assertPostConnection(
				helper,
				direction,
				ModBlocks.SPLIT_CHESTNUT_RAILS,
				PostConnection.RAIL
			);
			BlockState rail = helper.getBlockState(WALL_POS.relative(direction));
			helper.assertTrue(
				rail.getValue(CrossCollisionBlock.PROPERTY_BY_DIRECTION.get(direction.getOpposite())),
				direction.getName() + " split rail must extend into the post block"
			);
		}

		assertPostConnection(
			helper,
			Direction.NORTH,
			Blocks.COBBLESTONE_WALL,
			PostConnection.WALL_LOW
		);
		BlockState cobblestoneWall = helper.getBlockState(WALL_POS.north());
		helper.assertTrue(
			cobblestoneWall.getValue(WallBlock.SOUTH) != WallSide.NONE,
			"cobblestone wall must extend south into the chestnut post"
		);

		clearHorizontalNeighbours(helper);
		helper.setBlock(WALL_POS, ModBlocks.HEWN_CHESTNUT_POST.defaultBlockState());
		BlockPos tallWallPos = WALL_POS.north();
		helper.setBlock(tallWallPos.above(), Blocks.STONE);
		helper.setBlock(
			tallWallPos,
			ModBlocks.HEWN_CHESTNUT_WALL
				.defaultBlockState()
				.setValue(WallBlock.SOUTH, WallSide.TALL)
		);
		refreshNeighbours(helper, tallWallPos, ModBlocks.HEWN_CHESTNUT_WALL);
		BlockState tallWall = helper.getBlockState(tallWallPos);
		helper.assertValueEqual(
			tallWall.getValue(WallBlock.SOUTH),
			WallSide.TALL,
			"covered hewn wall must expose its tall side toward the post"
		);
		BlockState tallPost = helper.getBlockState(WALL_POS);
		helper.assertValueEqual(
			tallPost.getValue(HewnChestnutPostBlock.NORTH),
			PostConnection.WALL_TALL,
			"post must distinguish the neighbour's tall wall side"
		);
		assertConnectionShape(
			helper,
			tallPost,
			Direction.NORTH,
			PostConnection.WALL_TALL
		);

		assertPostConnection(
			helper,
			Direction.EAST,
			Blocks.OAK_FENCE,
			PostConnection.RAIL
		);
		BlockState oakFence = helper.getBlockState(WALL_POS.east());
		helper.assertTrue(
			oakFence.getValue(CrossCollisionBlock.WEST),
			"oak fence must extend west into the chestnut post"
		);

		clearHorizontalNeighbours(helper);
		helper.setBlock(WALL_POS, ModBlocks.HEWN_CHESTNUT_POST.defaultBlockState());
		helper.setBlock(WALL_POS.north(), ModBlocks.HEWN_CHESTNUT_WALL.defaultBlockState());
		helper.setBlock(WALL_POS.east(), ModBlocks.SPLIT_CHESTNUT_RAILS.defaultBlockState());
		refreshNeighbours(helper, WALL_POS.north(), ModBlocks.HEWN_CHESTNUT_WALL);
		refreshNeighbours(helper, WALL_POS.east(), ModBlocks.SPLIT_CHESTNUT_RAILS);
		BlockState mixed = helper.getBlockState(WALL_POS);
		helper.assertValueEqual(
			mixed.getValue(HewnChestnutPostBlock.NORTH),
			PostConnection.WALL_LOW,
			"north mixed join"
		);
		helper.assertValueEqual(
			mixed.getValue(HewnChestnutPostBlock.EAST),
			PostConnection.RAIL,
			"east mixed join"
		);
		helper.succeed();
	}

	private static void assertPostConnection(
		GameTestHelper helper,
		Direction direction,
		Block neighbourBlock,
		PostConnection expectedConnection
	) {
		clearHorizontalNeighbours(helper);
		helper.setBlock(WALL_POS, ModBlocks.HEWN_CHESTNUT_POST.defaultBlockState());
		BlockPos neighbourPos = WALL_POS.relative(direction);
		helper.setBlock(neighbourPos, neighbourBlock.defaultBlockState());
		refreshNeighbours(helper, neighbourPos, neighbourBlock);

		BlockState post = helper.getBlockState(WALL_POS);
		EnumProperty<PostConnection> property =
			HewnChestnutPostBlock.PROPERTY_BY_DIRECTION.get(direction);
		helper.assertValueEqual(
			post.getValue(property),
			expectedConnection,
			direction.getName() + " post connection"
		);
		assertConnectionShape(helper, post, direction, expectedConnection);
	}

	private static void assertConnectionShape(
		GameTestHelper helper,
		BlockState post,
		Direction direction,
		PostConnection expectedConnection
	) {
		AABB expectedBounds = connectedBounds(direction);
		BlockPos absolutePost = helper.absolutePos(WALL_POS);
		VoxelShape outline = post.getShape(helper.getLevel(), absolutePost);
		helper.assertValueEqual(
			outline.bounds(),
			expectedBounds,
			direction.getName() + " connection outline"
		);
		helper.assertValueEqual(
			post.getCollisionShape(helper.getLevel(), absolutePost).bounds(),
			expectedBounds,
			direction.getName() + " connection collision"
		);
		helper.assertTrue(
			!expectedBounds.equals(new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)),
			"connected post must not become a full cube"
		);
		if (
			expectedConnection == PostConnection.WALL_LOW
				|| expectedConnection == PostConnection.WALL_TALL
		) {
			boolean hasTopWallBridge = Shapes.joinIsNotEmpty(
				outline,
				topWallBridgeProbe(direction),
				BooleanOp.AND
			);
			helper.assertValueEqual(
				hasTopWallBridge,
				expectedConnection == PostConnection.WALL_TALL,
				direction.getName() + " tall-wall top bridge"
			);
		}
	}

	private static void clearHorizontalNeighbours(GameTestHelper helper) {
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			helper.setBlock(WALL_POS.relative(direction), Blocks.AIR);
			helper.setBlock(WALL_POS.relative(direction).above(), Blocks.AIR);
		}
	}

	private static void refreshNeighbours(GameTestHelper helper, BlockPos relativePos, Block block) {
		helper.getLevel().updateNeighborsAt(helper.absolutePos(relativePos), block);
		helper.getLevel().updateNeighborsAt(
			helper.absolutePos(WALL_POS),
			ModBlocks.HEWN_CHESTNUT_POST
		);
	}

	private static AABB connectedBounds(Direction direction) {
		return switch (direction) {
			case NORTH -> new AABB(0.25, 0.0, 0.0, 0.75, 1.0, 0.75);
			case EAST -> new AABB(0.25, 0.0, 0.25, 1.0, 1.0, 0.75);
			case SOUTH -> new AABB(0.25, 0.0, 0.25, 0.75, 1.0, 1.0);
			case WEST -> new AABB(0.0, 0.0, 0.25, 0.75, 1.0, 0.75);
			default -> throw new IllegalArgumentException("not horizontal: " + direction);
		};
	}

	private static VoxelShape topWallBridgeProbe(Direction direction) {
		return switch (direction) {
			case NORTH -> Block.box(5.0, 15.0, 0.0, 11.0, 16.0, 3.5);
			case EAST -> Block.box(12.5, 15.0, 5.0, 16.0, 16.0, 11.0);
			case SOUTH -> Block.box(5.0, 15.0, 12.5, 11.0, 16.0, 16.0);
			case WEST -> Block.box(0.0, 15.0, 5.0, 3.5, 16.0, 11.0);
			default -> throw new IllegalArgumentException("not horizontal: " + direction);
		};
	}

	private static AABB expectedBounds(Direction.Axis axis) {
		return switch (axis) {
			case X -> new AABB(0.0, 0.25, 0.25, 1.0, 0.75, 0.75);
			case Y -> new AABB(0.25, 0.0, 0.25, 0.75, 1.0, 0.75);
			case Z -> new AABB(0.25, 0.25, 0.0, 0.75, 0.75, 1.0);
		};
	}

	public HewnDetailGameTests() {
	}
}
