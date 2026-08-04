package net.beforetheblight.registry;

import net.beforetheblight.BeforeTheBlight;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

public final class ModTags {
	public static final TagKey<Block> CHESTNUT_LOGS = TagKey.create(
		Registries.BLOCK,
		BeforeTheBlight.id("chestnut_logs")
	);

	public static final TagKey<Item> CHESTNUT_LOG_ITEMS = TagKey.create(
		Registries.ITEM,
		BeforeTheBlight.id("chestnut_logs")
	);

	public static final TagKey<Block> HEMLOCK_LOGS = TagKey.create(
		Registries.BLOCK,
		BeforeTheBlight.id("hemlock_logs")
	);

	public static final TagKey<Item> HEMLOCK_LOG_ITEMS = TagKey.create(
		Registries.ITEM,
		BeforeTheBlight.id("hemlock_logs")
	);

	public static final TagKey<Block> AMERICAN_BEECH_LOGS = TagKey.create(
		Registries.BLOCK,
		BeforeTheBlight.id("american_beech_logs")
	);

	public static final TagKey<Item> AMERICAN_BEECH_LOG_ITEMS = TagKey.create(
		Registries.ITEM,
		BeforeTheBlight.id("american_beech_logs")
	);

	public static final TagKey<Block> BLACK_WALNUT_LOGS = TagKey.create(
		Registries.BLOCK,
		BeforeTheBlight.id("black_walnut_logs")
	);

	public static final TagKey<Item> BLACK_WALNUT_LOG_ITEMS = TagKey.create(
		Registries.ITEM,
		BeforeTheBlight.id("black_walnut_logs")
	);

	public static final TagKey<Block> CHESTNUT_WOODEN_BLOCKS = TagKey.create(
		Registries.BLOCK,
		BeforeTheBlight.id("chestnut_wooden_blocks")
	);

	public static final TagKey<Item> CHESTNUT_WOODEN_ITEMS = TagKey.create(
		Registries.ITEM,
		BeforeTheBlight.id("chestnut_wooden_blocks")
	);

	public static final TagKey<Block> HEWING_LOGS = TagKey.create(
		Registries.BLOCK,
		BeforeTheBlight.id("hewing_logs")
	);

	public static final TagKey<Item> HEWING_LOG_ITEMS = TagKey.create(
		Registries.ITEM,
		BeforeTheBlight.id("hewing_logs")
	);

	public static final TagKey<Block> HEWN_BEAMS = TagKey.create(
		Registries.BLOCK,
		BeforeTheBlight.id("hewn_beams")
	);

	public static final TagKey<Item> HEWN_BEAM_ITEMS = TagKey.create(
		Registries.ITEM,
		BeforeTheBlight.id("hewn_beams")
	);

	public static final TagKey<Block> SAWABLE_BEAMS = TagKey.create(
		Registries.BLOCK,
		BeforeTheBlight.id("sawable_beams")
	);

	public static final TagKey<Item> SAWABLE_BEAM_ITEMS = TagKey.create(
		Registries.ITEM,
		BeforeTheBlight.id("sawable_beams")
	);

	public static final TagKey<Block> ROUGH_BOARDS = TagKey.create(
		Registries.BLOCK,
		BeforeTheBlight.id("rough_boards")
	);

	public static final TagKey<Item> ROUGH_BOARD_ITEMS = TagKey.create(
		Registries.ITEM,
		BeforeTheBlight.id("rough_boards")
	);

	public static final TagKey<Biome> CHESTNUT_OAK_RIDGE_TARGETS = TagKey.create(
		Registries.BIOME,
		BeforeTheBlight.id("chestnut_oak_ridge_targets")
	);

	private ModTags() {
	}
}
