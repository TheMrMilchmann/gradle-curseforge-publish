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
 * Functional tests for the integration with the ForgeGradle plugin.
 *
 * @author  Leon Linhart
 */
class ForgeGradleIntegrationTest : AbstractFunctionalPluginTest() {

    private companion object {

        @JvmStatic
        private fun provideTestArguments(): List<Arguments> {
            return provideGradleVersions().mapNotNull { gradleVersion -> when {
                gradleVersion >= "8.7" -> null
                gradleVersion >= "8.1" -> "6.0.16"
                gradleVersion >= "8.0" -> null
                else -> "5.1.77"
            }?.let { Arguments.of(gradleVersion, it) }}
        }

    }

    @field:TempDir
    lateinit var projectDir: File

    @ParameterizedTest
    @MethodSource("provideTestArguments")
    fun testIntegration(gradleVersion: CharSequence, forgeGradleVersion: String) {
        File(projectDir, "settings.gradle.kts").writeText(
            """
            pluginManagement {
                plugins {
                    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
                }
            
                repositories {
                    gradlePluginPortal()
                    maven(url = "https://maven.minecraftforge.net")
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
                id("net.minecraftforge.gradle") version "$forgeGradleVersion"
                java
            }
            
            java {
                toolchain {
                    languageVersion.set(JavaLanguageVersion.of(17))
                }
            }

            minecraft {
                mappings("official", "1.20.2")
            }
            
            curseforge {
                apiToken.set("123e4567-e89b-12d3-a456-426614174000")

                publications {
                    named("minecraftForge") {
                        projectId.set("$PROJECT_ID")
                    }
                }
            }
            
            dependencies {
                minecraft("net.minecraftforge:forge:1.20.2-48.0.1")
            }
            """.removeSuffix("\n").trimIndent()
        )

        val result = GradleRunner.create()
            .withGradleVersion(gradleVersion.toString())
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .withArguments("publish", "--info", "--build-cache", "-Dorg.gradle.jvmargs=-Xmx2g", "-Pgradle-curseforge-publish.internal.base-url=http://localhost:8080")
            .forwardOutput()
            .build()

        assertTrue("Published main artifact (artifact 67890) of publication 'minecraftForge' to CurseForge (project $PROJECT_ID)" in result.output)
        assertTrue("Published publication 'minecraftForge' to CurseForge (project $PROJECT_ID)" in result.output)
    }

}