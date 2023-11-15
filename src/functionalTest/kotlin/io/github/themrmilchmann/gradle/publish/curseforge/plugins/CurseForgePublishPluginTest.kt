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

import org.gradle.api.JavaVersion
import org.gradle.testkit.runner.GradleRunner
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

        @JvmStatic
        private fun provideGradleVersions(): List<String> = buildList {
            // See https://docs.gradle.org/current/userguide/compatibility.html
            val javaVersion = JavaVersion.current()

            // We don't support Gradle 8+ yet.
            add("7.6.3")

            @Suppress("UnstableApiUsage")
            if (javaVersion >= JavaVersion.VERSION_19) return@buildList

            add("7.5.1")

            @Suppress("UnstableApiUsage")
            if (javaVersion >= JavaVersion.VERSION_18) return@buildList

            add("7.4.2")
        }

        @JvmStatic
        private fun provideSamples(): List<String> = buildList {
            add("forgegradle-kotlin")
            add("gradle-kotlin")

            // See https://docs.gradle.org/current/userguide/compatibility.html
            val javaVersion = JavaVersion.current()
            if (javaVersion < JavaVersion.VERSION_17) return@buildList

            add("loom-kotlin")
        }

        @JvmStatic
        private fun provideTestArguments(): List<Arguments> = provideGradleVersions().flatMap { gradleVersion ->
            provideSamples().map { sample -> Arguments.of(gradleVersion, sample) }
        }

    }

    @field:TempDir
    lateinit var projectDir: Path

    @ParameterizedTest
    @MethodSource("provideTestArguments")
    fun test(gradleVersion: String, sample: String) {
        @OptIn(ExperimentalPathApi::class)
        Paths.get("./samples", sample).copyToRecursively(target = projectDir, followLinks = true)

        GradleRunner.create()
            .withArguments("build", "--info", "-S")
            .withGradleVersion(gradleVersion)
            .withPluginClasspath()
            .withProjectDir(projectDir.toFile())
            .forwardOutput()
            .build()
    }

}