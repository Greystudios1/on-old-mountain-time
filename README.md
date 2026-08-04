# On Old Mountain Time

_An Appalachian survival world from another age._

On Old Mountain Time is a Fabric mod about life in a Southern Appalachian landscape shaped by chestnut forests, hand-worked timber, regional foodways, and farmstead building. The first public Alpha establishes the world, materials, tools, crops, structures, and seasonal foundation for that experience.

## Current Alpha features

- Chestnut-Oak Ridge, Hemlock Cove, and Grassy Bald biomes.
- American chestnut, eastern hemlock, American beech, and black walnut trees.
- Field, forest, old-growth, hollow, and fallen chestnut forms with regional understory plants.
- Broad-axe hewing, frame-saw cutting, and froe-and-maul splitting, including bounded stack processing.
- Corn cultivation and air-drying, cornmeal and cornbread, and chestnut gathering and roasting.
- A large Appalachian building palette of timber, fieldstone, shingles, furniture, farm tools, and domestic details.
- A sit-able Ladder-Back Rocking Chair.
- Naturally generated Appalachian homesteads and corn cribs.
- A seven-step onboarding advancement chain, organized creative inventory, and optional recipe-viewer guidance.
- Optional connected-texture, LabPBR, and seasonal resources; the base presentation remains complete without them.

## Requirements

| Component | Version |
| --- | --- |
| Minecraft | 26.1.2 |
| Java | 25 |
| Fabric Loader | 0.19.3 |
| Fabric API | 0.155.2+26.1.2 |
| Lithostitched | 1.7.13-fabric-26.1 |

Fabric API and Lithostitched are required at runtime. See [INSTALLATION.md](INSTALLATION.md) for client and server setup.

## Building

Use JDK 25 from the repository root:

```powershell
.\gradlew.bat clean build
```

On Linux or macOS:

```sh
./gradlew clean build
```

The production JAR is written to `build/libs/before-the-blight-0.1.0-alpha.1.jar`.

The executable GameTest suite is included under `src/gametest`. See [TESTING.md](TESTING.md) for the public verification commands.

## Release source

This repository contains the source snapshot corresponding to release `0.1.0-alpha.1`. The approved CurseForge artifact and its SHA-256 are recorded in [RELEASE_MANIFEST.md](RELEASE_MANIFEST.md).

The technical mod ID and registry namespace remain `before_the_blight` so existing worlds and identifiers stay compatible.

## Status and license

This is Alpha software. Back up important worlds and read [KNOWN_ISSUES.md](KNOWN_ISSUES.md) before installing.

On Old Mountain Time is distributed under an All Rights Reserved license. See [LICENSE.md](LICENSE.md) and [CREDITS.md](CREDITS.md).
