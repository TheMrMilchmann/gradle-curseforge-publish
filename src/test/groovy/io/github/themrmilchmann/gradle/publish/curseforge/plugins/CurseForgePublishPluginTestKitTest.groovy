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
package io.github.themrmilchmann.gradle.publish.curseforge.plugins

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.Unroll

class CurseForgePublishPluginTestKitTest extends Specification {

    private static def GRADLE_VERSIONS = [
        "7.4",
        "7.4.1",
        "7.4.2",
        "7.5",
        "7.5.1"
    ]

    @TempDir
    File projectDir
    File buildFile
    File settingsFile

    def setup() {
        buildFile = new File(projectDir, "build.gradle")
        settingsFile = new File(projectDir, "settings.gradle")
    }

    @Unroll
    def "configure (Gradle #gradleVersion)"() {
        given:
        buildFile << """\
            import io.github.themrmilchmann.gradle.publish.curseforge.*
            
            plugins {
                id 'java'
                id 'io.github.themrmilchmann.curseforge-publish'
            }
            
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(8)
                }
            }
            
            tasks.all {
                enabled = false
            }
            
            publishing {
                repositories {
                    curseForge {
                        repository {
                            /*
                             * In a real application, it is recommended to store the API key outside the build script.
                             *
                             * // Store the key in "~/.gradle/gradle.properties"
                             * apiKey = extra["cfApiKey"]
                             */
                            apiKey = "123e4567-e89b-12d3-a456-426614174000"
                        }
                    }
                }
                publications {
                    curseForge(CurseForgePublication) {
                        projectID = 123456 // The CurseForge project ID (required)
            
                        // Specify which game and version the mod/plugin targets (optional)
                        // When using the ForgeGradle plugin, this information is usually inferred and set automatically.
                        includeGameVersions { type, version -> type == "minecraft-1-16" && version == "1-16-5" }
            
                        artifact {
                            changelog = new Changelog("Example changelog...", ChangelogType.TEXT) // The changelog (required)
                            releaseType = ReleaseType.RELEASE // The release type (required)
            
                            displayName = "Example Project" // A user-friendly name for the project (optional)
                        }
                    }
                }
            }
        """.stripIndent()

        when:
        def result = runGradle(gradleVersion, "publish")

        then:
        result.task(":publish").outcome == TaskOutcome.SKIPPED

        where:
        gradleVersion << GRADLE_VERSIONS
    }

    private runGradle(String version, String... args) {
        def arguments = []
        arguments.addAll(args)
        arguments.add("-s")

        GradleRunner.create()
                .withGradleVersion(version)
                .withProjectDir(projectDir)
                .withArguments(arguments)
                .withPluginClasspath()
                .build()
    }

}