# Installation

## Required versions

| Component | Version |
| --- | --- |
| Minecraft | 26.1.2 |
| Java | 25 |
| Fabric Loader | 0.19.3 |
| Fabric API | 0.155.2+26.1.2 |
| Lithostitched | 1.7.13-fabric-26.1 |

## Client

1. Install Java 25.
2. Create a Minecraft 26.1.2 profile using Fabric Loader 0.19.3.
3. Place Fabric API, Lithostitched, and `before-the-blight-0.1.0-alpha.1.jar` in that profile's `mods` folder.
4. Remove older or duplicate copies of those mods.
5. Launch the profile and create a new test world before opening an important save.

Only newly generated chunks contain the mod's biomes, trees, and natural structures.

## Dedicated server

Use Minecraft 26.1.2, Java 25, and Fabric Loader 0.19.3 on the server. Install the same release JAR, Fabric API, and Lithostitched in the server's `mods` folder. Clients joining that server need the same required mod set.

Stop the server cleanly and back up its world before changing mod versions.

## Optional companions

Recipe viewers, seasonal mods, terrain companions, connected-texture renderers, and shader stacks are optional. They are not required to launch or play the Alpha.

The exact approved artifact filename and SHA-256 are listed in [RELEASE_MANIFEST.md](RELEASE_MANIFEST.md).
