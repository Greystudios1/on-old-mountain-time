#!/usr/bin/env python3
"""Fail-closed audit for the On Old Mountain Time production JAR.

The audit is deliberately independent of Gradle and third-party packages. It
checks representative release sentinels across Appalachian world generation,
timber processing, corn and drying, structures, onboarding, sounds, and PBR
assets; parses every JSON file in the archive; validates root Fabric metadata
and the beam CTM scope; and rejects development or GameTest material that must
not ship in the production artifact.
"""

from __future__ import annotations

import argparse
import copy
import hashlib
import json
import os
import re
import tempfile
from collections import Counter
from dataclasses import dataclass, field
from pathlib import Path, PurePosixPath
from typing import Any
from zipfile import BadZipFile, ZipFile


REPOSITORY_ROOT = Path(__file__).resolve().parents[2]
SAFE_ARTIFACT_COMPONENT = re.compile(r"[A-Za-z0-9][A-Za-z0-9._+-]*\Z")
REQUIRED_FABRIC_DEPENDENCIES = {
    "fabricloader": ">=0.19.3",
    "minecraft": "~26.1.2",
    "java": ">=25",
    "fabric-api": ">=0.155.2+26.1.2",
    "lithostitched": "1.7.13",
}
REQUIRED_FABRIC_SUGGESTIONS = {
    "sereneseasons": "26.1.2.0.4",
}


def resolve_default_jar(repository_root: Path = REPOSITORY_ROOT) -> Path:
    """Resolve the current main artifact from the two Gradle identity keys."""
    properties_path = repository_root / "gradle.properties"
    try:
        lines = properties_path.read_text(encoding="utf-8").splitlines()
    except (OSError, UnicodeError) as exc:
        raise ValueError(f"cannot read Gradle properties {properties_path}: {exc}") from exc

    properties: dict[str, str] = {}
    for line_number, raw_line in enumerate(lines, start=1):
        line = raw_line.strip()
        if not line or line.startswith(("#", "!")):
            continue
        if "=" not in line:
            continue
        key, value = (part.strip() for part in line.split("=", 1))
        if key in properties:
            raise ValueError(
                f"duplicate Gradle property {key!r} at "
                f"{properties_path}:{line_number}"
            )
        properties[key] = value

    components: dict[str, str] = {}
    for key in ("archives_base_name", "mod_version"):
        value = properties.get(key, "")
        if not value:
            raise ValueError(f"Gradle property {key!r} is missing or empty")
        if SAFE_ARTIFACT_COMPONENT.fullmatch(value) is None:
            raise ValueError(
                f"Gradle property {key!r} is not a safe artifact component: "
                f"{value!r}"
            )
        components[key] = value

    jar_name = (
        f"{components['archives_base_name']}-{components['mod_version']}.jar"
    )
    return repository_root / "build" / "libs" / jar_name

BASE_REQUIRED_ENTRIES = (
    "fabric.mod.json",
    "before_the_blight.mixins.json",
    "net/beforetheblight/BeforeTheBlight.class",
    "net/beforetheblight/block/HewingLogBlock.class",
    "net/beforetheblight/interaction/TimberProcessingRegistry.class",
    "net/beforetheblight/item/BroadAxeItem.class",
    "net/beforetheblight/block/AbstractSawingTrestlesBlock.class",
    "net/beforetheblight/block/LoadedSawingTrestlesBlock.class",
    "net/beforetheblight/block/SawingTrestlesBlock.class",
    "net/beforetheblight/interaction/SawingTrestleStateMachine.class",
    "net/beforetheblight/item/FrameSawItem.class",
    "net/beforetheblight/block/AbstractSplittingStumpBlock.class",
    "net/beforetheblight/block/LoadedSplittingStumpBlock.class",
    "net/beforetheblight/block/SplittingStumpBlock.class",
    "net/beforetheblight/interaction/SplittingStateMachine.class",
    "net/beforetheblight/interaction/TimberSplitKind.class",
    "net/beforetheblight/item/FroeItem.class",
    "net/beforetheblight/item/WoodenMaulItem.class",
    "net/beforetheblight/block/CornCropBlock.class",
    "net/beforetheblight/block/DryingCornBundleBlock.class",
    "net/beforetheblight/registry/ModSounds.class",
    "net/beforetheblight/mixin/InjectorBiomeSourceMixin.class",
    "net/beforetheblight/mixin/InjectorBiomeSourceMixin$FilterThreadState.class",
    "net/beforetheblight/worldgen/biome/ModBiomes.class",
    "net/beforetheblight/worldgen/biome/CoveBiomeTags.class",
    "net/beforetheblight/worldgen/biome/CoveMinimumFootprintFilter.class",
    "net/beforetheblight/worldgen/biome/GrassyBaldBiomeTags.class",
    "net/beforetheblight/worldgen/biome/RawInjectorBiomeSourceAccess.class",
    "net/beforetheblight/worldgen/biome/RidgeMinimumFootprintFilter.class",
    "net/beforetheblight/worldgen/biome/RidgeMinimumFootprintFilter$BoundedLruMap.class",
    "net/beforetheblight/worldgen/biome/RidgeMinimumFootprintFilter$Classifier.class",
    "net/beforetheblight/worldgen/biome/RidgeMinimumFootprintFilter$Decision.class",
    "net/beforetheblight/worldgen/biome/RidgeMinimumFootprintFilter$QuartCoordinate.class",
    "net/beforetheblight/worldgen/biome/RidgeMinimumFootprintFilter$RawRidgeSampler.class",
    "net/beforetheblight/worldgen/feature/ModConfiguredFeatures.class",
    "net/beforetheblight/worldgen/feature/ModFeatures.class",
    "net/beforetheblight/worldgen/feature/ModPlacedFeatures.class",
    "net/beforetheblight/worldgen/feature/RidgeTreeSelectorFeature.class",
    "net/beforetheblight/worldgen/feature/ModTreeGrowers.class",
    "net/beforetheblight/worldgen/feature/configurations/RidgeTreeConfiguration.class",
    "net/beforetheblight/worldgen/feature/trunkplacer/ForestChestnutTrunkPlacer.class",
    "net/beforetheblight/worldgen/feature/trunkplacer/HollowChestnutTrunkPlacer.class",
    "net/beforetheblight/worldgen/feature/trunkplacer/ModTrunkPlacerTypes.class",
    "net/beforetheblight/worldgen/structure/AbstractAppalachianTemplateStructure.class",
    "net/beforetheblight/worldgen/structure/AppalachianCornCribPiece.class",
    "net/beforetheblight/worldgen/structure/AppalachianCornCribStructure.class",
    "net/beforetheblight/worldgen/structure/AppalachianHomesteadPiece.class",
    "net/beforetheblight/worldgen/structure/AppalachianHomesteadStructure.class",
    "net/beforetheblight/worldgen/structure/ModStructures.class",
    "net/beforetheblight/worldgen/structure/ModStructureTypes.class",
    "data/before_the_blight/worldgen/biome/chestnut_oak_ridge.json",
    "data/before_the_blight/worldgen/configured_feature/chestnut_oak_ridge_trees.json",
    "data/before_the_blight/worldgen/configured_feature/chestnut_edge_trees.json",
    "data/before_the_blight/worldgen/configured_feature/chestnut_fallen.json",
    "data/before_the_blight/worldgen/configured_feature/chestnut_mature.json",
    "data/before_the_blight/worldgen/configured_feature/chestnut_old_growth.json",
    "data/before_the_blight/worldgen/configured_feature/chestnut_ordinary.json",
    "data/before_the_blight/worldgen/configured_feature/chestnut_pile_patch.json",
    "data/before_the_blight/worldgen/placed_feature/chestnut_oak_ridge_trees.json",
    "data/before_the_blight/worldgen/placed_feature/chestnut_edge_trees_checked.json",
    "data/before_the_blight/worldgen/placed_feature/chestnut_forest_checked.json",
    "data/before_the_blight/worldgen/placed_feature/chestnut_fallen.json",
    "data/before_the_blight/worldgen/placed_feature/chestnut_mature_checked.json",
    "data/before_the_blight/worldgen/placed_feature/chestnut_old_growth_checked.json",
    "data/before_the_blight/worldgen/placed_feature/chestnut_ordinary_checked.json",
    "data/before_the_blight/worldgen/placed_feature/chestnut_pile_patch.json",
    "data/before_the_blight/tags/worldgen/biome/chestnut_oak_ridge_targets.json",
    "data/before_the_blight/lithostitched/region/appalachian.json",
    "data/before_the_blight/lithostitched/biome_injector/chestnut_oak_ridge.json",
    "data/minecraft/tags/worldgen/biome/is_overworld.json",
    "data/minecraft/tags/worldgen/biome/is_forest.json",
    "data/minecraft/tags/worldgen/biome/is_hill.json",
    "data/minecraft/tags/worldgen/biome/is_mountain.json",
    "data/minecraft/tags/worldgen/biome/stronghold_biased_to.json",
    "data/minecraft/tags/worldgen/biome/has_structure/trial_chambers.json",
    "data/minecraft/tags/worldgen/biome/has_structure/village_plains.json",
    "data/before_the_blight/worldgen/configured_feature/chestnut_forest.json",
    "data/before_the_blight/worldgen/biome/hemlock_beech_cove.json",
    "data/before_the_blight/worldgen/configured_feature/hemlock_beech_cove_trees.json",
    "data/before_the_blight/worldgen/placed_feature/hemlock_beech_cove_trees.json",
    "data/before_the_blight/tags/worldgen/biome/hemlock_beech_cove_targets.json",
    "data/before_the_blight/lithostitched/biome_injector/hemlock_beech_cove.json",
    "data/before_the_blight/worldgen/biome/grassy_bald.json",
    "data/before_the_blight/tags/worldgen/biome/grassy_bald_targets.json",
    "data/before_the_blight/lithostitched/biome_injector/grassy_bald.json",
    "data/before_the_blight/worldgen/structure/appalachian_homestead.json",
    "data/before_the_blight/worldgen/structure_set/appalachian_homesteads.json",
    "data/before_the_blight/tags/worldgen/biome/has_structure/appalachian_homestead.json",
    "data/before_the_blight/worldgen/structure/appalachian_corn_crib.json",
    "data/before_the_blight/worldgen/structure_set/appalachian_corn_cribs.json",
    "data/before_the_blight/tags/worldgen/biome/has_structure/appalachian_corn_crib.json",
    "data/before_the_blight/structure/appalachian_demo_cabin.nbt",
    "data/before_the_blight/structure/appalachian_corn_crib.nbt",
    "data/before_the_blight/structure/appalachian_springhouse.nbt",
    "data/before_the_blight/advancement/onboarding/enter_appalachians.json",
    "data/before_the_blight/advancement/onboarding/obtain_chestnut.json",
    "data/before_the_blight/advancement/onboarding/hew_beam.json",
    "data/before_the_blight/advancement/onboarding/load_trestles.json",
    "data/before_the_blight/advancement/onboarding/saw_rough_boards.json",
    "data/before_the_blight/advancement/onboarding/obtain_shingles.json",
    "data/before_the_blight/advancement/onboarding/discover_homestead.json",
    "assets/before_the_blight/sounds.json",
    "assets/before_the_blight/blockstates/loaded_sawing_trestles.json",
    "assets/before_the_blight/items/frame_saw.json",
    "assets/before_the_blight/models/item/frame_saw.json",
    "data/before_the_blight/loot_table/blocks/loaded_sawing_trestles.json",
    "data/before_the_blight/recipe/frame_saw.json",
    "assets/before_the_blight/blockstates/loaded_splitting_stump.json",
    "assets/before_the_blight/models/block/loaded_splitting_stump_froe_shingles_2.json",
    "assets/before_the_blight/items/froe.json",
    "assets/before_the_blight/items/wooden_maul.json",
    "data/before_the_blight/loot_table/blocks/loaded_splitting_stump.json",
    "data/before_the_blight/recipe/splitting_stump.json",
    "assets/before_the_blight/blockstates/corn.json",
    "assets/before_the_blight/blockstates/drying_corn_bundle.json",
    "data/before_the_blight/loot_table/blocks/corn.json",
    "data/before_the_blight/recipe/cornmeal.json",
    "data/before_the_blight/recipe/drying_corn_bundle.json",
    "assets/before_the_blight/blockstates/chestnut_hewing_log.json",
    "assets/before_the_blight/blockstates/hewn_chestnut_beam.json",
    "assets/before_the_blight/models/block/chestnut_hewing_log_stage1.json",
    "assets/before_the_blight/models/block/chestnut_hewing_log_stage2.json",
    "assets/before_the_blight/models/block/chestnut_hewing_log_stage3.json",
    "assets/before_the_blight/models/block/hewn_chestnut_beam.json",
    "assets/before_the_blight/items/hewn_chestnut_beam.json",
    "assets/before_the_blight/items/broad_axe.json",
    "assets/before_the_blight/models/item/broad_axe.json",
    "data/before_the_blight/loot_table/blocks/chestnut_hewing_log.json",
    "data/before_the_blight/loot_table/blocks/hewn_chestnut_beam.json",
    "data/before_the_blight/recipe/broad_axe.json",
    "data/before_the_blight/advancement/recipes/tools/broad_axe.json",
    "data/before_the_blight/tags/block/hewing_logs.json",
    "data/before_the_blight/tags/item/hewing_logs.json",
    "data/before_the_blight/tags/block/hewn_beams.json",
    "data/before_the_blight/tags/item/hewn_beams.json",
    "data/before_the_blight/tags/block/sawable_beams.json",
    "data/before_the_blight/tags/item/sawable_beams.json",
    "data/minecraft/tags/block/mineable/axe.json",
    "assets/before_the_blight/textures/block/hewn_chestnut_beam.png",
    "assets/before_the_blight/textures/block/hewn_chestnut_beam_n.png",
    "assets/before_the_blight/textures/block/hewn_chestnut_beam_s.png",
    "assets/before_the_blight/textures/block/hewn_chestnut_beam_top.png",
    "assets/before_the_blight/textures/block/hewn_chestnut_beam_top_n.png",
    "assets/before_the_blight/textures/block/hewn_chestnut_beam_top_s.png",
    "assets/before_the_blight/textures/item/broad_axe.png",
    "assets/before_the_blight/textures/item/broad_axe_n.png",
    "assets/before_the_blight/textures/item/broad_axe_s.png",
    "assets/before_the_blight/textures/item/frame_saw.png",
    "assets/before_the_blight/textures/item/frame_saw_n.png",
    "assets/before_the_blight/textures/item/frame_saw_s.png",
    "assets/before_the_blight/textures/block/splitting_stump_side.png",
    "assets/before_the_blight/textures/block/splitting_stump_side_n.png",
    "assets/before_the_blight/textures/block/splitting_stump_side_s.png",
    "assets/before_the_blight/textures/block/corn_stage_7.png",
    "assets/before_the_blight/textures/block/corn_stage_7_n.png",
    "assets/before_the_blight/textures/block/corn_stage_7_s.png",
    "assets/before_the_blight/textures/block/corn_bundle_stage_3.png",
    "assets/before_the_blight/textures/block/corn_bundle_stage_3_n.png",
    "assets/before_the_blight/textures/block/corn_bundle_stage_3_s.png",
    "assets/before_the_blight/textures/block/hemlock_foliage.png",
    "assets/before_the_blight/textures/block/hemlock_foliage_n.png",
    "assets/before_the_blight/textures/block/hemlock_foliage_s.png",
    "assets/before_the_blight/optifine/ctm/hewn_chestnut_beam_vertical/hewn_chestnut_beam.properties",
    "assets/before_the_blight/optifine/ctm/hewn_chestnut_beam_vertical/0.png",
    "assets/before_the_blight/optifine/ctm/hewn_chestnut_beam_vertical/0_n.png",
    "assets/before_the_blight/optifine/ctm/hewn_chestnut_beam_vertical/0_s.png",
    "assets/before_the_blight/optifine/ctm/hewn_chestnut_beam_vertical/1.png",
    "assets/before_the_blight/optifine/ctm/hewn_chestnut_beam_vertical/1_n.png",
    "assets/before_the_blight/optifine/ctm/hewn_chestnut_beam_vertical/1_s.png",
    "assets/before_the_blight/optifine/ctm/hewn_chestnut_beam_vertical/2.png",
    "assets/before_the_blight/optifine/ctm/hewn_chestnut_beam_vertical/2_n.png",
    "assets/before_the_blight/optifine/ctm/hewn_chestnut_beam_vertical/2_s.png",
    "assets/before_the_blight/optifine/ctm/hewn_chestnut_beam_vertical/3.png",
    "assets/before_the_blight/optifine/ctm/hewn_chestnut_beam_vertical/3_n.png",
    "assets/before_the_blight/optifine/ctm/hewn_chestnut_beam_vertical/3_s.png",
)
FEATURE_UPGRADE_REQUIRED_ENTRIES = (
    # Runtime registration and behavior for each added subsystem.
    "net/beforetheblight/registry/ModBlocks.class",
    "net/beforetheblight/registry/ModFurniture.class",
    "net/beforetheblight/block/HewnChestnutPostBlock.class",
    "net/beforetheblight/block/RockingChairBlock.class",
    "net/beforetheblight/entity/RockingChairSeatEntity.class",
    "net/beforetheblight/client/RockingChairClient.class",
    "net/beforetheblight/client/render/RockingChairSeatRenderer.class",
    "net/beforetheblight/client/render/RockingChairSeatRenderState.class",
    "net/beforetheblight/block/UnderstoryPlantBlock.class",
    "net/beforetheblight/block/ForestDuffBlock.class",
    "net/beforetheblight/worldgen/feature/HollowFallenLogFeature.class",
    "net/beforetheblight/worldgen/feature/SparseUnderstoryFeature.class",
    "net/beforetheblight/worldgen/feature/trunkplacer/OldGrowthHemlockTrunkPlacer.class",
    "net/beforetheblight/worldgen/feature/trunkplacer/TieredHemlockTrunkPlacer.class",
    "net/beforetheblight/worldgen/feature/foliageplacer/ModFoliagePlacerTypes.class",
    "net/beforetheblight/worldgen/feature/foliageplacer/TieredHemlockFoliagePlacer.class",
    # Hemlock player-facing family: client, PBR, loot, crafting, tags, and
    # natural/player-growth worldgen.
    "assets/before_the_blight/blockstates/hemlock_log.json",
    "assets/before_the_blight/items/hemlock_sapling.json",
    "assets/before_the_blight/textures/block/hemlock_log.png",
    "assets/before_the_blight/textures/block/hemlock_log_n.png",
    "assets/before_the_blight/textures/block/hemlock_log_s.png",
    "data/before_the_blight/loot_table/blocks/hemlock_foliage.json",
    "data/before_the_blight/recipe/hemlock_planks.json",
    "data/before_the_blight/tags/block/hemlock_logs.json",
    "data/before_the_blight/worldgen/configured_feature/hemlock_tall.json",
    "data/before_the_blight/worldgen/placed_feature/hemlock_tall_checked.json",
    "data/before_the_blight/worldgen/configured_feature/hemlock_old_growth.json",
    "data/before_the_blight/worldgen/placed_feature/hemlock_old_growth_natural.json",
    # American beech uses public american_beech_* assets while the legacy
    # `beech` and `beech_checked` worldgen IDs remain save compatible.
    "assets/before_the_blight/blockstates/american_beech_leaves.json",
    "assets/before_the_blight/items/american_beech_sapling.json",
    "assets/before_the_blight/textures/block/american_beech_leaves.png",
    "assets/before_the_blight/textures/block/american_beech_leaves_n.png",
    "assets/before_the_blight/textures/block/american_beech_leaves_s.png",
    "data/before_the_blight/loot_table/blocks/american_beech_leaves.json",
    "data/before_the_blight/recipe/american_beech_planks.json",
    "data/before_the_blight/tags/block/american_beech_logs.json",
    "data/before_the_blight/worldgen/configured_feature/beech.json",
    "data/before_the_blight/worldgen/placed_feature/beech_checked.json",
    # Black Walnut is a narrow six-block species with a separate rare Cove
    # stream and a direct bridge into the existing Walnut furniture material.
    "assets/before_the_blight/blockstates/black_walnut_log.json",
    "assets/before_the_blight/items/black_walnut_sapling.json",
    "assets/before_the_blight/textures/block/black_walnut_log.png",
    "assets/before_the_blight/textures/block/black_walnut_log_n.png",
    "assets/before_the_blight/textures/block/black_walnut_log_s.png",
    "data/before_the_blight/loot_table/blocks/black_walnut_leaves.json",
    "data/before_the_blight/recipe/walnut_furniture_board.json",
    "data/before_the_blight/tags/block/black_walnut_logs.json",
    "data/before_the_blight/worldgen/configured_feature/black_walnut.json",
    "data/before_the_blight/worldgen/placed_feature/black_walnut_checked.json",
    "data/before_the_blight/worldgen/placed_feature/black_walnut_natural.json",
    "assets/before_the_blight/lang/en_us.json",
    # Representative shapes and surfaces across the six-block fieldstone
    # expansion, plus obtainable inventory, loot, and recipes.
    "assets/before_the_blight/blockstates/dressed_fieldstone_stairs.json",
    "assets/before_the_blight/blockstates/chiseled_fieldstone.json",
    "assets/before_the_blight/blockstates/fieldstone_pier.json",
    "assets/before_the_blight/items/dressed_fieldstone_wall.json",
    "assets/before_the_blight/textures/block/dressed_fieldstone.png",
    "assets/before_the_blight/textures/block/dressed_fieldstone_n.png",
    "assets/before_the_blight/textures/block/dressed_fieldstone_s.png",
    "data/before_the_blight/loot_table/blocks/dressed_fieldstone_wall.json",
    "data/before_the_blight/recipe/chiseled_fieldstone.json",
    "data/before_the_blight/recipe/fieldstone_pier.json",
    # Both hewn framing forms remain obtainable and represented client-side.
    "assets/before_the_blight/blockstates/hewn_chestnut_wall.json",
    "assets/before_the_blight/blockstates/hewn_chestnut_post.json",
    "assets/before_the_blight/items/hewn_chestnut_wall.json",
    "assets/before_the_blight/models/block/hewn_chestnut_post.json",
    "assets/before_the_blight/models/block/hewn_chestnut_post_rail_connection.json",
    "assets/before_the_blight/models/block/hewn_chestnut_post_wall_connection.json",
    "assets/before_the_blight/models/block/hewn_chestnut_post_wall_connection_tall.json",
    "data/before_the_blight/loot_table/blocks/hewn_chestnut_wall.json",
    "data/before_the_blight/recipe/hewn_chestnut_wall.json",
    "data/before_the_blight/recipe/hewn_chestnut_post.json",
    "data/before_the_blight/tags/block/hewn_details.json",
    # The chair's visible and hidden render states, obtainable item/data, and
    # technical seat entity are all required together.
    "assets/before_the_blight/blockstates/rocking_chair.json",
    "assets/before_the_blight/items/rocking_chair.json",
    "assets/before_the_blight/models/block/rocking_chair.json",
    "assets/before_the_blight/models/block/rocking_chair_hidden.json",
    "assets/before_the_blight/models/item/rocking_chair.json",
    "assets/before_the_blight/textures/block/rocking_chair_woven_seat.png",
    "assets/before_the_blight/textures/block/rocking_chair_woven_seat_n.png",
    "assets/before_the_blight/textures/block/rocking_chair_woven_seat_s.png",
    "data/before_the_blight/loot_table/blocks/rocking_chair.json",
    "data/before_the_blight/recipe/rocking_chair.json",
    # Forest-floor/understory assets and their Ridge/Cove placement graph.
    "assets/before_the_blight/blockstates/mountain_laurel.json",
    "assets/before_the_blight/blockstates/lowbush_blueberry.json",
    "assets/before_the_blight/blockstates/forest_duff.json",
    "assets/before_the_blight/items/forest_duff.json",
    "assets/before_the_blight/textures/block/mountain_laurel_a.png",
    "assets/before_the_blight/textures/block/lowbush_blueberry_a.png",
    "assets/before_the_blight/textures/block/forest_duff.png",
    "assets/before_the_blight/textures/block/forest_duff_n.png",
    "assets/before_the_blight/textures/block/forest_duff_s.png",
    "data/before_the_blight/loot_table/blocks/mountain_laurel.json",
    "data/before_the_blight/loot_table/blocks/lowbush_blueberry.json",
    "data/before_the_blight/loot_table/blocks/forest_duff.json",
    "data/before_the_blight/worldgen/configured_feature/ridge_understory_patch.json",
    "data/before_the_blight/worldgen/placed_feature/ridge_understory_patch.json",
    "data/before_the_blight/worldgen/configured_feature/cove_understory_patch.json",
    "data/before_the_blight/worldgen/placed_feature/cove_understory_patch.json",
    # Hollow fallen trunks occur in both Ridge and Cove; Cove also retains its
    # ordinary fallen-hemlock form.
    "data/before_the_blight/worldgen/configured_feature/chestnut_hollow_fallen.json",
    "data/before_the_blight/worldgen/placed_feature/chestnut_hollow_fallen.json",
    "data/before_the_blight/worldgen/configured_feature/cove_fallen_hemlock.json",
    "data/before_the_blight/worldgen/placed_feature/cove_fallen_hemlock.json",
    "data/before_the_blight/worldgen/configured_feature/cove_hollow_fallen_hemlock.json",
    "data/before_the_blight/worldgen/placed_feature/cove_hollow_fallen_hemlock.json",
)
SERENE_SEASONS_REQUIRED_ENTRIES = (
    "net/beforetheblight/compat/seasons/SeasonProvider.class",
    "net/beforetheblight/compat/seasons/SeasonProviderHealth$ProviderOperation.class",
    "net/beforetheblight/compat/seasons/SeasonProviderHealth$State.class",
    "net/beforetheblight/compat/seasons/SeasonProviderHealth$Status.class",
    "net/beforetheblight/compat/seasons/SeasonProviderHealth.class",
    "net/beforetheblight/compat/seasons/SeasonalPlantClock$Plant.class",
    "net/beforetheblight/compat/seasons/SeasonalPlantClock$Snapshot.class",
    "net/beforetheblight/compat/seasons/SeasonalPlantClock$Subseason.class",
    "net/beforetheblight/compat/seasons/SeasonalPlantClock.class",
    "net/beforetheblight/compat/seasons/SereneSeasonsSeasonProvider.class",
    "net/beforetheblight/block/SeasonalChestnutLeavesBlock.class",
    "net/beforetheblight/block/SeasonalSaplingBlock.class",
    "net/beforetheblight/client/season/SeasonalVisuals$SeasonalPalette.class",
    "net/beforetheblight/client/season/SeasonalVisuals$SeasonalTintSource.class",
    "net/beforetheblight/client/season/SeasonalVisuals.class",
    "data/sereneseasons/tags/block/autumn_crops.json",
    "data/sereneseasons/tags/block/spring_crops.json",
    "data/sereneseasons/tags/block/summer_crops.json",
    "data/sereneseasons/tags/block/unbreakable_infertile_crops.json",
    "data/sereneseasons/tags/item/autumn_crops.json",
    "data/sereneseasons/tags/item/spring_crops.json",
    "data/sereneseasons/tags/item/summer_crops.json",
)
REQUIRED_ENTRIES = (
    BASE_REQUIRED_ENTRIES
    + FEATURE_UPGRADE_REQUIRED_ENTRIES
    + SERENE_SEASONS_REQUIRED_ENTRIES
)
INTERNAL_STATE_ONLY_BLOCKS = (
    "chestnut_hewing_log",
    "oak_hewing_log",
    "spruce_hewing_log",
    "loaded_sawing_trestles",
    "loaded_splitting_stump",
    "chestnut_pile",
    "corn",
)
FORBIDDEN_EXACT_ENTRIES = tuple(
    entry
    for block_id in INTERNAL_STATE_ONLY_BLOCKS
    for entry in (
        f"assets/before_the_blight/items/{block_id}.json",
        f"assets/before_the_blight/models/item/{block_id}.json",
        f"data/before_the_blight/recipe/{block_id}.json",
    )
)
REQUIRED_CLASS_ENTRIES = tuple(
    entry for entry in REQUIRED_ENTRIES if entry.endswith(".class")
)
REQUIRED_JSON_ENTRIES = tuple(
    entry for entry in REQUIRED_ENTRIES if entry.endswith(".json")
)
REQUIRED_PNG_ENTRIES = tuple(
    entry for entry in REQUIRED_ENTRIES if entry.endswith(".png")
)
BEAM_CTM_PROPERTY_ENTRY = (
    "assets/before_the_blight/optifine/ctm/hewn_chestnut_beam_vertical/"
    "hewn_chestnut_beam.properties"
)
REQUIRED_BEAM_CTM_PROPERTIES = {
    "method": "vertical",
    "matchBlocks": "before_the_blight:hewn_chestnut_beam",
    "tiles": "0-3",
    "connect": "block",
    "faces": "sides",
    "orient": "texture",
    "innerSeams": "false",
}
CLASS_MAGIC = b"\xca\xfe\xba\xbe"
MIN_REQUIRED_CLASS_BYTES = 16
PNG_MAGIC = b"\x89PNG\r\n\x1a\n"
MIN_REQUIRED_PNG_BYTES = 24
MAIN_BUILD_OUTPUT_RELATIVE_ROOTS = (
    Path("build/classes/java/main"),
    Path("build/resources/main"),
)
CLIENT_BUILD_OUTPUT_RELATIVE_ROOTS = (
    Path("build/classes/java/client"),
    Path("build/resources/client"),
)

FORBIDDEN_PATH_COMPONENTS = frozenset(
    {
        ".cache",
        "__pycache__",
        "build",
        "cache",
        "run",
        "src",
        "test-results",
        "test_results",
        "tools",
    }
)
FORBIDDEN_DATA_COMPONENTS = frozenset(
    {
        "density_function",
        "dimension",
        "dimension_type",
        "dimensions",
        "noise",
        "noise_settings",
    }
)
FORBIDDEN_TEXT = (
    b"before_the_blight_gametest",
    b"fabric-gametest",
)
FORBIDDEN_SHADED_NAMESPACE_PREFIXES = (
    "sereneseasons/",
    "glitchcore/",
    "com/electronwill/nightconfig/",
)
FORBIDDEN_FOREST_PLACEMENT = (
    "data/before_the_blight/worldgen/placed_feature/chestnut_forest.json"
)


@dataclass
class AuditResult:
    jar_path: Path
    entry_count: int = 0
    file_count: int = 0
    json_count: int = 0
    json_parsed_count: int = 0
    required_present_count: int = 0
    forbidden_exact_absent_count: int = 0
    required_class_valid_count: int = 0
    required_png_valid_count: int = 0
    required_json_source_match_count: int = 0
    required_ctm_property_valid_count: int = 0
    build_output_manifest_checked_count: int = 0
    size_bytes: int = 0
    sha256: str = ""
    failures: list[str] = field(default_factory=list)

    @property
    def passed(self) -> bool:
        return not self.failures


def _sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest().upper()


def _path_failures(name: str) -> list[str]:
    failures: list[str] = []
    lowered = name.lower()

    if "\\" in name:
        failures.append(f"non-portable archive path uses backslashes: {name}")
        return failures

    path = PurePosixPath(name)
    parts = tuple(part.lower() for part in path.parts)
    if name.startswith("/") or ".." in parts:
        failures.append(f"unsafe archive path: {name}")

    forbidden_components = set(parts) & FORBIDDEN_PATH_COMPONENTS
    # `tools` is also Minecraft's generated recipe-book category.  Permit only
    # that exact data-pack location while still rejecting project/tooling trees.
    if parts[:5] == (
        "data",
        "before_the_blight",
        "advancement",
        "recipes",
        "tools",
    ):
        forbidden_components.discard("tools")
    forbidden_components = sorted(forbidden_components)
    if forbidden_components:
        failures.append(
            f"forbidden archive path component in {name}: "
            f"{', '.join(forbidden_components)}"
        )

    if "gametest" in lowered:
        failures.append(f"GameTest path leaked into production JAR: {name}")

    if lowered.endswith(".java"):
        failures.append(f"Java source leaked into production JAR: {name}")
    if lowered.endswith(".jar"):
        failures.append(f"nested JAR leaked into production JAR: {name}")

    namespace_path = re.sub(
        r"\Ameta-inf/versions/\d+/",
        "",
        lowered,
    )
    for prefix in FORBIDDEN_SHADED_NAMESPACE_PREFIXES:
        if namespace_path.startswith(prefix):
            failures.append(
                "optional companion namespace shaded into production JAR: "
                f"{name}"
            )
            break

    if len(parts) >= 2 and parts[0] == "data":
        forbidden_data = sorted(set(parts[2:]) & FORBIDDEN_DATA_COMPONENTS)
        if forbidden_data:
            failures.append(
                "dimension/noise/density-function data leaked into production "
                f"JAR at {name}: "
                f"{', '.join(forbidden_data)}"
            )

    if lowered == FORBIDDEN_FOREST_PLACEMENT:
        failures.append(
            "unscoped top-level chestnut forest placement leaked into production "
            f"data: {name}"
        )

    return failures


def _authoritative_json_path(source_root: Path, entry: str) -> Path | None:
    generated = source_root / "src/main/generated" / entry
    resources = source_root / "src/main/resources" / entry
    if generated.is_file():
        return generated
    if resources.is_file():
        return resources
    return None


def _build_output_manifest(source_root: Path) -> tuple[tuple[str, Path], ...]:
    """Map complete main build output to its production-JAR entry names.

    The manifest is intentionally conditional so the auditor remains usable as
    a standalone artifact beside a JAR.  When both main Gradle output roots
    exist, however, every regular file there and in either present client
    output root is authoritative production output and must be retained
    byte-for-byte.
    """
    main_output_roots = tuple(
        source_root / path for path in MAIN_BUILD_OUTPUT_RELATIVE_ROOTS
    )
    if not all(root.is_dir() for root in main_output_roots):
        return ()
    client_output_roots = tuple(
        source_root / path
        for path in CLIENT_BUILD_OUTPUT_RELATIVE_ROOTS
        if (source_root / path).is_dir()
    )
    output_roots = main_output_roots + client_output_roots

    entries: list[tuple[str, Path]] = []
    for output_root in output_roots:
        for build_file in sorted(
            (path for path in output_root.rglob("*") if path.is_file()),
            key=lambda path: path.relative_to(output_root).as_posix(),
        ):
            entries.append(
                (build_file.relative_to(output_root).as_posix(), build_file)
            )
    return tuple(entries)


def _verify_build_output_manifest(
    result: AuditResult,
    manifest: tuple[tuple[str, Path], ...],
    packaged_payloads: dict[str, bytes],
    source_root: Path,
) -> None:
    result.build_output_manifest_checked_count = len(manifest)
    for entry, build_file in manifest:
        source_label = build_file.relative_to(source_root).as_posix()
        packaged_payload = packaged_payloads.get(entry)
        if packaged_payload is None:
            result.failures.append(
                "build output file missing from production JAR: "
                f"{entry} ({source_label})"
            )
            continue
        try:
            build_payload = build_file.read_bytes()
        except OSError as exc:
            result.failures.append(
                f"cannot read build output file {source_label}: {exc}"
            )
            continue
        if packaged_payload != build_payload:
            result.failures.append(
                "production JAR payload differs from build output: "
                f"{entry} ({source_label})"
            )


def _parse_properties(payload: bytes, entry: str) -> tuple[dict[str, str], list[str]]:
    """Parse the simple key/value subset used by OptiFine CTM rules."""
    try:
        text = payload.decode("utf-8")
    except UnicodeDecodeError as exc:
        return {}, [f"invalid UTF-8 CTM property entry {entry}: {exc}"]

    properties: dict[str, str] = {}
    failures: list[str] = []
    for line_number, raw_line in enumerate(text.splitlines(), start=1):
        line = raw_line.strip()
        if not line or line.startswith(("#", "!")):
            continue
        if "=" not in line:
            failures.append(
                f"invalid CTM property line {entry}:{line_number}: {raw_line!r}"
            )
            continue
        key, value = (part.strip() for part in line.split("=", 1))
        if not key:
            failures.append(f"empty CTM property key {entry}:{line_number}")
            continue
        if key in properties:
            failures.append(f"duplicate CTM property key {key!r} in {entry}")
            continue
        properties[key] = value
    return properties, failures


def _verify_required_json_sources(
    result: AuditResult,
    parsed_json: dict[str, Any],
    source_root: Path,
) -> None:
    if not source_root.is_dir():
        result.failures.append(
            f"authoritative source root does not exist: {source_root}"
        )
        return

    for entry in REQUIRED_JSON_ENTRIES:
        packaged = parsed_json.get(entry)
        if entry not in parsed_json:
            continue

        source_path = _authoritative_json_path(source_root, entry)
        if source_path is None:
            result.failures.append(
                f"missing authoritative source for required JSON: {entry}"
            )
            continue

        try:
            source_document = json.loads(source_path.read_text(encoding="utf-8"))
        except (OSError, UnicodeError, json.JSONDecodeError) as exc:
            source_label = source_path.relative_to(source_root).as_posix()
            result.failures.append(
                f"invalid authoritative JSON source {source_label}: {exc}"
            )
            continue

        expected = source_document
        if entry == "fabric.mod.json":
            if not isinstance(source_document, dict):
                result.failures.append(
                    "authoritative fabric.mod.json must decode to a JSON object"
                )
                continue
            if source_document.get("version") != "${version}":
                result.failures.append(
                    "authoritative fabric.mod.json version must be exactly '${version}'"
                )
                continue
            source_suggestions = source_document.get("suggests")
            if (
                not isinstance(source_suggestions, dict)
                or source_suggestions.get("sereneseasons")
                != "${serene_seasons_version}"
            ):
                result.failures.append(
                    "authoritative fabric.mod.json suggests.sereneseasons "
                    "must be exactly '${serene_seasons_version}'"
                )
                continue
            if not isinstance(packaged, dict):
                continue
            packaged_version = packaged.get("version")
            if (
                not isinstance(packaged_version, str)
                or not packaged_version.strip()
                or packaged_version == "${version}"
            ):
                result.failures.append(
                    "packaged fabric.mod.json version must be a resolved non-empty string"
                )
                continue
            expected = copy.deepcopy(source_document)
            expected["version"] = packaged_version
            expected["suggests"]["sereneseasons"] = (
                REQUIRED_FABRIC_SUGGESTIONS["sereneseasons"]
            )

        if packaged != expected:
            source_label = source_path.relative_to(source_root).as_posix()
            result.failures.append(
                "required JSON differs from authoritative source: "
                f"{entry} ({source_label})"
            )
            continue
        result.required_json_source_match_count += 1


def audit_jar(jar_path: Path, source_root: Path = REPOSITORY_ROOT) -> AuditResult:
    """Audit *jar_path* against authoritative JSON below *source_root*."""
    path = jar_path.resolve()
    authoritative_root = source_root.resolve()
    result = AuditResult(jar_path=path)
    build_output_manifest = _build_output_manifest(authoritative_root)
    build_output_entry_names = {
        entry for entry, _build_file in build_output_manifest
    }

    if not path.is_file():
        result.failures.append(f"production JAR does not exist: {path}")
        return result

    try:
        result.size_bytes = path.stat().st_size
        result.sha256 = _sha256(path)
    except OSError as exc:
        result.failures.append(f"cannot read production JAR {path}: {exc}")
        return result

    parsed_json: dict[str, Any] = {}
    required_class_payloads: dict[str, bytes] = {}
    required_png_payloads: dict[str, bytes] = {}
    build_output_payloads: dict[str, bytes] = {}
    beam_ctm_property_payload: bytes | None = None
    try:
        with ZipFile(path, "r") as archive:
            infos = archive.infolist()
            result.entry_count = len(infos)
            names = [info.filename for info in infos]
            unique_names = set(names)
            result.required_present_count = sum(
                entry in unique_names for entry in REQUIRED_ENTRIES
            )
            result.forbidden_exact_absent_count = sum(
                entry not in unique_names for entry in FORBIDDEN_EXACT_ENTRIES
            )

            duplicate_names = sorted(
                name for name, count in Counter(names).items() if count > 1
            )
            for name in duplicate_names:
                result.failures.append(f"duplicate archive entry: {name}")

            for required in REQUIRED_ENTRIES:
                if required not in unique_names:
                    result.failures.append(f"missing required entry: {required}")
            for forbidden in FORBIDDEN_EXACT_ENTRIES:
                if forbidden in unique_names:
                    result.failures.append(
                        "forbidden internal/state-only block inventory entry: "
                        f"{forbidden}"
                    )

            nested_fabric_metadata = sorted(
                name
                for name in unique_names
                if name.lower().endswith("/fabric.mod.json")
            )
            for name in nested_fabric_metadata:
                result.failures.append(
                    f"nested Fabric metadata leaked into JAR: {name}"
                )

            for info in sorted(infos, key=lambda item: item.filename):
                name = info.filename
                result.failures.extend(_path_failures(name))
                if info.is_dir():
                    continue

                result.file_count += 1
                try:
                    payload = archive.read(info)
                except (BadZipFile, OSError, RuntimeError) as exc:
                    result.failures.append(f"cannot read archive entry {name}: {exc}")
                    continue

                lowered_payload = payload.lower()
                for token in FORBIDDEN_TEXT:
                    if token in lowered_payload:
                        result.failures.append(
                            f"forbidden text {token.decode('ascii')!r} found in {name}"
                        )

                if name in REQUIRED_CLASS_ENTRIES:
                    required_class_payloads[name] = payload
                if name in REQUIRED_PNG_ENTRIES:
                    required_png_payloads[name] = payload
                if name == BEAM_CTM_PROPERTY_ENTRY:
                    beam_ctm_property_payload = payload
                if name in build_output_entry_names:
                    build_output_payloads[name] = payload

                if name.lower().endswith(".json"):
                    result.json_count += 1
                    try:
                        text = payload.decode("utf-8")
                        parsed_json[name] = json.loads(text)
                        result.json_parsed_count += 1
                    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
                        result.failures.append(f"invalid JSON entry {name}: {exc}")
    except (BadZipFile, OSError) as exc:
        result.failures.append(f"cannot open production JAR {path}: {exc}")
        return result

    _verify_build_output_manifest(
        result,
        build_output_manifest,
        build_output_payloads,
        authoritative_root,
    )

    for entry in REQUIRED_CLASS_ENTRIES:
        payload = required_class_payloads.get(entry)
        if payload is None:
            continue
        valid = True
        if not payload.startswith(CLASS_MAGIC):
            result.failures.append(
                f"required class does not start with CAFEBABE: {entry}"
            )
            valid = False
        if len(payload) < MIN_REQUIRED_CLASS_BYTES:
            result.failures.append(
                f"required class is too small to be nontrivial: {entry} "
                f"({len(payload)} bytes; minimum {MIN_REQUIRED_CLASS_BYTES})"
            )
            valid = False
        if valid:
            result.required_class_valid_count += 1

    for entry in REQUIRED_PNG_ENTRIES:
        payload = required_png_payloads.get(entry)
        if payload is None:
            continue
        valid = True
        if not payload.startswith(PNG_MAGIC):
            result.failures.append(f"required PNG has an invalid signature: {entry}")
            valid = False
        if len(payload) < MIN_REQUIRED_PNG_BYTES:
            result.failures.append(
                f"required PNG is too small to be nontrivial: {entry} "
                f"({len(payload)} bytes; minimum {MIN_REQUIRED_PNG_BYTES})"
            )
            valid = False
        if valid:
            result.required_png_valid_count += 1

    if beam_ctm_property_payload is not None:
        properties, property_failures = _parse_properties(
            beam_ctm_property_payload,
            BEAM_CTM_PROPERTY_ENTRY,
        )
        result.failures.extend(property_failures)
        property_contract_valid = not property_failures
        for key, expected_value in REQUIRED_BEAM_CTM_PROPERTIES.items():
            actual_value = properties.get(key)
            if actual_value != expected_value:
                result.failures.append(
                    f"beam CTM property {key!r} must be exactly "
                    f"{expected_value!r}; found {actual_value!r}"
                )
                property_contract_valid = False
        unexpected_keys = sorted(
            set(properties) - set(REQUIRED_BEAM_CTM_PROPERTIES)
        )
        if unexpected_keys:
            result.failures.append(
                "unexpected beam CTM property keys: " + ", ".join(unexpected_keys)
            )
            property_contract_valid = False
        if property_contract_valid:
            result.required_ctm_property_valid_count = 1

    _verify_required_json_sources(result, parsed_json, authoritative_root)

    fabric_mod = parsed_json.get("fabric.mod.json")
    if not isinstance(fabric_mod, dict):
        result.failures.append("fabric.mod.json must decode to a JSON object")
    else:
        if fabric_mod.get("id") != "before_the_blight":
            result.failures.append(
                "fabric.mod.json id must be exactly 'before_the_blight'"
            )
        depends = fabric_mod.get("depends")
        if not isinstance(depends, dict):
            result.failures.append("fabric.mod.json depends must be a JSON object")
        else:
            missing_dependencies = sorted(
                set(REQUIRED_FABRIC_DEPENDENCIES) - set(depends)
            )
            extra_dependencies = sorted(
                set(depends) - set(REQUIRED_FABRIC_DEPENDENCIES)
            )
            if missing_dependencies:
                result.failures.append(
                    "fabric.mod.json depends is missing required keys: "
                    + ", ".join(missing_dependencies)
                )
            if extra_dependencies:
                result.failures.append(
                    "fabric.mod.json depends has unexpected keys: "
                    + ", ".join(extra_dependencies)
                )
            for dependency, expected_predicate in (
                REQUIRED_FABRIC_DEPENDENCIES.items()
            ):
                actual_predicate = depends.get(dependency)
                if actual_predicate != expected_predicate:
                    result.failures.append(
                        f"fabric.mod.json depends.{dependency} must be exactly "
                        f"{expected_predicate!r}; found {actual_predicate!r}"
                    )

        recommendations = fabric_mod.get("recommends")
        if recommendations not in (None, {}):
            result.failures.append(
                "fabric.mod.json recommends must be absent or empty"
            )

        suggestions = fabric_mod.get("suggests")
        if not isinstance(suggestions, dict):
            result.failures.append(
                "fabric.mod.json suggests must be a JSON object"
            )
        else:
            missing_suggestions = sorted(
                set(REQUIRED_FABRIC_SUGGESTIONS) - set(suggestions)
            )
            extra_suggestions = sorted(
                set(suggestions) - set(REQUIRED_FABRIC_SUGGESTIONS)
            )
            if missing_suggestions:
                result.failures.append(
                    "fabric.mod.json suggests is missing required keys: "
                    + ", ".join(missing_suggestions)
                )
            if extra_suggestions:
                result.failures.append(
                    "fabric.mod.json suggests has unexpected keys: "
                    + ", ".join(extra_suggestions)
                )
            for suggestion, expected_predicate in (
                REQUIRED_FABRIC_SUGGESTIONS.items()
            ):
                actual_predicate = suggestions.get(suggestion)
                if actual_predicate != expected_predicate:
                    result.failures.append(
                        f"fabric.mod.json suggests.{suggestion} must be exactly "
                        f"{expected_predicate!r}; found {actual_predicate!r}"
                    )

        nested_jars = fabric_mod.get("jars")
        if nested_jars not in (None, []):
            result.failures.append("fabric.mod.json jars must be absent or empty")

    return result


def render_result(result: AuditResult) -> str:
    """Render a stable, timestamp-free report suitable for retained evidence."""
    status = "PASS" if result.passed else "FAIL"
    lines = [f"{status}: On Old Mountain Time production JAR audit"]
    if result.failures:
        lines.extend(f"  - {failure}" for failure in result.failures)
    lines.append(
        "SUMMARY: "
        f"archive_entries={result.entry_count} "
        f"file_entries={result.file_count} "
        f"json_entries={result.json_count} "
        f"json_parsed={result.json_parsed_count} "
        f"required_entries_present={result.required_present_count}/{len(REQUIRED_ENTRIES)} "
        "forbidden_exact_entries_absent="
        f"{result.forbidden_exact_absent_count}/{len(FORBIDDEN_EXACT_ENTRIES)} "
        f"required_classes_valid={result.required_class_valid_count}/{len(REQUIRED_CLASS_ENTRIES)} "
        f"required_pngs_valid={result.required_png_valid_count}/{len(REQUIRED_PNG_ENTRIES)} "
        "required_ctm_properties_valid="
        f"{result.required_ctm_property_valid_count}/1 "
        "required_json_source_matches="
        f"{result.required_json_source_match_count}/{len(REQUIRED_JSON_ENTRIES)} "
        "build_output_files_checked="
        f"{result.build_output_manifest_checked_count} "
        f"size_bytes={result.size_bytes} "
        f"sha256={result.sha256 or 'UNAVAILABLE'}"
    )
    return "\n".join(lines) + "\n"


def write_report_atomic(report_path: Path, report: str) -> None:
    """Atomically replace *report_path* with UTF-8 *report*."""
    target = report_path.resolve()
    target.parent.mkdir(parents=True, exist_ok=True)
    temporary_name: str | None = None
    try:
        with tempfile.NamedTemporaryFile(
            mode="w",
            encoding="utf-8",
            newline="\n",
            dir=target.parent,
            prefix=f".{target.name}.",
            suffix=".tmp",
            delete=False,
        ) as handle:
            temporary_name = handle.name
            handle.write(report)
            handle.flush()
            os.fsync(handle.fileno())
        os.replace(temporary_name, target)
        temporary_name = None
    finally:
        if temporary_name is not None:
            try:
                Path(temporary_name).unlink()
            except FileNotFoundError:
                pass


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Fail-closed audit of the On Old Mountain Time production JAR."
    )
    parser.add_argument(
        "jar",
        nargs="?",
        type=Path,
        default=None,
        help=(
            "JAR to audit (default: resolve archives_base_name and mod_version "
            "from gradle.properties)"
        ),
    )
    parser.add_argument(
        "--report",
        type=Path,
        help="atomically write the same deterministic audit output to this path",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    try:
        jar_path = args.jar or resolve_default_jar(REPOSITORY_ROOT)
    except ValueError as exc:
        print(f"ERROR: cannot resolve the production JAR: {exc}")
        return 2
    result = audit_jar(jar_path, source_root=REPOSITORY_ROOT)
    report = render_result(result)
    print(report, end="")
    if args.report is not None:
        try:
            write_report_atomic(args.report, report)
        except OSError as exc:
            print(f"ERROR: cannot write report {args.report.resolve()}: {exc}")
            return 2
    return 0 if result.passed else 1


if __name__ == "__main__":
    raise SystemExit(main())
