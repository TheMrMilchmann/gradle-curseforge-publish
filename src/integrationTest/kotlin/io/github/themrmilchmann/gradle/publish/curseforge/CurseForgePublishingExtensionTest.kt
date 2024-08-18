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
package io.github.themrmilchmann.gradle.publish.curseforge

import io.github.themrmilchmann.gradle.publish.curseforge.internal.DefaultCurseForgePublicationContainer
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Integration tests for [CurseForgePublishingExtension].
 *
 * @author  Leon Linhart
 */
class CurseForgePublishingExtensionTest {

    @TempDir
    private lateinit var projectDir: File

    private lateinit var extension: CurseForgePublishingExtension

    @BeforeEach
    fun setup() {
        val project = ProjectBuilder.builder()
            .withProjectDir(projectDir)
            .build()

        val publicationContainer = project.objects.newInstance(DefaultCurseForgePublicationContainer::class.java)
        extension = project.objects.newInstance(CurseForgePublishingExtension::class.java, publicationContainer)
    }

    @Test
    fun testApiToken() {
        assertThrows<IllegalStateException> { extension.apiToken.get() }

        extension.apiToken.set("123e4567-e89b-12d3-a456-426614174000")
        assertEquals("123e4567-e89b-12d3-a456-426614174000", extension.apiToken.get())
    }

    @Test
    fun testPublications() {
        assertEquals(0, extension.publications.size)

        extension.publications {
            assertEquals(this, extension.publications)
        }
    }

}