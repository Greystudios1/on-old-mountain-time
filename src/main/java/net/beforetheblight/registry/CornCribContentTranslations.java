package net.beforetheblight.registry;

import java.util.function.BiConsumer;

/**
 * Isolated language-provider hook for the corn-crib slice.
 *
 * <p>The main English provider can call {@link #addTranslations(BiConsumer)}
 * without duplicating this module's names or REI acquisition instructions.</p>
 */
public final class CornCribContentTranslations {
	private CornCribContentTranslations() {
	}

	public static void addTranslations(BiConsumer<String, String> add) {
		add.accept(
			"block.before_the_blight.wide_set_chestnut_crib_wall",
			"Wide-Set Chestnut Crib Wall"
		);
		add.accept(
			"block.before_the_blight.puncheon_floor_edge_with_joists",
			"Puncheon Floor Edge with Joists"
		);
		add.accept(
			"block.before_the_blight.yellow_ear_corn_pile",
			"Yellow Ear-Corn Pile"
		);
		add.accept(
			"block.before_the_blight.mixed_ear_corn_pile",
			"Mixed Ear-Corn Pile"
		);
		add.accept(
			"block.before_the_blight.yellow_scattered_ear_corn",
			"Scattered Yellow Ear Corn"
		);
		add.accept(
			"block.before_the_blight.mixed_scattered_ear_corn",
			"Scattered Mixed Ear Corn"
		);
		add.accept("block.before_the_blight.seed_corn_bundle", "Seed-Corn Bundle");
		add.accept("block.before_the_blight.bushel_basket", "Bushel Basket");
		add.accept("block.before_the_blight.corn_bin", "Wooden Corn Bin");
		add.accept("block.before_the_blight.hand_corn_sheller", "Hand Corn Sheller");
		add.accept("block.before_the_blight.wooden_corn_scoop", "Wooden Corn Scoop");

		add.accept(
			"rei.before_the_blight.guide.wide_set_chestnut_crib_wall",
			"What: Connected, ventilated chestnut crib walling with alternating courses. "
				+ "Obtain: Craft hewn chestnut beams. Use: Build straight runs, ends, corners, "
				+ "T-junctions, and crosses without sealing stored corn."
		);
		add.accept(
			"rei.before_the_blight.guide.puncheon_floor_edge_with_joists",
			"What: The exposed edge of a raised puncheon floor with visible support joists. "
				+ "Obtain: Craft a broad chestnut puncheon with a hewn chestnut beam. "
				+ "Use: Finish raised crib floors without an invisible full-block underside."
		);
		add.accept(
			"rei.before_the_blight.guide.yellow_ear_corn_pile",
			"What: A stable yellow field-corn pile with five fullness states and four edge shapes. "
				+ "Obtain: Craft four dried ears. Use: Add or remove four dried ears per interaction; "
				+ "breaking the pile returns the stored ears."
		);
		add.accept(
			"rei.before_the_blight.guide.mixed_ear_corn_pile",
			"What: A mixed-color field-corn pile with five fullness states and four edge shapes. "
				+ "Obtain: Combine a yellow ear-corn pile with a corn kernel. Use: Add or remove four "
				+ "dried ears per interaction; breaking the pile returns the stored ears."
		);
		add.accept(
			"rei.before_the_blight.guide.yellow_scattered_ear_corn",
			"What: A low scatter of yellow dried ears. Obtain: Craft one dried ear. "
				+ "Use: Dress crib floors and husking areas; breaking it returns one dried ear."
		);
		add.accept(
			"rei.before_the_blight.guide.mixed_scattered_ear_corn",
			"What: A low scatter of mixed-color dried ears. Obtain: Combine scattered yellow ear "
				+ "corn with a kernel. Use: Dress crib floors; breaking it returns one dried ear."
		);
		add.accept(
			"rei.before_the_blight.guide.seed_corn_bundle",
			"What: Selected dried ears tied for next season's seed. Obtain: Tie dried ears with "
				+ "string. Use: Attach it to a wall or hang it beneath a solid ceiling or beam."
		);
		add.accept(
			"rei.before_the_blight.guide.bushel_basket",
			"What: A white-oak splint basket with empty and filled states. Obtain: Craft rough "
				+ "boards and sticks. Use: Insert or remove four dried ears at a time."
		);
		add.accept(
			"rei.before_the_blight.guide.corn_bin",
			"What: A slatted wooden corn bin holding up to sixteen dried ears. Obtain: Craft rough "
				+ "chestnut boards. Use: Insert or remove four dried ears per visible level."
		);
		add.accept(
			"rei.before_the_blight.guide.hand_corn_sheller",
			"What: A small hand-operated corn sheller. Obtain: Craft rough boards with an iron "
				+ "nugget. Use: Insert one dried ear, then use an empty hand to recover four kernels."
		);
		add.accept(
			"rei.before_the_blight.guide.wooden_corn_scoop",
			"What: A wooden scoop for handling ear corn and grain. Obtain: Craft a rough board "
				+ "with a stick. Use: Historically grounded crib and feed-storage equipment."
		);
	}
}
