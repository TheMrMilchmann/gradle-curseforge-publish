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
package io.github.themrmilchmann.gradle.publish.curseforge.internal.artifacts

import io.github.themrmilchmann.gradle.publish.curseforge.*
import org.gradle.api.internal.tasks.*
import org.gradle.api.publish.internal.*
import org.gradle.api.tasks.*
import java.io.*

internal class DerivedCurseForgeArtifact(
    original: AbstractCurseForgeArtifact,
    private val derivedFile: PublicationInternal.DerivedArtifact
) : AbstractCurseForgeArtifact() {

    override var changelog: Changelog = original.changelog
    override var displayName: String? = original.displayName
    override var releaseType: ReleaseType = original.releaseType

    override fun getDefaultBuildDependencies(): TaskDependency =
        TaskDependencyInternal.EMPTY

    override fun getFile(): File =
        derivedFile.create()!!

}