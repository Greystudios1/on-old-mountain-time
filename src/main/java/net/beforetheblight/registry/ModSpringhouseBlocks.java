package net.beforetheblight.registry;

import java.util.List;
import java.util.function.Function;

import net.beforetheblight.BeforeTheBlight;
import net.beforetheblight.block.springhouse.HollowedChestnutTroughBlock;
import net.beforetheblight.block.springhouse.HollowLimbSpoutBlock;
import net.beforetheblight.block.springhouse.SpringhousePropBlock;
import net.beforetheblight.block.springhouse.WoodenLouverBlock;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * Isolated registration surface for the command-placeable springhouse polish
 * palette.
 *
 * <p>This class deliberately has no world-generation hooks. The owning mod
 * entrypoint must call {@link #initialize()} before the creative tabs are
 * built. The shared furniture and door modules supply the general rough wall
 * shelf and functional pegged springhouse door, so this palette does not
 * register duplicate substitutes for them.</p>
 */
public final class ModSpringhouseBlocks {
	private static boolean initialized;

	public static final HollowedChestnutTroughBlock HOLLOWED_CHESTNUT_TROUGH =
		register(
			"hollowed_chestnut_trough",
			HollowedChestnutTroughBlock::new,
			woodDetailProperties(),
			true
		);

	public static final HollowLimbSpoutBlock HOLLOW_LIMB_SPOUT = register(
		"hollow_limb_spout",
		HollowLimbSpoutBlock::new,
		woodDetailProperties(),
		true
	);

	public static final SpringhousePropBlock DAIRY_WORK_TABLE = register(
		"dairy_work_table",
		properties -> new SpringhousePropBlock(
			properties,
			SpringhousePropBlock.ShapeKind.TABLE
		),
		woodFurnitureProperties(),
		true
	);

	public static final SpringhousePropBlock CROCK_STAND = register(
		"crock_stand",
		properties -> new SpringhousePropBlock(
			properties,
			SpringhousePropBlock.ShapeKind.STAND
		),
		woodFurnitureProperties(),
		true
	);

	public static final WoodenLouverBlock WOODEN_LOUVER = register(
		"wooden_louver",
		WoodenLouverBlock::new,
		woodDetailProperties(),
		true
	);

	public static final SpringhousePropBlock WOODEN_MILK_PAIL = register(
		"wooden_milk_pail",
		properties -> new SpringhousePropBlock(
			properties,
			SpringhousePropBlock.ShapeKind.PAIL
		),
		woodFurnitureProperties(),
		true
	);

	public static final SpringhousePropBlock STONEWARE_MILK_CROCK = register(
		"stoneware_milk_crock",
		properties -> new SpringhousePropBlock(
			properties,
			SpringhousePropBlock.ShapeKind.CROCK
		),
		ceramicProperties(),
		true
	);

	public static final SpringhousePropBlock SHALLOW_MILK_PAN = register(
		"shallow_milk_pan",
		properties -> new SpringhousePropBlock(
			properties,
			SpringhousePropBlock.ShapeKind.PAN
		),
		ceramicProperties(),
		true
	);

	public static final SpringhousePropBlock DASHER_CHURN = register(
		"dasher_churn",
		properties -> new SpringhousePropBlock(
			properties,
			SpringhousePropBlock.ShapeKind.CHURN
		),
		woodFurnitureProperties(),
		true
	);

	public static final SpringhousePropBlock BUTTER_CROCK = register(
		"butter_crock",
		properties -> new SpringhousePropBlock(
			properties,
			SpringhousePropBlock.ShapeKind.BUTTER_CROCK
		),
		ceramicProperties(),
		true
	);

	public static final SpringhousePropBlock CHEESE_WHEEL = register(
		"cheese_wheel",
		properties -> new SpringhousePropBlock(
			properties,
			SpringhousePropBlock.ShapeKind.CHEESE_WHEEL
		),
		foodPropProperties(),
		true
	);

	public static final Block DAMP_EARTH = register(
		"damp_earth",
		Block::new,
		BlockBehaviour.Properties.ofFullCopy(Blocks.MUD),
		true
	);

	public static final Block DAMP_FIELDSTONE = register(
		"damp_fieldstone",
		Block::new,
		BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE),
		true
	);

	public static final Block MOSSY_FIELDSTONE = register(
		"mossy_fieldstone",
		Block::new,
		BlockBehaviour.Properties.ofFullCopy(Blocks.MOSSY_COBBLESTONE),
		true
	);

	/**
	 * Blocks used as part of the springhouse shell or watercourse.
	 */
	public static final List<Block> BUILDING = List.of(
		HOLLOWED_CHESTNUT_TROUGH,
		HOLLOW_LIMB_SPOUT,
		WOODEN_LOUVER,
		DAMP_EARTH,
		DAMP_FIELDSTONE,
		MOSSY_FIELDSTONE
	);

	/**
	 * Self-contained furniture and dairy props registered by this module.
	 */
	public static final List<Block> FURNITURE = List.of(
		DAIRY_WORK_TABLE,
		CROCK_STAND,
		WOODEN_MILK_PAIL,
		STONEWARE_MILK_CROCK,
		SHALLOW_MILK_PAN,
		DASHER_CHURN,
		BUTTER_CROCK,
		CHEESE_WHEEL
	);

	public static final List<Block> ALL = List.of(
		HOLLOWED_CHESTNUT_TROUGH,
		HOLLOW_LIMB_SPOUT,
		DAIRY_WORK_TABLE,
		CROCK_STAND,
		WOODEN_LOUVER,
		WOODEN_MILK_PAIL,
		STONEWARE_MILK_CROCK,
		SHALLOW_MILK_PAN,
		DASHER_CHURN,
		BUTTER_CROCK,
		CHEESE_WHEEL,
		DAMP_EARTH,
		DAMP_FIELDSTONE,
		MOSSY_FIELDSTONE
	);

	private ModSpringhouseBlocks() {
	}

	private static BlockBehaviour.Properties woodDetailProperties() {
		return BlockBehaviour.Properties.of()
			.mapColor(MapColor.WOOD)
			.strength(1.5F, 2.5F)
			.sound(SoundType.WOOD)
			.noOcclusion()
			.ignitedByLava();
	}

	private static BlockBehaviour.Properties woodFurnitureProperties() {
		return BlockBehaviour.Properties.of()
			.mapColor(MapColor.WOOD)
			.strength(1.0F, 2.0F)
			.sound(SoundType.WOOD)
			.noOcclusion()
			.ignitedByLava();
	}

	private static BlockBehaviour.Properties ceramicProperties() {
		return BlockBehaviour.Properties.of()
			.mapColor(MapColor.TERRACOTTA_BROWN)
			.strength(0.8F, 1.5F)
			.sound(SoundType.DECORATED_POT)
			.noOcclusion();
	}

	private static BlockBehaviour.Properties foodPropProperties() {
		return BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_YELLOW)
			.strength(0.3F)
			.sound(SoundType.WOOL)
			.noOcclusion();
	}

	private static <T extends Block> T register(
		String name,
		Function<BlockBehaviour.Properties, T> blockFactory,
		BlockBehaviour.Properties properties,
		boolean registerBlockItem
	) {
		ResourceKey<Block> blockKey = ResourceKey.create(
			Registries.BLOCK,
			BeforeTheBlight.id(name)
		);
		T block = blockFactory.apply(properties.setId(blockKey));

		if (registerBlockItem) {
			ResourceKey<Item> itemKey = ResourceKey.create(
				Registries.ITEM,
				BeforeTheBlight.id(name)
			);
			BlockItem blockItem = new BlockItem(
				block,
				new Item.Properties()
					.setId(itemKey)
					.useBlockDescriptionPrefix()
			);
			blockItem.registerBlocks(Item.BY_BLOCK, blockItem);
			Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);
		}

		return Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
	}

	/**
	 * Forces registration and installs wood fire-spread behavior. This method
	 * intentionally performs no structure, placed-feature, or biome work.
	 */
	public static synchronized void initialize() {
		if (initialized) {
			return;
		}

		FlammableBlockRegistry flammable = FlammableBlockRegistry.getDefaultInstance();
		for (Block block : List.of(
			HOLLOWED_CHESTNUT_TROUGH,
			HOLLOW_LIMB_SPOUT,
			DAIRY_WORK_TABLE,
			CROCK_STAND,
			WOODEN_LOUVER,
			WOODEN_MILK_PAIL,
			DASHER_CHURN
		)) {
			flammable.add(block, 5, 20);
		}

		ModContentCatalog.register(
			ModContentCatalog.Category.BUILDING_MATERIALS,
			HOLLOWED_CHESTNUT_TROUGH,
			HOLLOW_LIMB_SPOUT,
			WOODEN_LOUVER,
			DAMP_EARTH,
			DAMP_FIELDSTONE,
			MOSSY_FIELDSTONE
		);
		ModContentCatalog.register(
			ModContentCatalog.Category.FURNITURE_DECOR,
			DAIRY_WORK_TABLE,
			CROCK_STAND,
			WOODEN_MILK_PAIL,
			STONEWARE_MILK_CROCK,
			SHALLOW_MILK_PAN,
			DASHER_CHURN,
			BUTTER_CROCK,
			CHEESE_WHEEL
		);
		initialized = true;
	}
}
