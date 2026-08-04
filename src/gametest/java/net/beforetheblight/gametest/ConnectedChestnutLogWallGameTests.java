package net.beforetheblight.gametest;

import java.util.EnumSet;
import java.util.Set;

import net.beforetheblight.block.ConnectedChestnutLogWallBlock;
import net.beforetheblight.block.ConnectedChestnutLogWallBlock.Course;
import net.beforetheblight.registry.ModTimberBlocks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ConnectedChestnutLogWallGameTests {
	private static final BlockPos CENTER = new BlockPos(4, 2, 4);
	private static final ConnectedChestnutLogWallBlock[] WALL_FORMS = {
		ModTimberBlocks.CONNECTED_CHESTNUT_LOG_WALL,
		ModTimberBlocks.CHESTNUT_LOG_DOOR_TERMINATION,
		ModTimberBlocks.CHESTNUT_LOG_WINDOW_TERMINATION,
	};

	@GameTest(maxTicks = 20)
	public void allWallFormsConnectAndDisconnectAlongBothAxes(
		GameTestHelper helper
	) {
		for (Direction.Axis axis : horizontalAxes()) {
			Direction negative = negativeDirection(axis);
			Direction positive = negative.getOpposite();
			Direction.Axis perpendicularAxis = perpendicularAxis(axis);
			Direction perpendicularNegative = negativeDirection(perpendicularAxis);
			Direction perpendicularPositive = perpendicularNegative.getOpposite();

			for (ConnectedChestnutLogWallBlock centerBlock : WALL_FORMS) {
				for (ConnectedChestnutLogWallBlock neighbourBlock : WALL_FORMS) {
					for (Direction direction : new Direction[] { negative, positive }) {
						clearFixture(helper);
						setWall(helper, CENTER, centerBlock, axis);
						BlockPos neighbourPos = CENTER.relative(direction);
						setWall(helper, neighbourPos, neighbourBlock, axis);
						assertConnection(
							helper,
							CENTER,
							alongProperty(direction),
							true,
							axis.getName() + " " + direction.getName() + " course end"
						);
						assertConnection(
							helper,
							neighbourPos,
							alongProperty(direction.getOpposite()),
							true,
							axis.getName() + " reciprocal course end"
						);
						helper.setBlock(neighbourPos, Blocks.AIR);
						assertConnection(
							helper,
							CENTER,
							alongProperty(direction),
							false,
							axis.getName() + " removed course end"
						);
					}

					for (
						Direction direction :
							new Direction[] {
								perpendicularNegative,
								perpendicularPositive,
							}
					) {
						clearFixture(helper);
						setWall(helper, CENTER, centerBlock, axis);
						BlockPos neighbourPos = CENTER.relative(direction);
						setWall(
							helper,
							neighbourPos,
							neighbourBlock,
							perpendicularAxis
						);
						assertConnection(
							helper,
							CENTER,
							perpendicularProperty(direction),
							true,
							axis.getName() + " " + direction.getName() + " corner arm"
						);
						assertConnection(
							helper,
							neighbourPos,
							alongProperty(direction.getOpposite()),
							true,
							axis.getName() + " reciprocal perpendicular end"
						);
						assertShapeMatchesState(helper, CENTER);
						helper.setBlock(neighbourPos, Blocks.AIR);
						assertConnection(
							helper,
							CENTER,
							perpendicularProperty(direction),
							false,
							axis.getName() + " removed corner arm"
						);
					}
				}
			}
		}

		clearFixture(helper);
		setWall(
			helper,
			CENTER,
			ModTimberBlocks.CONNECTED_CHESTNUT_LOG_WALL,
			Direction.Axis.X
		);
		setWall(
			helper,
			CENTER.west(),
			ModTimberBlocks.CHESTNUT_LOG_DOOR_TERMINATION,
			Direction.Axis.X
		);
		setWall(
			helper,
			CENTER.east(),
			ModTimberBlocks.CHESTNUT_LOG_WINDOW_TERMINATION,
			Direction.Axis.X
		);
		setWall(
			helper,
			CENTER.north(),
			ModTimberBlocks.CHESTNUT_LOG_WINDOW_TERMINATION,
			Direction.Axis.Z
		);
		setWall(
			helper,
			CENTER.south(),
			ModTimberBlocks.CHESTNUT_LOG_DOOR_TERMINATION,
			Direction.Axis.Z
		);
		BlockState cross = helper.getBlockState(CENTER);
		for (BooleanProperty property : connectionProperties()) {
			helper.assertTrue(
				cross.getValue(property),
				"four-way wall cross must set " + property.getName()
			);
		}
		assertShapeMatchesState(helper, CENTER);
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void wallRotationMirrorAndCollisionMatchEverySupportedAxis(
		GameTestHelper helper
	) {
		for (ConnectedChestnutLogWallBlock wall : WALL_FORMS) {
			for (Direction.Axis axis : horizontalAxes()) {
				for (Course course : Course.values()) {
					for (int mask = 0; mask < 4; mask++) {
						BlockState state = wall
							.defaultBlockState()
							.setValue(ConnectedChestnutLogWallBlock.AXIS, axis)
							.setValue(ConnectedChestnutLogWallBlock.COURSE, course)
							.setValue(
								ConnectedChestnutLogWallBlock.NEGATIVE_PERPENDICULAR_CONNECTED,
								(mask & 1) != 0
							)
							.setValue(
								ConnectedChestnutLogWallBlock.POSITIVE_PERPENDICULAR_CONNECTED,
								(mask & 2) != 0
							);
						assertShapeMatchesState(helper, state, CENTER);
					}
				}
			}
		}

		for (Direction.Axis axis : horizontalAxes()) {
			BlockState asymmetric = ModTimberBlocks.CONNECTED_CHESTNUT_LOG_WALL
				.defaultBlockState()
				.setValue(ConnectedChestnutLogWallBlock.AXIS, axis)
				.setValue(ConnectedChestnutLogWallBlock.NEGATIVE_CONNECTED, true)
				.setValue(ConnectedChestnutLogWallBlock.POSITIVE_CONNECTED, false)
				.setValue(
					ConnectedChestnutLogWallBlock.NEGATIVE_PERPENDICULAR_CONNECTED,
					false
				)
				.setValue(
					ConnectedChestnutLogWallBlock.POSITIVE_PERPENDICULAR_CONNECTED,
					true
				);
			for (Rotation rotation : Rotation.values()) {
				BlockState transformed = asymmetric.rotate(rotation);
				Set<Direction> expected = EnumSet.noneOf(Direction.class);
				for (Direction connectedDirection : connectedDirections(asymmetric)) {
					expected.add(rotation.rotate(connectedDirection));
				}
				helper.assertValueEqual(
					connectedDirections(transformed),
					expected,
					axis.getName() + " " + rotation.getSerializedName() + " connections"
				);
			}
			for (Mirror mirror : Mirror.values()) {
				BlockState transformed = asymmetric.mirror(mirror);
				Set<Direction> expected = EnumSet.noneOf(Direction.class);
				for (Direction connectedDirection : connectedDirections(asymmetric)) {
					expected.add(mirror.mirror(connectedDirection));
				}
				helper.assertValueEqual(
					connectedDirections(transformed),
					expected,
					axis.getName() + " " + mirror.getSerializedName() + " connections"
				);
			}
		}
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void stackedWallFormsAlternateCoursesThroughRealPlacement(
		GameTestHelper helper
	) {
		for (Direction.Axis axis : horizontalAxes()) {
			for (int offset = 0; offset < WALL_FORMS.length; offset++) {
				clearFixture(helper);
				Course previous = null;
				for (int height = 0; height < WALL_FORMS.length; height++) {
					ConnectedChestnutLogWallBlock wall = WALL_FORMS[
						(offset + height) % WALL_FORMS.length
					];
					BlockPos pos = CENTER.above(height);
					placeWall(helper, pos, wall, axis);
					BlockState placed = helper.getBlockState(pos);
					helper.assertValueEqual(
						placed.getValue(ConnectedChestnutLogWallBlock.AXIS),
						axis,
						axis.getName() + " placed wall axis"
					);
					Course course = placed.getValue(
						ConnectedChestnutLogWallBlock.COURSE
					);
					if (previous != null) {
						helper.assertValueEqual(
							course,
							previous.opposite(),
							axis.getName() + " stacked course " + height
						);
					}
					previous = course;
					assertShapeMatchesState(helper, pos);
				}
			}
		}

		clearFixture(helper);
		placeWall(
			helper,
			CENTER,
			ModTimberBlocks.CONNECTED_CHESTNUT_LOG_WALL,
			Direction.Axis.X
		);
		placeWall(
			helper,
			CENTER.above(),
			ModTimberBlocks.CHESTNUT_LOG_DOOR_TERMINATION,
			Direction.Axis.Z
		);
		BlockState resetAxisCourse = helper.getBlockState(CENTER.above());
		Course parityCourse = (helper.absolutePos(CENTER.above()).getY() & 1) == 0
			? Course.LOWER
			: Course.UPPER;
		helper.assertValueEqual(
			resetAxisCourse.getValue(ConnectedChestnutLogWallBlock.COURSE),
			parityCourse,
			"a different-axis wall must restart the parity course"
		);
		helper.succeed();
	}

	private static void setWall(
		GameTestHelper helper,
		BlockPos pos,
		ConnectedChestnutLogWallBlock wall,
		Direction.Axis axis
	) {
		helper.setBlock(
			pos,
			wall.defaultBlockState().setValue(
				ConnectedChestnutLogWallBlock.AXIS,
				axis
			)
		);
	}

	private static void placeWall(
		GameTestHelper helper,
		BlockPos pos,
		ConnectedChestnutLogWallBlock wall,
		Direction.Axis axis
	) {
		BlockPos absolutePos = helper.absolutePos(pos);
		Direction horizontalDirection = axis == Direction.Axis.X
			? Direction.EAST
			: Direction.NORTH;
		BlockHitResult hit = new BlockHitResult(
			Vec3.atCenterOf(absolutePos),
			Direction.UP,
			absolutePos,
			false
		);
		BlockPlaceContext context = new BlockPlaceContext(
			helper.getLevel(),
			null,
			InteractionHand.MAIN_HAND,
			wall.asItem().getDefaultInstance(),
			hit
		) {
			@Override
			public Direction getHorizontalDirection() {
				return horizontalDirection;
			}
		};
		((BlockItem) wall.asItem()).place(context);
		helper.assertBlockPresent(wall, pos);
	}

	private static void assertConnection(
		GameTestHelper helper,
		BlockPos pos,
		BooleanProperty property,
		boolean expected,
		String label
	) {
		helper.assertValueEqual(
			helper.getBlockState(pos).getValue(property),
			expected,
			label
		);
	}

	private static void assertShapeMatchesState(
		GameTestHelper helper,
		BlockPos relativePos
	) {
		assertShapeMatchesState(
			helper,
			helper.getBlockState(relativePos),
			relativePos
		);
	}

	private static void assertShapeMatchesState(
		GameTestHelper helper,
		BlockState state,
		BlockPos relativePos
	) {
		BlockPos absolutePos = helper.absolutePos(relativePos);
		VoxelShape outline = state.getShape(helper.getLevel(), absolutePos);
		VoxelShape collision = state.getCollisionShape(
			helper.getLevel(),
			absolutePos,
			CollisionContext.empty()
		);
		VoxelShape expected = expectedShape(state);
		helper.assertTrue(
			!Shapes.joinIsNotEmpty(outline, expected, BooleanOp.NOT_SAME),
			"outline must equal the visible clay, log, and corner-arm union"
		);
		helper.assertTrue(
			!Shapes.joinIsNotEmpty(collision, expected, BooleanOp.NOT_SAME),
			"collision must equal the visible clay, log, and corner-arm union"
		);
		helper.assertValueEqual(outline.bounds().minY, 0.0, "wall outline min Y");
		helper.assertValueEqual(outline.bounds().maxY, 1.0, "wall outline max Y");
	}

	private static VoxelShape expectedShape(BlockState state) {
		Direction.Axis axis = state.getValue(ConnectedChestnutLogWallBlock.AXIS);
		VoxelShape center = axis == Direction.Axis.X
			? Block.box(0.0, 0.0, 2.0, 16.0, 16.0, 14.0)
			: Block.box(2.0, 0.0, 0.0, 14.0, 16.0, 16.0);
		if (
			state.getValue(
				ConnectedChestnutLogWallBlock.NEGATIVE_PERPENDICULAR_CONNECTED
			)
		) {
			center = Shapes.or(
				center,
				axis == Direction.Axis.X
					? Block.box(2.0, 0.0, 0.0, 14.0, 16.0, 2.0)
					: Block.box(0.0, 0.0, 2.0, 2.0, 16.0, 14.0)
			);
		}
		if (
			state.getValue(
				ConnectedChestnutLogWallBlock.POSITIVE_PERPENDICULAR_CONNECTED
			)
		) {
			center = Shapes.or(
				center,
				axis == Direction.Axis.X
					? Block.box(2.0, 0.0, 14.0, 14.0, 16.0, 16.0)
					: Block.box(14.0, 0.0, 2.0, 16.0, 16.0, 14.0)
			);
		}
		return center;
	}

	private static Set<Direction> connectedDirections(BlockState state) {
		Direction.Axis axis = state.getValue(ConnectedChestnutLogWallBlock.AXIS);
		Direction negative = negativeDirection(axis);
		Direction perpendicularNegative = negativeDirection(perpendicularAxis(axis));
		Set<Direction> directions = EnumSet.noneOf(Direction.class);
		if (state.getValue(ConnectedChestnutLogWallBlock.NEGATIVE_CONNECTED)) {
			directions.add(negative);
		}
		if (state.getValue(ConnectedChestnutLogWallBlock.POSITIVE_CONNECTED)) {
			directions.add(negative.getOpposite());
		}
		if (
			state.getValue(
				ConnectedChestnutLogWallBlock.NEGATIVE_PERPENDICULAR_CONNECTED
			)
		) {
			directions.add(perpendicularNegative);
		}
		if (
			state.getValue(
				ConnectedChestnutLogWallBlock.POSITIVE_PERPENDICULAR_CONNECTED
			)
		) {
			directions.add(perpendicularNegative.getOpposite());
		}
		return directions;
	}

	private static BooleanProperty alongProperty(Direction direction) {
		return direction.getAxisDirection() == Direction.AxisDirection.NEGATIVE
			? ConnectedChestnutLogWallBlock.NEGATIVE_CONNECTED
			: ConnectedChestnutLogWallBlock.POSITIVE_CONNECTED;
	}

	private static BooleanProperty perpendicularProperty(Direction direction) {
		return direction.getAxisDirection() == Direction.AxisDirection.NEGATIVE
			? ConnectedChestnutLogWallBlock.NEGATIVE_PERPENDICULAR_CONNECTED
			: ConnectedChestnutLogWallBlock.POSITIVE_PERPENDICULAR_CONNECTED;
	}

	private static BooleanProperty[] connectionProperties() {
		return new BooleanProperty[] {
			ConnectedChestnutLogWallBlock.NEGATIVE_CONNECTED,
			ConnectedChestnutLogWallBlock.POSITIVE_CONNECTED,
			ConnectedChestnutLogWallBlock.NEGATIVE_PERPENDICULAR_CONNECTED,
			ConnectedChestnutLogWallBlock.POSITIVE_PERPENDICULAR_CONNECTED,
		};
	}

	private static Direction.Axis[] horizontalAxes() {
		return new Direction.Axis[] { Direction.Axis.X, Direction.Axis.Z };
	}

	private static Direction.Axis perpendicularAxis(Direction.Axis axis) {
		return axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
	}

	private static Direction negativeDirection(Direction.Axis axis) {
		return Direction.fromAxisAndDirection(
			axis,
			Direction.AxisDirection.NEGATIVE
		);
	}

	private static void clearFixture(GameTestHelper helper) {
		for (int height = 0; height < 4; height++) {
			helper.setBlock(CENTER.above(height), Blocks.AIR);
		}
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			helper.setBlock(CENTER.relative(direction), Blocks.AIR);
		}
	}

	public ConnectedChestnutLogWallGameTests() {
	}
}
