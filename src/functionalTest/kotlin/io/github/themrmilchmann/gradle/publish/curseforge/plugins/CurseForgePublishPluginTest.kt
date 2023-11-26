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
package io.github.themrmilchmann.gradle.publish.curseforge.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.gradle.api.JavaVersion
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.copyToRecursively

class CurseForgePublishPluginTest {

    private companion object {

        private val fabricLoomVersions: Map<String, String> = mapOf(
            "1" to "",
            "2" to "",

        )

        @JvmStatic
        private fun provideGradleVersions(): List<String> = buildList {
            // See https://docs.gradle.org/current/userguide/compatibility.html
            val javaVersion = JavaVersion.current()

            add("8.4")
            add("8.3")
            add("8.2.1")
            add("8.1.1")
            add("8.0.2")
            add("7.6.3")

            @Suppress("UnstableApiUsage")
            if (javaVersion >= JavaVersion.VERSION_19) return@buildList

            add("7.5.1")

            @Suppress("UnstableApiUsage")
            if (javaVersion >= JavaVersion.VERSION_18) return@buildList

            add("7.4.2")
        }

        private fun provideFabricLoomVersions() {

        }

        @JvmStatic
        private fun provideSamples(): List<String> = buildList {
            add("forgegradle")
            add("gradle")

            // See https://docs.gradle.org/current/userguide/compatibility.html
            val javaVersion = JavaVersion.current()
            if (javaVersion < JavaVersion.VERSION_17) return@buildList

            add("loom")
        }

        @JvmStatic
        private fun provideTestArguments(): List<Arguments> = provideGradleVersions().flatMap { gradleVersion ->
            provideSamples().map { sample -> Arguments.of(gradleVersion, sample) }
        }

    }

    @field:TempDir
    lateinit var projectDir: File

//    @ParameterizedTest
//    @MethodSource("provideTestArguments")
//    fun test(gradleVersion: String, sample: String) {
//        @OptIn(ExperimentalPathApi::class)
//        Paths.get("./samples", sample).copyToRecursively(target = projectDir, followLinks = true)
//
//        GradleRunner.create()
//            .withArguments("build", "--info", "-S")
//            .withGradleVersion(gradleVersion)
//            .withPluginClasspath()
//            .withProjectDir(projectDir.toFile())
//            .forwardOutput()
//            .build()
//    }

    @Test
    fun test() {
        File(projectDir, "settings.gradle.kts").writeText(
            """
            pluginManagement {
                plugins {
                    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
                }
            
                repositories {
                    gradlePluginPortal()
                    maven(url = "https://maven.fabricmc.net")
                }
            }
            
            plugins {
                id("org.gradle.toolchains.foojay-resolver-convention")
            }
            
            rootProject.name = "test-project"
            """.trimIndent()
        )

        File(projectDir, "gradle.properties").writeText(
            """
            group=com.github.themrmilchmann.fency
            version=1.0.2-1.20.2-0.1
            
            org.gradle.caching=true
            org.gradle.jvmargs=-Xmx4G
            org.gradle.parallel=true
            org.gradle.welcome=never
            """.trimIndent()
        )

        File(projectDir, "build.gradle.kts").writeText(
            """
            import io.github.themrmilchmann.gradle.publish.curseforge.*

            plugins {
                id("fabric-loom") version "1.0.12"
                id("io.github.themrmilchmann.curseforge-publish") version "0.4.0"
                java
            }
            
            java {
                toolchain {
                    languageVersion.set(JavaLanguageVersion.of(17))
                }
            }
            
            curseforge {
                /*
                 * In a real application, it is recommended to store the API key outside the build script.
                 *
                 * // Store the key in "~/.gradle/gradle.properties"
                 * apiKey.set(extra["cfApiKey"] as String)
                 */
                apiToken.set("123e4567-e89b-12d3-a456-426614174000")

                publications {
                    named("fabric") {
                        projectId.set("123456") // The CurseForge project ID (required)

                        // Game Dependencies are inferred when Fabric Loom is detected

                        artifacts.named("main") {
                            displayName = "Example Project" // A user-friendly name for the project (optional)
                            releaseType = ReleaseType.RELEASE // The release type (required)

                            changelog {
                                format = ChangelogFormat.TEXT // The changelog (required)
                                content = "Example changelog..."
                            }
                        }
                    }
                }
            }

            repositories {
                mavenCentral()
            }

            dependencies {
                minecraft("com.mojang:minecraft:1.18.2")
                mappings(loom.officialMojangMappings())

                modImplementation("net.fabricmc.fabric-api:fabric-api:0.59.0+1.18.2")
                modImplementation("net.fabricmc:fabric-loader:0.14.9")
            }
            """.trimIndent()
        )

        embeddedServer(Netty, port = 8080) {
            routing {
                get("/api/game/version-types") {
                    call.respondText(contentType = ContentType.Application.Json) {
                        """
                        {
                            "fooar"
                        }
                        """.trimIndent()
                    }
                }
            }
        }.start()

        GradleRunner.create()
            .withGradleVersion("8.4")
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .withArguments("publish", "--info")
            .forwardOutput()
            .build()

    }

}