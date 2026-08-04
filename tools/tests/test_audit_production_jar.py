#!/usr/bin/env python3
"""Focused unit tests for the production-JAR audit."""

from __future__ import annotations

import json
import sys
import tempfile
import unittest
import warnings
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile


TOOLS_TEST_DIR = Path(__file__).resolve().parent
if str(TOOLS_TEST_DIR) not in sys.path:
    sys.path.insert(0, str(TOOLS_TEST_DIR))

import audit_production_jar as audit  # noqa: E402


VALID_CLASS_PAYLOAD = (
    audit.CLASS_MAGIC + b"\x00\x00\x00\x41\x00\x02" + b"synthetic-required-class"
)
VALID_PNG_PAYLOAD = audit.PNG_MAGIC + b"synthetic-png-payload"
VALID_BEAM_CTM_PROPERTIES = "\n".join(
    f"{key}={value}" for key, value in audit.REQUIRED_BEAM_CTM_PROPERTIES.items()
).encode("utf-8")
EXPANDED_RELEASE_SENTINELS = {
    "net/beforetheblight/interaction/SawingTrestleStateMachine.class",
    "net/beforetheblight/item/FrameSawItem.class",
    "net/beforetheblight/interaction/SplittingStateMachine.class",
    "net/beforetheblight/item/FroeItem.class",
    "net/beforetheblight/item/WoodenMaulItem.class",
    "net/beforetheblight/block/CornCropBlock.class",
    "net/beforetheblight/block/DryingCornBundleBlock.class",
    "net/beforetheblight/registry/ModSounds.class",
    "net/beforetheblight/worldgen/biome/CoveMinimumFootprintFilter.class",
    "net/beforetheblight/worldgen/biome/GrassyBaldBiomeTags.class",
    "net/beforetheblight/worldgen/structure/AppalachianCornCribStructure.class",
    "net/beforetheblight/worldgen/structure/AppalachianHomesteadStructure.class",
    "data/before_the_blight/worldgen/biome/hemlock_beech_cove.json",
    "data/before_the_blight/lithostitched/biome_injector/grassy_bald.json",
    "data/before_the_blight/worldgen/structure/appalachian_homestead.json",
    "data/before_the_blight/worldgen/structure/appalachian_corn_crib.json",
    "data/before_the_blight/worldgen/structure_set/appalachian_corn_cribs.json",
    "data/before_the_blight/structure/appalachian_demo_cabin.nbt",
    "data/before_the_blight/structure/appalachian_corn_crib.nbt",
    "data/before_the_blight/structure/appalachian_springhouse.nbt",
    "data/before_the_blight/advancement/onboarding/enter_appalachians.json",
    "data/before_the_blight/advancement/onboarding/saw_rough_boards.json",
    "data/before_the_blight/advancement/onboarding/discover_homestead.json",
    "assets/before_the_blight/sounds.json",
    "assets/before_the_blight/blockstates/loaded_sawing_trestles.json",
    "assets/before_the_blight/blockstates/loaded_splitting_stump.json",
    "assets/before_the_blight/blockstates/corn.json",
    "assets/before_the_blight/blockstates/drying_corn_bundle.json",
    "assets/before_the_blight/textures/item/frame_saw_n.png",
    "assets/before_the_blight/textures/block/splitting_stump_side_n.png",
    "assets/before_the_blight/textures/block/corn_stage_7_n.png",
    "assets/before_the_blight/textures/block/corn_bundle_stage_3_n.png",
    "assets/before_the_blight/textures/block/hemlock_foliage_n.png",
}
FEATURE_UPGRADE_SENTINELS = set(audit.FEATURE_UPGRADE_REQUIRED_ENTRIES)
SERENE_SEASONS_SENTINELS = set(audit.SERENE_SEASONS_REQUIRED_ENTRIES)
EXPECTED_REQUIRED_ENTRY_COUNT = 300
EXPECTED_FEATURE_UPGRADE_SENTINEL_COUNT = 103
FOLLOWUP_RESOURCE_SENTINELS = {
    "assets/before_the_blight/models/block/hewn_chestnut_post_rail_connection.json",
    "assets/before_the_blight/models/block/hewn_chestnut_post_wall_connection.json",
    "assets/before_the_blight/models/block/hewn_chestnut_post_wall_connection_tall.json",
    "assets/before_the_blight/textures/block/rocking_chair_woven_seat.png",
    "assets/before_the_blight/textures/block/rocking_chair_woven_seat_n.png",
    "assets/before_the_blight/textures/block/rocking_chair_woven_seat_s.png",
}
FEATURE_RUNTIME_SOURCE_MATCH_CLASSES = (
    "net/beforetheblight/registry/ModBlocks.class",
    "net/beforetheblight/registry/ModFurniture.class",
    "net/beforetheblight/block/HewnChestnutPostBlock.class",
    "net/beforetheblight/block/RockingChairBlock.class",
    "net/beforetheblight/entity/RockingChairSeatEntity.class",
    "net/beforetheblight/client/RockingChairClient.class",
    "net/beforetheblight/client/render/RockingChairSeatRenderer.class",
    "net/beforetheblight/client/render/RockingChairSeatRenderState.class",
)


def valid_entries() -> dict[str, bytes]:
    entries: dict[str, bytes] = {}
    for name in audit.REQUIRED_ENTRIES:
        if name == "fabric.mod.json":
            document = {
                "schemaVersion": 1,
                "id": "before_the_blight",
                "version": "0.0.1-test",
                "depends": dict(audit.REQUIRED_FABRIC_DEPENDENCIES),
                "suggests": dict(audit.REQUIRED_FABRIC_SUGGESTIONS),
            }
            entries[name] = json.dumps(document).encode("utf-8")
        elif name.endswith(".json"):
            entries[name] = b"{}"
        elif name.endswith(".png"):
            entries[name] = VALID_PNG_PAYLOAD
        elif name == audit.BEAM_CTM_PROPERTY_ENTRY:
            entries[name] = VALID_BEAM_CTM_PROPERTIES
        else:
            entries[name] = VALID_CLASS_PAYLOAD
    return entries


def write_authoritative_sources(
    source_root: Path,
    packaged_entries: dict[str, bytes],
) -> None:
    for entry in audit.REQUIRED_JSON_ENTRIES:
        document = json.loads(packaged_entries[entry].decode("utf-8"))
        if entry == "fabric.mod.json":
            document["version"] = "${version}"
            document["suggests"]["sereneseasons"] = (
                "${serene_seasons_version}"
            )
        if (
            entry in {"fabric.mod.json", "before_the_blight.mixins.json"}
            or "/lithostitched/" in entry
        ):
            source_path = source_root / "src/main/resources" / entry
        else:
            source_path = source_root / "src/main/generated" / entry
        source_path.parent.mkdir(parents=True, exist_ok=True)
        source_path.write_text(json.dumps(document), encoding="utf-8")


def write_jar(path: Path, entries: dict[str, bytes]) -> None:
    with ZipFile(path, "w", ZIP_DEFLATED) as archive:
        for name, payload in sorted(entries.items()):
            archive.writestr(name, payload)


class ProductionJarAuditTests(unittest.TestCase):
    def test_default_jar_tracks_safe_gradle_artifact_identity(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            (root / "gradle.properties").write_text(
                "# release candidate\n"
                "archives_base_name=before-the-blight\n"
                "mod_version=0.4.0-alpha.2+fabric\n",
                encoding="utf-8",
            )

            self.assertEqual(
                audit.resolve_default_jar(root),
                root
                / "build/libs/before-the-blight-0.4.0-alpha.2+fabric.jar",
            )

            invalid_properties = (
                "archives_base_name=before-the-blight\n",
                "archives_base_name=../escape\nmod_version=1.0.0\n",
                (
                    "archives_base_name=before-the-blight\n"
                    "archives_base_name=duplicate\n"
                    "mod_version=1.0.0\n"
                ),
            )
            for index, content in enumerate(invalid_properties):
                with self.subTest(index=index):
                    (root / "gradle.properties").write_text(
                        content,
                        encoding="utf-8",
                    )
                    with self.assertRaises(ValueError):
                        audit.resolve_default_jar(root)

    def test_expanded_release_subsystem_sentinels_are_required(self) -> None:
        self.assertLessEqual(
            EXPANDED_RELEASE_SENTINELS,
            set(audit.REQUIRED_ENTRIES),
        )

        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            jar = root / "missing-expanded-release-sentinels.jar"
            entries = valid_entries()
            write_authoritative_sources(root, entries)
            for sentinel in EXPANDED_RELEASE_SENTINELS:
                del entries[sentinel]
            write_jar(jar, entries)

            result = audit.audit_jar(jar, source_root=root)
            failures = "\n".join(result.failures)

            self.assertFalse(result.passed)
            for sentinel in EXPANDED_RELEASE_SENTINELS:
                self.assertIn(f"missing required entry: {sentinel}", failures)

    def test_feature_upgrade_sentinels_are_individually_required(self) -> None:
        self.assertTrue(FEATURE_UPGRADE_SENTINELS)
        self.assertEqual(
            len(audit.REQUIRED_ENTRIES),
            EXPECTED_REQUIRED_ENTRY_COUNT,
        )
        self.assertEqual(
            len(audit.FEATURE_UPGRADE_REQUIRED_ENTRIES),
            EXPECTED_FEATURE_UPGRADE_SENTINEL_COUNT,
        )
        self.assertLessEqual(
            FOLLOWUP_RESOURCE_SENTINELS,
            FEATURE_UPGRADE_SENTINELS,
        )
        self.assertEqual(
            len(FEATURE_UPGRADE_SENTINELS),
            len(audit.FEATURE_UPGRADE_REQUIRED_ENTRIES),
            "Feature-upgrade sentinels must not contain duplicate archive paths.",
        )
        self.assertLessEqual(
            FEATURE_UPGRADE_SENTINELS,
            set(audit.REQUIRED_ENTRIES),
        )

        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            baseline_entries = valid_entries()
            write_authoritative_sources(root, baseline_entries)

            for index, missing_entry in enumerate(
                audit.FEATURE_UPGRADE_REQUIRED_ENTRIES
            ):
                with self.subTest(missing_entry=missing_entry):
                    jar = root / f"missing-feature-upgrade-entry-{index}.jar"
                    entries = dict(baseline_entries)
                    del entries[missing_entry]
                    write_jar(jar, entries)

                    result = audit.audit_jar(jar, source_root=root)

                    self.assertFalse(result.passed)
                    self.assertIn(
                        f"missing required entry: {missing_entry}",
                        result.failures,
                    )

    def test_serene_bridge_and_tag_sentinels_are_individually_required(self) -> None:
        self.assertTrue(SERENE_SEASONS_SENTINELS)
        self.assertEqual(
            len(SERENE_SEASONS_SENTINELS),
            len(audit.SERENE_SEASONS_REQUIRED_ENTRIES),
            "Serene Seasons sentinels must not contain duplicate archive paths.",
        )
        self.assertLessEqual(
            SERENE_SEASONS_SENTINELS,
            set(audit.REQUIRED_ENTRIES),
        )

        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            baseline_entries = valid_entries()
            write_authoritative_sources(root, baseline_entries)

            for index, missing_entry in enumerate(
                audit.SERENE_SEASONS_REQUIRED_ENTRIES
            ):
                with self.subTest(missing_entry=missing_entry):
                    jar = root / f"missing-serene-entry-{index}.jar"
                    entries = dict(baseline_entries)
                    del entries[missing_entry]
                    write_jar(jar, entries)

                    result = audit.audit_jar(jar, source_root=root)

                    self.assertFalse(result.passed)
                    self.assertIn(
                        f"missing required entry: {missing_entry}",
                        result.failures,
                    )

    def test_feature_runtime_classes_match_current_build_outputs(self) -> None:
        self.assertLessEqual(
            set(FEATURE_RUNTIME_SOURCE_MATCH_CLASSES),
            FEATURE_UPGRADE_SENTINELS,
        )

        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            main_class_root = root / "build/classes/java/main"
            client_class_root = root / "build/classes/java/client"
            (root / "build/resources/main").mkdir(parents=True)

            build_outputs: dict[str, bytes] = {}
            for index, entry in enumerate(FEATURE_RUNTIME_SOURCE_MATCH_CLASSES):
                payload = VALID_CLASS_PAYLOAD + f"-feature-{index}".encode("ascii")
                build_outputs[entry] = payload
                output_root = (
                    client_class_root
                    if entry.startswith("net/beforetheblight/client/")
                    else main_class_root
                )
                output_file = output_root / Path(entry)
                output_file.parent.mkdir(parents=True, exist_ok=True)
                output_file.write_bytes(payload)

            baseline_entries = valid_entries()
            baseline_entries.update(build_outputs)
            write_authoritative_sources(root, baseline_entries)

            baseline_jar = root / "feature-runtime-baseline.jar"
            write_jar(baseline_jar, baseline_entries)
            baseline_result = audit.audit_jar(baseline_jar, source_root=root)
            self.assertTrue(baseline_result.passed, baseline_result.failures)
            self.assertEqual(
                baseline_result.build_output_manifest_checked_count,
                len(build_outputs),
            )

            for index, entry in enumerate(FEATURE_RUNTIME_SOURCE_MATCH_CLASSES):
                with self.subTest(entry=entry):
                    jar = root / f"feature-runtime-mismatch-{index}.jar"
                    entries = dict(baseline_entries)
                    entries[entry] += b"-stale"
                    write_jar(jar, entries)

                    result = audit.audit_jar(jar, source_root=root)

                    self.assertFalse(result.passed)
                    self.assertTrue(
                        any(
                            failure.startswith(
                                "production JAR payload differs from build output: "
                                f"{entry} "
                            )
                            for failure in result.failures
                        ),
                        result.failures,
                    )

    def test_ridge_footprint_filter_runtime_entries_are_individually_required(
        self,
    ) -> None:
        footprint_entries = (
            "before_the_blight.mixins.json",
            "net/beforetheblight/mixin/InjectorBiomeSourceMixin.class",
            "net/beforetheblight/mixin/InjectorBiomeSourceMixin$FilterThreadState.class",
            "net/beforetheblight/worldgen/biome/RawInjectorBiomeSourceAccess.class",
            "net/beforetheblight/worldgen/biome/RidgeMinimumFootprintFilter.class",
            "net/beforetheblight/worldgen/biome/RidgeMinimumFootprintFilter$BoundedLruMap.class",
            "net/beforetheblight/worldgen/biome/RidgeMinimumFootprintFilter$Classifier.class",
            "net/beforetheblight/worldgen/biome/RidgeMinimumFootprintFilter$Decision.class",
            "net/beforetheblight/worldgen/biome/RidgeMinimumFootprintFilter$QuartCoordinate.class",
            "net/beforetheblight/worldgen/biome/RidgeMinimumFootprintFilter$RawRidgeSampler.class",
        )
        self.assertLessEqual(set(footprint_entries), set(audit.REQUIRED_ENTRIES))

        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            baseline_entries = valid_entries()
            write_authoritative_sources(root, baseline_entries)

            for index, missing_entry in enumerate(footprint_entries):
                with self.subTest(missing_entry=missing_entry):
                    jar = root / f"missing-ridge-footprint-entry-{index}.jar"
                    entries = dict(baseline_entries)
                    del entries[missing_entry]
                    write_jar(jar, entries)

                    result = audit.audit_jar(jar, source_root=root)

                    self.assertFalse(result.passed)
                    self.assertIn(
                        f"missing required entry: {missing_entry}",
                        result.failures,
                    )

    def test_each_transitive_ridge_tree_entry_is_individually_required(self) -> None:
        transitive_ridge_entries = (
            "data/before_the_blight/worldgen/configured_feature/chestnut_ordinary.json",
            "data/before_the_blight/worldgen/configured_feature/chestnut_mature.json",
            "data/before_the_blight/worldgen/configured_feature/chestnut_old_growth.json",
            "data/before_the_blight/worldgen/placed_feature/chestnut_ordinary_checked.json",
            "data/before_the_blight/worldgen/placed_feature/chestnut_mature_checked.json",
            "data/before_the_blight/worldgen/placed_feature/chestnut_old_growth_checked.json",
        )
        self.assertLessEqual(
            set(transitive_ridge_entries),
            set(audit.REQUIRED_ENTRIES),
        )

        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            baseline_entries = valid_entries()
            write_authoritative_sources(root, baseline_entries)

            for index, missing_entry in enumerate(transitive_ridge_entries):
                with self.subTest(missing_entry=missing_entry):
                    jar = root / f"missing-transitive-ridge-entry-{index}.jar"
                    entries = dict(baseline_entries)
                    del entries[missing_entry]
                    write_jar(jar, entries)

                    result = audit.audit_jar(jar, source_root=root)

                    self.assertFalse(result.passed)
                    self.assertIn(
                        f"missing required entry: {missing_entry}",
                        result.failures,
                    )

    def test_ridge_selector_runtime_and_data_graph_entries_are_required(self) -> None:
        ridge_entries = {
            "net/beforetheblight/worldgen/feature/ModFeatures.class",
            "net/beforetheblight/worldgen/feature/RidgeTreeSelectorFeature.class",
            "net/beforetheblight/worldgen/feature/configurations/RidgeTreeConfiguration.class",
            "data/before_the_blight/worldgen/configured_feature/chestnut_edge_trees.json",
            "data/before_the_blight/worldgen/configured_feature/chestnut_fallen.json",
            "data/before_the_blight/worldgen/configured_feature/chestnut_pile_patch.json",
            "data/before_the_blight/worldgen/placed_feature/chestnut_edge_trees_checked.json",
            "data/before_the_blight/worldgen/placed_feature/chestnut_forest_checked.json",
            "data/before_the_blight/worldgen/placed_feature/chestnut_fallen.json",
            "data/before_the_blight/worldgen/placed_feature/chestnut_pile_patch.json",
            "data/minecraft/tags/worldgen/biome/is_mountain.json",
            "data/minecraft/tags/worldgen/biome/has_structure/village_plains.json",
        }
        self.assertLessEqual(ridge_entries, set(audit.REQUIRED_ENTRIES))

        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            jar = root / "missing-ridge-graph.jar"
            entries = valid_entries()
            write_authoritative_sources(root, entries)
            for entry in ridge_entries:
                del entries[entry]
            write_jar(jar, entries)

            result = audit.audit_jar(jar, source_root=root)

            self.assertFalse(result.passed)
            failures = "\n".join(result.failures)
            for entry in ridge_entries:
                self.assertIn(f"missing required entry: {entry}", failures)

    def test_valid_checkpoint_jar_passes_and_report_replaces_atomically(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            jar = root / "production.jar"
            report_path = root / "evidence" / "jar-audit.txt"
            entries = valid_entries()
            write_authoritative_sources(root, entries)
            write_jar(jar, entries)

            result = audit.audit_jar(jar, source_root=root)
            report = audit.render_result(result)
            audit.write_report_atomic(report_path, report)

            self.assertTrue(result.passed, result.failures)
            self.assertEqual(result.entry_count, len(audit.REQUIRED_ENTRIES))
            self.assertEqual(result.json_count, len(audit.REQUIRED_JSON_ENTRIES))
            self.assertEqual(
                result.json_parsed_count,
                len(audit.REQUIRED_JSON_ENTRIES),
            )
            self.assertIn(
                f"required_entries_present={len(audit.REQUIRED_ENTRIES)}/"
                f"{len(audit.REQUIRED_ENTRIES)}",
                report,
            )
            self.assertIn(
                f"forbidden_exact_entries_absent={len(audit.FORBIDDEN_EXACT_ENTRIES)}/"
                f"{len(audit.FORBIDDEN_EXACT_ENTRIES)}",
                report,
            )
            self.assertIn(
                f"required_classes_valid={len(audit.REQUIRED_CLASS_ENTRIES)}/"
                f"{len(audit.REQUIRED_CLASS_ENTRIES)}",
                report,
            )
            self.assertIn(
                f"required_pngs_valid={len(audit.REQUIRED_PNG_ENTRIES)}/"
                f"{len(audit.REQUIRED_PNG_ENTRIES)}",
                report,
            )
            self.assertIn("required_ctm_properties_valid=1/1", report)
            self.assertIn(
                f"required_json_source_matches={len(audit.REQUIRED_JSON_ENTRIES)}/"
                f"{len(audit.REQUIRED_JSON_ENTRIES)}",
                report,
            )
            self.assertIn("build_output_files_checked=0", report)
            self.assertIn("sha256=", report)
            self.assertEqual(report_path.read_text(encoding="utf-8"), report)
            self.assertEqual(list(report_path.parent.glob("*.tmp")), [])

    def test_build_output_manifest_rejects_missing_and_mismatched_core_files(
        self,
    ) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            class_root = root / "build/classes/java/main"
            resource_root = root / "build/resources/main"
            build_outputs = {
                "net/beforetheblight/registry/ModBlocks.class": (
                    class_root,
                    VALID_CLASS_PAYLOAD + b"-mod-blocks",
                ),
                "net/beforetheblight/block/ChestnutSaplingBlock.class": (
                    class_root,
                    VALID_CLASS_PAYLOAD + b"-sapling-block",
                ),
                "assets/before_the_blight/blockstates/chestnut_leaves.json": (
                    resource_root,
                    b'{"variants":{"distance=1":{"model":"leaves"}}}',
                ),
                "assets/before_the_blight/textures/block/chestnut_leaves.png": (
                    resource_root,
                    VALID_PNG_PAYLOAD + b"-chestnut-leaves",
                ),
            }
            for entry, (output_root, payload) in build_outputs.items():
                output_file = output_root / Path(entry)
                output_file.parent.mkdir(parents=True, exist_ok=True)
                output_file.write_bytes(payload)

            baseline_entries = valid_entries()
            baseline_entries.update(
                {entry: payload for entry, (_root, payload) in build_outputs.items()}
            )
            write_authoritative_sources(root, baseline_entries)

            mutations = (
                ("net/beforetheblight/registry/ModBlocks.class", "missing"),
                (
                    "net/beforetheblight/block/ChestnutSaplingBlock.class",
                    "mismatched",
                ),
                (
                    "assets/before_the_blight/blockstates/chestnut_leaves.json",
                    "missing",
                ),
                (
                    "assets/before_the_blight/textures/block/chestnut_leaves.png",
                    "mismatched",
                ),
            )
            for index, (entry, mutation) in enumerate(mutations):
                with self.subTest(entry=entry, mutation=mutation):
                    jar = root / f"build-manifest-mutation-{index}.jar"
                    entries = dict(baseline_entries)
                    if mutation == "missing":
                        del entries[entry]
                    else:
                        entries[entry] += b"-jar-mismatch"
                    write_jar(jar, entries)

                    result = audit.audit_jar(jar, source_root=root)

                    self.assertFalse(result.passed)
                    self.assertEqual(
                        result.build_output_manifest_checked_count,
                        len(build_outputs),
                    )
                    failure_prefix = (
                        "build output file missing from production JAR: "
                        if mutation == "missing"
                        else "production JAR payload differs from build output: "
                    )
                    self.assertTrue(
                        any(
                            failure.startswith(f"{failure_prefix}{entry} ")
                            for failure in result.failures
                        ),
                        result.failures,
                    )

    def test_build_output_manifest_requires_present_client_class_and_resource(
        self,
    ) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            (root / "build/classes/java/main").mkdir(parents=True)
            (root / "build/resources/main").mkdir(parents=True)
            client_outputs = {
                "net/beforetheblight/client/BeforeTheBlightClient.class": (
                    root / "build/classes/java/client",
                    VALID_CLASS_PAYLOAD + b"-client-entrypoint",
                ),
                "before_the_blight.client.mixins.json": (
                    root / "build/resources/client",
                    b'{"required":true,"package":"net.beforetheblight.mixin.client"}',
                ),
            }
            for entry, (output_root, payload) in client_outputs.items():
                output_file = output_root / Path(entry)
                output_file.parent.mkdir(parents=True, exist_ok=True)
                output_file.write_bytes(payload)

            baseline_entries = valid_entries()
            baseline_entries.update(
                {entry: payload for entry, (_root, payload) in client_outputs.items()}
            )
            write_authoritative_sources(root, baseline_entries)

            for index, missing_entry in enumerate(client_outputs):
                with self.subTest(missing_entry=missing_entry):
                    jar = root / f"missing-client-output-{index}.jar"
                    entries = dict(baseline_entries)
                    del entries[missing_entry]
                    write_jar(jar, entries)

                    result = audit.audit_jar(jar, source_root=root)

                    self.assertFalse(result.passed)
                    self.assertEqual(
                        result.build_output_manifest_checked_count,
                        len(client_outputs),
                    )
                    self.assertTrue(
                        any(
                            failure.startswith(
                                "build output file missing from production JAR: "
                                f"{missing_entry} "
                            )
                            for failure in result.failures
                        ),
                        result.failures,
                    )

    def test_missing_metadata_contract_and_invalid_json_fail(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            jar = root / "broken.jar"
            entries = valid_entries()
            write_authoritative_sources(root, entries)
            del entries["net/beforetheblight/worldgen/biome/ModBiomes.class"]
            entries["fabric.mod.json"] = json.dumps(
                {
                    "id": "before_the_blight_gametest",
                    "depends": {
                        "fabricloader": ">=0.19.2",
                        "minecraft": "~26.1.1",
                        "java": ">=24",
                        "fabric-api": ">=0.155.2",
                        "lithostitched": "1.7.12",
                    },
                }
            ).encode("utf-8")
            entries["data/before_the_blight/worldgen/biome/broken.json"] = b"{not-json}"
            write_jar(jar, entries)

            result = audit.audit_jar(jar, source_root=root)
            failures = "\n".join(result.failures)

            self.assertFalse(result.passed)
            self.assertIn("missing required entry", failures)
            self.assertIn("invalid JSON entry", failures)
            self.assertIn("id must be exactly", failures)
            for dependency in audit.REQUIRED_FABRIC_DEPENDENCIES:
                self.assertIn(
                    f"depends.{dependency} must be exactly",
                    failures,
                )
            self.assertIn("forbidden text 'before_the_blight_gametest'", failures)

    def test_optional_companion_relationships_and_embedded_jars_fail(self) -> None:
        mutations = (
            ("depends", "continuity", "*"),
            ("recommends", "tectonic", "3.0.26"),
            ("suggests", "iris", "1.11.2"),
            ("jars", None, [{"file": "META-INF/jars/continuity.jar"}]),
        )
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            baseline_entries = valid_entries()
            write_authoritative_sources(root, baseline_entries)

            for index, (relationship, key, value) in enumerate(mutations):
                with self.subTest(relationship=relationship):
                    entries = dict(baseline_entries)
                    document = json.loads(entries["fabric.mod.json"].decode("utf-8"))
                    if relationship == "jars":
                        document[relationship] = value
                    else:
                        document.setdefault(relationship, {})[key] = value
                    entries["fabric.mod.json"] = json.dumps(document).encode("utf-8")
                    jar = root / f"optional-relationship-{index}.jar"
                    write_jar(jar, entries)

                    result = audit.audit_jar(jar, source_root=root)
                    failures = "\n".join(result.failures)

                    self.assertFalse(result.passed)
                    if relationship == "depends":
                        self.assertIn(
                            "depends has unexpected keys: continuity",
                            failures,
                        )
                    elif relationship == "suggests":
                        self.assertIn(
                            "suggests has unexpected keys: iris",
                            failures,
                        )
                    else:
                        self.assertIn(
                            f"fabric.mod.json {relationship} must be absent or empty",
                            failures,
                        )

    def test_serene_seasons_suggestion_is_exact_and_not_a_hard_dependency(
        self,
    ) -> None:
        mutations = (
            ("missing", {}),
            ("wrong-version", {"sereneseasons": "*"}),
            (
                "extra",
                {
                    "sereneseasons": "26.1.2.0.4",
                    "glitchcore": "26.1.2.0.2",
                },
            ),
        )
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            baseline_entries = valid_entries()
            write_authoritative_sources(root, baseline_entries)

            valid_document = json.loads(
                baseline_entries["fabric.mod.json"].decode("utf-8")
            )
            self.assertNotIn("sereneseasons", valid_document["depends"])
            self.assertEqual(
                valid_document["suggests"],
                audit.REQUIRED_FABRIC_SUGGESTIONS,
            )

            for index, (label, suggestions) in enumerate(mutations):
                with self.subTest(label=label):
                    entries = dict(baseline_entries)
                    document = json.loads(
                        entries["fabric.mod.json"].decode("utf-8")
                    )
                    document["suggests"] = suggestions
                    entries["fabric.mod.json"] = json.dumps(document).encode(
                        "utf-8"
                    )
                    jar = root / f"serene-suggestion-{index}.jar"
                    write_jar(jar, entries)

                    result = audit.audit_jar(jar, source_root=root)

                    self.assertFalse(result.passed)
                    self.assertTrue(
                        any(
                            failure.startswith("fabric.mod.json suggests")
                            for failure in result.failures
                        ),
                        result.failures,
                    )

    def test_gametest_development_and_worldgen_leaks_fail(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            jar = root / "leaky.jar"
            entries = valid_entries()
            write_authoritative_sources(root, entries)
            entries.update(
                {
                    "net/beforetheblight/gametest/WorldgenGameTests.class": b"class",
                    "src/main/java/Leaked.java": b"class Leaked {}",
                    "tools/private/audit.py": b"print('leaked')",
                    "run/test-results/result.json": b"{}",
                    "cache/nested.jar": b"fabric-gametest",
                    "data/before_the_blight/dimension/overworld.json": b"{}",
                    "data/before_the_blight/worldgen/noise/ridge.json": b"{}",
                    "data/minecraft/worldgen/noise/vanilla_leak.json": b"{}",
                    audit.FORBIDDEN_FOREST_PLACEMENT: b"{}",
                    "testmod/fabric.mod.json": b"{}",
                    "sereneseasons/api/season/SeasonHelper.class": b"class",
                    "glitchcore/config/Config.class": b"class",
                    "com/electronwill/nightconfig/core/Config.class": b"class",
                    "META-INF/versions/25/sereneseasons/api/season/Season.class": b"class",
                }
            )
            write_jar(jar, entries)

            result = audit.audit_jar(jar, source_root=root)
            failures = "\n".join(result.failures)

            self.assertFalse(result.passed)
            self.assertIn("GameTest path leaked", failures)
            self.assertIn("Java source leaked", failures)
            self.assertIn("forbidden archive path component", failures)
            self.assertIn("tools/private/audit.py", failures)
            self.assertIn("nested JAR leaked", failures)
            self.assertIn("dimension/noise/density-function data leaked", failures)
            self.assertIn(
                "data/minecraft/worldgen/noise/vanilla_leak.json",
                failures,
            )
            self.assertIn(
                "unscoped top-level chestnut forest placement leaked",
                failures,
            )
            self.assertIn("nested Fabric metadata leaked", failures)
            self.assertIn("forbidden text 'fabric-gametest'", failures)
            for leaked_namespace in (
                "sereneseasons/api/season/SeasonHelper.class",
                "glitchcore/config/Config.class",
                "com/electronwill/nightconfig/core/Config.class",
                "META-INF/versions/25/sereneseasons/api/season/Season.class",
            ):
                self.assertIn(
                    "optional companion namespace shaded into production JAR: "
                    + leaked_namespace,
                    failures,
                )

    def test_duplicate_entries_fail_closed(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            jar = root / "duplicate.jar"
            entries = valid_entries()
            write_authoritative_sources(root, entries)
            write_jar(jar, entries)
            with warnings.catch_warnings():
                warnings.simplefilter("ignore", UserWarning)
                with ZipFile(jar, "a", ZIP_DEFLATED) as archive:
                    archive.writestr("fabric.mod.json", entries["fabric.mod.json"])

            result = audit.audit_jar(jar, source_root=root)

            self.assertFalse(result.passed)
            self.assertIn(
                "duplicate archive entry: fabric.mod.json",
                result.failures,
            )

    def test_required_classes_need_magic_and_nontrivial_size(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            jar = root / "corrupt-classes.jar"
            entries = valid_entries()
            write_authoritative_sources(root, entries)
            wrong_magic = "net/beforetheblight/worldgen/biome/ModBiomes.class"
            too_small = "net/beforetheblight/worldgen/feature/ModPlacedFeatures.class"
            entries[wrong_magic] = b"not-a-java-class-but-nontrivial"
            entries[too_small] = audit.CLASS_MAGIC
            write_jar(jar, entries)

            result = audit.audit_jar(jar, source_root=root)
            failures = "\n".join(result.failures)

            self.assertFalse(result.passed)
            self.assertIn(
                f"required class does not start with CAFEBABE: {wrong_magic}",
                failures,
            )
            self.assertIn(
                f"required class is too small to be nontrivial: {too_small}",
                failures,
            )
            self.assertEqual(
                result.required_class_valid_count,
                len(audit.REQUIRED_CLASS_ENTRIES) - 2,
            )

    def test_required_json_must_match_authoritative_source(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            jar = root / "source-mismatch.jar"
            entries = valid_entries()
            write_authoritative_sources(root, entries)
            mismatch = (
                "data/before_the_blight/worldgen/configured_feature/"
                "chestnut_oak_ridge_trees.json"
            )
            entries[mismatch] = b'{"unexpected": true}'
            write_jar(jar, entries)

            result = audit.audit_jar(jar, source_root=root)

            self.assertFalse(result.passed)
            self.assertIn(
                f"required JSON differs from authoritative source: {mismatch} ",
                "\n".join(result.failures),
            )
            self.assertEqual(
                result.required_json_source_match_count,
                len(audit.REQUIRED_JSON_ENTRIES) - 1,
            )

    def test_internal_state_only_block_inventory_entries_are_forbidden(self) -> None:
        self.assertEqual(
            audit.INTERNAL_STATE_ONLY_BLOCKS,
            (
                "chestnut_hewing_log",
                "oak_hewing_log",
                "spruce_hewing_log",
                "loaded_sawing_trestles",
                "loaded_splitting_stump",
                "chestnut_pile",
                "corn",
            ),
        )
        expected_forbidden = {
            entry
            for block_id in audit.INTERNAL_STATE_ONLY_BLOCKS
            for entry in (
                f"assets/before_the_blight/items/{block_id}.json",
                f"assets/before_the_blight/models/item/{block_id}.json",
                f"data/before_the_blight/recipe/{block_id}.json",
            )
        }
        self.assertEqual(
            set(audit.FORBIDDEN_EXACT_ENTRIES),
            expected_forbidden,
        )

        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            jar = root / "obtainable-internal-state.jar"
            entries = valid_entries()
            write_authoritative_sources(root, entries)
            for forbidden in audit.FORBIDDEN_EXACT_ENTRIES:
                entries[forbidden] = b"{}"
            write_jar(jar, entries)

            result = audit.audit_jar(jar, source_root=root)
            failures = "\n".join(result.failures)

            self.assertFalse(result.passed)
            self.assertEqual(result.forbidden_exact_absent_count, 0)
            for forbidden in audit.FORBIDDEN_EXACT_ENTRIES:
                self.assertIn(
                    (
                        "forbidden internal/state-only block inventory entry: "
                        f"{forbidden}"
                    ),
                    failures,
                )

    def test_required_png_and_block_scoped_beam_ctm_contract_fail_closed(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            jar = root / "broken-client-assets.jar"
            entries = valid_entries()
            write_authoritative_sources(root, entries)
            broken_png = audit.REQUIRED_PNG_ENTRIES[0]
            entries[broken_png] = b"not-a-png"
            entries[audit.BEAM_CTM_PROPERTY_ENTRY] = (
                b"method=vertical\n"
                b"matchTiles=before_the_blight:block/hewn_chestnut_beam\n"
                b"tiles=0-3\n"
                b"connect=tile\n"
                b"faces=sides\n"
                b"orient=texture\n"
                b"innerSeams=false\n"
            )
            write_jar(jar, entries)

            result = audit.audit_jar(jar, source_root=root)
            failures = "\n".join(result.failures)

            self.assertFalse(result.passed)
            self.assertIn(
                f"required PNG has an invalid signature: {broken_png}",
                failures,
            )
            self.assertIn(
                f"required PNG is too small to be nontrivial: {broken_png}",
                failures,
            )
            self.assertIn("beam CTM property 'matchBlocks'", failures)
            self.assertIn("beam CTM property 'connect'", failures)
            self.assertIn("unexpected beam CTM property keys: matchTiles", failures)
            self.assertEqual(result.required_ctm_property_valid_count, 0)


if __name__ == "__main__":
    unittest.main()
