package net.beforetheblight.gametest;

import java.util.HashSet;

import net.beforetheblight.block.BushelBasketBlock;
import net.beforetheblight.block.CornBinBlock;
import net.beforetheblight.block.HandCornShellerBlock;
import net.beforetheblight.block.LayeredEarCornPileBlock;
import net.beforetheblight.block.LayeredEarCornPileBlock.Fullness;
import net.beforetheblight.block.LayeredEarCornPileBlock.PileShape;
import net.beforetheblight.block.WideSetCribWallBlock;
import net.beforetheblight.registry.ModCornCribBlocks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

/** Focused registration and persistent-state contracts for the corn-crib slice. */
public final class CornCribContentGameTests {
	@GameTest(maxTicks = 20)
	public void everyObtainableCornCribBlockHasOneMappedBlockItem(GameTestHelper helper) {
		helper.assertValueEqual(ModCornCribBlocks.ALL_BLOCKS.size(), 11, "corn-crib block count");
		helper.assertValueEqual(ModCornCribBlocks.ALL_ITEMS.size(), 11, "corn-crib item count");
		helper.assertValueEqual(
			new HashSet<>(ModCornCribBlocks.ALL_ITEMS).size(),
			11,
			"unique corn-crib item count"
		);
		for (Block block : ModCornCribBlocks.ALL_BLOCKS) {
			Item item = block.asItem();
			helper.assertTrue(item != Items.AIR, "Corn-crib block mapped to AIR: " + block);
			helper.assertValueEqual(Item.BY_BLOCK.get(block), item, "Item.BY_BLOCK mapping");
		}
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void earCornPileStatesEncodeExactStoredEarCounts(GameTestHelper helper) {
		helper.assertValueEqual(Fullness.values().length, 5, "pile fullness-state count");
		helper.assertValueEqual(PileShape.values().length, 4, "pile edge-shape count");
		int expected = LayeredEarCornPileBlock.EARS_PER_LEVEL;
		for (Fullness fullness : Fullness.values()) {
			helper.assertValueEqual(fullness.earCount(), expected, fullness + " stored ears");
			expected += LayeredEarCornPileBlock.EARS_PER_LEVEL;
		}
		helper.assertValueEqual(
			ModCornCribBlocks.YELLOW_EAR_CORN_PILE.defaultBlockState()
				.getValue(LayeredEarCornPileBlock.FULLNESS),
			Fullness.QUARTER,
			"default pile fullness"
		);
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void connectedWallStoresAllConnectionsAndAlternatingCourse(GameTestHelper helper) {
		helper.assertValueEqual(
			WideSetCribWallBlock.COURSE.getPossibleValues().size(),
			2,
			"crib-wall course count"
		);
		helper.assertValueEqual(
			WideSetCribWallBlock.Course.atY(10),
			WideSetCribWallBlock.Course.X_OVER_Z,
			"even course"
		);
		helper.assertValueEqual(
			WideSetCribWallBlock.Course.atY(11),
			WideSetCribWallBlock.Course.Z_OVER_X,
			"odd course"
		);
		helper.assertValueEqual(
			ModCornCribBlocks.WIDE_SET_CHESTNUT_CRIB_WALL.getStateDefinition()
				.getPossibleStates().size(),
			64,
			"four connections, two courses, and waterlogged states"
		);
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void cornContainersBeginEmptyAndExposeBoundedStates(GameTestHelper helper) {
		helper.assertTrue(
			!ModCornCribBlocks.BUSHEL_BASKET.defaultBlockState()
				.getValue(BushelBasketBlock.FILLED),
			"Bushel basket did not begin empty"
		);
		helper.assertValueEqual(
			CornBinBlock.LEVEL.getPossibleValues().size(),
			5,
			"corn-bin level count"
		);
		helper.assertValueEqual(
			ModCornCribBlocks.CORN_BIN.defaultBlockState().getValue(CornBinBlock.LEVEL),
			0,
			"default corn-bin level"
		);
		helper.assertTrue(
			!ModCornCribBlocks.HAND_CORN_SHELLER.defaultBlockState()
				.getValue(HandCornShellerBlock.LOADED),
			"Hand corn sheller did not begin unloaded"
		);
		helper.succeed();
	}
}
