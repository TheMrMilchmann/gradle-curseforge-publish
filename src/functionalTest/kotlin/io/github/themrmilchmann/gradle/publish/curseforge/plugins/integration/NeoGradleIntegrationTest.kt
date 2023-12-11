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
package io.github.themrmilchmann.gradle.publish.curseforge.plugins.integration

import io.github.themrmilchmann.gradle.publish.curseforge.plugins.AbstractFunctionalPluginTest
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File

/**
 * Functional tests for the integration with the NeoGradle plugin.
 *
 * @author  Leon Linhart
 */
class NeoGradleIntegrationTest : AbstractFunctionalPluginTest() {

    private companion object {

        @JvmStatic
        private fun provideTestArguments(): List<Arguments> {
            return provideGradleVersions().mapNotNull { gradleVersion -> when {
                gradleVersion >= "8.0" -> "7.0.61"
                else -> null
            }?.let { Arguments.of(gradleVersion, it) }}
        }

    }

    @field:TempDir
    lateinit var projectDir: File

    @ParameterizedTest
    @MethodSource("provideTestArguments")
    fun testIntegration(gradleVersion: String, forgeGradleVersion: String) {
        File(projectDir, "settings.gradle.kts").writeText(
            """
            pluginManagement {
                plugins {
                    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
                }
            
                repositories {
                    gradlePluginPortal()
                    maven(url = "https://maven.neoforged.net/releases")
                }
            }
            
            plugins {
                id("org.gradle.toolchains.foojay-resolver-convention")
            }
            
            rootProject.name = "test-project"
            """.removeSuffix("\n").trimIndent()
        )

        File(projectDir, "build.gradle.kts").writeText(
            """
            import io.github.themrmilchmann.gradle.publish.curseforge.*
            
            plugins {
                id("io.github.themrmilchmann.curseforge-publish")
                id("net.neoforged.gradle.userdev") version "$forgeGradleVersion"
                java
            }
            
            java {
                toolchain {
                    languageVersion.set(JavaLanguageVersion.of(17))
                }
            }
            
            curseforge {
                apiToken.set("123e4567-e89b-12d3-a456-426614174000")

                publications {
                    named("neoForge") {
                        projectId.set("$PROJECT_ID")
                    }
                }
            }
            
            dependencies {
                implementation("net.neoforged:neoforge:20.2.86")
            }
            """.removeSuffix("\n").trimIndent()
        )

        val result = GradleRunner.create()
            .withGradleVersion(gradleVersion)
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .withArguments("publish", "--info", "-Dorg.gradle.jvmargs=-Xmx3g", "-Pgradle-curseforge-publish.internal.base-url=http://localhost:8080")
            .forwardOutput()
            .build()

        assertTrue("Published main artifact (artifact 67890) of publication 'neoForge' to CurseForge (project $PROJECT_ID)" in result.output)
        assertTrue("Published publication 'neoForge' to CurseForge (project $PROJECT_ID)" in result.output)
    }

}