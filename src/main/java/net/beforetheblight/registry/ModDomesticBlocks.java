package net.beforetheblight.registry;

import java.util.List;
import java.util.function.Function;

import net.beforetheblight.BeforeTheBlight;
import net.beforetheblight.block.domestic.AttachmentPropBlock;
import net.beforetheblight.block.domestic.DomesticLightBlock;
import net.beforetheblight.block.domestic.DomesticPileBlock;
import net.beforetheblight.block.domestic.FillablePropBlock;
import net.beforetheblight.block.domestic.HorizontalPropBlock;
import net.beforetheblight.block.domestic.ThinTextileBlock;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Isolated registry for modest, placeable farmstead household objects.
 *
 * <p>Every visible object has its own block ID and matching block item in
 * {@link ModDomesticItems}. The shared Java classes provide placement, outline,
 * fill, and light behavior only; none of these decorative objects pretends to
 * contain an inventory or cooking process.</p>
 */
public final class ModDomesticBlocks {
	// Hearth cookware.
	public static final Block CAST_IRON_POT = prop(
		"cast_iron_pot", HorizontalPropBlock.Profile.MEDIUM, metalProperties()
	);
	public static final Block IRON_KETTLE = prop(
		"iron_kettle", HorizontalPropBlock.Profile.SMALL, metalProperties()
	);
	public static final Block SPIDER_SKILLET = prop(
		"spider_skillet", HorizontalPropBlock.Profile.WIDE_LOW, metalProperties()
	);
	public static final Block DUTCH_OVEN = prop(
		"dutch_oven", HorizontalPropBlock.Profile.MEDIUM, metalProperties()
	);

	// Wooden tableware.
	public static final Block WOODEN_BOWL = prop(
		"wooden_bowl", HorizontalPropBlock.Profile.SMALL, woodProperties()
	);
	public static final Block WOODEN_TRENCHER = prop(
		"wooden_trencher", HorizontalPropBlock.Profile.WIDE_LOW, woodProperties()
	);
	public static final Block WOODEN_CUP = prop(
		"wooden_cup", HorizontalPropBlock.Profile.SMALL, woodProperties()
	);
	public static final Block WOODEN_SPOON = prop(
		"wooden_spoon", HorizontalPropBlock.Profile.LONG_THIN, woodProperties()
	);
	public static final Block WOODEN_LADLE = prop(
		"wooden_ladle", HorizontalPropBlock.Profile.LONG_THIN, woodProperties()
	);

	// Containers and dishes.
	public static final Block WOVEN_BASKET = prop(
		"woven_basket", HorizontalPropBlock.Profile.MEDIUM, woodProperties()
	);
	public static final Block WOODEN_BUCKET = prop(
		"wooden_bucket", HorizontalPropBlock.Profile.MEDIUM, woodProperties()
	);
	public static final Block WOODEN_PAIL = prop(
		"wooden_pail", HorizontalPropBlock.Profile.MEDIUM, woodProperties()
	);
	public static final Block STONEWARE_CROCK = fillable(
		"stoneware_crock", HorizontalPropBlock.Profile.SMALL, ceramicProperties()
	);
	public static final Block STONEWARE_JUG = prop(
		"stoneware_jug", HorizontalPropBlock.Profile.TALL, ceramicProperties()
	);
	public static final Block CERAMIC_PITCHER = prop(
		"ceramic_pitcher", HorizontalPropBlock.Profile.TALL, ceramicProperties()
	);
	public static final Block TIN_CUP = prop(
		"tin_cup", HorizontalPropBlock.Profile.TINY, metalProperties()
	);
	public static final Block TIN_PLATE = prop(
		"tin_plate", HorizontalPropBlock.Profile.FLAT, metalProperties()
	);
	public static final Block PRESERVING_JAR = fillable(
		"preserving_jar", HorizontalPropBlock.Profile.SMALL, glassProperties()
	);
	public static final Block GREEN_GLASS_BOTTLE = prop(
		"green_glass_bottle", HorizontalPropBlock.Profile.TALL, glassProperties()
	);

	// Placeable food and pantry dressing.
	public static final Block CORNBREAD_ROUND = prop(
		"cornbread_round", HorizontalPropBlock.Profile.FLAT, foodProperties()
	);
	public static final Block HOE_CAKE = prop(
		"hoe_cake", HorizontalPropBlock.Profile.FLAT, foodProperties()
	);
	public static final Block HOMINY_BOWL = prop(
		"hominy_bowl", HorizontalPropBlock.Profile.SMALL, foodProperties()
	);
	public static final Block DRIED_BEAN_BOWL = prop(
		"dried_bean_bowl", HorizontalPropBlock.Profile.SMALL, foodProperties()
	);
	public static final Block DRIED_APPLE_RINGS = pile(
		"dried_apple_rings", HorizontalPropBlock.Profile.WIDE_LOW, foodProperties()
	);
	public static final Block HANGING_DRIED_APPLES = attached(
		"hanging_dried_apples", AttachmentPropBlock.Profile.TALL, foodProperties()
	);
	public static final Block DRIED_HERB_BUNCH = attached(
		"dried_herb_bunch", AttachmentPropBlock.Profile.TALL, plantProperties()
	);
	public static final Block HANGING_ONIONS = attached(
		"hanging_onions", AttachmentPropBlock.Profile.TALL, plantProperties()
	);
	public static final Block HANGING_GARLIC = attached(
		"hanging_garlic", AttachmentPropBlock.Profile.TALL, plantProperties()
	);

	// Low, warm domestic lighting. Lighting is toggled with an empty hand.
	public static final Block TALLOW_CANDLE = light(
		"tallow_candle", HorizontalPropBlock.Profile.TINY, 6, waxProperties()
	);
	public static final Block BETTY_LAMP = light(
		"betty_lamp", HorizontalPropBlock.Profile.SMALL, 8, metalProperties()
	);

	// Textiles with actual non-coplanar thickness.
	public static final Block FOLDED_PATCHWORK_QUILT = textile(
		"folded_patchwork_quilt", ThinTextileBlock.Profile.FOLDED, textileProperties()
	);
	public static final Block FOLDED_WOOL_BLANKET = textile(
		"folded_wool_blanket", ThinTextileBlock.Profile.FOLDED, textileProperties()
	);
	public static final Block RAG_RUG = textile(
		"rag_rug", ThinTextileBlock.Profile.RUG, textileProperties()
	);
	public static final Block WOVEN_WALL_TEXTILE = attached(
		"woven_wall_textile", AttachmentPropBlock.Profile.WIDE, textileProperties()
	);

	// Washing and cleaning.
	public static final Block WOODEN_WASH_TUB = prop(
		"wooden_wash_tub", HorizontalPropBlock.Profile.LARGE, woodProperties()
	);
	public static final Block WASHBOARD = attached(
		"washboard", AttachmentPropBlock.Profile.WIDE, woodProperties()
	);
	public static final Block SOAP_BLOCK = prop(
		"soap_block", HorizontalPropBlock.Profile.TINY, waxProperties()
	);

	// Restrained personal effects.
	public static final Block PLAIN_BIBLE = prop(
		"plain_bible", HorizontalPropBlock.Profile.WIDE_LOW, paperProperties()
	);
	public static final Block ALMANAC = prop(
		"almanac", HorizontalPropBlock.Profile.WIDE_LOW, paperProperties()
	);
	public static final Block LETTER_BUNDLE = prop(
		"letter_bundle", HorizontalPropBlock.Profile.FLAT, paperProperties()
	);
	public static final Block INK_BOTTLE = prop(
		"ink_bottle", HorizontalPropBlock.Profile.TINY, glassProperties()
	);
	public static final Block WORK_BOOTS = prop(
		"work_boots", HorizontalPropBlock.Profile.WIDE_LOW, leatherProperties()
	);
	public static final Block FIDDLE = prop(
		"fiddle", HorizontalPropBlock.Profile.LONG_THIN, woodProperties()
	);

	public static final List<Block> WOODEN_AND_PLANT_BLOCKS = List.of(
		WOODEN_BOWL,
		WOODEN_TRENCHER,
		WOODEN_CUP,
		WOODEN_SPOON,
		WOODEN_LADLE,
		WOVEN_BASKET,
		WOODEN_BUCKET,
		WOODEN_PAIL,
		DRIED_APPLE_RINGS,
		HANGING_DRIED_APPLES,
		DRIED_HERB_BUNCH,
		HANGING_ONIONS,
		HANGING_GARLIC,
		FOLDED_PATCHWORK_QUILT,
		FOLDED_WOOL_BLANKET,
		RAG_RUG,
		WOVEN_WALL_TEXTILE,
		WOODEN_WASH_TUB,
		WASHBOARD,
		PLAIN_BIBLE,
		ALMANAC,
		LETTER_BUNDLE,
		WORK_BOOTS,
		FIDDLE
	);

	private ModDomesticBlocks() {
	}

	private static Block prop(
		String name,
		HorizontalPropBlock.Profile profile,
		BlockBehaviour.Properties properties
	) {
		return register(name, candidate -> new HorizontalPropBlock(profile, candidate), properties);
	}

	private static Block pile(
		String name,
		HorizontalPropBlock.Profile profile,
		BlockBehaviour.Properties properties
	) {
		return register(name, candidate -> new DomesticPileBlock(profile, candidate), properties);
	}

	private static Block fillable(
		String name,
		HorizontalPropBlock.Profile profile,
		BlockBehaviour.Properties properties
	) {
		return register(name, candidate -> new FillablePropBlock(profile, candidate), properties);
	}

	private static Block attached(
		String name,
		AttachmentPropBlock.Profile profile,
		BlockBehaviour.Properties properties
	) {
		return register(name, candidate -> new AttachmentPropBlock(profile, candidate), properties);
	}

	private static Block light(
		String name,
		HorizontalPropBlock.Profile profile,
		int lightLevel,
		BlockBehaviour.Properties properties
	) {
		return register(
			name,
			candidate -> new DomesticLightBlock(profile, lightLevel, candidate),
			properties
		);
	}

	private static Block textile(
		String name,
		ThinTextileBlock.Profile profile,
		BlockBehaviour.Properties properties
	) {
		return register(name, candidate -> new ThinTextileBlock(profile, candidate), properties);
	}

	private static BlockBehaviour.Properties woodProperties() {
		return base(MapColor.WOOD, SoundType.WOOD, 0.8F).ignitedByLava();
	}

	private static BlockBehaviour.Properties metalProperties() {
		return base(MapColor.METAL, SoundType.METAL, 1.5F);
	}

	private static BlockBehaviour.Properties ceramicProperties() {
		return base(MapColor.TERRACOTTA_WHITE, SoundType.STONE, 0.7F);
	}

	private static BlockBehaviour.Properties glassProperties() {
		return base(MapColor.NONE, SoundType.GLASS, 0.3F);
	}

	private static BlockBehaviour.Properties foodProperties() {
		return base(MapColor.COLOR_BROWN, SoundType.WOOD, 0.2F)
			.pushReaction(PushReaction.DESTROY);
	}

	private static BlockBehaviour.Properties plantProperties() {
		return base(MapColor.PLANT, SoundType.GRASS, 0.2F)
			.ignitedByLava()
			.pushReaction(PushReaction.DESTROY);
	}

	private static BlockBehaviour.Properties waxProperties() {
		return base(MapColor.COLOR_YELLOW, SoundType.CANDLE, 0.2F)
			.ignitedByLava()
			.pushReaction(PushReaction.DESTROY);
	}

	private static BlockBehaviour.Properties textileProperties() {
		return base(MapColor.WOOL, SoundType.WOOL, 0.2F)
			.ignitedByLava()
			.pushReaction(PushReaction.DESTROY);
	}

	private static BlockBehaviour.Properties paperProperties() {
		return base(MapColor.COLOR_BROWN, SoundType.WOOL, 0.15F)
			.ignitedByLava()
			.pushReaction(PushReaction.DESTROY);
	}

	private static BlockBehaviour.Properties leatherProperties() {
		return base(MapColor.COLOR_BROWN, SoundType.WOOL, 0.35F)
			.ignitedByLava()
			.pushReaction(PushReaction.DESTROY);
	}

	private static BlockBehaviour.Properties base(
		MapColor mapColor,
		SoundType sound,
		float strength
	) {
		return BlockBehaviour.Properties.of()
			.mapColor(mapColor)
			.strength(strength)
			.sound(sound)
			.noOcclusion();
	}

	private static <T extends Block> T register(
		String name,
		Function<BlockBehaviour.Properties, T> factory,
		BlockBehaviour.Properties properties
	) {
		ResourceKey<Block> key = ResourceKey.create(
			Registries.BLOCK,
			BeforeTheBlight.id(name)
		);
		T block = factory.apply(properties.setId(key));
		return Registry.register(BuiltInRegistries.BLOCK, key, block);
	}

	public static void initialize() {
		FlammableBlockRegistry flammable = FlammableBlockRegistry.getDefaultInstance();
		for (Block block : WOODEN_AND_PLANT_BLOCKS) {
			flammable.add(block, 5, 20);
		}
	}
}
