package net.beforetheblight.registry;

import java.util.List;
import java.util.function.Function;

import net.beforetheblight.BeforeTheBlight;
import net.beforetheblight.block.ExteriorPropBlock;
import net.beforetheblight.block.ExteriorPropBlock.ShapeKind;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.block.state.properties.WoodType;

/**
 * Isolated registry family for farmstead exteriors and workshop dressing.
 *
 * <p>Block items live in {@link ModExteriorItems}; internal state blocks are
 * intentionally not created here.</p>
 */
public final class ModExteriorBlocks {
	public static final Block SPLIT_RAIL_FENCE = registerWoodFence("split_rail_fence");
	public static final Block SPLIT_RAIL_GATE = registerWoodGate("split_rail_gate", WoodType.OAK);
	public static final Block WEATHERED_SPLIT_RAIL_FENCE = registerWoodFence(
		"weathered_split_rail_fence"
	);
	public static final Block BROKEN_SPLIT_RAIL_FENCE = registerWoodFence(
		"broken_split_rail_fence"
	);
	public static final Block PEELED_POLE_FENCE = registerWoodFence("peeled_pole_fence");
	public static final Block PEELED_POLE_GATE = registerWoodGate(
		"peeled_pole_gate",
		WoodType.SPRUCE
	);

	public static final ExteriorPropBlock STACKED_FIREWOOD = registerProp(
		"stacked_firewood",
		ShapeKind.LOW_STACK,
		woodPropProperties()
	);
	public static final ExteriorPropBlock LOG_STACK = registerProp(
		"log_stack",
		ShapeKind.TALL_STACK,
		woodPropProperties()
	);
	public static final ExteriorPropBlock SHINGLE_STACK = registerProp(
		"shingle_stack",
		ShapeKind.LOW_STACK,
		woodPropProperties()
	);
	public static final ExteriorPropBlock CHOPPING_BLOCK = registerProp(
		"chopping_block",
		ShapeKind.STUMP,
		woodPropProperties()
	);
	public static final ExteriorPropBlock AXE_IN_CHOPPING_BLOCK = registerProp(
		"axe_in_chopping_block",
		ShapeKind.TOOL_STUMP,
		woodPropProperties()
	);
	public static final ExteriorPropBlock SAWHORSE = registerProp(
		"sawhorse",
		ShapeKind.SAWHORSE,
		woodPropProperties()
	);
	public static final ExteriorPropBlock WALL_TOOL_RACK = registerProp(
		"wall_tool_rack",
		ShapeKind.WALL_RACK,
		woodPropProperties()
	);
	public static final ExteriorPropBlock WAGON_WHEEL = registerProp(
		"wagon_wheel",
		ShapeKind.WHEEL,
		woodPropProperties()
	);
	public static final ExteriorPropBlock WOODEN_BARREL = registerProp(
		"wooden_barrel",
		ShapeKind.BARREL,
		woodPropProperties()
	);
	public static final ExteriorPropBlock PRODUCE_CRATE = registerProp(
		"produce_crate",
		ShapeKind.CRATE,
		woodPropProperties()
	);
	public static final ExteriorPropBlock FEED_SACK = registerProp(
		"feed_sack",
		ShapeKind.SACK,
		BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_WOOL).noOcclusion()
	);

	public static final ExteriorPropBlock PACKED_DIRT_PATH = registerProp(
		"packed_dirt_path",
		ShapeKind.PATH,
		groundProperties(SoundType.GRAVEL)
	);
	public static final ExteriorPropBlock PATH_EDGE = registerProp(
		"path_edge",
		ShapeKind.GROUND_STRIP,
		groundCoverProperties(SoundType.GRAVEL)
	);
	public static final ExteriorPropBlock WAGON_RUT = registerProp(
		"wagon_rut",
		ShapeKind.GROUND_STRIP,
		groundCoverProperties(SoundType.GRAVEL)
	);
	public static final ExteriorPropBlock MUDDY_WAGON_RUT = registerProp(
		"muddy_wagon_rut",
		ShapeKind.GROUND_STRIP,
		groundCoverProperties(SoundType.MUD)
	);
	public static final ExteriorPropBlock STREAM_BANK_STONES = registerProp(
		"stream_bank_stones",
		ShapeKind.STONES,
		BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE).noOcclusion()
	);
	public static final ExteriorPropBlock EXPOSED_ROOT = registerProp(
		"exposed_root",
		ShapeKind.ROOT,
		woodPropProperties()
	);
	public static final ExteriorPropBlock MOSS_PATCH = registerProp(
		"moss_patch",
		ShapeKind.GROUND_STRIP,
		groundCoverProperties(SoundType.MOSS_CARPET)
	);
	public static final ExteriorPropBlock LEAF_LITTER = registerProp(
		"leaf_litter",
		ShapeKind.GROUND_STRIP,
		groundCoverProperties(SoundType.GRASS)
	);
	public static final ExteriorPropBlock FALLEN_BRANCH = registerProp(
		"fallen_branch",
		ShapeKind.BRANCH,
		woodPropProperties()
	);
	public static final ExteriorPropBlock BRUSH_PILE = registerProp(
		"brush_pile",
		ShapeKind.BRUSH,
		woodPropProperties()
	);

	public static final ExteriorPropBlock ROTTED_LOG_STACK = registerProp(
		"rotted_log_stack",
		ShapeKind.TALL_STACK,
		woodPropProperties()
	);
	public static final ExteriorPropBlock BROKEN_WAGON_WHEEL = registerProp(
		"broken_wagon_wheel",
		ShapeKind.WHEEL,
		woodPropProperties()
	);
	public static final ExteriorPropBlock BROKEN_CRATE = registerProp(
		"broken_crate",
		ShapeKind.CRATE,
		woodPropProperties()
	);

	public static final List<Block> FENCE_BLOCKS = List.of(
		SPLIT_RAIL_FENCE,
		SPLIT_RAIL_GATE,
		WEATHERED_SPLIT_RAIL_FENCE,
		BROKEN_SPLIT_RAIL_FENCE,
		PEELED_POLE_FENCE,
		PEELED_POLE_GATE
	);
	public static final List<Block> WORKSHOP_BLOCKS = List.of(
		CHOPPING_BLOCK,
		AXE_IN_CHOPPING_BLOCK,
		SAWHORSE,
		WALL_TOOL_RACK
	);
	public static final List<Block> PROP_BLOCKS = List.of(
		STACKED_FIREWOOD,
		LOG_STACK,
		SHINGLE_STACK,
		WAGON_WHEEL,
		WOODEN_BARREL,
		PRODUCE_CRATE,
		FEED_SACK,
		FALLEN_BRANCH,
		BRUSH_PILE
	);
	public static final List<Block> GROUND_BLOCKS = List.of(
		PACKED_DIRT_PATH,
		PATH_EDGE,
		WAGON_RUT,
		MUDDY_WAGON_RUT,
		STREAM_BANK_STONES,
		EXPOSED_ROOT,
		MOSS_PATCH,
		LEAF_LITTER
	);
	public static final List<Block> DAMAGED_BLOCKS = List.of(
		WEATHERED_SPLIT_RAIL_FENCE,
		BROKEN_SPLIT_RAIL_FENCE,
		ROTTED_LOG_STACK,
		BROKEN_WAGON_WHEEL,
		BROKEN_CRATE
	);
	public static final List<Block> FLAMMABLE_WOOD_BLOCKS = List.of(
		SPLIT_RAIL_FENCE,
		SPLIT_RAIL_GATE,
		WEATHERED_SPLIT_RAIL_FENCE,
		BROKEN_SPLIT_RAIL_FENCE,
		PEELED_POLE_FENCE,
		PEELED_POLE_GATE,
		STACKED_FIREWOOD,
		LOG_STACK,
		SHINGLE_STACK,
		CHOPPING_BLOCK,
		AXE_IN_CHOPPING_BLOCK,
		SAWHORSE,
		WALL_TOOL_RACK,
		WAGON_WHEEL,
		WOODEN_BARREL,
		PRODUCE_CRATE,
		EXPOSED_ROOT,
		FALLEN_BRANCH,
		BRUSH_PILE,
		ROTTED_LOG_STACK,
		BROKEN_WAGON_WHEEL,
		BROKEN_CRATE
	);
	public static final List<Block> ALL_BLOCKS = List.of(
		SPLIT_RAIL_FENCE,
		SPLIT_RAIL_GATE,
		WEATHERED_SPLIT_RAIL_FENCE,
		BROKEN_SPLIT_RAIL_FENCE,
		PEELED_POLE_FENCE,
		PEELED_POLE_GATE,
		STACKED_FIREWOOD,
		LOG_STACK,
		SHINGLE_STACK,
		CHOPPING_BLOCK,
		AXE_IN_CHOPPING_BLOCK,
		SAWHORSE,
		WALL_TOOL_RACK,
		WAGON_WHEEL,
		WOODEN_BARREL,
		PRODUCE_CRATE,
		FEED_SACK,
		PACKED_DIRT_PATH,
		PATH_EDGE,
		WAGON_RUT,
		MUDDY_WAGON_RUT,
		STREAM_BANK_STONES,
		EXPOSED_ROOT,
		MOSS_PATCH,
		LEAF_LITTER,
		FALLEN_BRANCH,
		BRUSH_PILE,
		ROTTED_LOG_STACK,
		BROKEN_WAGON_WHEEL,
		BROKEN_CRATE
	);

	private ModExteriorBlocks() {
	}

	private static boolean initialized;

	private static Block registerWoodFence(String name) {
		return register(
			name,
			FenceBlock::new,
			woodPropProperties().forceSolidOn()
		);
	}

	private static Block registerWoodGate(String name, WoodType woodType) {
		return register(
			name,
			properties -> new FenceGateBlock(woodType, properties),
			woodPropProperties().forceSolidOn()
		);
	}

	private static ExteriorPropBlock registerProp(
		String name,
		ShapeKind shape,
		BlockBehaviour.Properties properties
	) {
		return register(
			name,
			blockProperties -> new ExteriorPropBlock(shape, blockProperties),
			properties.noOcclusion()
		);
	}

	private static BlockBehaviour.Properties woodPropProperties() {
		return BlockBehaviour.Properties.of()
			.mapColor(MapColor.WOOD)
			.strength(1.5F, 2.0F)
			.sound(SoundType.WOOD)
			.ignitedByLava();
	}

	private static BlockBehaviour.Properties groundProperties(SoundType sound) {
		return BlockBehaviour.Properties.of()
			.mapColor(MapColor.DIRT)
			.strength(0.5F)
			.sound(sound);
	}

	private static BlockBehaviour.Properties groundCoverProperties(SoundType sound) {
		return groundProperties(sound)
			.replaceable()
			.noOcclusion()
			.forceSolidOff()
			.pushReaction(PushReaction.DESTROY);
	}

	private static <T extends Block> T register(
		String name,
		Function<BlockBehaviour.Properties, T> blockFactory,
		BlockBehaviour.Properties properties
	) {
		ResourceKey<Block> blockKey = ResourceKey.create(
			Registries.BLOCK,
			BeforeTheBlight.id(name)
		);
		T block = blockFactory.apply(properties.setId(blockKey));
		return Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
	}

	public static synchronized void initialize() {
		if (initialized) {
			return;
		}
		initialized = true;

		FlammableBlockRegistry flammable = FlammableBlockRegistry.getDefaultInstance();
		for (Block block : FLAMMABLE_WOOD_BLOCKS) {
			flammable.add(block, 5, 20);
		}
		flammable.add(FEED_SACK, 30, 60);
		flammable.add(MOSS_PATCH, 30, 60);
		flammable.add(LEAF_LITTER, 60, 100);
	}
}
