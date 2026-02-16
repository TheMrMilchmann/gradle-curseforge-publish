### 0.9.0

_Released 2026 Feb 16_

#### Improvements

- The plugin now works with Gradle 9.

#### Breaking Changes

- The plugin now requires Java 21 or later (from 8).
- The plugin now requires at least Gradle 9.0.0 (from 8.0.0).
- The plugin is now tested against later versions of Minecraft modding tools:
    - The plugin is now tested against [Fabric Loom](https://github.com/FabricMC/fabric-loom) 1.11.7 (from 1.4.5).
    - The plugin is now tested against [ModDevGradle](https://github.com/neoforged/ModDevGradle) 2.0.107 (from 1.0.19).
    - The plugin is now tested against [NeoGradle](https://github.com/neoforged/NeoGradle) 7.0.192 (from 7.0.61).
    - This can lead to auto-detection failure when using older versions of these tools. In such cases, manual
      configuration is required.
- The integration for ForgeGradle has been removed as it's incompatible with Gradle 9. It is generally recommended to
  migrate to NeoForge moving forward.
