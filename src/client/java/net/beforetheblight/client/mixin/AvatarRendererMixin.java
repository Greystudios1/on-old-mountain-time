package net.beforetheblight.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.beforetheblight.client.render.RockingChairRenderTransform;
import net.beforetheblight.client.render.RockingChairRiderRenderState;
import net.beforetheblight.entity.RockingChairSeatEntity;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tilts only the third-person avatar model around its seated pelvis. The
 * player's actual yaw, pitch, and first-person camera are never modified.
 */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
	@Inject(
		method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
		at = @At("TAIL")
	)
	private void beforeTheBlight$extractChairRock(
		Avatar avatar,
		AvatarRenderState state,
		float partialTick,
		CallbackInfo callbackInfo
	) {
		RockingChairRiderRenderState rockingState =
			(RockingChairRiderRenderState)(Object)state;
		if (avatar.getVehicle() instanceof RockingChairSeatEntity seat) {
			Direction facing = seat.chairFacing();
			float headWorldRotation = state.bodyRot + state.yRot;
			state.bodyRot = facing.toYRot();
			state.yRot = Mth.clamp(
				Mth.wrapDegrees(headWorldRotation - state.bodyRot),
				-85.0F,
				85.0F
			);
			rockingState.beforeTheBlight$setRockingChair(
				facing,
				seat.rockingAngle(partialTick)
			);
		} else {
			rockingState.beforeTheBlight$clearRockingChair();
		}
	}

	@Inject(
		method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V",
		at = @At("HEAD")
	)
	private void beforeTheBlight$applyChairRock(
		AvatarRenderState state,
		PoseStack poseStack,
		float bodyRotation,
		float entityScale,
		CallbackInfo callbackInfo
	) {
		RockingChairRiderRenderState rockingState =
			(RockingChairRiderRenderState)(Object)state;
		if (!rockingState.beforeTheBlight$isInRockingChair()) {
			return;
		}

		RockingChairRenderTransform.applyAroundPivot(
			poseStack,
			rockingState.beforeTheBlight$getRockingChairFacing(),
			rockingState.beforeTheBlight$getRockingChairAngle(),
			Avatar.DEFAULT_VEHICLE_ATTACHMENT.y
		);
	}
}
