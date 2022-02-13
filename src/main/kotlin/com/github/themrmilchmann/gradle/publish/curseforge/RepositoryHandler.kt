/*
 * Copyright (c) 2022 Leon Linhart
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
package com.github.themrmilchmann.gradle.publish.curseforge

import com.github.themrmilchmann.gradle.publish.curseforge.internal.artifacts.repositories.*
import com.github.themrmilchmann.gradle.publish.curseforge.plugins.*
import org.gradle.api.*
import org.gradle.api.artifacts.dsl.*

public fun RepositoryHandler.curseForge(action: Action<in CurseForgeArtifactRepository>): CurseForgeArtifactRepository =
    curseForge("https://minecraft.curseforge.com", action)

public fun RepositoryHandler.curseForge(url: String, action: Action<in CurseForgeArtifactRepository>): CurseForgeArtifactRepository =
    CurseForgePublishPlugin.gradle.rootProject.objects.newInstance(DefaultCurseForgeArtifactRepository::class.java, url)
        .also(action::execute)
        .also(::add)