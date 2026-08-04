package net.beforetheblight.compat.rei;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.plugin.common.displays.DefaultInformationDisplay;
import net.beforetheblight.BeforeTheBlight;
import net.beforetheblight.interaction.TimberProcessingRegistry;
import net.beforetheblight.interaction.TimberProcessingRegistry.ProcessingOutput;
import net.beforetheblight.interaction.TimberProcessingRegistry.TimberProcess;
import net.beforetheblight.interaction.TimberSplitKind;
import net.beforetheblight.registry.ModBlocks;
import net.beforetheblight.registry.ModContentCatalog;
import net.beforetheblight.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/**
 * Optional Roughly Enough Items integration.
 *
 * <p>Vanilla recipes remain data-driven and are indexed by REI itself. This
 * plugin supplies the nine in-world processes and an acquisition/use page for
 * every obtainable Before the Blight entry. It intentionally registers no
 * transfer handler, so REI cannot bypass the hand-processing mechanics.</p>
 */
public final class BeforeTheBlightReiClientPlugin implements REIClientPlugin {
	public static final CategoryIdentifier<ReiProcessDisplay> HEWING =
		CategoryIdentifier.of(BeforeTheBlight.MOD_ID, "hand_hewing");
	public static final CategoryIdentifier<ReiProcessDisplay> SAWING =
		CategoryIdentifier.of(BeforeTheBlight.MOD_ID, "frame_sawing");
	public static final CategoryIdentifier<ReiProcessDisplay> SPLITTING =
		CategoryIdentifier.of(BeforeTheBlight.MOD_ID, "froe_splitting");
	public static final CategoryIdentifier<ReiProcessDisplay> DRYING =
		CategoryIdentifier.of(BeforeTheBlight.MOD_ID, "air_drying");

	@Override
	public void registerCategories(CategoryRegistry registry) {
		addPhysicalCategory(
			registry,
			new ReiProcessCategory(
				HEWING,
				EntryStacks.of(ModItems.BROAD_AXE),
				Component.translatable("rei.before_the_blight.category.hewing")
			)
		);
		addPhysicalCategory(
			registry,
			new ReiProcessCategory(
				SAWING,
				EntryStacks.of(ModItems.FRAME_SAW),
				Component.translatable("rei.before_the_blight.category.sawing")
			)
		);
		addPhysicalCategory(
			registry,
			new ReiProcessCategory(
				SPLITTING,
				EntryStacks.of(ModItems.FROE),
				Component.translatable("rei.before_the_blight.category.splitting")
			)
		);
		addPhysicalCategory(
			registry,
			new ReiProcessCategory(
				DRYING,
				EntryStacks.of(ModBlocks.DRYING_CORN_BUNDLE),
				Component.translatable("rei.before_the_blight.category.drying")
			)
		);

		registry.addWorkstations(HEWING, EntryStacks.of(ModItems.BROAD_AXE));
		registry.addWorkstations(
			SAWING,
			EntryStacks.of(ModBlocks.SAWING_TRESTLES),
			EntryStacks.of(ModItems.FRAME_SAW)
		);
		registry.addWorkstations(
			SPLITTING,
			EntryStacks.of(ModBlocks.SPLITTING_STUMP),
			EntryStacks.of(ModItems.FROE),
			EntryStacks.of(ModItems.WOODEN_MAUL)
		);
		registry.addWorkstations(DRYING, EntryStacks.of(ModBlocks.DRYING_CORN_BUNDLE));
	}

	private static void addPhysicalCategory(
		CategoryRegistry registry,
		ReiProcessCategory category
	) {
		registry.add(
			category,
			config -> config.setQuickCraftingEnabledByDefault(false)
		);
	}

	@Override
	public void registerDisplays(DisplayRegistry registry) {
		for (TimberProcess process : TimberProcessingRegistry.all()) {
			String timber = process.type().getSerializedName();
			registry.add(new ReiProcessDisplay(
				HEWING,
				BeforeTheBlight.id("rei/hand_hewing/" + timber),
				ingredient(process.sourceBlock()),
				List.of(ingredient(ModItems.BROAD_AXE)),
				ingredient(process.finalBlock()),
				Component.translatable("rei.before_the_blight.note.hewing")
			));

			ProcessingOutput boards = process.roughBoards();
			registry.add(new ReiProcessDisplay(
				SAWING,
				BeforeTheBlight.id("rei/frame_sawing/" + timber),
				ingredient(process.finalBlock()),
				List.of(ingredient(ModItems.FRAME_SAW)),
				ingredient(boards.item(), boards.count()),
				Component.translatable("rei.before_the_blight.note.sawing")
			));

			for (TimberSplitKind kind : TimberSplitKind.values()) {
				ProcessingOutput split = process.splitOutputs().get(kind);
				if (split == null) {
					continue;
				}
				registry.add(new ReiProcessDisplay(
					SPLITTING,
					BeforeTheBlight.id(
						"rei/froe_splitting/" + timber + "_" + kind.getSerializedName()
					),
					ingredient(process.finalBlock()),
					List.of(
						ingredient(ModItems.FROE),
						ingredient(ModItems.WOODEN_MAUL)
					),
					ingredient(split.item(), split.count()),
					Component.translatable(
						"rei.before_the_blight.note.splitting." + kind.getSerializedName()
					)
				));
			}
		}

		registry.add(new ReiProcessDisplay(
			DRYING,
			BeforeTheBlight.id("rei/air_drying/corn_rack"),
			ingredient(ModItems.EAR_OF_CORN),
			List.of(),
			ingredient(ModItems.DRIED_EAR_OF_CORN),
			Component.translatable("rei.before_the_blight.note.drying")
		));

		registerItemGuides(registry);
	}

	private static void registerItemGuides(DisplayRegistry registry) {
		/*
		 * Resolve the shared catalog only after common initialization has
		 * registered every content family. A static snapshot here can seal a
		 * partial creative catalog when REI loads its plugin class early.
		 */
		List<ItemLike> guideItems = ModContentCatalog.allItems();
		Set<ItemLike> uniqueItems = new HashSet<>(guideItems);
		if (guideItems.isEmpty() || uniqueItems.size() != guideItems.size()) {
			throw new IllegalStateException(
				"REI acquisition guide must contain unique obtainable entries."
			);
		}

		for (ItemLike item : guideItems) {
			if (item.asItem() == net.minecraft.world.item.Items.AIR) {
				throw new IllegalStateException("REI acquisition guide cannot expose an internal block.");
			}
			ItemStack stack = new ItemStack(item);
			String itemPath = BuiltInRegistries.ITEM.getKey(item.asItem()).getPath();
			registry.add(
				DefaultInformationDisplay.createFromEntry(
					EntryStacks.of(stack),
					stack.getHoverName()
				).line(
					Component.translatable("rei.before_the_blight.guide." + itemPath)
				)
			);
		}
	}

	private static EntryIngredient ingredient(ItemLike item) {
		return EntryIngredient.of(EntryStacks.of(item));
	}

	private static EntryIngredient ingredient(ItemLike item, int count) {
		return EntryIngredient.of(EntryStacks.of(item, count));
	}
}
