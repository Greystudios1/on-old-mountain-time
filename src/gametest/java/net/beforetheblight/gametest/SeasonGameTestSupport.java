package net.beforetheblight.gametest;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Keeps growth fixtures deterministic when the optional seasonal profile is
 * present. Production worlds still begin and progress on Serene Seasons'
 * calendar; only tests whose subject is tree/crop growth select midsummer.
 */
final class SeasonGameTestSupport {
	private static final String SERENE_SEASONS_MOD_ID = "sereneseasons";

	private SeasonGameTestSupport() {
	}

	static void useGrowingSeasonIfPresent(GameTestHelper helper) {
		if (!FabricLoader.getInstance().isModLoaded(SERENE_SEASONS_MOD_ID)) {
			return;
		}

		helper.getLevel().getServer().getCommands().performPrefixedCommand(
			helper.getLevel().getServer().createCommandSourceStack().withLevel(helper.getLevel()),
			"season set mid_summer"
		);
	}

	/**
	 * Geometry fixtures need to preserve their pinned feature seed. With the
	 * optional clock loaded, the natural-growth path is intentionally
	 * probabilistic and may consume a roll before tree generation. The public
	 * bonemeal path is the deterministic in-season route. Base-profile tests
	 * continue to exercise natural growth exactly as before.
	 */
	static void advanceSaplingForGeometry(
		SaplingBlock sapling,
		ServerLevel level,
		BlockPos pos,
		BlockState state,
		RandomSource random
	) {
		if (FabricLoader.getInstance().isModLoaded(SERENE_SEASONS_MOD_ID)) {
			sapling.performBonemeal(level, random, pos, state);
			return;
		}
		sapling.advanceTree(level, pos, state, random);
	}
}
