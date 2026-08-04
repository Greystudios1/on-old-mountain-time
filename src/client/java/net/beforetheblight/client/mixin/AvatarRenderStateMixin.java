package net.beforetheblight.client.mixin;

import net.beforetheblight.client.render.RockingChairRiderRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public abstract class AvatarRenderStateMixin implements RockingChairRiderRenderState {
	@Unique
	private boolean beforeTheBlight$inRockingChair;
	@Unique
	private float beforeTheBlight$rockingChairAngle;
	@Unique
	private Direction beforeTheBlight$rockingChairFacing = Direction.NORTH;

	@Override
	public void beforeTheBlight$setRockingChair(
		Direction facing,
		float angle
	) {
		this.beforeTheBlight$inRockingChair = true;
		this.beforeTheBlight$rockingChairFacing = facing;
		this.beforeTheBlight$rockingChairAngle = angle;
	}

	@Override
	public float beforeTheBlight$getRockingChairAngle() {
		return this.beforeTheBlight$rockingChairAngle;
	}

	@Override
	public Direction beforeTheBlight$getRockingChairFacing() {
		return this.beforeTheBlight$rockingChairFacing;
	}

	@Override
	public boolean beforeTheBlight$isInRockingChair() {
		return this.beforeTheBlight$inRockingChair;
	}

	@Override
	public void beforeTheBlight$clearRockingChair() {
		this.beforeTheBlight$inRockingChair = false;
		this.beforeTheBlight$rockingChairFacing = Direction.NORTH;
		this.beforeTheBlight$rockingChairAngle = 0.0F;
	}
}
