package net.beforetheblight.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;

/**
 * One facing-aware rocking transform shared by the occupied chair and its
 * third-person rider. The signs deliberately preserve the existing occupied
 * chair animation exactly.
 */
public final class RockingChairRenderTransform {
	private RockingChairRenderTransform() {
	}

	public static void applyAroundPivot(
		PoseStack poseStack,
		Direction facing,
		float angle,
		double pivotHeight
	) {
		poseStack.translate(0.0D, pivotHeight, 0.0D);
		applyRotation(poseStack, facing, angle);
		poseStack.translate(0.0D, -pivotHeight, 0.0D);
	}

	public static void applyRotation(
		PoseStack poseStack,
		Direction facing,
		float angle
	) {
		switch (facing) {
			case NORTH -> poseStack.mulPose(Axis.XP.rotationDegrees(angle));
			case SOUTH -> poseStack.mulPose(Axis.XP.rotationDegrees(-angle));
			case EAST -> poseStack.mulPose(Axis.ZP.rotationDegrees(-angle));
			case WEST -> poseStack.mulPose(Axis.ZP.rotationDegrees(angle));
			default -> {
			}
		}
	}
}
