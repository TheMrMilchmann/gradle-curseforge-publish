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
package io.github.themrmilchmann.gradle.publish.curseforge.plugins.integration

import io.github.themrmilchmann.gradle.publish.curseforge.CurseForgePublishingExtension
import io.github.themrmilchmann.gradle.publish.curseforge.plugins.CurseForgePublishPlugin
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import net.neoforged.moddevgradle.dsl.RunModel
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Integration tests for the integration with the NeoForge ModDevGradle plugin.
 *
 * @author  Leon Linhart
 */
class NeoForgeModDevGradleIntegrationTest {

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

        project.pluginManager.apply("net.neoforged.moddev")
        project.pluginManager.apply("java")

        val neoforge = project.extensions.getByType(NeoForgeExtension::class.java)
        neoforge.version.set("21.0.103-beta")
        neoforge.runs {
            create("client") { client() }
            create("server") { server() }
        }
    }

    @Test
    fun testIntegration() {
        val publication = extension.publications.findByName("neoForge")
        assertNotNull(publication)

        val gameVersions = publication!!.gameVersions.get()
        assertEquals(2, gameVersions.size)
        assertTrue(gameVersions.any { it.type == "minecraft-1-21" && it.version == "1-21-0" })
        assertTrue(gameVersions.any { it.type == "modloader" && it.version == "neoforge" })

        val mainArtifact = publication.artifacts.findByName("main")
        assertNotNull(mainArtifact)
    }

}