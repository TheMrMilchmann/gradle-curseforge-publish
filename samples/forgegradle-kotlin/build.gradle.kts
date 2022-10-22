/*
 * Copyright (c) 2022 Leon Linhart
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
    java
    id("net.minecraftforge.gradle") version "5.1.0"
    id("io.github.themrmilchmann.curseforge-publish") version "0.3.0"
}

version = "0.1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

minecraft {
    mappings("official", "1.16.5")
}

publishing {
    repositories {
        curseForge {
            /*
             * In a real application, it is recommended to store the API key outside the build script.
             *
             * // Store the key in "~/.gradle/gradle.properties"
             * apiKey.set(extra["cfApiKey"] as String)
             */
            apiKey.set("123e4567-e89b-12d3-a456-426614174000")
        }
    }
    publications {
        create<CurseForgePublication>("curseForge") {
            projectID.set(123456) // The CurseForge project ID (required)

            // Game Dependencies are inferred when ForgeGradle is detected

            artifact {
                changelog = Changelog("Example changelog...", ChangelogType.TEXT) // The changelog (required)
                releaseType = ReleaseType.RELEASE // The release type (required)

                displayName = "Example Project" // A user-friendly name for the project (optional)
            }
        }
    }
}

dependencies {
    minecraft("net.minecraftforge:forge:1.16.5-36.2.2")
}