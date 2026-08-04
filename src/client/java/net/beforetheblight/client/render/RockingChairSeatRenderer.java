package net.beforetheblight.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.beforetheblight.block.RockingChairBlock;
import net.beforetheblight.entity.RockingChairSeatEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Reuses the chair's ordinary baked block model, but tilts it around the
 * rocker contact point while occupied. No custom texture atlas or rendering
 * dependency is required.
 */
public final class RockingChairSeatRenderer
	extends EntityRenderer<RockingChairSeatEntity, RockingChairSeatRenderState> {
	private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();

	private final BlockModelResolver blockModelResolver;

	public RockingChairSeatRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = 0.0F;
		this.blockModelResolver = context.getBlockModelResolver();
	}

	@Override
	public RockingChairSeatRenderState createRenderState() {
		return new RockingChairSeatRenderState();
	}

	@Override
	public void extractRenderState(
		RockingChairSeatEntity entity,
		RockingChairSeatRenderState state,
		float partialTick
	) {
		super.extractRenderState(entity, state, partialTick);
		BlockState blockState = entity.level().getBlockState(entity.anchorPos());
		state.hasChair = blockState.getBlock() instanceof RockingChairBlock;
		if (!state.hasChair) {
			state.chairModel.clear();
			return;
		}

		state.facing = blockState.getValue(RockingChairBlock.FACING);
		state.rockingAngle = entity.rockingAngle(partialTick);
		this.blockModelResolver.update(
			state.chairModel,
			blockState.setValue(RockingChairBlock.OCCUPIED, false),
			DISPLAY_CONTEXT
		);
	}

	@Override
	public void submit(
		RockingChairSeatRenderState state,
		PoseStack poseStack,
		SubmitNodeCollector submitNodeCollector,
		CameraRenderState camera
	) {
		if (state.hasChair && !state.chairModel.isEmpty()) {
			poseStack.pushPose();
			RockingChairRenderTransform.applyAroundPivot(
				poseStack,
				state.facing,
				state.rockingAngle,
				RockingChairSeatEntity.ROCKING_PIVOT_HEIGHT
			);
			poseStack.translate(-0.5F, 0.0F, -0.5F);
			state.chairModel.submit(
				poseStack,
				submitNodeCollector,
				state.lightCoords,
				OverlayTexture.NO_OVERLAY,
				state.outlineColor
			);
			poseStack.popPose();
		}
		super.submit(state, poseStack, submitNodeCollector, camera);
	}

	@Override
	protected AABB getBoundingBoxForCulling(RockingChairSeatEntity entity) {
		return new AABB(
			entity.getX() - 0.75D,
			entity.getY(),
			entity.getZ() - 0.75D,
			entity.getX() + 0.75D,
			entity.getY() + 1.75D,
			entity.getZ() + 0.75D
		);
	}
}
