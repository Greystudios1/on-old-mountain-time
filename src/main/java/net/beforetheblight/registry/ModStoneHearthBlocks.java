package net.beforetheblight.registry;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import net.beforetheblight.BeforeTheBlight;
import net.beforetheblight.block.stonehearth.StoneFireboxBlock;
import net.beforetheblight.block.stonehearth.StoneHearthFacingBlock;
import net.beforetheblight.block.stonehearth.StoneHearthLayerBlock;
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
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Isolated registration and catalog contract for modular Appalachian masonry,
 * hearth, chimney, earth-floor, and chinking pieces.
 *
 * <p>The family deliberately reuses the existing Fieldstone, Dressed
 * Fieldstone, Chiseled Fieldstone, and Fieldstone Pier IDs instead of adding
 * misleading polished/pillar duplicates. Call {@link #initialize()} after the
 * base block registry and before creative tabs seal the content catalog.</p>
 */
public final class ModStoneHearthBlocks {
	private static final VoxelShape FIREBOX_BACK_SHAPE =
		Block.box(0.0, 0.0, 12.0, 16.0, 16.0, 16.0);
	private static final VoxelShape FIREBOX_ARCH_SHAPE = Shapes.or(
		Block.box(0.0, 0.0, 10.0, 4.0, 16.0, 16.0),
		Block.box(12.0, 0.0, 10.0, 16.0, 16.0, 16.0),
		Block.box(4.0, 12.0, 10.0, 12.0, 16.0, 16.0)
	);
	private static final VoxelShape KEYSTONE_SHAPE = Shapes.or(
		Block.box(5.0, 6.0, 9.0, 11.0, 16.0, 16.0),
		Block.box(4.0, 13.0, 8.0, 12.0, 16.0, 16.0)
	);
	private static final VoxelShape CORNER_SHAPE = Shapes.or(
		Block.box(0.0, 0.0, 0.0, 5.0, 16.0, 16.0),
		Block.box(0.0, 0.0, 11.0, 16.0, 16.0, 16.0)
	);
	private static final VoxelShape THROAT_SHAPE = Shapes.or(
		Block.box(0.0, 0.0, 8.0, 16.0, 6.0, 16.0),
		Block.box(2.0, 6.0, 9.0, 14.0, 11.0, 16.0),
		Block.box(4.0, 11.0, 10.0, 12.0, 16.0, 16.0)
	);
	private static final VoxelShape SMOKE_CHAMBER_SHAPE = Shapes.or(
		Block.box(1.0, 0.0, 6.0, 15.0, 6.0, 16.0),
		Block.box(3.0, 6.0, 8.0, 13.0, 11.0, 16.0),
		Block.box(5.0, 11.0, 10.0, 11.0, 16.0, 16.0)
	);
	private static final VoxelShape FLUE_SHAPE = Shapes.or(
		Block.box(0.0, 0.0, 0.0, 4.0, 16.0, 16.0),
		Block.box(12.0, 0.0, 0.0, 16.0, 16.0, 16.0),
		Block.box(4.0, 0.0, 0.0, 12.0, 16.0, 4.0),
		Block.box(4.0, 0.0, 12.0, 12.0, 16.0, 16.0)
	);
	private static final VoxelShape RAIN_CAP_SHAPE = Shapes.or(
		Block.box(1.0, 12.0, 1.0, 15.0, 16.0, 15.0),
		Block.box(2.0, 0.0, 2.0, 4.0, 12.0, 4.0),
		Block.box(12.0, 0.0, 2.0, 14.0, 12.0, 4.0),
		Block.box(2.0, 0.0, 12.0, 4.0, 12.0, 14.0),
		Block.box(12.0, 0.0, 12.0, 14.0, 12.0, 14.0)
	);
	private static final VoxelShape LOOSE_FIELDSTONE_SHAPE = Shapes.or(
		Block.box(1.0, 0.0, 2.0, 6.0, 3.0, 7.0),
		Block.box(8.0, 0.0, 1.0, 14.0, 4.0, 6.0),
		Block.box(3.0, 0.0, 9.0, 9.0, 4.0, 15.0),
		Block.box(11.0, 0.0, 9.0, 15.0, 3.0, 14.0)
	);
	private static final VoxelShape RUBBLE_SHAPE = Shapes.or(
		Block.box(0.5, 0.0, 2.0, 7.0, 4.0, 9.0),
		Block.box(6.0, 0.0, 0.5, 14.5, 5.0, 7.0),
		Block.box(2.0, 0.0, 8.0, 10.0, 6.0, 15.5),
		Block.box(9.0, 0.0, 6.0, 15.5, 4.5, 14.0),
		Block.box(5.0, 4.0, 5.0, 12.0, 8.0, 12.0)
	);
	private static final VoxelShape SMALL_FOOTING_SHAPE =
		Block.box(3.0, 0.0, 3.0, 13.0, 6.0, 13.0);
	private static final VoxelShape LARGE_FOOTING_SHAPE =
		Block.box(1.0, 0.0, 1.0, 15.0, 8.0, 15.0);
	private static final VoxelShape FLAT_SILL_SHAPE =
		Block.box(0.0, 0.0, 2.0, 16.0, 5.0, 14.0);
	private static final VoxelShape FIELDSTONE_STEP_SHAPE =
		Block.box(0.0, 0.0, 0.0, 16.0, 6.0, 16.0);
	private static final VoxelShape CHANNEL_LINING_SHAPE = Shapes.or(
		Block.box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0),
		Block.box(0.0, 3.0, 0.0, 3.0, 6.0, 16.0),
		Block.box(13.0, 3.0, 0.0, 16.0, 6.0, 16.0)
	);
	private static final VoxelShape SHORT_PIER_SHAPE =
		Block.box(3.0, 0.0, 3.0, 13.0, 10.0, 13.0);
	private static final VoxelShape TALL_PIER_SHAPE =
		Block.box(3.0, 0.0, 3.0, 13.0, 16.0, 13.0);
	private static final VoxelShape WIDE_PIER_SHAPE =
		Block.box(1.0, 0.0, 1.0, 15.0, 13.0, 15.0);
	private static final VoxelShape IRREGULAR_PIER_SHAPE = Shapes.or(
		Block.box(2.0, 0.0, 2.0, 14.0, 5.0, 14.0),
		Block.box(3.0, 5.0, 1.5, 13.0, 10.0, 14.5),
		Block.box(2.5, 10.0, 3.0, 14.5, 16.0, 13.0)
	);
	private static final VoxelShape EDGE_SHAPE =
		Block.box(0.0, 0.0, 10.0, 16.0, 16.0, 16.0);
	private static final VoxelShape ARCH_WEDGE_SHAPE = Shapes.or(
		Block.box(0.0, 0.0, 10.0, 16.0, 6.0, 16.0),
		Block.box(3.0, 6.0, 10.0, 13.0, 11.0, 16.0),
		Block.box(6.0, 11.0, 10.0, 10.0, 16.0, 16.0)
	);
	private static final VoxelShape CHINKING_STRIP_SHAPE = Shapes.or(
		Block.box(0.0, 4.0, 14.0, 5.0, 12.0, 16.0),
		Block.box(5.25, 4.5, 14.25, 10.5, 11.5, 16.0),
		Block.box(10.75, 4.0, 14.0, 16.0, 12.0, 16.0)
	);
	private static final VoxelShape RAISED_HEARTH_SHAPE =
		Block.box(0.0, 0.0, 0.0, 16.0, 10.0, 16.0);
	private static final VoxelShape FIREPLACE_JAMB_SHAPE =
		Block.box(4.0, 0.0, 5.0, 12.0, 16.0, 16.0);
	private static final VoxelShape FIREPLACE_LINTEL_SHAPE =
		Block.box(0.0, 10.0, 8.0, 16.0, 16.0, 16.0);
	private static final VoxelShape FIREPLACE_SIDE_SHAPE =
		Block.box(0.0, 0.0, 10.0, 5.0, 16.0, 16.0);
	private static final VoxelShape ROOF_TRANSITION_SHAPE = Shapes.or(
		Block.box(0.0, 0.0, 6.0, 16.0, 5.0, 16.0),
		Block.box(2.0, 5.0, 8.0, 14.0, 10.0, 16.0),
		Block.box(4.0, 10.0, 10.0, 12.0, 16.0, 16.0)
	);
	private static final VoxelShape OPEN_FLUE_TOP_SHAPE = Shapes.or(
		Block.box(0.0, 0.0, 0.0, 4.0, 12.0, 16.0),
		Block.box(12.0, 0.0, 0.0, 16.0, 14.0, 16.0),
		Block.box(4.0, 0.0, 0.0, 12.0, 13.0, 4.0),
		Block.box(4.0, 0.0, 12.0, 12.0, 12.0, 16.0)
	);
	private static final VoxelShape SINGLE_FIELDSTONE_SHAPE =
		Block.box(5.0, 0.0, 5.0, 11.0, 4.0, 11.0);
	private static final VoxelShape FIELDSTONE_PILE_SHAPE = Shapes.or(
		Block.box(1.0, 0.0, 1.0, 8.0, 4.0, 8.0),
		Block.box(8.0, 0.0, 2.0, 15.0, 5.0, 9.0),
		Block.box(3.0, 0.0, 8.0, 11.0, 5.0, 15.0),
		Block.box(6.0, 5.0, 5.0, 13.0, 9.0, 12.0)
	);

	public static final Block FIELDSTONE_HEARTH = register(
		"fieldstone_hearth",
		SlabBlock::new,
		masonryProperties()
	);
	public static final StoneFireboxBlock FIELDSTONE_FIREBOX = register(
		"fieldstone_firebox",
		StoneFireboxBlock::new,
		masonryProperties()
			.noOcclusion()
			.lightLevel(state -> switch (state.getValue(StoneFireboxBlock.FIRE_STATE)) {
				case COLD, ASH -> 0;
				case EMBER -> 5;
				case ACTIVE -> 12;
			})
	);
	public static final StoneHearthFacingBlock FIELDSTONE_FIREBOX_BACK =
		facing("fieldstone_firebox_back", FIREBOX_BACK_SHAPE);
	public static final Block FIELDSTONE_FIREBOX_WALL = register(
		"fieldstone_firebox_wall",
		WallBlock::new,
		masonryProperties().forceSolidOn()
	);
	public static final StoneHearthFacingBlock FIELDSTONE_FIREBOX_ARCH =
		facing("fieldstone_firebox_arch", FIREBOX_ARCH_SHAPE);
	public static final StoneHearthFacingBlock FIELDSTONE_FIREBOX_KEYSTONE =
		facing("fieldstone_firebox_keystone", KEYSTONE_SHAPE);
	public static final StoneHearthFacingBlock FIELDSTONE_CHIMNEY_CORNER =
		facing("fieldstone_chimney_corner", CORNER_SHAPE);
	public static final Block FIELDSTONE_CHIMNEY_SHOULDER = register(
		"fieldstone_chimney_shoulder",
		properties -> new StairBlock(ModBlocks.FIELDSTONE.defaultBlockState(), properties),
		masonryProperties()
	);
	public static final StoneHearthFacingBlock FIELDSTONE_CHIMNEY_THROAT =
		facing("fieldstone_chimney_throat", THROAT_SHAPE);
	public static final StoneHearthFacingBlock FIELDSTONE_CHIMNEY_SMOKE_CHAMBER =
		facing("fieldstone_chimney_smoke_chamber", SMOKE_CHAMBER_SHAPE);
	public static final StoneHearthFacingBlock FIELDSTONE_CHIMNEY_FLUE =
		facing("fieldstone_chimney_flue", FLUE_SHAPE);
	public static final Block FIELDSTONE_CHIMNEY_CAP = register(
		"fieldstone_chimney_cap",
		SlabBlock::new,
		masonryProperties()
	);
	public static final StoneHearthFacingBlock FIELDSTONE_CHIMNEY_RAIN_CAP =
		facing("fieldstone_chimney_rain_cap", RAIN_CAP_SHAPE);

	public static final Block SOOT_STAINED_FIELDSTONE = register(
		"soot_stained_fieldstone",
		Block::new,
		masonryProperties()
	);
	public static final Block WHITEWASHED_FIELDSTONE = register(
		"whitewashed_fieldstone",
		Block::new,
		masonryProperties()
	);
	public static final StoneHearthFacingBlock LOOSE_FIELDSTONE = facing(
		"loose_fieldstone",
		LOOSE_FIELDSTONE_SHAPE
	);
	public static final StoneHearthFacingBlock FIELDSTONE_RUBBLE = facing(
		"fieldstone_rubble",
		RUBBLE_SHAPE
	);
	public static final StoneHearthFacingBlock SMALL_FIELDSTONE_FOOTING =
		facing("small_fieldstone_footing", SMALL_FOOTING_SHAPE);
	public static final StoneHearthFacingBlock LARGE_FIELDSTONE_FOOTING =
		facing("large_fieldstone_footing", LARGE_FOOTING_SHAPE);
	public static final StoneHearthFacingBlock FLAT_FIELDSTONE_SILL =
		facing("flat_fieldstone_sill", FLAT_SILL_SHAPE);
	public static final StoneHearthFacingBlock FIELDSTONE_STEP =
		facing("fieldstone_step", FIELDSTONE_STEP_SHAPE);
	public static final Block FIELDSTONE_RETAINING_WALL = register(
		"fieldstone_retaining_wall",
		WallBlock::new,
		masonryProperties().forceSolidOn()
	);
	public static final StoneHearthFacingBlock FIELDSTONE_CHANNEL_LINING =
		facing("fieldstone_channel_lining", CHANNEL_LINING_SHAPE);
	public static final StoneHearthFacingBlock SHORT_FIELDSTONE_PIER =
		facing("short_fieldstone_pier", SHORT_PIER_SHAPE);
	public static final StoneHearthFacingBlock TALL_FIELDSTONE_PIER =
		facing("tall_fieldstone_pier", TALL_PIER_SHAPE);
	public static final StoneHearthFacingBlock WIDE_FIELDSTONE_PIER =
		facing("wide_fieldstone_pier", WIDE_PIER_SHAPE);
	public static final StoneHearthFacingBlock IRREGULAR_FIELDSTONE_PIER =
		facing("irregular_fieldstone_pier", IRREGULAR_PIER_SHAPE);

	public static final StoneHearthFacingBlock IRREGULAR_FIELDSTONE_CORNER =
		facing("irregular_fieldstone_corner", CORNER_SHAPE);
	public static final StoneHearthFacingBlock IRREGULAR_FIELDSTONE_EDGE =
		facing("irregular_fieldstone_edge", EDGE_SHAPE);
	public static final StoneHearthFacingBlock IRREGULAR_FIELDSTONE_ARCH_WEDGE =
		facing("irregular_fieldstone_arch_wedge", ARCH_WEDGE_SHAPE);
	public static final StoneHearthFacingBlock DRESSED_FIELDSTONE_CORNER =
		facing("dressed_fieldstone_corner", CORNER_SHAPE);
	public static final StoneHearthFacingBlock DRESSED_FIELDSTONE_EDGE =
		facing("dressed_fieldstone_edge", EDGE_SHAPE);
	public static final StoneHearthFacingBlock DRESSED_FIELDSTONE_ARCH_WEDGE =
		facing("dressed_fieldstone_arch_wedge", ARCH_WEDGE_SHAPE);

	public static final Block MUD_MORTARED_FIELDSTONE = register(
		"mud_mortared_fieldstone",
		Block::new,
		masonryProperties()
	);
	public static final Block DRIED_MUD_DAUB = register(
		"dried_mud_daub",
		Block::new,
		chinkingProperties()
	);
	public static final StoneHearthFacingBlock WOOD_SPLINT_CHINKING = facing(
		"wood_splint_chinking",
		CHINKING_STRIP_SHAPE,
		BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)
			.noOcclusion()
	);
	public static final StoneHearthFacingBlock CRACKED_MUD_CHINKING = facing(
		"cracked_mud_chinking",
		CHINKING_STRIP_SHAPE,
		chinkingProperties().noOcclusion()
	);
	public static final StoneHearthFacingBlock REPAIRED_MUD_CHINKING = facing(
		"repaired_mud_chinking",
		CHINKING_STRIP_SHAPE,
		chinkingProperties().noOcclusion()
	);

	public static final StoneHearthFacingBlock RAISED_FIELDSTONE_HEARTH =
		facing("raised_fieldstone_hearth", RAISED_HEARTH_SHAPE);
	public static final StoneHearthFacingBlock FIELDSTONE_FIREPLACE_JAMB =
		facing("fieldstone_fireplace_jamb", FIREPLACE_JAMB_SHAPE);
	public static final StoneHearthFacingBlock FIELDSTONE_FIREPLACE_LINTEL =
		facing("fieldstone_fireplace_lintel", FIREPLACE_LINTEL_SHAPE);
	public static final StoneHearthFacingBlock FIELDSTONE_FIREPLACE_SIDE =
		facing("fieldstone_fireplace_side", FIREPLACE_SIDE_SHAPE);
	public static final StoneHearthFacingBlock FIELDSTONE_FIREPLACE_CORNER =
		facing("fieldstone_fireplace_corner", CORNER_SHAPE);
	public static final StoneHearthFacingBlock FIELDSTONE_CHIMNEY_ROOF_TRANSITION =
		facing("fieldstone_chimney_roof_transition", ROOF_TRANSITION_SHAPE);
	public static final StoneHearthFacingBlock FIELDSTONE_CHIMNEY_OPEN_FLUE_TOP =
		facing("fieldstone_chimney_open_flue_top", OPEN_FLUE_TOP_SHAPE);
	public static final Block FIELDSTONE_CHIMNEY_BASE = register(
		"fieldstone_chimney_base",
		Block::new,
		masonryProperties()
	);
	public static final Block FIELDSTONE_CHIMNEY_LOWER_BODY = register(
		"fieldstone_chimney_lower_body",
		Block::new,
		masonryProperties()
	);
	public static final Block FIELDSTONE_CHIMNEY_UPPER_STACK = register(
		"fieldstone_chimney_upper_stack",
		Block::new,
		masonryProperties()
	);

	public static final Block PEELING_WHITEWASHED_FIELDSTONE = register(
		"peeling_whitewashed_fieldstone",
		Block::new,
		masonryProperties()
	);
	public static final Block SOOT_STAINED_WHITEWASHED_FIELDSTONE = register(
		"soot_stained_whitewashed_fieldstone",
		Block::new,
		masonryProperties()
	);
	public static final StoneHearthFacingBlock SINGLE_FIELDSTONE =
		facing("single_fieldstone", SINGLE_FIELDSTONE_SHAPE);
	public static final StoneHearthFacingBlock FIELDSTONE_PILE =
		facing("fieldstone_pile", FIELDSTONE_PILE_SHAPE);
	public static final Block BROKEN_FIELDSTONE_MASONRY = register(
		"broken_fieldstone_masonry",
		Block::new,
		masonryProperties()
	);

	public static final Block PACKED_EARTH = register(
		"packed_earth",
		Block::new,
		earthProperties()
	);
	public static final Block MUDDY_EARTH = register(
		"muddy_earth",
		Block::new,
		earthProperties().friction(0.72F)
	);
	public static final StoneHearthLayerBlock ASH_LAYER = layer("ash_layer");
	public static final StoneHearthLayerBlock SOOT_LAYER = layer("soot_layer");
	public static final StoneHearthLayerBlock HEARTH_ASH_SCATTER =
		layer("hearth_ash_scatter");
	public static final StoneHearthLayerBlock CLAY_SPILL_LAYER =
		layer("clay_spill_layer");
	public static final StoneHearthLayerBlock PACKED_EARTH_LAYER =
		layer("packed_earth_layer");
	public static final StoneHearthLayerBlock DAMP_EARTH_LAYER =
		layer("damp_earth_layer");
	public static final StoneHearthLayerBlock MUDDY_EARTH_LAYER =
		layer("muddy_earth_layer");
	public static final StoneHearthLayerBlock COLD_COAL_SCATTER =
		layer("cold_coal_scatter");
	public static final Block CLAY_CHINKING = register(
		"clay_chinking",
		Block::new,
		chinkingProperties()
	);
	public static final Block MUD_CHINKING = register(
		"mud_chinking",
		Block::new,
		chinkingProperties()
	);

	public static final List<Block> HEARTH_COMPONENTS = List.of(
		FIELDSTONE_HEARTH,
		FIELDSTONE_FIREBOX,
		FIELDSTONE_FIREBOX_BACK,
		FIELDSTONE_FIREBOX_WALL,
		FIELDSTONE_FIREBOX_ARCH,
		FIELDSTONE_FIREBOX_KEYSTONE,
		FIELDSTONE_CHIMNEY_CORNER,
		FIELDSTONE_CHIMNEY_SHOULDER,
		FIELDSTONE_CHIMNEY_THROAT,
		FIELDSTONE_CHIMNEY_SMOKE_CHAMBER,
		FIELDSTONE_CHIMNEY_FLUE,
		FIELDSTONE_CHIMNEY_CAP,
		FIELDSTONE_CHIMNEY_RAIN_CAP,
		RAISED_FIELDSTONE_HEARTH,
		FIELDSTONE_FIREPLACE_JAMB,
		FIELDSTONE_FIREPLACE_LINTEL,
		FIELDSTONE_FIREPLACE_SIDE,
		FIELDSTONE_FIREPLACE_CORNER,
		FIELDSTONE_CHIMNEY_ROOF_TRANSITION,
		FIELDSTONE_CHIMNEY_OPEN_FLUE_TOP,
		FIELDSTONE_CHIMNEY_BASE,
		FIELDSTONE_CHIMNEY_LOWER_BODY,
		FIELDSTONE_CHIMNEY_UPPER_STACK
	);
	public static final List<Block> FOUNDATION_COMPONENTS = List.of(
		SMALL_FIELDSTONE_FOOTING,
		LARGE_FIELDSTONE_FOOTING,
		FLAT_FIELDSTONE_SILL,
		FIELDSTONE_STEP,
		FIELDSTONE_RETAINING_WALL,
		FIELDSTONE_CHANNEL_LINING,
		SHORT_FIELDSTONE_PIER,
		TALL_FIELDSTONE_PIER,
		WIDE_FIELDSTONE_PIER,
		IRREGULAR_FIELDSTONE_PIER
	);
	public static final List<Block> MASONRY_SHAPES = List.of(
		IRREGULAR_FIELDSTONE_CORNER,
		IRREGULAR_FIELDSTONE_EDGE,
		IRREGULAR_FIELDSTONE_ARCH_WEDGE,
		DRESSED_FIELDSTONE_CORNER,
		DRESSED_FIELDSTONE_EDGE,
		DRESSED_FIELDSTONE_ARCH_WEDGE
	);
	public static final List<Block> MASONRY_MATERIALS = List.of(
		SOOT_STAINED_FIELDSTONE,
		WHITEWASHED_FIELDSTONE,
		LOOSE_FIELDSTONE,
		FIELDSTONE_RUBBLE,
		CLAY_CHINKING,
		MUD_CHINKING,
		MUD_MORTARED_FIELDSTONE,
		DRIED_MUD_DAUB,
		WOOD_SPLINT_CHINKING,
		CRACKED_MUD_CHINKING,
		REPAIRED_MUD_CHINKING,
		PEELING_WHITEWASHED_FIELDSTONE,
		SOOT_STAINED_WHITEWASHED_FIELDSTONE,
		SINGLE_FIELDSTONE,
		FIELDSTONE_PILE,
		BROKEN_FIELDSTONE_MASONRY
	);
	public static final List<Block> EARTH_FLOORS = List.of(
		PACKED_EARTH,
		MUDDY_EARTH
	);
	public static final List<Block> FLOOR_CLUTTER = List.of(
		ASH_LAYER,
		SOOT_LAYER,
		HEARTH_ASH_SCATTER,
		CLAY_SPILL_LAYER,
		PACKED_EARTH_LAYER,
		DAMP_EARTH_LAYER,
		MUDDY_EARTH_LAYER,
		COLD_COAL_SCATTER
	);
	public static final List<Block> ALL_BLOCKS = List.of(
		FIELDSTONE_HEARTH,
		FIELDSTONE_FIREBOX,
		FIELDSTONE_FIREBOX_BACK,
		FIELDSTONE_FIREBOX_WALL,
		FIELDSTONE_FIREBOX_ARCH,
		FIELDSTONE_FIREBOX_KEYSTONE,
		FIELDSTONE_CHIMNEY_CORNER,
		FIELDSTONE_CHIMNEY_SHOULDER,
		FIELDSTONE_CHIMNEY_THROAT,
		FIELDSTONE_CHIMNEY_SMOKE_CHAMBER,
		FIELDSTONE_CHIMNEY_FLUE,
		FIELDSTONE_CHIMNEY_CAP,
		FIELDSTONE_CHIMNEY_RAIN_CAP,
		SOOT_STAINED_FIELDSTONE,
		WHITEWASHED_FIELDSTONE,
		LOOSE_FIELDSTONE,
		FIELDSTONE_RUBBLE,
		PACKED_EARTH,
		MUDDY_EARTH,
		ASH_LAYER,
		SOOT_LAYER,
		HEARTH_ASH_SCATTER,
		CLAY_CHINKING,
		MUD_CHINKING,
		SMALL_FIELDSTONE_FOOTING,
		LARGE_FIELDSTONE_FOOTING,
		FLAT_FIELDSTONE_SILL,
		FIELDSTONE_STEP,
		FIELDSTONE_RETAINING_WALL,
		FIELDSTONE_CHANNEL_LINING,
		SHORT_FIELDSTONE_PIER,
		TALL_FIELDSTONE_PIER,
		WIDE_FIELDSTONE_PIER,
		IRREGULAR_FIELDSTONE_PIER,
		IRREGULAR_FIELDSTONE_CORNER,
		IRREGULAR_FIELDSTONE_EDGE,
		IRREGULAR_FIELDSTONE_ARCH_WEDGE,
		DRESSED_FIELDSTONE_CORNER,
		DRESSED_FIELDSTONE_EDGE,
		DRESSED_FIELDSTONE_ARCH_WEDGE,
		MUD_MORTARED_FIELDSTONE,
		DRIED_MUD_DAUB,
		WOOD_SPLINT_CHINKING,
		CRACKED_MUD_CHINKING,
		REPAIRED_MUD_CHINKING,
		RAISED_FIELDSTONE_HEARTH,
		FIELDSTONE_FIREPLACE_JAMB,
		FIELDSTONE_FIREPLACE_LINTEL,
		FIELDSTONE_FIREPLACE_SIDE,
		FIELDSTONE_FIREPLACE_CORNER,
		FIELDSTONE_CHIMNEY_ROOF_TRANSITION,
		FIELDSTONE_CHIMNEY_OPEN_FLUE_TOP,
		FIELDSTONE_CHIMNEY_BASE,
		FIELDSTONE_CHIMNEY_LOWER_BODY,
		FIELDSTONE_CHIMNEY_UPPER_STACK,
		PEELING_WHITEWASHED_FIELDSTONE,
		SOOT_STAINED_WHITEWASHED_FIELDSTONE,
		SINGLE_FIELDSTONE,
		FIELDSTONE_PILE,
		BROKEN_FIELDSTONE_MASONRY,
		CLAY_SPILL_LAYER,
		PACKED_EARTH_LAYER,
		DAMP_EARTH_LAYER,
		MUDDY_EARTH_LAYER,
		COLD_COAL_SCATTER
	);
	public static final List<Block> PICKAXE_MINEABLE = List.of(
		FIELDSTONE_HEARTH,
		FIELDSTONE_FIREBOX,
		FIELDSTONE_FIREBOX_BACK,
		FIELDSTONE_FIREBOX_WALL,
		FIELDSTONE_FIREBOX_ARCH,
		FIELDSTONE_FIREBOX_KEYSTONE,
		FIELDSTONE_CHIMNEY_CORNER,
		FIELDSTONE_CHIMNEY_SHOULDER,
		FIELDSTONE_CHIMNEY_THROAT,
		FIELDSTONE_CHIMNEY_SMOKE_CHAMBER,
		FIELDSTONE_CHIMNEY_FLUE,
		FIELDSTONE_CHIMNEY_CAP,
		FIELDSTONE_CHIMNEY_RAIN_CAP,
		SOOT_STAINED_FIELDSTONE,
		WHITEWASHED_FIELDSTONE,
		LOOSE_FIELDSTONE,
		FIELDSTONE_RUBBLE,
		SMALL_FIELDSTONE_FOOTING,
		LARGE_FIELDSTONE_FOOTING,
		FLAT_FIELDSTONE_SILL,
		FIELDSTONE_STEP,
		FIELDSTONE_RETAINING_WALL,
		FIELDSTONE_CHANNEL_LINING,
		SHORT_FIELDSTONE_PIER,
		TALL_FIELDSTONE_PIER,
		WIDE_FIELDSTONE_PIER,
		IRREGULAR_FIELDSTONE_PIER,
		IRREGULAR_FIELDSTONE_CORNER,
		IRREGULAR_FIELDSTONE_EDGE,
		IRREGULAR_FIELDSTONE_ARCH_WEDGE,
		DRESSED_FIELDSTONE_CORNER,
		DRESSED_FIELDSTONE_EDGE,
		DRESSED_FIELDSTONE_ARCH_WEDGE,
		MUD_MORTARED_FIELDSTONE,
		RAISED_FIELDSTONE_HEARTH,
		FIELDSTONE_FIREPLACE_JAMB,
		FIELDSTONE_FIREPLACE_LINTEL,
		FIELDSTONE_FIREPLACE_SIDE,
		FIELDSTONE_FIREPLACE_CORNER,
		FIELDSTONE_CHIMNEY_ROOF_TRANSITION,
		FIELDSTONE_CHIMNEY_OPEN_FLUE_TOP,
		FIELDSTONE_CHIMNEY_BASE,
		FIELDSTONE_CHIMNEY_LOWER_BODY,
		FIELDSTONE_CHIMNEY_UPPER_STACK,
		PEELING_WHITEWASHED_FIELDSTONE,
		SOOT_STAINED_WHITEWASHED_FIELDSTONE,
		SINGLE_FIELDSTONE,
		FIELDSTONE_PILE,
		BROKEN_FIELDSTONE_MASONRY
	);
	public static final List<Block> SHOVEL_MINEABLE = List.of(
		PACKED_EARTH,
		MUDDY_EARTH,
		ASH_LAYER,
		SOOT_LAYER,
		HEARTH_ASH_SCATTER,
		CLAY_CHINKING,
		MUD_CHINKING,
		DRIED_MUD_DAUB,
		CRACKED_MUD_CHINKING,
		REPAIRED_MUD_CHINKING,
		CLAY_SPILL_LAYER,
		PACKED_EARTH_LAYER,
		DAMP_EARTH_LAYER,
		MUDDY_EARTH_LAYER,
		COLD_COAL_SCATTER
	);
	public static final List<Block> AXE_MINEABLE = List.of(
		WOOD_SPLINT_CHINKING
	);
	public static final Map<String, String> LANGUAGE_ENTRIES = Map.ofEntries(
		Map.entry("fieldstone_hearth", "Fieldstone Hearth"),
		Map.entry("fieldstone_firebox", "Fieldstone Firebox"),
		Map.entry("fieldstone_firebox_back", "Fieldstone Firebox Back"),
		Map.entry("fieldstone_firebox_wall", "Fieldstone Firebox Wall"),
		Map.entry("fieldstone_firebox_arch", "Fieldstone Firebox Arch"),
		Map.entry("fieldstone_firebox_keystone", "Fieldstone Firebox Keystone"),
		Map.entry("fieldstone_chimney_corner", "Fieldstone Chimney Corner"),
		Map.entry("fieldstone_chimney_shoulder", "Fieldstone Chimney Shoulder"),
		Map.entry("fieldstone_chimney_throat", "Fieldstone Chimney Throat"),
		Map.entry("fieldstone_chimney_smoke_chamber", "Fieldstone Smoke Chamber"),
		Map.entry("fieldstone_chimney_flue", "Fieldstone Chimney Flue"),
		Map.entry("fieldstone_chimney_cap", "Fieldstone Chimney Cap"),
		Map.entry("fieldstone_chimney_rain_cap", "Fieldstone Chimney Rain Cap"),
		Map.entry("soot_stained_fieldstone", "Soot-Stained Fieldstone"),
		Map.entry("whitewashed_fieldstone", "Whitewashed Fieldstone"),
		Map.entry("loose_fieldstone", "Loose Fieldstone"),
		Map.entry("fieldstone_rubble", "Fieldstone Rubble"),
		Map.entry("packed_earth", "Packed Earth"),
		Map.entry("muddy_earth", "Muddy Earth"),
		Map.entry("ash_layer", "Ash Layer"),
		Map.entry("soot_layer", "Soot Layer"),
		Map.entry("hearth_ash_scatter", "Hearth Ash Scatter"),
		Map.entry("clay_chinking", "Clay Chinking"),
		Map.entry("mud_chinking", "Mud Chinking"),
		Map.entry("small_fieldstone_footing", "Small Fieldstone Footing"),
		Map.entry("large_fieldstone_footing", "Large Fieldstone Footing"),
		Map.entry("flat_fieldstone_sill", "Flat Fieldstone Sill"),
		Map.entry("fieldstone_step", "Fieldstone Step"),
		Map.entry("fieldstone_retaining_wall", "Fieldstone Retaining Wall"),
		Map.entry("fieldstone_channel_lining", "Fieldstone Channel Lining"),
		Map.entry("short_fieldstone_pier", "Short Fieldstone Pier"),
		Map.entry("tall_fieldstone_pier", "Tall Fieldstone Pier"),
		Map.entry("wide_fieldstone_pier", "Wide Fieldstone Pier"),
		Map.entry("irregular_fieldstone_pier", "Irregular Fieldstone Pier"),
		Map.entry("irregular_fieldstone_corner", "Irregular Fieldstone Corner"),
		Map.entry("irregular_fieldstone_edge", "Irregular Fieldstone Edge"),
		Map.entry(
			"irregular_fieldstone_arch_wedge",
			"Irregular Fieldstone Arch Wedge"
		),
		Map.entry("dressed_fieldstone_corner", "Dressed Fieldstone Corner"),
		Map.entry("dressed_fieldstone_edge", "Dressed Fieldstone Edge"),
		Map.entry(
			"dressed_fieldstone_arch_wedge",
			"Dressed Fieldstone Arch Wedge"
		),
		Map.entry("mud_mortared_fieldstone", "Mud-Mortared Fieldstone"),
		Map.entry("dried_mud_daub", "Dried Mud Daub"),
		Map.entry("wood_splint_chinking", "Wood-Splint Chinking"),
		Map.entry("cracked_mud_chinking", "Cracked Mud Chinking"),
		Map.entry("repaired_mud_chinking", "Repaired Mud Chinking"),
		Map.entry("raised_fieldstone_hearth", "Raised Fieldstone Hearth"),
		Map.entry("fieldstone_fireplace_jamb", "Fieldstone Fireplace Jamb"),
		Map.entry("fieldstone_fireplace_lintel", "Fieldstone Fireplace Lintel"),
		Map.entry("fieldstone_fireplace_side", "Fieldstone Fireplace Side"),
		Map.entry("fieldstone_fireplace_corner", "Fieldstone Fireplace Corner"),
		Map.entry(
			"fieldstone_chimney_roof_transition",
			"Fieldstone Chimney Roof Transition"
		),
		Map.entry(
			"fieldstone_chimney_open_flue_top",
			"Open Fieldstone Flue Top"
		),
		Map.entry("fieldstone_chimney_base", "Fieldstone Chimney Base"),
		Map.entry(
			"fieldstone_chimney_lower_body",
			"Fieldstone Chimney Lower Body"
		),
		Map.entry(
			"fieldstone_chimney_upper_stack",
			"Fieldstone Chimney Upper Stack"
		),
		Map.entry(
			"peeling_whitewashed_fieldstone",
			"Peeling Whitewashed Fieldstone"
		),
		Map.entry(
			"soot_stained_whitewashed_fieldstone",
			"Soot-Stained Whitewashed Fieldstone"
		),
		Map.entry("single_fieldstone", "Single Fieldstone"),
		Map.entry("fieldstone_pile", "Fieldstone Pile"),
		Map.entry("broken_fieldstone_masonry", "Broken Fieldstone Masonry"),
		Map.entry("clay_spill_layer", "Clay Spill"),
		Map.entry("packed_earth_layer", "Packed-Earth Spill"),
		Map.entry("damp_earth_layer", "Damp-Earth Spill"),
		Map.entry("muddy_earth_layer", "Muddy-Earth Spill"),
		Map.entry("cold_coal_scatter", "Cold-Coal Scatter")
	);

	private static boolean initialized;

	private ModStoneHearthBlocks() {
	}

	public static synchronized void initialize() {
		if (initialized) {
			return;
		}
		initialized = true;

		ModContentCatalog.register(
			Category.BUILDING_MATERIALS,
			join(
				HEARTH_COMPONENTS,
				FOUNDATION_COMPONENTS,
				MASONRY_SHAPES,
				MASONRY_MATERIALS
			)
		);
		ModContentCatalog.register(
			Category.NATURE_FARMING,
			join(EARTH_FLOORS, FLOOR_CLUTTER)
		);
		FlammableBlockRegistry.getDefaultInstance().add(
			WOOD_SPLINT_CHINKING,
			5,
			20
		);
	}

	@SafeVarargs
	private static ItemLike[] join(List<Block>... groups) {
		int size = 0;
		for (List<Block> group : groups) {
			size += group.size();
		}
		ItemLike[] result = new ItemLike[size];
		int index = 0;
		for (List<Block> group : groups) {
			for (Block block : group) {
				result[index++] = block;
			}
		}
		return result;
	}

	private static StoneHearthFacingBlock facing(
		String name,
		VoxelShape northShape
	) {
		return facing(name, northShape, masonryProperties().noOcclusion());
	}

	private static StoneHearthFacingBlock facing(
		String name,
		VoxelShape northShape,
		BlockBehaviour.Properties properties
	) {
		return register(
			name,
			blockProperties -> new StoneHearthFacingBlock(
				blockProperties,
				northShape
			),
			properties
		);
	}

	private static StoneHearthLayerBlock layer(String name) {
		return register(
			name,
			StoneHearthLayerBlock::new,
			BlockBehaviour.Properties.of()
				.mapColor(MapColor.TERRACOTTA_BROWN)
				.replaceable()
				.noCollision()
				.noOcclusion()
				.instabreak()
				.sound(SoundType.SAND)
				.pushReaction(PushReaction.DESTROY)
		);
	}

	private static BlockBehaviour.Properties masonryProperties() {
		return BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE);
	}

	private static BlockBehaviour.Properties earthProperties() {
		return BlockBehaviour.Properties.ofFullCopy(Blocks.PACKED_MUD);
	}

	private static BlockBehaviour.Properties chinkingProperties() {
		return BlockBehaviour.Properties.ofFullCopy(Blocks.PACKED_MUD)
			.mapColor(MapColor.TERRACOTTA_BROWN)
			.sound(SoundType.PACKED_MUD);
	}

	private static <T extends Block> T register(
		String name,
		Function<BlockBehaviour.Properties, T> factory,
		BlockBehaviour.Properties properties
	) {
		ResourceKey<Block> blockKey = ResourceKey.create(
			Registries.BLOCK,
			BeforeTheBlight.id(name)
		);
		T block = factory.apply(properties.setId(blockKey));
		ResourceKey<Item> itemKey = ResourceKey.create(
			Registries.ITEM,
			BeforeTheBlight.id(name)
		);
		BlockItem item = new BlockItem(
			block,
			new Item.Properties()
				.setId(itemKey)
				.useBlockDescriptionPrefix()
		);
		item.registerBlocks(Item.BY_BLOCK, item);
		Registry.register(BuiltInRegistries.ITEM, itemKey, item);
		return Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
	}
}
