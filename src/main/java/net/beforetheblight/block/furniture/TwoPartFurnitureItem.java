package net.beforetheblight.block.furniture;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Placement item for two-part furniture.
 *
 * <p>Vanilla's ordinary BlockItem notifies neighbours before
 * {@link AbstractTwoPartFurnitureBlock#setPlacedBy} can create the partner,
 * allowing the primary half to delete itself as an orphan. The same placement
 * flags used by BedItem defer that destructive neighbour validation until both
 * halves exist.</p>
 */
public final class TwoPartFurnitureItem extends BlockItem {
	public TwoPartFurnitureItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	protected boolean placeBlock(
		BlockPlaceContext context,
		BlockState placementState
	) {
		return context.getLevel().setBlock(
			context.getClickedPos(),
			placementState,
			Block.UPDATE_CLIENTS
				| Block.UPDATE_IMMEDIATE
				| Block.UPDATE_KNOWN_SHAPE
		);
	}
}
