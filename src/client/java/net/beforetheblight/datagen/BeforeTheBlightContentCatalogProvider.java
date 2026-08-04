package net.beforetheblight.datagen;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.beforetheblight.BeforeTheBlight;
import net.beforetheblight.registry.ModContentCatalog;
import net.beforetheblight.registry.ModContentCatalog.Category;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/**
 * Writes the immutable, registry-backed creative/REI catalog packaged in every
 * production JAR.
 *
 * <p>Qualification harnesses consume this file instead of pinning a numeric
 * item count or attempting to parse Java source. The provider fails closed on
 * AIR, duplicates, foreign namespaces, or a category/all-items mismatch.</p>
 */
public final class BeforeTheBlightContentCatalogProvider implements DataProvider {
	private static final int SCHEMA_VERSION = 1;

	private final Path outputPath;

	public BeforeTheBlightContentCatalogProvider(FabricPackOutput output) {
		this.outputPath = output
			.getOutputFolder(PackOutput.Target.RESOURCE_PACK)
			.resolve(BeforeTheBlight.MOD_ID)
			.resolve("content_catalog.json");
	}

	@Override
	public CompletableFuture<?> run(CachedOutput cache) {
		JsonObject root = new JsonObject();
		JsonObject categories = new JsonObject();
		JsonArray flattened = new JsonArray();
		Set<Identifier> seen = new HashSet<>();

		for (Category category : Category.values()) {
			JsonArray categoryEntries = new JsonArray();
			for (ItemLike itemLike : ModContentCatalog.items(category)) {
				Identifier id = validatedId(itemLike, seen);
				categoryEntries.add(id.toString());
				flattened.add(id.toString());
			}
			categories.add(category.name().toLowerCase(Locale.ROOT), categoryEntries);
		}

		List<ItemLike> allItems = ModContentCatalog.allItems();
		if (allItems.size() != seen.size()) {
			throw new IllegalStateException(
				"Content catalog category partition has "
					+ seen.size()
					+ " entries but All has "
					+ allItems.size()
			);
		}
		for (int index = 0; index < allItems.size(); index++) {
			String expected = BuiltInRegistries.ITEM.getKey(allItems.get(index).asItem()).toString();
			if (!expected.equals(flattened.get(index).getAsString())) {
				throw new IllegalStateException(
					"Content catalog All order diverges from its category partition at index "
						+ index
				);
			}
		}

		root.addProperty("schema_version", SCHEMA_VERSION);
		root.add("categories", categories);
		root.add("all_entries", flattened);
		root.addProperty("count", flattened.size());
		return DataProvider.saveStable(cache, root, outputPath);
	}

	private static Identifier validatedId(ItemLike itemLike, Set<Identifier> seen) {
		Item item = itemLike.asItem();
		if (item == Items.AIR) {
			throw new IllegalStateException("Content catalog contains AIR.");
		}
		Identifier id = BuiltInRegistries.ITEM.getKey(item);
		if (!BeforeTheBlight.MOD_ID.equals(id.getNamespace())) {
			throw new IllegalStateException("Foreign item in content catalog: " + id);
		}
		if (!seen.add(id)) {
			throw new IllegalStateException("Duplicate item in content catalog: " + id);
		}
		return id;
	}

	@Override
	public String getName() {
		return "Before the Blight Content Catalog";
	}
}
