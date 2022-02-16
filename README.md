# CurseForge Gradle Publish

[![License](https://img.shields.io/badge/license-MIT-green.svg?style=flat-square&label=License)](https://github.com/TheMrMilchmann/gradle-curseforge-publish/blob/master/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.themrmilchmann.gradle.publish.curseforge/gradle-curseforge-publish.svg?style=flat-square&label=Maven%20Central)](https://maven-badges.herokuapp.com/maven-central/io.github.themrmilchmann.gradle.publish.curseforge/gradle-curseforge-publish)
[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v.svg?style=flat-square&&label=Gradle%20Plugin%20Portal&logo=Gradle&metadataUrl=https%3A%2F%2Fplugins.gradle.org%2Fm2%2Fio%2Fgithub%2Fthemrmilchmann%2Fcurseforge-publish%2Fio.github.themrmilchmann.curseforge-publish.gradle.plugin%2Fmaven-metadata.xml)](https://plugins.gradle.org/plugin/io.github.themrmilchmann.curseforge-publish)
![Gradle](https://img.shields.io/badge/Gradle-7.4-green.svg?style=flat-square&color=1ba8cb&logo=Gradle)
![Java](https://img.shields.io/badge/Java-8-green.svg?style=flat-square&color=b07219&logo=Java)

Provides the ability to publish build artifacts to [CurseForge](https://www.curseforge.com/).


## Usage

Learn how to set up basic publishing. While all concepts apply to both DSLs,
code snippets are for [Gradle's Kotlin DSL](https://docs.gradle.org/current/userguide/kotlin_dsl.html).
Use the [samples](samples) for reference when working with the Groovy DSL.


### Applying the Plugin

To use the plugin, include the following in your build script:

```kotlin
plugins {
    id("foo") version "0.1.0"
}
```

The plugin uses an extension on the project named `publishing` of type [PublishingExtension](https://docs.gradle.org/current/dsl/org.gradle.api.publish.PublishingExtension.html).
This extension provides a container of named publications and a container of
named repositories. The CurseForge Publish Plugin work with
`CurseForgePublication` publications and `CurseForgeArtifactRepository`
repositories.

Once the plugin has been applied, repositories and publications can be defined


### Publications

This plugin provides [publications](https://docs.gradle.org/current/userguide/dependency_management_terminology.html#sub:terminology_publication)
of type `CurseForgePublication`. 

```kotlin
publishing {
    publications {
        create<CurseForgePublication>("curseForge") {
            projectID.set(123456) // The CurseForge project ID (required)

            // Specify which game and version the mod/plugin targets (required)
            includeGameVersions { type, version -> type == "minecraft-1-16" && version == "minecraft-1-16-5" }

            artifact {
                changelog = Changelog("Example changelog...", ChangelogType.TEXT) // The changelog (required)
                releaseType = ReleaseType.RELEASE // The release type (required)

                displayName = "Example Project" // A user-friendly name for the project (optional)
            }
        }
    }
}
```


### Repositories

This plugin provides [repositories](https://docs.gradle.org/current/userguide/dependency_management_terminology.html#sub:terminology_repository)
of type `CurseForgeArtifactRepository`.

Here's a simple example of defining a publishing repository.

```kotlin
publishing {
    repositories {
        curseForge {
            /*
             * In a real application, it is recommended to store the API key
             * outside the build script.
             * 
             * // Store the key in "~/.gradle/gradle.properties"
             * apiKey.set(extra["cfApiKey"] as String)
             */
            apiKey.set("123e4567-e89b-12d3-a456-426614174000")
        }
    }
}
```

**Make sure not to check in your API key (or other secrets) in Git.**


## Compatibility Map

| Gradle | Minimal plugin version |
|--------|------------------------|
| 7.4    | 0.1.0                  |


## Building from source

### Setup

This project uses [Gradle's toolchain support](https://docs.gradle.org/7.4/userguide/toolchains.html)
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
- `build`                   - assemble and test the Java library
- `publishToMavenLocal`       - build and install all public artifacts to the
                              local maven repository

Additionally `tasks` may be used to print a list of all available tasks.


## License

```
Copyright (c) 2022 Leon Linhart

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