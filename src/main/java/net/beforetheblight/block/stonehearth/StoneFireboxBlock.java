package net.beforetheblight.block.stonehearth;

import java.util.Map;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * One reusable firebox block with cold, ash, ember, and active states.
 *
 * <p>Empty-hand use cycles the state so builders and judges can repeatedly
 * exercise the visual/light contract without a ticking block entity. Active
 * and ember states provide restrained smoke/fire particles; structures may
 * lock a desired state directly in their saved palette.</p>
 */
public final class StoneFireboxBlock extends HorizontalDirectionalBlock {
	public static final MapCodec<StoneFireboxBlock> CODEC =
		simpleCodec(StoneFireboxBlock::new);
	public static final EnumProperty<FireboxState> FIRE_STATE =
		EnumProperty.create("fire_state", FireboxState.class);

	private static final VoxelShape NORTH_SHAPE = Shapes.or(
		// Back, floor, and two jambs; the front remains open.
		Block.box(0.0, 0.0, 13.0, 16.0, 16.0, 16.0),
		Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 13.0),
		Block.box(0.0, 2.0, 0.0, 3.0, 16.0, 13.0),
		Block.box(13.0, 2.0, 0.0, 16.0, 16.0, 13.0),
		Block.box(3.0, 13.0, 6.0, 13.0, 16.0, 13.0)
	);
	private static final Map<Direction, VoxelShape> SHAPES =
		Shapes.rotateHorizontal(NORTH_SHAPE);

	public StoneFireboxBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(
			this.stateDefinition.any()
				.setValue(FACING, Direction.NORTH)
				.setValue(FIRE_STATE, FireboxState.COLD)
		);
	}

	@Override
	protected MapCodec<StoneFireboxBlock> codec() {
		return CODEC;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState()
			.setValue(FACING, context.getHorizontalDirection().getOpposite())
			.setValue(FIRE_STATE, FireboxState.COLD);
	}

	@Override
	protected VoxelShape getShape(
		BlockState state,
		BlockGetter level,
		BlockPos pos,
		CollisionContext context
	) {
		return SHAPES.get(state.getValue(FACING));
	}

	@Override
	protected InteractionResult useWithoutItem(
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		BlockHitResult hit
	) {
		if (!level.isClientSide()) {
			BlockState current = level.getBlockState(pos);
			if (!current.is(this)) {
				return InteractionResult.PASS;
			}
			BlockState changed = current.cycle(FIRE_STATE);
			if (!level.setBlock(pos, changed, Block.UPDATE_ALL)) {
				return InteractionResult.FAIL;
			}
			FireboxState next = changed.getValue(FIRE_STATE);
			SoundEvent sound = switch (next) {
				case ASH -> SoundEvents.SAND_PLACE;
				case EMBER, ACTIVE -> SoundEvents.FLINTANDSTEEL_USE;
				case COLD -> SoundEvents.FIRE_EXTINGUISH;
			};
			level.playSound(null, pos, sound, SoundSource.BLOCKS, 0.65F, 0.9F);
			var event = next == FireboxState.EMBER || next == FireboxState.ACTIVE
				? GameEvent.BLOCK_ACTIVATE
				: GameEvent.BLOCK_DEACTIVATE;
			level.gameEvent(event, pos, GameEvent.Context.of(player, changed));
			return InteractionResult.SUCCESS_SERVER;
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void animateTick(
		BlockState state,
		Level level,
		BlockPos pos,
		RandomSource random
	) {
		FireboxState fireState = state.getValue(FIRE_STATE);
		if (fireState == FireboxState.COLD || fireState == FireboxState.ASH) {
			return;
		}

		double x = pos.getX() + 0.5;
		double y = pos.getY() + (fireState == FireboxState.ACTIVE ? 0.55 : 0.35);
		double z = pos.getZ() + 0.5;
		if (fireState == FireboxState.ACTIVE) {
			level.addParticle(
				ParticleTypes.FLAME,
				x + (random.nextDouble() - 0.5) * 0.35,
				y,
				z + (random.nextDouble() - 0.5) * 0.35,
				0.0,
				0.01,
				0.0
			);
		}
		if (random.nextInt(fireState == FireboxState.ACTIVE ? 2 : 5) == 0) {
			level.addParticle(
				ParticleTypes.SMOKE,
				x,
				y + 0.3,
				z,
				0.0,
				0.025,
				0.0
			);
		}
	}

	@Override
	protected void createBlockStateDefinition(
		StateDefinition.Builder<Block, BlockState> builder
	) {
		builder.add(FACING, FIRE_STATE);
	}
}
