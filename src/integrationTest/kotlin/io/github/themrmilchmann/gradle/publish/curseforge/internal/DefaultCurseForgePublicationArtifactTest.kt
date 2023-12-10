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
package io.github.themrmilchmann.gradle.publish.curseforge.internal

import io.github.themrmilchmann.gradle.publish.curseforge.ChangelogFormat
import io.github.themrmilchmann.gradle.publish.curseforge.CurseForgePublicationArtifact
import io.github.themrmilchmann.gradle.publish.curseforge.ReleaseType
import io.github.themrmilchmann.gradle.publish.curseforge.internal.artifacts.CurseForgeArtifactNotationParser
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Integration tests for [DefaultCurseForgePublicationArtifact].
 *
 * @author  Leon Linhart
 */
class DefaultCurseForgePublicationArtifactTest {

    @TempDir
    private lateinit var projectDir: File

    private lateinit var artifact: CurseForgePublicationArtifact

    @BeforeEach
    fun setup() {
        val project = ProjectBuilder.builder()
            .withProjectDir(projectDir)
            .build()

        val artifactFactory = project.objects.newInstance(CurseForgeArtifactNotationParser::class.java)
        artifact = project.objects.newInstance(DefaultCurseForgePublicationArtifact::class.java, "test", artifactFactory)
    }

    @Test
    fun testDisplayName() {
        assertThrows<IllegalStateException> { artifact.displayName.get() }

        artifact.displayName.set("Hello, World!")
        assertEquals("Hello, World!", artifact.displayName.get())
    }

    @Test
    fun testReleaseType() {
        assertEquals(ReleaseType.RELEASE, artifact.releaseType.get())

        artifact.releaseType.set(ReleaseType.ALPHA)
        assertEquals(ReleaseType.ALPHA, artifact.releaseType.get())
    }

    @Test
    fun testChangelog() {
        val changelog = artifact.changelog

        assertEquals(ChangelogFormat.TEXT, changelog.format.get())
        assertEquals("", changelog.content.get())

        artifact.changelog {
            assertEquals(this, changelog)

            format.set(ChangelogFormat.MARKDOWN)
            content.set("Hello, World!")
        }

        assertEquals(ChangelogFormat.MARKDOWN, changelog.format.get())
        assertEquals("Hello, World!", changelog.content.get())
    }

    @Test
    fun testRelations() {
        // TODO impl
    }

}