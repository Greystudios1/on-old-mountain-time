package net.beforetheblight.worldgen.structure;

import net.beforetheblight.BeforeTheBlight;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

public final class ModStructureTypes {
	public static final StructureType<AppalachianHomesteadStructure> APPALACHIAN_HOMESTEAD =
		Registry.register(
			BuiltInRegistries.STRUCTURE_TYPE,
			BeforeTheBlight.id("appalachian_homestead"),
			() -> AppalachianHomesteadStructure.CODEC
		);
	public static final StructureType<AppalachianCornCribStructure> APPALACHIAN_CORN_CRIB =
		Registry.register(
			BuiltInRegistries.STRUCTURE_TYPE,
			BeforeTheBlight.id("appalachian_corn_crib"),
			() -> AppalachianCornCribStructure.CODEC
		);

	public static final StructurePieceType APPALACHIAN_HOMESTEAD_PIECE = Registry.register(
		BuiltInRegistries.STRUCTURE_PIECE,
		BeforeTheBlight.id("appalachian_homestead"),
		(StructurePieceType.StructureTemplateType) AppalachianHomesteadPiece::new
	);
	public static final StructurePieceType APPALACHIAN_CORN_CRIB_PIECE = Registry.register(
		BuiltInRegistries.STRUCTURE_PIECE,
		BeforeTheBlight.id("appalachian_corn_crib"),
		(StructurePieceType.StructureTemplateType) AppalachianCornCribPiece::new
	);

	private ModStructureTypes() {
	}

	public static void initialize() {
		// Loading this class registers both codecs before dynamic structures decode.
	}
}
