# CurseForge Gradle Publish Plugin

[![License](https://img.shields.io/badge/license-MIT-green.svg?style=for-the-badge&label=License)](https://github.com/TheMrMilchmann/gradle-curseforge-publish/blob/master/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.themrmilchmann.gradle.publish.curseforge/gradle-curseforge-publish.svg?style=for-the-badge&label=Maven%20Central)](https://maven-badges.herokuapp.com/maven-central/io.github.themrmilchmann.gradle.publish.curseforge/gradle-curseforge-publish)
[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v.svg?style=for-the-badge&&label=Gradle%20Plugin%20Portal&logo=Gradle&metadataUrl=https%3A%2F%2Fplugins.gradle.org%2Fm2%2Fio%2Fgithub%2Fthemrmilchmann%2Fcurseforge-publish%2Fio.github.themrmilchmann.curseforge-publish.gradle.plugin%2Fmaven-metadata.xml)](https://plugins.gradle.org/plugin/io.github.themrmilchmann.curseforge-publish)
![Gradle](https://img.shields.io/badge/Gradle-8.0-green.svg?style=for-the-badge&color=1ba8cb&logo=Gradle)
![Java](https://img.shields.io/badge/Java-8-green.svg?style=for-the-badge&color=b07219&logo=Java)

A Gradle plugin that provides support for publishing artifacts to [CurseForge](https://www.curseforge.com/).


## Usage

> [!NOTE]
> The documentation of this plugin is written in [Gradle's Kotlin DSL](https://docs.gradle.org/current/userguide/kotlin_dsl.html).
> The plugin can also be used with Groovy build scripts and all concepts still
> apply but the exact syntax may differ.

### Applying the Plugin

To use the plugin, include the following in your build script:

```kotlin
plugins {
    id("io.github.themrmilchmann.curseforge-publish") version "0.6.1"
}
```

The plugin creates a top-level `curseforge` extension on the project. This
extension provides a convenient property which can be used to configure the API
token for all publications and a container of named publications.

Once the plugin has been applied, publications can be defined.


### Publications

This plugin provides publications similar to Gradle's built-in publishing
plugins (e.g. `maven-publish`). All required information for a publication is
the ID of the CurseForge project to upload the publication to and a `main`
artifact. A minimal publication could look as follows:

```kotlin
curseforge {
    publications {
        register("curseForge") {
            projectId = "123456"

            artifacts.register("main") {
                from(tasks.named("jar"))
            }
        }
    }
}
```

Usually, providing additional information to a publication is desirable, for
example, to improve discoverability or to give more information to users.

> [!NOTE]
> Information about mod dependencies (i.e. loader and game version) can
> automatically be inferred when using one of the available integrations.

```kotlin
curseforge {
    publications {
        register("curseForge") {
            projectId = "123456"

            gameVersions.add(GameVersion("minecraft-1-16", "1.16.5"))

            artifacts.register("main") {
                displayName = "Example Project"
                releaseType = ReleaseType.BETA

                changelog {
                    format = ChangelogFormat.MARKDOWN
                    from(file("CHANGELOG.md"))
                }

                relations {
                    embeddedLibrary("some-embedded-mod-slug")
                    incompatible("some-incompatible-mod-slug")
                    optionalDependency("some-optional-dependency-slug")
                    requiredDependency("some-required-dependency-slug")
                    tool("some-tool-slug")
                }
            }
        }
    }
}
```


### Authenticating with CurseForge

The plugin uses the [CurseForge Upload API](https://support.curseforge.com/en/support/solutions/articles/9000197321-curseforge-upload-api).
The API requires authentication using an API token. This token must be
configured through the `curseforge` extension before artifacts can be
published. (API tokens can be managed directly on [CurseForge](https://curseforge.com/account/api-tokens).)

```kotlin
curseforge {
    apiToken = "123e4567-e89b-12d3-a456-426614174000"
}
```

> [!WARNING]
> In the example above, a demo API token is hardcoded in the build script. This
> is dangerous as build scripts are usually committed into version-control which
> could leak the API token.
> Instead, consider placing the token in your user-specific Gradle properties
> (under `~/.gradle/gradle.properties`) by using `providers.gradleProperty("curseforgeApiToken")`.


## Interoperability

The CurseForge Gradle Publish Plugin provides out-of-box support for various
modding toolchains. When a supported toolchain is detected, the plugin attempts
to infer publications and information from the project. Typically, the plugin
then implicitly creates publications that are preconfigured using the inferred
data.

This plugin follows the [convention over configuration](https://en.wikipedia.org/wiki/Convention_over_configuration)
paradigm. Thus, all information inferred from the project may be overwritten.
Domain object creating can be tweaked using Gradle properties and properties
may be set explicitly to overwrite conventions. Refer to the toolchain-specific
sections for more information.

Using inference and further configuring implicit publications is the recommended
approach for working with this plugin. Alternatively, it is also possible to
disable integrations.


### FabricLoom

When the [Fabric Loom](https://github.com/FabricMC/fabric-loom) plugin is
detected, a publication is implicitly created. This publication is preconfigured
with the Fabric mod loader dependency and a dependency on the Minecraft version
that is targeted during the build.

| Property                                                       | Description                                     | Default  |
|----------------------------------------------------------------|-------------------------------------------------|----------|
| gradle-curseforge-publish.interop.fabric-loom                  | Whether the integration is enabled              | `true`   |
| gradle-curseforge-publish.interop.fabric-loom.publication-name | The name for the implicitly created publication | `fabric` |


### ForgeGradle

When the [ForgeGradle](https://github.com/MinecraftForge/ForgeGradle) plugin is
detected, a publication is implicitly created. This publication is preconfigured
with the MinecraftForge mod loader dependency and a dependency on the Minecraft
version that is targeted during the build.

| Property                                                        | Description                                     | Default          |
|-----------------------------------------------------------------|-------------------------------------------------|------------------|
| gradle-curseforge-publish.interop.forge-gradle                  | Whether the integration is enabled              | `true`           |
| gradle-curseforge-publish.interop.forge-gradle.publication-name | The name for the implicitly created publication | `minecraftForge` |


### NeoGradle

When the [NeoGradle](https://github.com/neoforged/NeoGradle) plugin is detected,
a publication is implicitly created. This publication is preconfigured with the
NeoForge mod loader dependency and a dependency on the Minecraft version that is
targeted during the build.

| Property                                                     | Description                                     | Default    |
|--------------------------------------------------------------|-------------------------------------------------|------------|
| gradle-curseforge-publish.interop.neogradle                  | Whether the integration is enabled              | `true`     |
| gradle-curseforge-publish.interop.neogradle.publication-name | The name for the implicitly created publication | `neoForge` |


### Java

When the `java-base` plugin is detected, the supported Java version is
automatically inferred for the compilation target of the `compileJava` task.

The inferred version can be overwritten by specifying the Java version manually
for a publication:

```kotlin
curseforge {
    publications {
        named("main") {
            javaVersions.add(JavaVersion.VERSION_1_8)
        }
    }
}
```


## Compatibility Map

The compatibility can be referred to, to check which Gradle versions are
supported by a given version of the plugin or which plugin version is required
for a given version of Gradle.

If a plugin version is not listed, it's compatibility has not changed since the
most recent listed version that is lower than the given version. Similarly, if a
maximum supported Gradle version is not listed, it is assumed that the plugin is
still working with the most recent version of Gradle.

| Plugin Version | Minimum Gradle Versions | Maximum Supported Gradle Version |
|----------------|-------------------------|----------------------------------|
| 0.6.0          | 7.6                     |                                  |
| 0.5.0          | 7.4                     | 7.6.*                            |
| 0.1.0          | 7.4                     | 7.5.*                            |

If a plugin version is not compatible with a version of Gradle despite this
table stating the opposite, please make sure to check the issue tracker and file
an issue if necessary.


## Building from source

### Setup

This project uses [Gradle's toolchain support](https://docs.gradle.org/current/userguide/toolchains.html)
to detect and select the JDKs required to run the build. Please refer to the
build scripts to find out which toolchains are requested.

An installed JDK 1.8 (or later) is required to use Gradle.

### Building

Once the setup is complete, invoke the respective Gradle tasks using the
following command on Unix/macOS:

    ./gradlew <tasks>

or the following command on Windows:

    gradlew <tasks>

Important Gradle tasks to remember are:
- `clean`                   - clean build results
- `build`                   - assemble and test the plugin
- `functionalTest`          - run the functional tests to verify compatibility
                              with different versions of Gradle and mod 
                              toolchains
- `publishToMavenLocal`     - build and install all public artifacts to the
                              local maven repository

Additionally `tasks` may be used to print a list of all available tasks.


## License

```
Copyright (c) 2022-2023 Leon Linhart

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```