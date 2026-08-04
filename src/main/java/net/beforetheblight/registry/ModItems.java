package net.beforetheblight.registry;

import java.util.List;
import java.util.function.Function;

import net.beforetheblight.BeforeTheBlight;
import net.beforetheblight.item.BroadAxeItem;
import net.beforetheblight.item.FrameSawItem;
import net.beforetheblight.item.FroeItem;
import net.beforetheblight.item.WoodenMaulItem;
import net.fabricmc.fabric.api.registry.CompostableRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemLore;

public final class ModItems {
	private static final FoodProperties ROASTED_CHESTNUTS_FOOD = new FoodProperties.Builder()
		.nutrition(4)
		.saturationModifier(0.6F)
		.build();
	private static final FoodProperties EAR_OF_CORN_FOOD = new FoodProperties.Builder()
		.nutrition(3)
		.saturationModifier(0.4F)
		.build();

	public static final Item HANDFUL_OF_CHESTNUTS = register(
		"handful_of_chestnuts",
		properties -> new BlockItem(ModBlocks.CHESTNUT_PILE, properties),
		new Item.Properties().useItemDescriptionPrefix()
	);

	public static final Item ROASTED_CHESTNUTS = register(
		"roasted_chestnuts",
		Item::new,
		new Item.Properties().food(ROASTED_CHESTNUTS_FOOD)
	);

	public static final Item CORN_KERNELS = register(
		"corn_kernels",
		properties -> new BlockItem(ModBlocks.CORN, properties),
		new Item.Properties().useItemDescriptionPrefix()
	);

	public static final Item EAR_OF_CORN = register(
		"ear_of_corn",
		Item::new,
		new Item.Properties().food(EAR_OF_CORN_FOOD)
	);

	public static final Item CORNMEAL = register(
		"cornmeal",
		Item::new,
		new Item.Properties()
	);

	public static final Item DRIED_EAR_OF_CORN = register(
		"dried_ear_of_corn",
		Item::new,
		new Item.Properties()
	);

	public static final Item BROAD_AXE = register(
		"broad_axe",
		BroadAxeItem::new,
		new Item.Properties().component(
			DataComponents.LORE,
			new ItemLore(List.of(
				Component.translatable("item.before_the_blight.broad_axe.tooltip")
					.withStyle(ChatFormatting.GRAY)
			))
		)
	);

	public static final Item FRAME_SAW = register(
		"frame_saw",
		FrameSawItem::new,
		new Item.Properties().durability(256)
	);

	public static final Item FROE = register(
		"froe",
		FroeItem::new,
		new Item.Properties().durability(128)
	);

	public static final Item WOODEN_MAUL = register(
		"wooden_maul",
		WoodenMaulItem::new,
		new Item.Properties().durability(256)
	);

	private ModItems() {
	}

	private static <T extends Item> T register(
		String name,
		Function<Item.Properties, T> itemFactory,
		Item.Properties properties
	) {
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, BeforeTheBlight.id(name));
		T item = itemFactory.apply(properties.setId(itemKey));
		if (item instanceof BlockItem blockItem) {
			blockItem.registerBlocks(Item.BY_BLOCK, item);
		}
		return Registry.register(BuiltInRegistries.ITEM, itemKey, item);
	}

	public static void initialize() {
		if (ModBlocks.CHESTNUT_PILE.asItem() != HANDFUL_OF_CHESTNUTS) {
			throw new IllegalStateException("Chestnut pile must stack from the Handful of Chestnuts block item.");
		}
		if (ModBlocks.CORN.asItem() != CORN_KERNELS) {
			throw new IllegalStateException("Corn must be planted and picked with Corn Kernels.");
		}

		CompostableRegistry.INSTANCE.add(CORN_KERNELS, 0.3F);
		CompostableRegistry.INSTANCE.add(EAR_OF_CORN, 0.65F);
		CompostableRegistry.INSTANCE.add(DRIED_EAR_OF_CORN, 0.65F);
	}
}
