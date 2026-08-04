package net.beforetheblight.block.furniture;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Shared one-item/two-block furniture placement and lifecycle.
 *
 * <p>The clicked block is {@code part=left}; the partner is placed to the
 * furniture's right. Either missing support or a missing/mismatched partner
 * removes the other half through ordinary neighbour updates. Piston movement
 * is blocked by registry properties, preventing a machine from splitting the
 * object.</p>
 */
public abstract class AbstractTwoPartFurnitureBlock
	extends DirectionalFurnitureBlock {
	public static final EnumProperty<FurniturePart> PART =
		EnumProperty.create("part", FurniturePart.class);

	private final Map<Direction, VoxelShape> leftShapes;
	private final Map<Direction, VoxelShape> rightShapes;
	private final boolean wallMounted;

	protected AbstractTwoPartFurnitureBlock(
		BlockBehaviour.Properties properties,
		VoxelShape northLeftShape,
		VoxelShape northRightShape,
		boolean wallMounted
	) {
		super(properties, northLeftShape);
		this.leftShapes = Shapes.rotateHorizontal(northLeftShape);
		this.rightShapes = Shapes.rotateHorizontal(northRightShape);
		this.wallMounted = wallMounted;
		this.registerDefaultState(this.stateDefinition.any()
			.setValue(FACING, Direction.NORTH)
			.setValue(PART, FurniturePart.LEFT));
	}

	protected static Direction partnerDirection(BlockState state) {
		Direction facing = state.getValue(FACING);
		return state.getValue(PART) == FurniturePart.LEFT
			? facing.getClockWise()
			: facing.getCounterClockWise();
	}

	protected BlockPos partnerPos(BlockPos pos, BlockState state) {
		return pos.relative(partnerDirection(state));
	}

	protected boolean isMatchingPartner(BlockState state, BlockState partner) {
		return partner.is(this)
			&& partner.getValue(FACING) == state.getValue(FACING)
			&& partner.getValue(PART) != state.getValue(PART);
	}

	/**
	 * Subclasses with shared state such as {@code active} or {@code open} copy
	 * that value from the surviving partner here.
	 */
	protected BlockState synchronizeFromPartner(
		BlockState state,
		BlockState partner
	) {
		return state;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction facing = context.getHorizontalDirection().getOpposite();
		BlockPos pos = context.getClickedPos();
		BlockState state = this.defaultBlockState()
			.setValue(FACING, facing)
			.setValue(PART, FurniturePart.LEFT);
		BlockPos partnerPos = pos.relative(facing.getClockWise());
		Level level = context.getLevel();
		BlockState partnerState = state.setValue(PART, FurniturePart.RIGHT);
		BlockPlaceContext partnerContext = BlockPlaceContext.at(
			context,
			partnerPos,
			facing.getClockWise()
		);

		if (!level.getWorldBorder().isWithinBounds(partnerPos)
			|| !level.getBlockState(partnerPos).canBeReplaced(partnerContext)
			|| !state.canSurvive(level, pos)
			|| !partnerState.canSurvive(level, partnerPos)
			|| !level.isUnobstructed(
				partnerState,
				partnerPos,
				CollisionContext.placementContext(context.getPlayer())
			)) {
			return null;
		}
		return state;
	}

	@Override
	public void setPlacedBy(
		Level level,
		BlockPos pos,
		BlockState state,
		@Nullable LivingEntity by,
		ItemStack itemStack
	) {
		super.setPlacedBy(level, pos, state, by, itemStack);
		if (!level.isClientSide()) {
			BlockPos partnerPos = this.partnerPos(pos, state);
			boolean placedPartner = level.setBlock(
				partnerPos,
				state.setValue(PART, FurniturePart.RIGHT),
				Block.UPDATE_ALL
			);
			if (!placedPartner) {
				level.setBlock(
					pos,
					Blocks.AIR.defaultBlockState(),
					Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS
				);
				return;
			}
			level.updateNeighborsAt(pos, Blocks.AIR);
			state.updateNeighbourShapes(level, pos, Block.UPDATE_ALL);
		}
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		if (this.wallMounted) {
			Direction facing = state.getValue(FACING);
			BlockPos supportPos = pos.relative(facing.getOpposite());
			return level.getBlockState(supportPos).isFaceSturdy(
				level,
				supportPos,
				facing
			);
		}

		BlockPos below = pos.below();
		return level.getBlockState(below).isFaceSturdy(
			level,
			below,
			Direction.UP
		);
	}

	@Override
	protected BlockState updateShape(
		BlockState state,
		LevelReader level,
		ScheduledTickAccess ticks,
		BlockPos pos,
		Direction directionToNeighbour,
		BlockPos neighbourPos,
		BlockState neighbourState,
		RandomSource random
	) {
		if (directionToNeighbour == partnerDirection(state)) {
			return this.isMatchingPartner(state, neighbourState)
				? this.synchronizeFromPartner(state, neighbourState)
				: Blocks.AIR.defaultBlockState();
		}

		Direction supportDirection = this.wallMounted
			? state.getValue(FACING).getOpposite()
			: Direction.DOWN;
		if (directionToNeighbour == supportDirection
			&& !state.canSurvive(level, pos)) {
			return Blocks.AIR.defaultBlockState();
		}

		return super.updateShape(
			state,
			level,
			ticks,
			pos,
			directionToNeighbour,
			neighbourPos,
			neighbourState,
			random
		);
	}

	@Override
	public BlockState playerWillDestroy(
		Level level,
		BlockPos pos,
		BlockState state,
		Player player
	) {
		// In creative, remove the partner explicitly because ordinary creative
		// destruction bypasses survival drops and some neighbour cleanup.
		if (!level.isClientSide() && player.preventsBlockDrops()) {
			BlockPos partnerPos = this.partnerPos(pos, state);
			BlockState partner = level.getBlockState(partnerPos);
			if (this.isMatchingPartner(state, partner)) {
				level.setBlock(partnerPos, Blocks.AIR.defaultBlockState(), 35);
				level.levelEvent(player, 2001, partnerPos, Block.getId(partner));
			}
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	protected VoxelShape getShape(
		BlockState state,
		BlockGetter level,
		BlockPos pos,
		CollisionContext context
	) {
		Map<Direction, VoxelShape> shapes =
			state.getValue(PART) == FurniturePart.LEFT
				? this.leftShapes
				: this.rightShapes;
		return shapes.get(state.getValue(FACING));
	}

	@Override
	protected void createBlockStateDefinition(
		StateDefinition.Builder<Block, BlockState> builder
	) {
		super.createBlockStateDefinition(builder);
		builder.add(PART);
	}
}
