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
package io.github.themrmilchmann.gradle.publish.curseforge.internal.artifacts

import io.github.themrmilchmann.gradle.publish.curseforge.CurseForgeArtifact
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.CollectionCallbackActionDecorator
import org.gradle.api.internal.DefaultDomainObjectSet
import org.gradle.api.internal.file.FileCollectionFactory
import org.gradle.api.internal.file.collections.MinimalFileSet
import org.gradle.api.internal.tasks.AbstractTaskDependency
import org.gradle.api.internal.tasks.TaskDependencyResolveContext
import org.gradle.api.publish.internal.PublicationArtifactSet
import java.io.File

internal open class CurseForgeArtifactSet(
    private val publicationName: String,
    fileCollectionFactory: FileCollectionFactory,
    collectionCallbackActionDecorator: CollectionCallbackActionDecorator
) : DefaultDomainObjectSet<CurseForgeArtifact>(
    CurseForgeArtifact::class.java,
    collectionCallbackActionDecorator
), PublicationArtifactSet<CurseForgeArtifact> {

    private val files = fileCollectionFactory.create(ArtifactsTaskDependency(), ArtifactsFileCollection())
    override fun getFiles(): FileCollection = files

    private inner class ArtifactsFileCollection : MinimalFileSet {

        override fun getDisplayName(): String =
            "artifacts for CurseForge publication '$publicationName'"

        override fun getFiles(): MutableSet<File> =
            this@CurseForgeArtifactSet.map { it.file }.toCollection(LinkedHashSet())

    }

    private inner class ArtifactsTaskDependency : AbstractTaskDependency() {

        override fun visitDependencies(context: TaskDependencyResolveContext) {
            this@CurseForgeArtifactSet.forEach(context::add)
        }

    }

}