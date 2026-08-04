package net.beforetheblight.registry;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import net.beforetheblight.BeforeTheBlight;
import net.beforetheblight.block.ChestnutChinkingStripBlock;
import net.beforetheblight.block.ChestnutLogEndBlock;
import net.beforetheblight.block.ChestnutRepairCornerBlock;
import net.beforetheblight.block.ChestnutStructuralMemberBlock;
import net.beforetheblight.block.ChestnutSupportPostBlock;
import net.beforetheblight.block.ConnectedChestnutLogWallBlock;
import net.beforetheblight.block.HewnChestnutKneeBraceBlock;
import net.beforetheblight.block.PeggedChestnutScarfJointBlock;
import net.beforetheblight.block.RegionalHalfLogBlock;
import net.beforetheblight.block.RegionalPoleBlock;
import net.beforetheblight.block.SaddleNotchedChestnutCornerBlock;
import net.beforetheblight.registry.ModContentCatalog.Category;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

/**
 * Isolated registration family for detailed chestnut timber-construction
 * pieces.
 *
 * <p>Every block in this family is obtainable and belongs in the building
 * materials category. The lists intentionally contain blocks, which also
 * satisfy Minecraft's {@code ItemLike} contract for downstream creative-tab
 * and datagen integration.</p>
 */
public final class ModTimberBlocks {
	public static final RegionalPoleBlock PEELED_CHESTNUT_POLE = register(
		"peeled_chestnut_pole",
		RegionalPoleBlock::new,
		timberProperties().noOcclusion()
	);
	public static final RegionalPoleBlock PARTIALLY_PEELED_CHESTNUT_POLE =
		register(
			"partially_peeled_chestnut_pole",
			RegionalPoleBlock::new,
			timberProperties().noOcclusion()
		);
	public static final RegionalHalfLogBlock CHESTNUT_HALF_LOG = register(
		"chestnut_half_log",
		RegionalHalfLogBlock::new,
		timberProperties().noOcclusion()
	);
	public static final Block CHESTNUT_EXPOSED_END_GRAIN = register(
		"chestnut_exposed_end_grain",
		Block::new,
		timberProperties()
	);
	public static final ChestnutLogEndBlock PROJECTING_CHESTNUT_LOG_END =
		register(
			"projecting_chestnut_log_end",
			ChestnutLogEndBlock::projecting,
			timberProperties().noOcclusion()
		);
	public static final ChestnutLogEndBlock FLUSH_CHESTNUT_LOG_END = register(
		"flush_chestnut_log_end",
		ChestnutLogEndBlock::flush,
		timberProperties().noOcclusion()
	);
	public static final ChestnutLogEndBlock NOTCHED_CHESTNUT_LOG_END = register(
		"notched_chestnut_log_end",
		ChestnutLogEndBlock::notched,
		timberProperties().noOcclusion()
	);

	public static final ConnectedChestnutLogWallBlock
		CONNECTED_CHESTNUT_LOG_WALL = register(
			"connected_chestnut_log_wall",
			ConnectedChestnutLogWallBlock::plain,
			timberProperties().noOcclusion()
		);
	public static final ConnectedChestnutLogWallBlock
		CHESTNUT_LOG_DOOR_TERMINATION = register(
			"chestnut_log_door_termination",
			ConnectedChestnutLogWallBlock::door,
			timberProperties().noOcclusion()
		);
	public static final ConnectedChestnutLogWallBlock
		CHESTNUT_LOG_WINDOW_TERMINATION = register(
			"chestnut_log_window_termination",
			ConnectedChestnutLogWallBlock::window,
			timberProperties().noOcclusion()
		);
	public static final ChestnutChinkingStripBlock CHESTNUT_CHINKING_STRIP =
		register(
			"chestnut_chinking_strip",
			ChestnutChinkingStripBlock::new,
			chinkingProperties().noOcclusion()
		);
	public static final ChestnutRepairCornerBlock CHESTNUT_REPAIR_CORNER =
		register(
			"chestnut_repair_corner",
			ChestnutRepairCornerBlock::new,
			timberProperties().noOcclusion()
		);

	public static final RegionalPoleBlock ROUND_CHESTNUT_SUPPORT_POLE =
		register(
			"round_chestnut_support_pole",
			RegionalPoleBlock::new,
			timberProperties().noOcclusion()
		);
	public static final RegionalPoleBlock CHESTNUT_RAFTER = register(
		"chestnut_rafter",
		RegionalPoleBlock::new,
		timberProperties().noOcclusion()
	);
	public static final RegionalPoleBlock CHESTNUT_RIDGEPOLE = register(
		"chestnut_ridgepole",
		RegionalPoleBlock::new,
		timberProperties().noOcclusion()
	);
	public static final RegionalPoleBlock CHESTNUT_PURLIN = register(
		"chestnut_purlin",
		RegionalPoleBlock::new,
		timberProperties().noOcclusion()
	);
	public static final RegionalPoleBlock CHESTNUT_RAFTER_TAIL = register(
		"chestnut_rafter_tail",
		RegionalPoleBlock::new,
		timberProperties().noOcclusion()
	);

	public static final ChestnutStructuralMemberBlock CHESTNUT_SILL_BEAM =
		register(
			"chestnut_sill_beam",
			ChestnutStructuralMemberBlock::beam,
			timberProperties().noOcclusion()
		);
	public static final ChestnutStructuralMemberBlock CHESTNUT_WALL_PLATE =
		register(
			"chestnut_wall_plate",
			ChestnutStructuralMemberBlock::beam,
			timberProperties().noOcclusion()
		);
	public static final ChestnutStructuralMemberBlock CHESTNUT_JOIST = register(
		"chestnut_joist",
		ChestnutStructuralMemberBlock::joist,
		timberProperties().noOcclusion()
	);
	public static final ChestnutStructuralMemberBlock CHESTNUT_TIE_BEAM =
		register(
			"chestnut_tie_beam",
			ChestnutStructuralMemberBlock::beam,
			timberProperties().noOcclusion()
		);
	public static final ChestnutStructuralMemberBlock CHESTNUT_TROUGH_SLEEPER =
		register(
			"chestnut_trough_sleeper",
			ChestnutStructuralMemberBlock::sleeper,
			timberProperties().noOcclusion()
		);
	public static final ChestnutStructuralMemberBlock
		CHESTNUT_BLOCKING_TIMBER = register(
			"chestnut_blocking_timber",
			ChestnutStructuralMemberBlock::blocking,
			timberProperties().noOcclusion()
		);

	public static final ChestnutSupportPostBlock
		FORKED_CHESTNUT_SUPPORT_POST = register(
			"forked_chestnut_support_post",
			ChestnutSupportPostBlock::forked,
			timberProperties().noOcclusion()
		);
	public static final ChestnutSupportPostBlock
		TAPERED_HEWN_CHESTNUT_POST = register(
			"tapered_hewn_chestnut_post",
			ChestnutSupportPostBlock::tapered,
			timberProperties().noOcclusion()
		);
	public static final HewnChestnutKneeBraceBlock
		PEGGED_CHESTNUT_DIAGONAL_BRACE = register(
			"pegged_chestnut_diagonal_brace",
			HewnChestnutKneeBraceBlock::new,
			timberProperties().noOcclusion()
		);

	public static final ChestnutStructuralMemberBlock
		CHESTNUT_MORTISE_TENON_JOINT = register(
			"chestnut_mortise_tenon_joint",
			ChestnutStructuralMemberBlock::beam,
			timberProperties().noOcclusion()
		);
	public static final ChestnutStructuralMemberBlock
		CHESTNUT_HALF_LAP_JOINT = register(
			"chestnut_half_lap_joint",
			ChestnutStructuralMemberBlock::beam,
			timberProperties().noOcclusion()
		);
	public static final ChestnutStructuralMemberBlock
		CHESTNUT_CROSS_LAP_JOINT = register(
			"chestnut_cross_lap_joint",
			ChestnutStructuralMemberBlock::beam,
			timberProperties().noOcclusion()
		);
	public static final ChestnutStructuralMemberBlock CHESTNUT_END_TENON =
		register(
			"chestnut_end_tenon",
			ChestnutStructuralMemberBlock::beam,
			timberProperties().noOcclusion()
		);
	public static final ChestnutStructuralMemberBlock
		CHESTNUT_BEAM_POCKET_REPAIR = register(
			"chestnut_beam_pocket_repair",
			ChestnutStructuralMemberBlock::beam,
			timberProperties().noOcclusion()
		);

	public static final SaddleNotchedChestnutCornerBlock
		SADDLE_NOTCHED_CHESTNUT_CORNER = register(
			"saddle_notched_chestnut_corner",
			SaddleNotchedChestnutCornerBlock::new,
			timberProperties().noOcclusion()
		);

	public static final PeggedChestnutScarfJointBlock
		PEGGED_CHESTNUT_SCARF_JOINT = register(
			"pegged_chestnut_scarf_joint",
			PeggedChestnutScarfJointBlock::new,
			timberProperties()
		);

	public static final HewnChestnutKneeBraceBlock
		HEWN_CHESTNUT_KNEE_BRACE = register(
			"hewn_chestnut_knee_brace",
			HewnChestnutKneeBraceBlock::new,
			timberProperties().noOcclusion()
		);

	public static final List<Block> BUILDING = List.of(
		PEELED_CHESTNUT_POLE,
		PARTIALLY_PEELED_CHESTNUT_POLE,
		CHESTNUT_HALF_LOG,
		CHESTNUT_EXPOSED_END_GRAIN,
		PROJECTING_CHESTNUT_LOG_END,
		FLUSH_CHESTNUT_LOG_END,
		NOTCHED_CHESTNUT_LOG_END,
		CONNECTED_CHESTNUT_LOG_WALL,
		CHESTNUT_LOG_DOOR_TERMINATION,
		CHESTNUT_LOG_WINDOW_TERMINATION,
		CHESTNUT_CHINKING_STRIP,
		CHESTNUT_REPAIR_CORNER,
		SADDLE_NOTCHED_CHESTNUT_CORNER,
		ROUND_CHESTNUT_SUPPORT_POLE,
		CHESTNUT_RAFTER,
		CHESTNUT_RIDGEPOLE,
		CHESTNUT_PURLIN,
		CHESTNUT_RAFTER_TAIL,
		CHESTNUT_SILL_BEAM,
		CHESTNUT_WALL_PLATE,
		CHESTNUT_JOIST,
		CHESTNUT_TIE_BEAM,
		CHESTNUT_TROUGH_SLEEPER,
		CHESTNUT_BLOCKING_TIMBER,
		FORKED_CHESTNUT_SUPPORT_POST,
		TAPERED_HEWN_CHESTNUT_POST,
		PEGGED_CHESTNUT_DIAGONAL_BRACE,
		PEGGED_CHESTNUT_SCARF_JOINT,
		CHESTNUT_MORTISE_TENON_JOINT,
		CHESTNUT_HALF_LAP_JOINT,
		CHESTNUT_CROSS_LAP_JOINT,
		CHESTNUT_END_TENON,
		CHESTNUT_BEAM_POCKET_REPAIR,
		HEWN_CHESTNUT_KNEE_BRACE
	);
	public static final List<Block> ALL_ITEMS = BUILDING;
	public static final List<Block> AXE_MINEABLE = BUILDING.stream()
		.filter(block -> block != CHESTNUT_CHINKING_STRIP)
		.toList();
	public static final List<Block> SHOVEL_MINEABLE = List.of(
		CHESTNUT_CHINKING_STRIP
	);
	public static final Map<String, String> LANGUAGE_ENTRIES = Map.ofEntries(
		Map.entry("peeled_chestnut_pole", "Peeled Chestnut Pole"),
		Map.entry(
			"partially_peeled_chestnut_pole",
			"Partially Peeled Chestnut Pole"
		),
		Map.entry("chestnut_half_log", "Chestnut Half Log"),
		Map.entry("chestnut_exposed_end_grain", "Chestnut Exposed End Grain"),
		Map.entry(
			"projecting_chestnut_log_end",
			"Projecting Chestnut Log End"
		),
		Map.entry("flush_chestnut_log_end", "Flush Chestnut Log End"),
		Map.entry("notched_chestnut_log_end", "Notched Chestnut Log End"),
		Map.entry(
			"connected_chestnut_log_wall",
			"Connected Chestnut Log Wall"
		),
		Map.entry(
			"chestnut_log_door_termination",
			"Chestnut Log Door Termination"
		),
		Map.entry(
			"chestnut_log_window_termination",
			"Chestnut Log Window Termination"
		),
		Map.entry("chestnut_chinking_strip", "Chestnut Chinking Strip"),
		Map.entry("chestnut_repair_corner", "Chestnut Repair Corner"),
		Map.entry(
			"saddle_notched_chestnut_corner",
			"Saddle-Notched Chestnut Corner"
		),
		Map.entry(
			"round_chestnut_support_pole",
			"Round Chestnut Support Pole"
		),
		Map.entry("chestnut_rafter", "Chestnut Rafter"),
		Map.entry("chestnut_ridgepole", "Chestnut Ridgepole"),
		Map.entry("chestnut_purlin", "Chestnut Purlin"),
		Map.entry("chestnut_rafter_tail", "Chestnut Rafter Tail"),
		Map.entry("chestnut_sill_beam", "Chestnut Sill Beam"),
		Map.entry("chestnut_wall_plate", "Chestnut Wall Plate"),
		Map.entry("chestnut_joist", "Chestnut Joist"),
		Map.entry("chestnut_tie_beam", "Chestnut Tie Beam"),
		Map.entry("chestnut_trough_sleeper", "Chestnut Trough Sleeper"),
		Map.entry("chestnut_blocking_timber", "Chestnut Blocking Timber"),
		Map.entry(
			"forked_chestnut_support_post",
			"Forked Chestnut Support Post"
		),
		Map.entry(
			"tapered_hewn_chestnut_post",
			"Tapered Hewn Chestnut Post"
		),
		Map.entry(
			"pegged_chestnut_diagonal_brace",
			"Pegged Chestnut Diagonal Brace"
		),
		Map.entry(
			"pegged_chestnut_scarf_joint",
			"Pegged Chestnut Scarf Joint"
		),
		Map.entry(
			"chestnut_mortise_tenon_joint",
			"Chestnut Mortise-and-Tenon Joint"
		),
		Map.entry("chestnut_half_lap_joint", "Chestnut Half-Lap Joint"),
		Map.entry("chestnut_cross_lap_joint", "Chestnut Cross-Lap Joint"),
		Map.entry("chestnut_end_tenon", "Chestnut End Tenon"),
		Map.entry(
			"chestnut_beam_pocket_repair",
			"Chestnut Beam-Pocket Repair"
		),
		Map.entry("hewn_chestnut_knee_brace", "Hewn Chestnut Knee Brace")
	);
	private static boolean initialized;

	private ModTimberBlocks() {
	}

	private static BlockBehaviour.Properties timberProperties() {
		return BlockBehaviour.Properties.of()
			.mapColor(MapColor.WOOD)
			.instrument(NoteBlockInstrument.BASS)
			.strength(2.0F, 3.0F)
			.sound(SoundType.WOOD)
			.ignitedByLava();
	}

	private static BlockBehaviour.Properties chinkingProperties() {
		return BlockBehaviour.Properties.ofFullCopy(Blocks.PACKED_MUD)
			.mapColor(MapColor.TERRACOTTA_BROWN)
			.sound(SoundType.PACKED_MUD);
	}

	private static <T extends Block> T register(
		String name,
		Function<BlockBehaviour.Properties, T> blockFactory,
		BlockBehaviour.Properties properties
	) {
		ResourceKey<Block> blockKey =
			ResourceKey.create(Registries.BLOCK, BeforeTheBlight.id(name));
		T block = blockFactory.apply(properties.setId(blockKey));

		ResourceKey<Item> itemKey =
			ResourceKey.create(Registries.ITEM, BeforeTheBlight.id(name));
		BlockItem blockItem = new BlockItem(
			block,
			new Item.Properties()
				.setId(itemKey)
				.useBlockDescriptionPrefix()
		);
		Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);
		blockItem.registerBlocks(Item.BY_BLOCK, blockItem);

		return Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
	}

	public static synchronized void initialize() {
		if (initialized) {
			return;
		}
		FlammableBlockRegistry flammableBlocks =
			FlammableBlockRegistry.getDefaultInstance();
		for (Block block : AXE_MINEABLE) {
			flammableBlocks.add(block, 5, 5);
		}
		ModContentCatalog.register(
			Category.BUILDING_MATERIALS,
			BUILDING.toArray(ItemLike[]::new)
		);
		initialized = true;
	}
}
