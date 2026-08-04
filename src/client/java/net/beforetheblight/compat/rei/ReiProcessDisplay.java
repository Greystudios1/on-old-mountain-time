package net.beforetheblight.compat.rei;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * A client-only REI description of one physical, in-world process.
 *
 * <p>The first entry is the consumed timber or crop bundle. Catalysts are
 * searchable inputs, but are deliberately excluded from required entries
 * because the hand tools are damaged rather than consumed.</p>
 */
public record ReiProcessDisplay(
	CategoryIdentifier<ReiProcessDisplay> category,
	Identifier id,
	EntryIngredient source,
	List<EntryIngredient> catalysts,
	EntryIngredient output,
	Component note
) implements Display {
	public ReiProcessDisplay {
		Objects.requireNonNull(category, "category");
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(source, "source");
		Objects.requireNonNull(catalysts, "catalysts");
		Objects.requireNonNull(output, "output");
		Objects.requireNonNull(note, "note");
		catalysts = List.copyOf(catalysts);
	}

	@Override
	public List<EntryIngredient> getInputEntries() {
		List<EntryIngredient> inputs = new ArrayList<>(1 + catalysts.size());
		inputs.add(source);
		inputs.addAll(catalysts);
		return List.copyOf(inputs);
	}

	@Override
	public List<EntryIngredient> getRequiredEntries() {
		return List.of(source);
	}

	@Override
	public List<EntryIngredient> getOutputEntries() {
		return List.of(output);
	}

	@Override
	public CategoryIdentifier<?> getCategoryIdentifier() {
		return category;
	}

	@Override
	public Optional<Identifier> getDisplayLocation() {
		return Optional.of(id);
	}

	@Override
	public DisplaySerializer<? extends Display> getSerializer() {
		// These synthetic displays are rebuilt from the authoritative gameplay
		// registry on every client reload and are never synchronized.
		return null;
	}
}
