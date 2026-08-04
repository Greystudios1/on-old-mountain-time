package net.beforetheblight.block.domestic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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

/**
 * A low domestic lamp or candle with a persistent lit state.
 *
 * <p>Empty-hand use toggles the state, sound, light emission, and game event.
 * It intentionally has no fuel inventory, block entity, or ticking behavior.</p>
 */
public final class DomesticLightBlock extends HorizontalPropBlock {
	public static final MapCodec<DomesticLightBlock> CODEC =
		RecordCodecBuilder.mapCodec(instance -> instance.group(
			Profile.CODEC.fieldOf("profile").forGetter(DomesticLightBlock::profile),
			Codec.intRange(0, 15).fieldOf("light_level")
				.forGetter(DomesticLightBlock::lightLevel),
			propertiesCodec()
		).apply(instance, DomesticLightBlock::new));
	public static final BooleanProperty LIT = BooleanProperty.create("lit");

	private final int lightLevel;

	public DomesticLightBlock(
		Profile profile,
		int lightLevel,
		BlockBehaviour.Properties properties
	) {
		super(profile, withLightEmission(properties, lightLevel));
		this.lightLevel = lightLevel;
		this.registerDefaultState(
			this.stateDefinition.any()
				.setValue(FACING, Direction.NORTH)
				.setValue(LIT, false)
		);
	}

	private static BlockBehaviour.Properties withLightEmission(
		BlockBehaviour.Properties properties,
		int lightLevel
	) {
		if (lightLevel < 0 || lightLevel > 15) {
			throw new IllegalArgumentException(
				"Domestic light level must be between 0 and 15"
			);
		}
		return properties.lightLevel(
			state -> state.getValue(LIT) ? lightLevel : 0
		);
	}

	@Override
	public MapCodec<DomesticLightBlock> codec() {
		return CODEC;
	}

	public int lightLevel() {
		return this.lightLevel;
	}

	@Override
	protected InteractionResult useWithoutItem(
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		BlockHitResult hit
	) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		boolean lit = !state.getValue(LIT);
		level.setBlock(pos, state.setValue(LIT, lit), Block.UPDATE_ALL);
		level.playSound(
			null,
			pos,
			lit ? SoundEvents.FLINTANDSTEEL_USE : SoundEvents.CANDLE_EXTINGUISH,
			SoundSource.BLOCKS,
			0.55F,
			lit ? 1.05F : 0.95F
		);
		level.gameEvent(
			player,
			lit ? GameEvent.BLOCK_ACTIVATE : GameEvent.BLOCK_DEACTIVATE,
			pos
		);
		return InteractionResult.SUCCESS_SERVER;
	}

	@Override
	protected void createBlockStateDefinition(
		StateDefinition.Builder<Block, BlockState> builder
	) {
		super.createBlockStateDefinition(builder);
		builder.add(LIT);
	}
}
