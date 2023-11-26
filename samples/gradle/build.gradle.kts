/*
 * Copyright (c) 2022-2023 Leon Linhart
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
import io.github.themrmilchmann.gradle.publish.curseforge.*

plugins {
    id("io.github.themrmilchmann.curseforge-publish") version "0.6.0"
    java
}

version = "0.1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

curseforge {
    /*
     * In a real application, it is recommended to store the API key outside the build script.
     *
     * // Store the key in "~/.gradle/gradle.properties"
     * apiKey.set(extra["cfApiKey"] as String)
     */
    apiKey.set("123e4567-e89b-12d3-a456-426614174000")

    publications {
        register("curseForge") {
            projectId.set("123456") // The CurseForge project ID (required)

            // Specify which game and version the mod/plugin targets (optional)
            gameVersions.add(GameVersion(type = "minecraft-1-16", version = "1-16-5"))

            // Specify which Java version is supported (optional)
            javaVersions.add(JavaVersion.VERSION_1_8)

            artifact(tasks.named("jar")) {
                changelog = Changelog("Example changelog...", ChangelogType.TEXT) // The changelog (required)
                releaseType = ReleaseType.RELEASE // The release type (required)
                 
                displayName = "Example Project" // A user-friendly name for the project (optional)
            }
        }
    }
}