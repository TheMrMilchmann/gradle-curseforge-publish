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
package io.github.themrmilchmann.gradle.publish.curseforge.internal.publication

import io.github.themrmilchmann.gradle.publish.curseforge.CurseForgePublication
import org.gradle.api.JavaVersion
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Integration tests for [CurseForgePublication].
 *
 * @author  Leon Linhart
 */
class DefaultCurseForgePublicationTest {

    @TempDir
    private lateinit var projectDir: File

    private lateinit var publication: CurseForgePublication

    @BeforeEach
    fun setup() {
        val project = ProjectBuilder.builder()
            .withProjectDir(projectDir)
            .build()

        publication = project.objects.newInstance(DefaultCurseForgePublication::class.java, "test")
    }

    @Test
    fun testProjectId() {
        assertThrows<IllegalStateException> { publication.projectId.get() }

        publication.projectId.set("12345")
        assertEquals("12345", publication.projectId.get())
    }

    @Test
    fun testGameVersions() {
        assertEquals(0, publication.gameVersions.get().size)

        publication.gameVersion("foo", "bar")
        assertEquals(1, publication.gameVersions.get().size)
    }

    @Test
    fun testJavaVersions() {
        assertEquals(0, publication.javaVersions.get().size)

        publication.javaVersion(JavaVersion.VERSION_11)
        assertEquals(1, publication.javaVersions.get().size)
    }

    @Test
    fun testArtifacts() {
        assertEquals(0, publication.artifacts.size)

        publication.artifacts {
            assertEquals(this, publication.artifacts)
        }
    }

}