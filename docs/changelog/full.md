### 0.5.0

_Released 2023 Nov 19_

#### Improvements

- Added support for artifact relations. [[GH-21](https://github.com/TheMrMilchmann/gradle-curseforge-publish/issues/21)]
- Plugin dependencies are now shadowed to avoid version conflicts with other
  plugins.

#### Fixes

- Improved resilience against changes to the CurseForge upload API.
- When using Loom, the correct mod artifact is now published by default. [[GH-19](https://github.com/TheMrMilchmann/gradle-curseforge-publish/issues/19)]



---

### 0.4.0

_Released 2022 Dec 11_

#### Improvements

- Plugin dependencies are now bundled with the plugin to prevent potential
  conflicts with other plugins.
- Support inferring metadata from [Fabric Loom](https://github.com/FabricMC/fabric-loom).
    - The Minecraft version and mod loader is now automatically set when the Loom
      plugin is detected.


---

### 0.3.0

_Released 2022 Oct 22_

#### Fixes

- Fixed how publishable files are exposed to Gradle. [[GH-17](https://github.com/TheMrMilchmann/gradle-curseforge-publish/issues/17)]
    - This should also fix the dependencies of the publishing tasks.
- Improved resilience to CurseForge changes by ignoring unknown keys in
  JSON responses. [[GH-18](https://github.com/TheMrMilchmann/gradle-curseforge-publish/issues/18)]


---

### 0.2.0

_Released 2022 Oct 17_

#### Improvements

- Improved error messages across the board.
- Improved reliability of Java version inference.
    - The inference now uses the `compileJava` task's properties instead of the
      project-wide toolchain.
    - The inferred Java version can now be overwritten and additional versions can
      be added using the `javaVersion` and `javaVersions` functions.

#### Fixes

- Fixed a bug that prevented the plugin from automatically deriving the
  Minecraft dependency from the ForgeGradle plugin (if available).
- Fixed a bug that could cause Gradle to issue confusing warnings.


---

### 0.1.0

_Released 2022 Feb 13_

#### Overview

CurseForge Publish provides the ability to publish build artifacts to [CurseForge](https://www.curseforge.com/).