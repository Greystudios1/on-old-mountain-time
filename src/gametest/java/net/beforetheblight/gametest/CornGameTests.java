package net.beforetheblight.gametest;

import java.util.List;

import net.beforetheblight.block.CornCropBlock;
import net.beforetheblight.registry.ModBlocks;
import net.beforetheblight.registry.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class CornGameTests {
	private static final BlockPos CROP_POS = new BlockPos(2, 2, 2);

	@GameTest(maxTicks = 20)
	public void cornRegistrationHasEightAgesAndKernelsAreTheOnlySeedItem(GameTestHelper helper) {
		helper.assertValueEqual(
			CornCropBlock.AGE.getPossibleValues().size(),
			8,
			"corn age-state count"
		);
		helper.assertValueEqual(
			ModBlocks.CORN.defaultBlockState().getValue(CornCropBlock.AGE),
			0,
			"default corn age"
		);
		helper.assertValueEqual(ModBlocks.CORN.getMaxAge(), 7, "maximum corn age");
		helper.assertValueEqual(ModBlocks.CORN.asItem(), ModItems.CORN_KERNELS, "corn seed item");
		helper.assertTrue(
			ModItems.CORN_KERNELS instanceof BlockItem blockItem && blockItem.getBlock() == ModBlocks.CORN,
			"Corn Kernels must be the corn crop's BlockItem"
		);
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void cornSurvivesOnFarmlandAndBonemealCapsAtAgeSeven(GameTestHelper helper) {
		SeasonGameTestSupport.useGrowingSeasonIfPresent(helper);
		helper.setBlock(CROP_POS.below(), Blocks.FARMLAND);
		helper.setBlock(CROP_POS.above(2), Blocks.GLOWSTONE);
		helper.setBlock(CROP_POS, ModBlocks.CORN.defaultBlockState());

		BlockPos absoluteCropPos = helper.absolutePos(CROP_POS);
		BlockState crop = helper.getBlockState(CROP_POS);
		helper.assertTrue(
			crop.canSurvive(helper.getLevel(), absoluteCropPos),
			"Corn did not survive on farmland in adequate light"
		);

		RandomSource random = RandomSource.create(20260722L);
		ModBlocks.CORN.performBonemeal(helper.getLevel(), random, absoluteCropPos, crop);
		int firstAge = helper.getBlockState(CROP_POS).getValue(CornCropBlock.AGE);
		helper.assertTrue(firstAge >= 2 && firstAge <= 5, "Vanilla bonemeal increment was outside 2-5");

		for (int attempt = 0; attempt < 8; attempt++) {
			BlockState current = helper.getBlockState(CROP_POS);
			if (!ModBlocks.CORN.isValidBonemealTarget(helper.getLevel(), absoluteCropPos, current)) {
				break;
			}
			ModBlocks.CORN.performBonemeal(helper.getLevel(), random, absoluteCropPos, current);
		}

		BlockState mature = helper.getBlockState(CROP_POS);
		helper.assertValueEqual(mature.getValue(CornCropBlock.AGE), 7, "bonemeal-capped corn age");
		helper.assertTrue(
			!ModBlocks.CORN.isValidBonemealTarget(helper.getLevel(), absoluteCropPos, mature),
			"Mature corn still accepted bonemeal"
		);
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void cornRejectsOrdinaryDirtAsCropSupport(GameTestHelper helper) {
		helper.setBlock(CROP_POS.below(), Blocks.DIRT);
		BlockState crop = ModBlocks.CORN.defaultBlockState();
		helper.assertTrue(
			!crop.canSurvive(helper.getLevel(), helper.absolutePos(CROP_POS)),
			"Corn survived on ordinary dirt instead of requiring crop-support soil"
		);
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void immatureAndMatureCornDropsMatchTheHarvestContract(GameTestHelper helper) {
		List<ItemStack> immatureDrops = Block.getDrops(
			ModBlocks.CORN.defaultBlockState(),
			helper.getLevel(),
			helper.absolutePos(CROP_POS),
			null
		);
		helper.assertValueEqual(count(immatureDrops, ModItems.CORN_KERNELS), 1, "immature kernel drops");
		helper.assertValueEqual(count(immatureDrops, ModItems.EAR_OF_CORN), 0, "immature ear drops");

		for (int sample = 0; sample < 32; sample++) {
			List<ItemStack> matureDrops = Block.getDrops(
				ModBlocks.CORN.getStateForAge(7),
				helper.getLevel(),
				helper.absolutePos(CROP_POS),
				null
			);
			int kernels = count(matureDrops, ModItems.CORN_KERNELS);
			helper.assertValueEqual(count(matureDrops, ModItems.EAR_OF_CORN), 1, "mature ear drops");
			helper.assertTrue(
				kernels >= 2 && kernels <= 4,
				"Mature corn kernel drops were outside the required 2-4 range: " + kernels
			);
		}
		helper.succeed();
	}

	private static int count(List<ItemStack> stacks, Item item) {
		return stacks.stream()
			.filter(stack -> stack.is(item))
			.mapToInt(ItemStack::getCount)
			.sum();
	}
}
