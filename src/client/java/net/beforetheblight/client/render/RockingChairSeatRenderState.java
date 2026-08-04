package net.beforetheblight.client.render;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.Direction;

/** Extracted data used by {@link RockingChairSeatRenderer}. */
public final class RockingChairSeatRenderState extends EntityRenderState {
	public final BlockModelRenderState chairModel = new BlockModelRenderState();
	public Direction facing = Direction.NORTH;
	public float rockingAngle;
	public boolean hasChair;
}
