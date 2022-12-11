### 0.4.0

_Released 2022 Dec 11_

#### Improvements

- Plugin dependencies are now bundled with the plugin to prevent potential
  conflicts with other plugins.
- Support inferring metadata from [Fabric Loom](https://github.com/FabricMC/fabric-loom).
  - The Minecraft version and mod loader is now automatically set when the Loom
    plugin is detected.