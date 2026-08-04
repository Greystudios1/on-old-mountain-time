package net.beforetheblight.compat.rei;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import net.minecraft.network.chat.Component;

/**
 * Compact shared layout for Before the Blight's four physical-process pages.
 */
public final class ReiProcessCategory implements DisplayCategory<ReiProcessDisplay> {
	private static final int DISPLAY_WIDTH = 170;
	private static final int DISPLAY_HEIGHT = 62;

	private final CategoryIdentifier<ReiProcessDisplay> id;
	private final Renderer icon;
	private final Component title;

	public ReiProcessCategory(
		CategoryIdentifier<ReiProcessDisplay> id,
		Renderer icon,
		Component title
	) {
		this.id = Objects.requireNonNull(id, "id");
		this.icon = Objects.requireNonNull(icon, "icon");
		this.title = Objects.requireNonNull(title, "title");
	}

	@Override
	public CategoryIdentifier<? extends ReiProcessDisplay> getCategoryIdentifier() {
		return id;
	}

	@Override
	public Renderer getIcon() {
		return icon;
	}

	@Override
	public Component getTitle() {
		return title;
	}

	@Override
	public List<Widget> setupDisplay(ReiProcessDisplay display, Rectangle bounds) {
		List<Widget> widgets = new ArrayList<>();
		widgets.add(Widgets.createRecipeBase(bounds));

		int left = bounds.x + 11;
		int slotY = bounds.y + 13;
		widgets.add(
			Widgets.createSlot(new Point(left, slotY))
				.entries(display.source())
				.markInput()
		);

		List<me.shedaniel.rei.api.common.entry.EntryIngredient> catalysts = display.catalysts();
		if (catalysts.size() == 1) {
			widgets.add(
				Widgets.createSlot(new Point(left + 37, slotY))
					.entries(catalysts.getFirst())
					.markInput()
			);
		} else if (catalysts.size() == 2) {
			widgets.add(
				Widgets.createSlot(new Point(left + 37, bounds.y + 4))
					.entries(catalysts.get(0))
					.markInput()
			);
			widgets.add(
				Widgets.createSlot(new Point(left + 37, bounds.y + 25))
					.entries(catalysts.get(1))
					.markInput()
			);
		}

		int arrowX = catalysts.isEmpty() ? left + 48 : left + 68;
		int outputX = arrowX + 39;
		widgets.add(Widgets.createArrow(new Point(arrowX, bounds.y + 14)));
		widgets.add(Widgets.createResultSlotBackground(new Point(outputX, slotY)));
		widgets.add(
			Widgets.createSlot(new Point(outputX, slotY))
				.entries(display.output())
				.disableBackground()
				.markOutput()
		);
		widgets.add(
			Widgets.createLabel(
				new Point(bounds.getCenterX(), bounds.y + 51),
				display.note()
			).centered()
		);
		return widgets;
	}

	@Override
	public int getDisplayWidth(ReiProcessDisplay display) {
		return DISPLAY_WIDTH;
	}

	@Override
	public int getDisplayHeight() {
		return DISPLAY_HEIGHT;
	}
}
