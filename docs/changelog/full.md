### 0.8.0

_Released 2025 Apr 01_

#### Improvements

- Added support for [ModDevGradle](https://github.com/neoforged/ModDevGradle) version 2.


---

### 0.7.0

_Released 2024 Nov 28_

#### Improvements

- Added `@DslMarker` annotations to the public configuration DSL to prevent
  accidental usage of functions from outer scopes.
- Implemented a new `from(any: Any)` function to flexibly parse changelog
  contents from char sequences or file sources.
  - This function was erroneously mentioned in the documentation already.
- Support inferring metadata from [ModDevGradle](https://github.com/neoforged/ModDevGradle).
  - The Minecraft version and mod loader is now automatically set when the
    ModDevGradle plugin is detected.

#### Breaking Changes

- As a result of the added `@DslMarker`, the public configuration DSL is now
  stricter and may require changes to existing build scripts.
- Updated the minimum required Gradle version to 8.0 (from 7.6).
- Updated the minimum required Java version to 17 (from 8).


---

### 0.6.1

_Released 2024 Jan 07_

#### Fixes

- Restored authentication against CurseForge.


---

### 0.6.0

_Released 2024 Jan 07_

#### Overview

This update contains a major rework of the API to be fully compatible with
Gradle's configuration caching to be less likely to break on Gradle updates
ahead of the 1.0.0 release.

The API is considered to be mostly stable now is unlikely to change going
forward. `1.0.0` will follow once enough testing has been done and feedback is
gathered.

#### Improvements

- The plugin is now compatible with Gradle 8 (tested up to Gradle 8.5).
    - The minimum required version is Gradle 7.6.
- Implemented support for Gradle's configuration caching. [[GH-22](https://github.com/GW2ToolBelt/GW2ChatLinks/issues/22)]
- Refactored integration with mod toolchain plugins.
    - Multiple mod toolchains can now coexist in a single Gradle project.
    - By default, publications are created with reasonable defaults when mod toolchains are detected.
    - Inference can be disabled via Gradle properties.
- Improved documentation and added missing JavaDoc to all public functionality.
- Implemented reasonable defaults for changelog, release type and display name.
- Publications can now fully participate in incremental builds.

#### Breaking Changes

- Migrated from the `publishing` extension to a custom `curseforge` extension.
- Renamed `apiKey` property to `apiToken` for consistency with CurseForge.
- Unified the concept of artifacts and extra artifacts in the publication DSL.
    - The primary artifact for a publication must have the special name `main`.
- The changelog is now configured through a DSL instead of a plain Java object
  for greater flexibility and better support for incremental builds.


---

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
