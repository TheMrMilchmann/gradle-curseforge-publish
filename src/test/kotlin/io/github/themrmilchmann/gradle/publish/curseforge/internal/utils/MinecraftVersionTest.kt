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
package io.github.themrmilchmann.gradle.publish.curseforge.internal.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Unit tests for `MinecraftVersion`
 *
 * @author  Leon Linhart
 */
class MinecraftVersionTest {

    @Test
    fun testExtractMinecraftVersionFromFabricLoomMinecraftDependencyVersion() {
        assertEquals(MinecraftVersion("2", "3", "77"), extractMinecraftVersionFromFabricLoomMinecraftDependencyVersion("2.3.77"))
        assertEquals(MinecraftVersion("2", "3", "0"), extractMinecraftVersionFromFabricLoomMinecraftDependencyVersion("2.3"))

        assertNull(extractMinecraftVersionFromFabricLoomMinecraftDependencyVersion("2.3.77.2"))
    }

    @Test
    fun testExtractMinecraftVersionFromForgeGradleMinecraftDependencyVersion() {
        assertEquals(MinecraftVersion("1", "20", "2"), extractMinecraftVersionFromForgeGradleMinecraftDependencyVersion("1.20.2-46.0.3"))
        assertEquals(MinecraftVersion("1", "20", "0"), extractMinecraftVersionFromForgeGradleMinecraftDependencyVersion("1.20-46.0.3"))
    }

    @Test
    fun testExtractMinecraftVersionFromNeoForgeVersion() {
        assertEquals(MinecraftVersion("1", "20", "2"), extractMinecraftVersionFromNeoForgeVersion("20.2.86"))
        assertEquals(MinecraftVersion("1", "20", "2"), extractMinecraftVersionFromNeoForgeVersion("20.2.86-beta"))

        assertNull(extractMinecraftVersionFromNeoForgeVersion("2.86-beta"))
        assertNull(extractMinecraftVersionFromNeoForgeVersion("20.2.86.4-beta"))
    }

}