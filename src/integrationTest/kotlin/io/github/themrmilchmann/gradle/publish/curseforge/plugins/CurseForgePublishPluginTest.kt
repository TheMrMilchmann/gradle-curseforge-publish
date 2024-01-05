/*
 * Copyright (c) 2021-2023 Leon Linhart,
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * 4. Redistributions of the software or derivative works, in any form, for
 *    commercial purposes, are strictly prohibited without the express written
 *    consent of the copyright holder. For the purposes of this license,
 *    "commercial purposes" includes, but is not limited to, redistributions
 *    through applications or services that generate revenue through
 *    advertising. This clause does neither not apply to "community
 *    contributions", as defined by the act of submitting changes or additions
 *    to the software, provided that the contributors are not copyright
 *    holders of the software, nor does it apply to derivatives of the software.
 *
 * 5. Any modifications or derivatives of the software, whether in source or
 *    binary form, must be made publicly available under the same license terms
 *    as this original license. This includes providing access to the modified
 *    source code.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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