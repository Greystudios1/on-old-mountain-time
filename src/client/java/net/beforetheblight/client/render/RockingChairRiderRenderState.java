package net.beforetheblight.client.render;

import net.minecraft.core.Direction;

/**
 * Client-only extension mixed into vanilla avatar render state so a seated
 * player can inherit the chair's visual tilt without changing camera pitch.
 */
public interface RockingChairRiderRenderState {
	void beforeTheBlight$setRockingChair(
		Direction facing,
		float angle
	);

	float beforeTheBlight$getRockingChairAngle();

	Direction beforeTheBlight$getRockingChairFacing();

	boolean beforeTheBlight$isInRockingChair();

	void beforeTheBlight$clearRockingChair();
}
