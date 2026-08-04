package net.beforetheblight.gametest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import net.beforetheblight.block.DryingCornBundleBlock;
import net.beforetheblight.registry.ModBlocks;
import net.beforetheblight.registry.ModItems;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class DryingCornGameTests {
	private static final BlockPos TARGET = new BlockPos(3, 2, 3);
	private static final double DROP_SEARCH_RADIUS = 1.5;

	@GameTest(maxTicks = 40)
	public void placementMigrationAndAllDryingStatesPersist(GameTestHelper helper) {
		BlockState state = ModBlocks.DRYING_CORN_BUNDLE.defaultBlockState();
		helper.assertValueEqual(
			state.getValue(DryingCornBundleBlock.COUNT),
			DryingCornBundleBlock.LEGACY_EAR_COUNT,
			"missing count property must retain the historical four-ear load"
		);
		BlockState placed = ModBlocks.DRYING_CORN_BUNDLE.getStateForPlacement(null);
		helper.assertValueEqual(
			placed.getValue(DryingCornBundleBlock.COUNT),
			0,
			"player-placed rack must start empty"
		);
		helper.assertValueEqual(
			DryingCornBundleBlock.advanceAge(placed),
			placed,
			"empty rack must not dry"
		);

		for (int expectedAge = 0; expectedAge <= DryingCornBundleBlock.MAX_AGE; expectedAge++) {
			for (int count : List.of(1, 16, 32, 48, 64)) {
				BlockState witness = state.setValue(DryingCornBundleBlock.COUNT, count);
				helper.assertValueEqual(
					witness.getValue(DryingCornBundleBlock.AGE),
					expectedAge,
					"drying age"
				);
				assertNbtRoundTrip(
					helper,
					witness,
					"drying rack age " + expectedAge + " count " + count
				);
			}
			state = DryingCornBundleBlock.advanceAge(state);
		}
		helper.assertValueEqual(
			state.getValue(DryingCornBundleBlock.AGE),
			DryingCornBundleBlock.MAX_AGE,
			"mature state must not advance past max age"
		);

		helper.setBlock(TARGET, ModBlocks.DRYING_CORN_BUNDLE.defaultBlockState());
		for (int tick = 0; tick < 512; tick++) {
			helper.randomTick(TARGET);
		}
		helper.assertBlockProperty(
			TARGET,
			DryingCornBundleBlock.AGE,
			DryingCornBundleBlock.MAX_AGE
		);
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void stackTransfersCapAt64AndBreakDropsConserveRack(GameTestHelper helper) {
		FakePlayer player = FakePlayer.get(
			helper.getLevel(),
			new GameProfile(UUID.randomUUID(), "btb-drying-load")
		);
		player.setGameMode(GameType.SURVIVAL);
		helper.setBlock(
			TARGET,
			ModBlocks.DRYING_CORN_BUNDLE.defaultBlockState()
				.setValue(DryingCornBundleBlock.COUNT, 0)
		);
		ItemStack oneEar = new ItemStack(ModItems.EAR_OF_CORN, 1);
		helper.assertTrue(
			useOnRack(helper, player, oneEar) instanceof InteractionResult.Success,
			"single-ear load did not succeed"
		);
		helper.assertValueEqual(oneEar.getCount(), 0, "single-ear input remainder");
		helper.assertBlockProperty(TARGET, DryingCornBundleBlock.COUNT, 1);

		helper.setBlock(
			TARGET,
			ModBlocks.DRYING_CORN_BUNDLE.defaultBlockState()
				.setValue(DryingCornBundleBlock.AGE, DryingCornBundleBlock.MAX_AGE)
				.setValue(DryingCornBundleBlock.COUNT, 63)
		);
		ItemStack fullStack = new ItemStack(ModItems.EAR_OF_CORN, 64);
		helper.assertTrue(
			useOnRack(helper, player, fullStack) instanceof InteractionResult.Success,
			"capacity-capped load did not succeed"
		);
		helper.assertValueEqual(fullStack.getCount(), 63, "capacity-capped input remainder");
		helper.assertBlockProperty(TARGET, DryingCornBundleBlock.COUNT, 64);
		helper.assertBlockProperty(TARGET, DryingCornBundleBlock.AGE, 0);
		ItemStack rejectedAtCapacity = new ItemStack(ModItems.EAR_OF_CORN, 64);
		useOnRack(helper, player, rejectedAtCapacity);
		helper.assertValueEqual(
			rejectedAtCapacity.getCount(),
			64,
			"full rack must transfer zero ears"
		);
		helper.assertBlockProperty(TARGET, DryingCornBundleBlock.COUNT, 64);

		BlockState fresh = ModBlocks.DRYING_CORN_BUNDLE.defaultBlockState()
			.setValue(DryingCornBundleBlock.COUNT, 1);
		BlockState dry = fresh
			.setValue(DryingCornBundleBlock.COUNT, DryingCornBundleBlock.MAX_EAR_COUNT)
			.setValue(DryingCornBundleBlock.AGE, DryingCornBundleBlock.MAX_AGE);

		assertDropsExactly(
			helper,
			fresh,
			Map.of(ModBlocks.DRYING_CORN_BUNDLE.asItem(), 1, ModItems.EAR_OF_CORN, 1),
			"unfinished rack"
		);
		assertDropsExactly(
			helper,
			dry,
			Map.of(
				ModBlocks.DRYING_CORN_BUNDLE.asItem(),
				1,
				ModItems.DRIED_EAR_OF_CORN,
				DryingCornBundleBlock.MAX_EAR_COUNT
			),
			"finished rack"
		);
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void emptyHandUnloadIsServerAuthoritativeAndOneShot(GameTestHelper helper) {
		clearItemEntities(helper);
		helper.setBlock(
			TARGET,
			ModBlocks.DRYING_CORN_BUNDLE.defaultBlockState()
				.setValue(DryingCornBundleBlock.AGE, DryingCornBundleBlock.MAX_AGE)
				.setValue(DryingCornBundleBlock.COUNT, DryingCornBundleBlock.MAX_EAR_COUNT)
		);
		FakePlayer player = FakePlayer.get(
			helper.getLevel(),
			new GameProfile(UUID.randomUUID(), "btb-drying-harvest")
		);
		player.setGameMode(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

		BlockPos absolutePos = helper.absolutePos(TARGET);
		BlockHitResult hit = new BlockHitResult(
			Vec3.atCenterOf(absolutePos),
			Direction.UP,
			absolutePos,
			false
		);
		InteractionResult result = useOnRack(helper, player, ItemStack.EMPTY, hit);

		helper.assertTrue(result instanceof InteractionResult.Success, "mature harvest was not successful");
		helper.assertBlockProperty(TARGET, DryingCornBundleBlock.COUNT, 0);
		helper.assertBlockProperty(TARGET, DryingCornBundleBlock.AGE, 0);
		helper.assertValueEqual(
			worldDropCounts(helper),
			Map.of(ModItems.DRIED_EAR_OF_CORN, DryingCornBundleBlock.MAX_EAR_COUNT),
			"mature harvest world drops"
		);
		useOnRack(helper, player, ItemStack.EMPTY, hit);
		helper.assertValueEqual(
			worldDropCounts(helper),
			Map.of(ModItems.DRIED_EAR_OF_CORN, DryingCornBundleBlock.MAX_EAR_COUNT),
			"empty rack duplicated mature output"
		);

		clearItemEntities(helper);
		helper.setBlock(
			TARGET,
			ModBlocks.DRYING_CORN_BUNDLE.defaultBlockState()
				.setValue(DryingCornBundleBlock.AGE, 2)
				.setValue(DryingCornBundleBlock.COUNT, 17)
		);
		useOnRack(helper, player, ItemStack.EMPTY, hit);
		helper.assertBlockProperty(TARGET, DryingCornBundleBlock.COUNT, 0);
		helper.assertValueEqual(
			worldDropCounts(helper),
			Map.of(ModItems.EAR_OF_CORN, 17),
			"unfinished unload world drops"
		);
		helper.succeed();
	}

	private static InteractionResult useOnRack(
		GameTestHelper helper,
		FakePlayer player,
		ItemStack stack
	) {
		BlockPos absolutePos = helper.absolutePos(TARGET);
		return useOnRack(
			helper,
			player,
			stack,
			new BlockHitResult(Vec3.atCenterOf(absolutePos), Direction.UP, absolutePos, false)
		);
	}

	private static InteractionResult useOnRack(
		GameTestHelper helper,
		FakePlayer player,
		ItemStack stack,
		BlockHitResult hit
	) {
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		return player.gameMode.useItemOn(
			player,
			helper.getLevel(),
			stack,
			InteractionHand.MAIN_HAND,
			hit
		);
	}

	private static void assertDropsExactly(
		GameTestHelper helper,
		BlockState state,
		Map<Item, Integer> expected,
		String label
	) {
		List<ItemStack> drops = Block.getDrops(
			state,
			helper.getLevel(),
			helper.absolutePos(TARGET),
			null
		);
		Map<Item, Integer> actual = new LinkedHashMap<>();
		for (ItemStack stack : drops) {
			actual.merge(stack.getItem(), stack.getCount(), Integer::sum);
		}
		helper.assertValueEqual(actual, expected, label + " drops");
	}

	private static void assertNbtRoundTrip(GameTestHelper helper, BlockState original, String label) {
		Tag encoded = BlockState.CODEC
			.encodeStart(NbtOps.INSTANCE, original)
			.getOrThrow(IllegalStateException::new);
		BlockState decoded = BlockState.CODEC
			.parse(NbtOps.INSTANCE, encoded)
			.getOrThrow(IllegalStateException::new);
		helper.assertValueEqual(decoded, original, label + " NBT round trip");
	}

	private static Map<Item, Integer> worldDropCounts(GameTestHelper helper) {
		Map<Item, Integer> counts = new LinkedHashMap<>();
		for (ItemEntity entity : itemEntities(helper)) {
			ItemStack stack = entity.getItem();
			counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
		}
		return counts;
	}

	private static List<ItemEntity> itemEntities(GameTestHelper helper) {
		BlockPos absolutePos = helper.absolutePos(TARGET);
		return helper.getLevel().getEntities(
			EntityType.ITEM,
			new AABB(absolutePos).inflate(DROP_SEARCH_RADIUS),
			ItemEntity::isAlive
		);
	}

	private static void clearItemEntities(GameTestHelper helper) {
		itemEntities(helper).forEach(ItemEntity::discard);
	}
}
