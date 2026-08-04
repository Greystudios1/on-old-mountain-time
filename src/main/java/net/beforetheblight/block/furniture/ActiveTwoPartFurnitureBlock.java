package net.beforetheblight.block.furniture;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

	/**
	 * Two-block treadle loom with a synchronized decorative working pose.
	 * The toggle moves the visible mechanism only; it intentionally has no
	 * inventory, recipe processing, or item output.
	 */
public final class ActiveTwoPartFurnitureBlock
	extends AbstractTwoPartFurnitureBlock {
	public static final MapCodec<ActiveTwoPartFurnitureBlock> CODEC =
		simpleCodec(ActiveTwoPartFurnitureBlock::new);
	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

	private static final VoxelShape LEFT_SHAPE = floorLoomShape(true);
	private static final VoxelShape RIGHT_SHAPE = floorLoomShape(false);

	public ActiveTwoPartFurnitureBlock(BlockBehaviour.Properties properties) {
		super(properties, LEFT_SHAPE, RIGHT_SHAPE, false);
		this.registerDefaultState(this.stateDefinition.any()
			.setValue(FACING, Direction.NORTH)
			.setValue(PART, FurniturePart.LEFT)
			.setValue(ACTIVE, false));
	}

	@Override
	public MapCodec<ActiveTwoPartFurnitureBlock> codec() {
		return CODEC;
	}

	@Override
	protected BlockState synchronizeFromPartner(
		BlockState state,
		BlockState partner
	) {
		return state.setValue(ACTIVE, partner.getValue(ACTIVE));
	}

	@Override
	protected InteractionResult useWithoutItem(
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		BlockHitResult hit
	) {
		BlockPos partnerPos = this.partnerPos(pos, state);
		BlockState partner = level.getBlockState(partnerPos);
		if (!this.isMatchingPartner(state, partner)) {
			return InteractionResult.CONSUME;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		BlockState current = level.getBlockState(pos);
		BlockPos currentPartnerPos = this.partnerPos(pos, current);
		BlockState currentPartner = level.getBlockState(currentPartnerPos);
		if (!current.is(this) || !this.isMatchingPartner(current, currentPartner)) {
			return InteractionResult.CONSUME;
		}
		boolean active = !current.getValue(ACTIVE);
		BlockState changed = current.setValue(ACTIVE, active);
		BlockState changedPartner = currentPartner.setValue(ACTIVE, active);
		if (!level.setBlock(pos, changed, Block.UPDATE_ALL)) {
			return InteractionResult.FAIL;
		}
		if (!level.setBlock(currentPartnerPos, changedPartner, Block.UPDATE_ALL)
			&& !level.getBlockState(currentPartnerPos).equals(changedPartner)) {
			level.setBlock(pos, current, Block.UPDATE_ALL);
			level.setBlock(currentPartnerPos, currentPartner, Block.UPDATE_ALL);
			return InteractionResult.FAIL;
		}
		level.playSound(
			null,
			pos,
			SoundEvents.WOOD_PLACE,
			SoundSource.BLOCKS,
			0.4F,
			active ? 1.15F : 0.85F
		);
		var event = active ? GameEvent.BLOCK_ACTIVATE : GameEvent.BLOCK_DEACTIVATE;
		level.gameEvent(event, pos, GameEvent.Context.of(player, changed));
		return InteractionResult.SUCCESS_SERVER;
	}

	@Override
	protected void createBlockStateDefinition(
		StateDefinition.Builder<Block, BlockState> builder
	) {
		super.createBlockStateDefinition(builder);
		builder.add(ACTIVE);
	}

	private static VoxelShape floorLoomShape(boolean left) {
		double outerMinX = left ? 1 : 13;
		double outerMaxX = left ? 3 : 15;
		double spanMinX = left ? 2 : 0;
		double spanMaxX = left ? 16 : 14;
		return Shapes.or(
			// Four global outside posts and their two depth rails.
			Block.box(outerMinX, 0.25, 2, outerMaxX, 16, 4),
			Block.box(outerMinX, 0.25, 12, outerMaxX, 16, 14),
			Block.box(outerMinX + 0.5, 4.5, 3, outerMaxX + 0.25, 6, 13),
			// Continuous top, warp, breast, and beater members.
			Block.box(spanMinX, 13.5, 2.5, spanMaxX, 15, 4.5),
			Block.box(spanMinX, 13.5, 11.5, spanMaxX, 15, 13.5),
			Block.box(spanMinX, 10, 10.75, spanMaxX, 12, 12.25),
			Block.box(spanMinX, 7.25, 3.75, spanMaxX, 9.6, 6.5),
			Block.box(spanMinX + 0.5, 6.75, 5.5, spanMaxX - 0.5, 13, 6.5),
			// Low treadles remain non-stateful collision despite the visual pose.
			Block.box(5, 0.5, 4, 7, 2, 11.5),
			Block.box(10, 0.5, 4, 12, 2, 11.5)
		);
	}
}
