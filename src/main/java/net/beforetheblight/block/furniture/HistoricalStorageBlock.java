package net.beforetheblight.block.furniture;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Historical storage furniture backed by vanilla's 27-slot barrel contract.
 *
 * <p>Every style receives persistence, loot-table support, open-count
 * synchronization, comparator output, menus, and multiplayer-safe access from
 * the established barrel implementation.</p>
 */
public final class HistoricalStorageBlock extends BarrelBlock {
	public enum Style {
		BLANKET_CHEST(
			"blanket_chest",
			blanketChestClosed(),
			blanketChestOpen(),
			false
		),
		WALL_CUPBOARD(
			"wall_cupboard",
			wallCupboardClosed(),
			wallCupboardOpen(),
			true
		),
		CORNER_CUPBOARD(
			"corner_cupboard",
			cornerCupboardClosed(),
			cornerCupboardOpen(),
			false
		),
		PIE_SAFE("pie_safe", pieSafeClosed(), pieSafeOpen(), false),
		DRAWER_CHEST(
			"drawer_chest",
			drawerChestClosed(),
			drawerChestOpen(),
			false
		);

		private final String id;
		private final Map<Direction, VoxelShape> closedShapes;
		private final Map<Direction, VoxelShape> openShapes;
		private final boolean wallMounted;

		Style(
			String id,
			VoxelShape northClosedShape,
			VoxelShape northOpenShape,
			boolean wallMounted
		) {
			this.id = id;
			this.closedShapes = Shapes.rotateHorizontal(northClosedShape);
			this.openShapes = Shapes.rotateHorizontal(northOpenShape);
			this.wallMounted = wallMounted;
		}

		public String id() {
			return this.id;
		}

		public String containerTranslationKey() {
			return "container.before_the_blight." + this.id;
		}
	}

	private final Style style;

	public HistoricalStorageBlock(
		Style style,
		BlockBehaviour.Properties properties
	) {
		super(properties);
		this.style = style;
	}

	public Style style() {
		return this.style;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		if (!this.style.wallMounted) {
			BlockState state = this.defaultBlockState()
				.setValue(FACING, context.getHorizontalDirection().getOpposite())
				.setValue(OPEN, false);
			return state.canSurvive(
				context.getLevel(),
				context.getClickedPos()
			) ? state : null;
		}

		for (Direction direction : context.getNearestLookingDirections()) {
			if (!direction.getAxis().isHorizontal()) {
				continue;
			}
			BlockState state = this.defaultBlockState()
				.setValue(FACING, direction.getOpposite())
				.setValue(OPEN, false);
			if (state.canSurvive(context.getLevel(), context.getClickedPos())) {
				return state;
			}
		}
		return null;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		if (!this.style.wallMounted) {
			BlockPos below = pos.below();
			return level.getBlockState(below).isFaceSturdy(
				level,
				below,
				Direction.UP
			);
		}
		Direction facing = state.getValue(FACING);
		BlockPos supportPos = pos.relative(facing.getOpposite());
		return level.getBlockState(supportPos).isFaceSturdy(
			level,
			supportPos,
			facing
		);
	}

	@Override
	protected void neighborChanged(
		BlockState state,
		Level level,
		BlockPos pos,
		net.minecraft.world.level.block.Block neighbourBlock,
		@Nullable Orientation orientation,
		boolean movedByPiston
	) {
		if (!state.canSurvive(level, pos)) {
			// destroyBlock uses the normal container drop path; returning AIR
			// from updateShape could silently discard an inventory.
			level.destroyBlock(pos, true);
			return;
		}
		super.neighborChanged(
			state,
			level,
			pos,
			neighbourBlock,
			orientation,
			movedByPiston
		);
	}

	@Override
	protected VoxelShape getShape(
		BlockState state,
		BlockGetter level,
		BlockPos pos,
		CollisionContext context
	) {
		Map<Direction, VoxelShape> shapes = state.getValue(OPEN)
			? this.style.openShapes
			: this.style.closedShapes;
		Direction facing = state.getValue(FACING);
		return shapes.getOrDefault(facing, shapes.get(Direction.NORTH));
	}

	@Override
	public HistoricalStorageBlockEntity newBlockEntity(
		BlockPos pos,
		BlockState state
	) {
		return new HistoricalStorageBlockEntity(pos, state);
	}

	private static VoxelShape blanketChestClosed() {
		return Shapes.or(
			net.minecraft.world.level.block.Block.box(0.75, 0, 1.75, 15.25, 11, 14.25),
			net.minecraft.world.level.block.Block.box(0.5, 10, 1.5, 15.5, 13, 14.5)
		);
	}

	private static VoxelShape blanketChestOpen() {
		return Shapes.or(
			net.minecraft.world.level.block.Block.box(0.75, 0, 1.75, 15.25, 11, 14.25),
			net.minecraft.world.level.block.Block.box(0.5, 10, 11, 15.5, 16, 15)
		);
	}

	private static VoxelShape wallCupboardClosed() {
		return Shapes.or(
			net.minecraft.world.level.block.Block.box(1, 2, 9, 15, 16, 16),
			net.minecraft.world.level.block.Block.box(0.5, 1.5, 8.5, 2, 16, 16),
			net.minecraft.world.level.block.Block.box(14, 1.5, 8.5, 15.5, 16, 16)
		);
	}

	private static VoxelShape wallCupboardOpen() {
		return Shapes.or(
			net.minecraft.world.level.block.Block.box(1, 2, 12, 15, 16, 16),
			net.minecraft.world.level.block.Block.box(0, 2, 6, 2, 16, 14),
			net.minecraft.world.level.block.Block.box(14, 2, 6, 16, 16, 14)
		);
	}

	private static VoxelShape cornerCupboardClosed() {
		return Shapes.or(
			net.minecraft.world.level.block.Block.box(3, 0, 3, 16, 16, 16),
			net.minecraft.world.level.block.Block.box(1, 0, 13, 3, 16, 16),
			net.minecraft.world.level.block.Block.box(13, 0, 1, 16, 16, 3)
		);
	}

	private static VoxelShape cornerCupboardOpen() {
		return Shapes.or(
			net.minecraft.world.level.block.Block.box(5, 0, 5, 16, 16, 16),
			net.minecraft.world.level.block.Block.box(1, 0, 10, 4, 16, 16)
		);
	}

	private static VoxelShape pieSafeClosed() {
		return Shapes.or(
			net.minecraft.world.level.block.Block.box(1, 0, 2, 15, 16, 14),
			net.minecraft.world.level.block.Block.box(0.5, 1, 1.5, 2, 15, 3),
			net.minecraft.world.level.block.Block.box(14, 1, 1.5, 15.5, 15, 3)
		);
	}

	private static VoxelShape pieSafeOpen() {
		return Shapes.or(
			net.minecraft.world.level.block.Block.box(1, 0, 8, 15, 16, 14),
			net.minecraft.world.level.block.Block.box(0, 1, 2, 2, 15, 12),
			net.minecraft.world.level.block.Block.box(14, 1, 2, 16, 15, 12)
		);
	}

	private static VoxelShape drawerChestClosed() {
		return net.minecraft.world.level.block.Block.box(1, 0, 2, 15, 14, 14);
	}

	private static VoxelShape drawerChestOpen() {
		return Shapes.or(
			net.minecraft.world.level.block.Block.box(1, 0, 5, 15, 14, 14),
			net.minecraft.world.level.block.Block.box(2, 3, 0, 14, 7, 7),
			net.minecraft.world.level.block.Block.box(2, 9, 1, 14, 13, 7)
		);
	}
}
