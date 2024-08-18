/*
 * Copyright (c) 2022-2024 Leon Linhart
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

import io.github.themrmilchmann.gradle.publish.curseforge.CurseForgePublishingExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Integration tests for [CurseForgePublishPlugin].
 *
 * @author  Leon Linhart
 */
@Suppress("FunctionName")
class CurseForgePublishPluginTest {

    @TempDir
    private lateinit var projectDir: File

    private lateinit var project: Project
    private lateinit var extension: CurseForgePublishingExtension

    @BeforeEach
    fun beforeEach() {
        project = ProjectBuilder.builder()
            .withProjectDir(projectDir)
            .build()

        project.pluginManager.apply(CurseForgePublishPlugin::class.java)

        extension = project.extensions.getByName(CurseForgePublishingExtension.NAME) as CurseForgePublishingExtension
        assertNotNull(extension)
    }

    @Test
    fun testApplyPlugin() {
        assertTrue(extension.publications.isEmpty(), "Unexpected CurseForge publication found")
    }

    @Test
    fun testPublication() {
        extension.publications.create("test")
        val publishToCurseForge = project.tasks.getByName("publishToCurseForge")
        val publishTestPublicationToCurseForge = project.tasks.getByName("publishTestPublicationToCurseForge")

        assertTrue(publishTestPublicationToCurseForge in publishToCurseForge.taskDependencies.getDependencies(null))
    }

    @Test
    fun testPublishToCurseForgeTask() {
        val publishToCurseForge = project.tasks.getByName("publishToCurseForge")
        assertEquals("Publishes all CurseForge publications produced by this project.", publishToCurseForge.description)
        assertEquals(PublishingPlugin.PUBLISH_TASK_GROUP, publishToCurseForge.group)
    }

    @Test
    fun testJavaIntegration_InferenceFromRelease() {
        val publication = extension.publications.create("test")

        assertEquals(1, extension.publications.size)
        assertEquals(0, publication.javaVersions.get().size)

        project.pluginManager.apply("java")
        project.tasks.named("compileJava", JavaCompile::class.java) {
            options.release.set(8)
        }

        assertEquals(1, publication.javaVersions.get().size)
        assertEquals(JavaVersion.VERSION_1_8, publication.javaVersions.get().single())
    }

    @Test
    fun testJavaIntegration_InferenceFromTargetCompatibility() {
        val publication = extension.publications.create("test")

        assertEquals(1, extension.publications.size)
        assertEquals(0, publication.javaVersions.get().size)

        project.pluginManager.apply("java")
        project.tasks.named("compileJava", JavaCompile::class.java) {
            targetCompatibility = "8"
        }

        assertEquals(1, publication.javaVersions.get().size)
        assertEquals(JavaVersion.VERSION_1_8, publication.javaVersions.get().single())
    }

    @Test
    fun testPublishingIntegration() {
        extension.publications.create("test")

        val publish = project.tasks.getByName(PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME)

        val publishToCurseForge = project.tasks.getByName("publishToCurseForge")
        assertTrue(publishToCurseForge in publish.taskDependencies.getDependencies(null))

        val publishTestPublicationToCurseForge = project.tasks.getByName("publishTestPublicationToCurseForge")
        assertTrue(publishTestPublicationToCurseForge in publish.taskDependencies.getDependencies(null))
    }

}